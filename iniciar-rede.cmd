@echo off
setlocal

rem Executa a partir da pasta onde este arquivo esta.
pushd "%~dp0"

where java.exe >nul 2>nul
if errorlevel 1 (
    echo ERRO: Java nao encontrado no PATH.
    echo Instale o JDK 17 ou superior e reabra o terminal.
    goto :erro
)

set "JAVAC_PATH="
for /f "delims=" %%J in ('where javac.exe 2^>nul') do if not defined JAVAC_PATH set "JAVAC_PATH=%%J"
if not defined JAVAC_PATH (
    echo ERRO: o JDK nao foi encontrado. O sistema precisa do compilador javac.
    echo Instale o JDK 17 ou superior e reabra o terminal.
    goto :erro
)

rem Usa o JDK que fornece o javac encontrado, mesmo se JAVA_HOME antigo apontar para Java 8.
for %%J in ("%JAVAC_PATH%") do set "JAVA_BIN=%%~dpJ"
for %%J in ("%JAVA_BIN%..") do set "JAVA_HOME=%%~fJ"

set "MAVEN_CMD="
for /f "delims=" %%M in ('where mvn.cmd 2^>nul') do if not defined MAVEN_CMD set "MAVEN_CMD=%%M"
if not defined MAVEN_CMD (
    for /d %%D in ("%USERPROFILE%\.maven\apache-maven-*") do (
        if exist "%%~fD\bin\mvn.cmd" set "MAVEN_CMD=%%~fD\bin\mvn.cmd"
    )
)
if not defined MAVEN_CMD (
    echo ERRO: Maven nao encontrado no PATH; o projeto nao chegou a iniciar.
    echo Instale o Apache Maven, adicione a pasta bin ao PATH e reabra o VS Code.
    echo Verifique no terminal com: mvn -version
    goto :erro
)

rem IP do computador que armazena o banco.
set "ORCAFACIL_HOST=192.168.15.24"

rem Carrega as configuracoes do application-lan.properties.
call "%MAVEN_CMD%" spring-boot:run "-Dspring-boot.run.profiles=lan"
if errorlevel 1 (
    echo ERRO: o Maven encerrou com falha. Leia a mensagem exibida acima.
    goto :erro
)

popd
endlocal
exit /b 0

:erro
popd
pause
endlocal
exit /b 1