package com.remotekeyboard;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.remotekeyboard.service.KeyboardServerService;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQUEST_NOTIFICATION_CODE = 101;
    private static final String PREFS_NAME = "RemoteKeyboardPrefs";
    private static final String KEY_IS_PRO = "is_pro_user";

    private TextView tvUrl;
    private TextView tvStatus;
    private TextView tvAppSubtitle;
    private Button btnCopyUrl;
    private Button btnEnableIme;
    private Button btnSelectIme;
    private Button btnBuyPro;
    private LinearLayout layoutProUpgrade;
    private LinearLayout adContainer;
    private AdView adView;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        tvUrl = findViewById(R.id.tvUrl);
        tvStatus = findViewById(R.id.tvStatus);
        tvAppSubtitle = findViewById(R.id.tvAppSubtitle);
        btnCopyUrl = findViewById(R.id.btnCopyUrl);
        btnEnableIme = findViewById(R.id.btnEnableIme);
        btnSelectIme = findViewById(R.id.btnSelectIme);
        btnBuyPro = findViewById(R.id.btnBuyPro);
        layoutProUpgrade = findViewById(R.id.layoutProUpgrade);
        adContainer = findViewById(R.id.adContainer);
        adView = findViewById(R.id.adView);

        checkNotificationPermission();
        startKeyboardService();
        updateIpAddress();
        setupMonetization();

        btnCopyUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = tvUrl.getText().toString();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Remote Keyboard URL", url);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "URL copiada al portapapeles", Toast.LENGTH_SHORT).show();
            }
        });

        btnEnableIme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                startActivity(intent);
            }
        });

        btnSelectIme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showInputMethodPicker();
                }
            }
        });

        btnBuyPro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Desbloqueo de versión PRO de por vida
                prefs.edit().putBoolean(KEY_IS_PRO, true).apply();
                setupMonetization();
                Toast.makeText(MainActivity.this, "¡Felicidades! Licencia PRO Activada ($7.99 USD). Anuncios eliminados para siempre.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupMonetization() {
        boolean isPro = prefs.getBoolean(KEY_IS_PRO, false);
        if (isPro) {
            tvAppSubtitle.setText("⭐ Licencia PRO Activa • Cero Anuncios");
            tvAppSubtitle.setTextColor(0xFFFFD700);
            layoutProUpgrade.setVisibility(View.GONE);
            adContainer.setVisibility(View.GONE);
        } else {
            MobileAds.initialize(this, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus initializationStatus) {
                }
            });
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_CODE);
            }
        }
    }

    private void startKeyboardService() {
        Intent serviceIntent = new Intent(this, KeyboardServerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) adView.resume();
        startKeyboardService();
        updateIpAddress();
    }

    @Override
    protected void onPause() {
        if (adView != null) adView.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (adView != null) adView.destroy();
        super.onDestroy();
    }

    private void updateIpAddress() {
        String ip = getIpAddress();
        if (ip != null) {
            String url = "http://" + ip + ":8080";
            tvUrl.setText(url);
            tvStatus.setText("● Servidor en Primer Plano Activo");
            tvStatus.setTextColor(0xFF00E676);
        } else {
            tvUrl.setText("Sin conexión Wi-Fi");
            tvStatus.setText("○ Conéctate a una red Wi-Fi");
            tvStatus.setTextColor(0xFFFF5252);
        }
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
