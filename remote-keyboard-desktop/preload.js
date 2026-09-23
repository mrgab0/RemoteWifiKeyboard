const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
    scanNetwork: () => ipcRenderer.invoke('scan-network'),
    minimize: () => ipcRenderer.send('window-minimize'),
    hide: () => ipcRenderer.send('window-hide'),
    close: () => ipcRenderer.send('window-close')
});
