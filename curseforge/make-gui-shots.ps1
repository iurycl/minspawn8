Add-Type -AssemblyName System.Drawing
$src = "C:\Users\iuryl\AppData\Local\Temp\claude\C--Users-iuryl-Claude-Projects-Eixus\0c5dd2d6-a84a-43da-8d61-60acb9d3b2f7\images"
$out = "C:\Users\iuryl\mods-dev\minspawn8\docs"
$scratch = "C:\Users\iuryl\AppData\Local\Temp\claude\C--Users-iuryl-Claude-Projects-Eixus\0c5dd2d6-a84a-43da-8d61-60acb9d3b2f7\scratchpad"
New-Item -ItemType Directory -Force $out | Out-Null

$shots = @(
  @{ In = "6.png"; Name = "gui-regions"; Title = "Regions list"; Sub = "Every region you marked, one click away (/minspawn8 gui)" },
  @{ In = "7.png"; Name = "gui-region"; Title = "Region editor"; Sub = "Spawn rates, mob list and strict mode in one screen" },
  @{ In = "8.png"; Name = "gui-mob-picker"; Title = "Mob picker"; Sub = "Pick the mobs allowed to spawn with spawn eggs" }
)
$scale = 4
foreach ($s in $shots) {
  $img = [System.Drawing.Image]::FromFile((Join-Path $src $s.In))
  $w = $img.Width * $scale
  $h = $img.Height * $scale
  $barH = 110
  $bmp = New-Object System.Drawing.Bitmap($w, ($h + $barH))
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.Clear([System.Drawing.Color]::FromArgb(24, 26, 32))
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
  $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
  $g.DrawImage($img, 0, 0, $w, $h)
  $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
  $f1 = New-Object System.Drawing.Font("Segoe UI", 30, [System.Drawing.FontStyle]::Bold)
  $f2 = New-Object System.Drawing.Font("Segoe UI", 17)
  $g.DrawString($s.Title, $f1, [System.Drawing.Brushes]::White, 24, ($h + 8))
  $g.DrawString($s.Sub, $f2, (New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(170, 180, 195))), 26, ($h + 64))
  $g.Dispose()
  $bmp.Save((Join-Path $out ($s.Name + ".png")), [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Save((Join-Path $scratch ($s.Name + ".png")), [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose(); $img.Dispose()
}
Write-Output "done"
