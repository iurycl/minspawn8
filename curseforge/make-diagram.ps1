Add-Type -AssemblyName System.Drawing

$w = 1280
$h = 720
$bmp = New-Object System.Drawing.Bitmap($w, $h)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit

# background
$path = New-Object System.Drawing.Drawing2D.GraphicsPath
$path.AddEllipse(-200, -200, $w + 400, $h + 400)
$pg = New-Object System.Drawing.Drawing2D.PathGradientBrush($path)
$pg.CenterColor = [System.Drawing.Color]::FromArgb(255, 0x1B, 0x28, 0x1F)
$pg.SurroundColors = @([System.Drawing.Color]::FromArgb(255, 0x0C, 0x0F, 0x0B))
$pg.CenterPoint = New-Object System.Drawing.PointF(($w*0.5), ($h*0.35))
$g.FillRectangle($pg, 0, 0, $w, $h)

$ink = [System.Drawing.Color]::FromArgb(255, 0xE9, 0xEE, 0xE4)
$muted = [System.Drawing.Color]::FromArgb(255, 0x9B, 0xAB, 0x9A)
$ember = [System.Drawing.Color]::FromArgb(255, 0xE6, 0x83, 0x57)
$teal = [System.Drawing.Color]::FromArgb(255, 0x6F, 0xC2, 0xAE)

$titleFont = New-Object System.Drawing.Font("Segoe UI", 34, [System.Drawing.FontStyle]::Bold)
$subFont = New-Object System.Drawing.Font("Segoe UI", 15, [System.Drawing.FontStyle]::Regular)
$labelFont = New-Object System.Drawing.Font("Consolas", 16, [System.Drawing.FontStyle]::Bold)
$pillFont = New-Object System.Drawing.Font("Consolas", 15, [System.Drawing.FontStyle]::Bold)

$g.DrawString("MinSpawn8 - zona de compensacao", $titleFont, (New-Object System.Drawing.SolidBrush($ink)), 60, 48)
$g.DrawString("a regiao marcada tem uma taxa de spawn propria; ao redor dela, o efeito e espelhado", $subFont, (New-Object System.Drawing.SolidBrush($muted)), 60, 100)

# outer ring (dashed, teal) - compensation zone
$outerRect = New-Object System.Drawing.Rectangle(260, 190, 760, 420)
$dashPen = New-Object System.Drawing.Pen($teal, 3)
$dashPen.DashStyle = [System.Drawing.Drawing2D.DashStyle]::Dash
$g.DrawRectangle($dashPen, $outerRect)

# inner rect (solid, ember) - the region itself
$innerRect = New-Object System.Drawing.Rectangle(460, 290, 360, 220)
$innerFill = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(40, 0xE6, 0x83, 0x57))
$g.FillRectangle($innerFill, $innerRect)
$innerPen = New-Object System.Drawing.Pen($ember, 4)
$g.DrawRectangle($innerPen, $innerRect)

function Draw-Pill {
    param($gfx, [string]$text, [int]$x, [int]$y, [int]$pw, [System.Drawing.Color]$color, $font)
    $ph = 40
    $rect = New-Object System.Drawing.Rectangle($x, $y, $pw, $ph)
    $bgBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(230, $color.R, $color.G, $color.B))
    $gp = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = 18
    $gp.AddArc($rect.X, $rect.Y, $d, $d, 180, 90)
    $gp.AddArc($rect.X + $rect.Width - $d, $rect.Y, $d, $d, 270, 90)
    $gp.AddArc($rect.X + $rect.Width - $d, $rect.Y + $rect.Height - $d, $d, $d, 0, 90)
    $gp.AddArc($rect.X, $rect.Y + $rect.Height - $d, $d, $d, 90, 90)
    $gp.CloseFigure()
    $gfx.FillPath($bgBrush, $gp)
    $textBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255,0x12,0x17,0x13))
    $gfx.DrawString($text, $font, $textBrush, [single]($x + 16), [single]($y + 9))
}

Draw-Pill $g "regiao +50%" 500 335 220 $ember $pillFont
Draw-Pill $g "zona ao redor: -50% (efetivo 50%)" 260 630 460 $teal $pillFont

$g.DrawString("regiao", $labelFont, (New-Object System.Drawing.SolidBrush($ember)), 470, 300)
$g.DrawString("zona de compensacao", $labelFont, (New-Object System.Drawing.SolidBrush($teal)), 270, 160)

$footFont = New-Object System.Drawing.Font("Consolas", 13, [System.Drawing.FontStyle]::Regular)
$g.DrawString("exemplo: uma selecao de 20x20 blocos cria uma zona de compensacao de +20 blocos para cada lado", $footFont, (New-Object System.Drawing.SolidBrush($muted)), 60, 685)

$outDir = "C:\Users\iuryl\mods-dev\minspawn8\curseforge"
$outPath = Join-Path $outDir "minspawn8-diagram-compensacao.png"
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()
Write-Output "Saved: $outPath"
