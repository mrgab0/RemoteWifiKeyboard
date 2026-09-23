let currentIp = localStorage.getItem('remote_keyboard_ip') || '';
let isConnected = false;

// DOM Elements
const realtimeInput = document.getElementById('realtimeInput');
const paragraphInput = document.getElementById('paragraphInput');
const btnSend = document.getElementById('btnSend');
const btnClear = document.getElementById('btnClear');
const statusDot = document.getElementById('statusDot');
const statusText = document.getElementById('statusText');
const deviceIpDisplay = document.getElementById('deviceIpDisplay');
const btnScan = document.getElementById('btnScan');
const btnManualIp = document.getElementById('btnManualIp');

const ipModal = document.getElementById('ipModal');
const manualIpInput = document.getElementById('manualIpInput');
const btnCancelModal = document.getElementById('btnCancelModal');
const btnSaveModal = document.getElementById('btnSaveModal');

// Titlebar Controls
document.getElementById('btnMinimize').addEventListener('click', () => window.electronAPI.minimize());
document.getElementById('btnHide').addEventListener('click', () => window.electronAPI.hide());
document.getElementById('btnClose').addEventListener('click', () => window.electronAPI.close());

// Quick Key Buttons
document.querySelectorAll('.key-button').forEach(btn => {
    btn.addEventListener('click', () => {
        const key = btn.getAttribute('data-key');
        if (key) sendKey(key);
    });
});

// API Helpers
async function sendApi(endpoint, body) {
    if (!currentIp) return false;
    try {
        const res = await fetch(`http://${currentIp}:8080${endpoint}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        return res.ok;
    } catch (e) {
        return false;
    }
}

async function sendText(text) {
    return sendApi('/api/type', { text: text });
}

async function sendKey(key) {
    return sendApi('/api/key', { key: key });
}

// Realtime Input Event Handlers
realtimeInput.addEventListener('keydown', async (e) => {
    if (e.key === 'Enter') {
        e.preventDefault();
        await sendKey('ENTER');
        realtimeInput.value = '';
    } else if (e.key === 'Backspace' && realtimeInput.value.length === 0) {
        e.preventDefault();
        await sendKey('BACKSPACE');
    } else if (e.key === 'Tab') {
        e.preventDefault();
        await sendKey('TAB');
    } else if (e.key === 'ArrowLeft' && realtimeInput.selectionStart === 0) {
        e.preventDefault();
        await sendKey('ARROW_LEFT');
    } else if (e.key === 'ArrowRight' && realtimeInput.selectionEnd === realtimeInput.value.length) {
        e.preventDefault();
        await sendKey('ARROW_RIGHT');
    }
});

realtimeInput.addEventListener('input', async () => {
    if (realtimeInput.value.length > 0) {
        const text = realtimeInput.value;
        realtimeInput.value = '';
        await sendText(text);
    }
});

// Paragraph Sender
btnSend.addEventListener('click', async () => {
    const text = paragraphInput.value;
    if (!text.trim()) return;
    btnSend.disabled = true;
    btnSend.innerText = 'Enviando...';
    await sendText(text);
    paragraphInput.value = '';
    btnSend.disabled = false;
    btnSend.innerText = 'Enviar al Teléfono 🚀';
    realtimeInput.focus();
});

btnClear.addEventListener('click', () => {
    paragraphInput.value = '';
});

paragraphInput.addEventListener('keydown', (e) => {
    if (e.ctrlKey && e.key === 'Enter') {
        e.preventDefault();
        btnSend.click();
    }
});

// Manual IP Modal
btnManualIp.addEventListener('click', () => {
    manualIpInput.value = currentIp || '192.168.1.';
    ipModal.classList.remove('hidden');
    manualIpInput.focus();
});

btnCancelModal.addEventListener('click', () => {
    ipModal.classList.add('hidden');
});

btnSaveModal.addEventListener('click', () => {
    const ip = manualIpInput.value.trim();
    if (ip) {
        currentIp = ip;
        localStorage.setItem('remote_keyboard_ip', ip);
        ipModal.classList.add('hidden');
        checkConnection();
    }
});

// Auto-Scan Network
async function triggerAutoScan() {
    statusText.innerText = 'Escaneando subred Wi-Fi...';
    btnScan.disabled = true;
    btnScan.innerText = 'Buscando...';

    try {
        const foundIp = await window.electronAPI.scanNetwork();
        if (foundIp) {
            currentIp = foundIp;
            localStorage.setItem('remote_keyboard_ip', foundIp);
            checkConnection();
        } else {
            statusText.innerText = 'No se encontró teléfono. Verifica que la app esté abierta.';
        }
    } catch (e) {
        statusText.innerText = 'Error al escanear red.';
    }

    btnScan.disabled = false;
    btnScan.innerText = '🔍 Auto-Detectar';
}

btnScan.addEventListener('click', triggerAutoScan);

// Check Connection Ping Loop
async function checkConnection() {
    if (!currentIp) {
        statusDot.className = 'status-dot disconnected';
        statusText.innerText = 'Sin IP configurada';
        deviceIpDisplay.innerText = 'Haz clic en Auto-Detectar';
        isConnected = false;
        return;
    }

    try {
        const res = await fetch(`http://${currentIp}:8080/api/ping`, { timeout: 1000 });
        if (res.ok) {
            statusDot.className = 'status-dot connected';
            statusText.innerText = 'Conectado al Teléfono';
            deviceIpDisplay.innerText = `http://${currentIp}:8080`;
            isConnected = true;
        } else {
            throw new Error('Not ok');
        }
    } catch (e) {
        statusDot.className = 'status-dot disconnected';
        statusText.innerText = 'Desconectado';
        deviceIpDisplay.innerText = `http://${currentIp}:8080 (Sin respuesta)`;
        isConnected = false;
    }
}

// Init
if (currentIp) {
    checkConnection();
} else {
    triggerAutoScan();
}

setInterval(checkConnection, 4000);
