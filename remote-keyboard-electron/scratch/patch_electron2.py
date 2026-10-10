import re
file_path = 'index.html'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

logcat_handler = '''                        } else if (data.type === 'logcat') {
                            const fs = require('fs');
                            const path = require('path');
                            const logPath = path.join(__dirname, 'android_crash_logs.txt');
                            fs.writeFileSync(logPath, data.logs, {flag: 'w'});
                            appendLog('🐞 Logs de Android guardados en ' + logPath);
                        } else if (data.type === 'clipboard_sync') {'''

if 'data.type === \'logcat\'' not in content:
    content = content.replace("} else if (data.type === 'clipboard_sync') {", logcat_handler)

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    print('Patched successfully')
else:
    print('Already patched')
