# Execute na pasta do projeto: .\iniciar-rede-local.ps1 -Endereco 192.168.1.50
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Endereco
)

$ErrorActionPreference = 'Stop'

# Aceita somente um IPv4 privado explicito, sem escolher interfaces automaticamente.
if ($Endereco -notmatch '^\d{1,3}(\.\d{1,3}){3}$') {
    throw 'Informe o IPv4 da conexao local, por exemplo 192.168.1.50.'
}
$ip = $null
if (-not [System.Net.IPAddress]::TryParse($Endereco, [ref]$ip)) {
    throw 'Endereco IPv4 invalido.'
}
$octetos = $ip.GetAddressBytes()
$privado = ($octetos[0] -eq 10) -or
    ($octetos[0] -eq 172 -and $octetos[1] -ge 16 -and $octetos[1] -le 31) -or
    ($octetos[0] -eq 192 -and $octetos[1] -eq 168)
if (-not $privado) {
    throw 'Use um IPv4 privado da sua rede: 10.x.x.x, 172.16-31.x.x ou 192.168.x.x.'
}
$Endereco = $ip.ToString()
if (-not (Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -eq $Endereco })) {
    throw 'Esse IP nao pertence a este computador. Confira a conexao ativa com ipconfig.'
}
if (-not (Test-Path (Join-Path $PSScriptRoot 'pom.xml'))) {
    throw 'Copie este script para a pasta que contem o pom.xml do OrcaFacil.'
}
if (-not (Test-Path (Join-Path $PSScriptRoot 'src/main/resources/application-lan.properties'))) {
    throw 'Copie tambem o arquivo application-lan.properties para src/main/resources/.'
}
if (-not (Get-Command mvn.cmd -ErrorAction SilentlyContinue)) {
    throw 'Maven nao encontrado. Confira a instalacao e abra novamente o terminal.'
}

# Executar sempre na raiz conserva o caminho do banco ./data/orcafacil.
Push-Location $PSScriptRoot
try {
    Write-Host "Aguarde o Spring Boot iniciar; depois abra http://${Endereco}:8080"
    Write-Host 'Os outros computadores usam o mesmo endereco no navegador.'
    Write-Host 'Para encerrar, pressione Ctrl+C. O firewall nao e alterado por este script.'
    $argumentos = "--spring.profiles.active=lan --server.address=$Endereco --server.port=8080 --spring.h2.console.enabled=false --spring.h2.console.settings.web-allow-others=false"
    & mvn.cmd spring-boot:run "-Dspring-boot.run.arguments=$argumentos"
    if ($LASTEXITCODE -ne 0) {
        throw "O Maven encerrou com codigo $LASTEXITCODE. Consulte o erro acima."
    }
}
finally {
    Pop-Location
}
