# API REST

Base local: `http://localhost:8080/api`. Requisições de escrita usam `Content-Type: application/json`.

| Método | Rota | Resultado |
|---|---|---|
| GET | `/customers` | Lista clientes |
| POST | `/customers` | Cadastra cliente; HTTP 201 |
| PUT | `/customers/{id}` | Atualiza nome, responsável, CNPJ, endereço e e-mail do cliente |
| GET | `/quotes` | Lista orçamentos completos |
| GET | `/quotes/{id}` | Consulta um orçamento |
| POST | `/quotes` | Cria rascunho; HTTP 201 |
| PUT | `/quotes/{id}` | Substitui conteúdo de rascunho |
| PATCH | `/quotes/{id}/status` | Altera situação |
| PATCH | `/quotes/status` | Altera atomicamente a situação de até 100 orçamentos |
| PATCH | `/quotes/{quoteId}/items/{itemId}/status` | Atualiza o andamento de um item aprovado |
| GET | `/export/quotes.xlsx` | Baixa orçamentos e itens em planilha Excel |
| GET | `/export/customers.xlsx` | Baixa clientes em planilha Excel |
| GET | `/export/production.xlsx` | Baixa itens em produção e concluídos em planilha Excel |

O nome atualizado do cliente também passa a aparecer nos orçamentos associados.

## Cadastrar cliente

```json
{
  "name":"Empresa Demonstração",
  "responsibleName":"Maria Silva",
  "cnpj":"12.345.678/0001-90",
  "address":"Rua Exemplo, 123 - Centro",
  "email":"cliente@example.com"
}
```

Guarde o `id` retornado; não suponha que será sempre 1.
Os campos `responsibleName`, `cnpj`, `address` e `email` são opcionais. O CNPJ aceita 14 dígitos, com ou sem pontuação. Quando há responsável, propostas e seletores mostram `Responsável - Empresa`.

## Criar orçamento

Substitua `customerId` pelo cliente criado:

```json
{
  "customerId": 1,
  "title": "Manutenção de computadores",
  "description": "Troca de peças e revisão geral do equipamento.",
  "validUntil": "2026-11-30",
  "discount": 25.00,
  "version": 0,
  "items": [
    {"description":"Manutenção", "quantity":2, "unitPrice":100.00, "imageData":"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l9sAAAAASUVORK5CYII="},
    {"description":"Instalação", "quantity":3, "unitPrice":50.00}
  ]
}
```

A resposta inclui `id`, `status`, `version`, `subtotal` e `total`. Para esse exemplo, subtotal = 350 e total = 325. O servidor calcula esses campos; não os envie como entrada.

`description` é opcional (até 2.000 caracteres). `imageData` é opcional e recebe uma imagem PNG, JPEG ou WebP codificada como data URL, de até 500 KB decodificados.

## Editar

Envie o mesmo formato a `PUT /quotes/{id}`, usando a `version` mais recente. Essa operação substitui todos os itens. Só funciona em `RASCUNHO`. A nova versão é retornada na resposta.

## Alterar situação

```json
{"status":"ENVIADO","version":0}
```

Use a versão retornada pela última leitura ou alteração. Para aprovar, o orçamento precisa estar `ENVIADO`.

Para alterar vários, envie a situação desejada e o ID/versão atual de cada orçamento. Se algum não existir, estiver desatualizado ou não permitir a transição, nenhum será alterado:

```json
{
  "status": "APROVADO",
  "quotes": [{"id": 12, "version": 1}, {"id": 13, "version": 2}]
}
```

Na tela de orçamentos, marque as caixas dos pedidos; serão mostradas somente as ações válidas para todos os selecionados.

## Atualizar andamento de item

Os itens começam como `PENDENTE`. Após a aprovação do orçamento, atualize cada item usando a versão atual do orçamento:

```json
{"status":"EM_PRODUCAO","version":2}
```

Os estados aceitos são `PENDENTE`, `EM_PRODUCAO` e `CONCLUIDO`. Cada alteração incrementa a versão do orçamento.

## Exportar para Excel

Os três endpoints de exportação retornam arquivos `.xlsx` prontos para abrir no Excel. A planilha de orçamentos contém uma aba com os totais por orçamento e outra com os itens e suas imagens. Imagens PNG/JPEG são incorporadas; imagens WebP ficam disponíveis em partes Base64 na aba `Imagens WebP (base64)`. A planilha de produção reúne os itens em andamento e concluídos.

## Erros

- 400: formato ou validação inválidos; desconto superior ao subtotal.
- 404: cliente ou orçamento não encontrado.
- 409: estado incompatível, conflito de versão ou integridade do banco.

```json
{"message":"Revise os campos informados.","errors":["title: não deve estar em branco"]}
```

A redação detalhada das mensagens de validação depende do idioma do ambiente. Utilize o status HTTP para decidir o comportamento do cliente.
