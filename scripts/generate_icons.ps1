Add-Type -AssemblyName System.Drawing

[xml]$xml = Get-Content 'app/src/main/res/drawable/ic_launcher_foreground.xml'
$bgCol = [System.Drawing.ColorTranslator]::FromHtml('#FF5A4E')

function Render-Icon([int]$size, [bool]$isRound, [string]$outPath) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    $scale = [float]$size / 108.0
    $bgBrush = New-Object System.Drawing.SolidBrush($bgCol)

    if ($isRound) {
        $pad = [float]($size * 0.04)
        $g.FillEllipse($bgBrush, $pad, $pad, [float]($size - 2 * $pad), [float]($size - 2 * $pad))
    } else {
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
    }
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
    Write-Output "Generated $outPath ($size x $size)"
}

Render-Icon 48 $false 'app/src/main/res/mipmap-mdpi/ic_launcher.png'
Render-Icon 48 $true  'app/src/main/res/mipmap-mdpi/ic_launcher_round.png'
Render-Icon 72 $false 'app/src/main/res/mipmap-hdpi/ic_launcher.png'
Render-Icon 72 $true  'app/src/main/res/mipmap-hdpi/ic_launcher_round.png'
Render-Icon 96 $false 'app/src/main/res/mipmap-xhdpi/ic_launcher.png'
Render-Icon 96 $true  'app/src/main/res/mipmap-xhdpi/ic_launcher_round.png'
Render-Icon 144 $false 'app/src/main/res/mipmap-xxhdpi/ic_launcher.png'
Render-Icon 144 $true  'app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png'
Render-Icon 192 $false 'app/src/main/res/mipmap-xxxhdpi/ic_launcher.png'
Render-Icon 192 $true  'app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png'
