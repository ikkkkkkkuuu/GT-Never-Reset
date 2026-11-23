package com.xyp.gtneverreset.common.machine.multiMachineBase.processing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import gregtech.api.recipe.lookup.GTRecipeLookup;
import gregtech.api.recipe.lookup.GTRecipeLookupIngredient;
import gregtech.api.util.GTRecipe;

/** Shares the ordered results of native trie traversal. GT still filters every candidate against live inputs. */
public final class RecipeCandidateCache {

    private static final int MAX_KEYS = 512;
    private static final int MAX_INGREDIENTS = 64;
    private static final int MAX_CANDIDATES = 256;
    private static final Scope NOOP = () -> {};
    private static volatile RecipeCandidateCache current;

    private final Thread serverThread = Thread.currentThread();
    private final LinkedHashMap<Key, Entry> entries = new LinkedHashMap<>(64, 0.75f, true);
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(
        1,
        1,
        0,
        TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(32),
        task -> {
            Thread thread = new Thread(task, "GTNR recipe lookup");
            thread.setDaemon(true);
            return thread;
        },
        new ThreadPoolExecutor.DiscardPolicy());
    private boolean active;
    private final Scope leave = () -> active = false;

    private RecipeCandidateCache() {}

    public static void start() {
        stop();
        current = new RecipeCandidateCache();
    }

    public static void stop() {
        RecipeCandidateCache cache = current;
        current = null;
        if (cache != null) cache.worker.shutdownNow();
    }

    public static Scope enter() {
        RecipeCandidateCache cache = current;
        if (cache == null || Thread.currentThread() != cache.serverThread || cache.active) return NOOP;
        cache.active = true;
        return cache.leave;
    }

    public static Iterator<GTRecipe> candidates(GTRecipeLookup lookup, Object lookupIdentity, long revision,
        List<GTRecipeLookupIngredient> ingredients) {
        RecipeCandidateCache cache = current;
        if (cache == null || Thread.currentThread() != cache.serverThread
            || !cache.active
            || ingredients.size() > MAX_INGREDIENTS) return null;
        return cache.get(lookup, new Key(lookupIdentity, revision, ingredients));
    }

    private Iterator<GTRecipe> get(GTRecipeLookup lookup, Key key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            if (entries.size() == MAX_KEYS) entries.remove(
                entries.keySet()
                    .iterator()
                    .next());
            List<GTRecipeLookupIngredient> snapshot = List.copyOf(key.ingredients);
            entries.put(new Key(key.lookupIdentity, key.revision, snapshot), new Entry(snapshot));
            return null;
        }
        List<GTRecipe> ready = entry.candidates;
        if (ready != null) return ready.iterator();
        // Only prefetch repeated queries. Pending, oversized and cold queries use the native lazy iterator.
        if (!entry.submitted && worker.getQueue()
            .remainingCapacity() > 0) {
            entry.submitted = true;
            worker.execute(() -> entry.load(lookup));
        }
        return null;
    }

    public interface Scope extends AutoCloseable {

        @Override
        void close();
    }

    // A retired trie may be large. Only queued and running jobs retain the trie itself.
    private record Key(Object lookupIdentity, long revision, List<GTRecipeLookupIngredient> ingredients) {}

    private static final class Entry {

        private final List<GTRecipeLookupIngredient> ingredients;
        private boolean submitted;
        private volatile List<GTRecipe> candidates;

        private Entry(List<GTRecipeLookupIngredient> ingredients) {
            this.ingredients = ingredients;
        }

        private void load(GTRecipeLookup lookup) {
            List<GTRecipe> found = new ArrayList<>();
            // GT mutates its trie in add(). Rebuilds and removals replace the trie with a new instance.
            synchronized (lookup) {
                Iterator<GTRecipe> recipes = lookup.iterator(ingredients);
                while (recipes.hasNext()) {
                    if (found.size() == MAX_CANDIDATES || Thread.currentThread()
                        .isInterrupted()) return;
                    found.add(recipes.next());
                }
            }
            candidates = List.copyOf(found);
        }
    }
}
