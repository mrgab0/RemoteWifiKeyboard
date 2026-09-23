package com.remotekeyboard;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = "RemoteKeyboard";
    private static final String PREFS_NAME = "RemoteKeyboardPrefs";
    private static final String KEY_IS_PRO = "is_pro_user";

    private TextView tvUrl;
    private TextView tvStatus;
    private TextView tvAppSubtitle;
    private EditText etTestInput;
    private Button btnCopyUrl;
    private Button btnEnableIme;
    private Button btnSelectIme;
    private Button btnBuyPro;
    private LinearLayout layoutProUpgrade;
    private LinearLayout adContainer;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Protección global contra cierres inesperados
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e(TAG, "Uncaught Exception in MainActivity: ", throwable);
            }
        });

        try {
            setContentView(R.layout.activity_main);

            prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

            tvUrl = findViewById(R.id.tvUrl);
            tvStatus = findViewById(R.id.tvStatus);
            tvAppSubtitle = findViewById(R.id.tvAppSubtitle);
            etTestInput = findViewById(R.id.etTestInput);
            btnCopyUrl = findViewById(R.id.btnCopyUrl);
            btnEnableIme = findViewById(R.id.btnEnableIme);
            btnSelectIme = findViewById(R.id.btnSelectIme);
            btnBuyPro = findViewById(R.id.btnBuyPro);
            layoutProUpgrade = findViewById(R.id.layoutProUpgrade);
            adContainer = findViewById(R.id.adContainer);

            updateIpAddress();
            setupMonetization();

            btnCopyUrl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = tvUrl.getText().toString();
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        ClipData clip = ClipData.newPlainText("Remote Keyboard URL", url);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(MainActivity.this, "URL copiada al portapapeles", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(MainActivity.this, "Error abriendo selector de teclado", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            btnBuyPro.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    prefs.edit().putBoolean(KEY_IS_PRO, true).apply();
                    setupMonetization();
                    Toast.makeText(MainActivity.this, "¡Felicidades! Licencia PRO Activada ($7.99 USD). Cero anuncios.", Toast.LENGTH_LONG).show();
                }
            });

        } catch (Throwable t) {
            Log.e(TAG, "Error durante onCreate: ", t);
        }
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
            Log.w(TAG, "Monetization setup warning: ", t);
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
        updateIpAddress();
    }

    private void updateIpAddress() {
        try {
            String ip = getIpAddress();
            if (ip != null) {
                String url = "http://" + ip + ":8080";
                if (tvUrl != null) tvUrl.setText(url);
                if (tvStatus != null) {
                    tvStatus.setText("● Servidor Listo en este Teléfono");
                    tvStatus.setTextColor(0xFF00E676);
                }
            } else {
                if (tvUrl != null) tvUrl.setText("Sin conexión Wi-Fi");
                if (tvStatus != null) {
                    tvStatus.setText("○ Conéctate a una red Wi-Fi");
                    tvStatus.setTextColor(0xFFFF5252);
                }
            }
        } catch (Exception ignored) {}
    }

    private String getIpAddress() {
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
        } catch (Exception ignored) { }
        return null;
    }
}
