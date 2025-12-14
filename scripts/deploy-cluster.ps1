<#
.SYNOPSIS
    Despliega Kubernetes (Kind) ESTANDAR y lo conecta a LocalStack.
    VERSION: Robust Network Peering
#>
$ErrorActionPreference = "Stop"

function Write-Log {
    param([string]$Message, [string]$Color = "White")
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $Message" -ForegroundColor $Color
}

$ClusterName = "finops-cluster"
$LocalStackContainer = "finops-localstack"
$KindNode = "$ClusterName-control-plane"

# --- PATH FIX ---
# Asegura que siempre encuentre el yaml sin importar desde donde corras el script
$KindConfigPath = Join-Path $PSScriptRoot "..\kind-config.yaml"
# ----------------

# 1. Verificar/Crear Cluster
if (kind get clusters | Select-String $ClusterName) {
    Write-Log "El cluster '$ClusterName' ya existe." "Green"
} else {
    Write-Log "1. Creando Cluster de Kubernetes..." "Cyan"
    
    # Validar que existe el archivo antes de correr kind
    if (-not (Test-Path $KindConfigPath)) {
        throw "No se encuentra el archivo de config: $KindConfigPath"
    }

    kind create cluster --config $KindConfigPath --name $ClusterName
    if ($LASTEXITCODE -ne 0) { throw "Error creando cluster Kind" }
    
    Write-Log "Cluster creado." "Green"
}

# 2. Network Peering (Conexion de Redes)
Write-Log "2. Configurando Red Docker..." "Yellow"

try {
    # Paso A: Detectar en qué red está corriendo LocalStack
    # Usamos docker inspect para sacar el nombre exacto de la red (ej. finops-bank-platform_default)
    $NetworkName = docker inspect $LocalStackContainer --format '{{range $k, $v := .NetworkSettings.Networks}}{{$k}}{{end}}'
    
    if (-not $NetworkName) { 
        Write-Log "WARN: No se pudo detectar la red de LocalStack. Asumiendo 'finops-bank-platform_default'." "Yellow"
        $NetworkName = "finops-bank-platform_default" 
    } else {
        Write-Log "   Red LocalStack detectada: $NetworkName" "Cyan"
    }
    
    # Paso B: Verificar si el nodo de Kind existe
    $NodeExists = docker ps -q -f name=$KindNode
    if (-not $NodeExists) {
        throw "El nodo del cluster ($KindNode) no esta corriendo. Kind fallo silenciosamente."
    }

    # Paso C: Verificar si el nodo ya está en esa red
    # Buscamos si el JSON de redes del nodo contiene el nombre de nuestra red
    $IsConnected = docker inspect $KindNode --format '{{json .NetworkSettings.Networks}}' | Select-String $NetworkName
    
    if (-not $IsConnected) {
        Write-Log "   Conectando $KindNode a la red $NetworkName..." "Cyan"
        docker network connect $NetworkName $KindNode
        if ($LASTEXITCODE -ne 0) { throw "Fallo al ejecutar docker network connect" }
        Write-Log "   Conexion establecida exitosamente." "Green"
    } else {
        Write-Log "   El nodo ya esta conectado a la red." "Gray"
    }

    # Paso D: Verificación de IP (Para Debug)
    $NodeIP = docker inspect $KindNode --format "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}"
    Write-Log "   IP del Cluster Control Plane: $NodeIP" "DarkGray"
}
catch {
    Write-Log "Error CRITICO conectando redes: $_" "Red"
    Write-Log "Esto causara errores 500 en API Gateway. Revisa Docker." "Red"
    exit 1
}

Write-Log ">>> PLATAFORMA KUBERNETES LISTA <<<" "Green"