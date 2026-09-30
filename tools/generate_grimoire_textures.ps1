Add-Type -AssemblyName System.Drawing

$project = Split-Path -Parent $PSScriptRoot
$textures = Join-Path $project 'src/main/resources/assets/manatech/textures/item'

function Color([int]$red, [int]$green, [int]$blue, [int]$alpha = 255) {
    return [System.Drawing.Color]::FromArgb($alpha, $red, $green, $blue)
}

function Fill-Region($bitmap, [int]$left, [int]$top, [int]$right, [int]$bottom, $color) {
    for ($y = $top; $y -lt $bottom; $y++) {
        for ($x = $left; $x -lt $right; $x++) {
            $bitmap.SetPixel($x, $y, $color)
        }
    }
}

$atlas = [System.Drawing.Bitmap]::new(64, 64)
try {
    Fill-Region $atlas 0 0 64 64 (Color 34 25 49)
    Fill-Region $atlas 0 0 48 17 (Color 48 35 76)
    for ($y = 0; $y -lt 17; $y++) {
        for ($x = 0; $x -lt 48; $x++) {
            if ((($x * 17 + $y * 29) % 13) -eq 0) { $atlas.SetPixel($x, $y, (Color 59 45 91)) }
        }
    }
    Fill-Region $atlas 0 18 47 35 (Color 216 208 186)
    for ($y = 20; $y -lt 34; $y += 3) {
        Fill-Region $atlas 1 $y 46 ($y + 1) (Color 176 168 151)
    }
    Fill-Region $atlas 0 36 31 54 (Color 54 37 76)
    Fill-Region $atlas 3 36 5 54 (Color 174 136 76)
    Fill-Region $atlas 9 36 11 54 (Color 174 136 76)
    Fill-Region $atlas 32 36 64 54 (Color 191 151 83)
    Fill-Region $atlas 32 36 64 38 (Color 239 204 130)
    Fill-Region $atlas 32 55 64 64 (Color 125 108 226)
    Fill-Region $atlas 32 55 64 57 (Color 196 183 255)
    $atlas.Save((Join-Path $textures 'rune_grimoire.png'), [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $atlas.Dispose()
}

$icon = [System.Drawing.Bitmap]::new(16, 16)
try {
    Fill-Region $icon 0 0 16 16 (Color 0 0 0 0)
    Fill-Region $icon 2 1 14 15 (Color 184 149 89)
    Fill-Region $icon 3 2 13 14 (Color 47 34 74)
    Fill-Region $icon 3 2 5 14 (Color 127 91 148)
    Fill-Region $icon 5 3 12 13 (Color 58 42 87)
    Fill-Region $icon 7 5 9 11 (Color 191 170 239)
    Fill-Region $icon 5 7 11 9 (Color 191 170 239)
    Fill-Region $icon 12 6 15 10 (Color 217 180 103)
    Fill-Region $icon 13 7 15 9 (Color 155 139 241)
    $icon.Save((Join-Path $textures 'rune_grimoire_icon.png'), [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $icon.Dispose()
}
