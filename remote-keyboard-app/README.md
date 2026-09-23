# ⌨️ Remote WiFi Keyboard (Android App)

Una aplicación Android ligera y moderna para utilizar el teclado de tu computadora (Windows / Mac / Linux) como teclado físico en tu teléfono Android a través de tu red Wi-Fi local.

---

## 🚀 Características
* **Escritura en Tiempo Real:** Cada letra o tecla que presionas en tu PC se escribe al instante en tu celular con latencia mínima (< 5ms).
* **Compatibilidad Total:** Funciona en WhatsApp, Telegram, Termux, Navegador, Notas y cualquier aplicación de Android.
* **Soporte de Teclas de Control:** `Enter` nativo, `Backspace`, `Tab`, flechas direccionales (`Arriba`, `Abajo`, `Izquierda`, `Derecha`) y `Seleccionar Todo`.
* **Envío de Bloques de Texto:** Área para redactar o pegar párrafos largos, código o enlaces y enviarlos con `Ctrl + Enter`.
* **Sin Dependencias Pesadas:** Servidor web embebido ultraligero que corre directamente dentro del servicio de teclado.
* **Compilación sin Android Studio:** Compatible con Java 17 y Gradle por línea de comandos.

---

## 🛠️ Cómo Compilar el APK
Solo necesitas abrir PowerShell en la carpeta del proyecto y ejecutar:

```powershell
.\build-apk.ps1
```

Esto descargará automáticamente Gradle (si no lo tienes) y compilará el archivo instalable **`RemoteWiFiKeyboard.apk`**.

---

## 📱 Cómo Usar la Aplicación en tu Teléfono

1. **Instalar el APK:**
   * Transfiere e instala el archivo `RemoteWiFiKeyboard.apk` en tu Android (o mediante `adb install RemoteWiFiKeyboard.apk`).
2. **Habilitar el Teclado:**
   * Abre la app **Remote WiFi Keyboard** en tu teléfono.
   * Toca **1. Habilitar en Ajustes** y activa el interruptor de *Remote WiFi Keyboard*.
   * Toca **2. Seleccionar como Teclado Activo**.
3. **Conectar desde la PC:**
   * La app te mostrará una dirección como `http://192.168.1.105:8080`.
   * Abre esa URL en cualquier navegador en tu PC (Chrome, Edge, Firefox).
   * ¡Listo! Empieza a escribir desde tu teclado físico.
