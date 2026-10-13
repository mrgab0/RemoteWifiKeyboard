package com.remotekeyboard;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * Servicio de Accesibilidad pasivo para acciones globales del sistema.
 * Cumple estrictamente con las políticas de Google Play Store:
 * - canRetrieveWindowContent = false (No lee datos en pantalla).
 * - Cero captura de eventos de texto o pantalla (0% CPU).
 * - Uso exclusivo para navegación: Multitarea (Recientes), Atrás, Inicio.
 */
public class RemoteAccessibilityService extends AccessibilityService {

    private static final String TAG = "RemoteAccessibility";
    private static RemoteAccessibilityService instance;

    public static synchronized RemoteAccessibilityService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "🟢 RemoteAccessibilityService conectado con éxito.");
        DebugLogger.log("RemoteAccessibilityService CONECTADO (Navegación nativa activada)");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Pasivo: Cero procesamiento de eventos para garantizar 0% consumo de CPU y máxima privacidad
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "RemoteAccessibilityService interrumpido");
        instance = null;
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        instance = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    public boolean triggerBack() {
        return performGlobalAction(GLOBAL_ACTION_BACK);
    }

    public boolean triggerRecents() {
        return performGlobalAction(GLOBAL_ACTION_RECENTS);
    }

    public boolean triggerHome() {
        return performGlobalAction(GLOBAL_ACTION_HOME);
    }

    public boolean triggerNotifications() {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
    }
}
