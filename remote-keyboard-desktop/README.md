# ⌨️ Remote WiFi Keyboard Desktop (Windows 10 & 11)
### Aplicación oficial para Microsoft Store y Windows Desktop

Aplicación de escritorio para Windows que conecta tu teclado físico a cualquier teléfono Android en tu red Wi-Fi local, con detección automática y soporte para System Tray.

---

## 🌟 Características
* **Auto-Descubrimiento Wi-Fi (Zero-Config):** Escanea tu subred local y se conecta automáticamente al teléfono en cuanto abres la app de Android.
* **Escritura en Tiempo Real:** Envía cada tecla (Enter, Backspace, Tab, Flechas) instantáneamente con latencia < 5ms.
* **Integración con Windows:**
  * Se minimiza a la **Bandeja del Sistema (System Tray)** junto al reloj de Windows.
  * **Atajo Global de Teclado:** Presiona `Ctrl + Alt + K` para mostrar/ocultar el teclado flotante desde cualquier juego o programa.
* **Envío de Párrafos / Código:** Envía bloques completos de texto con `Ctrl + Enter`.
* **Diseño Windows 11 Fluent UI:** Interfaz moderna con efecto Mica/Acrílico y tema oscuro.

---

## 🚀 Cómo Ejecutar en Desarrollo

1. Abre PowerShell en esta carpeta:
   ```powershell
   cd C:\Users\gabo\Documents\codigo\codespaces\remote-keyboard-desktop
   ```
2. Instala las dependencias y ejecuta:
   ```powershell
   npm install
   npm start
   ```

---

## 📦 Compilación y Empaquetado para Microsoft Store

### 1. Generar Paquete `.msix` para Microsoft Store:
```powershell
npm run build:msix
```
*El paquete `.msix` generado en la carpeta `dist/` se sube directamente a tu cuenta de **Microsoft Partner Center** para su publicación oficial.*

### 2. Generar Instalador `.exe` (NSIS):
```powershell
npm run build:win
```

### 3. Generar Versión Portable `.exe` (sin instalación):
```powershell
npm run build:portable
```
