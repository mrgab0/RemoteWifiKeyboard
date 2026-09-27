const fs = require('fs');

let html = fs.readFileSync('index.html', 'utf8');

// 1. Remove mini-active CSS
const cssRegex = /body\.mini-active \{[\s\S]*?body\.mini-active #mini-widget:active \{ transform: scale\(0\.95\); \}/g;
html = html.replace(cssRegex, '');

// 2. Remove mini-widget HTML
const htmlRegex = /<div id="mini-widget" class="status-off">[\s\S]*?<\/div>/g;
html = html.replace(htmlRegex, '');

// 3. Replace enableMiniMode function call in button
html = html.replace('onclick="enableMiniMode()"', `onclick="require('electron').ipcRenderer.send('toggle-mini-mode', true)"`);

// 4. Extract handleKey logic
const keydownRegex = /liveInput\.addEventListener\('keydown', \(e\) => \{([\s\S]*?)\}\);/g;
let match = keydownRegex.exec(html);
if (match) {
    let innerLogic = match[1];
    
    let replacement = `function handleKey(e) {
${innerLogic}
}
liveInput.addEventListener('keydown', handleKey);
ipcRenderer.on('simulate-keystroke', (event, keyData) => {
    // Create a mock event object for handleKey
    const mockEvent = {
        preventDefault: () => {},
        key: keyData.key,
        code: keyData.code,
        ctrlKey: keyData.ctrlKey,
        altKey: keyData.altKey,
        shiftKey: keyData.shiftKey,
        metaKey: keyData.metaKey
    };
    handleKey(mockEvent);
});`;
    
    html = html.replace(match[0], replacement);
}

// 5. Remove old JS logic
html = html.replace(/let isMini = false;[\s\S]*?function updateMiniUI\(\) \{[\s\S]*?\}\n/g, '');

fs.writeFileSync('index.html', html, 'utf8');
console.log('Fixed index.html');
