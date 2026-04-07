. "$PSScriptRoot\lib\framepilot-common.ps1"

$repoRoot = Get-FramePilotRoot
Use-FramePilotJava
Set-Location $repoRoot

$server = $null
$startedServer = $false

function Test-BackendHealth {
    try {
        $health = Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method Get -TimeoutSec 3
        return $health -eq "framepilot-ai-local-server-ok"
    } catch {
        return $false
    }
}

if (-not (Test-BackendHealth)) {
    $serverCommand = "Set-Location '$repoRoot'; .\gradlew.bat :apps:local-server:bootRun"
    $server = Start-Process powershell.exe -ArgumentList @("-NoProfile", "-Command", $serverCommand) -PassThru -WindowStyle Hidden
    $startedServer = $true
}

try {
    Write-Section "Wait for backend health"
    $ready = $false
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 2
        if (Test-BackendHealth) {
            $ready = $true
            break
        }
    }
    if (-not $ready) {
        throw "Backend did not become healthy in time."
    }

    Write-Section "Run smoke flow"
    $scan = Invoke-RestMethod -Uri "http://localhost:8080/api/device/scan" -Method Post
    $benchmark = Invoke-RestMethod -Uri "http://localhost:8080/api/device/benchmark" -Method Post -ContentType "application/json" -Body (@{ scanId = $scan.scanId } | ConvertTo-Json)
    $recommendation = Invoke-RestMethod -Uri "http://localhost:8080/api/device/recommendation?scanId=$($scan.scanId)&benchmarkId=$($benchmark.benchmarkId)" -Method Get
    $project = Invoke-RestMethod -Uri "http://localhost:8080/api/projects/import-demo" -Method Post
    Invoke-RestMethod -Uri "http://localhost:8080/api/projects/$($project.id)/panels/parse" -Method Post | Out-Null
    Invoke-RestMethod -Uri "http://localhost:8080/api/projects/$($project.id)/ocr/run" -Method Post | Out-Null
    $analysis = Invoke-RestMethod -Uri "http://localhost:8080/api/projects/$($project.id)/analysis/summary" -Method Post
    Invoke-RestMethod -Uri "http://localhost:8080/api/projects/$($project.id)/scene-plan" -Method Post | Out-Null
    $render = Invoke-RestMethod -Uri "http://localhost:8080/api/projects/$($project.id)/render/start" -Method Post
    $final = $null
    for ($i = 0; $i -lt 120; $i++) {
        Start-Sleep -Seconds 1
        $status = Invoke-RestMethod -Uri "http://localhost:8080/api/render/jobs/$($render.jobId)" -Method Get
        if ($status.status -in @("COMPLETED", "COMPLETED_WITH_FALLBACK", "FAILED", "CANCELLED")) {
            $final = $status
            break
        }
    }
    if ($null -eq $final) {
        throw "Render job did not finish in time."
    }

    [pscustomobject]@{
        scanId = $scan.scanId
        benchmarkId = $benchmark.benchmarkId
        preset = $recommendation.preset.preset
        projectId = $project.id
        pipeline = $analysis.pipelineType
        renderJobId = $render.jobId
        renderStatus = $final.status
        artifactPath = $final.artifactPath
    } | ConvertTo-Json -Depth 5
}
finally {
    if ($startedServer) {
        Get-CimInstance Win32_Process |
            Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -like '*com.framepilotai.server.FramePilotAiApplication*' } |
            ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
        if ($server -and -not $server.HasExited) {
            Stop-Process -Id $server.Id -Force -ErrorAction SilentlyContinue
        }
    }
}
