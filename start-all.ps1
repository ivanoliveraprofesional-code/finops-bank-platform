<#
.SYNOPSIS
    Master Script: Levanta la Plataforma y ejecuta Smoke Tests.
    VERSION: Fix de Rutas
#>
$ErrorActionPreference = "Stop"

# --- CONFIGURACION SSL GLOBAL ---
if ([System.Net.ServicePointManager]::SecurityProtocol -notcontains 'Tls12') {
    [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor [System.Net.SecurityProtocolType]::Tls12
}
[System.Net.ServicePointManager]::ServerCertificateValidationCallback = {$true}

function Write-Log {
    param([string]$Message, [string]$Color = "White")
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $Message" -ForegroundColor $Color
}

Write-Log "--- INICIANDO SECUENCIA DE ARRANQUE MAESTRA ---" "Cyan"

# FASE 1: INFRAESTRUCTURA
Write-Log "--- FASE 1: INFRAESTRUCTURA ---" "Yellow"
powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap.ps1
if ($LASTEXITCODE -ne 0) { throw "Fallo en Bootstrap" }

# FASE 2: ORQUESTACION
Write-Log "--- FASE 2: ORQUESTACION ---" "Yellow"
powershell -ExecutionPolicy Bypass -File .\scripts\deploy-cluster.ps1
if ($LASTEXITCODE -ne 0) { throw "Fallo en Cluster Deploy" }

# FASE 3: SERVICE MESH
Write-Log "--- FASE 3: SERVICE MESH ---" "Yellow"
$IstioCheck = kubectl get ns istio-system --ignore-not-found
if ($IstioCheck) {
    Write-Log "Istio ya esta instalado." "Green"
} else {
    Write-Log "Instalando Istio..." "Cyan"
    istioctl install --set profile=demo -y
    kubectl label namespace default istio-injection=enabled --overwrite
}

# FASE 4: CAPA DE DATOS
Write-Log "--- FASE 4: CAPA DE DATOS ---" "Yellow"

# 1. Create Namespace first
kubectl apply -f .\kubernetes\namespaces.yaml

# 2. Sync Secrets (Now includes JWT keys)
Write-Log "Sincronizando credenciales..." "Cyan"
powershell -ExecutionPolicy Bypass -File .\scripts\sync-secrets.ps1
if ($LASTEXITCODE -ne 0) { throw "Fallo sincronizando secretos" }

# 3. Deploy Database & LocalStack Service
Write-Log "Desplegando Base de Datos y Config..." "Cyan"
kubectl apply -f .\kubernetes\platform\database\
kubectl apply -f .\kubernetes\platform\localstack-service.yaml
kubectl apply -f .\kubernetes\platform\global-config.yaml

Write-Log "Esperando a que Postgres este listo..." "Gray"
kubectl wait --for=condition=Ready pod/postgres-0 --timeout=120s

# --- NEW SECTION ---
# FASE 4.5: PLATAFORMA Y APPS
Write-Log "--- FASE 4.5: SEGURIDAD Y APLICACIONES ---" "Yellow"

# 1. Apply Istio Gateway & Security Policies
kubectl apply -f .\kubernetes\platform\istio\
kubectl apply -f .\kubernetes\security\

# 2. Deploy Microservices
Write-Log "Desplegando Microservicios..." "Cyan"
kubectl apply -f .\kubernetes\apps\auth-service\
kubectl apply -f .\kubernetes\apps\core-banking\
kubectl apply -f .\kubernetes\apps\credit-risk-service\
kubectl apply -f .\kubernetes\apps\audit-service\

# FASE 5: SMOKE TESTS
Write-Log "--- FASE 5: SMOKE TESTS ---" "Yellow"
powershell -ExecutionPolicy Bypass -File .\scripts\test-connectivity.ps1

if ($LASTEXITCODE -ne 0) {
    Write-Log "¡ALERTA! Los tests fallaron." "Red"
} else {
    Write-Log "--- SISTEMA LISTO Y VERIFICADO ---" "Green"
}

# FASE 6: GITOPS (ARGOCD)
Write-Log "--- FASE 6: GITOPS (ARGOCD) ---" "Yellow"

# 1. Install ArgoCD
powershell -ExecutionPolicy Bypass -File .\scripts\install-argocd.ps1

# 2. Apply Apps Pattern
# NOTE: Ensure you have pushed your code to GitHub before running this!
# ArgoCD needs to see the repo online to sync.
Write-Log "Aplicando Manifiestos de Aplicación..." "Cyan"
kubectl apply -f .\kubernetes\argocd\apps.yaml

Write-Log "--- SISTEMA INICIADO ---" "Green"
Write-Log "Monitorear despliegues: http://localhost:8085" "Cyan"