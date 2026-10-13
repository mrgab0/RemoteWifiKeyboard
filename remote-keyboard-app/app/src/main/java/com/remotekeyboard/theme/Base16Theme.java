package com.remotekeyboard.theme;

import android.graphics.Color;

/**
 * Modelo inmutable para un esquema de color Base16 (tinted-theming/schemes).
 * Almacena los 16 colores base (base00 - base0F) y expone mapeos semánticos
 * para interfaces gráficas de teclado táctil.
 */
public class Base16Theme {
    private final String id;
    private final String name;
    private final String author;
    private final String variant; // "dark" o "light"
    private final int[] palette; // 16 colores ARGB

    public Base16Theme(String id, String name, String author, String variant, int[] palette) {
        this.id = id != null ? id : "custom";
        this.name = name != null ? name : "Base16 Theme";
        this.author = author != null ? author : "Community";
        this.variant = variant != null ? variant.toLowerCase() : "dark";
        this.palette = palette != null && palette.length == 16 ? palette : new int[16];
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public String getVariant() {
        return variant;
    }

    public boolean isDark() {
        return !"light".equalsIgnoreCase(variant);
    }

    public int[] getPalette() {
        return palette;
    }

    public int getBase(int index) {
        if (index >= 0 && index < palette.length) {
            return palette[index];
        }
        return Color.DKGRAY;
    }

    // --- Mapeo semántico para teclado visual ---

    /** base00: Fondo general del panel del teclado */
    public int getBackgroundColor() {
        return getBase(0x0);
    }

    /** base01 o base02: Fondo de teclas normales (garantizando contraste) */
    public int getKeyBackgroundColor() {
        int b00 = getBase(0x0);
        int b01 = getBase(0x1);
        int b02 = getBase(0x2);

        // Si base01 tiene muy poco contraste con base00, usamos base02
        if (Math.abs(getLuminance(b00) - getLuminance(b01)) < 0.04) {
            return b02;
        }
        return b01;
    }

    /** base02 o acento: Fondo cuando la tecla está presionada */
    public int getKeyPressedColor() {
        return getBase(0x2);
    }

    /** base05: Texto principal de las teclas */
    public int getTextColor() {
        return getBase(0x5);
    }

    /** base0D: Acento principal (Enter, Live Feedback, Pill de pegado) */
    public int getAccentColor() {
        return getBase(0xD);
    }

    /** base02 o base0E: Teclas modificadoras (Shift, Ctrl, Alt) */
    public int getModifierKeyColor() {
        return getBase(0x2);
    }

    /** base08: Tecla de borrado (Backspace / Delete) */
    public int getDeleteKeyColor() {
        return getBase(0x8);
    }

    private static double getLuminance(int color) {
        return (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
    }

    @Override
    public String toString() {
        return name + " (" + variant.toUpperCase() + ")";
    }
}
