package com.remotekeyboard.server;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import com.remotekeyboard.DebugLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Gestor para la consulta de aplicaciones instaladas con launcher y
 * extracción en caché de sus iconos reales en formato PNG.
 */
public class AppManager {

    public static class AppEntry {
        public final String name;
        public final String packageName;

        public AppEntry(String name, String packageName) {
            this.name = name;
            this.packageName = packageName;
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("name", name);
                obj.put("package", packageName);
            } catch (Exception ignored) {}
            return obj;
        }
    }

    // Caché LRU en memoria para hasta 120 iconos PNG (~600 KB RAM)
    private static final LruCache<String, byte[]> iconCache = new LruCache<>(120);
    private static List<AppEntry> cachedApps = null;
    private static long lastAppQueryTime = 0;

    /**
     * Obtiene la lista de aplicaciones del lanzador ordenadas alfabéticamente.
     */
    public static synchronized List<AppEntry> getInstalledApps(Context context, boolean forceRefresh) {
        long now = System.currentTimeMillis();
        if (!forceRefresh && cachedApps != null && (now - lastAppQueryTime < 60000)) {
            return cachedApps;
        }

        List<AppEntry> apps = new ArrayList<>();
        try {
            PackageManager pm = context.getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
            String ourPackage = context.getPackageName();

            for (ResolveInfo ri : resolveInfos) {
                if (ri.activityInfo == null) continue;
                String pkg = ri.activityInfo.packageName;
                if (ourPackage.equals(pkg)) continue;

                CharSequence labelSeq = ri.loadLabel(pm);
                String label = labelSeq != null ? labelSeq.toString().trim() : pkg;
                if (!label.isEmpty()) {
                    apps.add(new AppEntry(label, pkg));
                }
            }

            Collections.sort(apps, new Comparator<AppEntry>() {
                @Override
                public int compare(AppEntry a, AppEntry b) {
                    return a.name.compareToIgnoreCase(b.name);
                }
            });

            cachedApps = apps;
            lastAppQueryTime = now;
            DebugLogger.log("AppManager: Consultadas " + apps.size() + " apps con launcher.");
        } catch (Exception e) {
            DebugLogger.log("AppManager error al listar apps: " + e.getMessage());
        }
        return cachedApps != null ? cachedApps : Collections.<AppEntry>emptyList();
    }

    /**
     * Retorna la lista en formato JSONArray para serialización HTTP / WebSocket.
     */
    public static JSONArray getInstalledAppsJson(Context context) {
        List<AppEntry> apps = getInstalledApps(context, false);
        JSONArray arr = new JSONArray();
        for (AppEntry app : apps) {
            arr.put(app.toJson());
        }
        return arr;
    }

    /**
     * Obtiene el icono de la aplicación en PNG (64x64 px).
     */
    public static byte[] getAppIconPng(Context context, String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) return null;

        byte[] cached = iconCache.get(packageName);
        if (cached != null) return cached;

        try {
            PackageManager pm = context.getPackageManager();
            Drawable drawable = pm.getApplicationIcon(packageName);
            if (drawable == null) return null;

            Bitmap bitmap = drawableToBitmap(drawable, 72, 72);
            if (bitmap == null) return null;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, baos);
            byte[] bytes = baos.toByteArray();

            if (bytes.length > 0) {
                iconCache.put(packageName, bytes);
                return bytes;
            }
        } catch (Exception e) {
            DebugLogger.log("AppManager error al obtener icono de " + packageName + ": " + e.getMessage());
        }
        return null;
    }

    private static Bitmap drawableToBitmap(Drawable drawable, int width, int height) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bd = (BitmapDrawable) drawable;
            if (bd.getBitmap() != null) {
                return Bitmap.createScaledBitmap(bd.getBitmap(), width, height, true);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
