package com.xyp.gtnrlib.gui.theme.ae2;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeBuilder;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.theme.WidgetThemeKey;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.utils.JsonHelper;
import com.google.gson.JsonObject;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

/**
 * The look GTNRLib's AE2-styled widgets take in one panel: one of the AE2 skins, or the plain look every other theme
 * gets. Widgets ask {@link #of(IWidget)} while they draw, so a panel with a theme of its own, such as a dialog over an
 * AE2 screen, gets the look of its own theme rather than the screen's.
 * <p>
 * A theme names its AE2 skin in the {@link #KEY} entry of its theme data. The AE2 themes write that entry when they
 * are registered, and themes whose parent is an AE2 theme inherit it. A theme without the entry is recognised by its
 * identifier, so the AE2 themes keep their look even when the entry is missing.
 * <p>
 * The class loads on both sides. Resolving a look from a widget, a context or a theme is meant for drawing on the
 * client, while the widget is part of an open panel.
 *
 * @param variant AE2 skin, or {@code null} for the plain look
 */
public record AE2Look(@Nullable Variant variant) {

    /** Theme data entry that names the AE2 skin of a theme; themes without it get the plain look. */
    public static final WidgetThemeKey<Marker> KEY = Marker.register();
    private static final AE2Look PLAIN = new AE2Look(null);
    private static final AE2Look[] SKINNED = skinned();

    /**
     * Returns the plain look, used under every theme that is not an AE2 theme.
     *
     * @return the plain look
     */
    public static AE2Look plain() {
        return PLAIN;
    }

    /**
     * Returns the look of an AE2 skin.
     *
     * @param variant AE2 skin, or {@code null} for the plain look
     * @return the shared look of that skin
     */
    public static AE2Look of(@Nullable Variant variant) {
        return variant == null ? PLAIN : SKINNED[variant.ordinal()];
    }

    /**
     * Resolves the look of the panel a widget belongs to, from that panel's theme.
     *
     * @param widget widget that is part of an open panel
     * @return the panel's look, or the plain look for a widget that is not in a panel yet
     */
    public static AE2Look of(IWidget widget) {
        return widget.isValid() ? of(
            widget.getPanel()
                .getTheme())
            : PLAIN;
    }

    /**
     * Resolves the look of the screen being drawn, for drawing code that has no widget at hand. Prefer
     * {@link #of(IWidget)}: a panel can use a theme other than its screen's.
     *
     * @param context current drawing context
     * @return the screen's look, or the plain look outside modular screens
     */
    public static AE2Look of(GuiContext context) {
        return context instanceof ModularGuiContext modular ? of(modular.getTheme()) : PLAIN;
    }

    /**
     * Resolves the look a theme gives: the skin named in its {@link #KEY} entry, else the AE2 skin registered under its
     * identifier, else the plain look.
     *
     * @param theme theme to read, possibly {@code null}
     * @return the theme's look
     */
    public static AE2Look of(@Nullable ITheme theme) {
        if (theme == null) return PLAIN;
        WidgetThemeEntry<Marker> entry = theme.getWidgetTheme(KEY);
        Variant marked = entry == null ? null
            : skin(
                entry.getTheme()
                    .skin());
        return of(marked != null ? marked : AE2Theme.variant(theme.getId()));
    }

    /**
     * Finds the AE2 skin stored under a texture folder name.
     *
     * @param folder skin folder name below {@code textures/gui/skin/}, such as {@code ae2_dark}
     * @return the matching skin, or {@code null} for an empty or unknown name
     */
    public static @Nullable Variant skin(@Nullable String folder) {
        for (Variant variant : Variant.values()) if (variant.folder.equals(folder)) return variant;
        return null;
    }

    /**
     * Reports whether this is one of the AE2 skins rather than the plain look.
     *
     * @return {@code true} under an AE2 skin
     */
    public boolean isAE2() {
        return variant != null;
    }

    private static AE2Look[] skinned() {
        Variant[] variants = Variant.values();
        AE2Look[] looks = new AE2Look[variants.length];
        for (Variant variant : variants) looks[variant.ordinal()] = new AE2Look(variant);
        return looks;
    }

    /** Theme data naming the AE2 skin a theme uses. */
    public static class Marker extends WidgetTheme {

        /** JSON key holding the skin folder name. */
        public static final String SKIN = "skin";
        private final String skin;

        /**
         * Creates the entry of a theme.
         *
         * @param skin skin folder name, such as {@code ae2_dark}, or an empty string for the plain look
         */
        public Marker(String skin) {
            super(0, 0, null, Color.WHITE.main, Color.WHITE.main, false, Color.WHITE.main);
            this.skin = skin;
        }

        /**
         * Reads the entry of a theme from its theme data, keeping the parent's skin when the data names none.
         *
         * @param parent   entry of the parent theme
         * @param json     this entry's theme data
         * @param fallback the theme's top-level data, or {@code null}
         */
        public Marker(Marker parent, JsonObject json, JsonObject fallback) {
            super(parent, json, fallback);
            skin = JsonHelper.getString(json, parent.skin, SKIN);
        }

        /**
         * Returns the skin folder name.
         *
         * @return skin folder name, or an empty string for the plain look
         */
        public String skin() {
            return skin;
        }

        private static WidgetThemeKey<Marker> register() {
            Marker plain = new Marker("");
            return IThemeApi.get()
                .widgetThemeKeyBuilder("gtnrlibAE2Look", Marker.class)
                .defaultTheme(plain)
                .defaultHoverTheme(plain)
                .parser(Marker::new)
                .register();
        }

        /** Writes the entry into a theme definition. */
        public static class Builder extends WidgetThemeBuilder<Marker, Builder> {

            /**
             * Names the skin.
             *
             * @param folder skin folder name, such as {@code ae2_dark}
             * @return this builder
             */
            public Builder skin(String folder) {
                add(SKIN, folder);
                return this;
            }
        }
    }
}
