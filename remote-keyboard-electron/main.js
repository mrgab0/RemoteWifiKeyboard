if (require('electron-squirrel-startup')) return require('electron').app.quit();

const { app, BrowserWindow, Tray, Menu, ipcMain, globalShortcut } = require('electron');
const path = require('path');

let tray = null;
let mainWindow = null;
let isMiniMode = false;

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

  // Mini Mode IPC
  ipcMain.on('toggle-mini-mode', (event, enable) => {
    isMiniMode = enable;
    if (enable) {
      mainWindow.setMinimumSize(100, 100);
      mainWindow.setSize(180, 180);
      mainWindow.setAlwaysOnTop(true, 'floating');
    } else {
      mainWindow.setMinimumSize(320, 500);
      mainWindow.setSize(380, 700);
      mainWindow.setAlwaysOnTop(false);
    }
  });

  // Global Shortcut
  const ret = globalShortcut.register('CommandOrControl+Alt+A', () => {
    if (mainWindow.isFocused()) {
      mainWindow.blur();
      mainWindow.hide();
    } else {
      mainWindow.show();
      mainWindow.focus();
    }
  });

  if (!ret) {
    console.log('Fallo al registrar el atajo global');
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
