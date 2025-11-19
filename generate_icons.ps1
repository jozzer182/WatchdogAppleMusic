# Script para generar iconos de Android en todas las resoluciones necesarias
# Requiere el archivo icon_watchdog.png en la raíz del proyecto

Add-Type -AssemblyName System.Drawing

$sourceImage = "icon_watchdog.png"
$baseDir = "app\src\main\res"

# Definir las resoluciones necesarias para Android
$resolutions = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

# Función para redimensionar imagen
function Resize-Image {
    param(
        [string]$InputPath,
        [string]$OutputPath,
        [int]$Width,
        [int]$Height
    )
    
    $image = [System.Drawing.Image]::FromFile($InputPath)
    $bitmap = New-Object System.Drawing.Bitmap($Width, $Height)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    
    # Configurar alta calidad de redimensionamiento
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    
    # Dibujar la imagen redimensionada
    $graphics.DrawImage($image, 0, 0, $Width, $Height)
    
    # Guardar
    $bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    
    # Limpiar recursos
    $graphics.Dispose()
    $bitmap.Dispose()
    $image.Dispose()
}

Write-Host "🎨 Generando iconos de Android..." -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path $sourceImage)) {
    Write-Host "❌ Error: No se encontró $sourceImage" -ForegroundColor Red
    exit 1
}

foreach ($resolution in $resolutions.GetEnumerator()) {
    $folder = Join-Path $baseDir $resolution.Key
    $size = $resolution.Value
    
    # Crear carpeta si no existe
    if (-not (Test-Path $folder)) {
        New-Item -ItemType Directory -Path $folder -Force | Out-Null
    }
    
    # Generar ic_launcher.png
    $outputPath = Join-Path $folder "ic_launcher.png"
    Write-Host "  📱 Generando $($resolution.Key)/ic_launcher.png (${size}x${size}px)..." -ForegroundColor Green
    Resize-Image -InputPath $sourceImage -OutputPath $outputPath -Width $size -Height $size
    
    # Copiar también como ic_launcher_round.png
    $roundPath = Join-Path $folder "ic_launcher_round.png"
    Copy-Item -Path $outputPath -Destination $roundPath -Force
}

Write-Host ""
Write-Host "✅ Iconos generados exitosamente en todas las resoluciones!" -ForegroundColor Green
Write-Host "📁 Ubicación: $baseDir\mipmap-*\ic_launcher.png" -ForegroundColor Yellow
