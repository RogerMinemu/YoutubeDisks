Add-Type -AssemblyName System.Drawing

$dir = 'Z:\Proyectos\YoutubeDisks\src\main\resources\assets\youtubedisks\textures'

function New-Bitmap16 { New-Object System.Drawing.Bitmap 16, 16 }

function C { param([int]$r, [int]$g, [int]$b, [int]$a = 255)
    return [System.Drawing.Color]::FromArgb($a, $r, $g, $b) }

function Save-Bmp { param($bmp, [string]$relPath)
    $full = Join-Path $dir $relPath
    $bmp.Save($full, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "wrote $relPath" }

# ---------- shared palette ----------
$transparent  = C 0 0 0 0
$vinylBlack   = C 18 18 18
$vinylEdge    = C 40 40 40
$labelGrey    = C 205 205 205
$labelGreyDk  = C 135 135 135
$labelRed     = C 205 35 35
$labelRedDk   = C 135 20 20
$labelWhite   = C 240 240 240
$holeDark     = C 30 30 30

$body         = C 64 64 64
$bodyDark     = C 38 38 38
$bodyLight    = C 92 92 92
$discWell     = C 26 26 26
$discInner    = C 14 14 14
$spindle      = C 60 60 60
$ledRed       = C 220 35 35
$ledGlow      = C 175 30 30

$displayCyan    = C 80 200 220
$displayCyanDk  = C 35 100 120
$btnRed         = C 200 50 50
$btnRedHi       = C 240 100 100
$btnGreyLt      = C 195 195 195
$btnGreyMd      = C 130 130 130
$vent           = C 22 22 22
$ytPlay         = C 255 0 0

$speakerBody   = C 60 60 80
$speakerLight  = C 95 95 130
$speakerDark   = C 35 35 55
$grilleDark    = C 18 18 30
$grilleHole    = C 8 8 18

# blank_disk, recorded_disk, disk_recorder top/side/bottom (unchanged)
$bmp = New-Bitmap16
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) {
    $dx = $x - 7.5; $dy = $y - 7.5; $d = [Math]::Sqrt($dx*$dx + $dy*$dy)
    if     ($d -gt 7.7) { $c = $transparent }
    elseif ($d -gt 6.8) { $c = $vinylEdge }
    elseif ($d -gt 3.5) { $c = $vinylBlack }
    elseif ($d -gt 2.6) { $c = $labelGreyDk }
    elseif ($d -gt 1.2) { $c = $labelGrey }
    else                { $c = $holeDark }
    $bmp.SetPixel($x, $y, $c) } }
Save-Bmp $bmp 'item\blank_disk.png'

$bmp = New-Bitmap16
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) {
    $dx = $x - 7.5; $dy = $y - 7.5; $d = [Math]::Sqrt($dx*$dx + $dy*$dy)
    if     ($d -gt 7.7) { $c = $transparent }
    elseif ($d -gt 6.8) { $c = $vinylEdge }
    elseif ($d -gt 3.5) { $c = $vinylBlack }
    elseif ($d -gt 2.6) { $c = $labelRedDk }
    elseif ($d -gt 1.2) { $c = $labelRed }
    else                { $c = $labelWhite }
    $bmp.SetPixel($x, $y, $c) } }
Save-Bmp $bmp 'item\recorded_disk.png'

$bmp = New-Bitmap16
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) {
    $c = $body
    if ($x -eq 0 -or $x -eq 15 -or $y -eq 0 -or $y -eq 15) { $c = $bodyDark }
    if (($x -eq 1 -or $y -eq 1) -and ($x -ne 0 -and $y -ne 0 -and $x -ne 15 -and $y -ne 15)) { $c = $bodyLight }
    $dx = $x - 7.5; $dy = $y - 7.5; $d = [Math]::Sqrt($dx*$dx + $dy*$dy)
    if ($d -le 6.6) {
        $c = $discWell
        if ($d -le 5.6) { $c = $discInner }
        if ($d -le 3.5) { $c = $vinylBlack }
        if ($d -le 1.2) { $c = $spindle }
    }
    if ($x -eq 13 -and $y -eq 2) { $c = $ledRed }
    if ($x -eq 13 -and $y -eq 3) { $c = $ledGlow }
    if ($x -eq 12 -and $y -eq 2) { $c = $ledGlow }
    if ($x -eq 2  -and $y -eq 2 ) { $c = $bodyDark }
    if ($x -eq 13 -and $y -eq 13) { $c = $bodyDark }
    if ($x -eq 2  -and $y -eq 13) { $c = $bodyDark }
    $bmp.SetPixel($x, $y, $c) } }
Save-Bmp $bmp 'block\disk_recorder_top.png'

$bmp = New-Bitmap16
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) {
    $c = $body
    if ($x -eq 0 -or $x -eq 15 -or $y -eq 0 -or $y -eq 15) { $c = $bodyDark }
    if ($y -eq 1 -and $x -ge 1 -and $x -le 14) { $c = $bodyLight }
    if ($y -ge 3 -and $y -le 5) {
        if (($x -eq 2 -or $x -eq 13) -and $y -ge 3 -and $y -le 5) { $c = $bodyDark }
        if ($x -ge 3 -and $x -le 12) {
            if ($y -eq 3 -or $y -eq 5) { $c = $displayCyanDk } else { $c = $displayCyan }
        }
    }
    if ($y -ge 8 -and $y -le 9) {
        if ($x -ge 2 -and $x -le 3) { $c = $btnRed }
        if ($x -eq 2 -and $y -eq 8) { $c = $btnRedHi }
        if ($x -ge 5 -and $x -le 6) { $c = $btnGreyLt }
        if ($x -ge 8 -and $x -le 9) { $c = $btnGreyMd }
        if ($x -ge 11 -and $x -le 12) { $c = $btnGreyMd }
    }
    if (($y -eq 11 -or $y -eq 12) -and $x -ge 2 -and $x -le 13) {
        if ((($x - 2) % 2) -eq 0) { $c = $vent }
    }
    $bmp.SetPixel($x, $y, $c) } }
Save-Bmp $bmp 'block\disk_recorder_side.png'

$bmp = New-Bitmap16
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) {
    $c = $body
    if ($x -eq 0 -or $x -eq 15 -or $y -eq 0 -or $y -eq 15) { $c = $bodyDark }
    if ($x -eq 7 -or $x -eq 8 -or $y -eq 7 -or $y -eq 8) { $c = C 55 55 55 }
    if (($x -ge 2 -and $x -le 3 -and $y -ge 2 -and $y -le 3) -or
        ($x -ge 12 -and $x -le 13 -and $y -ge 2 -and $y -le 3) -or
        ($x -ge 2 -and $x -le 3 -and $y -ge 12 -and $y -le 13) -or
        ($x -ge 12 -and $x -le 13 -and $y -ge 12 -and $y -le 13)) { $c = C 18 18 18 }
    if ($x -eq 7 -and $y -eq 7) { $c = C 30 30 30 }
    $bmp.SetPixel($x, $y, $c) } }
Save-Bmp $bmp 'block\disk_recorder_bottom.png'

# ---------- Speaker: top with handle/vent, side with grille, bottom with feet ----------

# speaker_top: deep blue-grey panel with two vent slits + a center LED
$bmp = New-Bitmap16
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) {
    $c = $speakerBody
    if ($x -eq 0 -or $x -eq 15 -or $y -eq 0 -or $y -eq 15) { $c = $speakerDark }
    if (($x -eq 1 -or $y -eq 1) -and ($x -ne 0 -and $y -ne 0 -and $x -ne 15 -and $y -ne 15)) { $c = $speakerLight }
    # two horizontal vent slits at y=5 and y=10 (cols 3-12)
    if (($y -eq 5 -or $y -eq 10) -and $x -ge 3 -and $x -le 12) { $c = $vent }
    # center LED (REC indicator on top)
    if ($x -eq 7 -and $y -eq 7) { $c = $ledRed }
    if ($x -eq 8 -and $y -eq 7) { $c = $ledGlow }
    if ($x -eq 7 -and $y -eq 8) { $c = $ledGlow }
    $bmp.SetPixel($x, $y, $c) } }
Save-Bmp $bmp 'block\speaker_top.png'

# speaker_side: dark panel with a 6x6 grille of dots (like a speaker mesh)
$bmp = New-Bitmap16
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) {
    $c = $speakerBody
    if ($x -eq 0 -or $x -eq 15 -or $y -eq 0 -or $y -eq 15) { $c = $speakerDark }
    if ($y -eq 1 -and $x -ge 1 -and $x -le 14) { $c = $speakerLight }
    # grille frame
    if ($x -ge 2 -and $x -le 13 -and $y -ge 3 -and $y -le 14) {
        $c = $grilleDark
        # mesh dots: every 2 cols × 2 rows starting at (3,4)
        if ((($x - 3) % 2) -eq 0 -and (($y - 4) % 2) -eq 0 -and $x -ge 3 -and $x -le 12 -and $y -ge 4 -and $y -le 13) {
            $c = $grilleHole
        }
    }
    $bmp.SetPixel($x, $y, $c) } }
Save-Bmp $bmp 'block\speaker_side.png'

# speaker_bottom: plain darker panel with 4 rubber feet
$bmp = New-Bitmap16
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) {
    $c = $speakerDark
    if ($x -eq 7 -or $x -eq 8 -or $y -eq 7 -or $y -eq 8) { $c = C 30 30 50 }
    if (($x -ge 2 -and $x -le 3 -and $y -ge 2 -and $y -le 3) -or
        ($x -ge 12 -and $x -le 13 -and $y -ge 2 -and $y -le 3) -or
        ($x -ge 2 -and $x -le 3 -and $y -ge 12 -and $y -le 13) -or
        ($x -ge 12 -and $x -le 13 -and $y -ge 12 -and $y -le 13)) { $c = C 10 10 20 }
    $bmp.SetPixel($x, $y, $c) } }
Save-Bmp $bmp 'block\speaker_bottom.png'

Write-Host ""
Write-Host "All textures generated (discs + recorder + speaker)."
