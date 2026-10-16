Add-Type -AssemblyName System.Drawing

$ink = [System.Drawing.Color]::FromArgb(255, 0xE9, 0xEE, 0xE4)
$muted = [System.Drawing.Color]::FromArgb(255, 0x9B, 0xAB, 0x9A)
$ember = [System.Drawing.Color]::FromArgb(255, 0xE6, 0x83, 0x57)
$teal = [System.Drawing.Color]::FromArgb(255, 0x6F, 0xC2, 0xAE)
$dark = [System.Drawing.Color]::FromArgb(255, 0x12, 0x17, 0x13)

function New-Canvas {
    $bmp = New-Object System.Drawing.Bitmap(1280, 720)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddEllipse(-200, -200, 1680, 1120)
    $pg = New-Object System.Drawing.Drawing2D.PathGradientBrush($path)
    $pg.CenterColor = [System.Drawing.Color]::FromArgb(255, 0x1B, 0x28, 0x1F)
    $pg.SurroundColors = @([System.Drawing.Color]::FromArgb(255, 0x0C, 0x0F, 0x0B))
    $pg.CenterPoint = New-Object System.Drawing.PointF(640, 250)
    $g.FillRectangle($pg, 0, 0, 1280, 720)
    return @($bmp, $g)
}

function Draw-Pill {
    param($gfx, [string]$text, [int]$x, [int]$y, [int]$pw, [System.Drawing.Color]$color, $font)
    $rect = New-Object System.Drawing.Rectangle($x, $y, $pw, 40)
    $bgBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(230, $color.R, $color.G, $color.B))
    $gp = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = 18
    $gp.AddArc($rect.X, $rect.Y, $d, $d, 180, 90)
    $gp.AddArc($rect.X + $rect.Width - $d, $rect.Y, $d, $d, 270, 90)
    $gp.AddArc($rect.X + $rect.Width - $d, $rect.Y + $rect.Height - $d, $d, $d, 0, 90)
    $gp.AddArc($rect.X, $rect.Y + $rect.Height - $d, $d, $d, 90, 90)
    $gp.CloseFigure()
    $gfx.FillPath($bgBrush, $gp)
    $textBrush = New-Object System.Drawing.SolidBrush($dark)
    $gfx.DrawString($text, $font, $textBrush, [single]($x + 16), [single]($y + 9))
}

function Draw-Feather {
    param($gfx, [single]$cx, [single]$cy, [single]$s)
    $pts = @(
      @(220.15,149.68), @(252.96,160.05), @(281.68,209.60), @(291.38,265.33),
      @(299.27,309.03), @(292.04,345.03), @(286.81,367.75),
      @(273.11,338.56), @(253.05,315.64), @(231.65,301.56),
      @(205.21,284.91), @(190.28,251.91), @(194.38,212.73),
      @(197.14,182.39), @(207.41,162.23), @(220.15,149.68)
    )
    $toPt = { param($p) New-Object System.Drawing.PointF($p[0], $p[1]) }
    $fp = New-Object System.Drawing.Drawing2D.GraphicsPath
    $fp.StartFigure()
    $fp.AddBezier((& $toPt $pts[0]), (& $toPt $pts[1]), (& $toPt $pts[2]), (& $toPt $pts[3]))
    $fp.AddBezier((& $toPt $pts[3]), (& $toPt $pts[4]), (& $toPt $pts[5]), (& $toPt $pts[6]))
    $fp.AddBezier((& $toPt $pts[6]), (& $toPt $pts[7]), (& $toPt $pts[8]), (& $toPt $pts[9]))
    $fp.AddBezier((& $toPt $pts[9]), (& $toPt $pts[10]), (& $toPt $pts[11]), (& $toPt $pts[12]))
    $fp.AddBezier((& $toPt $pts[12]), (& $toPt $pts[13]), (& $toPt $pts[14]), (& $toPt $pts[15]))
    $fp.CloseFigure()
    $tx = $cx - 245 * $s
    $ty = $cy - 259 * $s
    $gfx.TranslateTransform($tx, $ty)
    $gfx.ScaleTransform($s, $s)
    $fill = New-Object System.Drawing.SolidBrush($ink)
    $gfx.FillPath($fill, $fp)
    $pen = New-Object System.Drawing.Pen($dark, 3)
    $gfx.DrawPath($pen, $fp)
    $gfx.ResetTransform()
}

function Draw-Stick {
    param($gfx, [single]$cx, [single]$cy, [single]$len)
    $half = $len / 2
    $outline = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 0x3A, 0x2A, 0x1C), 14)
    $outline.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $outline.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $gfx.DrawLine($outline, ($cx - $half), ($cy + $half), ($cx + $half), ($cy - $half))
    $wood = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 0xB8, 0x8B, 0x57), 8)
    $wood.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $wood.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $gfx.DrawLine($wood, ($cx - $half), ($cy + $half), ($cx + $half), ($cy - $half))
}

$outDir = "C:\Users\iuryl\mods-dev\minspawn8\curseforge"

# ---------- Image 1: compensation zone (English) ----------
$c = New-Canvas
$bmp = $c[0]; $g = $c[1]
$titleFont = New-Object System.Drawing.Font("Segoe UI", 34, [System.Drawing.FontStyle]::Bold)
$subFont = New-Object System.Drawing.Font("Segoe UI", 15, [System.Drawing.FontStyle]::Regular)
$labelFont = New-Object System.Drawing.Font("Consolas", 16, [System.Drawing.FontStyle]::Bold)
$pillFont = New-Object System.Drawing.Font("Consolas", 15, [System.Drawing.FontStyle]::Bold)
$footFont = New-Object System.Drawing.Font("Consolas", 13, [System.Drawing.FontStyle]::Regular)

$g.DrawString("MinSpawn8 - compensation zone", $titleFont, (New-Object System.Drawing.SolidBrush($ink)), 60, 48)
$g.DrawString("a marked region has its own spawn rate; the area around it gets the mirrored effect", $subFont, (New-Object System.Drawing.SolidBrush($muted)), 60, 100)

$dashPen = New-Object System.Drawing.Pen($teal, 3)
$dashPen.DashStyle = [System.Drawing.Drawing2D.DashStyle]::Dash
$g.DrawRectangle($dashPen, (New-Object System.Drawing.Rectangle(260, 190, 760, 420)))

$innerRect = New-Object System.Drawing.Rectangle(460, 290, 360, 220)
$g.FillRectangle((New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(40, 0xE6, 0x83, 0x57))), $innerRect)
$g.DrawRectangle((New-Object System.Drawing.Pen($ember, 4)), $innerRect)

Draw-Pill $g "region +50%" 500 335 220 $ember $pillFont
Draw-Pill $g "surrounding zone: -50% (effective 50%)" 260 630 520 $teal $pillFont
$g.DrawString("region", $labelFont, (New-Object System.Drawing.SolidBrush($ember)), 470, 300)
$g.DrawString("compensation zone", $labelFont, (New-Object System.Drawing.SolidBrush($teal)), 270, 160)
$g.DrawString("example: a 20x20 block selection creates a compensation zone of +20 blocks on every side", $footFont, (New-Object System.Drawing.SolidBrush($muted)), 60, 685)
$bmp.Save((Join-Path $outDir "minspawn8-diagram-compensation-en.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()

# ---------- Image 2: crafting recipe (English) ----------
$c = New-Canvas
$bmp = $c[0]; $g = $c[1]
$g.DrawString("MinSpawn8 - Spawn Selection Feather", $titleFont, (New-Object System.Drawing.SolidBrush($ink)), 60, 48)
$g.DrawString("craftable by any player, no operator needed - shovel-shaped recipe", $subFont, (New-Object System.Drawing.SolidBrush($muted)), 60, 100)

$cell = 130
$gap = 10
$gx = 260
$gy = 190
$slotBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 0x23, 0x2D, 0x24))
$slotPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 0x3B, 0x4A, 0x3C), 3)
for ($r = 0; $r -lt 3; $r++) {
    for ($k = 0; $k -lt 3; $k++) {
        $rx = $gx + $k * ($cell + $gap)
        $ry = $gy + $r * ($cell + $gap)
        $g.FillRectangle($slotBrush, $rx, $ry, $cell, $cell)
        $g.DrawRectangle($slotPen, $rx, $ry, $cell, $cell)
    }
}
$midX = $gx + ($cell + $gap) + $cell / 2
Draw-Feather $g $midX ($gy + $cell / 2) 0.5
Draw-Stick $g $midX ($gy + ($cell + $gap) + $cell / 2) 70
Draw-Stick $g $midX ($gy + 2 * ($cell + $gap) + $cell / 2) 70

# arrow
$arrowPen = New-Object System.Drawing.Pen($muted, 8)
$arrowPen.EndCap = [System.Drawing.Drawing2D.LineCap]::ArrowAnchor
$g.DrawLine($arrowPen, 700, 380, 810, 380)

# result slot
$g.FillRectangle($slotBrush, 860, 315, 150, 150)
$g.DrawRectangle((New-Object System.Drawing.Pen($teal, 4)), 860, 315, 150, 150)
Draw-Feather $g 935 390 0.58

$g.DrawString("Spawn Selection Feather", $labelFont, (New-Object System.Drawing.SolidBrush($teal)), 820, 485)
$g.DrawString("minspawn8:spawn_feather", $footFont, (New-Object System.Drawing.SolidBrush($muted)), 830, 515)

$g.DrawString("1x Feather (top)", $pillFont, (New-Object System.Drawing.SolidBrush($ink)), 260, 625)
$g.DrawString("2x Stick (middle + bottom)", $pillFont, (New-Object System.Drawing.SolidBrush($ink)), 260, 655)
$g.DrawString("Right-click 2 blocks to create a region", $footFont, (New-Object System.Drawing.SolidBrush($muted)), 640, 655)
$bmp.Save((Join-Path $outDir "minspawn8-craft-recipe-en.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()

Write-Output "done"
