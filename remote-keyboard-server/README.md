# ⚡ Remote Keyboard Server (Node.js + WebSockets)

Servidor de teclado remoto de ultra-baja latencia basado en **Express y WebSockets (`ws`)** para ejecutar directamente en **Termux (Android)** o en tu PC.

---

## 🚀 Cómo Usarlo en Termux (Android)

1. En la app Termux de tu teléfono:
   ```bash
   cd remote-keyboard-server
   chmod +x start-termux.sh
   ./start-termux.sh
   ```
2. La terminal te mostrará tu IP local (por ejemplo `http://192.168.1.105:8080`).
3. Abre esa dirección en el navegador de tu PC (Chrome, Edge, Firefox).
4. ¡Listo! Todo lo que escribas se transmitirá por WebSockets con latencia `< 1ms`.

---

## 💻 Cómo Usarlo en tu PC (Windows / Mac / Linux)

1. Abre la terminal en esta carpeta:
   ```powershell
   cd C:\Users\gabo\Documents\codigo\codespaces\remote-keyboard-server
   npm install
   npm start
   ```
2. Abre `http://localhost:8080` en tu navegador.
