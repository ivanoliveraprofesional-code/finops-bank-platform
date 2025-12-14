<#
.SYNOPSIS
    Reads DB credentials and JWT Keys from LocalStack and creates Kubernetes Secrets.
    LOCATION: /scripts/sync-secrets.ps1
#>
$ErrorActionPreference = "Stop"

# --- FIX SSL: Ignore invalid certificates ---
if ([System.Net.ServicePointManager]::SecurityProtocol -notcontains 'Tls12') {
    [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor [System.Net.SecurityProtocolType]::Tls12
}
[System.Net.ServicePointManager]::ServerCertificateValidationCallback = {$true}

Write-Host ">>> Syncing Secrets from LocalStack Terraform to Kubernetes..." -ForegroundColor Cyan

# Helper function to get secret value
function Get-SecretValue {
    param ($Id)
    try {
        # Fetch the SecretString directly
        $Val = aws --endpoint-url=http://localhost:4566 secretsmanager get-secret-value --secret-id $Id --query SecretString --output text
        if (-not $Val) { throw "Secret $Id is empty" }
        return $Val
    }
    catch {
        Write-Error "Failed to read $Id from Secrets Manager."
        exit 1
    }
}

# 1. Fetch DB Credentials (JSON)
$DbSecretJson = Get-SecretValue "finops-bank/db-owner"
$DbObj = $DbSecretJson | ConvertFrom-Json
$DbUser = $DbObj.username
$DbPass = $DbObj.password

# 2. Fetch JWT Keys (Raw Strings based on your Terraform)
$JwtPrivate = Get-SecretValue "finops-bank/auth/jwt-private-key"
$JwtPublic  = Get-SecretValue "finops-bank/auth/jwt-public-key"

# 3. Create 'db-credentials' (For Postgres Pod)
try {
    kubectl create secret generic db-credentials `
        --from-literal=POSTGRES_USER=$DbUser `
        --from-literal=POSTGRES_PASSWORD=$DbPass `
        --dry-run=client -o yaml | kubectl apply -f -
    Write-Host "[OK] Secret 'db-credentials' created." -ForegroundColor Green
} catch { Write-Error "Failed to create db-credentials"; exit 1 }

# 4. Create 'auth-secrets' (For Java Microservices)
try {
    kubectl create secret generic auth-secrets -n finops-bank `
        --from-literal=JWT_PRIVATE_KEY=$JwtPrivate `
        --from-literal=JWT_PUBLIC_KEY=$JwtPublic `
        --from-literal=DB_PASSWORD=$DbPass `
        --dry-run=client -o yaml | kubectl apply -f -
    Write-Host "[OK] Secret 'auth-secrets' created in namespace 'finops-bank'." -ForegroundColor Green
} catch { Write-Error "Failed to create auth-secrets"; exit 1 }