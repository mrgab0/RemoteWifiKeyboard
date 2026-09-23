# Script de compilación automática autónoma para Remote WiFi Keyboard
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  Compilando Remote WiFi Keyboard APK" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

$sdkDir = "$env:LOCALAPPDATA\Android\Sdk"
$cmdlineZip = "$PSScriptRoot\cmdline-tools.zip"
$cmdlineDir = "$sdkDir\cmdline-tools\latest"

# 1. Configurar Android SDK si no existe
if (-not (Test-Path "$sdkDir\platforms\android-34")) {
    Write-Host "[1/4] Preparando Android SDK en $sdkDir..." -ForegroundColor Yellow
    
    if (-not (Test-Path "$cmdlineDir\bin\sdkmanager.bat")) {
        New-Item -ItemType Directory -Path "$sdkDir\cmdline-tools" -Force | Out-Null
        $cmdlineUrl = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
        Write-Host "Descargando Android Command Line Tools..." -ForegroundColor Yellow
        Invoke-WebRequest -Uri $cmdlineUrl -OutFile $cmdlineZip -UseBasicParsing
        
        Expand-Archive -Path $cmdlineZip -DestinationPath "$sdkDir\cmdline-tools" -Force
        Remove-Item $cmdlineZip -Force -ErrorAction SilentlyContinue
        
        if (Test-Path "$sdkDir\cmdline-tools\cmdline-tools") {
            Move-Item "$sdkDir\cmdline-tools\cmdline-tools" $cmdlineDir -Force
        }
    }
    
    Write-Host "Instalando Platform 34 y Build Tools..." -ForegroundColor Yellow
    $env:ANDROID_HOME = $sdkDir
    $env:ANDROID_SDK_ROOT = $sdkDir
    
    $sdkManager = "$cmdlineDir\bin\sdkmanager.bat"
    $acceptLicenses = "y`ny`ny`ny`ny`ny`n"
    $acceptLicenses | & $sdkManager --sdk_root=$sdkDir --licenses | Out-Null
    & $sdkManager --sdk_root=$sdkDir "platforms;android-34" "build-tools;34.0.0" "platform-tools"
}

# 2. Configurar local.properties
$escapedSdkDir = $sdkDir -replace '\\', '\\'
Set-Content -Path "$PSScriptRoot\local.properties" -Value "sdk.dir=$escapedSdkDir"

# 3. Descargar Gradle si no existe
$gradleVersion = "8.5"
$gradleZip = "$PSScriptRoot\gradle-$gradleVersion-bin.zip"
$gradleDir = "$PSScriptRoot\gradle-$gradleVersion"

if (-not (Test-Path "$gradleDir\bin\gradle.bat")) {
    Write-Host "[2/4] Descargando Gradle $gradleVersion..." -ForegroundColor Yellow
    $url = "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"
    Invoke-WebRequest -Uri $url -OutFile $gradleZip -UseBasicParsing
    
    Write-Host "Extrayendo Gradle..." -ForegroundColor Yellow
    Expand-Archive -Path $gradleZip -DestinationPath $PSScriptRoot -Force
    Remove-Item $gradleZip -Force -ErrorAction SilentlyContinue
}

# 4. Compilar APK
Write-Host "[3/4] Compilando APK con Gradle..." -ForegroundColor Green
$gradleBin = "$gradleDir\bin\gradle.bat"
$env:ANDROID_HOME = $sdkDir
$env:ANDROID_SDK_ROOT = $sdkDir
& $gradleBin assembleDebug

if ($LASTEXITCODE -eq 0) {
    $apkPath = "$PSScriptRoot\app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        Write-Host "`n✅ ¡APK compilado con éxito!" -ForegroundColor Green
        Copy-Item $apkPath "$PSScriptRoot\RemoteWiFiKeyboard.apk" -Force
        Write-Host "Copia lista para instalar en: $PSScriptRoot\RemoteWiFiKeyboard.apk" -ForegroundColor Cyan
    }
} else {
    Write-Host "`n❌ Error en la compilación. Revisa los logs." -ForegroundColor Red
}
