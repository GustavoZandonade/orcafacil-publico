# Comandos de execução

Execute os comandos na pasta do repositório que contém `pom.xml`.

## Pré-requisitos

JDK 17 ou superior e Maven no PATH:

```powershell
java -version
javac -version
mvn -version
```

## Inicialização local

```powershell
mvn spring-boot:run
```

Endereço padrão: http://localhost:8080. Encerre com Ctrl+C.

## Inicialização na rede local

Com a execução anterior encerrada, consulte o IPv4 do servidor e informe-o ao iniciador:

```powershell
ipconfig
.\iniciar-rede-local.ps1 -Endereco 192.168.15.24
```

O IP acima é um exemplo; use o endereço real do servidor. Os outros dispositivos acessam `http://IP_DO_SERVIDOR:8080`.

O iniciador alternativo `cmd.exe /d /c .\iniciar-rede.cmd` contém o IP fixo `192.168.15.24`. Seu uso depende de esse endereço corresponder ao computador servidor. Consulte [o guia de rede](LEIA-ME-REDE-LOCAL.md) para configuração e firewall.

## Testes e empacotamento

```powershell
mvn verify
java -jar target/orcafacil-0.1.0.jar
```

## Perfil de nuvem

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=cloud
```

Usa a porta `PORT` ou 10000 e H2 em memória. Os dados são perdidos ao encerrar o processo.

## Tamanho do banco local

Nos modos padrão e `lan`, em bytes:

```powershell
(Get-Item .\data\orcafacil.mv.db).Length
```
