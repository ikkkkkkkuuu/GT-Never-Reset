package com.xyp.gtneverreset.utils.crafting.quantum.crafting.inv;

import java.util.Map;

import com.google.common.collect.Iterables;
import com.xyp.gtneverreset.utils.crafting.quantum.api.stacks.AEKey;
import com.xyp.gtneverreset.utils.crafting.quantum.api.stacks.KeyCounter;

import appeng.api.config.FuzzyMode;

/**
 * Currently, extracts the whole network contents when the job starts. Lazily extracting is unfortunately not possible
 * as long as the crafting simulation operates from a separate thread: any world access from this thread will deadlock
 * the server.
 */
public class NetworkCraftingSimulationState extends CraftingSimulationState {

    /**
     * Networks with at most this many distinct available keys are copied in full: the copy is cheap and avoids the
     * graph-peek/subset machinery entirely.
     */
    public static final int SNAPSHOT_SUBSET_THRESHOLD = 512;

    private final KeyCounter list;

    /**
     * @param copy a copy of the network inventory, which the state then owns
     */
    public NetworkCraftingSimulationState(KeyCounter copy) {
        this.list = copy;
    }

    /**
     * Number of entries in the snapshot, for performance logging.
     */
    public int getSnapshotEntryCount() {
        return this.list.size();
    }

    @Override
    protected long simulateExtractParent(AEKey what) {
        return Math.min(list.get(what), Long.MAX_VALUE);
    }

    @Override
    protected Iterable<AEKey> findFuzzyParent(AEKey input) {
        return Iterables.transform(list.findFuzzy(input, FuzzyMode.IGNORE_ALL), Map.Entry::getKey);
    }
}
