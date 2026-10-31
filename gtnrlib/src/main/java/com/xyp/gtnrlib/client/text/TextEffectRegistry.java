package com.xyp.gtnrlib.client.text;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.xyp.gtnrlib.text.TextEffectStyle;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

/** Register effects during client initialization, before rendering starts. */
public class TextEffectRegistry {

    private static final Map<String, TextEffect> EFFECTS = new Object2ObjectLinkedOpenHashMap<>();
    private static final Map<String, String> ALIASES = new Object2ObjectLinkedOpenHashMap<>();
    private static List<String> identifiers = List.of();

    public static void register(String identifier, TextEffect effect) {
        new TextEffectStyle(identifier, IntList.of(), 1);
        if (ALIASES.containsKey(identifier))
            throw new IllegalArgumentException("Text effect alias already registered: " + identifier);
        if (EFFECTS.putIfAbsent(identifier, Objects.requireNonNull(effect, "effect")) != null) {
            throw new IllegalArgumentException("Text effect already registered: " + identifier);
        }
        identifiers = List.copyOf(EFFECTS.keySet());
    }

    /** Returns an immutable snapshot in registration order, including effects supplied by other mods. */
    public static List<String> identifiers() {
        return identifiers;
    }

    public static TextEffect get(String identifier) {
        return EFFECTS.get(ALIASES.getOrDefault(identifier, identifier));
    }

    public static void registerAlias(String alias, String identifier) {
        new TextEffectStyle(alias, IntList.of(), 1);
        if (!EFFECTS.containsKey(identifier)) throw new IllegalArgumentException("Unknown text effect: " + identifier);
        if (EFFECTS.containsKey(alias) || ALIASES.putIfAbsent(alias, identifier) != null)
            throw new IllegalArgumentException("Text effect already registered: " + alias);
    }

    public static void reload() {
        for (TextEffect effect : EFFECTS.values()) effect.close();
    }
}
