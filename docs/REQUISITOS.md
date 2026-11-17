# Requisitos do OrçaFácil

## Problema e objetivo

Preparar orçamentos manualmente exige repetir dados e conferir cálculos a cada inclusão de item. O projeto centraliza clientes, itens e propostas em uma aplicação web. A redução de tempo é uma hipótese a avaliar com usuários, não um resultado quantitativo comprovado pelos registros do repositório.

O usuário-alvo é uma pessoa responsável por orçamentos de pequenos serviços. A versão implementada permite concluir um orçamento completo, persistido, consultável e imprimível.

| ID | Requisito | Critério verificável |
|---|---|---|
| RF01 | Cadastrar cliente | Nome obrigatório, e-mail opcional validado, responsável, CNPJ e endereço opcionais |
| RF10 | Editar cliente | Atualizar dados do cliente e exibir “Responsável - Empresa” quando existir responsável |
| RF02 | Criar orçamento | Exigir cliente existente, título, validade e pelo menos um item |
| RF03 | Gerenciar itens | Permitir adicionar/remover até 50 linhas; exigir descrição, quantidade positiva e preço não negativo |
| RF04 | Calcular total | Exemplo 2×100 + 3×50 − 25 deve resultar em R$ 325,00 |
| RF05 | Persistir | Fechar e abrir a aplicação preserva os dados no modo padrão |
| RF06 | Editar rascunho | Atualizar itens sem duplicação e recusar versão antiga |
| RF07 | Acompanhar situação | Só permitir as transições do diagrama de estados |
| RF08 | Buscar | Filtrar orçamentos por título ou cliente |
| RF09 | Imprimir | Abrir visualização sem botões de navegação e permitir salvar em PDF pelo navegador |
| RF11 | Descrever proposta | Salvar e exibir descrição complementar de até 2.000 caracteres |
| RF12 | Anexar imagem | Salvar e exibir uma imagem PNG, JPEG ou WebP de até 500 KB em cada item |
| RF13 | Acompanhar produção | Atualizar individualmente o item aprovado entre pendente, em produção e concluído |
| RF14 | Consultar produção | Exibir em abas separadas os itens em produção e os concluídos, com acesso ao orçamento e atualização de estado |
| RNF01 | Organização | Separar interface, controller, service e repository |
| RNF02 | Qualidade | Compilação e testes devem passar antes de integrar uma mudança |
| RNF03 | Reprodutibilidade | Instalação documentada e dependências declaradas no Maven |

## Regras de negócio

1. Cada orçamento pertence a um cliente e contém de 1 a 50 itens.
2. Quantidade: maior que zero, até 10.000, com no máximo três casas decimais.
3. Valor unitário: de zero a R$ 1.000.000,00, com no máximo duas casas decimais.
4. Arredondar cada quantidade × preço para centavos usando HALF_UP; depois somar as linhas.
5. Desconto é um valor absoluto em reais entre zero e o subtotal.
6. Somente rascunhos podem alterar conteúdo. Enviado, aprovado, rejeitado e cancelado ficam bloqueados.
7. Uma alteração deve apresentar a versão que o usuário consultou. Se outra alteração ocorreu, retornar 409.
8. A validade é uma data informativa; não há expiração automática nesta versão.
9. Não excluir propostas: registrar cancelamento preserva seu histórico de conteúdo. Ainda não existe trilha com data e autor de cada mudança de situação.

## Fora do escopo implementado

Estoque, nota fiscal, cobrança, envio automático de e-mail, múltiplas empresas, autenticação e infraestrutura de produção com persistência durável na nuvem. A exclusão de clientes não está incluída.
