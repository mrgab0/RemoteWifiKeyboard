# 🦀 Remote Keyboard (Servidor Nativo en Rust)
### Servidor ultraligero de alta velocidad para Windows 10 & 11

Servidor independiente escrito en **Rust (Tokio + Axum 0.7)** que procesa y transmite eventos de teclado y WebSockets con latencia sub-milisegundo (< 0.1ms) y consumo mínimo de memoria (< 5 MB RAM).

---

## 🚀 Características
* **Latencia Ultra-Baja (< 0.1ms):** Motor asíncrono con Tokio.
* **Consumo de Memoria:** Menos de 5 MB de RAM.
* **Binario Único:** Todo el frontend web (HTML/JS/CSS) está incrustado directamente en el binario compilado mediante `include_str!`.
* **Compatible con Microsoft Store:** Compila como ejecutable Win32 nativo y puede empaquetarse en `.msix`.
* **Detección Automática de IP:** Identifica tu IP local Wi-Fi al arrancar.

---

## 🛠️ Cómo Compilar y Ejecutar

### 1. Ejecutar en desarrollo:
```powershell
cd remote-keyboard-rust
cargo run
```

### 2. Compilar el ejecutable optimizado (`.exe` de ~3 MB):
```powershell
cargo build --release
```
*El binario final se generará en `target/release/remote-keyboard-rust.exe`.*
