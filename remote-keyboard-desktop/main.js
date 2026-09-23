const { app, BrowserWindow, ipcMain, Tray, Menu, globalShortcut, nativeImage } = require('electron');
const path = require('path');
const http = require('http');
const os = require('os');

let mainWindow = null;
let tray = null;

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 720,
        height: 640,
        minWidth: 500,
        minHeight: 520,
        frame: false,
        transparent: false,
        backgroundColor: '#0F1015',
        show: false,
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            nodeIntegration: false,
            contextIsolation: true
        }
    });

    mainWindow.loadFile(path.join(__dirname, 'src', 'index.html'));

    mainWindow.once('ready-to-show', () => {
        mainWindow.show();
    });

    mainWindow.on('close', (event) => {
        if (!app.isQuitting) {
            event.preventDefault();
            mainWindow.hide();
        }
    });
}

function createTray() {
    // Generate a simple colored dot icon or use asset if available
    const icon = nativeImage.createFromPath(path.join(__dirname, 'assets', 'icon.png'));
    tray = new Tray(icon.isEmpty() ? nativeImage.createEmpty() : icon);
    tray.setToolTip('Remote WiFi Keyboard');

    const contextMenu = Menu.buildFromTemplate([
        { label: 'Mostrar Teclado (Ctrl+Alt+K)', click: () => { mainWindow.show(); mainWindow.focus(); } },
        { type: 'separator' },
        { label: 'Salir', click: () => { app.isQuitting = true; app.quit(); } }
    ]);

    tray.setContextMenu(contextMenu);
    tray.on('double-click', () => {
        if (mainWindow.isVisible()) {
            mainWindow.hide();
        } else {
            mainWindow.show();
            mainWindow.focus();
        }
    });
}

function registerShortcuts() {
    globalShortcut.register('CommandOrControl+Alt+K', () => {
        if (mainWindow.isVisible()) {
            mainWindow.hide();
        } else {
            mainWindow.show();
            mainWindow.focus();
        }
    });
}

// Escaneo rápido de la red local para encontrar el teléfono automáticamente
ipcMain.handle('scan-network', async () => {
    const interfaces = os.networkInterfaces();
    let localSubnet = null;

    for (const name of Object.keys(interfaces)) {
        for (const net of interfaces[name]) {
            if (net.family === 'IPv4' && !net.internal) {
                const parts = net.address.split('.');
                localSubnet = `${parts[0]}.${parts[1]}.${parts[2]}`;
                break;
            }
        }
        if (localSubnet) break;
    }

    if (!localSubnet) return null;

    // Probar rangos comunes de IP en paralelo con timeout de 350ms
    const checkIp = (ip) => {
        return new Promise((resolve) => {
            const req = http.get(`http://${ip}:8080/api/ping`, { timeout: 350 }, (res) => {
                if (res.statusCode === 200) {
                    resolve(ip);
                } else {
                    resolve(null);
                }
            });
            req.on('error', () => resolve(null));
            req.on('timeout', () => {
                req.destroy();
                resolve(null);
            });
        });
    };

    const promises = [];
    for (let i = 1; i <= 254; i++) {
        promises.push(checkIp(`${localSubnet}.${i}`));
    }

    const results = await Promise.all(promises);
    const foundIp = results.find(ip => ip !== null);
    return foundIp || null;
});

// Controles de ventana
ipcMain.on('window-minimize', () => {
    if (mainWindow) mainWindow.minimize();
});

ipcMain.on('window-hide', () => {
    if (mainWindow) mainWindow.hide();
});

ipcMain.on('window-close', () => {
    if (mainWindow) {
        app.isQuitting = true;
        app.quit();
    }
});

app.whenReady().then(() => {
    createWindow();
    createTray();
    registerShortcuts();

    app.on('activate', () => {
        if (BrowserWindow.getAllWindows().length === 0) createWindow();
    });
});

app.on('will-quit', () => {
    globalShortcut.unregisterAll();
});
