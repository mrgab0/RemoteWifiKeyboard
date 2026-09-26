const fs = require('fs');

let html = fs.readFileSync('index.html', 'utf8');

// 1. Add mini mode CSS
const cssToInsert = `
        body.mini-active {
            margin: 0; padding: 0; width: 100vw; height: 100vh;
            display: flex; align-items: center; justify-content: center;
            background: transparent; overflow: hidden;
        }
        body.mini-active > *:not(#mini-widget) { display: none !important; }
        
        #mini-widget { display: none; }
        body.mini-active #mini-widget {
            display: flex; flex-direction: column; align-items: center; justify-content: center;
            width: 100%; height: 100%; border-radius: 50%; cursor: pointer;
            transition: all 0.2s ease; box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            text-align: center; color: white; font-family: var(--font); font-size: 14px;
            user-select: none; -webkit-app-region: drag; font-weight: bold;
        }
        body.mini-active #mini-widget.status-on { background: #34c759; border: 4px solid #fff; }
        body.mini-active #mini-widget.status-off { background: #ff3b30; border: 4px solid #fff; }
        body.mini-active #mini-widget.status-pending { background: #ff9500; border: 4px solid #fff; }
        body.mini-active #mini-widget:active { transform: scale(0.95); }
`;
html = html.replace('    <style>', '    <style>\n' + cssToInsert);

// 2. Add mini widget HTML
const widgetHtml = `
    <div id="mini-widget" class="status-off">
        <span id="mini-widget-text">Sin Foco</span>
    </div>
`;
html = html.replace('<body>', '<body>\n' + widgetHtml);

// 3. Add the big button "Mini Widget" to the header badges
const buttonHtml = `
            <button id="btn-mini-mode" onclick="enableMiniMode()" style="background:#000; color:#fff; border:none; padding:8px 12px; border-radius:8px; cursor:pointer; font-weight:bold; font-size:14px; margin-right:8px; display:inline-flex; align-items:center; gap:5px;" title="Activar Mini Player">
                <span style="font-size:16px;">🗗</span> Mini Widget
            </button>
`;
html = html.replace('<div class="header-badges">', '<div class="header-badges">\n' + buttonHtml);

// 4. Append JS script
const jsToAppend = `
        const { ipcRenderer } = require('electron');
        let isMini = false;
        let miniState = false;
        let miniWaitDoubleTap = false;
        const miniWidget = document.getElementById('mini-widget');
        const miniWidgetText = document.getElementById('mini-widget-text');

        function enableMiniMode() {
            isMini = true;
            miniState = document.hasFocus();
            document.body.classList.add('mini-active');
            ipcRenderer.send('toggle-mini-mode', true);
            updateMiniUI();
        }

        ipcRenderer.on('window-focus-changed', (event, focused) => {
            miniState = focused;
            miniWaitDoubleTap = false;
            if (isMini) updateMiniUI();
        });

        miniWidget.addEventListener('click', () => {
            if (miniWaitDoubleTap) {
                miniState = !miniState;
                miniWaitDoubleTap = false;
                if (miniState) liveInput.focus();
                else liveInput.blur();
                updateMiniUI();
            } else {
                miniWaitDoubleTap = true;
                miniWidget.className = 'status-pending';
                miniWidgetText.innerHTML = 'Toca otra vez<br><small style="font-size:10px; font-weight:normal;">(Clic derecho para salir)</small>';
                setTimeout(() => {
                    if (miniWaitDoubleTap) {
                        miniWaitDoubleTap = false;
                        updateMiniUI();
                    }
                }, 2000);
            }
        });

        miniWidget.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            isMini = false;
            document.body.classList.remove('mini-active');
            ipcRenderer.send('toggle-mini-mode', false);
        });

        function updateMiniUI() {
            if (!isMini) return;
            if (miniState) {
                miniWidget.className = 'status-on';
                miniWidgetText.innerText = 'ON\\n(Teléfono)';
            } else {
                miniWidget.className = 'status-off';
                miniWidgetText.innerText = 'OFF\\n(PC)';
            }
        }
`;

html = html.replace('setTimeout(connectPhone, 500);', 'setTimeout(connectPhone, 500);\n' + jsToAppend);

fs.writeFileSync('index.html', html, 'utf8');
console.log('Done patching index.html');
