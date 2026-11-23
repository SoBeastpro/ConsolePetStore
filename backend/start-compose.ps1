$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $Root ".env"
$Compose = Join-Path $PSScriptRoot "docker-compose.yml"

if (-not (Test-Path $Compose)) { throw "Не найден $Compose" }
if (-not (Test-Path $EnvFile)) { throw "Не найден $EnvFile. Скопируйте .env.example в .env и задайте пароли." }

Set-Location $Root
docker compose --env-file $EnvFile -f $Compose up -d --wait
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ""
Write-Host "API:     http://localhost:8080/api"
Write-Host "Swagger: http://localhost:8080/swagger-ui.html"
Write-Host "Health:  http://localhost:8080/actuator/health"
