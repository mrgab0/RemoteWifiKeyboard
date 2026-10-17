const electronInstaller = require('electron-winstaller');
const path = require('path');

async function createInstaller() {
  console.log('Building installer with Cyberpunk Pro Icon & Animated Splash...');
  try {
    await electronInstaller.createWindowsInstaller({
      appDirectory: path.join(__dirname, 'dist-pro/Remote WiFi Keyboard-win32-x64'),
      outputDirectory: path.join(__dirname, 'installer'),
      authors: 'mrgab0',
      exe: 'Remote WiFi Keyboard.exe',
      setupExe: 'RemoteWiFiKeyboard-Setup.exe',
      setupIcon: path.join(__dirname, 'icon.ico'),
      iconUrl: 'https://raw.githubusercontent.com/mrgab0/RemoteWifiKeyboard/main/remote-keyboard-electron/icon.ico',
      loadingGif: path.join(__dirname, 'loading.gif'),
      noMsi: true,
      description: 'Remote WiFi Keyboard Server for PC'
    });
    console.log('INSTALLER_BUILD_SUCCESS');
  } catch (e) {
    console.log(`No dice: ${e.message}`);
  }
}

createInstaller();
