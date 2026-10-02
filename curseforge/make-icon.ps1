Add-Type -AssemblyName System.Drawing

$size = 512
$bmp = New-Object System.Drawing.Bitmap($size, $size)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

# --- background: radial-ish gradient, deep moss green to near-black ---
$path = New-Object System.Drawing.Drawing2D.GraphicsPath
$path.AddEllipse(-100, -100, $size + 200, $size + 200)
$pgBrush = New-Object System.Drawing.Drawing2D.PathGradientBrush($path)
$pgBrush.CenterColor = [System.Drawing.Color]::FromArgb(255, 0x22, 0x32, 0x26)
$pgBrush.SurroundColors = @([System.Drawing.Color]::FromArgb(255, 0x0C, 0x0F, 0x0B))
$pgBrush.CenterPoint = New-Object System.Drawing.PointF(($size*0.3), ($size*0.2))
$g.FillRectangle($pgBrush, 0, 0, $size, $size)

# --- concentric rings ---
$penEmber = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(140, 0xE6, 0x83, 0x57), 4)
$g.DrawEllipse($penEmber, 256-184, 256-184, 184*2, 184*2)

$penTeal = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(150, 0x6F, 0xC2, 0xAE), 4)
$g.DrawEllipse($penTeal, 256-132, 256-132, 132*2, 132*2)

$penDash = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(90, 0xE9, 0xEE, 0xE4), 2.5)
$penDash.DashStyle = [System.Drawing.Drawing2D.DashStyle]::Dash
$g.DrawEllipse($penDash, 256-80, 256-80, 80*2, 80*2)

# --- feather silhouette (precomputed rotated+translated bezier points) ---
$featherPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$pts = @(
  @(220.15,149.68), @(252.96,160.05), @(281.68,209.60), @(291.38,265.33),
  @(299.27,309.03), @(292.04,345.03), @(286.81,367.75),
  @(273.11,338.56), @(253.05,315.64), @(231.65,301.56),
  @(205.21,284.91), @(190.28,251.91), @(194.38,212.73),
  @(197.14,182.39), @(207.41,162.23), @(220.15,149.68)
)
$toPt = { param($p) New-Object System.Drawing.PointF($p[0], $p[1]) }
$featherPath.StartFigure()
$featherPath.AddBezier((& $toPt $pts[0]), (& $toPt $pts[1]), (& $toPt $pts[2]), (& $toPt $pts[3]))
$featherPath.AddBezier((& $toPt $pts[3]), (& $toPt $pts[4]), (& $toPt $pts[5]), (& $toPt $pts[6]))
$featherPath.AddBezier((& $toPt $pts[6]), (& $toPt $pts[7]), (& $toPt $pts[8]), (& $toPt $pts[9]))
$featherPath.AddBezier((& $toPt $pts[9]), (& $toPt $pts[10]), (& $toPt $pts[11]), (& $toPt $pts[12]))
$featherPath.AddBezier((& $toPt $pts[12]), (& $toPt $pts[13]), (& $toPt $pts[14]), (& $toPt $pts[15]))
$featherPath.CloseFigure()

$fillBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 0xE9, 0xEE, 0xE4))
$g.FillPath($fillBrush, $featherPath)
$outlinePen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 0x12, 0x17, 0x13), 3)
$g.DrawPath($outlinePen, $featherPath)

# faint shaft line
$shaftPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(160, 0x9B, 0xAB, 0x9A), 2)
$g.DrawBezier($shaftPen, (& $toPt $pts[0]), (& $toPt $pts[1]), (& $toPt $pts[2]), (& $toPt $pts[3]))
$g.DrawBezier($shaftPen, (& $toPt $pts[3]), (& $toPt $pts[4]), (& $toPt $pts[5]), (& $toPt $pts[6]))

# --- accent dots ---
$emberDot = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 0xE6, 0x83, 0x57))
$g.FillEllipse($emberDot, 368-11, 164-11, 22, 22)
$tealDot = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 0x6F, 0xC2, 0xAE))
$g.FillEllipse($tealDot, 148-11, 356-11, 22, 22)

$outDir = "C:\Users\iuryl\mods-dev\minspawn8\curseforge"
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }
$outPath = Join-Path $outDir "minspawn8-icon-512.png"
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)

$g.Dispose()
$bmp.Dispose()
Write-Output "Saved: $outPath"
