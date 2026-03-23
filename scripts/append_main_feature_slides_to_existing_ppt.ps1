# 기존 PPTX에 주요 기능 화면 슬라이드를 추가하는 스크립트
param(
    [string]$SourceDeckPath,
    [string]$FeatureDeckPath = "docs/ui-mockups/main-features-tech-lead-interview-v2.pptx",
    [string]$SummarySvgPath = "docs/ui-mockups/ppt-main-features-slide-02.svg",
    [string]$OutputDeckPath
)

$root = Split-Path -Parent $PSScriptRoot
$featureDeckFullPath = Join-Path $root $FeatureDeckPath
$summarySvgFullPath = Join-Path $root $SummarySvgPath

if (-not (Test-Path $SourceDeckPath)) {
    throw "Source PPTX not found: $SourceDeckPath"
}
if (-not (Test-Path $featureDeckFullPath)) {
    throw "Feature PPTX not found: $featureDeckFullPath"
}
if (-not (Test-Path $summarySvgFullPath)) {
    throw "Summary SVG not found: $summarySvgFullPath"
}
if (-not $OutputDeckPath) {
    throw "OutputDeckPath is required."
}

$ppt = $null
$sourceDeck = $null
$featureDeck = $null
$outputDeck = $null

try {
    $ppt = New-Object -ComObject PowerPoint.Application
    $sourceDeck = $ppt.Presentations.Open($SourceDeckPath, $false, $false, $false)
    $featureDeck = $ppt.Presentations.Open($featureDeckFullPath, $false, $true, $false)

    $sourceDeck.SaveCopyAs($OutputDeckPath)
    $outputDeck = $ppt.Presentations.Open($OutputDeckPath, $false, $false, $false)

    # 실제 SVG 화면이 들어간 슬라이드 복사
    $featureDeck.Slides(2).Copy() | Out-Null
    $outputDeck.Slides.Paste($outputDeck.Slides.Count + 1) | Out-Null

    # 한국어 발표 톤 보강 슬라이드 추가
    $newSlide = $outputDeck.Slides.Add($outputDeck.Slides.Count + 1, 12)
    $newSlide.FollowMasterBackground = 0
    $newSlide.Background.Fill.ForeColor.RGB = 0xFBF6F3
    $newSlide.Shapes.AddPicture(
        $summarySvgFullPath,
        $false,
        $true,
        0,
        0,
        $outputDeck.PageSetup.SlideWidth,
        $outputDeck.PageSetup.SlideHeight
    ) | Out-Null

    $outputDeck.Save()
}
finally {
    if ($outputDeck) { $outputDeck.Close() }
    if ($featureDeck) { $featureDeck.Close() }
    if ($sourceDeck) { $sourceDeck.Close() }
    if ($ppt) { $ppt.Quit() }
    if ($outputDeck) { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($outputDeck) | Out-Null }
    if ($featureDeck) { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($featureDeck) | Out-Null }
    if ($sourceDeck) { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($sourceDeck) | Out-Null }
    if ($ppt) { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($ppt) | Out-Null }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}
