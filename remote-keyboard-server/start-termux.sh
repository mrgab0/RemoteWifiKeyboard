#!/data/data/com.termux/files/usr/bin/bash

echo "============================================="
echo "  Iniciando Remote Keyboard Node.js Server"
echo "============================================="

# Instalar Node.js y termux-api si no están instalados
if ! command -v node &> /dev/null; then
    echo "[1/3] Instalando Node.js en Termux..."
    pkg install nodejs -y
fi

if ! command -v termux-clipboard-set &> /dev/null; then
    echo "[2/3] Instalando Termux API..."
    pkg install termux-api -y
fi

# Instalar dependencias npm si falta node_modules
if [ ! -d "node_modules" ]; then
    echo "[3/3] Instalando dependencias de Node..."
    npm install
fi

# Iniciar servidor
node server.js
