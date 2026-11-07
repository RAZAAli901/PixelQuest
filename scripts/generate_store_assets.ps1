Add-Type -AssemblyName System.Drawing

# 1. Generate 512x512 Store Icon
[xml]$xml = Get-Content 'app/src/main/res/drawable/ic_launcher_foreground.xml'
$bgCol = [System.Drawing.ColorTranslator]::FromHtml('#FF5A4E')

function Render-StoreIcon([string]$outPath) {
    $size = 512
    $bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    $scale = [float]$size / 108.0
    $bgBrush = New-Object System.Drawing.SolidBrush($bgCol)

    $pad = [float]($size * 0.04)
    $radius = [float]($size * 0.20)
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $rect = New-Object System.Drawing.RectangleF($pad, $pad, [float]($size - 2 * $pad), [float]($size - 2 * $pad))
    $d = [float]($radius * 2)
    $path.AddArc($rect.X, $rect.Y, $d, $d, 180, 90)
    $path.AddArc($rect.Right - $d, $rect.Y, $d, $d, 270, 90)
    $path.AddArc($rect.Right - $d, $rect.Bottom - $d, $d, $d, 0, 90)
    $path.AddArc($rect.X, $rect.Bottom - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    $g.FillPath($bgBrush, $path)
    $path.Dispose()
    $bgBrush.Dispose()

    # Draw foreground paths
    foreach ($pathNode in $xml.vector.path) {
        $colorHex = $pathNode.fillColor
        if ($colorHex.Length -eq 9) {
            $a = [Convert]::ToInt32($colorHex.Substring(1, 2), 16)
            $r = [Convert]::ToInt32($colorHex.Substring(3, 2), 16)
            $gCol = [Convert]::ToInt32($colorHex.Substring(5, 2), 16)
            $b = [Convert]::ToInt32($colorHex.Substring(7, 2), 16)
            $col = [System.Drawing.Color]::FromArgb($a, $r, $gCol, $b)
        } else {
            $col = [System.Drawing.ColorTranslator]::FromHtml($colorHex)
        }
        $brush = New-Object System.Drawing.SolidBrush($col)
        $d = $pathNode.pathData
        $matches = [regex]::Matches($d, 'M([\d\.]+),([\d\.]+)h([\d\.]+)v([\d\.]+)h-?[\d\.]+z')
        foreach ($m in $matches) {
            $x = [float]::Parse($m.Groups[1].Value, [System.Globalization.CultureInfo]::InvariantCulture) * $scale
            $y = [float]::Parse($m.Groups[2].Value, [System.Globalization.CultureInfo]::InvariantCulture) * $scale
            $w = [float]::Parse($m.Groups[3].Value, [System.Globalization.CultureInfo]::InvariantCulture) * $scale
            $h = [float]::Parse($m.Groups[4].Value, [System.Globalization.CultureInfo]::InvariantCulture) * $scale
            $g.FillRectangle($brush, $x, $y, $w, $h)
        }
        $brush.Dispose()
    }

    $g.Dispose()
    $dir = [System.IO.Path]::GetDirectoryName($outPath)
    if (-not (Test-Path $dir)) { [System.IO.Directory]::CreateDirectory($dir) | Out-Null }
    $bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output "Generated Store Icon: $outPath ($size x $size)"
}

# 2. Generate 1024x500 Feature Graphic
function Render-FeatureGraphic([string]$outPath) {
    $width = 1024
    $height = 500
    $bmp = New-Object System.Drawing.Bitmap($width, $height, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

    # Background gradient
    $bgBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.Point(0, 0)),
        (New-Object System.Drawing.Point($width, $height)),
        ([System.Drawing.ColorTranslator]::FromHtml('#181528')),
        ([System.Drawing.ColorTranslator]::FromHtml('#2D1B2D'))
    )
    $g.FillRectangle($bgBrush, 0, 0, $width, $height)
    $bgBrush.Dispose()

    # Subtle retro grid
    $gridPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(20, 255, 255, 255), 1)
    for ($x = 0; $x -lt $width; $x += 32) {
        $g.DrawLine($gridPen, $x, 0, $x, $height)
    }
    for ($y = 0; $y -lt $height; $y += 32) {
        $g.DrawLine($gridPen, 0, $y, $width, $y)
    }
    $gridPen.Dispose()

    # Draw Badge Logo on left (size 320x320)
    $iconX = 80
    $iconY = 90
    $iconSize = 320
    $scale = [float]$iconSize / 108.0

    # Badge background
    $badgeBrush = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml('#FF5A4E'))
    $radius = $iconSize * 0.18
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $rect = New-Object System.Drawing.RectangleF($iconX, $iconY, $iconSize, $iconSize)
    $d = $radius * 2
    $path.AddArc($rect.X, $rect.Y, $d, $d, 180, 90)
    $path.AddArc($rect.Right - $d, $rect.Y, $d, $d, 270, 90)
    $path.AddArc($rect.Right - $d, $rect.Bottom - $d, $d, $d, 0, 90)
    $path.AddArc($rect.X, $rect.Bottom - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    
    # Drop shadow
    $shadowBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(100, 0, 0, 0))
    $g.FillRectangle($shadowBrush, ($iconX + 10), ($iconY + 10), $iconSize, $iconSize)
    $shadowBrush.Dispose()

    $g.FillPath($badgeBrush, $path)
    $path.Dispose()
    $badgeBrush.Dispose()

    # Draw sword foreground inside badge
    foreach ($pathNode in $xml.vector.path) {
        $colorHex = $pathNode.fillColor
        if ($colorHex.Length -eq 9) {
            $a = [Convert]::ToInt32($colorHex.Substring(1, 2), 16)
            $r = [Convert]::ToInt32($colorHex.Substring(3, 2), 16)
            $gCol = [Convert]::ToInt32($colorHex.Substring(5, 2), 16)
            $b = [Convert]::ToInt32($colorHex.Substring(7, 2), 16)
            $col = [System.Drawing.Color]::FromArgb($a, $r, $gCol, $b)
        } else {
            $col = [System.Drawing.ColorTranslator]::FromHtml($colorHex)
        }
        $brush = New-Object System.Drawing.SolidBrush($col)
        $d = $pathNode.pathData
        $matches = [regex]::Matches($d, 'M([\d\.]+),([\d\.]+)h([\d\.]+)v([\d\.]+)h-?[\d\.]+z')
        foreach ($m in $matches) {
            $x = $iconX + ([float]::Parse($m.Groups[1].Value, [System.Globalization.CultureInfo]::InvariantCulture) * $scale)
            $y = $iconY + ([float]::Parse($m.Groups[2].Value, [System.Globalization.CultureInfo]::InvariantCulture) * $scale)
            $w = [float]::Parse($m.Groups[3].Value, [System.Globalization.CultureInfo]::InvariantCulture) * $scale
            $h = [float]::Parse($m.Groups[4].Value, [System.Globalization.CultureInfo]::InvariantCulture) * $scale
            $g.FillRectangle($brush, $x, $y, $w, $h)
        }
        $brush.Dispose()
    }

    # Text layout
    $textX = 460
    $titleFont = New-Object System.Drawing.Font('Arial', 44, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $subFont = New-Object System.Drawing.Font('Arial', 22, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $descFont = New-Object System.Drawing.Font('Arial', 16, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
    $pillFont = New-Object System.Drawing.Font('Arial', 14, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)

    $goldBrush = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml('#FFD166'))
    $shadowTextBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(180, 0, 0, 0))
    $whiteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml('#FFFFFF'))
    $grayBrush = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml('#D0C8D8'))

    # Title with shadow
    $g.DrawString("PixelQuest", $titleFont, $shadowTextBrush, ($textX + 3), 113)
    $g.DrawString("PixelQuest", $titleFont, $goldBrush, $textX, 110)

    # Subtitle & Description
    $g.DrawString("Level Up Your Real Life", $subFont, $whiteBrush, $textX, 185)
    $g.DrawString("Turn daily tasks and study sessions into an 8-bit RPG adventure.", $descFont, $grayBrush, $textX, 230)

    # Feature badges
    $badges = @("Gamified Habits", "Study Parties", "Islamic Hub", "Cloud Sync")
    $pillX = $textX
    $pillY = 290
    $pillBg = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(40, 255, 255, 255))
    $pillBorder = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(90, 255, 255, 255), 1)

    foreach ($b in $badges) {
        $measure = $g.MeasureString($b, $pillFont)
        $w = $measure.Width + 24
        $h = 36
        $pillRect = New-Object System.Drawing.RectangleF($pillX, $pillY, $w, $h)
        $g.FillRectangle($pillBg, $pillRect)
        $g.DrawRectangle($pillBorder, $pillRect.X, $pillRect.Y, $pillRect.Width, $pillRect.Height)
        $g.DrawString($b, $pillFont, $whiteBrush, ($pillX + 12), ($pillY + 8))
        $pillX += ($w + 14)
        if ($pillX -gt 920) {
            $pillX = $textX
            $pillY += 46
        }
    }

    # Gold frame border
    $framePen = New-Object System.Drawing.Pen([System.Drawing.ColorTranslator]::FromHtml('#FFD166'), 4)
    $g.DrawRectangle($framePen, 2, 2, ($width - 4), ($height - 4))
    $framePen.Dispose()

    $g.Dispose()
    $dir = [System.IO.Path]::GetDirectoryName($outPath)
    if (-not (Test-Path $dir)) { [System.IO.Directory]::CreateDirectory($dir) | Out-Null }
    $bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output "Generated Feature Graphic: $outPath ($width x $height)"
}

Render-StoreIcon 'docs/store/icon-512.png'
Render-FeatureGraphic 'docs/store/feature-graphic.png'
