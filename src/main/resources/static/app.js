'use strict';
const $ = (selector, root=document) => root.querySelector(selector);
const money = value => Number(value).toLocaleString('pt-BR',{style:'currency',currency:'BRL'});
const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const labels = {RASCUNHO:'Rascunho',ENVIADO:'Enviado',APROVADO:'Aprovado',REJEITADO:'Rejeitado',CANCELADO:'Cancelado'};
const itemStatusLabels = {PENDENTE:'Pendente',EM_PRODUCAO:'Em produção',CONCLUIDO:'Concluído'};
let quotes=[], customers=[], editing=null, detail=null, editingCustomer=null;
const selectedQuoteIds=new Set();
async function api(path,method='GET',body) {
  const response=await fetch('/api'+path,{method,headers:body?{'Content-Type':'application/json'}:{},body:body?JSON.stringify(body):undefined});
  const data=await response.json().catch(()=>({message:'Resposta inesperada do servidor.'}));
  if(!response.ok) throw new Error(data.message+(data.errors?'\n'+data.errors.join('\n'):''));
  return data;
}
function notice(text,error=false){const el=$('#notice');el.textContent=text;el.classList.toggle('error',error);el.hidden=false;}
function badge(status){return `<span class="badge ${status.toLowerCase()}">${labels[status]}</span>`;}
function displayDate(value){return value.split('-').reverse().join('/');}
function customerDisplayName(customer){return customer.responsibleName?`${customer.responsibleName} - ${customer.name}`:customer.name;}
function quoteActions(quote){
  const actionButton=(action,label)=>`<button type="button" class="quote-action" data-id="${quote.id}" data-action="${action}">${label}</button>`;
  let actions=actionButton('open','Abrir detalhes')+actionButton('print','Imprimir / Salvar PDF');
  if(quote.status==='RASCUNHO')
    actions+=actionButton('edit','Editar')+actionButton('ENVIADO','Marcar enviado')+actionButton('CANCELADO','Cancelar orçamento');
  if(quote.status==='ENVIADO')
    actions+=actionButton('REJEITADO','Rejeitar')+actionButton('APROVADO','Aprovar')+actionButton('CANCELADO','Cancelar orçamento');
  return `<details class="quote-actions"><summary class="secondary">Opções</summary><div class="quote-actions-menu">${actions}</div></details>`;
}
function render(){
  const search=$('#search').value.toLocaleLowerCase('pt-BR');
  const filtered=quotes.filter(q=>(q.title+' '+q.customerName).toLocaleLowerCase('pt-BR').includes(search));
  $('#quotes-body').innerHTML=filtered.map(q=>`<tr><td><input class="select-checkbox quote-checkbox" type="checkbox" aria-label="Selecionar orçamento ${q.id}" data-id="${q.id}" ${selectedQuoteIds.has(q.id)?'checked':''}></td><td><div class="row-title">${esc(q.title)}</div><div class="code">ORÇ-${String(q.id).padStart(4,'0')}</div></td><td>${esc(q.customerName)}</td><td>${displayDate(q.validUntil)}</td><td>${badge(q.status)}</td><td class="right">${money(q.total)}</td><td>${quoteActions(q)}</td></tr>`).join('');
  const visibleIds=filtered.map(q=>q.id);
  const selectedVisible=visibleIds.filter(id=>selectedQuoteIds.has(id)).length;
  const selectAll=$('#select-all-quotes');
  selectAll.checked=visibleIds.length>0&&selectedVisible===visibleIds.length;
  selectAll.indeterminate=selectedVisible>0&&selectedVisible<visibleIds.length;
  updateBulkActions();
  $('#quotes-empty').hidden=filtered.length>0;
  $('#quotes-empty').textContent=quotes.length?'Nenhum orçamento corresponde à busca.':'Nenhum orçamento por aqui. Cadastre um cliente e crie sua primeira proposta.';
  $('#customers-body').innerHTML=customers.map(c=>`<tr><td class="row-title">${esc(customerDisplayName(c))}</td><td>${esc(c.cnpj||'—')}</td><td>${esc(c.address||'—')}</td><td>${esc(c.email||'—')}</td><td><button class="link edit-customer" data-id="${c.id}">Editar</button></td></tr>`).join('');
  $('#customers-empty').hidden=customers.length>0;
  const productionItems=quotes.filter(q=>q.status==='APROVADO').flatMap(q=>q.items
    .filter(item=>item.status==='EM_PRODUCAO')
    .map(item=>({quote:q,item})));
  const completedItems=quotes.filter(q=>q.status==='APROVADO').flatMap(q=>q.items
    .filter(item=>item.status==='CONCLUIDO')
    .map(item=>({quote:q,item})));
  const productionRow=({quote,item},completed)=>`<tr><td>${item.imageData?`<img class="production-image" src="${esc(item.imageData)}" alt="Imagem de ${esc(item.description)}">`:''}<span class="row-title">${esc(item.description)}</span></td><td><button class="link open-quote" data-id="${quote.id}">${esc(quote.title)}</button><div class="code">ORÇ-${String(quote.id).padStart(4,'0')}</div></td><td>${esc(quote.customerName)}</td><td>${Number(item.quantity).toLocaleString('pt-BR')}</td><td><button class="link production-status" data-quote-id="${quote.id}" data-item-id="${item.id}" data-status="${completed?'EM_PRODUCAO':'CONCLUIDO'}">${completed?'Reabrir produção':'Marcar concluído'}</button></td></tr>`;
  $('#production-body').innerHTML=productionItems.map(item=>productionRow(item,false)).join('');
  $('#completed-body').innerHTML=completedItems.map(item=>productionRow(item,true)).join('');
  $('#production-count').textContent=productionItems.length;
  $('#completed-count').textContent=completedItems.length;
  $('#production-empty').hidden=productionItems.length>0;
  $('#completed-empty').hidden=completedItems.length>0;
  $('#metric-count').textContent=quotes.length;
  $('#metric-pending').textContent=quotes.filter(q=>q.status==='ENVIADO').length;
  $('#metric-value').textContent=money(quotes.filter(q=>q.status==='APROVADO').reduce((n,q)=>n+Number(q.total),0));
}
function updateBulkActions(){
  const selected=quotes.filter(quote=>selectedQuoteIds.has(quote.id));
  const actions=$('#bulk-actions');
  actions.hidden=selected.length===0;
  $('#selected-count').textContent=`${selected.length} orçamento(s) selecionado(s)`;
  const allowed={
    ENVIADO:status=>status==='RASCUNHO',
    CANCELADO:status=>['RASCUNHO','ENVIADO'].includes(status),
    REJEITADO:status=>status==='ENVIADO',
    APROVADO:status=>status==='ENVIADO'
  };
  document.querySelectorAll('.bulk-status').forEach(button=>{
    const canApply=selected.length>0&&selected.every(quote=>allowed[button.dataset.status](quote.status));
    button.hidden=!canApply;
    button.disabled=false;
  });
}
async function refresh(){[quotes,customers]=await Promise.all([api('/quotes'),api('/customers')]);render();}
function showTab(name){for(const part of ['quotes','production','customers']){$('#'+part+'-panel').hidden=part!==name;$('#tab-'+part).classList.toggle('active',part===name);$('#tab-'+part).setAttribute('aria-pressed',String(part===name));}}
function cleanDialog(el){$('.form-error',el).textContent='';}
document.querySelectorAll('.close').forEach(button=>button.addEventListener('click',()=>button.closest('dialog').close()));
$('#tab-quotes').onclick=()=>showTab('quotes');$('#tab-production').onclick=()=>showTab('production');$('#tab-customers').onclick=()=>showTab('customers');$('#search').oninput=render;
$('#select-all-quotes').onchange=event=>{
  const search=$('#search').value.toLocaleLowerCase('pt-BR');
  quotes.filter(q=>(q.title+' '+q.customerName).toLocaleLowerCase('pt-BR').includes(search))
    .forEach(quote=>event.currentTarget.checked?selectedQuoteIds.add(quote.id):selectedQuoteIds.delete(quote.id));
  render();
};
$('#quotes-body').onchange=event=>{
  const checkbox=event.target.closest('.quote-checkbox');
  if(!checkbox)return;
  const id=Number(checkbox.dataset.id);
  if(checkbox.checked)selectedQuoteIds.add(id);else selectedQuoteIds.delete(id);
  updateBulkActions();
  const visible=[...$('#quotes-body').querySelectorAll('.quote-checkbox')];
  const selectedVisible=visible.filter(item=>item.checked).length;
  $('#select-all-quotes').checked=visible.length>0&&selectedVisible===visible.length;
  $('#select-all-quotes').indeterminate=selectedVisible>0&&selectedVisible<visible.length;
};
document.querySelectorAll('.bulk-status').forEach(button=>button.addEventListener('click',async()=>{
  const selected=quotes.filter(quote=>selectedQuoteIds.has(quote.id));
  if(!selected.length)return;
  const target=button.dataset.status;
  if(!window.confirm(`Alterar ${selected.length} orçamento(s) para ${labels[target]}?`))return;
  const buttons=[...document.querySelectorAll('.bulk-status')];
  buttons.forEach(item=>item.disabled=true);
  try{
    await api('/quotes/status','PATCH',{
      status:target,
      quotes:selected.map(quote=>({id:quote.id,version:quote.version}))
    });
    selectedQuoteIds.clear();
    await refresh();
    notice(`${selected.length} orçamento(s) atualizado(s) para ${labels[target]}.`);
  }catch(error){
    notice(error.message,true);
    updateBulkActions();
  }
}));
function openCustomer(customer=null){
  const dialog=$('#customer-dialog'),form=$('#customer-form');form.reset();cleanDialog(dialog);editingCustomer=customer;
  $('#customer-form-title').textContent=customer?'Editar cliente':'Novo cliente';
  form.elements.name.value=customer?.name||'';form.elements.responsibleName.value=customer?.responsibleName||'';
  form.elements.cnpj.value=customer?.cnpj||'';form.elements.address.value=customer?.address||'';
  form.elements.email.value=customer?.email||'';
  dialog.showModal();
}
$('#new-customer').onclick = () => openCustomer();
$('#customer-form').onsubmit=async event=>{
  event.preventDefault();const form=event.currentTarget,button=$('button[type=submit]',form);button.disabled=true;
  try{
    await api(editingCustomer?'/customers/'+editingCustomer.id:'/customers',editingCustomer?'PUT':'POST',{
      name:form.elements.name.value,
      responsibleName:form.elements.responsibleName.value,
      cnpj:form.elements.cnpj.value,
      address:form.elements.address.value,
      email:form.elements.email.value
    });
    $('#customer-dialog').close();await refresh();notice(editingCustomer?'Cadastro do cliente atualizado.':'Cliente cadastrado. Você já pode criar um orçamento.');
  }
  catch(error){$('.form-error',form).textContent=error.message;}finally{button.disabled=false;}
};
$('#customers-body').onclick=event=>{
  const button=event.target.closest('.edit-customer');
  if(button)openCustomer(customers.find(customer=>customer.id===Number(button.dataset.id)));
};
// Soma monetária estimada em centavos inteiros. O servidor recalcula tudo ao salvar.
function scaled(value,places){
  const s=String(value).trim().replace(',','.');if(!/^\d+(?:\.\d*)?$/.test(s))return 0n;
  const [whole,fraction='']=s.split('.');return BigInt(whole||'0')*10n**BigInt(places)+BigInt((fraction+'0'.repeat(places)).slice(0,places)||'0');
}
function previewTotal(){
  let cents=0n;document.querySelectorAll('#items-body tr').forEach(row=>{cents+=(scaled($('.quantity',row).value,3)*scaled($('.price',row).value,2)+500n)/1000n;});
  cents-=scaled($('#quote-form').elements.discount.value,2);$('#draft-total').textContent=money(Number(cents)/100);
}
function addItem(item={description:'',quantity:1,unitPrice:0,imageData:''}){
  if($('#items-body').children.length>=50)return;
  const row=document.createElement('tr');row.innerHTML=`<td><input class="description" aria-label="Descrição do item" maxlength="200" required value="${esc(item.description)}"></td><td><input class="quantity" aria-label="Quantidade" type="number" min="0.001" max="10000" step="0.001" required value="${esc(item.quantity)}"></td><td><input class="price" aria-label="Valor unitário" type="number" min="0" max="1000000" step="0.01" required value="${esc(item.unitPrice)}"></td><td><input class="item-image" aria-label="Imagem do item" type="file" accept="image/png,image/jpeg,image/webp"><img class="item-preview" alt="Prévia da imagem do item" ${item.imageData?'src="'+esc(item.imageData)+'"':'hidden'}><button type="button" class="link remove-image" ${item.imageData?'':'hidden'}>Remover imagem</button><input class="image-data" type="hidden" value="${esc(item.imageData||'')}"></td><td><button type="button" class="icon remove-item" aria-label="Remover item">×</button></td>`;
  row.imageLoad=Promise.resolve();
  $('.item-image',row).onchange=()=>{
    const file=$('.item-image',row).files[0];if(!file)return;
    if(!['image/png','image/jpeg','image/webp'].includes(file.type)){notice('Escolha uma imagem PNG, JPEG ou WebP.',true);$('.item-image',row).value='';return;}
    if(file.size>500000){notice('Cada imagem deve ter no máximo 500 KB.',true);$('.item-image',row).value='';return;}
    const reader=new FileReader();
    row.imageLoad=new Promise(resolve=>{
      reader.onload=()=>{$('.image-data',row).value=reader.result;$('.item-preview',row).src=reader.result;$('.item-preview',row).hidden=false;$('.remove-image',row).hidden=false;resolve();};
      reader.onerror=()=>{notice('Não foi possível ler a imagem selecionada.',true);resolve(false);};
    });
    reader.readAsDataURL(file);
  };
  $('.remove-image',row).onclick=()=>{$('.image-data',row).value='';$('.item-image',row).value='';$('.item-preview',row).removeAttribute('src');$('.item-preview',row).hidden=true;$('.remove-image',row).hidden=true;};
  row.addEventListener('input',previewTotal);$('.remove-item',row).onclick=()=>{if($('#items-body').children.length===1){notice('Mantenha pelo menos um item.',true);return;}row.remove();previewTotal();};
  $('#items-body').append(row);previewTotal();
}
function openQuote(quote=null){
  if(!customers.length){showTab('customers');notice('Cadastre um cliente antes de criar o orçamento.');openCustomer();return;}
  const form=$('#quote-form');form.reset();editing=quote;cleanDialog($('#quote-dialog'));
  $('#quote-form-title').textContent=quote?'Editar rascunho':'Novo orçamento';
  form.elements.customerId.innerHTML='<option value="">Selecione um cliente</option>'+customers.map(c=>`<option value="${c.id}">${esc(customerDisplayName(c))}</option>`).join('');
  form.elements.title.value=quote?.title||'';form.elements.customerId.value=quote?.customerId||'';
  form.elements.description.value=quote?.description||'';
  const future=new Date();future.setDate(future.getDate()+15);
  const localDate=`${future.getFullYear()}-${String(future.getMonth()+1).padStart(2,'0')}-${String(future.getDate()).padStart(2,'0')}`;
  form.elements.validUntil.value=quote?.validUntil||localDate;form.elements.discount.value=quote?.discount||0;
  $('#items-body').innerHTML='';(quote?.items||[{description:'',quantity:1,unitPrice:0}]).forEach(addItem);
  $('#quote-dialog').showModal();
}
$('#new-quote').onclick=()=>openQuote();$('#add-item').onclick=()=>addItem();$('#quote-form').elements.discount.oninput=previewTotal;
$('#quote-form').onsubmit=async event=>{
  event.preventDefault();const form=event.currentTarget,button=$('button[type=submit]',form);button.disabled=true;
  const imageResults=await Promise.all([...$('#items-body').children].map(row=>row.imageLoad));
  if(imageResults.includes(false)){$('.form-error',form).textContent='Não foi possível ler uma das imagens selecionadas.';button.disabled=false;return;}
  const body={customerId:Number(form.elements.customerId.value),title:form.elements.title.value,validUntil:form.elements.validUntil.value,discount:form.elements.discount.value,version:editing?.version||0,
    description:form.elements.description.value,
    items:[...$('#items-body').children].map(row=>({description:$('.description',row).value,quantity:$('.quantity',row).value,unitPrice:$('.price',row).value,imageData:$('.image-data',row).value||null}))};
  try{const saved=await api(editing?'/quotes/'+editing.id:'/quotes',editing?'PUT':'POST',body);$('#quote-dialog').close();await refresh();notice('Orçamento salvo. Os valores foram conferidos pelo servidor.');await openDetail(saved.id);}
  catch(error){$('.form-error',form).textContent=error.message;}finally{button.disabled=false;}
};
async function openDetail(id){
  detail=await api('/quotes/'+id);const q=detail;cleanDialog($('#detail-dialog'));
  $('#print-area').innerHTML=`<p class="eyebrow">ORÇAMENTO · ORÇ-${String(q.id).padStart(4,'0')}</p><h2 class="detail-title">${esc(q.title)}</h2>${badge(q.status)}<div class="detail-meta"><div><span>Cliente</span><p>${esc(q.customerName)}</p></div><div><span>Válido até</span><p>${displayDate(q.validUntil)}</p></div></div>${q.description?`<section class="quote-description"><h3>Descrição do orçamento</h3><p>${esc(q.description).replace(/\n/g,'<br>')}</p></section>`:''}<div class="table-scroll"><table><thead><tr><th>Descrição</th><th class="right">Qtd.</th><th class="right">Unitário</th><th class="right">Subtotal</th><th>Status</th>${q.status==='APROVADO'?'<th class="no-print">Ações</th>':''}</tr></thead><tbody>${q.items.map(i=>`<tr><td>${i.imageData?`<img class="detail-item-image" src="${esc(i.imageData)}" alt="Imagem de ${esc(i.description)}">`:''}${esc(i.description)}</td><td class="right">${Number(i.quantity).toLocaleString('pt-BR')}</td><td class="right">${money(i.unitPrice)}</td><td class="right">${money(Number((scaled(i.quantity,3)*scaled(i.unitPrice,2)+500n)/1000n)/100)}</td><td><span class="badge item-${i.status.toLowerCase()}">${itemStatusLabels[i.status]}</span></td>${q.status==='APROVADO'?`<td class="no-print"><button class="link item-status" data-id="${i.id}" data-status="${i.status==='PENDENTE'?'EM_PRODUCAO':i.status==='EM_PRODUCAO'?'CONCLUIDO':'EM_PRODUCAO'}">${i.status==='PENDENTE'?'Iniciar produção':i.status==='CONCLUIDO'?'Reabrir produção':'Marcar concluído'}</button></td>`:''}</tr>`).join('')}</tbody></table></div><div class="totals"><p><span>Subtotal</span><span>${money(q.subtotal)}</span></p><p><span>Desconto</span><span>− ${money(q.discount)}</span></p><p class="grand"><span>Total</span><span>${money(q.total)}</span></p></div>`;
  const actions=$('#detail-actions');actions.innerHTML='';
  const add=(label,fn,cls='secondary')=>{const b=document.createElement('button');b.textContent=label;b.className=cls;b.onclick=fn;actions.append(b);};
  add('Imprimir / Salvar PDF',()=>window.print());
  if(q.status==='RASCUNHO'){add('Editar',()=>{$('#detail-dialog').close();openQuote(q);});add('Marcar enviado',()=>transition('ENVIADO'),'primary');}
  if(q.status==='ENVIADO'){add('Rejeitar',()=>transition('REJEITADO'));add('Aprovar',()=>transition('APROVADO'),'primary');}
  if(['RASCUNHO','ENVIADO'].includes(q.status))add('Cancelar orçamento',()=>transition('CANCELADO'));
  if(!$('#detail-dialog').open)$('#detail-dialog').showModal();
}
async function transition(status){
  if(!window.confirm(`Alterar para ${labels[status]}? O conteúdo ficará bloqueado para edição.`))return;
  const buttons=[...$('#detail-actions').querySelectorAll('button')];buttons.forEach(b=>b.disabled=true);
  try{await api('/quotes/'+detail.id+'/status','PATCH',{status,version:detail.version});await refresh();await openDetail(detail.id);}
  catch(error){$('.form-error',$('#detail-dialog')).textContent=error.message;}
  finally{buttons.forEach(b=>b.disabled=false);}
}
$('#print-area').onclick=async event=>{
  const button=event.target.closest('.item-status');if(!button)return;
  button.disabled=true;
  try{await api(`/quotes/${detail.id}/items/${button.dataset.id}/status`,'PATCH',{status:button.dataset.status,version:detail.version});await refresh();await openDetail(detail.id);}
  catch(error){$('.form-error',$('#detail-dialog')).textContent=error.message;button.disabled=false;}
};
$('#production-panel').onclick=async event=>{
  const statusButton=event.target.closest('.production-status');
  if(statusButton){
    statusButton.disabled=true;
    try{
      const quote=quotes.find(item=>item.id===Number(statusButton.dataset.quoteId));
      if(!quote)throw new Error('Orçamento não encontrado. Atualize a página.');
      await api(`/quotes/${quote.id}/items/${statusButton.dataset.itemId}/status`,'PATCH',{status:statusButton.dataset.status,version:quote.version});
      await refresh();
      notice('Andamento do item atualizado.');
    }catch(error){notice(error.message,true);statusButton.disabled=false;}
    return;
  }
  const quoteButton=event.target.closest('.open-quote');
  if(quoteButton)openDetail(quoteButton.dataset.id).catch(error=>notice(error.message,true));
};
$('#quotes-body').onclick=async event=>{
  const actionButton=event.target.closest('.quote-action');
  if(actionButton){
    const quote=quotes.find(item=>item.id===Number(actionButton.dataset.id));
    if(!quote)return;
    const action=actionButton.dataset.action;
    actionButton.closest('.quote-actions').open=false;
    try{
      if(action==='edit'){openQuote(quote);return;}
      await openDetail(quote.id);
      if(action==='print'){window.print();return;}
      if(action!=='open')await transition(action);
    }catch(error){notice(error.message,true);}
    return;
  }
  const openButton=event.target.closest('.open-quote');
  if(openButton)openDetail(openButton.dataset.id).catch(error=>notice(error.message,true));
};
refresh().catch(error=>notice('Não foi possível carregar os dados. '+error.message,true));
