# OrçaFácil

O OrçaFácil foi desenvolvido como projeto acadêmico para aplicar conhecimentos de programação, banco de dados e desenvolvimento web. 
Durante sua construção, utilizei ferramentas de inteligência artificial como apoio à implementação, à compreensão do código e à resolução de problemas. 
Realizei adaptações, testes funcionais e avaliações com usuários para analisar a aplicação e identificar oportunidades de melhoria.

## Funcionalidades

- Cadastro, listagem e edição de clientes, com responsável, CNPJ, endereço e e-mail opcionais.
- Criação de orçamentos com até 50 itens e quantidades fracionadas.
- Descrição complementar do orçamento e imagem opcional por item (PNG, JPEG ou WebP, até 500 KB).
- Acompanhamento do andamento de cada item aprovado: pendente, em produção ou concluído.
- Aba de produção com listas de itens em andamento e finalizados, com ações para atualizar o estado.
- Exportação Excel (.xlsx) independente para orçamentos, clientes e produção.
- Cálculo no servidor com `BigDecimal`, arredondamento por item e desconto em reais.
- Consulta e busca por título ou cliente na interface.
- Edição de rascunhos com controle de versão para evitar sobrescrever alterações.
- Fluxo de situação: rascunho, enviado, aprovado, rejeitado e cancelado.
- Seleção múltipla de orçamentos para envio, aprovação, rejeição ou cancelamento em lote, com validação atômica.
- Impressão e opção de salvar em PDF pelo navegador.
- API REST com validação e erros HTTP; banco relacional em arquivo nos modos padrão e `lan`.
- Testes unitários, testes HTTP integrados ao banco e workflow do GitHub Actions.

## Tecnologias

Java 17, Spring Boot 4.0.8, Spring Web MVC, Bean Validation, JDBC, H2, HTML, CSS, JavaScript, Maven e JUnit. O SQL está explícito em `src/main/resources/schema.sql`, para tornar a modelagem e a persistência visíveis no trabalho.

Nos modos padrão e `lan`, o H2 funciona em arquivo, sem exigir instalar outro servidor de banco. Uma migração para PostgreSQL é uma evolução possível; não há suporte a PostgreSQL implementado nesta versão.

## Executar

Instale um **JDK 17 ou superior** e **Maven 3.6.3 ou superior**; ambos precisam estar no PATH. A primeira compilação precisa de internet para baixar dependências. Com o terminal aberto na pasta que contém `pom.xml`:

```bash
java -version
javac -version
mvn -version
mvn spring-boot:run
```

Abra **http://localhost:8080**. Use `Ctrl+C` no terminal para encerrar. No Windows, os mesmos comandos funcionam no PowerShell. Se a porta estiver ocupada:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

O primeiro acesso está vazio. Cadastre um cliente e crie o orçamento pela interface; os exemplos fictícios não são carregados automaticamente.

### Exemplo para conferir o fluxo

1. Cadastre “Cliente Demonstração”, com e-mail `cliente@example.com`.
2. Crie “Manutenção de computadores” com 2 unidades de R$ 100,00 e 3 unidades de R$ 50,00.
3. Aplique R$ 25,00 de desconto: subtotal **R$ 350,00**, total **R$ 325,00**.
4. No modo padrão ou `lan`, salve, encerre e reinicie a aplicação: os dados devem continuar disponíveis.
5. Edite enquanto estiver em rascunho. Marque como enviado e depois aprovado.
6. Abra “Imprimir / Salvar PDF”. O recurso usa o diálogo de impressão do navegador.

“Marcar enviado” registra a situação; o programa não envia e-mails ou mensagens. A validade é informativa nesta versão e não muda a situação automaticamente. Os estados dos itens podem ser atualizados após a aprovação do orçamento.

## Testar e empacotar

```bash
mvn verify
java -jar target/orcafacil-0.1.0.jar
```

Os testes HTTP usam banco **em memória** e não modificam o arquivo de dados local. Consulte [o registro de validação](docs/VALIDACAO.md) para conhecer os testes registrados e os limites de cada verificação.

## Organização

| Caminho | Responsabilidade |
|---|---|
| `src/main/java/.../controller` | Endpoints HTTP e tradução de erros |
| `src/main/java/.../dto` | Dados de entrada e validações |
| `src/main/java/.../service` | Regras e transações |
| `src/main/java/.../model` | Representação dos dados e situações |
| `src/main/java/.../repository` | SQL e acesso ao banco |
| `src/main/resources/static` | Interface que consome a API |
| `src/main/resources/schema.sql` | Tabelas, restrições e índices |
| `src/test` | Testes de regras e integração |
| `.github/workflows/ci.yml` | Compilação e testes em pushes e pull requests |
| `docs` | Arquitetura, requisitos, API, planejamento e contexto acadêmico |

## Documentação

- [Requisitos e critérios de aceitação](docs/REQUISITOS.md)
- [Arquitetura e modelo de dados](docs/ARQUITETURA.md)
- [API e exemplos de requisição](docs/API.md)
- [Planejamento inicial e escopo entregue](docs/PLANO.md)
- [Repositório e manutenção no GitHub](docs/GITHUB.md)
- [Projeto desenvolvido no PIT II](docs/PIT.md)
- [Ficha de teste com usuário](docs/FICHA_TESTE.md)
- [Validação e limitações](docs/VALIDACAO.md)

## Escopo de uso

A aplicação é uma demonstração acadêmica, sem autenticação e sem separação de usuários. O código está publicado em [GustavoZandonade/orcafacil-publico](https://github.com/GustavoZandonade/orcafacil-publico), com `master` como branch principal. GitHub Pages não executa este backend Java.

| Modo | Acesso | Armazenamento |
|---|---|---|
| Padrão | `127.0.0.1:8080` | H2 em arquivo |
| `lan` | IP definido por `ORCAFACIL_HOST` ou argumento; loopback por padrão | H2 em arquivo no servidor |
| `cloud` | `0.0.0.0`, porta `PORT` ou 10000 | H2 em memória; dados perdidos ao encerrar o processo |

O [guia de rede local](LEIA-ME-REDE-LOCAL.md) descreve o acesso entre dispositivos. Para iniciar o perfil de nuvem:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=cloud
```

Use dados fictícios nas demonstrações. O perfil de nuvem não implementa autenticação nem armazenamento durável.

Nos modos padrão e `lan`, os dados ficam em `data/orcafacil.mv.db`, dentro da pasta do projeto, ignorada pelo Git junto de `target/` e arquivos `.env`. Esse arquivo contém os clientes, orçamentos, itens e imagens. No PowerShell, confira o tamanho com `(Get-Item .\data\orcafacil.mv.db).Length`; para exibir em MB, use `(Get-Item .\data\orcafacil.mv.db).Length / 1MB`. Pare a aplicação antes de copiar ou substituir a base. A inicialização aplica mudanças aditivas do esquema (incluindo descrição, imagens e situação dos itens) às bases H2 existentes. Faça uma cópia da pasta `data/` antes de atualizar uma base importante.

A interface inclui os fluxos de clientes, orçamentos e produção. O [registro de validação](docs/VALIDACAO.md) distingue testes automatizados, verificações históricas e cobertura visual. Após alterações, execute `mvn verify` e confira os fluxos da interface afetados.

## Referências técnicas

- [Spring Boot — requisitos da versão 4.0](https://docs.spring.io/spring-boot/4.0/system-requirements.html)
- [GitHub — adicionar código local](https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github)

Versões fixadas no `pom.xml`. Ao atualizar dependências, execute novamente os testes.
