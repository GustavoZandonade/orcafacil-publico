# Registro de validação

Validação em 06/10/2026, no ambiente de preparação. Estes registros correspondem aos testes do código da base, não à execução acadêmica ou às avaliações de colegas pelo estudante.

## Validação histórica de 06/10/2026

Os números abaixo preservam o registro original de preparação; não representam uma nova execução nem a contagem atual da suíte.

### Testes automatizados

- 9 testes unitários de cálculo monetário, descontos e transições.
- 17 testes de integração HTTP com Spring Boot e H2 em memória.
- Resultado: **26 testes, 0 falhas, 0 erros, 0 ignorados**.
- Aplicação empacotada e inicializada a partir do JAR.
- Java 17.0.20 e Maven 3.9.11. O ambiente dispunha apenas de JRE; para a compilação local foi usado o compilador Eclipse via uma configuração temporária do Maven. O pom entregue mantém o compilador padrão e requer um JDK. O workflow do GitHub não foi executado remotamente nesta preparação.

### Verificação da interface na preparação inicial

A sintaxe do JavaScript foi verificada com `node --check`. O servidor respondeu à rota da interface no teste de integração. A conferência visual, o fluxo por cliques e o resultado da impressão não foram concluídos: não havia navegador local utilizável e o navegador remoto não alcançou o servidor deste ambiente. Essa limitação descreve o ambiente daquela validação e não é uma pendência de implementação da interface.

### Persistência em arquivo

Foi iniciado o JAR com um banco H2 em arquivo, criado um orçamento de dois itens com total R$ 325,00, encerrado o processo e reiniciado o mesmo banco. A consulta após reiniciar recuperou os dois itens e o total correto.

## Verificação do repositório em 17/11/2026

O workflow [Verificar aplicação — execução 43989503382](https://github.com/GustavoZandonade/orcafacil/actions/runs/43989503382) concluiu com sucesso para o commit `47ab115615f76a35b4a009288721ded5b5fbb508`. Ele executa `mvn --batch-mode --no-transfer-progress verify` com JDK 17. A execução ocorreu em 16/11/2026 às 21h40, no horário de Brasília.

A revisão documental conferiu os textos com os arquivos de configuração, as rotas, os serviços e a estrutura versionada. Não incluiu uma nova sessão de testes manuais na interface ou impressão. O sucesso da CI não comprova essas verificações nem as avaliações acadêmicas.

## Cobertura e limitações

- A suíte atual está em `src/test`; os números do registro de outubro são históricos.
- `docs/resultado-testes.json` preserva os resultados históricos de 06/10/2026, inclusive a ausência de CI remota naquela preparação.
- Os testes automatizados usam H2 em memória. Nos modos padrão e `lan`, a aplicação usa H2 em arquivo; o perfil `cloud` também usa memória e perde os dados ao encerrar o processo.
- O modelo `FICHA_TESTE.md` é um instrumento de avaliação, não um registro de respostas.
- A aplicação não possui autenticação, separação de usuários ou trilha completa de auditoria.
