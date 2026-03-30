# TIRED 실험을 자정 전까지 무인으로 점검하고 필요할 때만 자동 실행하는 러너입니다.
param(
    [string]$PythonExe = "C:\programing\mindcompass\ai-api-fastapi\.venv\Scripts\python.exe",
    [string]$WorkspaceRoot = "C:\programing\mindcompass",
    [string]$RunPrefix = "cpu_compare_tired_seed_auto_v2_unattended",
    [int]$SleepMinutes = 60
)

$ErrorActionPreference = "Stop"

$scriptDir = "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\scripts"
$evaluationDir = "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\evaluation"
$summaryJson = Join-Path $evaluationDir "tired_experiment_summary.json"
$summaryMd = Join-Path $evaluationDir "tired_experiment_summary.md"
$statusDir = "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\artifacts\unattended"
$statusFile = Join-Path $statusDir "tired_unattended_status.json"
$logFile = Join-Path $statusDir "tired_unattended_runner.log"

New-Item -ItemType Directory -Force -Path $statusDir | Out-Null

function Write-Log {
    param([string]$Message)
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $line = "[$timestamp] $Message"
    Add-Content -Path $logFile -Value $line -Encoding UTF8
    Write-Output $line
}

function Write-Status {
    param(
        [string]$Phase,
        [string]$Recommendation,
        [string]$LastRunName,
        [string]$Message
    )
    $status = [ordered]@{
        updated_at = (Get-Date).ToString("s")
        phase = $Phase
        recommendation = $Recommendation
        last_run_name = $LastRunName
        message = $Message
        summary_json = $summaryJson
        summary_md = $summaryMd
        log_file = $logFile
    }
    $status | ConvertTo-Json -Depth 4 | Set-Content -Path $statusFile -Encoding UTF8
}

function Refresh-Summary {
    & $PythonExe (Join-Path $scriptDir "summarize_tired_experiments.py") `
        --evaluation-dir $evaluationDir `
        --output-md $summaryMd `
        --output-json $summaryJson
}

function Read-Recommendation {
    if (-not (Test-Path $summaryJson)) {
        return ""
    }
    $summary = Get-Content -Path $summaryJson -Raw -Encoding UTF8 | ConvertFrom-Json
    return [string]$summary.recommendation
}

$now = Get-Date
$deadline = $now.Date.AddDays(1)
$lastRunName = ""

Write-Log "unattended runner start"
Write-Status -Phase "starting" -Recommendation "" -LastRunName "" -Message "runner initialized"

while ((Get-Date) -lt $deadline) {
    Refresh-Summary
    $recommendation = Read-Recommendation
    Write-Log "current recommendation: $recommendation"

    if ($recommendation -like "blind-expansion-stop*") {
        Write-Status -Phase "stopped_by_summary" -Recommendation $recommendation -LastRunName $lastRunName -Message "summary says unattended blind expansion should stop"
        Write-Log "runner stopped because summary recommended stop"
        break
    }

    $runName = "{0}_{1}" -f $RunPrefix, (Get-Date -Format "yyyyMMdd-HHmmss")
    Write-Log "triggering unattended run: $runName"
    Write-Status -Phase "running_pipeline" -Recommendation $recommendation -LastRunName $runName -Message "starting unattended tired pipeline"

    & $PythonExe (Join-Path $scriptDir "run_tired_seed_auto_pipeline.py") `
        --seed-version auto_v2 `
        --run-name $runName

    $lastRunName = $runName
    Refresh-Summary
    $recommendation = Read-Recommendation
    Write-Status -Phase "sleeping" -Recommendation $recommendation -LastRunName $lastRunName -Message "pipeline finished; waiting for next check"
    Write-Log "sleeping for $SleepMinutes minutes"
    Start-Sleep -Seconds ($SleepMinutes * 60)
}

if ((Get-Date) -ge $deadline) {
    $recommendation = Read-Recommendation
    Write-Status -Phase "deadline_reached" -Recommendation $recommendation -LastRunName $lastRunName -Message "midnight deadline reached"
    Write-Log "deadline reached"
}
