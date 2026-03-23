# slide7.png에서 실제 화면 영역을 잘라내는 스크립트
param(
    [string]$InputImage = "docs/ui-mockups/ppt-export/slide7.png",
    [string]$OutputDir = "docs/ui-mockups/ppt-export/crops"
)

$root = Split-Path -Parent $PSScriptRoot
$inputFullPath = Join-Path $root $InputImage
$outputFullDir = Join-Path $root $OutputDir

if (-not (Test-Path $inputFullPath)) {
    throw "Input image not found: $inputFullPath"
}

New-Item -ItemType Directory -Force -Path $outputFullDir | Out-Null
Add-Type -AssemblyName System.Drawing

$bitmap = [System.Drawing.Bitmap]::FromFile($inputFullPath)

$regions = @(
    @{Name="login-mobile.png"; X=72; Y=118; W=146; H=380},
    @{Name="calendar-mobile.png"; X=226; Y=118; W=150; H=380},
    @{Name="diary-mobile.png"; X=388; Y=118; W=152; H=380},
    @{Name="chat-mobile.png"; X=550; Y=118; W=150; H=380},
    @{Name="report-mobile.png"; X=712; Y=118; W=152; H=380},
    @{Name="calendar-web.png"; X=82; Y=646; W=396; H=140},
    @{Name="chat-web.png"; X=506; Y=646; W=396; H=140}
)

foreach ($region in $regions) {
    $rect = New-Object System.Drawing.Rectangle($region.X, $region.Y, $region.W, $region.H)
    $target = New-Object System.Drawing.Bitmap($region.W, $region.H)
    $graphics = [System.Drawing.Graphics]::FromImage($target)
    $graphics.DrawImage($bitmap, 0, 0, $rect, [System.Drawing.GraphicsUnit]::Pixel)
    $outputPath = Join-Path $outputFullDir $region.Name
    $target.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $target.Dispose()
}

$bitmap.Dispose()
