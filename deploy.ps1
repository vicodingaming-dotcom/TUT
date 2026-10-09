# deploy.ps1 — Deploy and START the Tutorial Island Bot
param(
    [int]$Port = 8082
)

$token = "22ff212bce6345b4a78759e9e0605cc6"
$tokenFile = "$env:USERPROFILE\.runelite\.agent-token"
if (Test-Path $tokenFile) {
    $token = (Get-Content $tokenFile -Raw).Trim()
}

$headers = @{
    "X-Agent-Token" = $token
}

$body = @{
    name       = "tutorialbot"
    sourcePath = $PSScriptRoot
} | ConvertTo-Json

Write-Host "Deploying Tutorial Island Bot to port $Port..." -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:$Port/scripts/deploy/reload" -Headers $headers -ContentType "application/json" -Body $body
    Write-Host "Reloaded: $($response.plugin)" -ForegroundColor Green
} catch {
    try {
        $response = Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:$Port/scripts/deploy" -Headers $headers -ContentType "application/json" -Body $body
        Write-Host "Deployed: $($response.plugin)" -ForegroundColor Green
    } catch {
        Write-Host "ERROR: Could not deploy to port $Port. Please ensure the client has Agent Server enabled on port $Port." -ForegroundColor Red
        Write-Host $_.Exception.Message -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "Tutorial Island Bot is now RUNNING on port $Port." -ForegroundColor Green
