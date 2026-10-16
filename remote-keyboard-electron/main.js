if (require('electron-squirrel-startup')) return require('electron').app.quit();

const { app, BrowserWindow, Tray, Menu, ipcMain, globalShortcut, screen } = require('electron');
const path = require('path');

let tray = null;
let mainWindow = null;
let isMiniMode = false;
let isSynergyEnabled = false;
let isSynergyActive = false;
let isWritingActive = false;
let synergyCaptureWindow = null;
let synergyEdgeInterval = null;

let appSettings = {
  universalShortcut: 'CommandOrControl+Shift+Insert',
  widgetHoverActivate: true,
  widgetPosition: 'bottom-left',
  widgetTooltips: true,
  wheelPasteEnabled: true,
  wheelPasteDebounce: 350,
  synergyEnabled: false
};

function createSynergyCaptureWindow() {
  if (synergyCaptureWindow && !synergyCaptureWindow.isDestroyed()) return;

  const primaryDisplay = screen.getPrimaryDisplay();
  const { width, height } = primaryDisplay.bounds;

  synergyCaptureWindow = new BrowserWindow({
    width,
    height,
    x: 0,
    y: 0,
    transparent: true,
    frame: false,
    alwaysOnTop: true,
    skipTaskbar: true,
    hasShadow: false,
    focusable: true,
    show: false,
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false
    }
  });

  synergyCaptureWindow.loadFile('synergy-capture.html');

  synergyCaptureWindow.on('closed', () => {
    synergyCaptureWindow = null;
    isSynergyActive = false;
  });
}

function startEdgeDetection() {
  if (synergyEdgeInterval) clearInterval(synergyEdgeInterval);
  synergyEdgeInterval = setInterval(() => {
    if (!isSynergyEnabled || isSynergyActive) return;

    try {
      const primaryDisplay = screen.getPrimaryDisplay();
      const pt = screen.getCursorScreenPoint();
      const bounds = primaryDisplay.bounds;

      // Colisión con el borde derecho del monitor (Salto hacia Android)
      if (pt.x >= bounds.x + bounds.width - 2) {
        enterSynergyMode();
      }
    } catch (e) {}
  }, 35);
}

function stopEdgeDetection() {
  if (synergyEdgeInterval) {
    clearInterval(synergyEdgeInterval);
    synergyEdgeInterval = null;
  }
}

function enterSynergyMode() {
  if (isSynergyActive) return;
  if (!synergyCaptureWindow || synergyCaptureWindow.isDestroyed()) {
    createSynergyCaptureWindow();
  }
  isSynergyActive = true;
  synergyCaptureWindow.show();
  synergyCaptureWindow.focus();
}

function exitSynergyMode() {
  if (!isSynergyActive) return;
  isSynergyActive = false;
  if (synergyCaptureWindow && !synergyCaptureWindow.isDestroyed()) {
    synergyCaptureWindow.hide();
  }
  if (mainWindow && !mainWindow.isDestroyed() && mainWindow.webContents) {
    mainWindow.webContents.send('synergy-relay-leave');
  }
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 380,
    height: 700,
    minWidth: 150,
    minHeight: 150,
    title: "Remote WiFi Keyboard",
    autoHideMenuBar: true,
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false
    }
  });

  mainWindow.setMenu(null);
  mainWindow.loadFile('index.html');

  mainWindow.on('close', function (event) {
    if (!app.isQuiting) {
      event.preventDefault();
      mainWindow.hide();
    }
    return false;
  });

  mainWindow.on('focus', () => {
    mainWindow.webContents.send('window-focus-changed', true);
  });
  mainWindow.on('blur', () => {
    mainWindow.webContents.send('window-focus-changed', false);
  });
}

app.whenReady().then(() => {
  createWindow();

  let widgetWindow = null;

  function getWidgetPosition(posKey = 'bottom-left', width = 240, height = 200) {
    const primaryDisplay = screen.getPrimaryDisplay();
    const { workArea } = primaryDisplay;
    let x = workArea.x + 24;
    let y = workArea.y + workArea.height - height - 24;

    if (posKey === 'bottom-right') {
      x = workArea.x + workArea.width - width - 24;
      y = workArea.y + workArea.height - height - 24;
    } else if (posKey === 'top-left') {
      x = workArea.x + 24;
      y = workArea.y + 24;
    } else if (posKey === 'top-right') {
      x = workArea.x + workArea.width - width - 24;
      y = workArea.y + 24;
    }
    return { x: Math.round(x), y: Math.round(y) };
  }

  // Mini Mode IPC
  ipcMain.on('toggle-mini-mode', (event, enable) => {
    isMiniMode = enable;
    if (enable) {
      if (!widgetWindow) {
        const { x, y } = getWidgetPosition(appSettings.widgetPosition, 240, 200);
        widgetWindow = new BrowserWindow({
          width: 240, height: 200,
          x, y,
          transparent: true, frame: false,
          alwaysOnTop: true, resizable: false,
          skipTaskbar: true,
          webPreferences: { nodeIntegration: true, contextIsolation: false }
        });
        widgetWindow.loadFile('widget.html');
        widgetWindow.webContents.on('did-finish-load', () => {
          widgetWindow.webContents.send('settings-sync', appSettings);
        });
        widgetWindow.on('closed', () => { widgetWindow = null; });
      } else {
        const { x, y } = getWidgetPosition(appSettings.widgetPosition, 240, 200);
        widgetWindow.setPosition(x, y);
      }
      mainWindow.hide();
      widgetWindow.show();
    } else {
      if (widgetWindow) {
        widgetWindow.close();
      }
      mainWindow.show();
    }
  });

  ipcMain.on('close-widget', () => {
    if (widgetWindow) widgetWindow.close();
    mainWindow.show();
    mainWindow.webContents.send('mini-mode-closed');
  });

  // Movimiento libre del widget a cualquier parte de la pantalla
  ipcMain.on('widget-move', (event, { deltaX, deltaY }) => {
    if (widgetWindow && !widgetWindow.isDestroyed()) {
      const [currX, currY] = widgetWindow.getPosition();
      widgetWindow.setPosition(Math.round(currX + deltaX), Math.round(currY + deltaY));
    }
  });

  // Auto-encendido al posar el mouse (hover activate)
  ipcMain.on('widget-hover-activate', () => {
    if (widgetWindow && !widgetWindow.isDestroyed()) {
      if (appSettings.widgetHoverActivate) {
        widgetWindow.focus();
      }
    }
  });

  ipcMain.on('widget-wake-device', () => {
    if (mainWindow && mainWindow.webContents) {
      mainWindow.webContents.send('wake-android-device');
    }
  });

  ipcMain.on('widget-keystroke', (event, keyData) => {
    if (mainWindow && mainWindow.webContents) {
      mainWindow.webContents.send('simulate-keystroke', keyData);
    }
  });

  ipcMain.on('widget-paste', () => {
    if (mainWindow && mainWindow.webContents) {
      mainWindow.webContents.send('trigger-paste');
    }
  });

  ipcMain.on('focus-android-input', () => {
    if (mainWindow && !mainWindow.isDestroyed() && mainWindow.webContents) {
      mainWindow.webContents.send('trigger-focus-input');
    }
  });

  ipcMain.on('update-ping', (event, ping) => {
    if (widgetWindow && widgetWindow.webContents) {
      widgetWindow.webContents.send('ping-update', ping);
    }
  });

  // Mouse Synergy IPC Handlers
  ipcMain.on('toggle-synergy', (event, enable) => {
    isSynergyEnabled = enable;
    if (enable) {
      createSynergyCaptureWindow();
      startEdgeDetection();
    } else {
      stopEdgeDetection();
      exitSynergyMode();
    }
    if (mainWindow && !mainWindow.isDestroyed() && mainWindow.webContents) {
      mainWindow.webContents.send('synergy-status-changed', isSynergyEnabled);
    }
    if (widgetWindow && !widgetWindow.isDestroyed() && widgetWindow.webContents) {
      widgetWindow.webContents.send('synergy-status-changed', isSynergyEnabled);
    }
  });

  ipcMain.on('synergy-exit', () => {
    exitSynergyMode();
  });

  ipcMain.on('synergy-mouse-move', (event, { x, y }) => {
    if (mainWindow && !mainWindow.isDestroyed() && mainWindow.webContents) {
      mainWindow.webContents.send('synergy-relay-move', { x, y });
    }
  });

  ipcMain.on('synergy-mouse-click', (event, { button, action }) => {
    if (mainWindow && !mainWindow.isDestroyed() && mainWindow.webContents) {
      mainWindow.webContents.send('synergy-relay-click', { button, action });
    }
  });

  ipcMain.on('synergy-mouse-scroll', (event, { deltaY }) => {
    if (mainWindow && !mainWindow.isDestroyed() && mainWindow.webContents) {
      mainWindow.webContents.send('synergy-relay-scroll', { deltaY });
    }
  });

  // Atajo rápido: Ctrl + Shift + Rueda del Ratón para alternar Synergy desde cualquier parte
  ipcMain.on('toggle-synergy-shortcut', () => {
    const newState = !isSynergyEnabled;
    isSynergyEnabled = newState;
    if (newState) {
      createSynergyCaptureWindow();
      startEdgeDetection();
    } else {
      stopEdgeDetection();
      exitSynergyMode();
    }
    if (mainWindow && !mainWindow.isDestroyed() && mainWindow.webContents) {
      mainWindow.webContents.send('synergy-status-changed', isSynergyEnabled);
    }
    if (widgetWindow && !widgetWindow.isDestroyed() && widgetWindow.webContents) {
      widgetWindow.webContents.send('synergy-status-changed', isSynergyEnabled);
    }
  });

  // Configuración IPC
  ipcMain.on('save-settings', (event, newSettings) => {
    appSettings = { ...appSettings, ...newSettings };
    if (newSettings.universalShortcut) {
      registerUniversalShortcut(newSettings.universalShortcut);
    }
    if (widgetWindow && !widgetWindow.isDestroyed()) {
      widgetWindow.webContents.send('settings-sync', appSettings);
    }
  });

  ipcMain.handle('get-settings', () => {
    return appSettings;
  });

  // Window Controls IPC
  ipcMain.on('window-minimize', () => {
    if (mainWindow) mainWindow.minimize();
  });
  ipcMain.on('window-maximize', () => {
    if (mainWindow) {
      if (mainWindow.isMaximized()) mainWindow.unmaximize();
      else mainWindow.maximize();
    }
  });
  ipcMain.on('window-close', () => {
    if (mainWindow) mainWindow.close();
  });

  // Manejador IPC para alternar el estado de escritura remota
  ipcMain.on('toggle-writing-state', (event, explicitState) => {
    if (typeof explicitState === 'boolean') {
      isWritingActive = explicitState;
    } else {
      isWritingActive = !isWritingActive;
    }
    if (mainWindow && !mainWindow.isDestroyed() && mainWindow.webContents) {
      mainWindow.webContents.send('writing-state-changed', isWritingActive);
      if (isWritingActive) mainWindow.webContents.send('wake-android-device');
    }
    if (widgetWindow && !widgetWindow.isDestroyed() && widgetWindow.webContents) {
      widgetWindow.webContents.send('writing-state-changed', isWritingActive);
      if (isWritingActive) widgetWindow.webContents.send('wake-android-device');
    }
  });

  ipcMain.handle('get-writing-state', () => {
    return isWritingActive;
  });

  // Atajo universal alternable para activar/desactivar escritura a Android (Ctrl+Shift+Insert)
  let registeredShortcut = null;
  function registerUniversalShortcut(keyCombo) {
    if (registeredShortcut) {
      try { globalShortcut.unregister(registeredShortcut); } catch(e){}
    }
    registeredShortcut = keyCombo || appSettings.universalShortcut;
    try {
      const ok = globalShortcut.register(registeredShortcut, () => {
        isWritingActive = !isWritingActive;
        if (mainWindow && !mainWindow.isDestroyed() && mainWindow.webContents) {
          mainWindow.webContents.send('writing-state-changed', isWritingActive);
          if (isWritingActive) {
            mainWindow.webContents.send('wake-android-device');
          }
        }
        if (widgetWindow && !widgetWindow.isDestroyed() && widgetWindow.webContents) {
          widgetWindow.webContents.send('writing-state-changed', isWritingActive);
          if (isWritingActive) {
            widgetWindow.webContents.send('wake-android-device');
          }
        }
      });
      if (!ok) console.log('No se pudo registrar atajo universal:', registeredShortcut);
    } catch(e) {
      console.log('Error registrando atajo universal:', e.message);
    }
  }

  registerUniversalShortcut(appSettings.universalShortcut);

  // Atajo secundario clásico (Ctrl+Alt+A) para mostrar/ocultar ventana principal
  globalShortcut.register('CommandOrControl+Alt+A', () => {
    if (mainWindow.isFocused()) {
      mainWindow.blur();
      mainWindow.hide();
    } else {
      mainWindow.show();
      mainWindow.focus();
    }
  });

// Enable DevTools only when the app is started with a '--dev' flag
if (process.argv.includes('--dev')) {
  mainWindow.webContents.openDevTools({ mode: 'detach' });
}

  // Create tray icon
  try {
    const { nativeImage } = require('electron');
    const base64Icon = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAZUlEQVQ4T2NkYGD4z8DAwMgAA0xISDCAG0CSYIQpQAkwM2BgYMSQYCSjARhkI0QBSAMjAwPDfzAGS1EMgDQwMjAw/CdDAyMDA8N/MmyAGwaWApAGGBgY/hOhAaQBBgaG/2RoQJYBAOJ5Jg34E17sAAAAAElFTkSuQmCC';
    let icon = nativeImage.createFromDataURL(base64Icon);
    tray = new Tray(icon);
  } catch (e) {
    console.log('Error creating Tray');
  }

  if (tray) {
    const contextMenu = Menu.buildFromTemplate([
      { label: 'Mostrar', click: () => { mainWindow.show(); } },
      { label: 'Salir', click: () => { app.isQuiting = true; app.quit(); } }
    ]);
    tray.setToolTip('Remote WiFi Keyboard');
    tray.setContextMenu(contextMenu);

    tray.on('click', () => {
      mainWindow.show();
    });
  }

  app.on('activate', function () {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', function () {
  if (process.platform !== 'darwin') app.quit();
});

app.on('will-quit', () => {
  globalShortcut.unregisterAll();
});
