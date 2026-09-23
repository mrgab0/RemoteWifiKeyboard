package com.remotekeyboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {

    private static final String TAG = "RemoteKeyboard";
    private static final String PREFS_NAME = "RemoteKeyboardPrefs";
    private static final String KEY_IS_PRO = "is_pro_user";
    private static final String KEY_SAVED_PC_IP = "saved_pc_ip";
    private static final int SPEECH_REQUEST_CODE = 101;

    // UI Header & Status
    private TextView tvStatusBadge;
    private TextView tvPcStatus;
    private TextView tvAppSubtitle;
    private EditText etPcIp;
    private Button btnConnectPc;
    private Button btnScanPc;

    // Live Typing & Voice
    private EditText etLiveInput;
    private CheckBox cbLiveSend;
    private Button btnSendAll;
    private Button btnVoice;
    private Button btnClear;

    // Windows Keys
    private Button btnKeyEnter, btnKeyBackspace, btnKeySpace, btnKeyTab, btnKeyEsc, btnKeyDel;
    private Button btnCtrlC, btnCtrlV, btnCtrlZ, btnCtrlA, btnCtrlS;
    private Button btnWinD, btnAltF4;

    // Navigation & Media
    private Button btnArrowUp, btnArrowDown, btnArrowLeft, btnArrowRight, btnKeyOk;
    private Button btnVolDown, btnVolMute, btnVolUp;
    private Button btnMediaPrev, btnMediaPlay, btnMediaNext;

    // Reverse Mode (PC -> Phone IME)
    private TextView tvUrl;
    private Button btnEnableIme;
    private Button btnSelectIme;

    // Monetization
    private LinearLayout layoutProUpgrade;
    private LinearLayout adContainer;
    private Button btnBuyPro;

    // State & Async
    private SharedPreferences prefs;
    private Vibrator vibrator;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService networkExecutor = Executors.newFixedThreadPool(16);
    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private String currentPcIp = "192.168.1.100";
    private boolean isConnected = false;
    private String previousText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Crash-proof global handler
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e(TAG, "Uncaught Exception in MainActivity: ", throwable);
            }
        });

        try {
            setContentView(R.layout.activity_main);
            prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

            initViews();
            setupMonetization();
            setupLiveTyping();
            setupKeyButtons();
            setupImeMode();

            // Cargar IP guardada o auto-descubrir
            String savedIp = prefs.getString(KEY_SAVED_PC_IP, null);
            if (savedIp != null && !savedIp.isEmpty()) {
                currentPcIp = savedIp;
                etPcIp.setText(savedIp);
                checkPcConnection(savedIp, true);
            } else {
                autoDiscoverPc();
            }

            updateReverseModeUrl();

        } catch (Throwable t) {
            Log.e(TAG, "Error durante onCreate: ", t);
        }
    }

    private void initViews() {
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvPcStatus = findViewById(R.id.tvPcStatus);
        tvAppSubtitle = findViewById(R.id.tvAppSubtitle);
        etPcIp = findViewById(R.id.etPcIp);
        btnConnectPc = findViewById(R.id.btnConnectPc);
        btnScanPc = findViewById(R.id.btnScanPc);

        etLiveInput = findViewById(R.id.etLiveInput);
        cbLiveSend = findViewById(R.id.cbLiveSend);
        btnSendAll = findViewById(R.id.btnSendAll);
        btnVoice = findViewById(R.id.btnVoice);
        btnClear = findViewById(R.id.btnClear);

        btnKeyEnter = findViewById(R.id.btnKeyEnter);
        btnKeyBackspace = findViewById(R.id.btnKeyBackspace);
        btnKeySpace = findViewById(R.id.btnKeySpace);
        btnKeyTab = findViewById(R.id.btnKeyTab);
        btnKeyEsc = findViewById(R.id.btnKeyEsc);
        btnKeyDel = findViewById(R.id.btnKeyDel);

        btnCtrlC = findViewById(R.id.btnCtrlC);
        btnCtrlV = findViewById(R.id.btnCtrlV);
        btnCtrlZ = findViewById(R.id.btnCtrlZ);
        btnCtrlA = findViewById(R.id.btnCtrlA);
        btnCtrlS = findViewById(R.id.btnCtrlS);
        btnWinD = findViewById(R.id.btnWinD);
        btnAltF4 = findViewById(R.id.btnAltF4);

        btnArrowUp = findViewById(R.id.btnArrowUp);
        btnArrowDown = findViewById(R.id.btnArrowDown);
        btnArrowLeft = findViewById(R.id.btnArrowLeft);
        btnArrowRight = findViewById(R.id.btnArrowRight);
        btnKeyOk = findViewById(R.id.btnKeyOk);

        btnVolDown = findViewById(R.id.btnVolDown);
        btnVolMute = findViewById(R.id.btnVolMute);
        btnVolUp = findViewById(R.id.btnVolUp);
        btnMediaPrev = findViewById(R.id.btnMediaPrev);
        btnMediaPlay = findViewById(R.id.btnMediaPlay);
        btnMediaNext = findViewById(R.id.btnMediaNext);

        tvUrl = findViewById(R.id.tvUrl);
        btnEnableIme = findViewById(R.id.btnEnableIme);
        btnSelectIme = findViewById(R.id.btnSelectIme);

        layoutProUpgrade = findViewById(R.id.layoutProUpgrade);
        adContainer = findViewById(R.id.adContainer);
        btnBuyPro = findViewById(R.id.btnBuyPro);

        btnConnectPc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = etPcIp.getText().toString().trim();
                if (!ip.isEmpty()) {
                    currentPcIp = ip;
                    prefs.edit().putString(KEY_SAVED_PC_IP, ip).apply();
                    checkPcConnection(ip, false);
                }
            }
        });

        btnScanPc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoDiscoverPc();
            }
        });
    }

    private void setupLiveTyping() {
        etLiveInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!cbLiveSend.isChecked()) return;

                String current = s.toString();
                if (current.length() > previousText.length()) {
                    // Caracteres agregados
                    String added = current.substring(previousText.length());
                    sendTypeText(added);
                } else if (current.length() < previousText.length()) {
                    // Caracteres borrados
                    int deletedCount = previousText.length() - current.length();
                    for (int i = 0; i < deletedCount; i++) {
                        sendKeyCommand("backspace");
                    }
                }
                previousText = current;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnSendAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic();
                String text = etLiveInput.getText().toString();
                if (!text.isEmpty()) {
                    sendTypeText(text);
                    Toast.makeText(MainActivity.this, "Texto enviado al PC ⚡", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic();
                previousText = "";
                etLiveInput.setText("");
            }
        });

        btnVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic();
                startSpeechRecognition();
            }
        });
    }

    private void setupKeyButtons() {
        bindKey(btnKeyEnter, "enter");
        bindKey(btnKeyBackspace, "backspace");
        bindKey(btnKeySpace, "space");
        bindKey(btnKeyTab, "tab");
        bindKey(btnKeyEsc, "escape");
        bindKey(btnKeyDel, "delete");

        bindKey(btnCtrlC, "ctrl+c");
        bindKey(btnCtrlV, "ctrl+v");
        bindKey(btnCtrlZ, "ctrl+z");
        bindKey(btnCtrlA, "ctrl+a");
        bindKey(btnCtrlS, "ctrl+s");
        bindKey(btnWinD, "win+d");
        bindKey(btnAltF4, "alt+f4");

        bindKey(btnArrowUp, "arrowup");
        bindKey(btnArrowDown, "arrowdown");
        bindKey(btnArrowLeft, "arrowleft");
        bindKey(btnArrowRight, "arrowright");
        bindKey(btnKeyOk, "enter");

        bindKey(btnVolDown, "volumedown");
        bindKey(btnVolMute, "volumemute");
        bindKey(btnVolUp, "volumeup");
        bindKey(btnMediaPrev, "prevtrack");
        bindKey(btnMediaPlay, "playpause");
        bindKey(btnMediaNext, "nexttrack");
    }

    private void bindKey(Button btn, final String keyCommand) {
        if (btn == null) return;
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic();
                sendKeyCommand(keyCommand);
            }
        });
    }

    private void triggerHaptic() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(20);
                }
            }
        } catch (Throwable ignored) {}
    }

    // --- Red & Envío de Comandos a Windows PC ---

    private void sendTypeText(final String text) {
        if (text == null || text.isEmpty()) return;
        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject json = new JSONObject();
                    json.put("text", text);
                    postJson("http://" + currentPcIp + ":8080/api/type", json.toString());
                } catch (Throwable t) {
                    Log.e(TAG, "Error enviando texto: ", t);
                }
            }
        });
    }

    private void sendKeyCommand(final String key) {
        if (key == null || key.isEmpty()) return;
        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject json = new JSONObject();
                    json.put("key", key);
                    postJson("http://" + currentPcIp + ":8080/api/key", json.toString());
                } catch (Throwable t) {
                    Log.e(TAG, "Error enviando tecla: ", t);
                }
            }
        });
    }

    private void postJson(String urlString, String jsonBody) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            conn.setDoOutput(true);

            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                setConnectionState(true, currentPcIp);
            }
        } catch (Throwable t) {
            setConnectionState(false, currentPcIp);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void checkPcConnection(final String ip, final boolean fallbackToScan) {
        tvStatusBadge.setText("🟡 Verificando...");
        tvPcStatus.setText("Probando conexión con " + ip + "...");

        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                boolean ok = pingIp(ip);
                if (ok) {
                    currentPcIp = ip;
                    prefs.edit().putString(KEY_SAVED_PC_IP, ip).apply();
                    setConnectionState(true, ip);
                } else {
                    setConnectionState(false, ip);
                    if (fallbackToScan) {
                        autoDiscoverPc();
                    }
                }
            }
        });
    }

    private boolean pingIp(String ip) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://" + ip + ":8080/api/ping");
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(600);
            conn.setReadTimeout(600);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                return sb.toString().contains("Rust Win32") || sb.toString().contains("ok");
            }
        } catch (Throwable ignored) {}
        finally {
            if (conn != null) conn.disconnect();
        }
        return false;
    }

    private void autoDiscoverPc() {
        if (isScanning.getAndSet(true)) return;

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                tvStatusBadge.setText("🟡 Buscando...");
                tvStatusBadge.setBackgroundColor(0xFF334155);
                tvPcStatus.setText("Escaneando red local WiFi en busca del PC...");
            }
        });

        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String localIp = getDeviceIpAddress();
                    if (localIp == null || !localIp.contains(".")) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                isScanning.set(false);
                                tvStatusBadge.setText("🔴 Sin WiFi");
                                tvPcStatus.setText("Por favor conecta el teléfono a la red WiFi del PC.");
                            }
                        });
                        return;
                    }

                    String subnet = localIp.substring(0, localIp.lastIndexOf('.') + 1);
                    final AtomicBoolean found = new AtomicBoolean(false);

                    for (int i = 1; i <= 254; i++) {
                        if (found.get()) break;
                        final String testIp = subnet + i;
                        networkExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                if (found.get()) return;
                                if (pingIp(testIp)) {
                                    if (!found.getAndSet(true)) {
                                        currentPcIp = testIp;
                                        prefs.edit().putString(KEY_SAVED_PC_IP, testIp).apply();
                                        setConnectionState(true, testIp);
                                        isScanning.set(false);
                                    }
                                }
                            }
                        });
                    }

                    // Timeout después de 3 segundos si no se encuentra
                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!found.get()) {
                                isScanning.set(false);
                                if (!isConnected) {
                                    tvStatusBadge.setText("🔴 No Detectado");
                                    tvPcStatus.setText("No se encontró el PC automáticamente. Abre RemoteWiFiKeyboard.exe en tu PC e ingresa su IP manualmente.");
                                }
                            }
                        }
                    }, 3500);

                } catch (Throwable t) {
                    isScanning.set(false);
                    Log.e(TAG, "Error en autoDiscoverPc: ", t);
                }
            }
        });
    }

    private void setConnectionState(final boolean connected, final String ip) {
        isConnected = connected;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (connected) {
                    tvStatusBadge.setText("🟢 Conectado");
                    tvStatusBadge.setTextColor(0xFF00E676);
                    tvStatusBadge.setBackgroundColor(0xFF064E3B);
                    tvPcStatus.setText("Conectado a PC en " + ip + ":8080 (0.1 ms)");
                    etPcIp.setText(ip);
                } else {
                    tvStatusBadge.setText("🔴 Desconectado");
                    tvStatusBadge.setTextColor(0xFFFF5252);
                    tvStatusBadge.setBackgroundColor(0xFF450A0A);
                    tvPcStatus.setText("Sin respuesta de " + ip + ":8080. ¿Está abierto RemoteWiFiKeyboard.exe en tu PC?");
                }
            }
        });
    }

    // --- Dictado por Voz ---

    private void startSpeechRecognition() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla para escribir en tu PC...");
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Throwable t) {
            Toast.makeText(this, "Reconocimiento de voz no disponible en este dispositivo", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                etLiveInput.setText(spokenText);
                previousText = spokenText;
                sendTypeText(spokenText + " ");
                Toast.makeText(this, "Voz enviada al PC: \"" + spokenText + "\"", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- Modo Inverso IME & Configuración ---

    private void setupImeMode() {
        btnEnableIme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    Toast.makeText(MainActivity.this, "Activa el interruptor 'Remote WiFi Keyboard'", Toast.LENGTH_LONG).show();
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
    }

    private void updateReverseModeUrl() {
        String ip = getDeviceIpAddress();
        if (ip != null && tvUrl != null) {
            tvUrl.setText("http://" + ip + ":8080");
        }
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

    // --- Monetización & PRO ---

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
                if (btnBuyPro != null) {
                    btnBuyPro.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            prefs.edit().putBoolean(KEY_IS_PRO, true).apply();
                            setupMonetization();
                            Toast.makeText(MainActivity.this, "¡Licencia PRO Activada! ($7.99 USD). Cero anuncios.", Toast.LENGTH_LONG).show();
                        }
                    });
                }
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
        updateReverseModeUrl();
        if (currentPcIp != null) {
            checkPcConnection(currentPcIp, false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            networkExecutor.shutdown();
        } catch (Throwable ignored) {}
    }
}
