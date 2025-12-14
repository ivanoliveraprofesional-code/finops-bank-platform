<#
.SYNOPSIS
    Total Destruction Script for FinOps Banking Platform.
    Eliminates Cluster, Containers, Local State, and Background Processes.
#>
$ErrorActionPreference = "SilentlyContinue" # We don't want to stop if a file is already gone

function Write-Log {
    param([string]$Message, [string]$Color = "White")
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $Message" -ForegroundColor $Color
}

Write-Log "--- STARTING DESTRUCTION SEQUENCE ---" "Red"

# 1. Kill Background Processes (Port Forwards)
# This is important because 'kubectl port-forward' might keep ports 8085/8080 locked.
Write-Log "1. Stopping background processes (kubectl/terraform)..." "Yellow"
Get-Process | Where-Object { $_.ProcessName -eq "kubectl" -or $_.ProcessName -eq "terraform" } | Stop-Process -Force

# 2. Delete Kubernetes Cluster (Kind)
# This destroys ArgoCD, Istio, Postgres, App Pods, and Secrets instantly.
if (Get-Command "kind" -ErrorAction SilentlyContinue) {
    Write-Log "2. Deleting Kind Cluster (finops-cluster)..." "Yellow"
    kind delete cluster --name finops-cluster
}

# 3. Delete Containers & Volumes (LocalStack)
# This destroys SQS queues, DynamoDB tables, and S3 buckets.
if (Get-Command "docker-compose" -ErrorAction SilentlyContinue) {
    Write-Log "3. Removing Docker Environment (LocalStack)..." "Yellow"
    # We look for the compose file in backend or root depending on where it is
    if (Test-Path "docker-compose.yml") {
        docker-compose down -v --remove-orphans
    } elseif (Test-Path "backend/docker-compose-dev.yml") {
         docker-compose -f backend/docker-compose-dev.yml down -v --remove-orphans
    }
}

# 4. Terraform Cleanup (Local State)
Write-Log "4. Cleaning Terraform state and cache..." "Yellow"
$TfDir = "terraform"
if (Test-Path $TfDir) {
    Remove-Item "$TfDir/.terraform" -Recurse -Force
    Remove-Item "$TfDir/.terraform.lock.hcl" -Force
    Remove-Item "$TfDir/tfplan" -Force
    Remove-Item "$TfDir/terraform.tfstate" -Force
    Remove-Item "$TfDir/terraform.tfstate.backup" -Force
}

# 5. Cleanup Temporary Artifacts (Scripts)
Write-Log "5. Cleaning temporary script artifacts..." "Yellow"
if (Test-Path "warmup_output.json") { Remove-Item "warmup_output.json" -Force }
# Clean up any stray Python temp files in scripts folder
Get-ChildItem "scripts" -Filter "*.tmp" | Remove-Item -Force
Get-ChildItem "scripts" -Filter "*.py" | Remove-Item -Force

Write-Log ">>> ENVIRONMENT DESTROYED AND CLEAN <<<" "Green"