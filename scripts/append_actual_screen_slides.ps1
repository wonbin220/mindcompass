# 실제 화면 SVG를 기존 PPTX 끝에 추가하는 스크립트
param(
    [string]$SourceDeckPath,
    [string]$OutputDeckPath
)

$root = Split-Path -Parent $PSScriptRoot
$uiPath = Join-Path $root "docs/ui-mockups"

$assets = @{
    LoginMobile    = Join-Path $uiPath "login-screen-01.svg"
    CalendarMobile = Join-Path $uiPath "calendar-home-screen-01.svg"
    DiaryMobile    = Join-Path $uiPath "diary-detail-screen-01.svg"
    ChatMobile     = Join-Path $uiPath "chat-screen-01.svg"
    ReportMobile   = Join-Path $uiPath "report-screen-01.svg"
    CalendarWeb    = Join-Path $uiPath "calendar-home-web-01.svg"
    DiaryWeb       = Join-Path $uiPath "diary-detail-web-01.svg"
    ChatWeb        = Join-Path $uiPath "chat-web-01.svg"
    ReportWeb      = Join-Path $uiPath "report-web-01.svg"
}

if (-not (Test-Path $SourceDeckPath)) {
    throw "Source deck not found: $SourceDeckPath"
}
foreach ($asset in $assets.Values) {
    if (-not (Test-Path $asset)) {
        throw "Asset not found: $asset"
    }
}
if (-not $OutputDeckPath) {
    throw "OutputDeckPath is required."
}

function Add-Label {
    param($Slide, [string]$Text, [double]$Left, [double]$Top, [double]$Width)
    $shape = $Slide.Shapes.AddTextbox(1, $Left, $Top, $Width, 20)
    $shape.TextFrame.TextRange.Text = $Text
    $shape.TextFrame.TextRange.Font.Name = "Arial"
    $shape.TextFrame.TextRange.Font.Size = 12
    $shape.TextFrame.TextRange.Font.Bold = -1
    $shape.TextFrame.TextRange.Font.Color.RGB = 0x6E7A8C
    return $shape
}

function Add-Title {
    param($Slide, [string]$Title, [string]$Subtitle)
    $titleShape = $Slide.Shapes.AddTextbox(1, 72, 48, 900, 40)
    $titleShape.TextFrame.TextRange.Text = $Title
    $titleShape.TextFrame.TextRange.Font.Name = "Arial"
    $titleShape.TextFrame.TextRange.Font.Size = 28
    $titleShape.TextFrame.TextRange.Font.Bold = -1
    $titleShape.TextFrame.TextRange.Font.Color.RGB = 0x1F2A37

    $subtitleShape = $Slide.Shapes.AddTextbox(1, 72, 84, 1100, 24)
    $subtitleShape.TextFrame.TextRange.Text = $Subtitle
    $subtitleShape.TextFrame.TextRange.Font.Name = "Arial"
    $subtitleShape.TextFrame.TextRange.Font.Size = 14
    $subtitleShape.TextFrame.TextRange.Font.Color.RGB = 0x738094
}

$ppt = $null
$sourceDeck = $null
$outputDeck = $null

try {
    $ppt = New-Object -ComObject PowerPoint.Application
    $sourceDeck = $ppt.Presentations.Open($SourceDeckPath, $false, $false, $false)
    $sourceDeck.SaveCopyAs($OutputDeckPath)
    $outputDeck = $ppt.Presentations.Open($OutputDeckPath, $false, $false, $false)

    $mobileSlide = $outputDeck.Slides.Add($outputDeck.Slides.Count + 1, 12)
    $mobileSlide.FollowMasterBackground = 0
    $mobileSlide.Background.Fill.ForeColor.RGB = 0xFBF6F3
    Add-Title -Slide $mobileSlide -Title "Main Feature Screens / Mobile" -Subtitle "Actual mobile UI screens instead of abstract placeholders"

    $mobileSlide.Shapes.AddPicture($assets.LoginMobile, $false, $true, 72, 146, 200, 432) | Out-Null
    $mobileSlide.Shapes.AddPicture($assets.CalendarMobile, $false, $true, 298, 146, 200, 432) | Out-Null
    $mobileSlide.Shapes.AddPicture($assets.DiaryMobile, $false, $true, 524, 146, 200, 432) | Out-Null
    $mobileSlide.Shapes.AddPicture($assets.ChatMobile, $false, $true, 750, 146, 200, 432) | Out-Null
    $mobileSlide.Shapes.AddPicture($assets.ReportMobile, $false, $true, 976, 146, 200, 432) | Out-Null

    Add-Label -Slide $mobileSlide -Text "Login" -Left 138 -Top 590 -Width 90 | Out-Null
    Add-Label -Slide $mobileSlide -Text "Calendar" -Left 356 -Top 590 -Width 90 | Out-Null
    Add-Label -Slide $mobileSlide -Text "Diary" -Left 600 -Top 590 -Width 90 | Out-Null
    Add-Label -Slide $mobileSlide -Text "Chat" -Left 828 -Top 590 -Width 90 | Out-Null
    Add-Label -Slide $mobileSlide -Text "Report" -Left 1048 -Top 590 -Width 90 | Out-Null

    $mobileNote = $mobileSlide.Shapes.AddShape(1, 72, 658, 1400, 140)
    $mobileNote.Fill.ForeColor.RGB = 0xFFFFFF
    $mobileNote.Line.ForeColor.RGB = 0xE8EDF4
    $mobileNote.TextFrame.TextRange.Text = "These screens show the actual mobile interaction path: authentication, calendar-based entry, diary detail with AI interpretation, chat response branching, and report visualization."
    $mobileNote.TextFrame.TextRange.Font.Name = "Arial"
    $mobileNote.TextFrame.TextRange.Font.Size = 18
    $mobileNote.TextFrame.TextRange.Font.Color.RGB = 0x556274

    $webSlide = $outputDeck.Slides.Add($outputDeck.Slides.Count + 1, 12)
    $webSlide.FollowMasterBackground = 0
    $webSlide.Background.Fill.ForeColor.RGB = 0xFBF6F3
    Add-Title -Slide $webSlide -Title "Web Responsive Extension" -Subtitle "Concrete desktop layouts replacing the previous ambiguous responsive block"

    $webSlide.Shapes.AddPicture($assets.CalendarWeb, $false, $true, 72, 150, 690, 330) | Out-Null
    $webSlide.Shapes.AddPicture($assets.ChatWeb, $false, $true, 838, 150, 690, 330) | Out-Null
    $webSlide.Shapes.AddPicture($assets.DiaryWeb, $false, $true, 72, 522, 690, 330) | Out-Null
    $webSlide.Shapes.AddPicture($assets.ReportWeb, $false, $true, 838, 522, 690, 330) | Out-Null

    Add-Label -Slide $webSlide -Text "Calendar web layout" -Left 332 -Top 490 -Width 170 | Out-Null
    Add-Label -Slide $webSlide -Text "Chat web layout" -Left 1118 -Top 490 -Width 150 | Out-Null
    Add-Label -Slide $webSlide -Text "Diary detail web layout" -Left 314 -Top 862 -Width 210 | Out-Null
    Add-Label -Slide $webSlide -Text "Report web layout" -Left 1114 -Top 862 -Width 160 | Out-Null

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
