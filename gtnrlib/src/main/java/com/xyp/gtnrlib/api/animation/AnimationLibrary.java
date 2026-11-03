package com.xyp.gtnrlib.api.animation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Owner-thread resource registry. Registered providers run in registration order on each reload. */
public final class AnimationLibrary {

    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private final Map<String, Provider> providers = new LinkedHashMap<>();
    private Map<String, AnimationClip> clips = Collections.emptyMap();

    @FunctionalInterface
    public interface Provider {

        void load(Registrar registrar) throws Exception;
    }

    @FunctionalInterface
    public interface Registrar {

        void register(String id, AnimationClip clip);
    }

    public void registerProvider(String id, Provider provider) {
        checkId(id);
        if (providers.putIfAbsent(id, Objects.requireNonNull(provider)) != null)
            throw new IllegalArgumentException("duplicate provider: " + id);
    }

    public void removeProvider(String id) {
        providers.remove(id);
    }

    public AnimationClip find(String id) {
        return clips.get(id);
    }

    public Map<String, AnimationClip> snapshot() {
        return clips;
    }

    /** A provider failure preserves the entire previous snapshot. Later providers override earlier entries. */
    public boolean reload(Consumer<String> diagnostics) {
        Map<String, AnimationClip> next = new LinkedHashMap<>();
        for (var entry : new LinkedHashMap<>(providers).entrySet()) {
            Map<String, AnimationClip> local = new LinkedHashMap<>();
            try {
                entry.getValue()
                    .load((id, clip) -> {
                        checkId(id);
                        if (local.putIfAbsent(id, Objects.requireNonNull(clip)) != null)
                            throw new IllegalArgumentException("duplicate clip: " + id);
                    });
                next.putAll(local);
            } catch (Exception e) {
                diagnostics.accept(entry.getKey() + ": " + e.getMessage());
                return false;
            }
        }
        clips = Collections.unmodifiableMap(next);
        return true;
    }

    private static void checkId(String id) {
        if (!ID.matcher(id)
            .matches()) throw new IllegalArgumentException("invalid animation resource ID: " + id);
    }
}
