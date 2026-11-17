# OrçaFácil no PIT II

## Projeto desenvolvido

O OrçaFácil é o produto prático do Projeto Integrador Transdisciplinar em Sistemas de Informação II. A aplicação reúne front-end, back-end e banco relacional para elaborar e acompanhar orçamentos de serviços.

## Objetivo e resultado

O sistema implementa cadastro e edição de clientes, criação e edição de orçamentos em rascunho, cálculo de valores e descontos, acompanhamento de situações, controle de produção por item, imagens, exportação Excel e impressão pelo navegador.

A solução utiliza Java, Spring Boot, JDBC, H2, HTML, CSS e JavaScript. O código está organizado em camadas de interface, controller, service, model e repository, com validação das entradas e operações transacionais.

## Metodologia

O desenvolvimento utilizou requisitos verificáveis, modelagem relacional, implementação incremental, controle de versão no GitHub e testes unitários e de integração. Ferramentas de inteligência artificial apoiaram a implementação, a compreensão do código e a resolução de problemas, conforme declarado no README.

## Documentação e evidências

- [Requisitos](REQUISITOS.md): escopo e critérios de aceitação.
- [Arquitetura](ARQUITETURA.md): camadas, dados e decisões técnicas.
- [API](API.md): operações disponíveis e exemplos.
- [Planejamento inicial e escopo entregue](PLANO.md): estimativa histórica e funcionalidades implementadas.
- [Validação](VALIDACAO.md): registros de testes e limites das verificações.
- [Ficha de teste](FICHA_TESTE.md): modelo reutilizável para registrar avaliações.

O formulário acadêmico, as avaliações identificadas, as horas efetivamente realizadas e as demais evidências da disciplina são registros separados do código-fonte. O cronograma inicial não comprova horas executadas; o modelo de ficha não representa uma avaliação preenchida.

## Limitações

A aplicação não possui autenticação, envio automático de propostas, emissão fiscal ou controle de estoque. A validade é informativa e o PDF utiliza a impressão do navegador. O perfil `cloud` usa banco em memória, enquanto os perfis padrão e `lan` usam H2 em arquivo. Os detalhes estão no README e no registro de validação.
