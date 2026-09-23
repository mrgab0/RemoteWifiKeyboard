package com.remotekeyboard;

import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.os.Build;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;

import com.remotekeyboard.server.RemoteWebServer;

public class RemoteInputMethodService extends InputMethodService {

    private static RemoteInputMethodService instance;
    private RemoteWebServer webServer;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final int PORT = 8080;

    private TextView tvLiveFeedback;
    private Vibrator vibrator;

    public static RemoteInputMethodService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        startServer();
    }

    private synchronized void startServer() {
        if (webServer == null) {
            try {
                webServer = new RemoteWebServer(getApplicationContext(), this, PORT);
                webServer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public View onCreateInputView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#0F1015"));
        layout.setPadding(32, 28, 32, 28);
        layout.setGravity(Gravity.CENTER);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("⌨️ Remote WiFi Keyboard Conectado");
        tvTitle.setTextColor(Color.parseColor("#00E676"));
        tvTitle.setTextSize(14);
        tvTitle.setGravity(Gravity.CENTER);

        tvLiveFeedback = new TextView(this);
        tvLiveFeedback.setText("Esperando teclas desde la PC...");
        tvLiveFeedback.setTextColor(Color.parseColor("#FF97A4"));
        tvLiveFeedback.setTextSize(12);
        tvLiveFeedback.setGravity(Gravity.CENTER);
        tvLiveFeedback.setPadding(0, 10, 0, 0);

        layout.addView(tvTitle);
        layout.addView(tvLiveFeedback);

        return layout;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        startServer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webServer != null) {
            webServer.stop();
            webServer = null;
        }
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
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                vibrate();
                if (tvLiveFeedback != null) {
                    tvLiveFeedback.setText("🟢 Texto: " + (text.length() > 25 ? text.substring(0, 25) + "..." : text));
                }
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.commitText(text, 1);
                } else {
                    // Si no hay input connection activa, enviar como eventos de teclado estándar
                    for (char c : text.toCharArray()) {
                        sendDownUpKeyEvents(KeyEvent.getDeadChar(0, c));
                    }
                }
            }
        });
    }

    public void sendSpecialKey(final String key) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                vibrate();
                if (tvLiveFeedback != null) {
                    tvLiveFeedback.setText("🟢 Tecla: " + key);
                }

                InputConnection ic = getCurrentInputConnection();

                switch (key) {
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
                    case "ARROW_LEFT":
                        if (ic != null) {
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT));
                        } else {
                            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT);
                        }
                        break;
                    case "ARROW_RIGHT":
                        if (ic != null) {
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT));
                        } else {
                            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT);
                        }
                        break;
                    case "SELECT_ALL":
                        if (ic != null) {
                            ic.performContextMenuAction(android.R.id.selectAll);
                        }
                        break;
                }
            }
        });
    }
}
