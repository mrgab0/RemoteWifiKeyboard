package com.remotekeyboard.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;
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
            path.lineTo(x, y + 1f); // CRÍTICO: Una ruta sin lineTo está vacía en Skia y Android descarta el gesto

            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 75));

            boolean dispatched = dispatchGesture(builder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    DebugLogger.log("Tap COMPLETADO con éxito en (" + (int) x + ", " + (int) y + ")");
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    DebugLogger.log("Tap CANCELADO por Android en (" + (int) x + ", " + (int) y + ")");
                }
            }, null);

            if (!dispatched) {
                clickNodeAt(x, y);
            }
            return dispatched;
        } catch (Exception e) {
            DebugLogger.log("Error en dispatchTap: " + e.getMessage());
            Log.e(TAG, "Error en dispatchTap: ", e);
            clickNodeAt(x, y);
            return false;
        }
    }

    /**
     * Realiza un toque sostenido (long click) en pantalla.
     */
    public boolean dispatchLongClick(float x, float y) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        try {
            Path path = new Path();
            path.moveTo(x, y);
            path.lineTo(x, y + 1f);

            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 550));
            return dispatchGesture(builder.build(), null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error en dispatchLongClick: ", e);
            return false;
        }
    }

    /**
     * Fallback de clic directo mediante AccessibilityNodeInfo.
     */
    public boolean clickNodeAt(float x, float y) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                List<AccessibilityWindowInfo> windows = getWindows();
                if (windows != null && !windows.isEmpty()) {
                    for (AccessibilityWindowInfo window : windows) {
                        Rect bounds = new Rect();
                        window.getBoundsInScreen(bounds);
                        if (bounds.contains((int) x, (int) y)) {
                            AccessibilityNodeInfo root = window.getRoot();
                            if (root != null) {
                                if (clickDeepestNodeAt(root, (int) x, (int) y)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                return clickDeepestNodeAt(root, (int) x, (int) y);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en clickNodeAt: ", e);
        }
        return false;
    }

    private boolean clickDeepestNodeAt(AccessibilityNodeInfo node, int x, int y) {
        if (node == null) return false;
        android.graphics.Rect rect = new android.graphics.Rect();
        node.getBoundsInScreen(rect);
        if (!rect.contains(x, y)) return false;

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null && clickDeepestNodeAt(child, x, y)) {
                return true;
            }
        }

        if (node.isClickable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        return false;
    }

    /**
     * Comprueba si en las coordenadas (x, y) de la pantalla existe un elemento interactivo
     * en cualquier ventana (sistema, notificaciones, barra de navegación o aplicaciones).
     */
    public boolean isInteractiveElementAt(float x, float y) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                List<AccessibilityWindowInfo> windows = getWindows();
                if (windows != null && !windows.isEmpty()) {
                    for (AccessibilityWindowInfo window : windows) {
                        Rect bounds = new Rect();
                        window.getBoundsInScreen(bounds);
                        if (bounds.contains((int) x, (int) y)) {
                            AccessibilityNodeInfo root = window.getRoot();
                            if (root != null && checkNodeClickableAt(root, (int) x, (int) y)) {
                                return true;
                            }
                        }
                    }
                }
            }
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                return checkNodeClickableAt(root, (int) x, (int) y);
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private boolean checkNodeClickableAt(AccessibilityNodeInfo node, int x, int y) {
        if (node == null) return false;
        android.graphics.Rect rect = new android.graphics.Rect();
        node.getBoundsInScreen(rect);
        if (!rect.contains(x, y)) return false;

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null && checkNodeClickableAt(child, x, y)) {
                return true;
            }
        }

        if (node.isClickable() || node.isCheckable() || node.isLongClickable() || node.isEditable()) {
            return true;
        }

        CharSequence cls = node.getClassName();
        if (cls != null) {
            String c = cls.toString();
            if (c.contains("Button") || c.contains("ImageButton") || c.contains("CheckBox") ||
                c.contains("Switch") || c.contains("RadioButton") || (c.contains("TextView") && node.isClickable())) {
                return true;
            }
        }

        for (AccessibilityNodeInfo.AccessibilityAction action : node.getActionList()) {
            if (action.getId() == AccessibilityNodeInfo.ACTION_CLICK) {
                return true;
            }
        }

        return false;
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
     * Despliega la barra y panel de notificaciones de Android.
     */
    public boolean performNotifications() {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
    }

    /**
     * Despliega el panel de ajustes rápidos del sistema.
     */
    public boolean performQuickSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
        }
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
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
