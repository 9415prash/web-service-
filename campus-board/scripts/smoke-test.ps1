param(
    [string]$BaseUrl = $(if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:18707" })
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd("/")
$suffix = [Guid]::NewGuid().ToString("N")
$password = "CampusBoard-Test-Password-2026!"
$emailA = "smoke-$suffix@example.test"
$emailB = "smoke-other-$suffix@example.test"
$created = $null
$tokenA = $null

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token,
        [object]$Body
    )

    $headers = @{}
    if ($Token) {
        $headers.Authorization = "Bearer $Token"
    }
    $parameters = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        Headers = $headers
        ErrorAction = "Stop"
    }
    if ($null -ne $Body) {
        $parameters.ContentType = "application/json"
        $parameters.Body = $Body | ConvertTo-Json -Compress
    }
    Invoke-RestMethod @parameters
}

Write-Host "Checking service health..."
$health = Invoke-Api -Method "GET" -Path "/actuator/health"
if ($health.status -ne "UP") {
    throw "Health check did not return UP."
}

Write-Host "Registering two temporary test accounts..."
$userA = Invoke-Api -Method "POST" -Path "/api/auth/register" -Body @{
    name = "Smoke Test Author"
    email = $emailA
    password = $password
}
$null = Invoke-Api -Method "POST" -Path "/api/auth/register" -Body @{
    name = "Smoke Test Visitor"
    email = $emailB
    password = $password
}
$tokenA = (Invoke-Api -Method "POST" -Path "/api/auth/login" -Body @{
    email = $emailA
    password = $password
}).accessToken
$tokenB = (Invoke-Api -Method "POST" -Path "/api/auth/login" -Body @{
    email = $emailB
    password = $password
}).accessToken

$profile = Invoke-Api -Method "GET" -Path "/api/me" -Token $tokenA
if ($profile.id -ne $userA.id) {
    throw "The current-user endpoint returned the wrong account."
}

try {
    $created = Invoke-Api -Method "POST" -Path "/api/posts" -Token $tokenA -Body @{
        title = "Smoke test $suffix"
        content = "Temporary post used to verify owner-only post permissions."
    }
    if ($created.ownerId -ne $userA.id) {
        throw "The post owner did not match the authenticated user."
    }

    $publicPosts = Invoke-Api -Method "GET" -Path "/api/posts"
    if (-not ($publicPosts | Where-Object { $_.id -eq $created.id })) {
        throw "The new post was not visible in the public post list."
    }

    try {
        $null = Invoke-Api -Method "PUT" -Path "/api/posts/$($created.id)" -Token $tokenB -Body @{
            title = "Unauthorized edit"
            content = "This edit must be rejected."
        }
        throw "A different account unexpectedly edited the post."
    }
    catch {
        $response = $_.Exception.Response
        if ($null -eq $response -or [int]$response.StatusCode -ne 403) {
            throw
        }
    }

    $updated = Invoke-Api -Method "PUT" -Path "/api/posts/$($created.id)" -Token $tokenA -Body @{
        title = "Smoke test updated $suffix"
        content = "Owner updates are permitted."
    }
    if ($updated.title -ne "Smoke test updated $suffix") {
        throw "The owner could not update their post."
    }

    Invoke-Api -Method "DELETE" -Path "/api/posts/$($created.id)" -Token $tokenA | Out-Null
    $created = $null
    Write-Host "PASS: health, registration, login, current user, public posts, and owner permissions."
    Write-Host "Created test accounts: $emailA and $emailB"
}
finally {
    if ($null -ne $created -and $tokenA) {
        try {
            Invoke-Api -Method "DELETE" -Path "/api/posts/$($created.id)" -Token $tokenA | Out-Null
        }
        catch {
            Write-Warning "Could not clean up smoke-test post $($created.id): $($_.Exception.Message)"
        }
    }
}
