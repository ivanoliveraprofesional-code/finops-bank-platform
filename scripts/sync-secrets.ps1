<#
.SYNOPSIS
    Reads DB credentials and JWT Keys from LocalStack and creates Kubernetes Secrets.
#>
$ErrorActionPreference = "Stop"

# --- FIX SSL ---
if ([System.Net.ServicePointManager]::SecurityProtocol -notcontains 'Tls12') {
    [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor [System.Net.SecurityProtocolType]::Tls12
}
[System.Net.ServicePointManager]::ServerCertificateValidationCallback = {$true}

Write-Host ">>> Syncing Secrets from LocalStack Terraform to Kubernetes..." -ForegroundColor Cyan

function Get-SecretValue {
    param ($Id)
    try {
        $Val = aws --endpoint-url=http://localhost:4566 secretsmanager get-secret-value --secret-id $Id --query SecretString --output text
        if (-not $Val) { throw "Secret $Id is empty" }
        return $Val
    }
    catch { Write-Error "Failed to read $Id from Secrets Manager."; exit 1 }
}

# 1. Fetch Credentials
$DbSecretJson = Get-SecretValue "finops-bank/db-owner"
$DbObj = $DbSecretJson | ConvertFrom-Json
$DbUser = $DbObj.username
$DbPass = $DbObj.password

Write-Host "   > DB User detected: $DbUser" -ForegroundColor Gray

# 2. Fetch Keys
$JwtPrivate = Get-SecretValue "finops-bank/auth/jwt-private-key"
$JwtPublic  = Get-SecretValue "finops-bank/auth/jwt-public-key"

# 3. Create 'db-credentials' (Core Banking)
try {
    kubectl create secret generic db-credentials -n finops-bank `
        --from-literal=POSTGRES_USER=$DbUser `
        --from-literal=POSTGRES_PASSWORD=$DbPass `
        --dry-run=client -o yaml | kubectl apply -f -
    Write-Host "[OK] Secret 'db-credentials' created." -ForegroundColor Green
} catch { Write-Error "Failed to create db-credentials"; exit 1 }

# 4. Create 'auth-secrets' (Auth Service)
try {
    # Ensure DB_USERNAME is explicitly added here
    kubectl create secret generic auth-secrets -n finops-bank `
        --from-literal=JWT_PRIVATE_KEY=$JwtPrivate `
        --from-literal=JWT_PUBLIC_KEY=$JwtPublic `
        --from-literal=DB_USERNAME=$DbUser `
        --from-literal=DB_PASSWORD=$DbPass `
        --dry-run=client -o yaml | kubectl apply -f -
    Write-Host "[OK] Secret 'auth-secrets' created." -ForegroundColor Green
} catch { Write-Error "Failed to create auth-secrets"; exit 1 }