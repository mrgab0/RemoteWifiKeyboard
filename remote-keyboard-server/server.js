const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const path = require('path');
const os = require('os');
const { exec, spawn } = require('child_process');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

const PORT = process.env.PORT || 8080;

app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Función para interactuar con Termux API si está disponible
function sendToTermuxClipboard(text) {
    const child = spawn('termux-clipboard-set');
    child.stdin.write(text);
    child.stdin.end();
}

function triggerTermuxToast(msg) {
    exec(`termux-toast "${msg}"`, (err) => {});
}

function triggerTermuxVibrate() {
    exec('termux-vibrate -d 30', (err) => {});
}

// REST Endpoints
app.get('/api/ping', (req, res) => {
    res.json({ status: 'ok', server: 'node-websocket', time: Date.now() });
});

app.post('/api/type', (req, res) => {
    const { text } = req.body;
    if (text) {
        console.log(`[TYPE]: ${text}`);
        sendToTermuxClipboard(text);
        // Notificar a todos los clientes WebSocket conectados
        broadcast({ type: 'type', text });
    }
    res.json({ status: 'ok' });
});

app.post('/api/key', (req, res) => {
    const { key } = req.body;
    if (key) {
        console.log(`[KEY]: ${key}`);
        triggerTermuxVibrate();
        broadcast({ type: 'key', key });
    }
    res.json({ status: 'ok' });
});

// WebSocket Server para latencia ultra-baja (< 1ms)
function broadcast(data) {
    const message = JSON.stringify(data);
    wss.clients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(message);
        }
    });
}

wss.on('connection', (ws, req) => {
    const clientIp = req.socket.remoteAddress;
    console.log(`[WS CONNECTED] Cliente conectado desde: ${clientIp}`);

    ws.send(JSON.stringify({ type: 'welcome', message: 'Conectado al servidor Node.js de Remote Keyboard' }));

    ws.on('message', (message) => {
        try {
            const data = JSON.parse(message);
            if (data.type === 'type' && data.text) {
                console.log(`[WS TYPE]: ${data.text}`);
                sendToTermuxClipboard(data.text);
            } else if (data.type === 'key' && data.key) {
                console.log(`[WS KEY]: ${data.key}`);
                triggerTermuxVibrate();
            }
        } catch (e) {
            console.error('Error parseando mensaje WebSocket:', e);
        }
    });

    ws.on('close', () => {
        console.log('[WS DISCONNECTED] Cliente desconectado');
    });
});

// Obtener la IP local Wi-Fi
function getLocalIp() {
    const interfaces = os.networkInterfaces();
    for (const name of Object.keys(interfaces)) {
        for (const net of interfaces[name]) {
            if (net.family === 'IPv4' && !net.internal) {
                return net.address;
            }
        }
    }
    return '127.0.0.1';
}

server.listen(PORT, '0.0.0.0', () => {
    const ip = getLocalIp();
    console.log('====================================================');
    console.log('  ⚡ Servidor Node.js (Express + WebSocket) Activo  ');
    console.log('====================================================');
    console.log(`\n📱 Abre esta dirección en el navegador de tu PC:\n`);
    console.log(`   👉 http://${ip}:${PORT}\n`);
    console.log('====================================================');
});
