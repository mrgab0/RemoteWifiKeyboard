package com.remotekeyboard.theme;

import android.app.Activity;
import android.os.Bundle;
import com.remotekeyboard.RemoteInputMethodService;

/**
 * Actividad transparente tipo Diálogo para explorar y seleccionar esquemas Base16
 * de manera 100% segura desde cualquier contexto (IME, Notificaciones, etc.).
 */
public class ThemePickerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Base16ThemeDialog.show(this, new Base16ThemeDialog.OnThemeSelectedListener() {
            @Override
            public void onThemeSelected(Base16Theme theme) {
                RemoteInputMethodService ime = RemoteInputMethodService.getInstance();
                if (ime != null) {
                    ime.applyBase16Theme(theme);
                }
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isFinishing()) {
            finish();
        }
    }
}
