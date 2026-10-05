// shortcuts.js — Catálogo completo de atajos de teclado para Developer
// Organizado en categorías. Cada entrada tiene:
//   label    : texto del botón
//   combo    : texto descriptivo del atajo (solo display)
//   type     : 'key' (sendSpecialKey) | 'key_event' (sendRawKey down+up)
//   key      : nombre de tecla especial (solo para type:'key')
//   code     : Web KeyboardEvent.code (solo para type:'key_event')
//   mods     : array de modificadores: 'CTRL','SHIFT','ALT','META'
//   text     : texto literal a enviar (solo para type:'text')

module.exports = [
    {
        id: 'universal',
        label: '🌐 Universal',
        shortcuts: [
            // Edición básica
            { label: 'Selec. Todo',   combo: 'Ctrl+A',         type: 'key',       key: 'SELECT_ALL' },
            { label: 'Copiar',        combo: 'Ctrl+C',         type: 'key',       key: 'COPY' },
            { label: 'Pegar',         combo: 'Ctrl+V',         type: 'key',       key: 'PASTE' },
            { label: 'Cortar',        combo: 'Ctrl+X',         type: 'key',       key: 'CUT' },
            { label: 'Deshacer',      combo: 'Ctrl+Z',         type: 'key_event', code: 'KeyZ',   mods: ['CTRL'] },
            { label: 'Rehacer',       combo: 'Ctrl+Y',         type: 'key_event', code: 'KeyY',   mods: ['CTRL'] },
            { label: 'Buscar',        combo: 'Ctrl+F',         type: 'key_event', code: 'KeyF',   mods: ['CTRL'] },
            { label: 'Guardar',       combo: 'Ctrl+S',         type: 'key_event', code: 'KeyS',   mods: ['CTRL'] },
            { label: 'Guardar Todo',  combo: 'Ctrl+Shift+S',   type: 'key_event', code: 'KeyS',   mods: ['CTRL','SHIFT'] },
            { label: 'Abrir',         combo: 'Ctrl+O',         type: 'key_event', code: 'KeyO',   mods: ['CTRL'] },
            { label: 'Nuevo',         combo: 'Ctrl+N',         type: 'key_event', code: 'KeyN',   mods: ['CTRL'] },
            { label: 'Imprimir',      combo: 'Ctrl+P',         type: 'key_event', code: 'KeyP',   mods: ['CTRL'] },
            // Navegación de texto
            { label: 'Inicio Doc',    combo: 'Ctrl+Home',      type: 'key_event', code: 'Home',   mods: ['CTRL'] },
            { label: 'Fin Doc',       combo: 'Ctrl+End',       type: 'key_event', code: 'End',    mods: ['CTRL'] },
            { label: 'Borra Palabra', combo: 'Ctrl+Backspace', type: 'key_event', code: 'Backspace', mods: ['CTRL'] },
            { label: 'Borra Sig Pal', combo: 'Ctrl+Delete',   type: 'key_event', code: 'Delete', mods: ['CTRL'] },
            // Selección
            { label: 'Sel ← Pal',    combo: 'Ctrl+Shift+←',  type: 'key_event', code: 'ArrowLeft',  mods: ['CTRL','SHIFT'] },
            { label: 'Sel → Pal',    combo: 'Ctrl+Shift+→',  type: 'key_event', code: 'ArrowRight', mods: ['CTRL','SHIFT'] },
            { label: 'Sel Inicio',    combo: 'Shift+Home',     type: 'key_event', code: 'Home',   mods: ['SHIFT'] },
            { label: 'Sel Fin',       combo: 'Shift+End',      type: 'key_event', code: 'End',    mods: ['SHIFT'] },
            { label: 'Sel ↑ Página',  combo: 'Shift+PgUp',    type: 'key_event', code: 'PageUp', mods: ['SHIFT'] },
            { label: 'Sel ↓ Página',  combo: 'Shift+PgDown',  type: 'key_event', code: 'PageDown', mods: ['SHIFT'] },
        ]
    },
    {
        id: 'navigation',
        label: '🧭 Navegación',
        shortcuts: [
            { label: 'Home',          combo: 'Home',           type: 'key_event', code: 'Home',     mods: [] },
            { label: 'End',           combo: 'End',            type: 'key_event', code: 'End',      mods: [] },
            { label: 'Pág Arriba',    combo: 'PgUp',           type: 'key_event', code: 'PageUp',   mods: [] },
            { label: 'Pág Abajo',     combo: 'PgDown',         type: 'key_event', code: 'PageDown', mods: [] },
            { label: 'Insert',        combo: 'Insert',         type: 'key_event', code: 'Insert',   mods: [] },
            { label: 'Delete (Supr)', combo: 'Delete',         type: 'key',       key: 'DELETE' },
            { label: 'Backspace',     combo: 'Backspace',      type: 'key',       key: 'BACKSPACE' },
            { label: 'Enter',         combo: 'Enter',          type: 'key',       key: 'ENTER' },
            { label: 'Tab',           combo: 'Tab',            type: 'key',       key: 'TAB' },
            { label: 'Shift+Tab',     combo: 'Shift+Tab',      type: 'key_event', code: 'Tab',      mods: ['SHIFT'] },
            { label: 'Escape',        combo: 'Esc',            type: 'key_event', code: 'Escape',   mods: [] },
            { label: '← Palabra',    combo: 'Ctrl+←',         type: 'key_event', code: 'ArrowLeft',  mods: ['CTRL'] },
            { label: '→ Palabra',    combo: 'Ctrl+→',         type: 'key_event', code: 'ArrowRight', mods: ['CTRL'] },
        ]
    },
    {
        id: 'function_keys',
        label: '🔧 F-Keys',
        shortcuts: [
            { label: 'F1  Ayuda',     combo: 'F1',   type: 'key_event', code: 'F1',  mods: [] },
            { label: 'F2  Renombrar', combo: 'F2',   type: 'key_event', code: 'F2',  mods: [] },
            { label: 'F3  Buscar sig',combo: 'F3',   type: 'key_event', code: 'F3',  mods: [] },
            { label: 'F4  Cerrar/Dir',combo: 'F4',   type: 'key_event', code: 'F4',  mods: [] },
            { label: 'F5  Ejecutar',  combo: 'F5',   type: 'key_event', code: 'F5',  mods: [] },
            { label: 'F6  Foco',      combo: 'F6',   type: 'key_event', code: 'F6',  mods: [] },
            { label: 'F7  Revisar',   combo: 'F7',   type: 'key_event', code: 'F7',  mods: [] },
            { label: 'F8  Debug',     combo: 'F8',   type: 'key_event', code: 'F8',  mods: [] },
            { label: 'F9  Breakpt',   combo: 'F9',   type: 'key_event', code: 'F9',  mods: [] },
            { label: 'F10 Step Over', combo: 'F10',  type: 'key_event', code: 'F10', mods: [] },
            { label: 'F11 Step Into', combo: 'F11',  type: 'key_event', code: 'F11', mods: [] },
            { label: 'F12 Definición',combo: 'F12',  type: 'key_event', code: 'F12', mods: [] },
            { label: 'Shift+F5 Ret.', combo: 'Shift+F5',  type: 'key_event', code: 'F5',  mods: ['SHIFT'] },
            { label: 'Ctrl+F5 Nuevo', combo: 'Ctrl+F5',   type: 'key_event', code: 'F5',  mods: ['CTRL'] },
            { label: 'Alt+F4  Cerrar',combo: 'Alt+F4',    type: 'key_event', code: 'F4',  mods: ['ALT'] },
        ]
    },
    {
        id: 'vscode',
        label: '💜 VS Code',
        shortcuts: [
            { label: 'Paleta Cmds',   combo: 'Ctrl+Shift+P',  type: 'key_event', code: 'KeyP',   mods: ['CTRL','SHIFT'] },
            { label: 'Abrir Archivo', combo: 'Ctrl+P',        type: 'key_event', code: 'KeyP',   mods: ['CTRL'] },
            { label: 'Ir a Línea',    combo: 'Ctrl+G',        type: 'key_event', code: 'KeyG',   mods: ['CTRL'] },
            { label: 'Terminal',      combo: 'Ctrl+`',        type: 'key_event', code: 'Backquote', mods: ['CTRL'] },
            { label: 'Split Term.',   combo: 'Ctrl+Shift+5',  type: 'key_event', code: 'Digit5', mods: ['CTRL','SHIFT'] },
            { label: 'Explorador',    combo: 'Ctrl+Shift+E',  type: 'key_event', code: 'KeyE',   mods: ['CTRL','SHIFT'] },
            { label: 'Buscar Arch.',  combo: 'Ctrl+Shift+F',  type: 'key_event', code: 'KeyF',   mods: ['CTRL','SHIFT'] },
            { label: 'Extensiones',   combo: 'Ctrl+Shift+X',  type: 'key_event', code: 'KeyX',   mods: ['CTRL','SHIFT'] },
            { label: 'Comentar',      combo: 'Ctrl+/',        type: 'key_event', code: 'Slash',  mods: ['CTRL'] },
            { label: 'Duplicar Lín.', combo: 'Alt+Shift+↓',  type: 'key_event', code: 'ArrowDown', mods: ['ALT','SHIFT'] },
            { label: 'Mover Lín ↑',   combo: 'Alt+↑',        type: 'key_event', code: 'ArrowUp',   mods: ['ALT'] },
            { label: 'Mover Lín ↓',   combo: 'Alt+↓',        type: 'key_event', code: 'ArrowDown', mods: ['ALT'] },
            { label: 'Borrar Línea',  combo: 'Ctrl+Shift+K', type: 'key_event', code: 'KeyK',   mods: ['CTRL','SHIFT'] },
            { label: 'Selec. Línea',  combo: 'Ctrl+L',        type: 'key_event', code: 'KeyL',   mods: ['CTRL'] },
            { label: 'Multi Cursor ↑',combo: 'Ctrl+Alt+↑',   type: 'key_event', code: 'ArrowUp',   mods: ['CTRL','ALT'] },
            { label: 'Multi Cursor ↓',combo: 'Ctrl+Alt+↓',   type: 'key_event', code: 'ArrowDown', mods: ['CTRL','ALT'] },
            { label: 'Go to Symbol',  combo: 'Ctrl+Shift+O', type: 'key_event', code: 'KeyO',   mods: ['CTRL','SHIFT'] },
            { label: 'Peek Def.',     combo: 'Alt+F12',       type: 'key_event', code: 'F12',    mods: ['ALT'] },
            { label: 'Rename Symbol', combo: 'F2',            type: 'key_event', code: 'F2',     mods: [] },
            { label: 'Quick Fix',     combo: 'Ctrl+.',        type: 'key_event', code: 'Period', mods: ['CTRL'] },
            { label: 'Format Doc',    combo: 'Shift+Alt+F',  type: 'key_event', code: 'KeyF',   mods: ['SHIFT','ALT'] },
            { label: 'Reemplazar',    combo: 'Ctrl+H',        type: 'key_event', code: 'KeyH',   mods: ['CTRL'] },
            { label: 'Sidebar',       combo: 'Ctrl+B',        type: 'key_event', code: 'KeyB',   mods: ['CTRL'] },
            { label: 'Zen Mode',      combo: 'Ctrl+K Z',      type: 'key_event', code: 'KeyZ',   mods: ['CTRL'] },
        ]
    },
    {
        id: 'intellij',
        label: '🟠 IntelliJ',
        shortcuts: [
            { label: 'Find Action',   combo: 'Ctrl+Shift+A', type: 'key_event', code: 'KeyA',   mods: ['CTRL','SHIFT'] },
            { label: 'Navigate',      combo: 'Ctrl+N',       type: 'key_event', code: 'KeyN',   mods: ['CTRL'] },
            { label: 'Everywhere',    combo: 'Shift×2',       type: 'key_event', code: 'ShiftLeft', mods: ['SHIFT'] },
            { label: 'Quick Fix',     combo: 'Alt+Enter',    type: 'key_event', code: 'Enter',  mods: ['ALT'] },
            { label: 'Duplicar Lín.', combo: 'Ctrl+D',       type: 'key_event', code: 'KeyD',   mods: ['CTRL'] },
            { label: 'Borrar Línea',  combo: 'Ctrl+Y',       type: 'key_event', code: 'KeyY',   mods: ['CTRL'] },
            { label: 'Comentar',      combo: 'Ctrl+/',       type: 'key_event', code: 'Slash',  mods: ['CTRL'] },
            { label: 'Reformat',      combo: 'Ctrl+Alt+L',  type: 'key_event', code: 'KeyL',   mods: ['CTRL','ALT'] },
            { label: 'Run',           combo: 'Shift+F10',    type: 'key_event', code: 'F10',    mods: ['SHIFT'] },
            { label: 'Debug',         combo: 'Shift+F9',     type: 'key_event', code: 'F9',     mods: ['SHIFT'] },
            { label: 'Step Over',     combo: 'F8',           type: 'key_event', code: 'F8',     mods: [] },
            { label: 'Step Into',     combo: 'F7',           type: 'key_event', code: 'F7',     mods: [] },
            { label: 'Toggle BP',     combo: 'Ctrl+F8',      type: 'key_event', code: 'F8',     mods: ['CTRL'] },
            { label: 'Refactor',      combo: 'Ctrl+Alt+Shift+T', type: 'key_event', code: 'KeyT', mods: ['CTRL','ALT','SHIFT'] },
            { label: 'Surround With', combo: 'Ctrl+Alt+T',  type: 'key_event', code: 'KeyT',   mods: ['CTRL','ALT'] },
            { label: 'Extract Method',combo: 'Ctrl+Alt+M',  type: 'key_event', code: 'KeyM',   mods: ['CTRL','ALT'] },
            { label: 'Back',          combo: 'Ctrl+Alt+←',  type: 'key_event', code: 'ArrowLeft', mods: ['CTRL','ALT'] },
            { label: 'Forward',       combo: 'Ctrl+Alt+→',  type: 'key_event', code: 'ArrowRight', mods: ['CTRL','ALT'] },
        ]
    },
    {
        id: 'termux',
        label: '🖥️ Termux',
        shortcuts: [
            { label: 'Interrupt ✗',   combo: 'Ctrl+C',  type: 'key_event', code: 'KeyC', mods: ['CTRL'] },
            { label: 'EOF / Salir',   combo: 'Ctrl+D',  type: 'key_event', code: 'KeyD', mods: ['CTRL'] },
            { label: 'Suspender',     combo: 'Ctrl+Z',  type: 'key_event', code: 'KeyZ', mods: ['CTRL'] },
            { label: 'Limpiar',       combo: 'Ctrl+L',  type: 'key_event', code: 'KeyL', mods: ['CTRL'] },
            { label: 'Borra Línea',   combo: 'Ctrl+U',  type: 'key_event', code: 'KeyU', mods: ['CTRL'] },
            { label: 'Inicio Línea',  combo: 'Ctrl+A',  type: 'key_event', code: 'KeyA', mods: ['CTRL'] },
            { label: 'Fin Línea',     combo: 'Ctrl+E',  type: 'key_event', code: 'KeyE', mods: ['CTRL'] },
            { label: 'Buscar Hist.',  combo: 'Ctrl+R',  type: 'key_event', code: 'KeyR', mods: ['CTRL'] },
            { label: '← Palabra',    combo: 'Alt+B',   type: 'key_event', code: 'KeyB', mods: ['ALT'] },
            { label: '→ Palabra',    combo: 'Alt+F',   type: 'key_event', code: 'KeyF', mods: ['ALT'] },
            { label: 'Borra Palabra', combo: 'Ctrl+W',  type: 'key_event', code: 'KeyW', mods: ['CTRL'] },
            { label: 'Borra → Fin',  combo: 'Ctrl+K',  type: 'key_event', code: 'KeyK', mods: ['CTRL'] },
            { label: 'Reemplazar',    combo: 'Ctrl+T',  type: 'key_event', code: 'KeyT', mods: ['CTRL'] },
            { label: 'Tab Completo',  combo: 'Tab',     type: 'key',       key: 'TAB' },
            { label: '↑ Historial',  combo: '↑',       type: 'key_event', code: 'ArrowUp',   mods: [] },
            { label: '↓ Historial',  combo: '↓',       type: 'key_event', code: 'ArrowDown', mods: [] },
            { label: 'Pegar Clip',    combo: 'Ctrl+V',  type: 'key',       key: 'PASTE' },
        ]
    },
    {
        id: 'android',
        label: '📱 Android',
        shortcuts: [
            { label: 'Home',          combo: 'Meta',         type: 'key_event', code: 'MetaLeft',   mods: [] },
            { label: 'Back',          combo: 'Alt+←',        type: 'key_event', code: 'ArrowLeft',  mods: ['ALT'] },
            { label: 'Recientes',     combo: 'Meta+Tab',     type: 'key_event', code: 'Tab',        mods: ['META'] },
            { label: 'Cerrar App',    combo: 'Alt+F4',       type: 'key_event', code: 'F4',         mods: ['ALT'] },
            { label: 'Volumen ↑',    combo: 'Vol+',          type: 'key',       key: 'VOLUME_UP' },
            { label: 'Volumen ↓',    combo: 'Vol-',          type: 'key',       key: 'VOLUME_DOWN' },
            { label: 'Screenshot',    combo: 'Meta+Shift+S', type: 'key_event', code: 'KeyS',       mods: ['META','SHIFT'] },
            { label: 'Notifs.',       combo: 'Meta+N',       type: 'key_event', code: 'KeyN',       mods: ['META'] },
            { label: 'Buscar Global', combo: 'Meta+/',       type: 'key_event', code: 'Slash',      mods: ['META'] },
            { label: 'Caps Lock',     combo: 'CapsLock',     type: 'key_event', code: 'CapsLock',   mods: [] },
            { label: 'Enter',         combo: 'Enter',        type: 'key',       key: 'ENTER' },
            { label: 'Space',         combo: 'Space',        type: 'key',       key: 'SPACE' },
            { label: 'Escape',        combo: 'Esc',          type: 'key_event', code: 'Escape',     mods: [] },
        ]
    }
];
