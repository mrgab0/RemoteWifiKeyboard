use windows_sys::Win32::UI::Input::KeyboardAndMouse::{
    SendInput, INPUT, INPUT_0, INPUT_KEYBOARD, KEYBDINPUT,
    KEYEVENTF_KEYUP, KEYEVENTF_UNICODE, KEYEVENTF_EXTENDEDKEY,
    VK_BACK, VK_CONTROL, VK_DELETE, VK_DOWN, VK_END, VK_ESCAPE, VK_F1, VK_F10,
    VK_F11, VK_F12, VK_F2, VK_F3, VK_F4, VK_F5, VK_F6, VK_F7, VK_F8, VK_F9,
    VK_HOME, VK_LEFT, VK_LWIN, VK_MENU, VK_NEXT, VK_PRIOR, VK_RETURN, VK_RIGHT,
    VK_SPACE, VK_TAB, VK_UP, VK_VOLUME_DOWN, VK_VOLUME_MUTE, VK_VOLUME_UP,
    VK_MEDIA_PLAY_PAUSE, VK_MEDIA_NEXT_TRACK, VK_MEDIA_PREV_TRACK,
};
use std::mem::size_of;

/// Envía un caracter o texto completo simulando pulsaciones Unicode de Windows
pub fn type_text(text: &str) {
    for c in text.encode_utf16() {
        send_unicode_char(c);
    }
}

fn send_unicode_char(utf16_char: u16) {
    unsafe {
        let mut inputs: [INPUT; 2] = [
            INPUT {
                r#type: INPUT_KEYBOARD,
                Anonymous: INPUT_0 {
                    ki: KEYBDINPUT {
                        wVk: 0,
                        wScan: utf16_char,
                        dwFlags: KEYEVENTF_UNICODE,
                        time: 0,
                        dwExtraInfo: 0,
                    },
                },
            },
            INPUT {
                r#type: INPUT_KEYBOARD,
                Anonymous: INPUT_0 {
                    ki: KEYBDINPUT {
                        wVk: 0,
                        wScan: utf16_char,
                        dwFlags: KEYEVENTF_UNICODE | KEYEVENTF_KEYUP,
                        time: 0,
                        dwExtraInfo: 0,
                    },
                },
            },
        ];

        SendInput(
            inputs.len() as u32,
            inputs.as_mut_ptr(),
            size_of::<INPUT>() as i32,
        );
    }
}

/// Envía una tecla virtual específica (VK_*)
pub fn send_virtual_key(vk: u16, is_extended: bool) {
    let ext_flag = if is_extended { KEYEVENTF_EXTENDEDKEY } else { 0 };
    unsafe {
        let mut inputs: [INPUT; 2] = [
            INPUT {
                r#type: INPUT_KEYBOARD,
                Anonymous: INPUT_0 {
                    ki: KEYBDINPUT {
                        wVk: vk,
                        wScan: 0,
                        dwFlags: ext_flag,
                        time: 0,
                        dwExtraInfo: 0,
                    },
                },
            },
            INPUT {
                r#type: INPUT_KEYBOARD,
                Anonymous: INPUT_0 {
                    ki: KEYBDINPUT {
                        wVk: vk,
                        wScan: 0,
                        dwFlags: ext_flag | KEYEVENTF_KEYUP,
                        time: 0,
                        dwExtraInfo: 0,
                    },
                },
            },
        ];

        SendInput(
            inputs.len() as u32,
            inputs.as_mut_ptr(),
            size_of::<INPUT>() as i32,
        );
    }
}

/// Envía combinaciones de teclas con modificadores (ej. Ctrl+C, Ctrl+V, Alt+Tab, Win+D)
pub fn send_key_combination(modifiers: &[u16], target_vk: u16) {
    unsafe {
        let mut inputs = Vec::new();

        // 1. Presionar modificadores
        for &mod_vk in modifiers {
            inputs.push(INPUT {
                r#type: INPUT_KEYBOARD,
                Anonymous: INPUT_0 {
                    ki: KEYBDINPUT {
                        wVk: mod_vk,
                        wScan: 0,
                        dwFlags: 0,
                        time: 0,
                        dwExtraInfo: 0,
                    },
                },
            });
        }

        // 2. Presionar tecla objetivo
        inputs.push(INPUT {
            r#type: INPUT_KEYBOARD,
            Anonymous: INPUT_0 {
                ki: KEYBDINPUT {
                    wVk: target_vk,
                    wScan: 0,
                    dwFlags: 0,
                    time: 0,
                    dwExtraInfo: 0,
                },
            },
        });

        // 3. Soltar tecla objetivo
        inputs.push(INPUT {
            r#type: INPUT_KEYBOARD,
            Anonymous: INPUT_0 {
                ki: KEYBDINPUT {
                    wVk: target_vk,
                    wScan: 0,
                    dwFlags: KEYEVENTF_KEYUP,
                    time: 0,
                    dwExtraInfo: 0,
                },
            },
        });

        // 4. Soltar modificadores en orden inverso
        for &mod_vk in modifiers.iter().rev() {
            inputs.push(INPUT {
                r#type: INPUT_KEYBOARD,
                Anonymous: INPUT_0 {
                    ki: KEYBDINPUT {
                        wVk: mod_vk,
                        wScan: 0,
                        dwFlags: KEYEVENTF_KEYUP,
                        time: 0,
                        dwExtraInfo: 0,
                    },
                },
            });
        }

        SendInput(
            inputs.len() as u32,
            inputs.as_mut_ptr(),
            size_of::<INPUT>() as i32,
        );
    }
}

/// Mapea nombres de tecla comunes desde la app móvil a pulsaciones de Windows
pub fn handle_key_command(key_name: &str) {
    let lower = key_name.to_lowercase();
    match lower.as_str() {
        "enter" | "return" => send_virtual_key(VK_RETURN, false),
        "backspace" | "back" => send_virtual_key(VK_BACK, false),
        "tab" => send_virtual_key(VK_TAB, false),
        "escape" | "esc" => send_virtual_key(VK_ESCAPE, false),
        "space" => send_virtual_key(VK_SPACE, false),
        "arrowup" | "up" => send_virtual_key(VK_UP, true),
        "arrowdown" | "down" => send_virtual_key(VK_DOWN, true),
        "arrowleft" | "left" => send_virtual_key(VK_LEFT, true),
        "arrowright" | "right" => send_virtual_key(VK_RIGHT, true),
        "delete" | "del" => send_virtual_key(VK_DELETE, true),
        "home" => send_virtual_key(VK_HOME, true),
        "end" => send_virtual_key(VK_END, true),
        "pageup" => send_virtual_key(VK_PRIOR, true),
        "pagedown" => send_virtual_key(VK_NEXT, true),
        
        // Teclas de Función F1 - F12
        "f1" => send_virtual_key(VK_F1, false),
        "f2" => send_virtual_key(VK_F2, false),
        "f3" => send_virtual_key(VK_F3, false),
        "f4" => send_virtual_key(VK_F4, false),
        "f5" => send_virtual_key(VK_F5, false),
        "f6" => send_virtual_key(VK_F6, false),
        "f7" => send_virtual_key(VK_F7, false),
        "f8" => send_virtual_key(VK_F8, false),
        "f9" => send_virtual_key(VK_F9, false),
        "f10" => send_virtual_key(VK_F10, false),
        "f11" => send_virtual_key(VK_F11, false),
        "f12" => send_virtual_key(VK_F12, false),

        // Control de Volumen y Multimedia
        "volumeup" | "volume_up" => send_virtual_key(VK_VOLUME_UP, true),
        "volumedown" | "volume_down" => send_virtual_key(VK_VOLUME_DOWN, true),
        "volumemute" | "volume_mute" => send_virtual_key(VK_VOLUME_MUTE, true),
        "playpause" | "play_pause" => send_virtual_key(VK_MEDIA_PLAY_PAUSE, true),
        "nexttrack" | "next_track" => send_virtual_key(VK_MEDIA_NEXT_TRACK, true),
        "prevtrack" | "prev_track" => send_virtual_key(VK_MEDIA_PREV_TRACK, true),

        // Atajos comunes
        "ctrl+c" | "copy" => send_key_combination(&[VK_CONTROL], 0x43), // 'C'
        "ctrl+v" | "paste" => send_key_combination(&[VK_CONTROL], 0x56), // 'V'
        "ctrl+x" | "cut" => send_key_combination(&[VK_CONTROL], 0x58), // 'X'
        "ctrl+z" | "undo" => send_key_combination(&[VK_CONTROL], 0x5A), // 'Z'
        "ctrl+a" | "select_all" => send_key_combination(&[VK_CONTROL], 0x41), // 'A'
        "ctrl+s" | "save" => send_key_combination(&[VK_CONTROL], 0x53), // 'S'
        "alt+f4" | "close_window" => send_key_combination(&[VK_MENU], VK_F4),
        "win+d" | "show_desktop" => send_key_combination(&[VK_LWIN], 0x44), // 'D'

        // Si es un solo carácter no mapeado, escribirlo
        _ => {
            if key_name.chars().count() == 1 {
                type_text(key_name);
            }
        }
    }
}
