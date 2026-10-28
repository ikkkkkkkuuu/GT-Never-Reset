package com.xyp.gtneverreset.client.text;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.xyp.gtneverreset.utils.text.effect.TextEffectStyle;

/** Register effects during client initialization, before rendering starts. */
@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public class TextEffectRegistry {

    private static final Map<String, TextEffect> EFFECTS = new LinkedHashMap<>();

    public static void register(String identifier, TextEffect effect) {
        new TextEffectStyle(identifier, List.of(), 1);
        if (EFFECTS.putIfAbsent(identifier, Objects.requireNonNull(effect, "effect")) != null) {
            throw new IllegalArgumentException("Text effect already registered: " + identifier);
        }
    }

    public static TextEffect get(String identifier) {
        return EFFECTS.get(identifier);
    }

    public static void reload() {
        for (TextEffect effect : EFFECTS.values()) effect.close();
    }
}
