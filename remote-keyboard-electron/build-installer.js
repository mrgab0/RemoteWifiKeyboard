const electronInstaller = require('electron-winstaller');
const path = require('path');

async function createInstaller() {
  console.log('Building installer...');
  try {
    await electronInstaller.createWindowsInstaller({
      appDirectory: path.join(__dirname, 'dist-pro/Remote WiFi Keyboard-win32-x64'),
      outputDirectory: path.join(__dirname, 'installer'),
      authors: 'mrgab0',
      exe: 'Remote WiFi Keyboard.exe',
      setupExe: 'RemoteWiFiKeyboard-Setup.exe',
      setupIcon: undefined, // Add icon if available
      noMsi: true,
      description: 'Remote WiFi Keyboard Server for PC'
    });
    console.log('It worked!');
  } catch (e) {
    console.log(`No dice: ${e.message}`);
  }
}

createInstaller();
