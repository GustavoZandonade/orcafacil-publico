package br.com.gustavo.orcafacil.service;
import br.com.gustavo.orcafacil.dto.*;
import br.com.gustavo.orcafacil.model.*;
import br.com.gustavo.orcafacil.repository.QuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
@Service
@Transactional(readOnly=true)
public class QuoteService {
    private final QuoteRepository repository;
    public QuoteService(QuoteRepository repository) { this.repository=repository; }
    public List<Customer> customers() { return repository.customers(); }
    public List<Quote> all() { return repository.all(); }
    public Quote find(long id) { return repository.find(id); }
    @Transactional public Customer createCustomer(CustomerInput input) { return repository.createCustomer(input); }
    @Transactional public Customer updateCustomer(long id,CustomerInput input) {
        return repository.updateCustomer(id,input);
    }
    private void validate(QuoteInput input) {
        repository.customer(input.customerId());
        for(var item:input.items()) validateImage(item.imageData());
        var items=input.items().stream().map(i -> new QuoteItem(i.description(),i.quantity(),i.unitPrice())).toList();
        QuoteRules.total(items,input.discount());
    }
    private void validateImage(String imageData) {
        if(imageData==null || imageData.isEmpty()) return;
        if(!imageData.matches("data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/]+={0,2}"))
            throw new BusinessException(400,"A imagem deve ser PNG, JPEG ou WebP codificada corretamente.");
        try {
            byte[] decoded=Base64.getDecoder().decode(imageData.substring(imageData.indexOf(',')+1));
            if(decoded.length>500_000) throw new BusinessException(400,"Cada imagem deve ter no máximo 500 KB.");
        } catch(IllegalArgumentException ex) {
            throw new BusinessException(400,"A imagem enviada não é válida.");
        }
    }
    @Transactional public Quote create(QuoteInput input) {
        validate(input); return repository.find(repository.create(input));
    }
    @Transactional public Quote update(long id,QuoteInput input) {
        repository.find(id); validate(input); repository.update(id,input); return repository.find(id);
    }
    @Transactional public Quote changeStatus(long id,StatusInput input) {
        var quote=repository.find(id);
        if(quote.version()!=input.version()) throw new BusinessException(409,"Versão desatualizada. Recarregue a página.");
        QuoteRules.checkTransition(quote.status(),input.status());
        repository.changeStatus(id,input); return repository.find(id);
    }
    @Transactional public List<Quote> changeStatuses(BulkStatusInput input) {
        var ids=new HashSet<Long>();
        for(var requested:input.quotes()) {
            if(!ids.add(requested.id()))
                throw new BusinessException(400,"A seleção contém o mesmo orçamento mais de uma vez.");
            var quote=repository.find(requested.id());
            if(quote.version()!=requested.version())
                throw new BusinessException(409,"Um ou mais orçamentos foram alterados. Recarregue a página.");
            QuoteRules.checkTransition(quote.status(),input.status());
        }
        for(var requested:input.quotes())
            repository.changeStatus(requested.id(),new StatusInput(input.status(),requested.version()));
        return input.quotes().stream().map(requested->repository.find(requested.id())).toList();
    }
    @Transactional public Quote changeItemStatus(long quoteId,long itemId,ItemStatusInput input) {
        repository.find(quoteId);
        repository.changeItemStatus(quoteId,itemId,input.status(),input.version());
        return repository.find(quoteId);
    }
}
