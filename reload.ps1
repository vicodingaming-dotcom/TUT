# reload.ps1 — Hot Reload the Tutorial Island Bot
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

Write-Host "Reloading Tutorial Island Bot on port $Port..." -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:$Port/scripts/deploy/reload" -Headers $headers -ContentType "application/json" -Body $body
    Write-Host "Reloaded successfully: $($response.plugin)" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Could not reload on port $Port." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}
