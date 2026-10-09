# 📜 MANIFIESTO DEL TECLADO PARA PROGRAMADORES (Remote WiFi Keyboard)

> **ESTE DOCUMENTO ES LEY INMUTABLE DEL PROYECTO.**
> Ningún desarrollador ni agente de IA debe eliminar, degradar o sobreescribir los principios y mecanismos aquí consagrados. Cada modificación futura debe someterse a este manifiesto como contrato arquitectónico.

---

## 1. Misión Fundamental
Remote WiFi Keyboard no es un simple teclado virtual de chat: **es una interfaz de control remoto de alto rendimiento diseñada para programadores, administradores de sistemas y power-users**. Permite controlar dispositivos móviles (Android) desde una PC física con la misma velocidad, precisión de combinaciones de teclas y fidelidad que un teclado mecánico conectado directamente por hardware.

---

## 2. Las 5 Leyes Inmutables de la Arquitectura

### ⚖️ LEY 1: La Conexión y el Auto-Descubrimiento son Sagrados
1. **Canal UDP de Descubrimiento (Puerto 9998)**:
   - El broadcaster Android emite beacons periódicos en el puerto 9998 UDP.
   - El cliente Electron escucha en el puerto 9998 UDP para autoconectar sin requerir que el usuario digite la IP manualmente.
   - **PROHIBIDO**: Eliminar o bloquear los sockets UDP de broadcast o escucha.
2. **Canal Primario de Tiempo Real (WebSocket - Puerto 9999 /ws)**:
   - Toda la interacción interactiva de pulsaciones, atajos y ping de telemetría debe circular por el WebSocket abierto.
   - El canal HTTP REST (`/api/type`, `/api/key_event`) es **exclusivamente un mecanismo de respaldo (fallback)** en caso de reconexión.
3. **Resiliencia de Red**:
   - La reconexión automática con retroceso exponencial (*exponential backoff*) debe permanecer activa ante cualquier interrupción de la red local.

---

### ⚖️ LEY 2: El Pipeline de Teclas es Puro, Atómico y Libre de Expresiones Regulares Ciegas
1. **No a los Scripts Regex Destructivos**:
   - Está terminantemente prohibido ejecutar scripts de reemplazo masivo de expresiones regulares sobre archivos fuente principales (`index.html`, `RemoteInputMethodService.java`). Toda modificación debe ser modular, atómica y validada línea por línea.
2. **Separación de Responsabilidades**:
   - La captura física en el DOM (`handleKey`), la serialización del payload JSON y el despacho de red no deben mezclarse con la lógica de renderizado visual.
   - Los logs de telemetría y el haptic feedback jamás deben retrasar el despacho de la tecla.

---

### ⚖️ LEY 3: Principio de Cero Regresión en Atajos (Zero-Regression)
1. **Inviolabilidad de Atajos Esenciales**:
   - Los atajos universales de edición (`Ctrl+A`, `Ctrl+C`, `Ctrl+V`, `Ctrl+X`, `Ctrl+Z`, `Ctrl+Y`, `Ctrl+S`, `Ctrl+F`) y de navegación (`Home`, `End`, `PageUp`, `PageDown`, flechas direccionales, `Tab`, `Escape`) **DEBEN FUNCIONAR SIEMPRE**.
2. **Despacho Híbrido Android**:
   - En campos de texto estándar de Android (`EditText`, `WebView`, navegadores), atajos como `Ctrl+A`, `Ctrl+C`, `Ctrl+V` se garantizan mediante `ic.performContextMenuAction` para no depender de que la vista interprete teclas físicas.
   - En emuladores de terminal (`Termux`) y editores de código (`Acode`), se garantiza el despacho del `KeyEvent` físico con dispositivo de hardware real (dispositivo 0, sin `FLAG_SOFT_KEYBOARD`).
3. **Borrado Seguro (`Backspace` / `Delete`)**:
   - Si existe texto seleccionado, `Backspace` y `Delete` deben borrar la selección completa de inmediato.
   - Si no hay selección, `Backspace` borra con `deleteSurroundingText(1, 0)` y `Delete` con `deleteSurroundingText(0, 1)`.
   - Las combinaciones con modificadores (`Ctrl+Backspace` para borrar palabra previa) deben preservarse.

---

### ⚖️ LEY 4: Presupuesto Estricto de Latencia (Objetivo < 5ms en Wi-Fi Local)
1. **Garantía de Radio Wi-Fi Despierto**:
   - El servicio de teclado en Android DEBE sostener un `WifiLock` en modo `WIFI_MODE_FULL_HIGH_PERF` (o `WIFI_MODE_FULL_LOW_LATENCY` en Android 10+) durante la sesión activa.
   - Sin este bloqueo, el ahorro de energía del chip Wi-Fi (802.11 PSM) retrasa los paquetes hasta la baliza DTIM del router (~100 ms).
2. **No Bloquear el MainLooper**:
   - No se permite doble encolado (`mainHandler.post` dentro de otro `post`).
   - `TCP_NODELAY` debe permanecer activo en los sockets de servidor y cliente para deshabilitar el algoritmo de Nagle.

---

### ⚖️ LEY 5: Arquitectura Dual de Perfiles
El sistema debe proveer dos modos de operación claramente diferenciados:

1. **⚡ Modo Código (Programmer / Turbo)**:
   - **Propósito**: Desarrollo en Termux, SSH, Acode, Vim, Nano, VS Code.
   - **Comportamiento**: Transmisión inmediata y cruda de eventos `keydown` y `keyup` para todas las teclas físicas.
   - **Modificadores**: Soporte íntegro de `Ctrl`, `Alt`, `Shift`, `Meta` (Win/Cmd), teclas `F1` a `F12`, `Esc`, `Tab`.
   - **Cero Búfer**: Sin retrasos ni composiciones intermedias.

2. **📝 Modo Diario (Normal / Fluido)**:
   - **Propósito**: Mensajería rápida (WhatsApp, Telegram), notas y navegación web.
   - **Comportamiento**: Inserción fluida mediante `ic.commitText` para caracteres imprimibles, con soporte completo para tildes y diacríticos (`á`, `é`, `í`, `ó`, `ú`, `ñ`).
   - **Atajos**: Mapeo directo de `Ctrl+A/C/V/Z` a las acciones nativas del sistema operativo.

---

## 3. Matriz de Puertos y Protocolos

| Componente | Protocolo | Puerto | Rol |
|---|---|---|---|
| **Auto-Discovery** | UDP Broadcast | `9998` | Teléfono transmite beacon JSON; PC autoconecta |
| **Keystroke Streaming** | WebSocket | `9999` (`/ws`) | Stream bidireccional < 5ms de teclas, portapapeles y pings |
| **HTTP Fallback** | HTTP REST | `9999` (`/api/*`) | Diagnóstico (`/api/diag`), Ping (`/api/ping`) y respaldo |

---

## 4. Lista de Comprobación para Nuevas Modificaciones (Checklist Pre-Merge)
Antes de dar por buena cualquier modificación al código:
- [ ] ¿El auto-descubrimiento UDP conecta automáticamente sin escribir la IP?
- [ ] ¿Presionar `Ctrl + A` en el teclado físico de la PC selecciona todo el texto en el teléfono?
- [ ] ¿Presionar `Backspace` borra caracteres tanto en WhatsApp como en una consola Termux?
- [ ] ¿La latencia promedio del ping en la interfaz de telemetría permanece por debajo de 15ms?
- [ ] ¿La sincronización de portapapeles bidireccional continúa funcionando sin bucles infinitos?
- [ ] ¿Se puede alternar limpiamente entre Modo Código y Modo Diario sin reiniciar la app?
