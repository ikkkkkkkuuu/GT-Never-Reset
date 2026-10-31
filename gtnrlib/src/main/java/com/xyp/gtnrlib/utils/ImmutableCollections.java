package com.xyp.gtnrlib.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/** Shallow, unmodifiable snapshots that retain encounter order and accept null elements on Java 8. */
public final class ImmutableCollections {

    @SafeVarargs
    public static <T> List<T> listOf(T... values) {
        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(values)));
    }

    public static <T> List<T> copyList(Collection<? extends T> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    @SafeVarargs
    public static <T> Set<T> setOf(T... values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(values)));
    }

    public static <T> Set<T> copySet(Collection<? extends T> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    public static <K, V> Map<K, V> copyMap(Map<? extends K, ? extends V> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static <K, V> Map<K, V> emptyMap() {
        return Collections.emptyMap();
    }

    public static <T> Collector<T, ?, Set<T>> toImmutableSet() {
        return Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), Collections::unmodifiableSet);
    }
}
