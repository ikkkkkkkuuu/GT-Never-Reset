package com.xyp.gtneverreset.common.wand;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

/** Resumable connected or gap-limited search. All world access belongs to the caller's tick. */
public final class UniversalWandSearch {

    @FunctionalInterface
    public interface Matcher {

        boolean matches(int x, int y, int z);
    }

    @FunctionalInterface
    public interface Result {

        void accept(int x, int y, int z);
    }

    private static final int[][] NEIGHBORS = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };
    private final int limit, maxGap;
    private final boolean enhanced;
    private final Matcher matcher;
    private final Matcher available;
    private final Long2IntOpenHashMap bestGap = new Long2IntOpenHashMap();
    private final ArrayDeque<int[]> frontier = new ArrayDeque<>();
    private final Set<Long> visited = new HashSet<>();
    private int[] current;
    private int neighbor;
    private int found;
    private boolean finished;

    UniversalWandSearch(int sx, int sy, int sz, int limit, boolean enhanced, int maxGap, Matcher matcher,
        Matcher available) {
        this.limit = limit;
        this.enhanced = enhanced;
        this.maxGap = maxGap;
        this.available = available;
        bestGap.defaultReturnValue(Integer.MAX_VALUE);
        this.matcher = matcher;
        finished = limit <= 0;
        if (enhanced) {
            bestGap.put(key(sx, sy, sz), 0);
            expand(sx, sy, sz, 0);
        } else {
            frontier.add(new int[] { sx, sy, sz });
            visited.add(key(sx, sy, sz));
        }
    }

    public int getFound() {
        return found;
    }

    public boolean isFinished() {
        return finished;
    }

    boolean matches(int x, int y, int z) {
        return matcher.matches(x, y, z);
    }

    void harvestAll(Matcher harvest) {
        while (!finished) advance(Integer.MAX_VALUE, (x, y, z) -> { if (!harvest.matches(x, y, z)) finished = true; });
    }

    public int advance(int budget, Result result) {
        if (enhanced) return advanceEnhanced(budget, result);
        int scanned = 0;
        while (!finished && scanned < budget) {
            int[] position = nextConnected();
            if (position == null) {
                finished = true;
                break;
            }
            scanned++;
            if (position.length == 0) continue;
            int x = position[0], y = position[1], z = position[2];
            if (!enhanced && !visited.add(key(x, y, z))) continue;
            if (!matcher.matches(x, y, z)) continue;
            found++;
            if (!enhanced) frontier.addLast(position);
            result.accept(x, y, z);
            if (found >= limit) finished = true;
        }
        return scanned;
    }

    private int[] nextConnected() {
        if (current == null) {
            current = frontier.pollFirst();
            neighbor = 0;
            if (current == null) return null;
        }
        int index = neighbor++;
        int x = current[0] + index / 9 - 1;
        int y = current[1] + index / 3 % 3 - 1;
        int z = current[2] + index % 3 - 1;
        if (neighbor == 27) current = null;
        return new int[] { x, y, z };
    }

    private int advanceEnhanced(int budget, Result result) {
        int scanned = 0;
        while (!finished && scanned < budget) {
            int[] position = frontier.pollFirst();
            if (position == null) {
                finished = true;
                break;
            }
            scanned++;
            int x = position[0], y = position[1], z = position[2], gap = position[3];
            long positionKey = key(x, y, z);
            if (gap != bestGap.get(positionKey) || !available.matches(x, y, z)) continue;
            if (matcher.matches(x, y, z)) {
                bestGap.put(positionKey, 0);
                expand(x, y, z, 0);
                found++;
                result.accept(x, y, z);
                if (found >= limit) finished = true;
            } else if (gap < maxGap) {
                expand(x, y, z, gap + 1);
            }
        }
        return scanned;
    }

    private void expand(int x, int y, int z, int gap) {
        for (int[] offset : NEIGHBORS) {
            int nx = x + offset[0], ny = y + offset[1], nz = z + offset[2];
            long neighborKey = key(nx, ny, nz);
            if (gap < bestGap.get(neighborKey)) {
                bestGap.put(neighborKey, gap);
                frontier.addLast(new int[] { nx, ny, nz, gap });
            }
        }
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3ffffff) << 38) | ((long) (z & 0x3ffffff) << 12) | (y & 0xfff);
    }
}
