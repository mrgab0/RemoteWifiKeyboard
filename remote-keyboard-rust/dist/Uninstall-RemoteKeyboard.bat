@echo off
title Desinstalador - Remote WiFi Keyboard para Windows
color 0c
echo ========================================================
echo   Desinstalando Remote WiFi Keyboard de Windows
echo ========================================================
echo.

set "INSTALL_DIR=%LOCALAPPDATA%\RemoteWiFiKeyboard"
set "EXE_NAME=RemoteWiFiKeyboard.exe"

echo 1. Cerrando procesos activos...
taskkill /F /IM "%EXE_NAME%" /T >nul 2>&1

echo 2. Eliminando del auto-arranque de Windows...
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v "RemoteWiFiKeyboard" /f >nul 2>&1

echo 3. Eliminando acceso directo del Menu Inicio...
powershell -Command "Remove-Item \"$([Environment]::GetFolderPath('Programs'))\Remote WiFi Keyboard.lnk\" -ErrorAction SilentlyContinue"

echo 4. Eliminando archivos del sistema...
if exist "%INSTALL_DIR%" rmdir /S /Q "%INSTALL_DIR%" >nul 2>&1

echo.
echo ========================================================
echo   DESINSTALACION COMPLETADA
echo ========================================================
timeout /t 3 >nul
exit
