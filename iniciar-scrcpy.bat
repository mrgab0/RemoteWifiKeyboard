@echo off
chcp 65001 >nul
title Scrcpy 4.1 Inalámbrico (GTX 1060)
cls
echo ======================================================================
echo       📱 SCRCPY 4.1 INALÁMBRICO (Acelerado por GTX 1060 NVDEC)
echo ======================================================================
echo.
echo  [1] Activar WiFi por Cable USB (Primera vez: conecta cable, pulsa 1)
echo  [2] Conectar por IP WiFi (Sin cable: si ya conoces la IP de tu móvil)
echo  [3] Instalar RemoteWiFiKeyboard.apk directamente al teléfono
echo  [4] Salir
echo.
set /p opt="Selecciona una opción [1-4]: "

if "%opt%"=="1" (
    echo.
    echo 🔌 Asegúrate de tener el cable USB conectado con Depuración USB activa...
    adb tcpip 5555
    echo 📶 Modo inalámbrico activado en el móvil. Ya puedes desconectar el cable.
    echo 🚀 Abriendo pantalla del móvil...
    scrcpy --tcpip
    goto end
)

if "%opt%"=="2" (
    echo.
    set /p ip="👉 Introduce la IP de tu móvil (la que ves en la app, ej: 192.168.1.15): "
    echo 🔗 Conectando con %ip%:5555...
    adb connect %ip%:5555
    echo 🚀 Abriendo pantalla del móvil...
    scrcpy -s %ip%:5555
    goto end
)

if "%opt%"=="3" (
    echo.
    echo 📦 Instalando RemoteWiFiKeyboard.apk en el dispositivo...
    adb install -r "%~dp0remote-keyboard-app\RemoteWiFiKeyboard.apk"
    echo.
    echo ✅ ¡APK instalado exitosamente!
    pause
    goto end
)

:end
