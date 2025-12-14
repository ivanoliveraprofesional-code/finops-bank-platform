<#
.SYNOPSIS
    Master Script: Bootstraps Infrastructure, GitOps, and verifies connectivity.
    FLOW: Infra -> Cluster -> Istio -> DB/Secrets -> ArgoCD (Deploy) -> Wait -> Smoke Tests
#>
$ErrorActionPreference = "Stop"

# --- SSL Configuration (Fixes Invoke-WebRequest errors) ---
if ([System.Net.ServicePointManager]::SecurityProtocol -notcontains 'Tls12') {
    [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor [System.Net.SecurityProtocolType]::Tls12
}
[System.Net.ServicePointManager]::ServerCertificateValidationCallback = {$true}

function Write-Log {
    param([string]$Message, [string]$Color = "White")
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $Message" -ForegroundColor $Color
}

Write-Log "--- STARTING MASTER BOOTSTRAP ---" "Cyan"

# ---------------------------------------------------------
# FASE 1: INFRAESTRUCTURA (Docker/LocalStack)
# ---------------------------------------------------------
Write-Log "--- FASE 1: INFRAESTRUCTURA ---" "Yellow"
powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap.ps1
if ($LASTEXITCODE -ne 0) { throw "Fallo en Bootstrap" }

# ---------------------------------------------------------
# FASE 2: ORQUESTACION (Kind Cluster)
# ---------------------------------------------------------
Write-Log "--- FASE 2: ORQUESTACION ---" "Yellow"
powershell -ExecutionPolicy Bypass -File .\scripts\deploy-cluster.ps1
if ($LASTEXITCODE -ne 0) { throw "Fallo en Cluster Deploy" }

# ---------------------------------------------------------
# FASE 3: PREPARACION K8S (Namespace & Istio Base)
# ---------------------------------------------------------
Write-Log "--- FASE 3: ISTIO & NAMESPACES ---" "Yellow"
# Create namespace first so we can inject secrets later
kubectl apply -f .\kubernetes\namespaces.yaml

# Install Istio Base (Required before Apps)
Write-Log "Instalando Istio Core..." "Cyan"
istioctl install --set profile=demo -y
kubectl label namespace default istio-injection=enabled --overwrite

# ---------------------------------------------------------
# FASE 4: DATOS & SECRETOS
# ---------------------------------------------------------
Write-Log "--- FASE 4: CAPA DE DATOS ---" "Yellow"
Write-Log "Sincronizando credenciales (Terraform -> K8s)..." "Cyan"
powershell -ExecutionPolicy Bypass -File .\scripts\sync-secrets.ps1
if ($LASTEXITCODE -ne 0) { throw "Fallo sincronizando secretos" }

Write-Log "Desplegando Postgres & Configuración Global..." "Cyan"
kubectl apply -f .\kubernetes\platform\database\
kubectl apply -f .\kubernetes\platform\localstack-service.yaml
kubectl apply -f .\kubernetes\platform\global-config.yaml

Write-Log "Esperando a que Postgres este listo..." "Gray"
kubectl wait --for=condition=Ready pod/postgres-0 --timeout=120s

# ---------------------------------------------------------
# FASE 5: GITOPS (ARGOCD) - EL MOTOR DE DESPLIEGUE
# ---------------------------------------------------------
Write-Log "--- FASE 5: GITOPS (ARGOCD) ---" "Yellow"

# 1. Install ArgoCD
powershell -ExecutionPolicy Bypass -File .\scripts\install-argocd.ps1

# 2. Tell ArgoCD what to deploy (The 'Applications' Pattern)
Write-Log "Sincronizando Aplicaciones via GitOps..." "Cyan"
kubectl apply -f .\kubernetes\argocd\apps.yaml

# 3. CRITICAL WAIT: Wait for ArgoCD to clone repo and start pods
Write-Log "Esperando a que ArgoCD despliegue los microservicios..." "Magenta"
Write-Log "(Esto puede tardar 2-3 mins mientras baja imagenes de GHCR)" "Gray"

# --- FIX: Loop check using valid kubectl syntax ---
$Retries = 0
Write-Host "Waiting for ArgoCD to sync core-banking..." -NoNewline

while ($true) {
    # FIX: Use --ignore-not-found instead of PowerShell -ErrorAction
    # This prevents the 'unknown shorthand flag' error
    $Dep = kubectl get deployment core-banking -n finops-bank --ignore-not-found
    
    if ($Dep) { 
        Write-Host " Found!" -ForegroundColor Green
        break 
    }

    Start-Sleep -Seconds 10
    $Retries++
    Write-Host "." -NoNewline
    
    if ($Retries -gt 30) { 
        throw "Timeout: ArgoCD no creó el deployment de Core Banking tras 5 minutos. Revisa el UI de ArgoCD." 
    }
}
# --------------------------------------------------

# Now wait for the pods to be 'Ready' (Running and passing Health Checks)
Write-Log "Deployment creado. Esperando Pods Ready..." "Cyan"
kubectl wait --for=condition=Available deployment/core-banking -n finops-bank --timeout=300s
kubectl wait --for=condition=Available deployment/auth-service -n finops-bank --timeout=300s

# ---------------------------------------------------------
# FASE 6: ACCESO (PORT FORWARDING)
# ---------------------------------------------------------
Write-Log "--- FASE 6: ACCESO & NETWORKING ---" "Yellow"

# Kill any old port-forwards to prevent conflicts (Wait 2s to ensure release)
Get-Job | Remove-Job -Force
Start-Sleep -Seconds 2

# --- FIX: Automate Port Forwarding in Background Jobs ---
Write-Log "Iniciando Port-Forward para ArgoCD (8085 -> 80)..." "Cyan"
$ArgoJob = Start-Job -ScriptBlock { 
    # This runs in background so the script continues
    kubectl port-forward svc/argocd-server -n argocd 8085:80 
}
Write-Log " > Job ID: $($ArgoJob.Id)" "Gray"

Write-Log "Iniciando Port-Forward para Istio Gateway (80 -> 80)..." "Cyan"
$IstioJob = Start-Job -ScriptBlock { 
    kubectl port-forward -n istio-system svc/istio-ingressgateway 80:80 
}
Write-Log " > Job ID: $($IstioJob.Id)" "Gray"
# -------------------------------------------------------

Write-Log "Esperando 10s para estabilizar tuneles..." "Gray"
Start-Sleep -Seconds 10

# ---------------------------------------------------------
# FASE 7: VALIDACION FINAL (SMOKE TESTS)
# ---------------------------------------------------------
Write-Log "--- FASE 7: SMOKE TESTS ---" "Yellow"
powershell -ExecutionPolicy Bypass -File .\scripts\test-connectivity.ps1

if ($LASTEXITCODE -ne 0) {
    Write-Log "¡ALERTA! Los tests fallaron. Revisa: kubectl get pods -n finops-bank" "Red"
} else {
    Write-Log "--- SISTEMA LISTO Y OPERATIVO ---" "Green"
    Write-Log "ArgoCD UI:   http://localhost:8085" "Cyan"
    Write-Log "Banking API: http://localhost:80" "Cyan"
}