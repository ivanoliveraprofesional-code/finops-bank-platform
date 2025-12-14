<#
.SYNOPSIS
    End-to-End Connectivity Test (Istio Mesh).
    Target: localhost:80 (Kind Host Port)
#>
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

# SSL Fix
if ([System.Net.ServicePointManager]::SecurityProtocol -notcontains 'Tls12') {
    [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor [System.Net.SecurityProtocolType]::Tls12
}
[System.Net.ServicePointManager]::ServerCertificateValidationCallback = {$true}

Write-Host "--- STARTING SMOKE TEST (ISTIO MESH) ---" -ForegroundColor Cyan

# 1. Get Private Key (To Sign Test Token)
Write-Host "1. Fetching Private Key from Terraform..." -ForegroundColor Yellow
Push-Location "$PSScriptRoot/../terraform"
try {
    # We still need the key to generate a valid JWT for the test
    $PrivateKey = terraform output -raw jwt_private_key_pkcs8
}
finally { Pop-Location }

if (-not $PrivateKey) { throw "Terraform did not return jwt_private_key_pem." }

# 2. Generate JWT (Using Python)
Write-Host "2. Generating Admin JWT..." -ForegroundColor Yellow
$TempPyFile = [System.IO.Path]::GetTempFileName() + ".py"
$Env:KEY = $PrivateKey

# Python script to sign JWT using the Terraform Key
$PyCode = @"
import jwt, time, os, sys
key = os.environ.get('KEY')
payload = {
    'sub': 'smoke-test-admin', 
    'roles': ['ADMIN'], 
    'iat': int(time.time()), 
    'exp': int(time.time()) + 300
}
try:
    print(jwt.encode(payload, key, algorithm='RS256'))
except Exception as e:
    sys.exit(str(e))
"@
Set-Content $TempPyFile $PyCode

try {
    $Token = python $TempPyFile
    if ($LASTEXITCODE -ne 0) { throw "Python JWT Generation Failed" }
}
finally { Remove-Item $TempPyFile }

Write-Host "   Token Generated." -ForegroundColor Gray

# 3. Execution (The Tests)
$BaseUrl = "http://localhost:80" # Istio Gateway
$AllPassed = $true

function Run-Test ($Name, $Url, $Method, $Headers, $ExpectedCode) {
    Write-Host "`n[TEST] $Name ($Url)..." -NoNewline
    try {
        $Params = @{ Uri = $Url; Method = $Method; UseBasicParsing = $true; ErrorAction = "Stop" }
        if ($Headers) { $Params.Headers = $Headers }
        
        $Resp = Invoke-WebRequest @Params
        $Code = $Resp.StatusCode
    }
    catch {
        if ($_.Exception.Response) { $Code = $_.Exception.Response.StatusCode.value__ }
        else { $Code = 500; Write-Error $_ }
    }

    if ($Code -eq $ExpectedCode) {
        Write-Host " PASS ($Code)" -ForegroundColor Green
        return $true
    } else {
        Write-Host " FAIL (Got $Code, Expected $ExpectedCode)" -ForegroundColor Red
        return $false
    }
}

# TEST A: Public Endpoint (Auth Service - Login) - No Token Needed
# Should return 400 or 401 because body is empty, but 200/400 proves connectivity
$ResA = Run-Test "Public Access (Auth)" "$BaseUrl/auth/login" "POST" @{} 400 
if (-not $ResA) { $AllPassed = $false }

# TEST B: Protected Endpoint (Core Banking) - NO TOKEN
# Istio RequestAuthentication should block this with 401
$ResB = Run-Test "Protected Access (No Token)" "$BaseUrl/api/accounts" "GET" @{} 401
if (-not $ResB) { $AllPassed = $false }

# TEST C: Protected Endpoint (Core Banking) - WITH TOKEN
# Istio should validate JWT and Java should return 200/404 (Business Logic)
# 404 is acceptable here (Account doesn't exist), getting past 401 is the goal.
$ResC = Run-Test "Protected Access (With Token)" "$BaseUrl/api/accounts" "GET" @{ "Authorization" = "Bearer $Token" } 200
if (-not $ResC) { 
    # Accept 200 (Empty List) or 404 (Not Found) or 500 (DB Error)
    # If we get 401/403 here, the test FAILED.
    Write-Host "   (Note: If this failed with 500, check Database Logs)" -ForegroundColor Gray
    $AllPassed = $false 
}

if ($AllPassed) {
    Write-Host "`n--- SMOKE TESTS PASSED ---" -ForegroundColor Green
} else {
    Write-Host "`n--- SMOKE TESTS FAILED ---" -ForegroundColor Red
    exit 1
}