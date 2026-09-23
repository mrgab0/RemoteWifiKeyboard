@echo off
title Instalador - Remote WiFi Keyboard para Windows
color 0b
echo ========================================================
echo   Instalando Remote WiFi Keyboard para Windows 10/11
echo ========================================================
echo.

set "INSTALL_DIR=%LOCALAPPDATA%\RemoteWiFiKeyboard"
set "EXE_NAME=RemoteWiFiKeyboard.exe"

echo 1. Creando directorio de instalacion: %INSTALL_DIR%
if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"

echo 2. Copiando ejecutable nativo en Rust...
copy /Y "%~dp0%EXE_NAME%" "%INSTALL_DIR%\%EXE_NAME%" >nul

echo 3. Configurando auto-arranque con Windows...
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v "RemoteWiFiKeyboard" /t REG_SZ /d "\"%INSTALL_DIR%\%EXE_NAME%\"" /f >nul

echo 4. Creando acceso directo en el Menu Inicio...
powershell -Command "$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut(\"$([Environment]::GetFolderPath('Programs'))\Remote WiFi Keyboard.lnk\"); $s.TargetPath = \"%INSTALL_DIR%\%EXE_NAME%\"; $s.Save()"

echo.
echo ========================================================
echo   INSTALACION COMPLETADA CON EXITO!
echo ========================================================
echo   - La app se iniciara silenciosamente en la bandeja del sistema (System Tray).
echo   - Cero consola negra de comandos.
echo.
echo Iniciando aplicacion ahora...
start "" "%INSTALL_DIR%\%EXE_NAME%"

timeout /t 3 >nul
exit
