# OrçaFácil — execução em rede local

O perfil `lan` e os iniciadores para Windows fazem parte do repositório. Uma única instância Java atende os navegadores da rede; os dados permanecem no banco H2 em arquivo do computador servidor.

## 1. Iniciar no computador servidor

No terminal PowerShell do VS Code, na raiz do projeto:

```powershell
ipconfig
```

Identifique o IPv4 da conexão Wi-Fi ou Ethernet ativa. Os endereços `192.168.15.24` abaixo são exemplos: substitua pelo seu IP real.

```powershell
.\iniciar-rede-local.ps1 -Endereco 192.168.15.24
```

O iniciador `iniciar-rede.cmd` também ativa esse perfil, mas contém o IP fixo `192.168.15.24`; em outras redes, esse endereço precisa corresponder ao servidor. O PowerShell permite informar o IP sem editar o script.

O script PowerShell aceita apenas IPs privados atribuídos ao computador. Executa o Maven na raiz para conservar o caminho do banco, ativa `lan` e desativa explicitamente o console H2. Não instala programas, não altera o firewall e não muda permanentemente variáveis de ambiente.

Se a política do Windows bloquear scripts, você pode iniciar diretamente, sem alterar essa política:

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=lan --server.address=192.168.15.24 --server.port=8080 --spring.h2.console.enabled=false --spring.h2.console.settings.web-allow-others=false"
```

Para esse comando direto, permaneça na pasta do `pom.xml`. Requer o JDK 17 ou superior compatível e o Maven, conforme os requisitos do README.

## 2. Firewall no Windows

Na sua rede doméstica de confiança, confira se o perfil da conexão é **Privado**. Não reclassifique redes públicas ou corporativas sem autorização.

Se você ainda não criou a regra de acesso, abra separadamente o PowerShell como administrador e execute uma vez, usando seu IP real:

```powershell
New-NetFirewallRule -DisplayName "OrcaFacil LAN" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8080 -LocalAddress 192.168.15.24 -RemoteAddress LocalSubnet -Profile Private
```

Se já criou a regra com esse IP, não precisa duplicá-la. A regra restringe essa permissão à sub-rede local e ao perfil privado; regras mais amplas preexistentes precisam ser avaliadas separadamente. Não desligue o firewall nem configure encaminhamento de portas no roteador.

Para remover as regras com esse nome quando não precisar mais:

```powershell
Get-NetFirewallRule -DisplayName "OrcaFacil LAN" | Remove-NetFirewallRule
```

Se o IP do servidor mudar, ajuste o comando de inicialização e a regra. Uma reserva de DHCP no roteador pode manter o endereço estável.

## 3. Usar e verificar em dois aparelhos

1. Aguarde a mensagem de inicialização concluída do Spring Boot.
2. No próprio servidor, abra `http://192.168.15.24:8080` com seu IP real.
3. Em outro computador da mesma rede, abra o mesmo endereço. Não use `localhost` no segundo computador.
4. Cadastre um cliente fictício em um aparelho e atualize a página no outro: o cadastro deve aparecer.
5. Cadastre um orçamento e confirme a consulta no segundo aparelho.
6. Pare a aplicação com Ctrl+C, inicie novamente pelo mesmo comando e confirme que os registros continuam disponíveis.

Os computadores clientes precisam apenas do navegador. Somente o servidor executa o Java e abre o banco. A atualização de tela é manual; a aplicação não possui notificações em tempo real.

Se funciona no servidor mas não no outro aparelho, verifique o perfil privado, o IP, a regra do firewall e se o Wi-Fi está em uma rede de convidados com isolamento entre dispositivos. O console `/h2-console` deve continuar indisponível.

## 4. Dados e segurança

- O servidor precisa ficar ligado, sem suspender, com a aplicação executando.
- O banco fica em `data/orcafacil.mv.db` do servidor. Não é sincronizado entre máquinas e não deve ficar em pasta de rede, OneDrive ou Google Drive durante o uso.
- A aplicação não tem autenticação nem configuração própria de HTTPS. Pessoas com acesso à aplicação podem consultar e alterar registros. Use dados fictícios nos testes.
- Faça cópias de segurança com a aplicação parada. Não substitua o banco enquanto ela estiver executando.
- O código e as configurações de rede estão versionados. O banco e seus backups ficam fora do repositório; a pasta `/data/` é ignorada pelo Git.

## 5. Voltar ao uso apenas neste computador

Pare a execução e use `mvn spring-boot:run`, mantendo o `application.properties` com `server.address=127.0.0.1` e sem perfil `lan` ou variáveis de rede ativas. O mesmo banco será utilizado. O perfil usa loopback por padrão quando nenhum IP é fornecido.

## Registro histórico da validação do perfil

Na preparação inicial, o perfil foi carregado como configuração externa pelo JAR já compilado do projeto original, em uma pasta temporária de testes. Foram verificados cadastro por um cliente HTTP e leitura por outro, criação do arquivo H2, preservação do registro após reiniciar o processo e resposta 404 no caminho do console H2. Isso valida a configuração e a persistência no servidor, mas não equivale a testar dois computadores físicos.

Naquela validação, o script PowerShell foi revisado, mas não executado, pois o ambiente era Linux e não dispunha de PowerShell. O registro não comprova a configuração do firewall do Windows nem o acesso entre aparelhos físicos. A seção 3 contém um roteiro reutilizável para essa verificação.

## Referências

- Spring Boot — perfis: https://docs.spring.io/spring-boot/reference/features/profiles.html
- Spring Boot — execução via Maven: https://docs.spring.io/spring-boot/maven-plugin/run.html
- H2 — modos de conexão: https://h2database.github.io/html/features.html
- Firewall do Windows: https://learn.microsoft.com/en-us/powershell/module/netsecurity/new-netfirewallrule
