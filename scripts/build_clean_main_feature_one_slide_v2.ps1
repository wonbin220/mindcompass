# Build a readable one-slide PPTX using slide 8 background and fewer larger screen images.
param(
    [string]$OutputPath = "docs/ui-mockups/main-features-clean-one-slide-v2.pptx"
)

$root = Split-Path -Parent $PSScriptRoot
$exportDir = Join-Path $root "docs/ui-mockups/ppt-export"
$cropDir = Join-Path $exportDir "crops"
$bgImage = Join-Path $exportDir "slide8.png"
$outputFullPath = Join-Path $root $OutputPath

$assets = @{
    Background     = $bgImage
    CalendarMobile = Join-Path $cropDir "calendar-mobile.png"
    DiaryMobile    = Join-Path $cropDir "diary-mobile.png"
    ChatMobile     = Join-Path $cropDir "chat-mobile.png"
    CalendarWeb    = Join-Path $cropDir "calendar-web.png"
}

foreach ($asset in $assets.Values) {
    if (-not (Test-Path $asset)) {
        throw "Missing asset: $asset"
    }
}

$ppt = $null
$deck = $null

try {
    $ppt = New-Object -ComObject PowerPoint.Application
    $deck = $ppt.Presentations.Add()
    $deck.PageSetup.SlideWidth = 1600
    $deck.PageSetup.SlideHeight = 900

    $slide = $deck.Slides.Add(1, 12)
    $slide.FollowMasterBackground = 0
    $slide.Background.Fill.ForeColor.RGB = 0xFBF7F2

    $slide.Shapes.AddPicture($assets.Background, $false, $true, 0, 0, 1600, 900) | Out-Null

    # Hide old placeholder content with simple white cards.
    $mobileCover = $slide.Shapes.AddShape(1, 84, 300, 648, 292)
    $mobileCover.Fill.ForeColor.RGB = 0xFFFFFF
    $mobileCover.Line.Visible = 0

    $webCover = $slide.Shapes.AddShape(1, 822, 320, 444, 374)
    $webCover.Fill.ForeColor.RGB = 0xFFFFFF
    $webCover.Line.Visible = 0

    # Larger, readable mobile screens.
    $slide.Shapes.AddPicture($assets.CalendarMobile, $false, $true, 102, 314, 184, 332) | Out-Null
    $slide.Shapes.AddPicture($assets.DiaryMobile, $false, $true, 316, 314, 184, 332) | Out-Null
    $slide.Shapes.AddPicture($assets.ChatMobile, $false, $true, 530, 314, 184, 332) | Out-Null

    # One large web example.
    $slide.Shapes.AddPicture($assets.CalendarWeb, $false, $true, 846, 342, 398, 182) | Out-Null
}
finally {
    if (Test-Path $outputFullPath) {
        Remove-Item $outputFullPath -Force
    }
    if ($deck) { $deck.SaveAs($outputFullPath) }
    if ($deck) { $deck.Close() }
    if ($ppt) { $ppt.Quit() }
    if ($deck) { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($deck) | Out-Null }
    if ($ppt) { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($ppt) | Out-Null }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}
