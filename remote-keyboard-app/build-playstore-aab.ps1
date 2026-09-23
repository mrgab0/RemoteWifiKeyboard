# Script de generación de paquete de producción para Google Play Store (.aab y .apk firmado)
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Generando Paquete para Google Play Store (.aab / .apk)  " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$sdkDir = "$env:LOCALAPPDATA\Android\Sdk"
$gradleDir = "$PSScriptRoot\gradle-8.5"
$gradleBin = "$gradleDir\bin\gradle.bat"
$keystorePath = "$PSScriptRoot\release.keystore"

# 1. Crear release.keystore si no existe
if (-not (Test-Path $keystorePath)) {
    Write-Host "[1/3] Creando clave de firma digital (Keystore)..." -ForegroundColor Yellow
    $keytool = "keytool"
    & $keytool -genkey -v -keystore $keystorePath -alias remotekeyboard -keyalg RSA -keysize 2048 -validity 10000 -storepass remotekeyboard123 -keypass remotekeyboard123 -dname "CN=RemoteKeyboard, OU=Mobile, O=Gabo, L=Houston, ST=Texas, C=US"
}

# 2. Compilar Android App Bundle (.aab) y APK firmado
Write-Host "[2/3] Compilando Android App Bundle (.aab) para Play Store..." -ForegroundColor Green
$env:ANDROID_HOME = $sdkDir
$env:ANDROID_SDK_ROOT = $sdkDir
& $gradleBin bundleRelease assembleRelease

# 3. Copiar los archivos listos a la raíz
if ($LASTEXITCODE -eq 0) {
    $aabSource = "$PSScriptRoot\app\build\outputs\bundle\release\app-release.aab"
    $apkSource = "$PSScriptRoot\app\build\outputs\apk\release\app-release.apk"
    
    if (Test-Path $aabSource) {
        Copy-Item $aabSource "$PSScriptRoot\RemoteWiFiKeyboard.aab" -Force
        Write-Host "`n🎉 ¡Android App Bundle (.aab) generado con éxito!" -ForegroundColor Green
        Write-Host "👉 Archivo para Google Play Console: $PSScriptRoot\RemoteWiFiKeyboard.aab" -ForegroundColor White
    }
    
    if (Test-Path $apkSource) {
        Copy-Item $apkSource "$PSScriptRoot\RemoteWiFiKeyboard.apk" -Force
        Write-Host "`n🎉 ¡APK firmado generado con éxito!" -ForegroundColor Green
        Write-Host "👉 Archivo para instalación directa: $PSScriptRoot\RemoteWiFiKeyboard.apk" -ForegroundColor White
    }
} else {
    Write-Host "`n❌ Error en la compilación de producción. Revisa los logs." -ForegroundColor Red
}
