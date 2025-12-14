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
        
        if ($ExpectedStatus -ge 200 -and $ExpectedStatus -lt 300) {
            Write-Host "PASS (200)" -ForegroundColor Green
            return $Response
        } else {
            Write-Host "FAIL (Got 200, Expected $ExpectedStatus)" -ForegroundColor Red
            return $null
        }
    }
    catch {
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
                    Write-Host "   Server Error: $ErrorBody" -ForegroundColor DarkGray
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

# Extract Token based on your App's response format
# Usually it's in $LoginResponse.accessToken or $LoginResponse.token
$RealToken = $null

if ($LoginResponse) {
    if ($LoginResponse.accessToken) { $RealToken = $LoginResponse.accessToken }
    elseif ($LoginResponse.token) { $RealToken = $LoginResponse.token }
    else {
        # Fallback: Assume the whole response might be the token string
        $RealToken = $LoginResponse
    }
    Write-Host "   > Token Acquired" -ForegroundColor Gray
} else {
    Write-Error "Could not login. Aborting tests."
    exit 1
}

# ---------------------------------------------------------
# 2. Test Protected Endpoints
# ---------------------------------------------------------

# TEST B: No Token (Should Fail)
$ResB = Run-Test "Protected Access (No Token)" "$AppUrl/accounts" "GET" -ExpectedStatus 401

# TEST C: With Token (Should Pass)
$AuthHeaders = @{
    Authorization = "Bearer $RealToken"
}
$ResC = Run-Test "Protected Access (With Token)" "$AppUrl/accounts" "GET" -Headers $AuthHeaders -ExpectedStatus 200

Write-Host ""
if ($LoginResponse -and $ResB -and $ResC) {
    Write-Host "--- SMOKE TESTS PASSED ---" -ForegroundColor Green
} else {
    Write-Host "--- SMOKE TESTS FAILED ---" -ForegroundColor Red
}