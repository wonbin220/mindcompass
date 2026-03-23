# Replace slide 7/8 with one merged slide based on existing slide 8 styling
param(
    [string]$SourceDeckPath,
    [string]$OutputDeckPath
)

$root = Split-Path -Parent $PSScriptRoot
$uiPath = Join-Path $root "docs/ui-mockups"
$cropPath = Join-Path $uiPath "ppt-export/crops"

$assets = @{
    LoginMobile    = Join-Path $cropPath "login-mobile.png"
    CalendarMobile = Join-Path $cropPath "calendar-mobile.png"
    DiaryMobile    = Join-Path $cropPath "diary-mobile.png"
    ChatMobile     = Join-Path $cropPath "chat-mobile.png"
    ReportMobile   = Join-Path $cropPath "report-mobile.png"
    CalendarWeb    = Join-Path $cropPath "calendar-web.png"
    ChatWeb        = Join-Path $cropPath "chat-web.png"
}

if (-not (Test-Path $SourceDeckPath)) { throw "Source deck not found: $SourceDeckPath" }
if (-not $OutputDeckPath) { throw "OutputDeckPath is required." }
foreach ($asset in $assets.Values) {
    if (-not (Test-Path $asset)) { throw "Asset not found: $asset" }
}

$ppt = $null
$sourceDeck = $null
$outputDeck = $null

try {
    $ppt = New-Object -ComObject PowerPoint.Application
    $sourceDeck = $ppt.Presentations.Open($SourceDeckPath, $false, $false, $false)
    $sourceDeck.SaveCopyAs($OutputDeckPath)
    $outputDeck = $ppt.Presentations.Open($OutputDeckPath, $false, $false, $false)

    # Remove old slide 7 so old detailed screen page disappears.
    $outputDeck.Slides.Item(7).Delete()

    # After deletion, original slide 8 becomes slide 7.
    $slide = $outputDeck.Slides.Item(7)

    # Cover placeholder area first.
    $mobileCover = $slide.Shapes.AddShape(1, 84, 298, 648, 272)
    $mobileCover.Fill.ForeColor.RGB = 0xFFFFFF
    $mobileCover.Line.Visible = 0

    $webTopCover = $slide.Shapes.AddShape(1, 822, 316, 446, 156)
    $webTopCover.Fill.ForeColor.RGB = 0xFFFFFF
    $webTopCover.Line.Visible = 0

    $webBottomCover = $slide.Shapes.AddShape(1, 822, 540, 446, 156)
    $webBottomCover.Fill.ForeColor.RGB = 0xFFFFFF
    $webBottomCover.Line.Visible = 0

    # Add actual mobile screens.
    $slide.Shapes.AddPicture($assets.LoginMobile, $false, $true, 88, 302, 114, 246) | Out-Null
    $slide.Shapes.AddPicture($assets.CalendarMobile, $false, $true, 218, 302, 114, 246) | Out-Null
    $slide.Shapes.AddPicture($assets.DiaryMobile, $false, $true, 348, 302, 114, 246) | Out-Null
    $slide.Shapes.AddPicture($assets.ChatMobile, $false, $true, 478, 302, 114, 246) | Out-Null
    $slide.Shapes.AddPicture($assets.ReportMobile, $false, $true, 608, 302, 114, 246) | Out-Null

    # Add actual web layouts.
    $slide.Shapes.AddPicture($assets.CalendarWeb, $false, $true, 826, 318, 438, 152) | Out-Null
    $slide.Shapes.AddPicture($assets.ChatWeb, $false, $true, 826, 542, 438, 152) | Out-Null

    $outputDeck.Save()
}
finally {
    if ($outputDeck) { $outputDeck.Close() }
    if ($sourceDeck) { $sourceDeck.Close() }
    if ($ppt) { $ppt.Quit() }
    if ($outputDeck) { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($outputDeck) | Out-Null }
    if ($sourceDeck) { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($sourceDeck) | Out-Null }
    if ($ppt) { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($ppt) | Out-Null }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}
