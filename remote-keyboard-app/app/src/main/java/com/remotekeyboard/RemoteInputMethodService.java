package com.remotekeyboard;

import android.content.Context;
import android.content.Intent;
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

public class RemoteInputMethodService extends InputMethodService {

    private static final String TAG = "RemoteIME";
    private static RemoteInputMethodService instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TextView tvLiveFeedback;
    private Vibrator vibrator;

    public static synchronized RemoteInputMethodService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
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
        layout.setPadding(24, 20, 24, 20);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("⌨️ Remote WiFi Keyboard Activo");
        tvTitle.setTextColor(Color.parseColor("#818CF8"));
        tvTitle.setTextSize(14);
        tvTitle.setGravity(Gravity.CENTER);

        tvLiveFeedback = new TextView(this);
        tvLiveFeedback.setText("Esperando pulsaciones desde la PC...");
        tvLiveFeedback.setTextColor(Color.parseColor("#94A3B8"));
        tvLiveFeedback.setTextSize(12);
        tvLiveFeedback.setGravity(Gravity.CENTER);
        tvLiveFeedback.setPadding(0, 6, 0, 12);

        layout.addView(tvTitle);
        layout.addView(tvLiveFeedback);

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
        btnSettings.setText("⚙️ Panel de Control");
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
        params.setMargins(8, 0, 8, 0);

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
    }

    private void vibrate() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(15);
                }
            }
        } catch (Exception ignored) {}
    }

    public void typeText(final String text) {
        if (text == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                vibrate();
                if (tvLiveFeedback != null) {
                    tvLiveFeedback.setText("🟢 Recibido: " + (text.length() > 25 ? text.substring(0, 25) + "..." : text));
                }

                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.commitText(text, 1);
                    Log.d(TAG, "Texto inyectado via commitText: " + text);
                } else {
                    Log.w(TAG, "InputConnection es null, intentando enviar KeyEvent");
                    for (char c : text.toCharArray()) {
                        sendDownUpKeyEvents(KeyEvent.getDeadChar(0, c));
                    }
                }
            }
        });
    }

    public void sendSpecialKey(final String key) {
        if (key == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                vibrate();
                if (tvLiveFeedback != null) {
                    tvLiveFeedback.setText("🟢 Tecla: " + key);
                }

                InputConnection ic = getCurrentInputConnection();
                String upper = key.toUpperCase();

                switch (upper) {
                    case "ENTER":
                    case "RETURN":
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
                            ic.deleteSurroundingText(1, 0);
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
                        if (ic != null) {
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT));
                        } else {
                            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT);
                        }
                        break;
                    case "ARROW_RIGHT":
                    case "RIGHT":
                        if (ic != null) {
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT));
                        } else {
                            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT);
                        }
                        break;
                    case "ARROW_UP":
                    case "UP":
                        if (ic != null) {
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP));
                        } else {
                            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_UP);
                        }
                        break;
                    case "ARROW_DOWN":
                    case "DOWN":
                        if (ic != null) {
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN));
                        } else {
                            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_DOWN);
                        }
                        break;
                    case "SELECT_ALL":
                        if (ic != null) {
                            ic.performContextMenuAction(android.R.id.selectAll);
                        }
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
}
