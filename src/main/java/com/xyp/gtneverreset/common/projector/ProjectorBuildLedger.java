package com.xyp.gtneverreset.common.projector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public final class ProjectorBuildLedger extends WorldSavedData {

    private static final String NAME = "GTNR_ProjectorBuilds";
    private static final int MAX_BUILDS_PER_PLAYER = 8;
    private static final int MAX_CELLS_PER_PLAYER = 4_000_000;
    private final Map<UUID, Deque<Build>> histories = new HashMap<>();

    public ProjectorBuildLedger(String name) {
        super(name);
    }

    static Build begin(EntityPlayerMP player, ProjectorPlan plan) {
        return new Build(get(), player.getUniqueID(), Site.of(plan));
    }

    static Build latest(EntityPlayerMP player, ProjectorPlan plan) {
        ProjectorBuildLedger ledger = get();
        if (ledger == null) return null;
        Deque<Build> builds = ledger.histories.get(player.getUniqueID());
        if (builds == null) return null;
        Site site = Site.of(plan);
        var recent = builds.descendingIterator();
        while (recent.hasNext()) {
            Build build = recent.next();
            if (build.site.equals(site) && !build.positions.isEmpty()) return build;
        }
        return null;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        histories.clear();
        NBTTagList entries = tag.getTagList("Builds", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            UUID owner = new UUID(entry.getLong("OwnerMost"), entry.getLong("OwnerLeast"));
            Build build = new Build(this, owner, Site.read(entry));
            build.read(entry);
            if (!build.positions.isEmpty()) add(build);
        }
        for (Deque<Build> history : histories.values()) trim(history);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList entries = new NBTTagList();
        for (Deque<Build> builds : histories.values()) {
            for (Build build : builds) {
                if (build.positions.isEmpty()) continue;
                NBTTagCompound entry = new NBTTagCompound();
                entry.setLong("OwnerMost", build.owner.getMostSignificantBits());
                entry.setLong("OwnerLeast", build.owner.getLeastSignificantBits());
                build.site.write(entry);
                build.write(entry);
                entries.appendTag(entry);
            }
        }
        tag.setTag("Builds", entries);
    }

    private static ProjectorBuildLedger get() {
        World world = DimensionManager.getWorld(0);
        MapStorage storage = world == null ? null : world.mapStorage;
        if (storage == null) return null;
        var ledger = (ProjectorBuildLedger) storage.loadData(ProjectorBuildLedger.class, NAME);
        if (ledger == null) {
            ledger = new ProjectorBuildLedger(NAME);
            storage.setData(NAME, ledger);
        }
        return ledger;
    }

    private void add(Build build) {
        Deque<Build> builds = histories.computeIfAbsent(build.owner, owner -> new ArrayDeque<>());
        builds.addLast(build);
        if (builds.size() > MAX_BUILDS_PER_PLAYER) builds.removeFirst();
    }

    private void finish(Build build) {
        Deque<Build> builds = histories.get(build.owner);
        if (builds == null) return;
        if (build.positions.isEmpty()) builds.remove(build);
        if (builds.isEmpty()) histories.remove(build.owner);
        else trim(builds);
        markDirty();
    }

    private static void trim(Deque<Build> builds) {
        long cells = 0;
        for (Build build : builds) cells += build.positions.size();
        while (builds.size() > 1 && cells > MAX_CELLS_PER_PLAYER) cells -= builds.removeFirst().positions.size();
    }

    static final class Build {

        private final ProjectorBuildLedger ledger;
        private final UUID owner;
        private final Site site;
        private final Long2IntLinkedOpenHashMap positions = new Long2IntLinkedOpenHashMap();
        private final Object2IntOpenHashMap<BlockState> paletteIndices = new Object2IntOpenHashMap<>();
        private final List<BlockState> palette = new ArrayList<>();

        private Build(ProjectorBuildLedger ledger, UUID owner, Site site) {
            this.ledger = ledger;
            this.owner = owner;
            this.site = site;
            paletteIndices.defaultReturnValue(-1);
            positions.defaultReturnValue(-1);
        }

        void recordPlaced(World world, int x, int y, int z, ItemStack item) {
            if (ledger == null) return;
            if (positions.isEmpty()) ledger.add(this);
            Block block = world.getBlock(x, y, z);
            int machine = world.getTileEntity(x, y, z) instanceof IGregTechTileEntity tile ? tile.getMetaTileID() : 0;
            int paletteIndex = intern(new BlockState(block, world.getBlockMetadata(x, y, z), machine, Drop.of(item)));
            positions.put(CoordinatePacker.pack(x, y, z), paletteIndex);
            ledger.markDirty();
        }

        LongBidirectionalIterator newestFirst() {
            return positions.keySet()
                .iterator(positions.lastLongKey());
        }

        BlockState stateAt(long position) {
            return palette.get(positions.get(position));
        }

        void changed() {
            if (ledger != null) ledger.markDirty();
        }

        void finish() {
            if (ledger != null && !positions.isEmpty()) ledger.finish(this);
        }

        void finishUndo() {
            if (ledger != null) ledger.finish(this);
        }

        private int intern(BlockState state) {
            int index = paletteIndices.getInt(state);
            if (index >= 0) return index;
            index = palette.size();
            palette.add(state);
            paletteIndices.put(state, index);
            return index;
        }

        private void read(NBTTagCompound entry) {
            NBTTagList states = entry.getTagList("Palette", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < states.tagCount(); i++) palette.add(BlockState.read(states.getCompoundTagAt(i)));
            int[] cells = entry.getIntArray("Cells");
            if (cells.length % 3 != 0 || cells.length / 3 > MAX_CELLS_PER_PLAYER) return;
            for (int i = 0; i < cells.length; i += 3) {
                int state = cells[i + 2];
                if (state >= 0 && state < palette.size() && palette.get(state).block != Blocks.air) {
                    long position = ((long) cells[i] << 32) | (cells[i + 1] & 0xffffffffL);
                    positions.put(position, state);
                }
            }
        }

        private void write(NBTTagCompound entry) {
            NBTTagList states = new NBTTagList();
            for (BlockState state : palette) states.appendTag(state.write());
            entry.setTag("Palette", states);
            int[] cells = new int[positions.size() * 3];
            int i = 0;
            for (var cell : positions.long2IntEntrySet()) {
                long position = cell.getLongKey();
                cells[i++] = (int) (position >>> 32);
                cells[i++] = (int) position;
                cells[i++] = cell.getIntValue();
            }
            entry.setIntArray("Cells", cells);
        }
    }

    @Desugar
    record BlockState(Block block, int meta, int machine, Drop drop) {

        boolean matches(World world, int x, int y, int z) {
            if (block != world.getBlock(x, y, z) || meta != world.getBlockMetadata(x, y, z)) return false;
            int actual = world.getTileEntity(x, y, z) instanceof IGregTechTileEntity tile ? tile.getMetaTileID() : 0;
            return actual == machine;
        }

        ItemStack expectedDrop() {
            return drop.stack();
        }

        private static BlockState read(NBTTagCompound tag) {
            return new BlockState(
                (Block) Block.blockRegistry.getObject(tag.getString("Block")),
                tag.getInteger("Meta"),
                tag.getInteger("Machine"),
                Drop.read(tag));
        }

        private NBTTagCompound write() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Block", Block.blockRegistry.getNameForObject(block));
            tag.setInteger("Meta", meta);
            tag.setInteger("Machine", machine);
            drop.write(tag);
            return tag;
        }
    }

    @Desugar
    private record Drop(Item item, int damage, NBTTagCompound data) {

        private static Drop of(ItemStack stack) {
            return stack == null ? new Drop(null, 0, null)
                : new Drop(stack.getItem(), stack.getItemDamage(), stack.getTagCompound());
        }

        private ItemStack stack() {
            if (item == null) return null;
            ItemStack stack = new ItemStack(item, 1, damage);
            if (data != null) stack.setTagCompound((NBTTagCompound) data.copy());
            return stack;
        }

        private static Drop read(NBTTagCompound tag) {
            return new Drop(
                (Item) Item.itemRegistry.getObject(tag.getString("DropItem")),
                tag.getInteger("DropDamage"),
                tag.hasKey("DropData", Constants.NBT.TAG_COMPOUND) ? tag.getCompoundTag("DropData") : null);
        }

        private void write(NBTTagCompound tag) {
            if (item == null) return;
            tag.setString("DropItem", Item.itemRegistry.getNameForObject(item));
            tag.setInteger("DropDamage", damage);
            if (data != null) tag.setTag("DropData", data);
        }
    }

    @Desugar
    private record Site(int dimension, int x, int y, int z, int machine) {

        private static Site of(ProjectorPlan plan) {
            return new Site(plan.dimension, plan.x, plan.y, plan.z, plan.machine);
        }

        private static Site read(NBTTagCompound tag) {
            return new Site(
                tag.getInteger("Dimension"),
                tag.getInteger("X"),
                tag.getInteger("Y"),
                tag.getInteger("Z"),
                tag.getInteger("Controller"));
        }

        private void write(NBTTagCompound tag) {
            tag.setInteger("Dimension", dimension);
            tag.setInteger("X", x);
            tag.setInteger("Y", y);
            tag.setInteger("Z", z);
            tag.setInteger("Controller", machine);
        }
    }
}
