import re
import sys

with open("app/src/main/java/com/remotekeyboard/RemoteInputMethodService.java", "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add fields
fields_str = """
    // --- Visual Keyboard & Clipboard ---
    private LinearLayout visualKeyboardContainer;
    private LinearLayout suggestionContainer;
    private Button pastePillButton;
    private int currentThemeIndex = 0;
    private boolean isVisualKeyboardVisible = true;
    private java.util.HashMap<String, Button> keyButtons = new java.util.HashMap<>();
    private String pcClipboardText = "";

    // Themes: {Bg, KeyBg, KeyPressed, Text}
    private final int[][] THEMES = {
        {Color.parseColor("#0B0D14"), Color.parseColor("#1E2337"), Color.parseColor("#818CF8"), Color.WHITE},
        {Color.parseColor("#000000"), Color.parseColor("#003300"), Color.parseColor("#00FF00"), Color.parseColor("#00FF00")},
        {Color.parseColor("#F5F5F7"), Color.parseColor("#FFFFFF"), Color.parseColor("#007AFF"), Color.BLACK},
        {Color.parseColor("#1A1B26"), Color.parseColor("#24283B"), Color.parseColor("#7AA2F7"), Color.parseColor("#C0CAF5")},
        {Color.parseColor("#282A36"), Color.parseColor("#44475A"), Color.parseColor("#FF79C6"), Color.parseColor("#F8F8F2")}
    };

    public static synchronized RemoteInputMethodService getInstance() {"""

content = content.replace("public static synchronized RemoteInputMethodService getInstance() {", fields_str, 1)

# 2. Modify setClipboardText
setClipboardText_match = re.search(r'public void setClipboardText\(String text\) \{.*?\}\n', content, re.DOTALL)
if setClipboardText_match:
    new_setClipboardText = """    public void setClipboardText(String text) {
        mainHandler.post(() -> {
            pcClipboardText = text;
            showLiveFeedback("📋 Texto PC recibido. Toca Pegar.");
            updatePastePill();
        });
    }
"""
    content = content.replace(setClipboardText_match.group(0), new_setClipboardText, 1)

# 3. Modify onCreateInputView
oncreateinputview_match = re.search(r'@Override\s+public View onCreateInputView\(\) \{.*?\n    \}\n', content, re.DOTALL)
if oncreateinputview_match:
    new_oncreateinputview = """@Override
    public View onCreateInputView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#0B0D14"));
        layout.setPadding(16, 16, 16, 16);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        suggestionContainer = new LinearLayout(this);
        suggestionContainer.setOrientation(LinearLayout.HORIZONTAL);
        suggestionContainer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams suggParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        suggParams.setMargins(0, 10, 0, 10);
        
        Button btnToggleKb = new Button(this);
        btnToggleKb.setText("👁️");
        btnToggleKb.setTextSize(14);
        btnToggleKb.setPadding(0,0,0,0);
        btnToggleKb.setOnClickListener(v -> {
            isVisualKeyboardVisible = !isVisualKeyboardVisible;
            if (visualKeyboardContainer != null) {
                visualKeyboardContainer.setVisibility(isVisualKeyboardVisible ? View.VISIBLE : View.GONE);
            }
        });
        
        Button btnTheme = new Button(this);
        btnTheme.setText("🎨");
        btnTheme.setTextSize(14);
        btnTheme.setPadding(0,0,0,0);
        btnTheme.setOnClickListener(v -> {
            currentThemeIndex = (currentThemeIndex + 1) % THEMES.length;
            applyTheme(layout);
        });

        pastePillButton = new Button(this);
        pastePillButton.setVisibility(View.GONE);
        pastePillButton.setTextSize(12);
        pastePillButton.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null && pcClipboardText != null && !pcClipboardText.isEmpty()) {
                ic.commitText(pcClipboardText, 1);
                pcClipboardText = "";
                updatePastePill();
                performHapticFeedbackAsync();
            }
        });
        
        suggestionContainer.addView(btnToggleKb);
        suggestionContainer.addView(btnTheme);
        suggestionContainer.addView(pastePillButton);
        
        layout.addView(suggestionContainer, suggParams);

        visualKeyboardContainer = new LinearLayout(this);
        visualKeyboardContainer.setOrientation(LinearLayout.VERTICAL);
        buildVisualKeyboard();
        layout.addView(visualKeyboardContainer);

        tvLiveFeedback = new TextView(this);
        tvLiveFeedback.setText("🟢 Listo para recibir teclas...");
        tvLiveFeedback.setTextColor(Color.parseColor("#34D399"));
        tvLiveFeedback.setTextSize(12);
        tvLiveFeedback.setGravity(Gravity.CENTER);
        tvLiveFeedback.setPadding(0, 4, 0, 4);

        tvMetrics = new TextView(this);
        tvMetrics.setText("Pulsaciones: " + totalKeysTyped.get());
        tvMetrics.setTextColor(Color.parseColor("#64748B"));
        tvMetrics.setTextSize(10);
        tvMetrics.setGravity(Gravity.CENTER);
        tvMetrics.setPadding(0, 0, 0, 8);

        layout.addView(tvLiveFeedback);
        layout.addView(tvMetrics);

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER);

        Button btnSwitch = new Button(this);
        btnSwitch.setText("🌐 Cambiar Teclado");
        btnSwitch.setTextColor(Color.WHITE);
        btnSwitch.setBackgroundColor(Color.parseColor("#1E2337"));
        btnSwitch.setTextSize(11);
        btnSwitch.setOnClickListener(v -> {
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
            } catch (Exception ignored) {}
        });

        Button btnSettings = new Button(this);
        btnSettings.setText("⚙️ Panel");
        btnSettings.setTextColor(Color.WHITE);
        btnSettings.setBackgroundColor(Color.parseColor("#1E2337"));
        btnSettings.setTextSize(11);
        btnSettings.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(RemoteInputMethodService.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception ignored) {}
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(6, 0, 6, 0);

        buttonRow.addView(btnSwitch, params);
        buttonRow.addView(btnSettings, params);
        layout.addView(buttonRow);
        
        applyTheme(layout);

        return layout;
    }
"""
    content = content.replace(oncreateinputview_match.group(0), new_oncreateinputview, 1)

# 4. Insert new helper methods after onCreateInputView
helpers = """
    private void updatePastePill() {
        mainHandler.post(() -> {
            if (pastePillButton != null) {
                if (pcClipboardText != null && !pcClipboardText.isEmpty()) {
                    String preview = pcClipboardText.length() > 15 ? pcClipboardText.substring(0, 15) + "..." : pcClipboardText;
                    pastePillButton.setText("📋 " + preview);
                    pastePillButton.setVisibility(View.VISIBLE);
                } else {
                    pastePillButton.setVisibility(View.GONE);
                }
            }
        });
    }

    private void buildVisualKeyboard() {
        visualKeyboardContainer.removeAllViews();
        keyButtons.clear();
        String[] rows = {"QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM"};
        for (String rowStr : rows) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            for (int i = 0; i < rowStr.length(); i++) {
                String key = String.valueOf(rowStr.charAt(i));
                Button btn = new Button(this);
                btn.setText(key);
                btn.setPadding(0, 0, 0, 0);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 110, 1.0f);
                lp.setMargins(2, 2, 2, 2);
                btn.setLayoutParams(lp);
                btn.setOnClickListener(v -> typeText(key));
                keyButtons.put(key, btn);
                row.addView(btn);
            }
            visualKeyboardContainer.addView(row);
        }
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        Button spaceBtn = new Button(this);
        spaceBtn.setText("SPACE");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 110, 1.0f);
        lp.setMargins(2, 2, 2, 2);
        spaceBtn.setLayoutParams(lp);
        spaceBtn.setOnClickListener(v -> typeText(" "));
        keyButtons.put("SPACE", spaceBtn);
        row.addView(spaceBtn);
        visualKeyboardContainer.addView(row);
    }

    private void applyTheme(View rootLayout) {
        int[] theme = THEMES[currentThemeIndex];
        int bgColor = theme[0];
        int keyBgColor = theme[1];
        int textColor = theme[3];
        
        rootLayout.setBackgroundColor(bgColor);
        if (visualKeyboardContainer != null) {
            visualKeyboardContainer.setBackgroundColor(bgColor);
        }
        
        for (Button btn : keyButtons.values()) {
            btn.setBackgroundColor(keyBgColor);
            btn.setTextColor(textColor);
        }
        
        if (pastePillButton != null) {
            pastePillButton.setBackgroundColor(theme[2]); 
            pastePillButton.setTextColor(theme[0]);
        }
    }

    private void animateKey(String keyStr) {
        if (!isVisualKeyboardVisible || keyStr == null) return;
        String upper = keyStr.toUpperCase();
        if (upper.equals(" ")) upper = "SPACE";
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
content = content.replace("    @Override\n    public void onStartInput(EditorInfo attribute, boolean restarting) {", helpers + "\n    @Override\n    public void onStartInput(EditorInfo attribute, boolean restarting) {", 1)

# 5. Insert animateKey calls
content = content.replace("totalKeysTyped.addAndGet(text.length());", "totalKeysTyped.addAndGet(text.length());\n        animateKey(text);", 1)
content = content.replace("totalKeysTyped.incrementAndGet();", "totalKeysTyped.incrementAndGet();\n        animateKey(key);", 1)

content = content.replace("final int keyCode = mapWebCodeToAndroidKeyCode(code, key);", "final int keyCode = mapWebCodeToAndroidKeyCode(code, key);\n        if (androidAction == KeyEvent.ACTION_DOWN) animateKey(key);", 1)

with open("app/src/main/java/com/remotekeyboard/RemoteInputMethodService.java", "w", encoding="utf-8") as f:
    f.write(content)

print("Patch applied successfully.")
