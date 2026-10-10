import re
file_path = 'remote-keyboard-electron/index.html'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# I will find the exact string and replace it
target = """        function setKeyboardMode(mode) {
            currentKeyboardMode = mode;
            localStorage.setItem('rk_keyboard_mode', mode);
            updateKeyboardModeUI();
            appendLog(`🔄 Perfil cambiado a: ${mode === 'code' ? '⚡ Modo Código (Pro)' : '📝 Modo Diario (Normal)'}`);
        }"""

replacement = """        function setKeyboardMode(mode) {
            currentKeyboardMode = mode;
            localStorage.setItem('rk_keyboard_mode', mode);
            updateKeyboardModeUI();
            appendLog(`🔄 Perfil cambiado a: ${mode === 'code' ? '⚡ Modo Código (Pro)' : '📝 Modo Diario (Normal)'}`);
            if (ws && ws.readyState === WebSocket.OPEN) {
                sendPayload({ type: 'set_theme', theme: mode === 'code' ? 'pro' : 'daily' });
            }
        }"""

if "sendPayload({ type: 'set_theme'" not in content:
    # Use re.sub to handle any whitespace variations
    pattern = re.compile(r'function setKeyboardMode\(mode\)\s*\{[^}]*?appendLog\([^}]*?\);\s*\}')
    
    def repl(m):
        original = m.group(0)
        # insert before the last brace
        return original[:-1] + "    if (ws && ws.readyState === WebSocket.OPEN) {\n                sendPayload({ type: 'set_theme', theme: mode === 'code' ? 'pro' : 'daily' });\n            }\n        }"

    content = pattern.sub(repl, content)
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Patched successfully")
else:
    print("Already patched")
