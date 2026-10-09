package com.remotekeyboard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.remotekeyboard.server.RemoteWebServerManager;
import com.remotekeyboard.server.RemoteWebServer;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ClipDescription;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class RemoteInputMethodService extends InputMethodService {

    private static final String TAG = "RemoteIME";
    private static final String PREFS_NAME = "RemoteKeyboardPrefs";
    private static final String KEY_VIBRATION = "vibration_enabled";

    private static RemoteInputMethodService instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService asyncExecutor = Executors.newSingleThreadExecutor();
    private final AtomicLong totalKeysTyped = new AtomicLong(0);

    private TextView tvLiveFeedback;
    private TextView tvMetrics;
    private Vibrator vibrator;
    private SharedPreferences prefs;
    private android.os.PowerManager.WakeLock typingWakeLock;
    private android.net.wifi.WifiManager.WifiLock wifiLock;

    // --- Portapapeles M�gico ---
    private ClipboardManager clipboardManager;
    private ClipboardManager.OnPrimaryClipChangedListener clipListener;
    private String lastClipboardText = "";

    // --- UI Visual Keyboard & Themes ---
    private LinearLayout mainLayout;
    private android.widget.HorizontalScrollView suggestionBar;
    private Button clipboardPill;
    private LinearLayout visualKeyboardContainer;
    private TextView tvTitle;
    private Button btnSwitch;
    private Button btnSettings;
    private Button btnToggleKb;
    private java.util.Map<String, TextView> keyViewsMap = new java.util.HashMap<>();
    private java.util.Map<Integer, TextView> keyCodeViewsMap = new java.util.HashMap<>();
    private boolean isVisualKeyboardVisible = false;
    private int currentThemeIndex = 0;

    private static class ThemeInfo {
        int bg, keyBg, text, highlight;
        ThemeInfo(String bg, String keyBg, String text, String highlight) {
            this.bg = Color.parseColor(bg);
            this.keyBg = Color.parseColor(keyBg);
            this.text = Color.parseColor(text);
            this.highlight = Color.parseColor(highlight);
        }
    }
    
    private final ThemeInfo[] THEMES = new ThemeInfo[]{
        new ThemeInfo("#0B0D14", "#1E2337", "#818CF8", "#34D399"), // 0: Dark Hacker
        new ThemeInfo("#000000", "#0D220D", "#00FF00", "#00FF00"), // 1: Neon Matrix
        new ThemeInfo("#F0F0F0", "#FFFFFF", "#333333", "#007AFF"), // 2: Light Mac
        new ThemeInfo("#1A1B26", "#24283B", "#7DCFFF", "#BB9AF7"), // 3: Tokyo Night
        new ThemeInfo("#282A36", "#44475A", "#F8F8F2", "#FF79C6")  // 4: Dracula
    };

    public static synchronized RemoteInputMethodService getInstance() {
        return instance;
    }

    public long getTotalKeysTyped() {
        return totalKeysTyped.get();
    }

    public boolean isConnectedToEditor() {
        return getCurrentInputConnection() != null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DebugLogger.init(this);
        DebugLogger.log("RemoteInputMethodService onCreate iniciado.");
        instance = this;
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception ignored) {}
        try {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                // ACQUIRE_CAUSES_WAKEUP enciende/avisa a la pantalla, ON_AFTER_RELEASE resetea el timeout de apagado
                typingWakeLock = pm.newWakeLock(android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK | android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP | android.os.PowerManager.ON_AFTER_RELEASE, "RemoteIME:TypingWakeLock");
                typingWakeLock.setReferenceCounted(false);
            }
        } catch (Exception ignored) {}
        try {
            android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                int wifiMode = android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF;
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    wifiMode = 4; // WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                }
                wifiLock = wm.createWifiLock(wifiMode, "RemoteIME:WifiLock");
                wifiLock.setReferenceCounted(false);
                wifiLock.acquire();
                DebugLogger.log("WifiLock adquirido en modo alto rendimiento/baja latencia");
            }
        } catch (Exception ignored) {}
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipListener = new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                if (clipboardManager != null && clipboardManager.hasPrimaryClip()) {
                    ClipData clip = clipboardManager.getPrimaryClip();
                    if (clip != null && clip.getItemCount() > 0) {
                        if (clip.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || clip.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
                            CharSequence text = clip.getItemAt(0).getText();
                            if (text != null) {
                                String str = text.toString();
                                if (!str.equals(lastClipboardText)) {
                                    lastClipboardText = str;
                                    updateClipboardPill(str);
                                    RemoteWebServer server = RemoteWebServerManager.getServerInstance();
                                    if (server != null) {
                                        try {
                                            JSONObject json = new JSONObject();
                                            json.put("type", "clipboard_sync");
                                            json.put("contentType", "text");
                                            json.put("content", str);
                                            server.broadcastWebSocket(json.toString());
                                            DebugLogger.log("Portapapeles texto enviado a PC");
                                        } catch (Exception e) {}
                                    }
                                }
                            }
                        } else if (clip.getItemAt(0).getUri() != null) {
                            android.net.Uri uri = clip.getItemAt(0).getUri();
                            asyncExecutor.execute(() -> {
                                try {
                                    java.io.InputStream is = getContentResolver().openInputStream(uri);
                                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
                                    if (bitmap != null) {
                                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos);
                                        byte[] bytes = baos.toByteArray();
                                        String base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
                                        String dataUrl = "data:image/png;base64," + base64;
                                        RemoteWebServer server = RemoteWebServerManager.getServerInstance();
                                        if (server != null) {
                                            JSONObject json = new JSONObject();
                                            json.put("type", "clipboard_sync");
                                            json.put("contentType", "image");
                                            json.put("content", dataUrl);
                                            server.broadcastWebSocket(json.toString());
                                            DebugLogger.log("Portapapeles imagen enviada a PC");
                                        }
                                    }
                                } catch (Exception e) {}
                            });
                        }
                    }
                }
            }
        };
        clipboardManager.addPrimaryClipChangedListener(clipListener);

        try {
            Intent serviceIntent = new Intent(this, com.remotekeyboard.server.ConnectionService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {}
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        asyncExecutor.shutdownNow();
        if (clipboardManager != null && clipListener != null) {
            clipboardManager.removePrimaryClipChangedListener(clipListener);
        }
        if (wifiLock != null && wifiLock.isHeld()) {
            try { wifiLock.release(); } catch (Exception ignored) {}
        }
        if (typingWakeLock != null && typingWakeLock.isHeld()) {
            try { typingWakeLock.release(); } catch (Exception ignored) {}
        }
    }

    public void setClipboardText(String text) {
        mainHandler.post(() -> {
            lastClipboardText = text;
            if (clipboardManager != null) {
                ClipData clip = ClipData.newPlainText("PC Sync", text);
                clipboardManager.setPrimaryClip(clip);
                showLiveFeedback("📋 Texto del PC copiado");
                updateClipboardPill(text);
            }
        });
    }

    public void setClipboardImage(String dataUrl) {
        asyncExecutor.execute(() -> {
            try {
                String base64 = dataUrl.substring(dataUrl.indexOf(",") + 1);
                byte[] bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                java.io.File cacheDir = new java.io.File(getCacheDir(), "clipboard_images");
                if (!cacheDir.exists()) cacheDir.mkdirs();
                java.io.File imgFile = new java.io.File(cacheDir, "shared_image.png");
                java.io.FileOutputStream fos = new java.io.FileOutputStream(imgFile);
                fos.write(bytes);
                fos.close();

                android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        getApplicationContext().getPackageName() + ".provider",
                        imgFile);

                mainHandler.post(() -> {
                    if (clipboardManager != null) {
                        ClipData clip = ClipData.newUri(getContentResolver(), "Image from PC", uri);
                        clipboardManager.setPrimaryClip(clip);
                        showLiveFeedback("?? Imagen del PC copiada");
                    }
                });
            } catch (Exception e) {}
        });
    }

        private void showLiveFeedback(final String msg) {
        mainHandler.post(() -> {
            if (tvLiveFeedback != null) {
                tvLiveFeedback.setText(msg);
                tvLiveFeedback.setTextColor(Color.parseColor("#34D399"));
            }
        });
    }

    @Override
    public View onCreateInputView() {
        if (prefs == null) prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentThemeIndex = prefs.getInt("theme_index", 0);
        isVisualKeyboardVisible = prefs.getBoolean("visual_keyboard_visible", true);

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(0, 8, 0, 8);

        // 1. Suggestion Bar
        suggestionBar = new android.widget.HorizontalScrollView(this);
        suggestionBar.setHorizontalScrollBarEnabled(false);
        LinearLayout suggestionContainer = new LinearLayout(this);
        suggestionContainer.setOrientation(LinearLayout.HORIZONTAL);
        suggestionContainer.setGravity(Gravity.CENTER_VERTICAL);
        suggestionContainer.setPadding(24, 0, 24, 8);
        
        clipboardPill = new Button(this);
        clipboardPill.setText("📋 Pegar");
        clipboardPill.setTextSize(12);
        clipboardPill.setPadding(32, 16, 32, 16);
        clipboardPill.setVisibility(View.GONE);
        clipboardPill.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null && lastClipboardText != null) {
                ic.commitText(lastClipboardText, 1);
                clipboardPill.setVisibility(View.GONE);
            }
        });
        
        LinearLayout.LayoutParams pillParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        suggestionContainer.addView(clipboardPill, pillParams);
        suggestionBar.addView(suggestionContainer);
        mainLayout.addView(suggestionBar);

        // 2. Classic Panel
        LinearLayout classicPanel = new LinearLayout(this);
        classicPanel.setOrientation(LinearLayout.VERTICAL);
        classicPanel.setPadding(24, 8, 24, 16);
        classicPanel.setGravity(Gravity.CENTER_HORIZONTAL);

        tvTitle = new TextView(this);
        tvTitle.setText("⌨️ Remote WiFi Keyboard • Ultrabaja Latencia");
        tvTitle.setTextSize(13);
        tvTitle.setGravity(Gravity.CENTER);

        tvLiveFeedback = new TextView(this);
        tvLiveFeedback.setText("🟢 Listo para recibir teclas...");
        tvLiveFeedback.setTextSize(12);
        tvLiveFeedback.setGravity(Gravity.CENTER);
        tvLiveFeedback.setPadding(0, 4, 0, 4);

        tvMetrics = new TextView(this);
        tvMetrics.setText("Pulsaciones: " + totalKeysTyped.get());
        tvMetrics.setTextSize(10);
        tvMetrics.setGravity(Gravity.CENTER);
        tvMetrics.setPadding(0, 0, 0, 8);

        classicPanel.addView(tvTitle);
        classicPanel.addView(tvLiveFeedback);
        classicPanel.addView(tvMetrics);

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER);

        btnSwitch = new Button(this);
        btnSwitch.setText("🌐 Cambiar");
        btnSwitch.setTextSize(11);
        btnSwitch.setOnClickListener(v -> {
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
            } catch (Exception ignored) {}
        });

        btnSettings = new Button(this);
        btnSettings.setText("⚙️ Panel");
        btnSettings.setTextSize(11);
        btnSettings.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(RemoteInputMethodService.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception ignored) {}
        });

        btnToggleKb = new Button(this);
        btnToggleKb.setText(isVisualKeyboardVisible ? "🔽 Ocultar" : "⌨️ Mostrar");
        btnToggleKb.setTextSize(11);
        btnToggleKb.setOnClickListener(v -> {
            isVisualKeyboardVisible = !isVisualKeyboardVisible;
            prefs.edit().putBoolean("visual_keyboard_visible", isVisualKeyboardVisible).apply();
            btnToggleKb.setText(isVisualKeyboardVisible ? "🔽 Ocultar" : "⌨️ Mostrar");
            visualKeyboardContainer.setVisibility(isVisualKeyboardVisible ? View.VISIBLE : View.GONE);
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnParams.setMargins(4, 0, 4, 0);

        buttonRow.addView(btnSwitch, btnParams);
        buttonRow.addView(btnSettings, btnParams);
        buttonRow.addView(btnToggleKb, btnParams);
        classicPanel.addView(buttonRow);
        
        mainLayout.addView(classicPanel);

        // 3. Visual Keyboard
        visualKeyboardContainer = new LinearLayout(this);
        visualKeyboardContainer.setOrientation(LinearLayout.VERTICAL);
        visualKeyboardContainer.setPadding(8, 0, 8, 8);
        visualKeyboardContainer.setVisibility(isVisualKeyboardVisible ? View.VISIBLE : View.GONE);
        
        buildVisualKeyboardGrid();
        mainLayout.addView(visualKeyboardContainer);

        applyTheme();
        return mainLayout;
    }

    private void buildVisualKeyboardGrid() {
        keyViewsMap.clear();
        keyCodeViewsMap.clear();
        
        String[][] rows = {
            {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"},
            {"A", "S", "D", "F", "G", "H", "J", "K", "L"},
            {"SHIFT", "Z", "X", "C", "V", "B", "N", "M", "DEL"},
            {"?123", ",", "SPACE", ".", "ENTER"}
        };
        
        for (String[] rowKeys : rows) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER);
            
            for (String keyStr : rowKeys) {
                TextView keyView = new TextView(this);
                keyView.setText(keyStr);
                keyView.setGravity(Gravity.CENTER);
                keyView.setTextSize(16);
                
                // Set fixed height and weight for width
                LinearLayout.LayoutParams params;
                if ("SPACE".equals(keyStr)) {
                    params = new LinearLayout.LayoutParams(0, 120, 4f);
                } else if ("SHIFT".equals(keyStr) || "DEL".equals(keyStr) || "ENTER".equals(keyStr) || "?123".equals(keyStr)) {
                    params = new LinearLayout.LayoutParams(0, 120, 1.5f);
                } else {
                    params = new LinearLayout.LayoutParams(0, 120, 1f);
                }
                params.setMargins(4, 4, 4, 4);
                keyView.setLayoutParams(params);
                
                keyViewsMap.put(keyStr.toUpperCase(), keyView);
                rowLayout.addView(keyView);
            }
            visualKeyboardContainer.addView(rowLayout);
        }
        
        // Map common keycodes to the visual keys for handleRawKeyEvent
        keyCodeViewsMap.put(KeyEvent.KEYCODE_DEL, keyViewsMap.get("DEL"));
        keyCodeViewsMap.put(KeyEvent.KEYCODE_ENTER, keyViewsMap.get("ENTER"));
        keyCodeViewsMap.put(KeyEvent.KEYCODE_SPACE, keyViewsMap.get("SPACE"));
        keyCodeViewsMap.put(KeyEvent.KEYCODE_SHIFT_LEFT, keyViewsMap.get("SHIFT"));
        keyCodeViewsMap.put(KeyEvent.KEYCODE_SHIFT_RIGHT, keyViewsMap.get("SHIFT"));
    }

    public void setThemeIndex(int index) {
        if (index >= 0 && index < THEMES.length) {
            currentThemeIndex = index;
            if (prefs != null) {
                prefs.edit().putInt("theme_index", index).apply();
            }
            mainHandler.post(this::applyTheme);
        }
    }

    public void applyTheme() {
        if (currentThemeIndex < 0 || currentThemeIndex >= THEMES.length) currentThemeIndex = 0;
        ThemeInfo theme = THEMES[currentThemeIndex];

        if (mainLayout != null) {
            mainLayout.setBackgroundColor(theme.bg);
            tvTitle.setTextColor(theme.text);
            tvMetrics.setTextColor(theme.text);
            tvLiveFeedback.setTextColor(theme.highlight);
            
            clipboardPill.setBackgroundColor(theme.keyBg);
            clipboardPill.setTextColor(theme.highlight);

            btnSwitch.setBackgroundColor(theme.keyBg);
            btnSwitch.setTextColor(theme.text);
            btnSettings.setBackgroundColor(theme.keyBg);
            btnSettings.setTextColor(theme.text);
            btnToggleKb.setBackgroundColor(theme.keyBg);
            btnToggleKb.setTextColor(theme.text);
            
            for (TextView keyView : keyViewsMap.values()) {
                keyView.setBackgroundColor(theme.keyBg);
                keyView.setTextColor(theme.text);
            }
        }
    }

    private void updateClipboardPill(final String text) {
        mainHandler.post(() -> {
            if (clipboardPill != null) {
                lastClipboardText = text;
                String snippet = text.length() > 15 ? text.substring(0, 15) + "..." : text;
                clipboardPill.setText("📋 " + snippet);
                clipboardPill.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        instance = this;
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipListener = new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                if (clipboardManager != null && clipboardManager.hasPrimaryClip()) {
                    ClipData clip = clipboardManager.getPrimaryClip();
                    if (clip != null && clip.getItemCount() > 0) {
                        if (clip.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || clip.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
                            CharSequence text = clip.getItemAt(0).getText();
                            if (text != null) {
                                String str = text.toString();
                                if (!str.equals(lastClipboardText)) {
                                    lastClipboardText = str;
                                    updateClipboardPill(str);
                                    RemoteWebServer server = RemoteWebServerManager.getServerInstance();
                                    if (server != null) {
                                        try {
                                            JSONObject json = new JSONObject();
                                            json.put("type", "clipboard_sync");
                                            json.put("contentType", "text");
                                            json.put("content", str);
                                            server.broadcastWebSocket(json.toString());
                                            DebugLogger.log("Portapapeles texto enviado a PC");
                                        } catch (Exception e) {}
                                    }
                                }
                            }
                        } else if (clip.getItemAt(0).getUri() != null) {
                            android.net.Uri uri = clip.getItemAt(0).getUri();
                            asyncExecutor.execute(() -> {
                                try {
                                    java.io.InputStream is = getContentResolver().openInputStream(uri);
                                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
                                    if (bitmap != null) {
                                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos);
                                        byte[] bytes = baos.toByteArray();
                                        String base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
                                        String dataUrl = "data:image/png;base64," + base64;
                                        RemoteWebServer server = RemoteWebServerManager.getServerInstance();
                                        if (server != null) {
                                            JSONObject json = new JSONObject();
                                            json.put("type", "clipboard_sync");
                                            json.put("contentType", "image");
                                            json.put("content", dataUrl);
                                            server.broadcastWebSocket(json.toString());
                                            DebugLogger.log("Portapapeles imagen enviada a PC");
                                        }
                                    }
                                } catch (Exception e) {}
                            });
                        }
                    }
                }
            }
        };
        clipboardManager.addPrimaryClipChangedListener(clipListener);

        try {
            Intent serviceIntent = new Intent(this, com.remotekeyboard.server.ConnectionService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {}
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        instance = this;
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipListener = new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                if (clipboardManager != null && clipboardManager.hasPrimaryClip()) {
                    ClipData clip = clipboardManager.getPrimaryClip();
                    if (clip != null && clip.getItemCount() > 0) {
                        if (clip.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || clip.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
                            CharSequence text = clip.getItemAt(0).getText();
                            if (text != null) {
                                String str = text.toString();
                                if (!str.equals(lastClipboardText)) {
                                    lastClipboardText = str;
                                    updateClipboardPill(str);
                                    RemoteWebServer server = RemoteWebServerManager.getServerInstance();
                                    if (server != null) {
                                        try {
                                            JSONObject json = new JSONObject();
                                            json.put("type", "clipboard_sync");
                                            json.put("contentType", "text");
                                            json.put("content", str);
                                            server.broadcastWebSocket(json.toString());
                                            DebugLogger.log("Portapapeles texto enviado a PC");
                                        } catch (Exception e) {}
                                    }
                                }
                            }
                        } else if (clip.getItemAt(0).getUri() != null) {
                            android.net.Uri uri = clip.getItemAt(0).getUri();
                            asyncExecutor.execute(() -> {
                                try {
                                    java.io.InputStream is = getContentResolver().openInputStream(uri);
                                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
                                    if (bitmap != null) {
                                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos);
                                        byte[] bytes = baos.toByteArray();
                                        String base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
                                        String dataUrl = "data:image/png;base64," + base64;
                                        RemoteWebServer server = RemoteWebServerManager.getServerInstance();
                                        if (server != null) {
                                            JSONObject json = new JSONObject();
                                            json.put("type", "clipboard_sync");
                                            json.put("contentType", "image");
                                            json.put("content", dataUrl);
                                            server.broadcastWebSocket(json.toString());
                                            DebugLogger.log("Portapapeles imagen enviada a PC");
                                        }
                                    }
                                } catch (Exception e) {}
                            });
                        }
                    }
                }
            }
        };
        clipboardManager.addPrimaryClipChangedListener(clipListener);

        try {
            Intent serviceIntent = new Intent(this, com.remotekeyboard.server.ConnectionService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {}
    }

    private void performHapticFeedbackAsync() {
        if (prefs == null || !prefs.getBoolean(KEY_VIBRATION, false)) {
            return; // Vibración desactivada por defecto para máxima velocidad
        }
        asyncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (vibrator != null && vibrator.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(10);
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
    }
    private boolean isDevTierEnabled = true;

    private void checkDevAndExecute(Runnable action) {
        if (isDevTierEnabled) {
            action.run();
        } else {
            // Toast.makeText(getApplicationContext(), "🌟 Actualiza a PRO", Toast.LENGTH_SHORT).show();
        }
    }

    private final Runnable releaseWakeLockRunnable = new Runnable() {
        @Override
        public void run() {
            if (typingWakeLock != null && typingWakeLock.isHeld()) {
                try {
                    typingWakeLock.release();
                    DebugLogger.log("WakeLock liberado suavemente, respetando timeout del sistema");
                } catch (Exception e) {}
            }
        }
    };

    private void pokeWakeLock() {
        if (typingWakeLock != null) {
            try {
                if (!typingWakeLock.isHeld()) {
                    typingWakeLock.acquire();
                    DebugLogger.log("WakeLock sostenido");
                }
                mainHandler.removeCallbacks(releaseWakeLockRunnable);
                mainHandler.postDelayed(releaseWakeLockRunnable, 3000);
            } catch (Exception e) {}
        }
    }

    private void highlightVisualKeyByString(final String keyStr) {
        if (!isVisualKeyboardVisible || keyStr == null || keyStr.isEmpty()) return;
        mainHandler.post(() -> {
            String upper = keyStr.toUpperCase();
            TextView view = keyViewsMap.get(upper);
            if (view != null) {
                int originalColor = THEMES[currentThemeIndex].keyBg;
                int highlightColor = THEMES[currentThemeIndex].highlight;
                view.setBackgroundColor(highlightColor);
                view.setTextColor(THEMES[currentThemeIndex].bg);
                mainHandler.postDelayed(() -> {
                    view.setBackgroundColor(originalColor);
                    view.setTextColor(THEMES[currentThemeIndex].text);
                }, 100);
            }
        });
    }

    private void highlightVisualKeyByCode(final int keyCode) {
        if (!isVisualKeyboardVisible) return;
        mainHandler.post(() -> {
            TextView view = keyCodeViewsMap.get(keyCode);
            if (view != null) {
                int originalColor = THEMES[currentThemeIndex].keyBg;
                int highlightColor = THEMES[currentThemeIndex].highlight;
                view.setBackgroundColor(highlightColor);
                view.setTextColor(THEMES[currentThemeIndex].bg);
                mainHandler.postDelayed(() -> {
                    view.setBackgroundColor(originalColor);
                    view.setTextColor(THEMES[currentThemeIndex].text);
                }, 100);
            }
        });
    }

    public void typeText(final String text) {
        pokeWakeLock();
        if (text == null) return;
        DebugLogger.log("typeText llamado con texto: [" + text + "]");
        totalKeysTyped.addAndGet(text.length());
        performHapticFeedbackAsync();
        if (text.length() == 1) {
            highlightVisualKeyByString(text);
        }

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (tvLiveFeedback != null) {
                    tvLiveFeedback.setText("🟢 Recibido: " + (text.length() > 20 ? text.substring(0, 20) + "..." : text));
                }
                if (tvMetrics != null) {
                    tvMetrics.setText("Pulsaciones: " + totalKeysTyped.get());
                }

                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    DebugLogger.log("ic.commitText ejecutando para: [" + text + "]");
                    ic.commitText(text, 1);
                } else {
                    DebugLogger.log("ERROR: InputConnection (ic) es NULL. El usuario no está en un campo de texto.");
                    if (tvLiveFeedback != null) {
                        tvLiveFeedback.setText("⚠️ Error: Toca un campo de texto en Android primero");
                        tvLiveFeedback.setTextColor(Color.parseColor("#FBBF24"));
                    }
                }
            }
        });
    }

    public void sendSpecialKey(final String key) {
        pokeWakeLock();
        if (key == null) return;
        totalKeysTyped.incrementAndGet();
        performHapticFeedbackAsync();

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (tvLiveFeedback != null) {
                    tvLiveFeedback.setText("🟢 Tecla: " + key);
                }
                if (tvMetrics != null) {
                    tvMetrics.setText("Pulsaciones: " + totalKeysTyped.get());
                }

                InputConnection ic = getCurrentInputConnection();
                if (ic == null && tvLiveFeedback != null) {
                    tvLiveFeedback.setText("⚠️ Sin Foco: Toca un campo de texto en Android");
                    tvLiveFeedback.setTextColor(Color.parseColor("#FBBF24"));
                }
                
                String upper = key.toUpperCase();

                switch (upper) {
                    case "ENTER":
                        if (ic != null) {
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                            ic.performEditorAction(EditorInfo.IME_ACTION_DONE);
                            ic.performEditorAction(EditorInfo.IME_ACTION_SEND);
                        } else {
                            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
                        }
                        break;
                    case "BACKSPACE":
                    case "BACK":
                        if (ic != null) {
                            CharSequence selected = ic.getSelectedText(0);
                            if (selected != null && selected.length() > 0) {
                                ic.commitText("", 1); // Bugfix: Borrar selección actual
                            } else {
                                ic.deleteSurroundingText(1, 0); // Borrado de carácter
                            }
                        } else {
                            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
                        }
                        break;
                    case "TAB":
                        if (ic != null) {
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB));
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB));
                        } else {
                            sendDownUpKeyEvents(KeyEvent.KEYCODE_TAB);
                        }
                        break;
                    case "SPACE":
                        if (ic != null) {
                            ic.commitText(" ", 1);
                        } else {
                            sendDownUpKeyEvents(KeyEvent.KEYCODE_SPACE);
                        }
                        break;
                    case "ARROW_LEFT":
                    case "LEFT":
                        checkDevAndExecute(() -> {
                            if (ic != null) {
                                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
                                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT));
                            } else { sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT); }
                        });
                        break;
                    case "ARROW_RIGHT":
                    case "RIGHT":
                        checkDevAndExecute(() -> {
                            if (ic != null) {
                                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
                                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT));
                            } else { sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT); }
                        });
                        break;
                    case "ARROW_UP":
                    case "UP":
                        checkDevAndExecute(() -> {
                            if (ic != null) {
                                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
                                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP));
                            } else { sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_UP); }
                        });
                        break;
                    case "ARROW_DOWN":
                    case "DOWN":
                        checkDevAndExecute(() -> {
                            if (ic != null) {
                                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
                                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN));
                            } else { sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_DOWN); }
                        });
                        break;
                    case "SELECT_ALL":
                        checkDevAndExecute(() -> {
                            if (ic != null) ic.performContextMenuAction(android.R.id.selectAll);
                        });
                        break;
                    case "HOME":
                        checkDevAndExecute(() -> {
                            if (ic != null) {
                                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_HOME));
                                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_HOME));
                            } else { sendDownUpKeyEvents(KeyEvent.KEYCODE_MOVE_HOME); }
                        });
                        break;
                    case "END":
                        checkDevAndExecute(() -> {
                            if (ic != null) {
                                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_END));
                                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_END));
                            } else { sendDownUpKeyEvents(KeyEvent.KEYCODE_MOVE_END); }
                        });
                        break;
                    case "PAGE_UP":
                        checkDevAndExecute(() -> {
                            if (ic != null) {
                                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_UP));
                                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_PAGE_UP));
                            } else { sendDownUpKeyEvents(KeyEvent.KEYCODE_PAGE_UP); }
                        });
                        break;
                    case "PAGE_DOWN":
                        checkDevAndExecute(() -> {
                            if (ic != null) {
                                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_DOWN));
                                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_PAGE_DOWN));
                            } else { sendDownUpKeyEvents(KeyEvent.KEYCODE_PAGE_DOWN); }
                        });
                        break;
                    case "INSERT":
                        checkDevAndExecute(() -> {
                            if (ic != null) {
                                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_INSERT));
                                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_INSERT));
                            } else { sendDownUpKeyEvents(KeyEvent.KEYCODE_INSERT); }
                        });
                        break;
                    case "DELETE":
                        checkDevAndExecute(() -> {
                            if (ic != null) {
                                CharSequence selected = ic.getSelectedText(0);
                                if (selected != null && selected.length() > 0) {
                                    ic.commitText("", 1); // Delete selection
                                } else {
                                    ic.deleteSurroundingText(0, 1); // Delete forward
                                }
                            } else { sendDownUpKeyEvents(KeyEvent.KEYCODE_FORWARD_DEL); }
                        });
                        break;
                    default:
                        if (key.length() == 1) {
                            typeText(key);
                        }
                        break;
                }
            }
        });
    }

    public void handleRawKeyEvent(final String action, final String key, final String code, final int metaState, final long ts) {
        final int androidAction = "keyup".equalsIgnoreCase(action) ? KeyEvent.ACTION_UP : KeyEvent.ACTION_DOWN;
        if (androidAction == KeyEvent.ACTION_DOWN) {
            pokeWakeLock();
        }
        final int keyCode = mapWebCodeToAndroidKeyCode(code, key);
        
        if (androidAction == KeyEvent.ACTION_DOWN) {
            if (key != null && key.length() == 1) {
                highlightVisualKeyByString(key);
            } else if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                highlightVisualKeyByCode(keyCode);
            }
        }

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                InputConnection ic = getCurrentInputConnection();
                if (ic == null) return;

                // 1. Manejo seguro e inmediato de Backspace y Delete (WhatsApp, Chrome, editores)
                if ("Backspace".equalsIgnoreCase(code)) {
                    if (androidAction == KeyEvent.ACTION_DOWN) {
                        CharSequence selected = ic.getSelectedText(0);
                        if (selected != null && selected.length() > 0) {
                            ic.commitText("", 1); // Borrar bloque seleccionado
                        } else {
                            ic.deleteSurroundingText(1, 0); // Borrar carácter anterior
                        }
                    }
                    return;
                }

                if ("Delete".equalsIgnoreCase(code)) {
                    if (androidAction == KeyEvent.ACTION_DOWN) {
                        CharSequence selected = ic.getSelectedText(0);
                        if (selected != null && selected.length() > 0) {
                            ic.commitText("", 1);
                        } else {
                            ic.deleteSurroundingText(0, 1);
                        }
                    }
                    return;
                }

                // 2. Teclas de control básicas sin modificadores
                if (metaState == 0) {
                    if ("Space".equalsIgnoreCase(code)) {
                        if (androidAction == KeyEvent.ACTION_DOWN) {
                            ic.commitText(" ", 1);
                        }
                        return;
                    }
                    if ("Enter".equalsIgnoreCase(code) || "NumpadEnter".equalsIgnoreCase(code)) {
                        if (androidAction == KeyEvent.ACTION_DOWN) {
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                            ic.performEditorAction(EditorInfo.IME_ACTION_DONE);
                            ic.performEditorAction(EditorInfo.IME_ACTION_SEND);
                        }
                        return;
                    }
                    if ("Tab".equalsIgnoreCase(code)) {
                        if (androidAction == KeyEvent.ACTION_DOWN) {
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB));
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB));
                        }
                        return;
                    }
                }

                // 3. Atajos universales con CTRL (Garantía híbrida: acción nativa + KeyEvent físico)
                if ((metaState & KeyEvent.META_CTRL_ON) != 0 && androidAction == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_A) {
                        ic.performContextMenuAction(android.R.id.selectAll);
                    } else if (keyCode == KeyEvent.KEYCODE_C) {
                        ic.performContextMenuAction(android.R.id.copy);
                    } else if (keyCode == KeyEvent.KEYCODE_V) {
                        ic.performContextMenuAction(android.R.id.paste);
                    } else if (keyCode == KeyEvent.KEYCODE_X) {
                        ic.performContextMenuAction(android.R.id.cut);
                    } else if (keyCode == KeyEvent.KEYCODE_Z) {
                        ic.performContextMenuAction(android.R.id.undo);
                    }
                }

                // 4. Despacho de KeyEvent físico limpio (sin FLAG_SOFT_KEYBOARD para Termux, Acode y editores)
                if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                    long eventTime = android.os.SystemClock.uptimeMillis();
                    KeyEvent event = new KeyEvent(eventTime, eventTime, androidAction, keyCode, 0, metaState);
                    ic.sendKeyEvent(event);
                } else {
                    if (androidAction == KeyEvent.ACTION_DOWN && key != null && key.length() == 1) {
                        ic.commitText(key, 1);
                    }
                }
            }
        });
    }

    private int mapWebCodeToAndroidKeyCode(String code, String key) {
        if (code == null) return KeyEvent.KEYCODE_UNKNOWN;
        
        switch (code) {
            case "Enter": case "NumpadEnter": return KeyEvent.KEYCODE_ENTER;
            case "Backspace": return KeyEvent.KEYCODE_DEL;
            case "Tab": return KeyEvent.KEYCODE_TAB;
            case "Space": return KeyEvent.KEYCODE_SPACE;
            case "Escape": return KeyEvent.KEYCODE_ESCAPE;
            case "ShiftLeft": return KeyEvent.KEYCODE_SHIFT_LEFT;
            case "ShiftRight": return KeyEvent.KEYCODE_SHIFT_RIGHT;
            case "ControlLeft": return KeyEvent.KEYCODE_CTRL_LEFT;
            case "ControlRight": return KeyEvent.KEYCODE_CTRL_RIGHT;
            case "AltLeft": return KeyEvent.KEYCODE_ALT_LEFT;
            case "AltRight": return KeyEvent.KEYCODE_ALT_RIGHT;
            case "MetaLeft": return KeyEvent.KEYCODE_META_LEFT;
            case "MetaRight": return KeyEvent.KEYCODE_META_RIGHT;
            case "ArrowUp": return KeyEvent.KEYCODE_DPAD_UP;
            case "ArrowDown": return KeyEvent.KEYCODE_DPAD_DOWN;
            case "ArrowLeft": return KeyEvent.KEYCODE_DPAD_LEFT;
            case "ArrowRight": return KeyEvent.KEYCODE_DPAD_RIGHT;
            case "F1": return KeyEvent.KEYCODE_F1;
            case "F2": return KeyEvent.KEYCODE_F2;
            case "F3": return KeyEvent.KEYCODE_F3;
            case "F4": return KeyEvent.KEYCODE_F4;
            case "F5": return KeyEvent.KEYCODE_F5;
            case "F6": return KeyEvent.KEYCODE_F6;
            case "F7": return KeyEvent.KEYCODE_F7;
            case "F8": return KeyEvent.KEYCODE_F8;
            case "F9": return KeyEvent.KEYCODE_F9;
            case "F10": return KeyEvent.KEYCODE_F10;
            case "F11": return KeyEvent.KEYCODE_F11;
            case "F12": return KeyEvent.KEYCODE_F12;
            case "Insert": return KeyEvent.KEYCODE_INSERT;
            case "Delete": return KeyEvent.KEYCODE_FORWARD_DEL;
            case "Home": return KeyEvent.KEYCODE_MOVE_HOME;
            case "End": return KeyEvent.KEYCODE_MOVE_END;
            case "PageUp": return KeyEvent.KEYCODE_PAGE_UP;
            case "PageDown": return KeyEvent.KEYCODE_PAGE_DOWN;
            case "CapsLock": return KeyEvent.KEYCODE_CAPS_LOCK;
            case "NumLock": return KeyEvent.KEYCODE_NUM_LOCK;
            case "ScrollLock": return KeyEvent.KEYCODE_SCROLL_LOCK;
            case "Pause": return KeyEvent.KEYCODE_BREAK;
            case "PrintScreen": return KeyEvent.KEYCODE_SYSRQ;
        }

        if (code.startsWith("Key") && code.length() == 4) {
            char c = code.charAt(3);
            if (c >= 'A' && c <= 'Z') return KeyEvent.KEYCODE_A + (c - 'A');
        }
        
        if (code.startsWith("Digit") && code.length() == 6) {
            char c = code.charAt(5);
            if (c >= '0' && c <= '9') return KeyEvent.KEYCODE_0 + (c - '0');
        }

        if (code.startsWith("Numpad") && code.length() == 7) {
            char c = code.charAt(6);
            if (c >= '0' && c <= '9') return KeyEvent.KEYCODE_NUMPAD_0 + (c - '0');
        }

        // Simbolos especiales mapeo rápido
        switch (code) {
            case "Semicolon": return KeyEvent.KEYCODE_SEMICOLON;
            case "Equal": return KeyEvent.KEYCODE_EQUALS;
            case "Comma": return KeyEvent.KEYCODE_COMMA;
            case "Minus": return KeyEvent.KEYCODE_MINUS;
            case "Period": return KeyEvent.KEYCODE_PERIOD;
            case "Slash": return KeyEvent.KEYCODE_SLASH;
            case "Backquote": return KeyEvent.KEYCODE_GRAVE;
            case "BracketLeft": return KeyEvent.KEYCODE_LEFT_BRACKET;
            case "Backslash": return KeyEvent.KEYCODE_BACKSLASH;
            case "BracketRight": return KeyEvent.KEYCODE_RIGHT_BRACKET;
            case "Quote": return KeyEvent.KEYCODE_APOSTROPHE;
            case "ContextMenu": return KeyEvent.KEYCODE_MENU;
            case "NumpadAdd": return KeyEvent.KEYCODE_NUMPAD_ADD;
            case "NumpadSubtract": return KeyEvent.KEYCODE_NUMPAD_SUBTRACT;
            case "NumpadMultiply": return KeyEvent.KEYCODE_NUMPAD_MULTIPLY;
            case "NumpadDivide": return KeyEvent.KEYCODE_NUMPAD_DIVIDE;
            case "NumpadDecimal": return KeyEvent.KEYCODE_NUMPAD_DOT;
            case "NumpadEqual": return KeyEvent.KEYCODE_NUMPAD_EQUALS;
        }

        return KeyEvent.KEYCODE_UNKNOWN;
    }
}






