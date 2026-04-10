# ============================================================================
# validate-security.ps1 - Valida correcoes de seguranca do SecureShop
#
# Uso: .\validate-security.ps1 [URL_BASE]
#      Default: http://localhost:4000
# ============================================================================

param([string]$Api = "http://localhost:4000")

$Pass = 0
$Fail = 0
$Total = 8

Write-Host ""
Write-Host "===========================================================" -ForegroundColor White
Write-Host "  SecureShop - Validacao de Seguranca                       " -ForegroundColor White
Write-Host "===========================================================" -ForegroundColor White

try {
    $LoginResult = Invoke-RestMethod -Uri "$Api/api/auth/login" -Method POST `
        -ContentType "application/json" `
        -Body '{"email":"carlos@secureshop.com","password":"Senha123"}'
    $Token = $LoginResult.token
} catch {
    Write-Host "  ERRO: Nao foi possivel obter token de login" -ForegroundColor Red
    Write-Host "===========================================================" -ForegroundColor White
    exit 1
}

$Headers = @{ "Authorization" = "Bearer $Token"; "Content-Type" = "application/json" }

function Check {
    param([string]$Num, [string]$Name, [bool]$Passed)
    $Padded = $Name.PadRight(43)
    if ($Passed) {
        Write-Host "  V$Num  $Padded " -NoNewline; Write-Host "PASS" -ForegroundColor Green
        $script:Pass++
    } else {
        Write-Host "  V$Num  $Padded " -NoNewline; Write-Host "FAIL" -ForegroundColor Red
        $script:Fail++
    }
}

# -- V1: SQL Injection Login --------------------------------------------------
try {
    $SqliLogin = Invoke-RestMethod -Uri "$Api/api/auth/login" -Method POST `
        -ContentType "application/json" `
        -Body '{"email":"admin@secureshop.com","password":"'' OR ''1''=''1'' --"}'
    Check "1" "SQL Injection Login" ($null -eq $SqliLogin.token)
} catch {
    Check "1" "SQL Injection Login" $true
}

# -- V2: Price Manipulation ---------------------------------------------------
try {
    $Result = Invoke-RestMethod -Uri "$Api/api/orders" -Method POST -Headers $Headers `
        -Body '{"items":[{"product_id":1,"quantity":1,"price":0.01}],"total":0.01}'
    $OrderTotal = $Result.order.total
    Check "2" "Price Manipulation" ($OrderTotal -gt 100)
} catch {
    Check "2" "Price Manipulation" $true
}

# -- V3: IDOR Users -----------------------------------------------------------
try {
    $null = Invoke-RestMethod -Uri "$Api/api/users/1" -Headers $Headers
    Check "3" "IDOR Users" $false
} catch {
    $StatusCode = $_.Exception.Response.StatusCode.value__
    Check "3" "IDOR Users" ($StatusCode -eq 403 -or $StatusCode -eq 401)
}

# -- V4: Mass Assignment ------------------------------------------------------
$Rand = Get-Random
try {
    $RegResult = Invoke-RestMethod -Uri "$Api/api/auth/register" -Method POST `
        -ContentType "application/json" `
        -Body "{`"nome`":`"Test$Rand`",`"email`":`"test${Rand}@test.com`",`"password`":`"test123`",`"role`":`"admin`"}"
    $Role = $RegResult.user.role
    Check "4" "Mass Assignment" ($Role -eq "user" -or $null -eq $Role)
} catch {
    Check "4" "Mass Assignment" $true
}

# -- V5: Stored XSS -----------------------------------------------------------
$Rand2 = Get-Random
try {
    $XssResult = Invoke-RestMethod -Uri "$Api/api/products/1/reviews" -Method POST -Headers $Headers `
        -Body "{`"text`":`"<script>alert($Rand2)</script>`",`"rating`":5}"
    $StoredText = $XssResult.review.text
    Check "5" "Stored XSS" (-not ($StoredText -match "<script>"))
} catch {
    Check "5" "Stored XSS" $true
}

# -- V6: Error Disclosure + Path Traversal -------------------------------------
$ForgotOk = $true
$TraversalOk = $true

try {
    $ForgotResult = Invoke-WebRequest -Uri "$Api/api/auth/forgot-password" -Method POST `
        -ContentType "application/json" `
        -Body '{"email":"admin@secureshop.com"}' -ErrorAction SilentlyContinue
    if ($ForgotResult.Content -match "stack") { $ForgotOk = $false }
} catch {
    $ErrBody = $_.ErrorDetails.Message
    if ($ErrBody -match "stack") { $ForgotOk = $false }
}

try {
    $TraversalResult = Invoke-RestMethod -Uri "$Api/api/export/..%2Fpackage.json" -Headers $Headers
    $TraversalJson = $TraversalResult | ConvertTo-Json
    if ($TraversalJson -match "secureshop-api") { $TraversalOk = $false }
} catch {
    $TraversalOk = $true
}

Check "6" "Error Disclosure + Path Traversal" ($ForgotOk -and $TraversalOk)

# -- V7: CORS Wildcard --------------------------------------------------------
try {
    $CorsResponse = Invoke-WebRequest -Uri "$Api/api/products" -Method GET -ErrorAction SilentlyContinue
    $CorsHeader = $CorsResponse.Headers["Access-Control-Allow-Origin"]
    $IsWildcard = $CorsHeader -eq "*"
    Check "7" "CORS Wildcard" (-not $IsWildcard)
} catch {
    Check "7" "CORS Wildcard" $true
}

# -- V8: SQL Injection Search -------------------------------------------------
try {
    $SqliSearch = Invoke-RestMethod -Uri "$Api/api/products/search?q=a'%20UNION%20SELECT%20id,email,password,role,nome,created_at%20FROM%20users--"
    $SearchJson = $SqliSearch | ConvertTo-Json
    Check "8" "SQL Injection Search" (-not ($SearchJson -match "admin@secureshop.com"))
} catch {
    Check "8" "SQL Injection Search" $true
}

# -- Resultado -----------------------------------------------------------------
Write-Host "===========================================================" -ForegroundColor White
if ($Pass -eq $Total) {
    Write-Host "  Resultado: " -NoNewline; Write-Host "$Pass/$Total PASS" -ForegroundColor Green
} else {
    Write-Host "  Resultado: " -NoNewline
    Write-Host "$Pass PASS" -ForegroundColor Green -NoNewline
    Write-Host " / " -NoNewline
    Write-Host "$Fail FAIL" -ForegroundColor Red
}
Write-Host "===========================================================" -ForegroundColor White
Write-Host ""
