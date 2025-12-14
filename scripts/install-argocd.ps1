<#
.SYNOPSIS
    Installs ArgoCD in the Kind cluster and configures it for local development.
#>
$ErrorActionPreference = "Stop"

Write-Host ">>> Installing ArgoCD (GitOps Controller)..." -ForegroundColor Cyan

# 1. Create Namespace
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -

# 2. Install Stable Version
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# 3. Wait for Components
Write-Host "Waiting for ArgoCD Server to start..." -ForegroundColor Gray
kubectl wait --for=condition=Available deployment/argocd-server -n argocd --timeout=300s

# 4. Patch for Local Insecure Mode (Avoids SSL errors in localhost)
# We disable TLS on the Argo Server so we can access it via HTTP locally
kubectl -n argocd patch deployment argocd-server --type json `
  -p='[{"op": "add", "path": "/spec/template/spec/containers/0/command/-", "value": "--insecure"}]'

# 5. Get Initial Admin Password
$ArgoPass = kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | 
    ForEach-Object { [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($_)) }

Write-Host "`n------------------------------------------------" -ForegroundColor Green
Write-Host "ArgoCD Installed Successfully!" -ForegroundColor Green
Write-Host "URL:      http://localhost:8085 (Run Port-Forward command below)"
Write-Host "User:     admin"
Write-Host "Password: $ArgoPass"
Write-Host "------------------------------------------------`n"

Write-Host ">>> ACTION REQUIRED: Run this command in a NEW terminal to open the UI:" -ForegroundColor Yellow
Write-Host "kubectl port-forward svc/argocd-server -n argocd 8085:80" -ForegroundColor Cyan