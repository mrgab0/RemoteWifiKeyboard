import re

with open("app/src/main/java/com/remotekeyboard/RemoteInputMethodService.java", "r", encoding="utf-8") as f:
    content = f.read()

new_space_row = """
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        
        String[] bottomKeys = {"CTRL", "META", "ALT", "SPACE", "FN", "MENU"};
        float[] weights = {1.2f, 1.0f, 1.0f, 4.0f, 1.0f, 1.2f};
        
        for (int i = 0; i < bottomKeys.length; i++) {
            String key = bottomKeys[i];
            Button btn = new Button(this);
            btn.setText(key.equals("META") ? "WIN" : (key.equals("MENU") ? "CTX" : key));
            btn.setPadding(0, 0, 0, 0);
            btn.setTextSize(10);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 110, weights[i]);
            lp.setMargins(2, 2, 2, 2);
            btn.setLayoutParams(lp);
            
            btn.setOnClickListener(v -> {
                if (key.equals("SPACE")) {
                    typeText(" ");
                } else {
                    sendSpecialKey(key);
                }
            });
            
            keyButtons.put(key, btn);
            row.addView(btn);
        }
        visualKeyboardContainer.addView(row);
"""

start_str = 'LinearLayout row = new LinearLayout(this);\n        row.setOrientation(LinearLayout.HORIZONTAL);\n        row.setGravity(Gravity.CENTER);\n        Button spaceBtn = new Button(this);'
start_idx = content.find('LinearLayout row = new LinearLayout(this);\n        row.setOrientation(LinearLayout.HORIZONTAL);\n        row.setGravity(Gravity.CENTER);\n        Button spaceBtn = new Button(this);')
end_idx = content.find('visualKeyboardContainer.addView(row);\n    }', start_idx)

if start_idx != -1 and end_idx != -1:
    end_idx += len('visualKeyboardContainer.addView(row);')
    content = content[:start_idx] + new_space_row.strip() + content[end_idx:]
    
    with open("app/src/main/java/com/remotekeyboard/RemoteInputMethodService.java", "w", encoding="utf-8") as f:
        f.write(content)
    print("Success")
else:
    print("Could not find block. start_idx:", start_idx, "end_idx:", end_idx)
