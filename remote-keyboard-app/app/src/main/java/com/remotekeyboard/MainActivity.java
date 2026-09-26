package com.remotekeyboard;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.remotekeyboard.server.RemoteWebServer;
import com.remotekeyboard.server.RemoteWebServerManager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = "RemoteKeyboard";
    private static final String PREFS_NAME = "RemoteKeyboardPrefs";
    private static final String KEY_IS_PRO = "is_pro_user";
    private static final String KEY_VIBRATION = "vibration_enabled";

    private TextView tvStatus;
    private TextView tvUrl;
    private TextView tvAppSubtitle;
    private Button btnCopyUrl;
    private Button btnEnableIme;
    private Button btnSelectIme;
    private EditText etTestInput;

    // Telemetría & Debug
    private TextView tvWsClients;
    private TextView tvImeStatus;
    private TextView tvKeystrokes;
    private Switch switchVibration;

    private LinearLayout layoutProUpgrade;
    private LinearLayout adContainer;
    private Button btnBuyPro;
    private SharedPreferences prefs;
    private final Handler telemetryHandler = new Handler(Looper.getMainLooper());
    private Runnable telemetryRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DebugLogger.init(this);
        DebugLogger.log("MainActivity onCreate iniciado.");

        // Protección global contra cierres inesperados
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e(TAG, "Uncaught Exception in MainActivity: ", throwable);
                DebugLogger.log("CRASH en MainActivity: " + throwable.getMessage());
            }
        });

        try {
            setContentView(R.layout.activity_main);
            prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

            // Iniciar servidor HTTP & WebSocket en segundo plano
            RemoteWebServerManager.ensureServerStarted(this);

            initViews();
            setupMonetization();
            updateIpDisplay();
            setupTelemetryLoop();

        } catch (Throwable t) {
            Log.e(TAG, "Error durante onCreate: ", t);
            DebugLogger.log("Error en onCreate de MainActivity: " + t.getMessage());
        }
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvUrl = findViewById(R.id.tvUrl);
        tvAppSubtitle = findViewById(R.id.tvAppSubtitle);
        btnCopyUrl = findViewById(R.id.btnCopyUrl);
        btnEnableIme = findViewById(R.id.btnEnableIme);
        btnSelectIme = findViewById(R.id.btnSelectIme);
        etTestInput = findViewById(R.id.etTestInput);

        tvWsClients = findViewById(R.id.tvWsClients);
        tvImeStatus = findViewById(R.id.tvImeStatus);
        tvKeystrokes = findViewById(R.id.tvKeystrokes);
        switchVibration = findViewById(R.id.switchVibration);

        layoutProUpgrade = findViewById(R.id.layoutProUpgrade);
        adContainer = findViewById(R.id.adContainer);
        btnBuyPro = findViewById(R.id.btnBuyPro);

        // Configuración de Switch de Vibración
        boolean vibEnabled = prefs.getBoolean(KEY_VIBRATION, false);
        switchVibration.setChecked(vibEnabled);
        switchVibration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_VIBRATION, isChecked).apply();
                if (isChecked) {
                    Toast.makeText(MainActivity.this, "Vibración activada", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Vibración desactivada (Máxima velocidad)", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCopyUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = tvUrl.getText().toString();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("Remote Keyboard URL", url);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(MainActivity.this, "URL copiada: " + url, Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnEnableIme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    Toast.makeText(MainActivity.this, "Busca 'Remote WiFi Keyboard' y activa el interruptor", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    try {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                    } catch (Exception ignored) {}
                }
            }
        });

        btnSelectIme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showInputMethodPicker();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error al abrir el selector de teclado", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnBuyPro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.edit().putBoolean(KEY_IS_PRO, true).apply();
                setupMonetization();
                Toast.makeText(MainActivity.this, "¡Licencia PRO Activada ($7.99 USD)! Cero anuncios.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupTelemetryLoop() {
        telemetryRunnable = new Runnable() {
            @Override
            public void run() {
                updateTelemetryUI();
                telemetryHandler.postDelayed(this, 1200);
            }
        };
    }

    private void updateTelemetryUI() {
        try {
            RemoteWebServer server = RemoteWebServerManager.getServerInstance();
            RemoteInputMethodService ime = RemoteInputMethodService.getInstance();

            // 1. Clientes WebSocket
            if (server != null) {
                int wsCount = server.getActiveWebSocketCount();
                if (wsCount > 0) {
                    tvWsClients.setText("• Conexión PC: 🟢 " + wsCount + " Cliente(s) WebSocket Activo(s) (<3ms)");
                    tvWsClients.setTextColor(Color.parseColor("#34D399"));
                } else {
                    tvWsClients.setText("• Conexión PC: ⚪ Esperando conexión desde PC / Navegador...");
                    tvWsClients.setTextColor(Color.parseColor("#94A3B8"));
                }
            }

            // 2. Estado Teclado IME
            if (ime != null) {
                tvImeStatus.setText("• Teclado IME: 🟢 Servicio Activo y Vinculado");
                tvImeStatus.setTextColor(Color.parseColor("#34D399"));
                tvKeystrokes.setText("• Pulsaciones Recibidas: " + ime.getTotalKeysTyped() + " teclas");
                tvKeystrokes.setTextColor(Color.parseColor("#E2E8F0"));
            } else {
                boolean isEnabled = isImeEnabled();
                if (isEnabled) {
                    tvImeStatus.setText("• Teclado IME: 🟡 Habilitado (Falta seleccionar como activo en Paso 2)");
                    tvImeStatus.setTextColor(Color.parseColor("#FBBF24"));
                } else {
                    tvImeStatus.setText("• Teclado IME: 🔴 No habilitado en ajustes del sistema");
                    tvImeStatus.setTextColor(Color.parseColor("#F87171"));
                }
            }
        } catch (Exception ignored) {}
    }

    private boolean isImeEnabled() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                List<InputMethodInfo> list = imm.getEnabledInputMethodList();
                for (InputMethodInfo info : list) {
                    if (info.getPackageName().equals(getPackageName())) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void updateIpDisplay() {
        try {
            String ip = getDeviceIpAddress();
            if (ip != null) {
                String fullUrl = "http://" + ip + ":9999";
                tvUrl.setText(fullUrl);
                tvStatus.setText("● Servidor Activo (HTTP + WebSocket)");
                tvStatus.setTextColor(0xFF10B981);
            } else {
                tvUrl.setText("Sin conexión Wi-Fi");
                tvStatus.setText("○ Conéctate a una red Wi-Fi");
                tvStatus.setTextColor(0xFFEF4444);
            }
        } catch (Exception ignored) {}
    }

    private String getDeviceIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr != null && sAddr.indexOf(':') < 0) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void setupMonetization() {
        try {
            boolean isPro = prefs.getBoolean(KEY_IS_PRO, false);
            if (isPro) {
                if (tvAppSubtitle != null) {
                    tvAppSubtitle.setText("⭐ Licencia PRO Activa • Cero Anuncios");
                    tvAppSubtitle.setTextColor(0xFFFFD700);
                }
                if (layoutProUpgrade != null) layoutProUpgrade.setVisibility(View.GONE);
                if (adContainer != null) adContainer.setVisibility(View.GONE);
            } else {
                loadAdMobSafely();
            }
        } catch (Throwable t) {
            Log.w(TAG, "Monetization warning: ", t);
        }
    }

    private void loadAdMobSafely() {
        try {
            com.google.android.gms.ads.MobileAds.initialize(this, new com.google.android.gms.ads.initialization.OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(com.google.android.gms.ads.initialization.InitializationStatus initializationStatus) {
                }
            });

            if (adContainer != null && adContainer.getChildCount() == 0) {
                com.google.android.gms.ads.AdView adView = new com.google.android.gms.ads.AdView(this);
                adView.setAdSize(com.google.android.gms.ads.AdSize.BANNER);
                adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");

                adContainer.addView(adView);
                com.google.android.gms.ads.AdRequest adRequest = new com.google.android.gms.ads.AdRequest.Builder().build();
                adView.loadAd(adRequest);
            }
        } catch (Throwable t) {
            Log.w(TAG, "AdMob safe load skipped: " + t.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        RemoteWebServerManager.ensureServerStarted(this);
        updateIpDisplay();
        if (telemetryRunnable != null) {
            telemetryHandler.post(telemetryRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (telemetryRunnable != null) {
            telemetryHandler.removeCallbacks(telemetryRunnable);
        }
    }
}
