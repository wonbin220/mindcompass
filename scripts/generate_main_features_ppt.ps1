# 주요 기능 화면 발표용 PPTX 생성 스크립트
param(
    [string]$OutputPath = "docs/ui-mockups/main-features-tech-lead-interview.pptx"
)

$root = Split-Path -Parent $PSScriptRoot
$outputFullPath = Join-Path $root $OutputPath
$uiPath = Join-Path $root "docs/ui-mockups"

$assets = @{
    LoginMobile    = Join-Path $uiPath "login-screen-01.svg"
    CalendarMobile = Join-Path $uiPath "calendar-home-screen-01.svg"
    DiaryMobile    = Join-Path $uiPath "diary-detail-screen-01.svg"
    ChatMobile     = Join-Path $uiPath "chat-screen-01.svg"
    ReportMobile   = Join-Path $uiPath "report-screen-01.svg"
    CalendarWeb    = Join-Path $uiPath "calendar-home-web-01.svg"
    ChatWeb        = Join-Path $uiPath "chat-web-01.svg"
}

foreach ($asset in $assets.Values) {
    if (-not (Test-Path $asset)) {
        throw "UI asset not found: $asset"
    }
}

function Add-TextBox {
    param($Slide, [string]$Text, [double]$Left, [double]$Top, [double]$Width, [double]$Height, [int]$FontSize, [int]$Color, [bool]$Bold = $false)
    $shape = $Slide.Shapes.AddTextbox(1, $Left, $Top, $Width, $Height)
    $shape.TextFrame.TextRange.Text = $Text
    $shape.TextFrame.TextRange.Font.Name = "Arial"
    $shape.TextFrame.TextRange.Font.Size = $FontSize
    $shape.TextFrame.TextRange.Font.Bold = $(if ($Bold) { -1 } else { 0 })
    $shape.TextFrame.TextRange.Font.Color.RGB = $Color
    return $shape
}

$ppt = $null
$presentation = $null

try {
    $ppt = New-Object -ComObject PowerPoint.Application
    $presentation = $ppt.Presentations.Add()
    $presentation.PageSetup.SlideWidth = 1600
    $presentation.PageSetup.SlideHeight = 900

    $titleSlide = $presentation.Slides.Add(1, 11)
    $titleSlide.FollowMasterBackground = 0
    $titleSlide.Background.Fill.ForeColor.RGB = 0xFBF6F3

    Add-TextBox -Slide $titleSlide -Text "Mind Compass Main Feature Screens" -Left 72 -Top 120 -Width 1200 -Height 80 -FontSize 28 -Color 0x2B2017 -Bold $true | Out-Null
    Add-TextBox -Slide $titleSlide -Text ("Tech Lead Interview Deck" + [Environment]::NewLine + "Screen flow, interaction points, and safety-first UX summary") -Left 72 -Top 214 -Width 1200 -Height 120 -FontSize 18 -Color 0x6B6257 | Out-Null

    $tag1 = $titleSlide.Shapes.AddShape(1, 72, 342, 210, 42)
    $tag1.Fill.ForeColor.RGB = 0xFFF3EA
    $tag1.Line.Visible = 0
    $tag1.TextFrame.TextRange.Text = "Mobile first, web scalable"
    $tag1.TextFrame.TextRange.Font.Name = "Arial"
    $tag1.TextFrame.TextRange.Font.Size = 14
    $tag1.TextFrame.TextRange.Font.Bold = -1
    $tag1.TextFrame.TextRange.Font.Color.RGB = 0x9A5D33

    $tag2 = $titleSlide.Shapes.AddShape(1, 296, 342, 190, 42)
    $tag2.Fill.ForeColor.RGB = 0xF2F6FF
    $tag2.Line.Visible = 0
    $tag2.TextFrame.TextRange.Text = "Safety-first UX"
    $tag2.TextFrame.TextRange.Font.Name = "Arial"
    $tag2.TextFrame.TextRange.Font.Size = 14
    $tag2.TextFrame.TextRange.Font.Bold = -1
    $tag2.TextFrame.TextRange.Font.Color.RGB = 0x3B6BC7

    Add-TextBox -Slide $titleSlide -Text ("• Calendar -> Diary -> Chat -> Report flow" + [Environment]::NewLine + "• Diary and Chat expose AI analysis and risk branching clearly in UI" + [Environment]::NewLine + "• Screen structure aligns directly with backend API flow") -Left 72 -Top 438 -Width 1160 -Height 180 -FontSize 20 -Color 0x463E36 | Out-Null

    $slide = $presentation.Slides.Add(2, 12)
    $slide.FollowMasterBackground = 0
    $slide.Background.Fill.ForeColor.RGB = 0xFBF6F3

    Add-TextBox -Slide $slide -Text "Main Feature Screens" -Left 72 -Top 44 -Width 600 -Height 50 -FontSize 26 -Color 0x17202B -Bold $true | Out-Null
    Add-TextBox -Slide $slide -Text "Actual mobile mockups with web-responsive examples" -Left 72 -Top 80 -Width 600 -Height 28 -FontSize 15 -Color 0x6F7C8F | Out-Null

    $slide.Shapes.AddPicture($assets.LoginMobile, $false, $true, 88, 138, 170, 368) | Out-Null
    $slide.Shapes.AddPicture($assets.CalendarMobile, $false, $true, 274, 138, 170, 368) | Out-Null
    $slide.Shapes.AddPicture($assets.DiaryMobile, $false, $true, 460, 138, 170, 368) | Out-Null
    $slide.Shapes.AddPicture($assets.ChatMobile, $false, $true, 646, 138, 170, 368) | Out-Null
    $slide.Shapes.AddPicture($assets.ReportMobile, $false, $true, 832, 138, 170, 368) | Out-Null

    Add-TextBox -Slide $slide -Text "Login" -Left 140 -Top 520 -Width 80 -Height 24 -FontSize 13 -Color 0x5D6D82 -Bold $true | Out-Null
    Add-TextBox -Slide $slide -Text "Calendar" -Left 315 -Top 520 -Width 90 -Height 24 -FontSize 13 -Color 0x5D6D82 -Bold $true | Out-Null
    Add-TextBox -Slide $slide -Text "Diary" -Left 516 -Top 520 -Width 70 -Height 24 -FontSize 13 -Color 0x5D6D82 -Bold $true | Out-Null
    Add-TextBox -Slide $slide -Text "Chat" -Left 706 -Top 520 -Width 70 -Height 24 -FontSize 13 -Color 0x5D6D82 -Bold $true | Out-Null
    Add-TextBox -Slide $slide -Text "Report" -Left 885 -Top 520 -Width 80 -Height 24 -FontSize 13 -Color 0x5D6D82 -Bold $true | Out-Null

    $summaryCard = $slide.Shapes.AddShape(1, 1040, 138, 490, 408)
    $summaryCard.Fill.ForeColor.RGB = 0xFFFFFF
    $summaryCard.Line.ForeColor.RGB = 0xE8EDF4
    Add-TextBox -Slide $slide -Text "Why these screens matter" -Left 1070 -Top 172 -Width 300 -Height 30 -FontSize 20 -Color 0x17202B -Bold $true | Out-Null
    Add-TextBox -Slide $slide -Text ("• Calendar is the navigation hub for date-based entry" + [Environment]::NewLine +
        "• Diary is the core input and AI analysis checkpoint" + [Environment]::NewLine +
        "• Chat visualizes NORMAL / SUPPORTIVE / SAFETY branching" + [Environment]::NewLine +
        "• Report turns stored data into user-facing insight") -Left 1070 -Top 222 -Width 420 -Height 150 -FontSize 16 -Color 0x556274 | Out-Null
    Add-TextBox -Slide $slide -Text ("Backend alignment" + [Environment]::NewLine +
        "Calendar -> daily/monthly query APIs" + [Environment]::NewLine +
        "Diary -> CRUD + analysis + risk score" + [Environment]::NewLine +
        "Chat -> session/message + reply/risk branching" + [Environment]::NewLine +
        "Report -> monthly summary + trend queries") -Left 1070 -Top 390 -Width 420 -Height 136 -FontSize 15 -Color 0x6B7685 | Out-Null

    Add-TextBox -Slide $slide -Text "Web-responsive examples" -Left 72 -Top 592 -Width 320 -Height 28 -FontSize 20 -Color 0x17202B -Bold $true | Out-Null
    Add-TextBox -Slide $slide -Text "Replaced the abstract strip with actual desktop layout mockups" -Left 72 -Top 624 -Width 520 -Height 24 -FontSize 14 -Color 0x738094 | Out-Null

    $slide.Shapes.AddPicture($assets.CalendarWeb, $false, $true, 88, 660, 470, 220) | Out-Null
    $slide.Shapes.AddPicture($assets.ChatWeb, $false, $true, 584, 660, 470, 220) | Out-Null

    Add-TextBox -Slide $slide -Text "Calendar web" -Left 258 -Top 852 -Width 120 -Height 20 -FontSize 13 -Color 0x5D6D82 -Bold $true | Out-Null
    Add-TextBox -Slide $slide -Text "Chat web" -Left 764 -Top 852 -Width 110 -Height 20 -FontSize 13 -Color 0x5D6D82 -Bold $true | Out-Null

    if (Test-Path $outputFullPath) {
        Remove-Item $outputFullPath -Force
    }

    $presentation.SaveAs($outputFullPath)
}
finally {
    if ($presentation) { $presentation.Close() }
    if ($ppt) { $ppt.Quit() }
    if ($presentation) { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($presentation) | Out-Null }
    if ($ppt) { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($ppt) | Out-Null }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}
