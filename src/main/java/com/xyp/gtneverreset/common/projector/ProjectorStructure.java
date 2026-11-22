package com.xyp.gtneverreset.common.projector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public class ProjectorStructure {

    @Desugar
    public record HatchSlot(int[] candidates, int[] accepted, ItemStack casing) {}

    @Desugar
    public record Material(ItemStack stack, int count) {}

    @Desugar
    public record Choice(String key, ItemStack base, List<ItemStack> blocks, boolean air, String channel, int[] tiers,
        IntOpenHashSet accepted) {

        public static Choice of(ItemStack base, List<ItemStack> blocks, boolean air, String channel, int[] tiers) {
            IntOpenHashSet accepted = new IntOpenHashSet();
            List<String> keys = new ArrayList<>();
            for (ItemStack block : blocks) {
                accepted.add(idOf(block.getItem(), block.getItemDamage()));
                keys.add(ProjectorPlan.blockKey(block));
            }
            if (air) keys.add(ProjectorPlan.AIR);
            Collections.sort(keys);
            String key = ProjectorPlan.blockKey(base) + "#" + Integer.toHexString(keys.hashCode());
            return new Choice(key, base, Collections.unmodifiableList(blocks), air, channel, tiers, accepted);
        }

        public int tierOf(ItemStack block) {
            if (block == null) return 0;
            int id = idOf(block.getItem(), block.getItemDamage());
            for (int i = 0; i < blocks.size() && i < tiers.length; i++) {
                if (idOf(
                    blocks.get(i)
                        .getItem(),
                    blocks.get(i)
                        .getItemDamage())
                    == id) return tiers[i];
            }
            return 0;
        }

        public ItemStack blockOfTier(int tier) {
            for (int i = 0; i < blocks.size() && i < tiers.length; i++) {
                if (tiers[i] == tier) return blocks.get(i);
            }
            return null;
        }

        public static int idOf(Item item, int damage) {
            return Item.getIdFromItem(item) << 16 | damage & 0xFFFF;
        }

        public boolean accepts(Item item, int damage) {
            return item != null && accepted.contains(idOf(item, damage));
        }

        public ItemStack find(String blockKey) {
            for (ItemStack block : blocks) {
                if (ProjectorPlan.blockKey(block)
                    .equals(blockKey)) return block;
            }
            return null;
        }
    }

    public static final int ANY_META = -1;

    public final ProjectorPlan.Settings settings;
    public final ExtendedFacing canonical;
    public final int size;
    public final int[] a, b, c;
    public final Block[] blocks;
    public final int[] metas;
    public final int[] machines;
    public final int[] slots;
    public final int[] choiceOf;
    public final ItemStack[] items;
    public final List<HatchSlot> hatchSlots;
    public final List<Choice> choices;
    public final long[] emptyCells;
    public List<Material> materials = Collections.emptyList();
    public final List<String> channels;
    public final boolean formed, checked;
    public final List<String> errors;
    public final String limit;
    public final String failure;
    public final int rounds;
    public long nanos;
    public final int minA, minB, minC, maxA, maxB, maxC;
    public final ProjectorSimulationWorld world;
    public final int originX, originY, originZ;
    private final Long2IntOpenHashMap index = new Long2IntOpenHashMap();
    private int[][] simulationPositions;

    public ProjectorStructure(ProjectorSimulation simulation, int size) {
        this(
            size,
            simulation.settings,
            simulation.canonical,
            simulation.hatchSlots,
            simulation.choices,
            simulation.emptyCells.toLongArray(),
            Collections.unmodifiableList(simulation.channels),
            simulation.formed,
            simulation.checked,
            Collections.unmodifiableList(simulation.errors),
            simulation.limit,
            simulation.failure,
            simulation.rounds,
            simulation.world,
            simulation.bounds);
    }

    protected ProjectorStructure(ProjectorStructure template, int size, long[] emptyCells, int[] bounds,
        ProjectorSimulationWorld view) {
        this(
            size,
            template.settings,
            template.canonical,
            template.hatchSlots,
            template.choices,
            emptyCells,
            template.channels,
            template.formed,
            template.checked,
            template.errors,
            template.limit,
            template.failure,
            template.rounds,
            view,
            bounds);
    }

    private ProjectorStructure(int size, ProjectorPlan.Settings settings, ExtendedFacing canonical,
        List<HatchSlot> hatchSlots, List<Choice> choices, long[] emptyCells, List<String> channels, boolean formed,
        boolean checked, List<String> errors, String limit, String failure, int rounds, ProjectorSimulationWorld world,
        int[] bounds) {
        this.size = size;
        a = new int[size];
        b = new int[size];
        c = new int[size];
        blocks = new Block[size];
        metas = new int[size];
        machines = new int[size];
        slots = new int[size];
        choiceOf = new int[size];
        items = new ItemStack[size];
        this.settings = settings;
        this.canonical = canonical;
        this.hatchSlots = hatchSlots;
        this.choices = choices;
        this.emptyCells = emptyCells;
        this.channels = channels;
        this.formed = formed;
        this.checked = checked;
        this.errors = errors;
        this.limit = limit;
        this.failure = failure;
        this.rounds = rounds;
        this.world = world;
        originX = ProjectorSimulation.CONTROLLER_X;
        originY = ProjectorSimulation.CONTROLLER_Y;
        originZ = ProjectorSimulation.CONTROLLER_Z;
        minA = bounds[0];
        minB = bounds[1];
        minC = bounds[2];
        maxA = bounds[3];
        maxB = bounds[4];
        maxC = bounds[5];
        index.defaultReturnValue(-1);
    }

    public void set(int cell, int cellA, int cellB, int cellC, Block block, int meta, int machine, int slot, int choice,
        ItemStack item) {
        a[cell] = cellA;
        b[cell] = cellB;
        c[cell] = cellC;
        blocks[cell] = block;
        metas[cell] = meta;
        machines[cell] = machine;
        slots[cell] = slot;
        choiceOf[cell] = choice;
        items[cell] = item;
        index.put(ProjectorPlan.pack(cellA, cellB, cellC), cell);
    }

    public Choice choiceAt(int cell) {
        return cell < 0 || cell >= size || choiceOf[cell] < 0 ? null : choices.get(choiceOf[cell]);
    }

    public int groupOfChannel(String channel) {
        int best = -1, bestCount = 0;
        for (int group = 0; group < choices.size(); group++) {
            Choice choice = choices.get(group);
            if (!channel.equals(choice.channel()) || choice.blocks()
                .isEmpty()) continue;
            int count = countOf(group);
            if (count > bestCount) {
                best = group;
                bestCount = count;
            }
        }
        return best;
    }

    public int countChannel(String channel) {
        int count = 0;
        for (int cell = 0; cell < size; cell++) {
            Choice choice = choiceAt(cell);
            if (choice != null && channel.equals(choice.channel())) count++;
        }
        return count;
    }

    public int countOf(int choice) {
        int count = 0;
        for (int cell = 0; cell < size; cell++) {
            if (choiceOf[cell] == choice) count++;
        }
        return count;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int indexOf(int cellA, int cellB, int cellC) {
        return index.get(ProjectorPlan.pack(cellA, cellB, cellC));
    }

    public HatchSlot slotAt(int cell) {
        return cell < 0 || slots[cell] < 0 ? null : hatchSlots.get(slots[cell]);
    }

    public int[] worldBounds(ExtendedFacing facing) {
        int[] bounds = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE,
            Integer.MIN_VALUE };
        int[] corner = new int[3];
        for (int i = 0; i < 8; i++) {
            facing.getWorldOffset(
                new int[] { (i & 1) == 0 ? minA : maxA, (i & 2) == 0 ? minB : maxB, (i & 4) == 0 ? minC : maxC },
                corner);
            for (int axis = 0; axis < 3; axis++) {
                bounds[axis] = Math.min(bounds[axis], corner[axis]);
                bounds[axis + 3] = Math.max(bounds[axis + 3], corner[axis]);
            }
        }
        return bounds;
    }

    public void simulationPosition(int cellA, int cellB, int cellC, int[] out) {
        canonical.getWorldOffset(new int[] { cellA, cellB, cellC }, out);
        out[0] += originX;
        out[1] += originY;
        out[2] += originZ;
    }

    public int[][] simulationPositions() {
        if (simulationPositions == null) {
            int[][] positions = new int[3][size];
            int[] point = new int[3];
            for (int cell = 0; cell < size; cell++) {
                simulationPosition(a[cell], b[cell], c[cell], point);
                for (int axis = 0; axis < 3; axis++) positions[axis][cell] = point[axis];
            }
            simulationPositions = positions;
        }
        return simulationPositions;
    }

    public static int damageOf(World world, Block block, int meta, int x, int y, int z) {
        try {
            return block.getDamageValue(world, x, y, z);
        } catch (RuntimeException e) {
            return block.damageDropped(meta);
        }
    }
}
