@echo off
setlocal enabledelayedexpansion
title Scrcpy 4.1 Inalambrico (GTX 1060)

:: Auto-detectar directorio de Scrcpy instalado por WinGet
set "SCRCPY_DIR=%LOCALAPPDATA%\Microsoft\WinGet\Packages\Genymobile.scrcpy_Microsoft.Winget.Source_8wekyb3d8bbwe\scrcpy-win64-v4.1"
if exist "%SCRCPY_DIR%" (
    set "PATH=%SCRCPY_DIR%;%PATH%"
)

cls
echo ======================================================================
echo       SCRCPY 4.1 INALAMBRICO (Aceleracion por GTX 1060 NVDEC)
echo ======================================================================
echo.
echo  [1] Activar WiFi por Cable USB (Recomendado 1a vez: cable 5 seg)
echo  [2] Conectar por IP WiFi (Puerto 5555 ya activo)
echo  [3] Vincular por Codigo de Android 11+ (Sin cable)
echo  [4] Instalar RemoteWiFiKeyboard.apk directamente al telefono
echo  [5] Salir
echo.
set /p opt="Selecciona una opcion [1-5]: "

if "%opt%"=="1" (
    echo.
    echo 1. Conecta el cable USB al telefono con Depuracion USB activada.
    echo 2. Si el telefono muestra un aviso de "Permitir depuracion USB", dale ACEPTAR.
    echo.
    pause
    echo Activando puerto WiFi 5555 en el telefono...
    adb tcpip 5555
    if errorlevel 1 (
        echo.
        echo ERROR: No se detecto el telefono por USB.
        echo Asegurate de habilitar "Depuracion USB" en Opciones de Desarrollador.
        pause
        goto end
    )
    echo.
    echo Exito! Modo inalambrico activado. Ya puedes desconectar el cable USB.
    echo Abriendo Scrcpy...
    scrcpy --tcpip
    pause
    goto end
)

if "%opt%"=="2" (
    echo.
    set /p ip="Introduce la IP de tu telefono (ej: 192.168.1.101): "
    echo Conectando a !ip!:5555...
    adb connect !ip!:5555
    echo Abriendo Scrcpy...
    scrcpy -s !ip!:5555
    pause
    goto end
)

if "%opt%"=="3" (
    echo.
    echo En tu movil: Ajustes - Opciones de desarrollador - Depuracion inalambrica.
    echo Toca en "Vincular dispositivo con codigo".
    echo.
    set /p pair_ip="Introduce IP y PUERTO de vinculacion (ej: 192.168.1.101:38921): "
    set /p pair_code="Introduce el CODIGO de 6 digitos: "
    echo Vinculando...
    adb pair !pair_ip! !pair_code!
    echo.
    set /p conn_ip="Introduce IP y PUERTO principal de Depuracion Inalambrica: "
    echo Conectando...
    adb connect !conn_ip!
    echo Abriendo Scrcpy...
    scrcpy -s !conn_ip!
    pause
    goto end
)

if "%opt%"=="4" (
    echo.
    echo Verificando dispositivos conectados...
    adb devices
    echo.
    echo Instalando RemoteWiFiKeyboard.apk...
    adb install -r "%~dp0remote-keyboard-app\RemoteWiFiKeyboard.apk"
    echo.
    pause
    goto end
)

:end