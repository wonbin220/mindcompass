# KcELECTRA HAPPY/CALM 단일-knob 실험을 2026-03-30 21:30 KST까지 자동 점검하고 이어서 수행하는 스크립트입니다.
[CmdletBinding()]
param(
    [string]$CutoffLocalIso = "2026-03-30T21:30:00"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$TrainingRoot = Split-Path -Parent $ScriptDir
$ProjectRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $TrainingRoot))

$PythonExe = Join-Path $ProjectRoot "ai-api-fastapi\.venv\Scripts\python.exe"
$TrainScript = Join-Path $ScriptDir "train_emotion_classifier.py"
$EvaluateScript = Join-Path $ScriptDir "evaluate_emotion_classifier.py"

$ConfigsDir = Join-Path $TrainingRoot "configs"
$ProcessedDir = Join-Path $TrainingRoot "processed"
$ArtifactsDir = Join-Path $TrainingRoot "artifacts"
$EvaluationDir = Join-Path $ArtifactsDir "evaluation"
$LogsRoot = Join-Path $TrainingRoot "logs\unattended"
$StatusDocPath = Join-Path $ProjectRoot "docs\IMPLEMENTATION_STATUS.md"
$SummaryCsvPath = Join-Path $EvaluationDir "happy_calm_fixed_compare_summary.csv"
$TrainCsvPath = Join-Path $ProcessedDir "train_emotion_mvp_manual_seed_v2_fixed_compare_medium.csv"
$ValidCsvPath = Join-Path $ProcessedDir "valid_emotion_mvp_manual_seed_v2_fixed_compare_medium.csv"
$LabelMapPath = Join-Path $ConfigsDir "label_map.json"
$BaseConfigPath = Join-Path $ConfigsDir "training_config_cpu_happy_calm_guard_metric_label_smoothing_hidden_dropout20_v1.json"
$HfCacheDir = Join-Path $ProjectRoot "ai-api-fastapi\.cache\huggingface"
$BaselineExperimentName = "cpu_compare_medium_relabel_weighted"
$StrongestLocalReferenceExperimentName = "cpu_compare_medium_manual_seed_v2_fixed_compare_happy_calm_guard_metric_label_smoothing_hidden_dropout20_happy_to_calm_penalty02_v1"

$RunId = Get-Date -Format "yyyyMMdd-HHmmss"
$RunDir = Join-Path $LogsRoot $RunId
$RunLogPath = Join-Path $RunDir "run.log"
$SummaryPath = Join-Path $RunDir "summary.txt"
$StatePath = Join-Path $RunDir "state.json"

New-Item -ItemType Directory -Force -Path $RunDir, $EvaluationDir, $HfCacheDir | Out-Null
$env:HF_HOME = $HfCacheDir
$env:HF_HUB_DISABLE_SYMLINKS_WARNING = "1"

$script:AttemptedTrainNames = [System.Collections.Generic.HashSet[string]]::new()
$script:AttemptedEvalNames = [System.Collections.Generic.HashSet[string]]::new()
$script:SessionRecords = New-Object System.Collections.Generic.List[object]
$script:FailureRecords = New-Object System.Collections.Generic.List[object]
$script:LoopCounter = 0
$script:SummaryWritten = $false

function Write-RunLog {
    param(
        [Parameter(Mandatory = $true)][string]$Message,
        [ValidateSet("INFO", "WARN", "ERROR")][string]$Level = "INFO"
    )

    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $line = "[{0}] [{1}] {2}" -f $timestamp, $Level, $Message
    $line | Tee-Object -FilePath $RunLogPath -Append | Out-Null
}

function Save-State {
    $state = [ordered]@{
        runId = $RunId
        updatedAt = (Get-Date).ToString("s")
        loopCounter = $script:LoopCounter
        sessionRecords = @($script:SessionRecords.ToArray())
        failures = @($script:FailureRecords.ToArray())
        summaryPath = $SummaryPath
        runLogPath = $RunLogPath
    }
    $state | ConvertTo-Json -Depth 8 | Set-Content -Path $StatePath -Encoding UTF8
}

function Get-SeoulNow {
    $tz = [System.TimeZoneInfo]::FindSystemTimeZoneById("Korea Standard Time")
    return [System.TimeZoneInfo]::ConvertTime((Get-Date), $tz)
}

function Get-Cutoff {
    $parsed = [datetime]::Parse($CutoffLocalIso, [System.Globalization.CultureInfo]::InvariantCulture)
    return [datetime]::SpecifyKind($parsed, [System.DateTimeKind]::Unspecified)
}

function Get-RemainingMinutes {
    $now = Get-SeoulNow
    $cutoff = Get-Cutoff
    return [math]::Floor(($cutoff - $now).TotalMinutes)
}

function Read-TextFile {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        return ""
    }
    return Get-Content -LiteralPath $Path -Raw -Encoding UTF8
}

function Read-SummaryRows {
    if (-not (Test-Path -LiteralPath $SummaryCsvPath)) {
        return @()
    }
    return Import-Csv -LiteralPath $SummaryCsvPath -Encoding UTF8
}

function Get-SummaryExperimentSet {
    $set = [System.Collections.Generic.HashSet[string]]::new()
    foreach ($row in Read-SummaryRows) {
        if ($row.experiment) {
            [void]$set.Add([string]$row.experiment)
        }
    }
    return $set
}

function Get-SummaryRowByExperiment {
    param([Parameter(Mandatory = $true)][string]$ExperimentName)

    foreach ($row in Read-SummaryRows) {
        if ($row.experiment -eq $ExperimentName) {
            return $row
        }
    }

    return $null
}

function Convert-ConfigNameToExperimentName {
    param([Parameter(Mandatory = $true)][string]$ConfigFileName)

    if ($ConfigFileName -notmatch '^training_config_cpu_(.+)\.json$') {
        return $null
    }

    return "cpu_compare_medium_manual_seed_v2_fixed_compare_{0}" -f $Matches[1]
}

function Get-EvaluationJsonPathForExperiment {
    param([Parameter(Mandatory = $true)][string]$ExperimentName)
    return Join-Path $EvaluationDir ("valid_metrics_{0}.json" -f $ExperimentName)
}

function Get-ArtifactDirForExperiment {
    param([Parameter(Mandatory = $true)][string]$ExperimentName)
    return Join-Path $ArtifactsDir $ExperimentName
}

function New-CandidatePlan {
    param(
        [Parameter(Mandatory = $true)][double]$PenaltyWeight,
        [Parameter(Mandatory = $true)][string]$PenaltyCode
    )

    $suffix = "happy_calm_guard_metric_label_smoothing_hidden_dropout20_happy_to_calm_penalty{0}_v1" -f $PenaltyCode
    $configName = "training_config_cpu_{0}.json" -f $suffix
    $experimentName = "cpu_compare_medium_manual_seed_v2_fixed_compare_{0}" -f $suffix

    [pscustomobject]@{
        kind = "penalty"
        knob = "happy_to_calm_penalty_weight"
        knobValue = $PenaltyWeight
        penaltyCode = $PenaltyCode
        configName = $configName
        configPath = Join-Path $ConfigsDir $configName
        experimentName = $experimentName
        artifactDir = Get-ArtifactDirForExperiment -ExperimentName $experimentName
        bestModelDir = Join-Path (Get-ArtifactDirForExperiment -ExperimentName $experimentName) "best"
        evaluationJsonPath = Get-EvaluationJsonPathForExperiment -ExperimentName $experimentName
        source = "hidden_dropout20_reference"
        reason = "hidden_dropout=0.2 strongest local reference 유지, penalty 작은 값 우선"
    }
}

function New-BidirectionalCandidatePlan {
    param(
        [Parameter(Mandatory = $true)][double]$PenaltyWeight,
        [Parameter(Mandatory = $true)][string]$PenaltyCode
    )

    $suffix = "happy_calm_guard_metric_label_smoothing_hidden_dropout20_bidirectional_penalty{0}_v1" -f $PenaltyCode
    $configName = "training_config_cpu_{0}.json" -f $suffix
    $experimentName = "cpu_compare_medium_manual_seed_v2_fixed_compare_{0}" -f $suffix

    [pscustomobject]@{
        kind = "bidirectional_penalty"
        knob = "happy_calm_bidirectional_penalty_weight"
        knobValue = $PenaltyWeight
        penaltyCode = $PenaltyCode
        configName = $configName
        configPath = Join-Path $ConfigsDir $configName
        experimentName = $experimentName
        artifactDir = Get-ArtifactDirForExperiment -ExperimentName $experimentName
        bestModelDir = Join-Path (Get-ArtifactDirForExperiment -ExperimentName $experimentName) "best"
        evaluationJsonPath = Get-EvaluationJsonPathForExperiment -ExperimentName $experimentName
        source = "hidden_dropout20_bidirectional_family"
        reason = "one-sided penalty family 종료 후 HAPPY/CALM 양방향 경계 붕괴를 동시에 억제하는 balanced loss 후보"
    }
}

function Get-CandidateQueue {
    return @(
        (New-CandidatePlan -PenaltyWeight 0.015 -PenaltyCode "015"),
        (New-CandidatePlan -PenaltyWeight 0.01 -PenaltyCode "01"),
        (New-CandidatePlan -PenaltyWeight 0.005 -PenaltyCode "005"),
        (New-BidirectionalCandidatePlan -PenaltyWeight 0.0025 -PenaltyCode "0025"),
        (New-BidirectionalCandidatePlan -PenaltyWeight 0.005 -PenaltyCode "005"),
        (New-BidirectionalCandidatePlan -PenaltyWeight 0.0075 -PenaltyCode "0075")
    )
}

function Ensure-CandidateConfig {
    param([Parameter(Mandatory = $true)]$Candidate)

    if (Test-Path -LiteralPath $Candidate.configPath) {
        return
    }

    if (-not (Test-Path -LiteralPath $BaseConfigPath)) {
        throw "base config not found: $BaseConfigPath"
    }

    $baseConfig = Get-Content -LiteralPath $BaseConfigPath -Raw -Encoding UTF8 | ConvertFrom-Json
    $ordered = [ordered]@{}
    foreach ($property in $baseConfig.PSObject.Properties) {
        $ordered[$property.Name] = $property.Value
    }
    $ordered["happy_to_calm_penalty_weight"] = $Candidate.knobValue

    ($ordered | ConvertTo-Json -Depth 10) | Set-Content -LiteralPath $Candidate.configPath -Encoding UTF8
    Write-RunLog "created config $($Candidate.configName) with happy_to_calm_penalty_weight=$($Candidate.knobValue)"
}

function Get-IncompleteEvaluationPlans {
    $plans = New-Object System.Collections.Generic.List[object]
    $summarySet = Get-SummaryExperimentSet

    foreach ($configFile in Get-ChildItem -LiteralPath $ConfigsDir -File -Filter 'training_config_cpu_*.json') {
        $experimentName = Convert-ConfigNameToExperimentName -ConfigFileName $configFile.Name
        if (-not $experimentName) {
            continue
        }

        $artifactDir = Get-ArtifactDirForExperiment -ExperimentName $experimentName
        $bestModelDir = Join-Path $artifactDir "best"
        $evaluationJsonPath = Get-EvaluationJsonPathForExperiment -ExperimentName $experimentName

        if (-not (Test-Path -LiteralPath $artifactDir)) {
            continue
        }
        if (-not (Test-Path -LiteralPath $bestModelDir)) {
            continue
        }
        if (Test-Path -LiteralPath $evaluationJsonPath) {
            continue
        }

        $plans.Add([pscustomobject]@{
            configName = $configFile.Name
            configPath = $configFile.FullName
            experimentName = $experimentName
            artifactDir = $artifactDir
            bestModelDir = $bestModelDir
            evaluationJsonPath = $evaluationJsonPath
            alreadyInSummary = $summarySet.Contains($experimentName)
            mode = "evaluate-only"
        }) | Out-Null
    }

    return @($plans.ToArray())
}

function Get-MetricsFromEvaluation {
    param([Parameter(Mandatory = $true)][string]$EvaluationJsonPath)

    $json = Get-Content -LiteralPath $EvaluationJsonPath -Raw -Encoding UTF8 | ConvertFrom-Json
    $report = $json.classification_report
    $focused = $json.focused_metrics

    return [ordered]@{
        accuracy = [double]$report.accuracy
        macro_f1 = [double]$report.'macro avg'.'f1-score'
        happy_f1 = if ($report.HAPPY) { [double]$report.HAPPY.'f1-score' } else { 0.0 }
        calm_f1 = if ($report.CALM) { [double]$report.CALM.'f1-score' } else { 0.0 }
        happy_calm_macro_f1 = if ($focused.happy_calm_macro_f1) { [double]$focused.happy_calm_macro_f1 } else { 0.0 }
        happy_to_calm_count = if ($focused.happy_to_calm_count -ne $null) { [int]$focused.happy_to_calm_count } else { 0 }
        happy_to_calm_rate = if ($focused.happy_to_calm_rate -ne $null) { [double]$focused.happy_to_calm_rate } else { 0.0 }
        calm_to_happy_count = if ($focused.calm_to_happy_count -ne $null) { [int]$focused.calm_to_happy_count } else { 0 }
        calm_to_happy_rate = if ($focused.calm_to_happy_rate -ne $null) { [double]$focused.calm_to_happy_rate } else { 0.0 }
    }
}

function Update-SummaryCsv {
    param(
        [Parameter(Mandatory = $true)][string]$ExperimentName,
        [Parameter(Mandatory = $true)][string]$EvaluationJsonPath
    )

    $existingRows = @()
    if (Test-Path -LiteralPath $SummaryCsvPath) {
        $existingRows = Import-Csv -LiteralPath $SummaryCsvPath -Encoding UTF8
    }

    $alreadyExists = $false
    foreach ($row in $existingRows) {
        if ($row.experiment -eq $ExperimentName) {
            $alreadyExists = $true
            break
        }
    }
    if ($alreadyExists) {
        return $false
    }

    $metrics = Get-MetricsFromEvaluation -EvaluationJsonPath $EvaluationJsonPath
    $newRow = [pscustomobject]@{
        experiment = $ExperimentName
        source_json = [System.IO.Path]::GetFileName($EvaluationJsonPath)
        accuracy = $metrics.accuracy
        macro_f1 = $metrics.macro_f1
        happy_f1 = $metrics.happy_f1
        calm_f1 = $metrics.calm_f1
        happy_calm_macro_f1 = $metrics.happy_calm_macro_f1
        happy_to_calm_count = $metrics.happy_to_calm_count
        happy_to_calm_rate = $metrics.happy_to_calm_rate
        calm_to_happy_count = $metrics.calm_to_happy_count
        calm_to_happy_rate = $metrics.calm_to_happy_rate
    }

    $allRows = @($existingRows) + @($newRow)
    $allRows | Export-Csv -LiteralPath $SummaryCsvPath -Encoding UTF8 -NoTypeInformation
    Write-RunLog "updated summary csv with $ExperimentName"
    return $true
}

function Update-ImplementationStatus {
    param(
        [Parameter(Mandatory = $true)][string]$ExperimentName,
        [Parameter(Mandatory = $true)][string]$EvaluationJsonPath,
        [Parameter(Mandatory = $true)][string]$ConfigPath,
        [string]$ArtifactDir = ""
    )

    if (-not (Test-Path -LiteralPath $StatusDocPath)) {
        return $false
    }

    $currentText = Read-TextFile -Path $StatusDocPath
    $heading = "### 2026-03-30 unattended auto run - $ExperimentName"
    if ($currentText.Contains($heading)) {
        return $false
    }

    $metrics = Get-MetricsFromEvaluation -EvaluationJsonPath $EvaluationJsonPath
    $baselineRow = Get-SummaryRowByExperiment -ExperimentName $BaselineExperimentName
    $referenceRow = Get-SummaryRowByExperiment -ExperimentName $StrongestLocalReferenceExperimentName
    $artifactLine = "  - artifact dir: (not provided)"
    if ($ArtifactDir) {
        $artifactLine = "  - artifact dir: $ArtifactDir"
    }

    $comparisonLines = New-Object System.Collections.Generic.List[string]
    $comparisonLines.Add("- Comparison note") | Out-Null
    if ($baselineRow) {
        $comparisonLines.Add(("  - baseline macro F1 {0}, happy_calm_macro_f1 {1}, HAPPY -> CALM {2}, CALM -> HAPPY {3}" -f $baselineRow.macro_f1, $baselineRow.happy_calm_macro_f1, $baselineRow.happy_to_calm_count, $baselineRow.calm_to_happy_count)) | Out-Null
    }
    if ($referenceRow) {
        $comparisonLines.Add(("  - strongest local reference macro F1 {0}, happy_calm_macro_f1 {1}, HAPPY -> CALM {2}, CALM -> HAPPY {3}" -f $referenceRow.macro_f1, $referenceRow.happy_calm_macro_f1, $referenceRow.happy_to_calm_count, $referenceRow.calm_to_happy_count)) | Out-Null
    }

    $blockLines = New-Object System.Collections.Generic.List[string]
    foreach ($line in @(
        "",
        $heading,
        "",
        "- Completed change",
        "  - script-generated evaluation completion for $ExperimentName",
        "  - config: $ConfigPath",
        "  - evaluation json: $EvaluationJsonPath",
        $artifactLine,
        "- Verification status",
        ("  - accuracy {0}" -f ([string]::Format("{0:0.0000}", $metrics.accuracy))),
        ("  - macro F1 {0}" -f ([string]::Format("{0:0.0000}", $metrics.macro_f1))),
        ("  - happy_calm_macro_f1 {0}" -f ([string]::Format("{0:0.0000}", $metrics.happy_calm_macro_f1))),
        ("  - HAPPY -> CALM = {0} ({1:0.0000})" -f $metrics.happy_to_calm_count, $metrics.happy_to_calm_rate),
        ("  - CALM -> HAPPY = {0} ({1:0.0000})" -f $metrics.calm_to_happy_count, $metrics.calm_to_happy_rate)
    )) {
        $blockLines.Add($line) | Out-Null
    }

    foreach ($line in $comparisonLines.ToArray()) {
        $blockLines.Add($line) | Out-Null
    }

    foreach ($line in @(
        "- Current blocker or caution",
        "  - baseline, registry, and serving remain unchanged by this unattended script",
        "- Next recommended work",
        "  - re-read fixed compare summary before any promotion discussion"
    )) {
        $blockLines.Add($line) | Out-Null
    }

    $block = ($blockLines.ToArray()) -join "`r`n"

    Add-Content -LiteralPath $StatusDocPath -Value $block -Encoding UTF8
    Write-RunLog "appended implementation status note for $ExperimentName"
    return $true
}

function Add-SessionRecord {
    param(
        [Parameter(Mandatory = $true)][string]$ExperimentName,
        [Parameter(Mandatory = $true)][string]$Stage,
        [Parameter(Mandatory = $true)][string]$Status,
        [string]$ConfigPath = "",
        [string]$EvaluationJsonPath = "",
        [string]$ArtifactDir = "",
        [string]$Message = ""
    )

    $script:SessionRecords.Add([pscustomobject]@{
        timestamp = (Get-Date).ToString("s")
        experimentName = $ExperimentName
        stage = $Stage
        status = $Status
        configPath = $ConfigPath
        evaluationJsonPath = $EvaluationJsonPath
        artifactDir = $ArtifactDir
        message = $Message
    }) | Out-Null
    Save-State
}

function Add-FailureRecord {
    param(
        [Parameter(Mandatory = $true)][string]$ExperimentName,
        [Parameter(Mandatory = $true)][string]$Stage,
        [Parameter(Mandatory = $true)][string]$Message
    )

    $script:FailureRecords.Add([pscustomobject]@{
        timestamp = (Get-Date).ToString("s")
        experimentName = $ExperimentName
        stage = $Stage
        message = $Message
    }) | Out-Null
    Save-State
    Write-RunLog "$ExperimentName [$Stage] failed: $Message" "ERROR"
}

function Invoke-StepProcess {
    param(
        [Parameter(Mandatory = $true)][string]$StepName,
        [Parameter(Mandatory = $true)][string]$ExperimentName,
        [Parameter(Mandatory = $true)][string]$Executable,
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )

    $safeName = $ExperimentName -replace '[^A-Za-z0-9_.-]', '_'
    $stdoutPath = Join-Path $RunDir ("{0}_{1}.out.log" -f $safeName, $StepName)
    $stderrPath = Join-Path $RunDir ("{0}_{1}.err.log" -f $safeName, $StepName)

    $quotedArguments = $Arguments | ForEach-Object {
        if ($_ -match '[\s"]') {
            '"' + ($_ -replace '"', '\"') + '"'
        }
        else {
            $_
        }
    }

    Write-RunLog "$ExperimentName [$StepName] started"
    $process = Start-Process `
        -FilePath $Executable `
        -ArgumentList ($quotedArguments -join ' ') `
        -WorkingDirectory $ProjectRoot `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath `
        -NoNewWindow `
        -PassThru `
        -Wait

    if ($process.ExitCode -ne 0) {
        throw "$StepName failed with exit code $($process.ExitCode). stderr=$stderrPath"
    }

    Write-RunLog "$ExperimentName [$StepName] completed"
}

function Evaluate-Experiment {
    param(
        [Parameter(Mandatory = $true)][string]$ExperimentName,
        [Parameter(Mandatory = $true)][string]$BestModelDir,
        [Parameter(Mandatory = $true)][string]$EvaluationJsonPath,
        [string]$ConfigPath = ""
    )

    if (-not (Test-Path -LiteralPath $BestModelDir)) {
        throw "best model dir not found: $BestModelDir"
    }

    Invoke-StepProcess `
        -StepName "evaluate" `
        -ExperimentName $ExperimentName `
        -Executable $PythonExe `
        -Arguments @(
            $EvaluateScript,
            "--model-dir", $BestModelDir,
            "--input-csv", $ValidCsvPath,
            "--output-json", $EvaluationJsonPath
        )

    $summaryUpdated = Update-SummaryCsv -ExperimentName $ExperimentName -EvaluationJsonPath $EvaluationJsonPath
    $docUpdated = Update-ImplementationStatus `
        -ExperimentName $ExperimentName `
        -EvaluationJsonPath $EvaluationJsonPath `
        -ConfigPath $ConfigPath `
        -ArtifactDir (Split-Path -Parent $BestModelDir)

    Add-SessionRecord `
        -ExperimentName $ExperimentName `
        -Stage "evaluate" `
        -Status "completed" `
        -ConfigPath $ConfigPath `
        -EvaluationJsonPath $EvaluationJsonPath `
        -ArtifactDir (Split-Path -Parent $BestModelDir) `
        -Message ("summaryUpdated={0}; docUpdated={1}" -f $summaryUpdated, $docUpdated)
}

function Train-Experiment {
    param([Parameter(Mandatory = $true)]$Candidate)

    Invoke-StepProcess `
        -StepName "train" `
        -ExperimentName $Candidate.experimentName `
        -Executable $PythonExe `
        -Arguments @(
            $TrainScript,
            "--train-csv", $TrainCsvPath,
            "--valid-csv", $ValidCsvPath,
            "--config", $Candidate.configPath,
            "--label-map", $LabelMapPath,
            "--output-dir", $Candidate.artifactDir,
            "--auto-resume-last-checkpoint"
        )

    Add-SessionRecord `
        -ExperimentName $Candidate.experimentName `
        -Stage "train" `
        -Status "completed" `
        -ConfigPath $Candidate.configPath `
        -ArtifactDir $Candidate.artifactDir `
        -Message "train completed"
}

function Get-NextCandidate {
    param([Parameter(Mandatory = $true)][System.Collections.Generic.HashSet[string]]$SummarySet)

    foreach ($candidate in Get-CandidateQueue) {
        if ($SummarySet.Contains($candidate.experimentName)) {
            continue
        }
        if (Test-Path -LiteralPath $candidate.evaluationJsonPath) {
            continue
        }
        if ($script:AttemptedTrainNames.Contains($candidate.experimentName)) {
            continue
        }
        return $candidate
    }
    return $null
}

function Get-NextRecommendation {
    param([Parameter(Mandatory = $true)][System.Collections.Generic.HashSet[string]]$SummarySet)

    foreach ($candidate in Get-CandidateQueue) {
        if ($SummarySet.Contains($candidate.experimentName)) {
            continue
        }
        if (Test-Path -LiteralPath $candidate.evaluationJsonPath) {
            continue
        }
        return $candidate
    }
    return $null
}

function Write-FinalSummary {
    if ($script:SummaryWritten) {
        return
    }

    $summarySet = Get-SummaryExperimentSet
    $nextCandidate = Get-NextRecommendation -SummarySet $summarySet
    $remainingMinutes = Get-RemainingMinutes
    $baselineRow = Get-SummaryRowByExperiment -ExperimentName $BaselineExperimentName
    $referenceRow = Get-SummaryRowByExperiment -ExperimentName $StrongestLocalReferenceExperimentName

    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add("runId=$RunId") | Out-Null
    $lines.Add("finishedAtKst=$((Get-SeoulNow).ToString('yyyy-MM-dd HH:mm:ss'))") | Out-Null
    $lines.Add(("cutoffKst={0}" -f ((Get-Cutoff).ToString("yyyy-MM-dd HH:mm:ss")))) | Out-Null
    $lines.Add("remainingMinutes=$remainingMinutes") | Out-Null
    $lines.Add("") | Out-Null
    $lines.Add("[today attempted experiments]") | Out-Null

    if ($script:SessionRecords.Count -eq 0) {
        $lines.Add("- none") | Out-Null
    }
    else {
        foreach ($record in $script:SessionRecords) {
            $detail = "- {0} | stage={1} | status={2}" -f $record.experimentName, $record.stage, $record.status
            if ($record.evaluationJsonPath) {
                $detail = "{0} | json={1}" -f $detail, $record.evaluationJsonPath
            }
            if ($record.message) {
                $detail = "{0} | note={1}" -f $detail, $record.message
            }
            $lines.Add($detail) | Out-Null
        }
    }

    $lines.Add("") | Out-Null
    $lines.Add("[failures]") | Out-Null
    if ($script:FailureRecords.Count -eq 0) {
        $lines.Add("- none") | Out-Null
    }
    else {
        foreach ($failure in $script:FailureRecords) {
            $lines.Add("- $($failure.experimentName) | stage=$($failure.stage) | message=$($failure.message)") | Out-Null
        }
    }

    $lines.Add("") | Out-Null
    $lines.Add("[new evaluation json paths]") | Out-Null
    $jsonPaths = $script:SessionRecords |
        Where-Object { $_.evaluationJsonPath } |
        Select-Object -ExpandProperty evaluationJsonPath -Unique

    if (-not $jsonPaths) {
        $lines.Add("- none") | Out-Null
    }
    else {
        foreach ($path in $jsonPaths) {
            $lines.Add("- $path") | Out-Null
        }
    }

    $latestEvaluationRecord = $script:SessionRecords |
        Where-Object { $_.stage -eq "evaluate" -and $_.status -eq "completed" } |
        Select-Object -Last 1

    $lines.Add("") | Out-Null
    $lines.Add("[result interpretation]") | Out-Null
    if ($latestEvaluationRecord) {
        $latestRow = Get-SummaryRowByExperiment -ExperimentName $latestEvaluationRecord.experimentName
        if ($latestRow) {
            $lines.Add("- latest experiment=$($latestRow.experiment)") | Out-Null
            $lines.Add("- latest macro_f1=$($latestRow.macro_f1), happy_calm_macro_f1=$($latestRow.happy_calm_macro_f1)") | Out-Null
            $lines.Add("- latest HAPPY->CALM=$($latestRow.happy_to_calm_count), CALM->HAPPY=$($latestRow.calm_to_happy_count)") | Out-Null
            if ($baselineRow) {
                $lines.Add("- baseline macro_f1=$($baselineRow.macro_f1), happy_calm_macro_f1=$($baselineRow.happy_calm_macro_f1)") | Out-Null
                $lines.Add("- baseline HAPPY->CALM=$($baselineRow.happy_to_calm_count), CALM->HAPPY=$($baselineRow.calm_to_happy_count)") | Out-Null
            }
            if ($referenceRow) {
                $lines.Add("- strongest local reference=$($referenceRow.experiment)") | Out-Null
                $lines.Add("- reference macro_f1=$($referenceRow.macro_f1), happy_calm_macro_f1=$($referenceRow.happy_calm_macro_f1)") | Out-Null
                $lines.Add("- reference HAPPY->CALM=$($referenceRow.happy_to_calm_count), CALM->HAPPY=$($referenceRow.calm_to_happy_count)") | Out-Null
            }
            $lines.Add("- baseline, registry, and serving stay unchanged unless both fixed-compare quality and directional counts clear the gate") | Out-Null
        }
        else {
            $lines.Add("- latest evaluation completed, but summary row lookup failed") | Out-Null
        }
    }
    else {
        $lines.Add("- no new evaluation completed in this run") | Out-Null
    }

    $lines.Add("") | Out-Null
    $lines.Add("[next recommended candidate]") | Out-Null
    if ($nextCandidate) {
        $lines.Add("- $($nextCandidate.experimentName)") | Out-Null
        $lines.Add("- knob=$($nextCandidate.knob) value=$($nextCandidate.knobValue)") | Out-Null
        $lines.Add("- reason=$($nextCandidate.reason)") | Out-Null
    }
    else {
        $lines.Add("- no remaining conservative penalty candidate in the built-in queue") | Out-Null
    }

    $lines | Set-Content -LiteralPath $SummaryPath -Encoding UTF8
    $script:SummaryWritten = $true
    Write-RunLog "wrote final summary to $SummaryPath"
}

function Assert-Prerequisites {
    $requiredPaths = @(
        $PythonExe,
        $TrainScript,
        $EvaluateScript,
        $StatusDocPath,
        $SummaryCsvPath,
        $TrainCsvPath,
        $ValidCsvPath,
        $LabelMapPath,
        $BaseConfigPath
    )

    foreach ($path in $requiredPaths) {
        if (-not (Test-Path -LiteralPath $path)) {
            throw "required path not found: $path"
        }
    }
}

Write-RunLog "run started"

try {
    Assert-Prerequisites

    $startNow = Get-SeoulNow
    $cutoff = Get-Cutoff
    $startRemainingMinutes = Get-RemainingMinutes
    Write-RunLog ("currentKst={0}" -f $startNow.ToString("yyyy-MM-dd HH:mm:ss"))
    Write-RunLog ("cutoffKst={0}" -f $cutoff.ToString("yyyy-MM-dd HH:mm:ss"))
    Write-RunLog ("remainingMinutes={0}" -f $startRemainingMinutes)

    if ($startRemainingMinutes -le 0) {
        Write-RunLog "cutoff already passed; skip training and leave summary only" "WARN"
        Write-FinalSummary
        Save-State
        exit 0
    }

    while ($true) {
        $script:LoopCounter += 1
        Save-State
        Write-RunLog "loop $($script:LoopCounter) started"

        $docText = Read-TextFile -Path $StatusDocPath
        $summaryRows = Read-SummaryRows
        $summarySet = Get-SummaryExperimentSet
        Write-RunLog ("implementation status length={0}; summary rows={1}" -f $docText.Length, $summaryRows.Count)

        $didWork = $false

        foreach ($plan in Get-IncompleteEvaluationPlans) {
            if ($script:AttemptedEvalNames.Contains($plan.experimentName)) {
                continue
            }

            try {
                [void]$script:AttemptedEvalNames.Add($plan.experimentName)
                Evaluate-Experiment `
                    -ExperimentName $plan.experimentName `
                    -BestModelDir $plan.bestModelDir `
                    -EvaluationJsonPath $plan.evaluationJsonPath `
                    -ConfigPath $plan.configPath
                $didWork = $true
            }
            catch {
                Add-FailureRecord `
                    -ExperimentName $plan.experimentName `
                    -Stage "evaluate-only" `
                    -Message $_.Exception.Message
            }
        }

        $remainingMinutes = Get-RemainingMinutes
        Write-RunLog ("remainingMinutesBeforeTrainCheck={0}" -f $remainingMinutes)

        if ($remainingMinutes -lt 70) {
            Write-RunLog "less than 70 minutes remain; do not start a new train" "WARN"
            break
        }

        $summarySet = Get-SummaryExperimentSet
        $candidate = Get-NextCandidate -SummarySet $summarySet

        if (-not $candidate) {
            Write-RunLog "no remaining conservative new candidate to train"
            if (-not $didWork) {
                break
            }
            continue
        }

        try {
            Ensure-CandidateConfig -Candidate $candidate

            if (Test-Path -LiteralPath $candidate.bestModelDir) {
                Write-RunLog "$($candidate.experimentName) already has best model dir; skip fresh train and move to evaluation"
            }
            else {
                [void]$script:AttemptedTrainNames.Add($candidate.experimentName)
                Train-Experiment -Candidate $candidate
                $didWork = $true
            }

            if (-not (Test-Path -LiteralPath $candidate.evaluationJsonPath)) {
                [void]$script:AttemptedEvalNames.Add($candidate.experimentName)
                Evaluate-Experiment `
                    -ExperimentName $candidate.experimentName `
                    -BestModelDir $candidate.bestModelDir `
                    -EvaluationJsonPath $candidate.evaluationJsonPath `
                    -ConfigPath $candidate.configPath
                $didWork = $true
            }
        }
        catch {
            Add-FailureRecord `
                -ExperimentName $candidate.experimentName `
                -Stage "train-or-evaluate" `
                -Message $_.Exception.Message

            if ((Test-Path -LiteralPath $candidate.bestModelDir) -and (-not (Test-Path -LiteralPath $candidate.evaluationJsonPath))) {
                try {
                    [void]$script:AttemptedEvalNames.Add($candidate.experimentName)
                    Evaluate-Experiment `
                        -ExperimentName $candidate.experimentName `
                        -BestModelDir $candidate.bestModelDir `
                        -EvaluationJsonPath $candidate.evaluationJsonPath `
                        -ConfigPath $candidate.configPath
                    $didWork = $true
                }
                catch {
                    Add-FailureRecord `
                        -ExperimentName $candidate.experimentName `
                        -Stage "fallback-evaluate" `
                        -Message $_.Exception.Message
                }
            }
        }

        if (-not $didWork) {
            Write-RunLog "loop finished without actionable work"
            break
        }
    }
}
catch {
    Add-FailureRecord -ExperimentName "script" -Stage "fatal" -Message $_.Exception.Message
}
finally {
    Write-FinalSummary
    Save-State
    Write-RunLog "run finished"
}
