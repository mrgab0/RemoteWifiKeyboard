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
        RemoteWebServerManager.ensureServerStarted(this);
    }

    @Override
    public View onCreateInputView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#0B0D14"));
        layout.setPadding(24, 16, 24, 16);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("⌨️ Remote WiFi Keyboard • Modo Ultrabaja Latencia");
        tvTitle.setTextColor(Color.parseColor("#818CF8"));
        tvTitle.setTextSize(13);
        tvTitle.setGravity(Gravity.CENTER);

        tvLiveFeedback = new TextView(this);
        tvLiveFeedback.setText("🟢 Listo para recibir teclas desde PC...");
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

        layout.addView(tvTitle);
        layout.addView(tvLiveFeedback);
        layout.addView(tvMetrics);

        // Barra de botones rápidos dentro del teclado
        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER);

        Button btnSwitch = new Button(this);
        btnSwitch.setText("🌐 Cambiar Teclado");
        btnSwitch.setTextColor(Color.WHITE);
        btnSwitch.setBackgroundColor(Color.parseColor("#1E2337"));
        btnSwitch.setTextSize(11);
        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showInputMethodPicker();
                    }
                } catch (Exception ignored) {}
            }
        });

        Button btnSettings = new Button(this);
        btnSettings.setText("⚙️ Panel y Debug");
        btnSettings.setTextColor(Color.WHITE);
        btnSettings.setBackgroundColor(Color.parseColor("#1E2337"));
        btnSettings.setTextSize(11);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(RemoteInputMethodService.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception ignored) {}
            }
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(6, 0, 6, 0);

        buttonRow.addView(btnSwitch, params);
        buttonRow.addView(btnSettings, params);
        layout.addView(buttonRow);

        return layout;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        instance = this;
        RemoteWebServerManager.ensureServerStarted(this);
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        instance = this;
        RemoteWebServerManager.ensureServerStarted(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        asyncExecutor.shutdownNow();
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

    public void typeText(final String text) {
        if (text == null) return;
        DebugLogger.log("typeText llamado con texto: [" + text + "]");
        totalKeysTyped.addAndGet(text.length());
        performHapticFeedbackAsync();

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
        final int keyCode = mapWebCodeToAndroidKeyCode(code, key);
        
        DebugLogger.log("[" + ts + "] RECEIVE " + code + " " + action.toUpperCase());

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                InputConnection ic = getCurrentInputConnection();
                if (ic == null) {
                    DebugLogger.log("[" + ts + "] INPUT_CONNECTION = NULL. Aborting raw event.");
                    return;
                }

                DebugLogger.log("[" + ts + "] INPUT_CONNECTION = AVAILABLE");

                if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                    long eventTime = android.os.SystemClock.uptimeMillis();
                    KeyEvent event = new KeyEvent(eventTime, eventTime, androidAction, keyCode, 0, metaState);
                    
                    DebugLogger.log("[" + ts + "] ACTION = ic.sendKeyEvent(" + KeyEvent.keyCodeToString(keyCode) + ", " + action + ")");
                    ic.sendKeyEvent(event);
                } else {
                    DebugLogger.log("[" + ts + "] ACTION = UNKNOWN KEY CODE. Falling back to typeText.");
                    if (androidAction == KeyEvent.ACTION_DOWN && key.length() == 1) {
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
        }

        return KeyEvent.KEYCODE_UNKNOWN;
    }
}
