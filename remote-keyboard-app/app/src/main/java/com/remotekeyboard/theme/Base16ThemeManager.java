package com.remotekeyboard.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import com.remotekeyboard.DebugLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gestor singleton para la carga, búsqueda y persistencia de temas Base16.
 */
public class Base16ThemeManager {
    private static final String PREFS_NAME = "RemoteKeyboardPrefs";
    private static final String KEY_BASE16_THEME_ID = "base16_selected_theme_id";
    private static final String ASSET_FILE = "base16_schemes.json";

    private static volatile Base16ThemeManager instance;

    private final List<Base16Theme> allThemes = new ArrayList<>();
    private final List<Base16Theme> featuredThemes = new ArrayList<>();
    private final Map<String, Base16Theme> themesById = new HashMap<>();
    private boolean isLoaded = false;

    // Lista de IDs de esquemas populares para ciclado rápido en el teclado
    private static final List<String> FEATURED_IDS = Arrays.asList(
            "dracula",
            "nord",
            "gruvbox-dark-hard",
            "tokyo-night-dark",
            "catppuccin-mocha",
            "one-dark",
            "monokai",
            "solarized-dark",
            "material-darker",
            "everforest",
            "oceanicnext",
            "ayu-mirage",
            "one-light",
            "solarized-light"
    );

    private Base16ThemeManager() {}

    public static Base16ThemeManager getInstance() {
        if (instance == null) {
            synchronized (Base16ThemeManager.class) {
                if (instance == null) {
                    instance = new Base16ThemeManager();
                }
            }
        }
        return instance;
    }

    public synchronized void ensureLoaded(Context context) {
        if (isLoaded) return;
        try {
            long startTime = System.currentTimeMillis();
            InputStream is = context.getAssets().open(ASSET_FILE);
            byte[] buffer = new byte[is.available()];
            int read = is.read(buffer);
            is.close();

            String jsonStr = new String(buffer, 0, read, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(jsonStr);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String id = obj.optString("id");
                String name = obj.optString("name");
                String author = obj.optString("author");
                String variant = obj.optString("variant", "dark");
                JSONArray palArr = obj.optJSONArray("palette");

                int[] palette = new int[16];
                if (palArr != null && palArr.length() >= 16) {
                    for (int p = 0; p < 16; p++) {
                        String hex = palArr.optString(p, "#000000");
                        try {
                            palette[p] = Color.parseColor(hex);
                        } catch (Exception e) {
                            palette[p] = Color.DKGRAY;
                        }
                    }
                }

                Base16Theme theme = new Base16Theme(id, name, author, variant, palette);
                allThemes.add(theme);
                themesById.put(id.toLowerCase(Locale.ROOT), theme);
            }

            // Construir lista de destacados
            for (String featId : FEATURED_IDS) {
                Base16Theme featTheme = themesById.get(featId.toLowerCase(Locale.ROOT));
                if (featTheme != null && !featuredThemes.contains(featTheme)) {
                    featuredThemes.add(featTheme);
                }
            }

            // Si por alguna razón la lista de destacados quedó corta, agregar los primeros 10
            if (featuredThemes.isEmpty() && !allThemes.isEmpty()) {
                for (int i = 0; i < Math.min(10, allThemes.size()); i++) {
                    featuredThemes.add(allThemes.get(i));
                }
            }

            isLoaded = true;
            long elapsed = System.currentTimeMillis() - startTime;
            DebugLogger.log("Base16ThemeManager: " + allThemes.size() + " esquemas cargados en " + elapsed + "ms");
        } catch (Exception e) {
            DebugLogger.log("Base16ThemeManager error cargando assets: " + e.getMessage());
        }
    }

    public List<Base16Theme> getAllThemes(Context context) {
        ensureLoaded(context);
        return Collections.unmodifiableList(allThemes);
    }

    public List<Base16Theme> getFeaturedThemes(Context context) {
        ensureLoaded(context);
        return Collections.unmodifiableList(featuredThemes);
    }

    public Base16Theme getThemeById(Context context, String id) {
        if (id == null) return null;
        ensureLoaded(context);
        return themesById.get(id.toLowerCase(Locale.ROOT));
    }

    public Base16Theme getThemeByName(Context context, String name) {
        if (name == null) return null;
        ensureLoaded(context);
        for (Base16Theme t : allThemes) {
            if (t.getName().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

    public List<Base16Theme> searchThemes(Context context, String query) {
        ensureLoaded(context);
        if (query == null || query.trim().isEmpty()) {
            return Collections.unmodifiableList(allThemes);
        }
        String q = query.toLowerCase(Locale.ROOT).trim();
        List<Base16Theme> filtered = new ArrayList<>();
        for (Base16Theme theme : allThemes) {
            if (theme.getName().toLowerCase(Locale.ROOT).contains(q) ||
                theme.getId().toLowerCase(Locale.ROOT).contains(q) ||
                theme.getVariant().equalsIgnoreCase(q)) {
                filtered.add(theme);
            }
        }
        return filtered;
    }

    public Base16Theme getSelectedTheme(Context context) {
        ensureLoaded(context);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedId = prefs.getString(KEY_BASE16_THEME_ID, null);
        if (savedId != null) {
            Base16Theme theme = getThemeById(context, savedId);
            if (theme != null) return theme;
        }
        // Retornar Dracula o el primer destacado si no hay ninguno guardado
        return getThemeById(context, "dracula");
    }

    public void setSelectedTheme(Context context, Base16Theme theme) {
        if (theme == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_BASE16_THEME_ID, theme.getId()).apply();
    }
}
