$ErrorActionPreference = "Stop"
Write-Host "--- DIAGNOSTICO DE LAMBDA AUTHORIZER ---" -ForegroundColor Cyan

# 1. Obtener Datos
Push-Location "$PSScriptRoot/../terraform"
try {
    $PrivateKey = terraform output -raw jwt_private_key_pkcs8
} finally { Pop-Location }

# 2. Generar un Token valido usando Python (reutilizamos tu logica)
Write-Host "Generando Token de prueba..." -ForegroundColor Yellow
$Env:BKP_PRIVATE_KEY = $PrivateKey
$TempPy = [System.IO.Path]::GetTempFileName() + ".py"
Set-Content $TempPy @"
import jwt, time, os
key = os.environ.get('BKP_PRIVATE_KEY', '')
token = jwt.encode({
    'sub': 'debug-user',
    'name': 'Debug',
    'iat': int(time.time()),
    'exp': int(time.time()) + 3600
}, key, algorithm='RS256')
if isinstance(token, bytes): token = token.decode('utf-8')
print(token)
"@

$Token = python $TempPy
Remove-Item $TempPy
$Env:BKP_PRIVATE_KEY = $null
$Token = $Token.Trim()

# 3. Crear Payload simulando API Gateway
$PayloadFile = "lambda-payload.json"
$Payload = @{
    type = "TOKEN"
    methodArn = "arn:aws:execute-api:us-east-1:000000:api-id/dev/GET/smoke-test"
    authorizationToken = "Bearer $Token"
} | ConvertTo-Json

Set-Content $PayloadFile $Payload

# 4. INVOCAR DIRECTAMENTE (La prueba de fuego)
Write-Host "Invocando Lambda finops-authorizer directamente..." -ForegroundColor Cyan

aws --endpoint-url=http://localhost:4566 lambda invoke `
    --function-name finops-authorizer `
    --payload file://lambda-payload.json `
    response.json

# 5. Analizar Respuesta
Write-Host "`n--- RESULTADO ---" -ForegroundColor Yellow
$Response = Get-Content response.json | ConvertFrom-Json

if ($Response.errorMessage) {
    Write-Host "CRASH DETECTADO:" -ForegroundColor Red
    Write-Host "Error: $($Response.errorMessage)" -ForegroundColor Red
    Write-Host "Type: $($Response.errorType)" -ForegroundColor Red
    if ($Response.stackTrace) {
        Write-Host "Stack Trace:" -ForegroundColor DarkRed
        $Response.stackTrace | ForEach-Object { Write-Host "  $_" }
    }
} else {
    Write-Host "EXITO: La Lambda funciona correctamente." -ForegroundColor Green
    Write-Host "Policy Generada:" -ForegroundColor Gray
    $Response | ConvertTo-Json -Depth 5
}

Remove-Item $PayloadFile
Remove-Item response.json