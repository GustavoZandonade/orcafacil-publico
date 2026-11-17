# Repositório e manutenção no GitHub

O código está publicado em [GustavoZandonade/orcafacil-publico](https://github.com/GustavoZandonade/orcafacil-publico). A branch principal é `master`.

## Obter o projeto

```bash
git clone https://github.com/GustavoZandonade/orcafacil-publico.git
cd orcafacil-publico
git switch master
mvn verify
```

Os requisitos de execução estão no [README](../README.md).

## Propor alterações

As mudanças são preparadas em branches e apresentadas em pull requests para `master`. Exemplo de revisão da documentação:

```bash
git switch master
git pull --ff-only origin master
git switch -c docs/atualizar-documentacao
# Após editar os arquivos:
git diff --check
git diff
git add README.md docs/
git diff --cached
git commit -m "docs: atualizar documentação do sistema"
git push -u origin docs/atualizar-documentacao
```

A pull request descreve o problema, o comportamento resultante e a validação realizada. O workflow `.github/workflows/ci.yml` executa `mvn verify` em pushes e pull requests com Java 17. A integração ocorre após a revisão das alterações e dos resultados das verificações.

## Arquivos versionados

O repositório contém código-fonte, configurações, scripts, testes e documentação. O `.gitignore` exclui a base local, arquivos de ambiente, logs e binários compilados. Bancos e backups permanecem fora do repositório; exemplos e capturas públicas usam dados fictícios.

## Código e execução da aplicação

O GitHub hospeda o código. O backend Java depende de um processo servidor; GitHub Pages não executa esta aplicação.

A configuração padrão atende em loopback e usa H2 em arquivo. O perfil `lan` permite acesso pela rede local e mantém o banco no servidor. O perfil `cloud` escuta em `0.0.0.0`, usa a porta `PORT` (padrão 10000) e H2 em memória: seus dados não sobrevivem ao encerramento do processo.

A aplicação não possui autenticação ou separação de usuários. As limitações de cada modo estão no [README](../README.md).

Referência: [documentação oficial do GitHub](https://docs.github.com/en/pull-requests).
