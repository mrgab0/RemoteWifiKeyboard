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
    private int currentOverlayType = 0;

    // Para soporte de arrastre (drag)
    private float dragStartX = 0;
    private float dragStartY = 0;
    private long dragStartTime = 0;
    private boolean isDragging = false;

    // Detección de elementos interactivos (icono de mano / pointer)
    private long lastHoverCheckTime = 0;
    private boolean isHandCursor = false;

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
        RemoteAccessibilityService accessService = RemoteAccessibilityService.getInstance();
        int desiredType = (accessService != null)
                ? WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                : (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE);

        if (isOverlayAttached) {
            // Si ya está adjunto pero podemos ascender a TYPE_ACCESSIBILITY_OVERLAY, re-adjuntamos
            if (currentOverlayType != WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY 
                    && desiredType == WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY) {
                try {
                    if (windowManager != null && pointerView != null) {
                        windowManager.removeView(pointerView);
                    }
                } catch (Exception ignored) {}
                isOverlayAttached = false;
            } else {
                return;
            }
        }

        if (desiredType != WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(context)) {
            Log.w(TAG, "No se puede mostrar el puntero: falta permiso SYSTEM_ALERT_WINDOW");
            return;
        }

        try {
            updateScreenDimensions();

            WindowManager wm = null;
            if (accessService != null) {
                wm = (WindowManager) accessService.getSystemService(Context.WINDOW_SERVICE);
            }
            if (wm == null) {
                wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            }
            if (wm != null) {
                windowManager = wm;
            }

            currentOverlayType = desiredType;
            layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    currentOverlayType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT
            );

            layoutParams.gravity = Gravity.TOP | Gravity.START;
            layoutParams.x = (int) currentX;
            layoutParams.y = (int) currentY;

            Context viewContext = accessService != null ? accessService : context;
            pointerView = new ImageView(viewContext);
            pointerView.setImageResource(isHandCursor ? R.drawable.ic_mouse_pointer_hand : R.drawable.ic_mouse_pointer);
            pointerView.setVisibility(View.GONE);

            windowManager.addView(pointerView, layoutParams);
            isOverlayAttached = true;
            DebugLogger.log("Overlay de puntero en capa superior absoluta (tipo: " + 
                (currentOverlayType == WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY ? "ACCESSIBILITY_OVERLAY" : "APPLICATION_OVERLAY") + ")");
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

            // Detección reactiva de elementos interactivos (botones, enlaces, inputs -> manito)
            long now = System.currentTimeMillis();
            if (now - lastHoverCheckTime > 75) {
                lastHoverCheckTime = now;
                RemoteAccessibilityService accessService = RemoteAccessibilityService.getInstance();
                float statusBarThreshold = Math.max(80f, screenHeight * 0.045f);
                float navBarThreshold = Math.max(120f, screenHeight * 0.065f);
                boolean inSystemBar = currentY <= statusBarThreshold || currentY >= screenHeight - navBarThreshold;
                boolean shouldBeHand = (accessService != null && accessService.isInteractiveElementAt(currentX, currentY)) || inSystemBar;
                if (shouldBeHand != isHandCursor) {
                    isHandCursor = shouldBeHand;
                    pointerView.setImageResource(isHandCursor ? R.drawable.ic_mouse_pointer_hand : R.drawable.ic_mouse_pointer);
                }
            }
        });
    }

    /**
     * Procesa eventos de clic de ratón (Izquierdo: Tap / Drag, Derecho: Atrás, Central: Home).
     */
    public void onMouseClick(final int button, final String action) {
        mainHandler.post(() -> {
            RemoteAccessibilityService accessService = RemoteAccessibilityService.getInstance();
            if (accessService == null) {
                DebugLogger.log("⚠️ Clic falló: RemoteAccessibilityService es NULL. Activa el servicio en Ajustes -> Accesibilidad.");
                return;
            }

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

                        float statusBarThreshold = Math.max(80f, screenHeight * 0.045f);
                        float navBarThreshold = Math.max(120f, screenHeight * 0.065f);

                        // Si el ratón no se desplazó significativamente (< 25px), siempre es un Tap
                        if (distSq < 625) {
                            if (duration > 500) {
                                if (!accessService.clickNodeAt(currentX, currentY)) {
                                    accessService.dispatchLongClick(currentX, currentY);
                                }
                                DebugLogger.log("Mouse Long Click en (" + (int) currentX + ", " + (int) currentY + ")");
                            } else {
                                executeSmartClick(accessService, currentX, currentY, statusBarThreshold, navBarThreshold);
                            }
                        } else {
                            // Arrastre real (Drag / Swipe con desplazamiento)
                            if (dragStartY <= statusBarThreshold && (currentY - dragStartY) > 50) {
                                if ((currentY - dragStartY) > screenHeight * 0.25f) {
                                    accessService.performQuickSettings();
                                    DebugLogger.log("Mouse Drag desde barra superior: Ajustes Rápidos");
                                } else {
                                    accessService.performNotifications();
                                    DebugLogger.log("Mouse Drag desde barra superior: Notificaciones");
                                }
                            } else if (dragStartY >= screenHeight - navBarThreshold && (dragStartY - currentY) > 50) {
                                accessService.performHome();
                                DebugLogger.log("Mouse Drag desde barra inferior: Home");
                            } else {
                                accessService.dispatchDrag(dragStartX, dragStartY, currentX, currentY, Math.max(100, duration));
                                DebugLogger.log("Mouse Drag desde (" + (int) dragStartX + ", " + (int) dragStartY + ") a (" + (int) currentX + ", " + (int) currentY + ")");
                            }
                        }
                    }
                }
            }
            // Botón 2: Clic Derecho (Atrás)
            else if (button == 2) {
                if (isDown) {
                    accessService.performBack();
                    DebugLogger.log("Mouse Clic Derecho: Atrás ejecutado");
                }
            }
            // Botón 1: Clic Central (Inicio / Home)
            else if (button == 1) {
                if (isDown) {
                    accessService.performHome();
                    DebugLogger.log("Mouse Clic Central: Inicio (Home) ejecutado");
                }
            }
        });
    }

    /**
     * Ejecuta un clic inteligente sobre la pantalla de Android:
     * 1. Primero intenta activar el nodo interactivo de accesibilidad en cualquier ventana (aplicación o sistema).
     * 2. Si es en la barra superior (status bar), despliega notificaciones.
     * 3. Si es en la barra inferior (navigation bar), ejecuta Atrás / Home / Recientes según la posición X.
     * 4. Si es en el cuerpo general, despacha un tap sintético de pantalla táctil.
     */
    private void executeSmartClick(RemoteAccessibilityService accessService, float x, float y, float statusBarThreshold, float navBarThreshold) {
        // 1. Buscar nodo accesible en todas las capas activas (incluyendo SYSTEM windows como botones de UI)
        if (accessService.clickNodeAt(x, y)) {
            DebugLogger.log("Mouse Clic directo en nodo accesible en (" + (int) x + ", " + (int) y + ")");
            return;
        }

        // 2. Zona de barra de estado superior
        if (y <= statusBarThreshold) {
            accessService.performNotifications();
            DebugLogger.log("Mouse Clic en barra de estado -> Notificaciones");
            return;
        }

        // 3. Zona de barra de navegación inferior (3 botones del sistema o pill bar)
        if (y >= screenHeight - navBarThreshold) {
            if (x < screenWidth * 0.33f) {
                accessService.performBack();
                DebugLogger.log("Mouse Clic en barra navegación (Izquierda) -> Atrás");
            } else if (x > screenWidth * 0.67f) {
                accessService.performRecents();
                DebugLogger.log("Mouse Clic en barra navegación (Derecha) -> Recientes");
            } else {
                accessService.performHome();
                DebugLogger.log("Mouse Clic en barra navegación (Centro) -> Inicio");
            }
            return;
        }

        // 4. Tap táctil estándar
        accessService.dispatchTap(x, y);
        DebugLogger.log("Mouse Tap sintético en (" + (int) x + ", " + (int) y + ")");
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
                if (isHandCursor) {
                    isHandCursor = false;
                    pointerView.setImageResource(R.drawable.ic_mouse_pointer);
                }
            }
        });
    }
}
