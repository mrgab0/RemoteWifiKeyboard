# ⌨️ Remote WiFi Keyboard

> Convierte tu teléfono o tablet Android en un teclado y centro de control inalámbrico de ultra-baja latencia (< 0.1ms) para tu PC con Windows.

[![Windows](https://img.shields.io/badge/Windows-10%20%7C%2011-0078D6?logo=windows&logoColor=white)](https://github.com/mrgab0/RemoteWifiKeyboard)
[![Rust](https://img.shields.io/badge/Rust-Axum%20%7C%20Tokio%20%7C%20Win32-DEA584?logo=rust&logoColor=black)](https://github.com/mrgab0/RemoteWifiKeyboard)
[![Android](https://img.shields.io/badge/Android-10%20to%2016-3DDC84?logo=android&logoColor=white)](https://github.com/mrgab0/RemoteWifiKeyboard)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 🌟 Características Principales

- ⚡ **Latencia Ultra-Baja (< 0.1ms):** Conexión directa TCP WebSocket y HTTP a través de tu red WiFi local.
- 🦀 **Servidor Nativo en Rust para Windows:**
  - Reside silenciosamente en la **Bandeja del Sistema (System Tray)** junto al reloj.
  - Cero consola negra de comandos (`#![windows_subsystem = "windows"]`).
  - Inyección real de pulsaciones y atajos en Windows mediante la API Win32 `SendInput` (Unicode, tildes, `Ctrl+C`, `Ctrl+V`, `Win+D`, volumen, etc.).
  - Panel gráfico de configuración moderno (Fluent Dark) con código QR generado en tiempo real para emparejamiento instantáneo.
- 📱 **Aplicación Móvil en Android:**
  - Compatible con Android 10, 11, 12, 13, 14, 15 y 16.
  - Servicio en primer plano (*Foreground Service* con `type="connectedDevice"`) para mantener la conexión activa aun con la pantalla bloqueada.
  - Integración de anuncios AdMob y versión PRO ($7.99 USD) vía Google Play Billing.

---

## 📂 Estructura del Ecosistema

```
RemoteWifiKeyboard/
├── 📱 remote-keyboard-app/       # Aplicación Android (React Native, AdMob, Billing, Foreground Service)
│   ├── android/                  # Proyecto nativo Gradle de Android
│   │   └── app/build/outputs/    # APK y AAB listos para producción
│   └── src/                      # Código fuente React Native y UI
│
├── 💻 remote-keyboard-rust/      # Servidor de producción en Rust para Windows
│   ├── src/                      # Código fuente en Rust (Axum, Tokio, Win32 SendInput, Tray)
│   │   ├── main.rs               # Bucle de eventos Windows, Tray y Servidor Web
│   │   ├── keyboard.rs           # Inyección de teclas Unicode y atajos Win32
│   │   └── web/                  # Panel de control GUI (HTML5/JS Fluent Dark)
│   ├── dist/                     # Ejecutables e Instaladores listos:
│   │   ├── RemoteWiFiKeyboard-Setup.exe   # Instalador Clásico con Wizard Gráfico
│   │   ├── RemoteWiFiKeyboard.msix        # Paquete Moderno MSIX para Microsoft Store
│   │   └── RemoteWiFiKeyboard.exe         # Binario portable de ejecución directa
│   └── msix-package/             # Manifiesto y assets para Microsoft Partner Center
│
├── 🖥️ remote-keyboard-desktop/   # Alternativa de cliente de escritorio en Electron
└── ⚡ remote-keyboard-server/    # Servidor backend alternativo en Node.js
```

---

## 🚀 Instalación y Uso Rápido

### 1. En tu PC con Windows
1. Descarga y ejecuta [`RemoteWiFiKeyboard-Setup.exe`](remote-keyboard-rust/dist/RemoteWiFiKeyboard-Setup.exe) (o corre el binario portable [`RemoteWiFiKeyboard.exe`](remote-keyboard-rust/dist/RemoteWiFiKeyboard.exe)).
2. El servidor se iniciará automáticamente en la bandeja del sistema y abrirá el panel de control mostrando tu IP local y el código QR de emparejamiento.

### 2. En tu Teléfono Android
1. Instala [`RemoteWiFiKeyboard.apk`](remote-keyboard-app/android/app/build/outputs/apk/release/RemoteWiFiKeyboard.apk) o sube el paquete [`RemoteWiFiKeyboard.aab`](remote-keyboard-app/android/app/build/outputs/bundle/release/RemoteWiFiKeyboard.aab) a Google Play Console.
2. Abre la aplicación: detectará tu PC automáticamente en la red WiFi o apunta tu cámara al código QR en la pantalla.
3. ¡Comienza a escribir o controlar tu PC desde el móvil con cero retraso!

---

## 🛠️ Compilación desde el Código Fuente

### Compilar el Servidor de Windows en Rust
```bash
cd remote-keyboard-rust
cargo build --release
```
El ejecutable optimizado se generará en `target/release/remote-keyboard-rust.exe`.

### Compilar la App de Android
```bash
cd remote-keyboard-app/android
./gradlew assembleRelease   # Genera el APK
./gradlew bundleRelease     # Genera el AAB para Google Play
```

---

## 📄 Licencia
Este proyecto está bajo la Licencia MIT.
