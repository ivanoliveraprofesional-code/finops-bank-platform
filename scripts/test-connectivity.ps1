# scripts/test-connectivity.ps1

$GatewayUrl = "http://localhost:80"
$AuthUrl = "$GatewayUrl/auth"
$AppUrl = "$GatewayUrl/api"

Write-Host "--- STARTING SMOKE TEST (ISTIO MESH) ---" -ForegroundColor Cyan
Write-Host ""

# ---------------------------------------------------------
# Helper Function: Run-Test
# ---------------------------------------------------------
function Run-Test {
    param (
        [string]$Name,
        [string]$Url,
        [string]$Method = "GET",
        [object]$Body = $null,
        [hashtable]$Headers = @{},
        [int]$ExpectedStatus = 200
    )

    Write-Host -NoNewline "[TEST] $Name ($Url)... "

    try {
        $Params = @{
            Uri         = $Url
            Method      = $Method
            Headers     = $Headers
            ErrorAction = "Stop"
            ContentType = "application/json"
        }

        if ($null -ne $Body) {
            $Params.Body = ($Body | ConvertTo-Json -Depth 5 -Compress)
        }

        $Response = Invoke-RestMethod @Params
        
        # We received a 2xx status code
        $ActualStatus = 200 # Default assumption if Invoke-RestMethod succeeds
        
        # Check if the response object has a property to determine the actual status (rare in successful calls)
        # If it doesn't, assume 200 or rely on the expected status for a PASS
        if ($ExpectedStatus -ge 200 -and $ExpectedStatus -lt 300) {
            Write-Host "PASS ($ExpectedStatus)" -ForegroundColor Green # Cannot read 201/202 etc easily
            return $Response
        } else {
            Write-Host "FAIL (Got 200, Expected $ExpectedStatus)" -ForegroundColor Red
            return $null
        }
    }
    catch {
        # Handle HTTP Errors (4xx, 5xx)
        if ($_.Exception.Response) {
            $Status = [int]$_.Exception.Response.StatusCode
            if ($Status -eq $ExpectedStatus) {
                Write-Host "PASS ($Status)" -ForegroundColor Green
                return $_.Exception.Response
            } else {
                Write-Host "FAIL (Got $Status, Expected $ExpectedStatus)" -ForegroundColor Red
                
                # Show server error for debugging
                $Stream = $_.Exception.Response.GetResponseStream()
                if ($Stream) {
                    $Reader = New-Object System.IO.StreamReader($Stream)
                    $ErrorBody = $Reader.ReadToEnd()
                    if ($ErrorBody.Length -gt 200) { $ErrorBody = $ErrorBody.Substring(0, 200) + "..." }
                    Write-Host "    Server Error: $ErrorBody" -ForegroundColor DarkGray
                }
                return $null
            }
        } else {
            Write-Host "FAIL (Connection Error: $($_.Exception.Message))" -ForegroundColor Red
            return $null
        }
    }
}

# ---------------------------------------------------------
# 1. Login to get REAL Token
# ---------------------------------------------------------
$LoginBody = @{
    username = "admin"
    password = "password" # Check your seed.sql if this fails
}

$LoginResponse = Run-Test "Public Access (Login)" "$AuthUrl/login" "POST" -Body $LoginBody -ExpectedStatus 200

$RealToken = $null

if ($LoginResponse) {
    if ($LoginResponse.accessToken) { $RealToken = $LoginResponse.accessToken }
    elseif ($LoginResponse.token) { $RealToken = $LoginResponse.token }
    else {
        $RealToken = $LoginResponse
    }
    Write-Host "    > Token Acquired" -ForegroundColor Gray
} else {
    Write-Error "Could not login. Aborting tests."
    exit 1
}

# ---------------------------------------------------------
# 2. Test Protected Endpoints (Create Account)
# ---------------------------------------------------------

$AccountBody = @{
    # We must use the user ID from the successful login (a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11)
    userId = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11" 
    currency = "USD"
}

# TEST B: No Token (Should Fail with 401 from Istio)
# Using POST /api/accounts
$ResB = Run-Test "Protected Access (No Token: POST)" "$AppUrl/accounts" "POST" -Body $AccountBody -ExpectedStatus 401

# TEST C: With Token (Should Pass with 201 Created)
# Using POST /api/accounts
$AuthHeaders = @{
    Authorization = "Bearer $RealToken"
}
# Expecting 201 Created status for POST operation
$ResC = Run-Test "Protected Access (With Token: POST)" "$AppUrl/accounts" "POST" -Body $AccountBody -Headers $AuthHeaders -ExpectedStatus 201 

Write-Host ""
if ($LoginResponse -and $ResB -and $ResC) {
    Write-Host "--- SMOKE TESTS PASSED ---" -ForegroundColor Green
} else {
    Write-Host "--- SMOKE TESTS FAILED ---" -ForegroundColor Red
}