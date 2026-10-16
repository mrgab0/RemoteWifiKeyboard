package com.remotekeyboard;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
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
    private static final String KEY_VIBRATION_INTENSITY = "vibration_intensity";
    private static final String KEY_VIBRATION_DURATION = "vibration_duration";
    private static final String KEY_APP_THEME = "app_theme";

    // Containers & Cards
    private ScrollView rootScrollView;
    private LinearLayout cardServer;
    private LinearLayout cardIme;
    private LinearLayout cardTheme;
    private LinearLayout cardTelemetry;
    private LinearLayout cardTest;
    private LinearLayout layoutProUpgrade;
    private LinearLayout adContainer;

    // Zen Mode Views
    private LinearLayout cardZenSummary;
    private LinearLayout layoutAdvancedSettings;
    private Button btnToggleAdvancedSettings;
    private TextView tvZenSummaryPill;
    private TextView tvZenSummaryDetails;

    // Theme Switcher Buttons
    private LinearLayout themeDarkBtn;
    private LinearLayout themeAmoledBtn;
    private LinearLayout themeLightBtn;
    private TextView tvThemeBadge;
    private TextView tvLabelDark;
    private TextView tvLabelAmoled;
    private TextView tvLabelLight;
    private Button btnExploreBase16;

    // Headers & Labels
    private TextView tvAppTitle;
    private TextView tvAppSubtitle;
    private TextView tvServerCardSubtitle;
    private TextView tvUrl;
    private Button btnCopyUrl;

    private TextView tvImeCardHeader;
    private TextView tvImeStatus;
    private Button btnEnableIme;
    private Button btnSelectIme;

    // Mouse Synergy Controls
    private LinearLayout cardMouseSynergy;
    private TextView tvMouseCardHeader;
    private TextView tvOverlayStatus;
    private Button btnEnableOverlay;
    private TextView tvAccessibilityStatus;
    private Button btnEnableAccessibility;

    private TextView tvThemeCardHeader;
    private TextView tvTelemetryHeader;
    private TextView tvPingBadge;
    private TextView tvWsClients;
    private TextView tvStatus;
    private TextView tvKeystrokes;

    private TextView tvVibSwitchLabel;
    private Switch switchVibration;
    private SeekBar seekBarVibration;
    private TextView tvVibIntensity;
    private SeekBar seekBarVibrationDuration;
    private TextView tvVibDuration;

    private TextView tvTestAreaHeader;
    private TextView tvTestAreaHint;
    private EditText etTestInput;

    private Button btnBuyPro;
    private SharedPreferences prefs;
    private final Handler telemetryHandler = new Handler(Looper.getMainLooper());
    private Runnable telemetryRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DebugLogger.init(this);
        DebugLogger.log("MainActivity onCreate iniciado.");

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
        rootScrollView = findViewById(R.id.rootScrollView);
        cardServer = findViewById(R.id.cardServer);
        cardZenSummary = findViewById(R.id.cardZenSummary);
        layoutAdvancedSettings = findViewById(R.id.layoutAdvancedSettings);
        btnToggleAdvancedSettings = findViewById(R.id.btnToggleAdvancedSettings);
        tvZenSummaryPill = findViewById(R.id.tvZenSummaryPill);
        tvZenSummaryDetails = findViewById(R.id.tvZenSummaryDetails);

        if (btnToggleAdvancedSettings != null && layoutAdvancedSettings != null) {
            btnToggleAdvancedSettings.setOnClickListener(v -> {
                boolean isVisible = layoutAdvancedSettings.getVisibility() == View.VISIBLE;
                layoutAdvancedSettings.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                btnToggleAdvancedSettings.setText(isVisible ? "⚙️ CONFIGURACIÓN Y PERMISOS ▼" : "⚙️ OCULTAR CONFIGURACIÓN ▲");
            });
        }

        cardIme = findViewById(R.id.cardIme);
        cardTheme = findViewById(R.id.cardTheme);
        cardTelemetry = findViewById(R.id.cardTelemetry);
        cardTest = findViewById(R.id.cardTest);
        layoutProUpgrade = findViewById(R.id.layoutProUpgrade);
        adContainer = findViewById(R.id.adContainer);

        themeDarkBtn = findViewById(R.id.themeDarkBtn);
        themeAmoledBtn = findViewById(R.id.themeAmoledBtn);
        themeLightBtn = findViewById(R.id.themeLightBtn);
        tvThemeBadge = findViewById(R.id.tvThemeBadge);
        tvLabelDark = findViewById(R.id.tvLabelDark);
        tvLabelAmoled = findViewById(R.id.tvLabelAmoled);
        tvLabelLight = findViewById(R.id.tvLabelLight);

        tvAppTitle = findViewById(R.id.tvAppTitle);
        tvAppSubtitle = findViewById(R.id.tvAppSubtitle);
        tvServerCardSubtitle = findViewById(R.id.tvServerCardSubtitle);
        tvUrl = findViewById(R.id.tvUrl);
        btnCopyUrl = findViewById(R.id.btnCopyUrl);

        tvImeCardHeader = findViewById(R.id.tvImeCardHeader);
        tvImeStatus = findViewById(R.id.tvImeStatus);
        btnEnableIme = findViewById(R.id.btnEnableIme);
        btnSelectIme = findViewById(R.id.btnSelectIme);

        // Mouse Synergy
        cardMouseSynergy = findViewById(R.id.cardMouseSynergy);
        tvMouseCardHeader = findViewById(R.id.tvMouseCardHeader);
        tvOverlayStatus = findViewById(R.id.tvOverlayStatus);
        btnEnableOverlay = findViewById(R.id.btnEnableOverlay);
        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus);
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility);

        tvThemeCardHeader = findViewById(R.id.tvThemeCardHeader);
        tvTelemetryHeader = findViewById(R.id.tvTelemetryHeader);
        tvPingBadge = findViewById(R.id.tvPingBadge);
        tvWsClients = findViewById(R.id.tvWsClients);
        tvStatus = findViewById(R.id.tvStatus);
        tvKeystrokes = findViewById(R.id.tvKeystrokes);

        tvVibSwitchLabel = findViewById(R.id.tvVibSwitchLabel);
        switchVibration = findViewById(R.id.switchVibration);
        seekBarVibration = findViewById(R.id.seekBarVibration);
        tvVibIntensity = findViewById(R.id.tvVibIntensity);
        seekBarVibrationDuration = findViewById(R.id.seekBarVibrationDuration);
        tvVibDuration = findViewById(R.id.tvVibDuration);

        tvTestAreaHeader = findViewById(R.id.tvTestAreaHeader);
        tvTestAreaHint = findViewById(R.id.tvTestAreaHint);
        etTestInput = findViewById(R.id.etTestInput);
        btnBuyPro = findViewById(R.id.btnBuyPro);

        btnExploreBase16 = findViewById(R.id.btnExploreBase16);
        if (btnExploreBase16 != null) {
            btnExploreBase16.setOnClickListener(v -> {
                com.remotekeyboard.theme.Base16ThemeDialog.show(MainActivity.this, theme -> {
                    Toast.makeText(MainActivity.this, "Esquema Base16 aplicado: " + theme.getName(), Toast.LENGTH_SHORT).show();
                    RemoteInputMethodService ime = RemoteInputMethodService.getInstance();
                    if (ime != null) {
                        ime.applyBase16Theme(theme);
                    }
                    if (tvThemeBadge != null) {
                        tvThemeBadge.setText(theme.getName().toUpperCase());
                        tvThemeBadge.setTextColor(theme.getAccentColor());
                    }
                    if (etTestInput != null) {
                        etTestInput.setBackgroundColor(theme.getBackgroundColor());
                        etTestInput.setTextColor(theme.getTextColor());
                        etTestInput.setHintTextColor(theme.getBase(0x3));
                    }
                });
            });
        }

        // Theme Switcher listeners
        themeDarkBtn.setOnClickListener(v -> applyAppTheme("dark", true));
        themeAmoledBtn.setOnClickListener(v -> applyAppTheme("amoled", true));
        themeLightBtn.setOnClickListener(v -> applyAppTheme("light", true));

        // Cargar tema guardado
        String savedTheme = prefs.getString(KEY_APP_THEME, "dark");
        applyAppTheme(savedTheme, false);

        String savedBase16 = prefs.getString("base16_selected_theme_id", null);
        if (savedBase16 != null) {
            com.remotekeyboard.theme.Base16Theme b16 = com.remotekeyboard.theme.Base16ThemeManager.getInstance().getThemeById(this, savedBase16);
            if (b16 != null && tvThemeBadge != null) {
                tvThemeBadge.setText(b16.getName().toUpperCase());
                tvThemeBadge.setTextColor(b16.getAccentColor());
            }
        }

        // Configuración de Switch de Vibración
        boolean vibEnabled = prefs.getBoolean(KEY_VIBRATION, true);
        switchVibration.setChecked(vibEnabled);
        switchVibration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_VIBRATION, isChecked).apply();
                if (isChecked) {
                    Toast.makeText(MainActivity.this, "Vibración activada", Toast.LENGTH_SHORT).show();
                    buttonView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                } else {
                    Toast.makeText(MainActivity.this, "Vibración desactivada (Máxima velocidad)", Toast.LENGTH_SHORT).show();
                }
            }
        });

        int currentIntensity = prefs.getInt(KEY_VIBRATION_INTENSITY, 100);
        seekBarVibration.setProgress(currentIntensity);
        tvVibIntensity.setText("Intensidad: " + currentIntensity + "%");

        int currentDuration = prefs.getInt(KEY_VIBRATION_DURATION, 50);
        seekBarVibrationDuration.setProgress(currentDuration);
        tvVibDuration.setText("Duración: " + currentDuration + "%");

        seekBarVibration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvVibIntensity.setText("Intensidad: " + progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                prefs.edit().putInt(KEY_VIBRATION_INTENSITY, progress).apply();
                testVibration();
            }
        });

        seekBarVibrationDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvVibDuration.setText("Duración: " + progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                prefs.edit().putInt(KEY_VIBRATION_DURATION, progress).apply();
                testVibration();
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

        if (btnEnableOverlay != null) {
            btnEnableOverlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:" + getPackageName()));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            Toast.makeText(MainActivity.this, "Permite 'Mostrar sobre otras aplicaciones'", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            try {
                                startActivity(new Intent(Settings.ACTION_SETTINGS));
                            } catch (Exception ignored) {}
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Permiso no necesario en esta versión", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (btnEnableAccessibility != null) {
            btnEnableAccessibility.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        Toast.makeText(MainActivity.this, "Busca 'Remote WiFi Keyboard' y activa el servicio", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        try {
                            startActivity(new Intent(Settings.ACTION_SETTINGS));
                        } catch (Exception ignored) {}
                    }
                }
            });
        }

        btnBuyPro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.edit().putBoolean(KEY_IS_PRO, true).apply();
                setupMonetization();
                Toast.makeText(MainActivity.this, "¡Licencia PRO Activada ($7.99 USD)! Cero anuncios.", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void applyAppTheme(String themeKey, boolean userTriggered) {
        if (themeKey == null) themeKey = "dark";

        if (userTriggered) {
            prefs.edit().putString(KEY_APP_THEME, themeKey).apply();
            RemoteInputMethodService ime = RemoteInputMethodService.getInstance();
            if (ime != null) {
                ime.setExternalTheme(themeKey);
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            try {
                if ("amoled".equalsIgnoreCase(themeKey)) {
                    getWindow().setStatusBarColor(Color.BLACK);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        getWindow().getDecorView().setSystemUiVisibility(0);
                    }
                } else if ("light".equalsIgnoreCase(themeKey)) {
                    getWindow().setStatusBarColor(Color.parseColor("#F1F5F9"));
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    }
                } else {
                    getWindow().setStatusBarColor(Color.parseColor("#060E20"));
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        getWindow().getDecorView().setSystemUiVisibility(0);
                    }
                }
            } catch (Exception ignored) {}
        }

        if ("amoled".equalsIgnoreCase(themeKey)) {
            // --- TEMA AMOLED DARK BATTERY SAVER (Negros puros #000000) ---
            if (rootScrollView != null) rootScrollView.setBackgroundColor(Color.BLACK);
            if (cardServer != null) cardServer.setBackgroundResource(R.drawable.bg_card_amoled);
            if (cardZenSummary != null) cardZenSummary.setBackgroundResource(R.drawable.bg_card_amoled);
            if (cardIme != null) cardIme.setBackgroundResource(R.drawable.bg_card_amoled);
            if (cardMouseSynergy != null) cardMouseSynergy.setBackgroundResource(R.drawable.bg_card_amoled);
            if (cardTheme != null) cardTheme.setBackgroundResource(R.drawable.bg_card_amoled);
            if (cardTelemetry != null) cardTelemetry.setBackgroundResource(R.drawable.bg_card_amoled);
            if (cardTest != null) cardTest.setBackgroundResource(R.drawable.bg_card_amoled);
            if (layoutProUpgrade != null) layoutProUpgrade.setBackgroundResource(R.drawable.bg_card_amoled);

            if (tvAppTitle != null) tvAppTitle.setTextColor(Color.WHITE);
            if (tvAppSubtitle != null) tvAppSubtitle.setTextColor(Color.parseColor("#A3A3A3"));
            if (tvServerCardSubtitle != null) tvServerCardSubtitle.setTextColor(Color.parseColor("#A3A3A3"));
            if (tvUrl != null) tvUrl.setTextColor(Color.parseColor("#00F0FF"));
            if (btnCopyUrl != null) btnCopyUrl.setTextColor(Color.BLACK);

            if (tvImeCardHeader != null) tvImeCardHeader.setTextColor(Color.parseColor("#E2E8F0"));
            if (tvMouseCardHeader != null) tvMouseCardHeader.setTextColor(Color.parseColor("#E2E8F0"));
            if (btnEnableIme != null) {
                btnEnableIme.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#111111")));
                btnEnableIme.setTextColor(Color.WHITE);
            }
            if (btnSelectIme != null) {
                btnSelectIme.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#111111")));
                btnSelectIme.setTextColor(Color.WHITE);
            }
            if (btnEnableOverlay != null) {
                btnEnableOverlay.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#111111")));
                btnEnableOverlay.setTextColor(Color.WHITE);
            }
            if (btnEnableAccessibility != null) {
                btnEnableAccessibility.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#111111")));
                btnEnableAccessibility.setTextColor(Color.WHITE);
            }
            if (btnExploreBase16 != null) {
                btnExploreBase16.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#111111")));
                btnExploreBase16.setTextColor(Color.WHITE);
            }

            if (tvThemeCardHeader != null) tvThemeCardHeader.setTextColor(Color.parseColor("#00FF88"));
            if (tvThemeBadge != null) {
                tvThemeBadge.setText("AMOLED SAVER");
                tvThemeBadge.setTextColor(Color.parseColor("#00FF88"));
            }

            if (tvTelemetryHeader != null) tvTelemetryHeader.setTextColor(Color.parseColor("#00F0FF"));
            if (tvVibSwitchLabel != null) tvVibSwitchLabel.setTextColor(Color.parseColor("#E2E8F0"));
            if (tvVibIntensity != null) tvVibIntensity.setTextColor(Color.parseColor("#A3A3A3"));
            if (tvVibDuration != null) tvVibDuration.setTextColor(Color.parseColor("#A3A3A3"));

            if (tvTestAreaHeader != null) tvTestAreaHeader.setTextColor(Color.parseColor("#00F0FF"));
            if (tvTestAreaHint != null) tvTestAreaHint.setTextColor(Color.parseColor("#737373"));
            if (etTestInput != null) {
                etTestInput.setBackgroundColor(Color.parseColor("#050505"));
                etTestInput.setTextColor(Color.WHITE);
                etTestInput.setHintTextColor(Color.parseColor("#444444"));
            }

            if (themeDarkBtn != null) themeDarkBtn.setBackgroundResource(R.drawable.btn_theme_unselected);
            if (tvLabelDark != null) tvLabelDark.setTextColor(Color.parseColor("#94A3B8"));
            if (themeAmoledBtn != null) themeAmoledBtn.setBackgroundResource(R.drawable.btn_theme_selected);
            if (tvLabelAmoled != null) tvLabelAmoled.setTextColor(Color.parseColor("#00FF88"));
            if (themeLightBtn != null) themeLightBtn.setBackgroundResource(R.drawable.btn_theme_unselected);
            if (tvLabelLight != null) tvLabelLight.setTextColor(Color.parseColor("#94A3B8"));

            if (userTriggered) {
                Toast.makeText(this, "Tema AMOLED Dark Battery Saver activado (Negros puros)", Toast.LENGTH_SHORT).show();
            }

        } else if ("light".equalsIgnoreCase(themeKey)) {
            // --- TEMA CLARO (Minimalist Light #F1F5F9) ---
            if (rootScrollView != null) rootScrollView.setBackgroundColor(Color.parseColor("#F1F5F9"));
            if (cardServer != null) cardServer.setBackgroundResource(R.drawable.bg_card_light);
            if (cardZenSummary != null) cardZenSummary.setBackgroundResource(R.drawable.bg_card_light);
            if (cardIme != null) cardIme.setBackgroundResource(R.drawable.bg_card_light);
            if (cardMouseSynergy != null) cardMouseSynergy.setBackgroundResource(R.drawable.bg_card_light);
            if (cardTheme != null) cardTheme.setBackgroundResource(R.drawable.bg_card_light);
            if (cardTelemetry != null) cardTelemetry.setBackgroundResource(R.drawable.bg_card_light);
            if (cardTest != null) cardTest.setBackgroundResource(R.drawable.bg_card_light);
            if (layoutProUpgrade != null) layoutProUpgrade.setBackgroundResource(R.drawable.bg_card_light);

            if (tvAppTitle != null) tvAppTitle.setTextColor(Color.parseColor("#0F172A"));
            if (tvAppSubtitle != null) tvAppSubtitle.setTextColor(Color.parseColor("#64748B"));
            if (tvServerCardSubtitle != null) tvServerCardSubtitle.setTextColor(Color.parseColor("#64748B"));
            if (tvUrl != null) tvUrl.setTextColor(Color.parseColor("#0284C7"));
            if (btnCopyUrl != null) btnCopyUrl.setTextColor(Color.WHITE);

            if (tvImeCardHeader != null) tvImeCardHeader.setTextColor(Color.parseColor("#0F172A"));
            if (tvMouseCardHeader != null) tvMouseCardHeader.setTextColor(Color.parseColor("#0F172A"));
            if (btnEnableIme != null) {
                btnEnableIme.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));
                btnEnableIme.setTextColor(Color.parseColor("#0F172A"));
            }
            if (btnSelectIme != null) {
                btnSelectIme.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));
                btnSelectIme.setTextColor(Color.parseColor("#0F172A"));
            }
            if (btnEnableOverlay != null) {
                btnEnableOverlay.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));
                btnEnableOverlay.setTextColor(Color.parseColor("#0F172A"));
            }
            if (btnEnableAccessibility != null) {
                btnEnableAccessibility.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));
                btnEnableAccessibility.setTextColor(Color.parseColor("#0F172A"));
            }
            if (btnExploreBase16 != null) {
                btnExploreBase16.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));
                btnExploreBase16.setTextColor(Color.parseColor("#0F172A"));
            }

            if (tvThemeCardHeader != null) tvThemeCardHeader.setTextColor(Color.parseColor("#0284C7"));
            if (tvThemeBadge != null) {
                tvThemeBadge.setText("LIGHT THEME");
                tvThemeBadge.setTextColor(Color.parseColor("#0284C7"));
            }

            if (tvTelemetryHeader != null) tvTelemetryHeader.setTextColor(Color.parseColor("#0284C7"));
            if (tvVibSwitchLabel != null) tvVibSwitchLabel.setTextColor(Color.parseColor("#0F172A"));
            if (tvVibIntensity != null) tvVibIntensity.setTextColor(Color.parseColor("#64748B"));
            if (tvVibDuration != null) tvVibDuration.setTextColor(Color.parseColor("#64748B"));

            if (tvTestAreaHeader != null) tvTestAreaHeader.setTextColor(Color.parseColor("#4F46E5"));
            if (tvTestAreaHint != null) tvTestAreaHint.setTextColor(Color.parseColor("#64748B"));
            if (etTestInput != null) {
                etTestInput.setBackgroundColor(Color.parseColor("#F8FAFC"));
                etTestInput.setTextColor(Color.parseColor("#0F172A"));
                etTestInput.setHintTextColor(Color.parseColor("#94A3B8"));
            }

            if (themeDarkBtn != null) themeDarkBtn.setBackgroundResource(R.drawable.btn_theme_unselected);
            if (tvLabelDark != null) tvLabelDark.setTextColor(Color.parseColor("#64748B"));
            if (themeAmoledBtn != null) themeAmoledBtn.setBackgroundResource(R.drawable.btn_theme_unselected);
            if (tvLabelAmoled != null) tvLabelAmoled.setTextColor(Color.parseColor("#64748B"));
            if (themeLightBtn != null) themeLightBtn.setBackgroundResource(R.drawable.btn_theme_selected);
            if (tvLabelLight != null) tvLabelLight.setTextColor(Color.parseColor("#0284C7"));

            if (userTriggered) {
                Toast.makeText(this, "Tema Claro (Light Theme) activado", Toast.LENGTH_SHORT).show();
            }

        } else {
            // --- TEMA OSCURO NORMAL (Cyber-Dark / Electron Style #060E20) ---
            if (rootScrollView != null) rootScrollView.setBackgroundColor(Color.parseColor("#060E20"));
            if (cardServer != null) cardServer.setBackgroundResource(R.drawable.bg_card_cyber_dark);
            if (cardZenSummary != null) cardZenSummary.setBackgroundResource(R.drawable.bg_card_cyber_dark);
            if (cardIme != null) cardIme.setBackgroundResource(R.drawable.bg_card_cyber_dark);
            if (cardMouseSynergy != null) cardMouseSynergy.setBackgroundResource(R.drawable.bg_card_cyber_dark);
            if (cardTheme != null) cardTheme.setBackgroundResource(R.drawable.bg_card_cyber_dark);
            if (cardTelemetry != null) cardTelemetry.setBackgroundResource(R.drawable.bg_card_cyber_dark);
            if (cardTest != null) cardTest.setBackgroundResource(R.drawable.bg_card_cyber_dark);
            if (layoutProUpgrade != null) layoutProUpgrade.setBackgroundResource(R.drawable.bg_card_cyber_dark);

            if (tvAppTitle != null) tvAppTitle.setTextColor(Color.WHITE);
            if (tvAppSubtitle != null) tvAppSubtitle.setTextColor(Color.parseColor("#94A3B8"));
            if (tvServerCardSubtitle != null) tvServerCardSubtitle.setTextColor(Color.parseColor("#94A3B8"));
            if (tvUrl != null) tvUrl.setTextColor(Color.parseColor("#00F0FF"));
            if (btnCopyUrl != null) btnCopyUrl.setTextColor(Color.parseColor("#060E20"));

            if (tvImeCardHeader != null) tvImeCardHeader.setTextColor(Color.parseColor("#E2E8F0"));
            if (tvMouseCardHeader != null) tvMouseCardHeader.setTextColor(Color.parseColor("#E2E8F0"));
            if (btnEnableIme != null) {
                btnEnableIme.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#162444")));
                btnEnableIme.setTextColor(Color.WHITE);
            }
            if (btnSelectIme != null) {
                btnSelectIme.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#162444")));
                btnSelectIme.setTextColor(Color.WHITE);
            }
            if (btnEnableOverlay != null) {
                btnEnableOverlay.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#162444")));
                btnEnableOverlay.setTextColor(Color.WHITE);
            }
            if (btnEnableAccessibility != null) {
                btnEnableAccessibility.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#162444")));
                btnEnableAccessibility.setTextColor(Color.WHITE);
            }
            if (btnExploreBase16 != null) {
                btnExploreBase16.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#162444")));
                btnExploreBase16.setTextColor(Color.WHITE);
            }

            if (tvThemeCardHeader != null) tvThemeCardHeader.setTextColor(Color.parseColor("#00F0FF"));
            if (tvThemeBadge != null) {
                tvThemeBadge.setText("CYBER-DARK");
                tvThemeBadge.setTextColor(Color.parseColor("#00F0FF"));
            }

            if (tvTelemetryHeader != null) tvTelemetryHeader.setTextColor(Color.parseColor("#38BDF8"));
            if (tvVibSwitchLabel != null) tvVibSwitchLabel.setTextColor(Color.parseColor("#E2E8F0"));
            if (tvVibIntensity != null) tvVibIntensity.setTextColor(Color.parseColor("#94A3B8"));
            if (tvVibDuration != null) tvVibDuration.setTextColor(Color.parseColor("#94A3B8"));

            if (tvTestAreaHeader != null) tvTestAreaHeader.setTextColor(Color.parseColor("#818CF8"));
            if (tvTestAreaHint != null) tvTestAreaHint.setTextColor(Color.parseColor("#64748B"));
            if (etTestInput != null) {
                etTestInput.setBackgroundColor(Color.parseColor("#0A1224"));
                etTestInput.setTextColor(Color.WHITE);
                etTestInput.setHintTextColor(Color.parseColor("#475569"));
            }

            if (themeDarkBtn != null) themeDarkBtn.setBackgroundResource(R.drawable.btn_theme_selected);
            if (tvLabelDark != null) tvLabelDark.setTextColor(Color.parseColor("#00F0FF"));
            if (themeAmoledBtn != null) themeAmoledBtn.setBackgroundResource(R.drawable.btn_theme_unselected);
            if (tvLabelAmoled != null) tvLabelAmoled.setTextColor(Color.parseColor("#94A3B8"));
            if (themeLightBtn != null) themeLightBtn.setBackgroundResource(R.drawable.btn_theme_unselected);
            if (tvLabelLight != null) tvLabelLight.setTextColor(Color.parseColor("#94A3B8"));

            if (userTriggered) {
                Toast.makeText(this, "Tema Oscuro Cyber-Dark activado", Toast.LENGTH_SHORT).show();
            }
        }
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
                    tvWsClients.setText("• Conexión PC: 🟢 " + wsCount + " Cliente(s) Conectado(s) (<3ms)");
                    tvWsClients.setTextColor(Color.parseColor("#34D399"));
                } else {
                    tvWsClients.setText("• Conexión PC: ⚪ Esperando conexión desde PC / Navegador...");
                    String curTheme = prefs.getString(KEY_APP_THEME, "dark");
                    tvWsClients.setTextColor("light".equalsIgnoreCase(curTheme) ? Color.parseColor("#64748B") : Color.parseColor("#94A3B8"));
                }
            }

            // 2. Estado Teclado IME
            if (ime != null) {
                tvImeStatus.setText("• Teclado IME: 🟢 Servicio Activo y Vinculado");
                tvImeStatus.setTextColor(Color.parseColor("#34D399"));
                tvKeystrokes.setText("• Pulsaciones Recibidas: " + ime.getTotalKeysTyped() + " teclas");
                String curTheme = prefs.getString(KEY_APP_THEME, "dark");
                tvKeystrokes.setTextColor("light".equalsIgnoreCase(curTheme) ? Color.parseColor("#0F172A") : Color.parseColor("#E2E8F0"));
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

            // 3. Permisos de Mouse Synergy
            if (tvOverlayStatus != null) {
                boolean canDraw = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
                if (canDraw) {
                    tvOverlayStatus.setText("• Superposición: 🟢 Concedido");
                    tvOverlayStatus.setTextColor(Color.parseColor("#34D399"));
                    if (btnEnableOverlay != null) btnEnableOverlay.setText("✓ Superposición Concedida");
                } else {
                    tvOverlayStatus.setText("• Superposición: 🔴 Falta Permiso");
                    tvOverlayStatus.setTextColor(Color.parseColor("#F87171"));
                    if (btnEnableOverlay != null) btnEnableOverlay.setText("1. Permitir Superposición (Dibujar Puntero)");
                }
            }

            if (tvAccessibilityStatus != null) {
                boolean accessActive = com.remotekeyboard.accessibility.RemoteAccessibilityService.getInstance() != null;
                if (accessActive) {
                    tvAccessibilityStatus.setText("• Accesibilidad: 🟢 Activo (Gestos Listos)");
                    tvAccessibilityStatus.setTextColor(Color.parseColor("#34D399"));
                    if (btnEnableAccessibility != null) btnEnableAccessibility.setText("✓ Accesibilidad Activada");
                } else {
                    tvAccessibilityStatus.setText("• Accesibilidad: 🔴 Desactivado en Ajustes");
                    tvAccessibilityStatus.setTextColor(Color.parseColor("#F87171"));
                    if (btnEnableAccessibility != null) btnEnableAccessibility.setText("2. Activar Accesibilidad (Simular Toques)");
                }
            }

            // 4. Actualizar Estado Global Zen (Dashboard simplificado)
            if (tvZenSummaryPill != null && tvZenSummaryDetails != null) {
                boolean imeOk = (ime != null) || isImeEnabled();
                boolean overlayOk = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
                boolean accessOk = com.remotekeyboard.accessibility.RemoteAccessibilityService.getInstance() != null;

                int activeServices = (imeOk ? 1 : 0) + (overlayOk ? 1 : 0) + (accessOk ? 1 : 0);
                if (activeServices == 3) {
                    tvZenSummaryPill.setText("3/3 SERVICIOS LISTOS");
                    tvZenSummaryPill.setTextColor(Color.parseColor("#00FF88"));
                    tvZenSummaryDetails.setText("Teclado, Mouse Synergy y Accesibilidad activos en segundo plano.");
                } else {
                    tvZenSummaryPill.setText(activeServices + "/3 SERVICIOS LISTOS");
                    tvZenSummaryPill.setTextColor(Color.parseColor("#FBBF24"));
                    tvZenSummaryDetails.setText("Se requiere configurar " + (3 - activeServices) + " permiso(s). Toca abajo para ver detalles.");
                }
            }
        } catch (Exception ignored) {}
    }

    private void testVibration() {
        int intensity = prefs.getInt(KEY_VIBRATION_INTENSITY, 100);
        int durationPercent = prefs.getInt(KEY_VIBRATION_DURATION, 50);

        android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator() && intensity > 0) {
            int duration = 5 + (int) (145 * (durationPercent / 100.0f)); // 5ms a 150ms
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                int amplitude = 1 + (int) (254 * (intensity / 100.0f));
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(duration, amplitude));
            } else {
                vibrator.vibrate(duration);
            }
        }
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
                String fullUrl = "http://" + ip + ":" + RemoteWebServerManager.SERVER_PORT;
                tvUrl.setText(fullUrl);
                tvStatus.setText("• Servidor: 🟢 Activo (NanoHTTPD + WebSocket)");
                tvStatus.setTextColor(0xFF10B981);
            } else {
                tvUrl.setText("Sin conexión Wi-Fi");
                tvStatus.setText("• Servidor: ○ Conéctate a una red Wi-Fi");
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
