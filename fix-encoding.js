const fs = require('fs');

let html = fs.readFileSync('remote-keyboard-electron/index.html', 'utf8');

// The original file contains mojibake like:
// CONEXIÃ³N CON TELÃ©FONO -> CONEXIÓN CON TELÉFONO
// We can just define a dictionary of replacements
const fixes = {
    'CONEXIÃ³N': 'CONEXIÓN',
    'TELÃ©FONO': 'TELÉFONO',
    'PÃ¡RRAFO': 'PÁRRAFO',
    'RÃ¡PIDAS': 'RÁPIDAS',
    'TELEMETRÃ\x8dA': 'TELEMETRÍA',
    'ULTRARRÃ¡PIDA': 'ULTRARRÁPIDA',
    'aquÃ\xad': 'aquí',
    'telÃ©fono': 'teléfono',
    'TelÃ©fono': 'Teléfono',
    'Ãº': 'ú',
    'Ã³': 'ó',
    'Ã¡': 'á',
    'Ã©': 'é',
    'Ã\xad': 'í',
    'â†\x92': '→',
    'ðŸ“±': '📱',
    'ðŸš€': '🚀',
    'ðŸŽ¤': '🎤',
    'ðŸ”®': '🔍',
    'âš¡': '⚡',
    'ðŸ’¡': '💡',
    'ðŸ›¡': '🛡',
    'ðŸ“¦': '📦',
    'ðŸ“Œ': '📌',
    'âŽ\x8e': '⎎',
    'âŒ«': '⌫',
    'â†¥': '⇥',
    'â\x90£': '␣',
    'â–\xac': '▭',
    'â†‘': '↑',
    'â†“': '↓',
    'â†\x90': '←',
    'â†\x92': '→',
    'â\x97\x8f': '●',
    'âš\xa0ï¸\x8f': '⚠️',
    'âš\xa0': '⚠️',
    'ðŸŸ¢': '🟢',
    'ðŸ”\x8d': '🔍',
    'âœ…': '✅'
};

// However, it looks like PowerShell converted these bad characters to \ufffd ().
// So the above dictionary won't work if they are .
// Let's check if there are  in the file.
if (html.includes('\ufffd')) {
    // If it has , we just replace the whole sentences.
    html = html.replace(/Cliente Windows   Android/g, 'Cliente Windows ➔ Android');
    html = html.replace(/ CONEXIN CON TELFONO/g, '📱 CONEXIÓN CON TELÉFONO');
    html = html.replace(/ Test Ping/g, '🚀 Test Ping');
    html = html.replace(/ Auto-Detectar/g, '🔍 Auto-Detectar');
    html = html.replace(/ ESCRITURA ULTRARRPIDA \(<3MS\)/g, '⚡ ESCRITURA ULTRARRÁPIDA (<3MS)');
    html = html.replace(/Escribe directamente aqu/g, 'Escribe directamente aquí');
    html = html.replace(/Haz clic aqu y escribe con tu teclado.../g, 'Haz clic aquí y escribe con tu teclado...');
    html = html.replace(/ PRRAFO O PORTAPAPELES/g, '📝 PÁRRAFO O PORTAPAPELES');
    html = html.replace(/al telfono/g, 'al teléfono');
    html = html.replace(/ Dictar/g, '🎤 Dictar');
    html = html.replace(/Enviar al Telfono /g, 'Enviar al Teléfono 🚀');
    html = html.replace(/ TECLAS RPIDAS/g, '⌨️ TECLAS RÁPIDAS');
    html = html.replace(/\s+Enter/g, '⎎ Enter');
    html = html.replace(/\s+Borrar/g, '⌫ Borrar');
    html = html.replace(/\s+Tab/g, '⇥ Tab');
    html = html.replace(/\s+Espacio/g, '␣ Espacio');
    html = html.replace(/\s+Selec. Todo/g, '▭ Selec. Todo');
    html = html.replace(/\s+Izquierda/g, '← Izquierda');
    html = html.replace(/\s+Derecha/g, '→ Derecha');
    html = html.replace(/\s+Arriba/g, '↑ Arriba');
    html = html.replace(/\s+Abajo/g, '↓ Abajo');
    html = html.replace(/ TELEMETRA/g, '📊 TELEMETRÍA');
    html = html.replace(/<span style="font-size:16px;">=<\/span>/g, '<span style="font-size:16px;">🗗</span>');
    html = html.replace(/=4 Escuchando.../g, '🔴 Escuchando...');
    html = html.replace(/< Dictado iniciado/g, '🎤 Dictado iniciado');
    html = html.replace(/  Error de dictado:/g, '⚠️ Error de dictado:');
    html = html.replace(/< Dictar/g, '🎤 Dictar');
    html = html.replace(/\(Telfono\)/g, '(Teléfono)');
    html = html.replace(/focusIcon.innerText = '=';/g, "focusIcon.innerText = '🟢';");
    html = html.replace(/focusText.innerText = 'Capturando Teclas Fsicas';/g, "focusText.innerText = 'Capturando Teclas Físicas';");
    html = html.replace(/focusIcon.innerText = ' ';/g, "focusIcon.innerText = '⚠️';");
    html = html.replace(/focusText.innerText = 'Ventana sin foco: Haz clic aqu para escribir';/g, "focusText.innerText = 'Ventana sin foco: Haz clic aquí para escribir';");
    html = html.replace(/appendLog\("  No se detect automticamente. Ingresa la IP que muestra la app de tu telfono."\);/g, 'appendLog("⚠️ No se detectó automáticamente. Ingresa la IP que muestra la app de tu teléfono.");');
    html = html.replace(/appendLog\(' Ejecutando test de latencia en rfaga \(10 paquetes\)...'\);/g, "appendLog('🚀 Ejecutando test de latencia en ráfaga (10 paquetes)...');");
    html = html.replace(/appendLog\(' Test completado.'\);/g, "appendLog('✅ Test completado.');");
    html = html.replace(/appendLog\(" Escaneando red local en busca de tu telfono Android..."\);/g, 'appendLog("🔍 Escaneando red local en busca de tu teléfono Android...");');
    html = html.replace(/<div class="brand-icon">\( <\/div>/g, '<div class="brand-icon">⚡</div>');
} else {
    // Replace the mojibake
    for (const [bad, good] of Object.entries(fixes)) {
        html = html.split(bad).join(good);
    }
}

// Ensure the UI is fixed
html = html.replace('CONEXIÃ³N', 'CONEXIÓN');
html = html.replace('TELÃ©FONO', 'TELÉFONO');

fs.writeFileSync('remote-keyboard-electron/index.html', html, 'utf8');
console.log('Fixed!');
