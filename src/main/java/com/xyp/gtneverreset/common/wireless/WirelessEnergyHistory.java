package com.xyp.gtneverreset.common.wireless;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * Computes net EU/t from server-timed balance snapshots, without narrowing the balance to a long or double.
 * History belongs to one wireless team and covers at most five minutes of game time.
 */
public final class WirelessEnergyHistory {

    public static final int SAMPLE_INTERVAL = 100;
    private static final int WINDOW_TICKS = 6000;
    private static final int MAX_GAP_TICKS = 400;
    private final Deque<Sample> samples = new ArrayDeque<>();
    private UUID owner;
    private BigDecimal realtime;

    /**
     * Records one authoritative snapshot. Team changes, clock rollback and gaps restart the measurement window.
     *
     * @param network wireless team leader resolved by the server
     * @param tick    server overworld's total game time, independent of the player's dimension
     * @param energy  current network balance
     */
    public void add(UUID network, long tick, BigInteger energy) {
        Sample previous = samples.peekLast();
        if (!network.equals(owner)
            || (previous != null && (tick < previous.tick || tick - previous.tick > MAX_GAP_TICKS))) {
            clear();
        } else if (previous != null && tick == previous.tick) {
            return;
        }
        owner = network;
        previous = samples.peekLast();
        realtime = previous == null ? null : slope(previous, new Sample(tick, energy));
        samples.addLast(new Sample(tick, energy));
        while (samples.size() > 301 || tick - samples.getFirst().tick > WINDOW_TICKS) {
            samples.removeFirst();
        }
    }

    public BigInteger energy() {
        return samples.isEmpty() ? null : samples.getLast().energy;
    }

    public BigDecimal realtime() {
        return realtime;
    }

    public BigDecimal average() {
        return samples.size() < 2 ? null : slope(samples.getFirst(), samples.getLast());
    }

    public void clear() {
        samples.clear();
        owner = null;
        realtime = null;
    }

    public List<Sample> samples() {
        return List.copyOf(samples);
    }

    private static BigDecimal slope(Sample first, Sample last) {
        return new BigDecimal(last.energy.subtract(first.energy))
            .divide(BigDecimal.valueOf(last.tick - first.tick), 6, RoundingMode.HALF_UP);
    }

    public static final class Sample {

        public final long tick;
        public final BigInteger energy;

        private Sample(long tick, BigInteger energy) {
            this.tick = tick;
            this.energy = energy;
        }
    }
}
