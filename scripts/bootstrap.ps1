<#
.SYNOPSIS
    Script de Bootstrapping para FinOps Banking Platform (Local).
    Handles LocalStack lifecycle, S3 Backend, DynamoDB Locking, and Terraform.
    LOCATION: /scripts/bootstrap.ps1
#>

$ErrorActionPreference = "Stop"

# --- PATH FIX: Calcular rutas relativas al script ---
$ScriptPath = $PSScriptRoot
$ProjectRoot = Split-Path -Parent $ScriptPath
$TerraformDir = Join-Path $ProjectRoot "terraform"
$DockerComposeFile = Join-Path $ProjectRoot "docker-compose.yml"

# --- CONFIGURACION ---
$LocalStackEndpoint = "http://localhost:4566"
$StateBucket = "s3://terraform-state"
$LockTable = "terraform-lock"
$Region = "us-east-1"

# --- FIX DE CREDENCIALES ---
$Env:AWS_ACCESS_KEY_ID = "test"
$Env:AWS_SECRET_ACCESS_KEY = "test"
$Env:AWS_DEFAULT_REGION = $Region

# --- FUNCIONES AUXILIARES ---
function Write-Log {
    param([string]$Message, [string]$Color = "White")
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $Message" -ForegroundColor $Color
}

function Test-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        Write-Log "ERROR: $Name no esta instalado o no esta en el PATH." "Red"
        exit 1
    }
}

# --- VALIDACIONES INICIALES ---
Write-Log ">>> Iniciando Bootstrapping de FinOps Bank Platform..." "Cyan"
Test-Command "docker"
Test-Command "aws"
Test-Command "terraform"

# --- PASO 1: INFRAESTRUCTURA DE CONTENEDORES ---
Write-Log "1. Levantando LocalStack con Docker Compose..." "Yellow"
try {
    # FIX: Apuntamos al archivo en el root
    docker-compose -f $DockerComposeFile up -d --remove-orphans
    if ($LASTEXITCODE -ne 0) { throw "Docker Compose fallo." }
}
catch {
    Write-Log "Error al ejecutar docker-compose en $DockerComposeFile" "Red"
    exit 1
}

# --- PASO 2: HEALTH CHECK ---
Write-Log "2. Esperando a que LocalStack este listo (Port 4566)..." "Yellow"

$MaxRetries = 30
$RetryCount = 0
$IsReady = $false

while (-not $IsReady -and $RetryCount -lt $MaxRetries) {
    try {
        aws --endpoint-url=$LocalStackEndpoint s3 ls 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            $IsReady = $true
            Write-Log "LocalStack esta respondiendo correctamente." "Green"
        } else {
            throw "Not ready"
        }
    }
    catch {
        Write-Log "Esperando servicios AWS simulados... ($($RetryCount+1)/$MaxRetries)" "Gray"
        Start-Sleep -Seconds 2
        $RetryCount++
    }
}

if (-not $IsReady) {
    Write-Log "TIMEOUT: LocalStack no respondio despues de 60 segundos." "Red"
    exit 1
}

# --- PASO 3: CONFIGURACION DE BACKEND (S3 + DYNAMODB) ---
Write-Log "3. Configurando Backend (State & Locking)..." "Yellow"

try {
    # A. S3 BUCKET
    $BucketExists = aws --endpoint-url=$LocalStackEndpoint s3 ls | Select-String "terraform-state"
    if ($BucketExists) {
        Write-Log "- Bucket '$StateBucket' ya existe." "Green"
    } else {
        Write-Log "- Creando bucket '$StateBucket'..." "Cyan"
        aws --endpoint-url=$LocalStackEndpoint s3 mb $StateBucket --region $Region | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Error creando bucket S3" }
        Write-Log "- Bucket creado." "Green"
    }

    # B. DYNAMODB LOCK TABLE
    Write-Log "- Verificando Tabla de Lock DynamoDB..." "Cyan"
    
    $OldErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    aws --endpoint-url=$LocalStackEndpoint dynamodb describe-table --table-name $LockTable 2>&1 | Out-Null
    $TableExists = $LASTEXITCODE -eq 0
    $ErrorActionPreference = $OldErrorActionPreference

    if ($TableExists) {
        Write-Log "- Tabla de Lock '$LockTable' ya existe." "Green"
    } else {
        Write-Log "- La tabla no existe. Creando Tabla DynamoDB '$LockTable'..." "Cyan"
        aws --endpoint-url=$LocalStackEndpoint dynamodb create-table `
            --table-name $LockTable `
            --attribute-definitions AttributeName=LockID,AttributeType=S `
            --key-schema AttributeName=LockID,KeyType=HASH `
            --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 `
            --region $Region | Out-Null
        
        if ($LASTEXITCODE -ne 0) { throw "FALLO CRITICO: No se pudo crear la tabla DynamoDB." }
        
        Write-Log "- Esperando consistencia de tabla..." "Gray"
        aws --endpoint-url=$LocalStackEndpoint dynamodb wait table-exists --table-name $LockTable
        
        Write-Log "- Tabla de Lock creada y activa." "Green"
    }
}
catch {
    Write-Log "Error critico configurando Backend: $_" "Red"
    exit 1
}

# --- PASO 4: TERRAFORM ---
Write-Log "4. Ejecutando Terraform..." "Yellow"

# FIX: Usamos la ruta absoluta calculada
Push-Location $TerraformDir

try {
    if (Test-Path ".terraform/terraform.tfstate") { Remove-Item ".terraform/terraform.tfstate" -Force }

    Write-Log "-> terraform init -reconfigure" "Cyan"
    terraform init -input=false -reconfigure
    if ($LASTEXITCODE -ne 0) { throw "Terraform Init failed" }

    Write-Log "-> terraform validate" "Cyan"
    terraform validate
    if ($LASTEXITCODE -ne 0) { throw "Terraform Validate failed" }

    Write-Log "-> terraform plan" "Cyan"
    terraform plan -out=tfplan
    if ($LASTEXITCODE -ne 0) { throw "Terraform Plan failed" }

    Write-Log "-> terraform apply" "Cyan"
    terraform apply -auto-approve tfplan
    if ($LASTEXITCODE -ne 0) { throw "Terraform Apply failed" }
    
    Write-Log ">>> INFRAESTRUCTURA DESPLEGADA EXITOSAMENTE <<<" "Green"
}
catch {
    Write-Log "FALLO TERRAFORM. Revisa los logs arriba." "Red"
    exit 1
}
finally {
    Pop-Location
}