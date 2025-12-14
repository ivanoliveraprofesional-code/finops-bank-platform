<#
.SYNOPSIS
    Genera un JWT HS256 válido usando el secreto compartido.
#>
$ErrorActionPreference = "Stop"

# 1. Obtener el Secreto (HS256)
try {
    $Secret = aws --endpoint-url=http://localhost:4566 secretsmanager get-secret-value --secret-id finops-bank/auth/jwt-secret --query SecretString --output text
    if (-not $Secret) { throw "Secreto vacio" }
}
catch {
    Write-Error "Error AWS: $_"
    exit 1
}

# Guardar temporalmente
$Secret | Out-File "temp_secret.txt" -Encoding ascii

# 2. Script Python
$PythonCode = @"
import jwt
import time
import sys

try:
    with open('temp_secret.txt', 'r') as f:
        secret = f.read().strip()

    payload = {
        "sub": "test-user-123",
        "role": "client",
        "iat": int(time.time()),
        "exp": int(time.time()) + 3600,
        "iss": "finops-bank-auth"
    }

    # CAMBIO: HS256
    token = jwt.encode(payload, secret, algorithm="HS256")
    print(token)
except Exception as e:
    print(f"ERROR_PYTHON: {str(e)}", file=sys.stderr)
    sys.exit(1)
"@

$PythonCode | Out-File "temp_signer.py" -Encoding ascii

# 3. Ejecutar
try {
    $Token = python temp_signer.py
    if ($LASTEXITCODE -ne 0) { throw "Python fallo" }
}
finally {
    if (Test-Path "temp_secret.txt") { Remove-Item "temp_secret.txt" }
    if (Test-Path "temp_signer.py") { Remove-Item "temp_signer.py" }
}

$Token = $Token.Trim()
Write-Output $Token