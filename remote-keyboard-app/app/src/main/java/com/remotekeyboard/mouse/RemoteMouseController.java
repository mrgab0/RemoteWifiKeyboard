package com.remotekeyboard.mouse;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.remotekeyboard.DebugLogger;
import com.remotekeyboard.R;
import com.remotekeyboard.accessibility.RemoteAccessibilityService;

/**
 * Controlador de Puntero Flotante y Gestos de Ratón Remoto.
 * Gestiona el overlay visual del cursor en pantalla y coordina los toques con RemoteAccessibilityService.
 */
public class RemoteMouseController {

    private static final String TAG = "RemoteMouseController";
    private static volatile RemoteMouseController instance;

    private final Context context;
    private final Handler mainHandler;
    private WindowManager windowManager;
    private ImageView pointerView;
    private WindowManager.LayoutParams layoutParams;

    private int screenWidth = 1080;
    private int screenHeight = 2400;
    private float currentX = 540;
    private float currentY = 1200;
    private boolean isOverlayAttached = false;

    // Para soporte de arrastre (drag)
    private float dragStartX = 0;
    private float dragStartY = 0;
    private long dragStartTime = 0;
    private boolean isDragging = false;

    public static synchronized RemoteMouseController getInstance(Context context) {
        if (instance == null) {
            instance = new RemoteMouseController(context.getApplicationContext());
        }
        return instance;
    }

    private RemoteMouseController(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        updateScreenDimensions();
    }

    private void updateScreenDimensions() {
        try {
            windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (windowManager != null) {
                DisplayMetrics metrics = new DisplayMetrics();
                windowManager.getDefaultDisplay().getRealMetrics(metrics);
                this.screenWidth = metrics.widthPixels;
                this.screenHeight = metrics.heightPixels;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error obteniendo dimensiones de pantalla: ", e);
        }
    }

    private void ensureOverlayView() {
        if (isOverlayAttached) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            Log.w(TAG, "No se puede mostrar el puntero: falta permiso SYSTEM_ALERT_WINDOW");
            return;
        }

        try {
            updateScreenDimensions();

            int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;

            layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    overlayType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT
            );

            layoutParams.gravity = Gravity.TOP | Gravity.START;
            layoutParams.x = (int) currentX;
            layoutParams.y = (int) currentY;

            pointerView = new ImageView(context);
            pointerView.setImageResource(R.drawable.ic_mouse_pointer);
            pointerView.setVisibility(View.GONE);

            windowManager.addView(pointerView, layoutParams);
            isOverlayAttached = true;
            DebugLogger.log("Overlay de puntero de ratón inicializado con éxito");
        } catch (Exception e) {
            Log.e(TAG, "Error agregando vista de puntero flotante: ", e);
            isOverlayAttached = false;
        }
    }

    /**
     * Actualiza la posición del puntero mediante coordenadas normalizadas (0.0 a 1.0).
     */
    public void onMouseMove(final float normX, final float normY) {
        mainHandler.post(() -> {
            ensureOverlayView();
            if (pointerView == null || !isOverlayAttached) return;

            currentX = Math.max(0, Math.min(screenWidth - 1, normX * screenWidth));
            currentY = Math.max(0, Math.min(screenHeight - 1, normY * screenHeight));

            layoutParams.x = (int) currentX;
            layoutParams.y = (int) currentY;

            if (pointerView.getVisibility() != View.VISIBLE) {
                pointerView.setVisibility(View.VISIBLE);
            }

            try {
                windowManager.updateViewLayout(pointerView, layoutParams);
            } catch (Exception ignored) {}
        });
    }

    /**
     * Procesa eventos de clic de ratón (Izquierdo: Tap / Drag, Derecho: Atrás).
     */
    public void onMouseClick(final int button, final String action) {
        mainHandler.post(() -> {
            RemoteAccessibilityService accessService = RemoteAccessibilityService.getInstance();
            boolean isDown = "down".equalsIgnoreCase(action);

            // Botón 0: Clic Izquierdo (Toque / Arrastre)
            if (button == 0) {
                if (isDown) {
                    dragStartX = currentX;
                    dragStartY = currentY;
                    dragStartTime = System.currentTimeMillis();
                    isDragging = true;
                } else {
                    if (isDragging) {
                        isDragging = false;
                        long duration = System.currentTimeMillis() - dragStartTime;
                        float distSq = (currentX - dragStartX) * (currentX - dragStartX) + (currentY - dragStartY) * (currentY - dragStartY);

                        if (accessService != null) {
                            if (distSq < 400 && duration < 350) { // Menos de 20px de movimiento = Tap simple
                                accessService.dispatchTap(currentX, currentY);
                                DebugLogger.log("Mouse Tap ejecutado en (" + (int) currentX + ", " + (int) currentY + ")");
                            } else { // Arrastre (Drag)
                                accessService.dispatchDrag(dragStartX, dragStartY, currentX, currentY, duration);
                                DebugLogger.log("Mouse Drag ejecutado desde (" + (int) dragStartX + ", " + (int) dragStartY + ") a (" + (int) currentX + ", " + (int) currentY + ")");
                            }
                        }
                    }
                }
            }
            // Botón 2: Clic Derecho (Atrás)
            else if (button == 2) {
                if (isDown) {
                    if (accessService != null) {
                        accessService.performBack();
                        DebugLogger.log("Mouse Clic Derecho: Botón Atrás ejecutado");
                    }
                }
            }
            // Botón 1: Clic Central (Inicio / Home)
            else if (button == 1) {
                if (isDown) {
                    if (accessService != null) {
                        accessService.performHome();
                        DebugLogger.log("Mouse Clic Central: Inicio (Home) ejecutado");
                    }
                }
            }
        });
    }

    /**
     * Procesa eventos de scroll vertical de la rueda del ratón.
     */
    public void onMouseScroll(final int deltaY) {
        mainHandler.post(() -> {
            RemoteAccessibilityService accessService = RemoteAccessibilityService.getInstance();
            if (accessService != null) {
                accessService.dispatchScroll(currentX, currentY, deltaY);
            }
        });
    }

    /**
     * Oculta el puntero visual cuando el ratón regresa a Windows.
     */
    public void onMouseLeave() {
        mainHandler.post(() -> {
            if (pointerView != null && isOverlayAttached) {
                pointerView.setVisibility(View.GONE);
            }
        });
    }
}
