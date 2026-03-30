# KcELECTRA 데이터 준비/학습/평가/검증을 한 번에 실행하고 로그를 남기는 무인 실행 스크립트입니다.
[CmdletBinding()]
param(
    [string]$TrainXlsx = "",
    [string]$TrainJson = "",
    [string]$ValidXlsx = "",
    [string]$ValidJson = "",
    [string]$ConfigPath = "",
    [switch]$SkipPrepare,
    [switch]$SkipTrain,
    [switch]$SkipEvaluate,
    [switch]$SkipVerifyApi
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$TrainingRoot = Split-Path -Parent $ScriptDir
$ProjectRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $TrainingRoot))

$PythonExe = Join-Path $ProjectRoot "ai-api-fastapi\.venv\Scripts\python.exe"
$UvicornExe = Join-Path $ProjectRoot "ai-api-fastapi\.venv\Scripts\uvicorn.exe"
$PrepareScript = Join-Path $ScriptDir "prepare_emotion_dataset.py"
$TrainScript = Join-Path $ScriptDir "train_emotion_classifier.py"
$EvaluateScript = Join-Path $ScriptDir "evaluate_emotion_classifier.py"

$ProcessedDir = Join-Path $TrainingRoot "processed"
$ArtifactsDir = Join-Path $TrainingRoot "artifacts"
$EvaluationDir = Join-Path $ArtifactsDir "evaluation"
$LogsRoot = Join-Path $TrainingRoot "logs"
$CacheDir = Join-Path $ProjectRoot "ai-api-fastapi\.cache\huggingface"

$RunId = Get-Date -Format "yyyyMMdd-HHmmss"
$RunLogDir = Join-Path $LogsRoot $RunId
$StatusPath = Join-Path $RunLogDir "status.json"
$SummaryPath = Join-Path $RunLogDir "summary.txt"

New-Item -ItemType Directory -Force -Path $ProcessedDir, $ArtifactsDir, $EvaluationDir, $LogsRoot, $RunLogDir, $CacheDir | Out-Null
$env:HF_HOME = $CacheDir
$env:HF_HUB_DISABLE_SYMLINKS_WARNING = "1"

$TrainCsv = Join-Path $ProcessedDir "train_emotion_mvp.csv"
$ValidCsv = Join-Path $ProcessedDir "valid_emotion_mvp.csv"
$LabelMapPath = Join-Path $TrainingRoot "configs\label_map.json"
$BestModelDir = Join-Path $ArtifactsDir "best"
$EvalOutputPath = Join-Path $EvaluationDir "valid_metrics.json"
$VerifyOutputPath = Join-Path $RunLogDir "verify-api.json"

if (-not $ConfigPath) {
    $ConfigPath = Join-Path $TrainingRoot "configs\training_config_cpu.json"
}

$Status = [ordered]@{
    runId = $RunId
    startedAt = (Get-Date).ToString("s")
    currentStep = "initializing"
    steps = [ordered]@{
        prepareTrain = "pending"
        prepareValid = "pending"
        train = "pending"
        evaluate = "pending"
        verifyApi = "pending"
    }
    outputs = [ordered]@{
        trainCsv = $TrainCsv
        validCsv = $ValidCsv
        bestModelDir = $BestModelDir
        evaluationJson = $EvalOutputPath
        verifyJson = $VerifyOutputPath
        logsDir = $RunLogDir
    }
}

function Save-Status {
    $Status | ConvertTo-Json -Depth 6 | Set-Content -Path $StatusPath -Encoding UTF8
}

function Resolve-InputPath {
    param(
        [string]$ProvidedPath,
        [string]$FilterPattern
    )

    if ($ProvidedPath) {
        return $ProvidedPath
    }

    $oneDriveRoot = Join-Path $env:USERPROFILE "OneDrive"
    $match = Get-ChildItem -Path $oneDriveRoot -Recurse -File -Filter $FilterPattern |
        Sort-Object FullName |
        Select-Object -First 1

    if (-not $match) {
        throw "Could not resolve input file for pattern $FilterPattern"
    }

    return $match.FullName
}

function Invoke-LoggedStep {
    param(
        [Parameter(Mandatory = $true)][string]$StepName,
        [Parameter(Mandatory = $true)][string]$Executable,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$StdOutPath,
        [Parameter(Mandatory = $true)][string]$StdErrPath,
        [string]$WorkingDirectory = $ProjectRoot
    )

    $Status.currentStep = $StepName
    Save-Status

    $quotedArguments = $Arguments | ForEach-Object {
        if ($_ -match '[\s"]') {
            '"' + ($_ -replace '"', '\"') + '"'
        }
        else {
            $_
        }
    }

    $process = Start-Process `
        -FilePath $Executable `
        -ArgumentList ($quotedArguments -join ' ') `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $StdOutPath `
        -RedirectStandardError $StdErrPath `
        -NoNewWindow `
        -PassThru `
        -Wait

    if ($process.ExitCode -ne 0) {
        throw "$StepName failed with exit code $($process.ExitCode)"
    }
}

function Write-Summary {
    $lines = @(
        "runId=$RunId",
        "startedAt=$($Status.startedAt)",
        "finishedAt=$((Get-Date).ToString("s"))",
        "trainCsv=$TrainCsv",
        "validCsv=$ValidCsv",
        "bestModelDir=$BestModelDir",
        "evaluationJson=$EvalOutputPath",
        "verifyJson=$VerifyOutputPath",
        "logsDir=$RunLogDir"
    )
    $lines | Set-Content -Path $SummaryPath -Encoding UTF8
}

function Invoke-VerifyApi {
    $Status.currentStep = "verify-api"
    Save-Status

    $serverOut = Join-Path $RunLogDir "verify-api-server.out.log"
    $serverErr = Join-Path $RunLogDir "verify-api-server.err.log"
    $verifyLog = Join-Path $RunLogDir "verify-api.log"

    $server = Start-Process `
        -FilePath $UvicornExe `
        -ArgumentList @("app.main:app", "--host", "127.0.0.1", "--port", "8002") `
        -WorkingDirectory (Join-Path $ProjectRoot "ai-api-fastapi") `
        -RedirectStandardOutput $serverOut `
        -RedirectStandardError $serverErr `
        -NoNewWindow `
        -PassThru

    try {
        Start-Sleep -Seconds 8

        $emotionBody = '{"text":"요즘 회사 일 때문에 너무 지치고 불안해요.","returnTopK":3}'
        $diaryBody = '{"userId":1,"diaryId":101,"content":"오늘은 일이 많아서 너무 지치고 불안했어요.","writtenAt":"2026-03-27T22:00:00"}'

        $emotionResult = Invoke-RestMethod `
            -Method Post `
            -Uri "http://127.0.0.1:8002/internal/model/emotion-classify" `
            -ContentType "application/json; charset=utf-8" `
            -Body $emotionBody

        $diaryResult = Invoke-RestMethod `
            -Method Post `
            -Uri "http://127.0.0.1:8002/internal/ai/analyze-diary" `
            -ContentType "application/json; charset=utf-8" `
            -Body $diaryBody

        [ordered]@{
            checkedAt = (Get-Date).ToString("s")
            emotionClassify = $emotionResult
            analyzeDiary = $diaryResult
        } | ConvertTo-Json -Depth 6 | Set-Content -Path $VerifyOutputPath -Encoding UTF8

        "verify api completed" | Set-Content -Path $verifyLog -Encoding UTF8
    }
    finally {
        if ($server -and -not $server.HasExited) {
            Stop-Process -Id $server.Id -Force
        }
    }
}

Save-Status

try {
    $TrainXlsx = Resolve-InputPath -ProvidedPath $TrainXlsx -FilterPattern "*_Training.xlsx"
    $TrainJson = Resolve-InputPath -ProvidedPath $TrainJson -FilterPattern "*_Training.json"
    $ValidXlsx = Resolve-InputPath -ProvidedPath $ValidXlsx -FilterPattern "*_Validation.xlsx"
    $ValidJson = Resolve-InputPath -ProvidedPath $ValidJson -FilterPattern "*_Validation.json"

    if (-not $SkipPrepare) {
        Invoke-LoggedStep `
            -StepName "prepare-train" `
            -Executable $PythonExe `
            -Arguments @(
                $PrepareScript,
                "--xlsx", $TrainXlsx,
                "--json", $TrainJson,
                "--split", "train",
                "--output", $TrainCsv
            ) `
            -StdOutPath (Join-Path $RunLogDir "prepare-train.out.log") `
            -StdErrPath (Join-Path $RunLogDir "prepare-train.err.log")
        $Status.steps.prepareTrain = "completed"
        Save-Status

        Invoke-LoggedStep `
            -StepName "prepare-valid" `
            -Executable $PythonExe `
            -Arguments @(
                $PrepareScript,
                "--xlsx", $ValidXlsx,
                "--json", $ValidJson,
                "--split", "valid",
                "--output", $ValidCsv
            ) `
            -StdOutPath (Join-Path $RunLogDir "prepare-valid.out.log") `
            -StdErrPath (Join-Path $RunLogDir "prepare-valid.err.log")
        $Status.steps.prepareValid = "completed"
        Save-Status
    }
    else {
        $Status.steps.prepareTrain = "skipped"
        $Status.steps.prepareValid = "skipped"
        Save-Status
    }

    if (-not $SkipTrain) {
        Invoke-LoggedStep `
            -StepName "train" `
            -Executable $PythonExe `
            -Arguments @(
                $TrainScript,
                "--train-csv", $TrainCsv,
                "--valid-csv", $ValidCsv,
                "--config", $ConfigPath,
                "--label-map", $LabelMapPath,
                "--output-dir", $ArtifactsDir
            ) `
            -StdOutPath (Join-Path $RunLogDir "train.out.log") `
            -StdErrPath (Join-Path $RunLogDir "train.err.log")
        $Status.steps.train = "completed"
        Save-Status
    }
    else {
        $Status.steps.train = "skipped"
        Save-Status
    }

    if (-not $SkipEvaluate) {
        Invoke-LoggedStep `
            -StepName "evaluate" `
            -Executable $PythonExe `
            -Arguments @(
                $EvaluateScript,
                "--model-dir", $BestModelDir,
                "--input-csv", $ValidCsv,
                "--output-json", $EvalOutputPath
            ) `
            -StdOutPath (Join-Path $RunLogDir "evaluate.out.log") `
            -StdErrPath (Join-Path $RunLogDir "evaluate.err.log")
        $Status.steps.evaluate = "completed"
        Save-Status
    }
    else {
        $Status.steps.evaluate = "skipped"
        Save-Status
    }

    if (-not $SkipVerifyApi) {
        Invoke-VerifyApi
        $Status.steps.verifyApi = "completed"
        Save-Status
    }
    else {
        $Status.steps.verifyApi = "skipped"
        Save-Status
    }

    $Status.currentStep = "completed"
    $Status.finishedAt = (Get-Date).ToString("s")
    Save-Status
    Write-Summary
    Write-Host "KcELECTRA pipeline completed successfully. Logs: $RunLogDir"
}
catch {
    $Status.currentStep = "failed"
    $Status.failedAt = (Get-Date).ToString("s")
    $Status.error = $_.Exception.Message
    Save-Status
    Write-Summary
    Write-Error $_
    exit 1
}
