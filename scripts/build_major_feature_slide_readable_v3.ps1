# Build a readable one-slide PPTX for major feature screens.
$ErrorActionPreference = "Stop"

function Decode-Text($base64) {
    return [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($base64))
}

function Add-Label($slide, $left, $top, $width, $height, $text, $fontSize, $bold, $colorRgb) {
    $shape = $slide.Shapes.AddTextbox(1, $left, $top, $width, $height)
    $shape.TextFrame.TextRange.Text = $text
    $shape.TextFrame.TextRange.Font.Name = "Malgun Gothic"
    $shape.TextFrame.TextRange.Font.Size = $fontSize
    $shape.TextFrame.TextRange.Font.Bold = [bool]$bold
    $shape.TextFrame.TextRange.Font.Color.RGB = $colorRgb
    $shape.TextFrame.MarginLeft = 0
    $shape.TextFrame.MarginRight = 0
    $shape.TextFrame.MarginTop = 0
    $shape.TextFrame.MarginBottom = 0
    return $shape
}

function Add-Card($slide, $left, $top, $width, $height, $fillRgb, $lineRgb) {
    $shape = $slide.Shapes.AddShape(1, $left, $top, $width, $height)
    $shape.Fill.Visible = -1
    $shape.Fill.ForeColor.RGB = $fillRgb
    $shape.Line.Visible = -1
    $shape.Line.ForeColor.RGB = $lineRgb
    $shape.Line.Weight = 1
    return $shape
}

$root = "C:\programing\mindcompass"
$outDir = Join-Path $root "docs\ui-mockups"
$pptPath = Join-Path $outDir "major-feature-slide-readable-v3.pptx"

$calendarMobile = Join-Path $root "docs\ui-mockups\ppt-export\crops\calendar-mobile.png"
$diaryMobile = Join-Path $root "docs\ui-mockups\ppt-export\crops\diary-mobile.png"
$chatMobile = Join-Path $root "docs\ui-mockups\ppt-export\crops\chat-mobile.png"
$reportMobile = Join-Path $root "docs\ui-mockups\ppt-export\crops\report-mobile.png"
$calendarWeb = Join-Path $root "docs\ui-mockups\ppt-export\crops\calendar-web.png"
$chatWeb = Join-Path $root "docs\ui-mockups\ppt-export\crops\chat-web.png"

foreach ($path in @($calendarMobile, $diaryMobile, $chatMobile, $reportMobile, $calendarWeb, $chatWeb)) {
    if (-not (Test-Path $path)) {
        throw "Missing asset: $path"
    }
}

if (Test-Path $pptPath) {
    Remove-Item $pptPath -Force
}

$titleMain = Decode-Text "7KO87JqUIOq4sOuKpSDtmZTrqbQ="
$labelMobile = Decode-Text "66qo67CU7J28IO2VteyLrCDtmZTrqbQ="
$labelWeb = Decode-Text "7Ju5IOyYiOyLnCDtmZTrqbQ="
$textCalendar = Decode-Text "7LqY66aw642U"
$textCalendarDesc = Decode-Text "7JuU6rCEIOqwkOyglSDtnZDrpoTqs7wg64Kg7Kec67OEIOq4sOuhnSDtmZXsnbg="
$textDiary = Decode-Text "7J286riwIOyDgeyEuA=="
$textDiaryDesc = Decode-Text "6riw66GdIOuCtOyaqSwgQUkg67aE7ISdLCDsnITtl5jrj4Qg6rKw6rO8IO2ZleyduA=="
$textChat = Decode-Text "7LGE7YyF"
$textChatDesc = Decode-Text "6rCQ7KCVIOuMgO2ZlOyZgCBBSSDsnZHri7Ug7Z2Q66aEIOygnOqztQ=="
$textReport = Decode-Text "66as7Y+s7Yq4"
$textReportDesc = Decode-Text "6rCQ7KCVIO2MqO2EtOqzvCDsnITtl5jrj4Qg7LaU7J20IO2ajOqzoA=="
$textCalendarWeb = Decode-Text "7LqY66aw642UIOybuQ=="
$textCalendarWebDesc = Decode-Text "7JuU6rCEIOqwkOygleqzvCDrgqDsp5zrs4Qg7IOB7YOc66W8IOuEk+ydgCDtmZTrqbTsl5DshJwg7ZWc64iI7JeQIO2ZleyduA=="
$textChatWeb = Decode-Text "7LGE7YyFIOybuQ=="
$textChatWebDesc = Decode-Text "7IS47IWYIOuqqeuhneqzvCDrjIDtmZQg7JiB7Jet7J2EIOuPmeyLnOyXkCDrs7Tsl6zso7zripQg6rWs7KGw"
$textEtc = Decode-Text "6riw7YOAIO2ZlOuptCDshKTrqoU="
$bullet1 = Decode-Text "66Gc6re47J24IO2ZlOuptDog7ISc67mE7IqkIOynhOyeheqzvCDqs4TsoJUg7J247Kad7J2EIOuLtOuLue2VmOuKlCDssqsg7ZmU66m0"
$bullet2 = Decode-Text "7J286riwIOyekeyEsSDtmZTrqbQ6IOygnOuqqSwg67O466y4LCDqsJDsoJUg6rCV64+E66W8IOyeheugpe2VmOqzoCDsoIDsnqXtlZjripQg6riw66GdIOyLnOyekeygkA=="
$bullet3 = Decode-Text "7KO87JqUIO2dkOumhDog7LqY66aw642UIOKGkiDsnbzquLAg7IOB7IS4IOKGkiDssYTtjIUg4oaSIOumrO2PrO2KuCDsiJzsnLzroZwg7J6Q7Jew7Iqk65+96rKMIOyXsOqysA=="
$bullet4 = Decode-Text "7ISk6rOEIOydmOuPhDog7ZWcIO2ZlOuptOyXkCDtlbXsi6wg6riw64ql7J2EIOu5oOultOqyjCDrs7Tsl6zso7zrkJgg7Iuk7KCcIOyEnOu5hOyKpCDtmZTrqbQg7JyE7KO866GcIOq1rOyEsQ=="
$footerText = Decode-Text "7Iuk7KCcIO2ZlOuptCDsnITso7zroZwg7KCV66as7ZW0IOq4sOuKpSDtnZDrpoTqs7wg6rWs7ZiEIOuylOychOulvCDruaDrpbTqsowg7ISk66qF7ZWgIOyImCDsnojrj4TroZ0g6rWs7ISx"

$ppt = New-Object -ComObject PowerPoint.Application
$presentation = $ppt.Presentations.Add()
$presentation.PageSetup.SlideWidth = 960
$presentation.PageSetup.SlideHeight = 540

$slide = $presentation.Slides.Add(1, 12)
$slide.Background.Fill.Visible = -1
$slide.Background.Fill.ForeColor.RGB = 237 + (256 * 243) + (65536 * 247)
$slide.Background.Fill.Solid()

$titleColor = 44 + (256 * 44) + (65536 * 44)
$mutedColor = 108 + (256 * 102) + (65536 * 97)
$accentColor = 81 + (256 * 117) + (65536 * 103)
$cardFill = 252 + (256 * 251) + (65536 * 249)
$cardLine = 227 + (256 * 221) + (65536 * 214)

Add-Label $slide 52 26 280 28 $titleMain 24 -1 $titleColor | Out-Null
$line = $slide.Shapes.AddLine(52, 66, 908, 66)
$line.Line.ForeColor.RGB = $cardLine
$line.Line.Weight = 1

Add-Label $slide 52 82 130 20 $labelMobile 13 -1 $accentColor | Out-Null
Add-Label $slide 694 82 120 20 $labelWeb 13 -1 $accentColor | Out-Null

$mobileCards = @(
    @{ x = 52;  y = 112; w = 150; h = 150; image = $calendarMobile; title = $textCalendar; desc = $textCalendarDesc },
    @{ x = 218; y = 112; w = 150; h = 150; image = $diaryMobile;    title = $textDiary; desc = $textDiaryDesc },
    @{ x = 384; y = 112; w = 150; h = 150; image = $chatMobile;     title = $textChat; desc = $textChatDesc },
    @{ x = 550; y = 112; w = 150; h = 150; image = $reportMobile;   title = $textReport; desc = $textReportDesc }
)

foreach ($item in $mobileCards) {
    Add-Card $slide $item.x $item.y $item.w $item.h $cardFill $cardLine | Out-Null
    $pic = $slide.Shapes.AddPicture($item.image, 0, -1, $item.x + 14, $item.y + 14, $item.w - 28, 92)
    $pic.LockAspectRatio = -1
    Add-Label $slide ($item.x + 14) ($item.y + 110) ($item.w - 28) 18 $item.title 13 -1 $titleColor | Out-Null
    $descShape = Add-Label $slide ($item.x + 14) ($item.y + 130) ($item.w - 28) 28 $item.desc 9 0 $mutedColor
    $descShape.TextFrame.WordWrap = -1
}

Add-Card $slide 694 112 214 150 $cardFill $cardLine | Out-Null
$web1 = $slide.Shapes.AddPicture($calendarWeb, 0, -1, 708, 126, 186, 80)
$web1.LockAspectRatio = -1
Add-Label $slide 708 212 186 16 $textCalendarWeb 12 -1 $titleColor | Out-Null
Add-Label $slide 708 228 186 18 $textCalendarWebDesc 9 0 $mutedColor | Out-Null

Add-Card $slide 694 278 214 110 $cardFill $cardLine | Out-Null
$web2 = $slide.Shapes.AddPicture($chatWeb, 0, -1, 708, 292, 94, 54)
$web2.LockAspectRatio = -1
Add-Label $slide 814 296 80 16 $textChatWeb 12 -1 $titleColor | Out-Null
$chatWebText = Add-Label $slide 814 314 80 30 $textChatWebDesc 9 0 $mutedColor
$chatWebText.TextFrame.WordWrap = -1

Add-Card $slide 52 290 632 150 $cardFill $cardLine | Out-Null
Add-Label $slide 68 308 120 18 $textEtc 13 -1 $titleColor | Out-Null

$bullets = @($bullet1, $bullet2, $bullet3, $bullet4)
$top = 332
foreach ($bullet in $bullets) {
    $shape = Add-Label $slide 76 $top 590 18 $bullet 11 0 $mutedColor
    $shape.TextFrame.WordWrap = -1
    $top += 24
}

$footer = Add-Label $slide 694 404 214 34 $footerText 10 0 $mutedColor
$footer.TextFrame.WordWrap = -1

$presentation.SaveAs($pptPath)
$presentation.Close()
$ppt.Quit()

[System.Runtime.Interopservices.Marshal]::ReleaseComObject($line) | Out-Null
[System.Runtime.Interopservices.Marshal]::ReleaseComObject($slide) | Out-Null
[System.Runtime.Interopservices.Marshal]::ReleaseComObject($presentation) | Out-Null
[System.Runtime.Interopservices.Marshal]::ReleaseComObject($ppt) | Out-Null
[GC]::Collect()
[GC]::WaitForPendingFinalizers()

Write-Host "Created $pptPath"
