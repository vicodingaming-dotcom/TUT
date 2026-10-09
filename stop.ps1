# stop.ps1 — Undeploy/Stop the Tutorial Island Bot
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
    name = "tutorialbot"
} | ConvertTo-Json

Write-Host "Stopping Tutorial Island Bot on port $Port..." -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:$Port/scripts/undeploy" -Headers $headers -ContentType "application/json" -Body $body
    Write-Host "Stopped and undeployed: $($response.plugin)" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Could not stop tutorialbot on port $Port." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}
