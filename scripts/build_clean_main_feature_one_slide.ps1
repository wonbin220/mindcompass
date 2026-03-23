# Build one clean PPTX slide using slide 8 as background image and actual cropped screens.
param(
    [string]$OutputPath = "docs/ui-mockups/main-features-clean-one-slide.pptx"
)

$root = Split-Path -Parent $PSScriptRoot
$exportDir = Join-Path $root "docs/ui-mockups/ppt-export"
$cropDir = Join-Path $exportDir "crops"
$bgImage = Join-Path $exportDir "slide8.png"
$outputFullPath = Join-Path $root $OutputPath

$assets = @{
    Background     = $bgImage
    LoginMobile    = Join-Path $cropDir "login-mobile.png"
    CalendarMobile = Join-Path $cropDir "calendar-mobile.png"
    DiaryMobile    = Join-Path $cropDir "diary-mobile.png"
    ChatMobile     = Join-Path $cropDir "chat-mobile.png"
    ReportMobile   = Join-Path $cropDir "report-mobile.png"
    CalendarWeb    = Join-Path $cropDir "calendar-web.png"
    ChatWeb        = Join-Path $cropDir "chat-web.png"
}

foreach ($asset in $assets.Values) {
    if (-not (Test-Path $asset)) {
        throw "Missing asset: $asset"
    }
}

function Add-Text {
    param($Slide, [string]$Text, [double]$Left, [double]$Top, [double]$Width, [double]$Height)
    $shape = $Slide.Shapes.AddTextbox(1, $Left, $Top, $Width, $Height)
    $shape.TextFrame.TextRange.Text = $Text
    $shape.TextFrame.TextRange.Font.Name = "Arial"
    $shape.TextFrame.TextRange.Font.Size = 10
    $shape.TextFrame.TextRange.Font.Bold = -1
    $shape.TextFrame.TextRange.Font.Color.RGB = 0x6F7C8F
    return $shape
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

    # Use exported slide 8 as a stable background so Korean text/style remain intact.
    $slide.Shapes.AddPicture($assets.Background, $false, $true, 0, 0, 1600, 900) | Out-Null

    # White cover over old placeholder area only.
    $coverMobile = $slide.Shapes.AddShape(1, 84, 300, 644, 266)
    $coverMobile.Fill.ForeColor.RGB = 0xFFFFFF
    $coverMobile.Line.Visible = 0

    $coverWeb1 = $slide.Shapes.AddShape(1, 822, 318, 444, 150)
    $coverWeb1.Fill.ForeColor.RGB = 0xFFFFFF
    $coverWeb1.Line.Visible = 0

    $coverWeb2 = $slide.Shapes.AddShape(1, 822, 542, 444, 150)
    $coverWeb2.Fill.ForeColor.RGB = 0xFFFFFF
    $coverWeb2.Line.Visible = 0

    # Actual mobile screens.
    $slide.Shapes.AddPicture($assets.LoginMobile, $false, $true, 92, 304, 110, 240) | Out-Null
    $slide.Shapes.AddPicture($assets.CalendarMobile, $false, $true, 220, 304, 110, 240) | Out-Null
    $slide.Shapes.AddPicture($assets.DiaryMobile, $false, $true, 348, 304, 110, 240) | Out-Null
    $slide.Shapes.AddPicture($assets.ChatMobile, $false, $true, 476, 304, 110, 240) | Out-Null
    $slide.Shapes.AddPicture($assets.ReportMobile, $false, $true, 604, 304, 110, 240) | Out-Null

    # Actual web layouts.
    $slide.Shapes.AddPicture($assets.CalendarWeb, $false, $true, 832, 324, 424, 144) | Out-Null
    $slide.Shapes.AddPicture($assets.ChatWeb, $false, $true, 832, 548, 424, 144) | Out-Null

    # Minimal labels for readability.
    Add-Text -Slide $slide -Text "Login" -Left 126 -Top 548 -Width 46 -Height 14 | Out-Null
    Add-Text -Slide $slide -Text "Calendar" -Left 246 -Top 548 -Width 56 -Height 14 | Out-Null
    Add-Text -Slide $slide -Text "Diary" -Left 386 -Top 548 -Width 40 -Height 14 | Out-Null
    Add-Text -Slide $slide -Text "Chat" -Left 516 -Top 548 -Width 34 -Height 14 | Out-Null
    Add-Text -Slide $slide -Text "Report" -Left 638 -Top 548 -Width 44 -Height 14 | Out-Null

    if (Test-Path $outputFullPath) {
        Remove-Item $outputFullPath -Force
    }
    $deck.SaveAs($outputFullPath)
}
finally {
    if ($deck) { $deck.Close() }
    if ($ppt) { $ppt.Quit() }
    if ($deck) { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($deck) | Out-Null }
    if ($ppt) { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($ppt) | Out-Null }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}
