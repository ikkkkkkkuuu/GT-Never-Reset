package com.xyp.gtneverreset.common.wand;

import java.util.HashSet;
import java.util.Set;

public final class UniversalWandSearchContracts {

    public static void main(String[] args) {
        checkLine(2, 20, false, Set.of(3, 6, 10), Set.of(3, 6));
        checkLine(2, 20, true, Set.of(3, 6, 10), Set.of(3, 6));
        checkLine(128, 20, true, Set.of(129, 258, 388), Set.of(129, 258));
        checkLine(2, 1, true, Set.of(3, 6), Set.of(3));
        Set<Integer> result = new HashSet<>();
        UniversalWandSearch blocked = new UniversalWandSearch(
            0,
            0,
            0,
            20,
            true,
            128,
            (x, y, z) -> x == 3,
            (x, y, z) -> y == 0 && z == 0 && x >= 0 && x != 2 && x < 10);
        while (!blocked.isFinished()) blocked.advance(1, (x, y, z) -> result.add(x));
        if (!result.isEmpty()) throw new AssertionError("Search crossed unavailable blocks");
        UniversalWandSearch normal = new UniversalWandSearch(
            0,
            0,
            0,
            20,
            false,
            128,
            (x, y, z) -> x == 1 && y == 1 && z == 1 || x == 3 && y == 1 && z == 1,
            (x, y, z) -> true);
        while (!normal.isFinished()) normal.advance(1, (x, y, z) -> result.add(x));
        if (!result.equals(Set.of(1))) throw new AssertionError("Normal diagonal connectivity changed");
        System.out.println("Universal wand search contracts passed");
    }

    private static void checkLine(int gap, int limit, boolean harvest, Set<Integer> ores, Set<Integer> expected) {
        Set<Integer> remaining = new HashSet<>(ores);
        Set<Integer> result = new HashSet<>();
        UniversalWandSearch search = new UniversalWandSearch(
            0,
            0,
            0,
            limit,
            true,
            gap,
            (x, y, z) -> remaining.contains(x),
            (x, y, z) -> y == 0 && z == 0 && x >= 0 && x <= 600);
        while (!search.isFinished()) search.advance(1, (x, y, z) -> {
            if (!result.add(x)) throw new AssertionError("Duplicate harvest at " + x);
            if (harvest) remaining.remove(x);
        });
        if (!result.equals(expected) || search.getFound() != expected.size())
            throw new AssertionError("Gap " + gap + ": expected " + expected + ", got " + result);
    }
}
