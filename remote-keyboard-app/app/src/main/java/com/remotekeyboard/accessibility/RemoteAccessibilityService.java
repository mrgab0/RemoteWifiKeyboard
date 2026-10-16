package com.remotekeyboard.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.remotekeyboard.DebugLogger;

/**
 * Servicio de Accesibilidad para inyección física de eventos táctiles
 * (toque, arrastre, scroll y navegación global) enviados desde el ratón de Windows.
 */
public class RemoteAccessibilityService extends AccessibilityService {

    private static final String TAG = "RemoteAccessService";
    private static volatile RemoteAccessibilityService instance;

    public static RemoteAccessibilityService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "RemoteAccessibilityService conectado exitosamente.");
        DebugLogger.log("RemoteAccessibilityService conectado (Gestos habilitados)");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No necesitamos procesar eventos de accesibilidad entrantes
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "RemoteAccessibilityService interrumpido");
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        if (instance == this) {
            instance = null;
        }
        DebugLogger.log("RemoteAccessibilityService desvinculado");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (instance == this) {
            instance = null;
        }
        DebugLogger.log("RemoteAccessibilityService onDestroy");
    }

    /**
     * Realiza un toque instantáneo (tap) en las coordenadas especificadas.
     */
    public boolean dispatchTap(float x, float y) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        try {
            Path path = new Path();
            path.moveTo(x, y);
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 50));
            return dispatchGesture(builder.build(), null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error en dispatchTap: ", e);
            return false;
        }
    }

    /**
     * Realiza un gesto de scroll vertical proporcional al movimiento de la rueda del ratón.
     */
    public boolean dispatchScroll(float x, float y, int deltaY) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        try {
            float distance = 300f * (deltaY > 0 ? 1 : -1);
            float startY = y;
            float endY = Math.max(0, y - distance);

            Path path = new Path();
            path.moveTo(x, startY);
            path.lineTo(x, endY);

            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
            return dispatchGesture(builder.build(), null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error en dispatchScroll: ", e);
            return false;
        }
    }

    /**
     * Realiza un gesto de arrastre sostenido (drag & drop o swipe).
     */
    public boolean dispatchDrag(float startX, float startY, float endX, float endY, long durationMs) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        try {
            Path path = new Path();
            path.moveTo(startX, startY);
            path.lineTo(endX, endY);

            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, Math.max(100, durationMs)));
            return dispatchGesture(builder.build(), null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error en dispatchDrag: ", e);
            return false;
        }
    }

    /**
     * Ejecuta el botón Atrás de Android.
     */
    public boolean performBack() {
        return performGlobalAction(GLOBAL_ACTION_BACK);
    }

    /**
     * Ejecuta el botón de Inicio (Home).
     */
    public boolean performHome() {
        return performGlobalAction(GLOBAL_ACTION_HOME);
    }

    /**
     * Abre el menú de aplicaciones recientes / multitarea.
     */
    public boolean performRecents() {
        return performGlobalAction(GLOBAL_ACTION_RECENTS);
    }

    /**
     * Busca de manera recursiva el primer nodo editable en pantalla (EditText / Prompt de IA).
     */
    private AccessibilityNodeInfo findEditableNode(AccessibilityNodeInfo node) {
        if (node == null) return null;

        CharSequence className = node.getClassName();
        boolean isEditClass = className != null && (
            className.toString().contains("EditText") ||
            className.toString().contains("AutoCompleteTextView") ||
            className.toString().contains("ComposeInput")
        );

        if (node.isEditable() || isEditClass) {
            if (node.isVisibleToUser() && node.isEnabled()) {
                return node;
            }
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo editable = findEditableNode(child);
                if (editable != null) {
                    return editable;
                }
                child.recycle();
            }
        }
        return null;
    }

    /**
     * Salta el cursor y enfoca inmediatamente la primera caja de texto disponible en la app activa.
     */
    public boolean focusFirstEditable() {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) {
                Log.d(TAG, "getRootInActiveWindow retornó null");
                return false;
            }

            AccessibilityNodeInfo target = findEditableNode(root);
            if (target != null) {
                boolean focused = target.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                boolean clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.i(TAG, "Campo editable enfocado exitosamente (focus=" + focused + ", click=" + clicked + ")");
                DebugLogger.log("🎯 Foco asignado a caja de texto: " + target.getClassName());
                target.recycle();
                root.recycle();
                return true;
            }
            root.recycle();
        } catch (Exception e) {
            Log.e(TAG, "Error al enfocar campo editable: ", e);
        }
        return false;
    }

    /**
     * Ejecuta reintentos automáticos no bloqueantes para esperar que la interfaz de la app
     * termine de cargarse e inflarse en pantalla antes de fijar el cursor.
     */
    public void autoFocusWithRetry(final int maxRetries, final long delayMs) {
        final Handler handler = new Handler(Looper.getMainLooper());
        Runnable attempt = new Runnable() {
            int attemptCount = 0;

            @Override
            public void run() {
                attemptCount++;
                if (focusFirstEditable()) {
                    Log.i(TAG, "Auto-foco completado en intento #" + attemptCount);
                } else if (attemptCount < maxRetries) {
                    handler.postDelayed(this, delayMs);
                } else {
                    Log.w(TAG, "No se encontró ningún campo editable tras " + maxRetries + " intentos");
                }
            }
        };
        handler.postDelayed(attempt, delayMs);
    }
}
