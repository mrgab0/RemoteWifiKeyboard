import re

with open("app/src/main/java/com/remotekeyboard/RemoteInputMethodService.java", "r", encoding="utf-8") as f:
    content = f.read()

new_animate_key = """
    private void animateKey(String keyStr) {
        if (!isVisualKeyboardVisible || keyStr == null) return;
        String upper = keyStr.toUpperCase();
        if (upper.equals(" ")) upper = "SPACE";
        else if (upper.equals("CONTROL")) upper = "CTRL";
        else if (upper.equals("CONTEXTMENU")) upper = "MENU";
        else if (upper.equals("WIN") || upper.equals("SUPER") || upper.equals("OS")) upper = "META";
        else if (upper.equals("FUNCTION")) upper = "FN";
        
        Button btn = keyButtons.get(upper);
        if (btn != null) {
            mainHandler.post(() -> {
                int[] theme = THEMES[currentThemeIndex];
                btn.setBackgroundColor(theme[2]); // Pressed color
                btn.setTextColor(theme[0]);
                mainHandler.postDelayed(() -> {
                    btn.setBackgroundColor(theme[1]); // Normal color
                    btn.setTextColor(theme[3]);
                }, 100);
            });
        }
    }
"""

start_str = 'private void animateKey(String keyStr) {'
start_idx = content.find(start_str)
if start_idx != -1:
    end_idx = content.find('    }', start_idx)
    
    if end_idx != -1:
        end_idx += len('    }')
        content = content[:start_idx] + new_animate_key.strip() + "\n" + content[end_idx:]
        
        with open("app/src/main/java/com/remotekeyboard/RemoteInputMethodService.java", "w", encoding="utf-8") as f:
            f.write(content)
        print("Success")
    else:
        print("Could not find end of animateKey")
else:
    print("Could not find animateKey")
