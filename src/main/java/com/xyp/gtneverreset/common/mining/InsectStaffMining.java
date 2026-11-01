package com.xyp.gtneverreset.common.mining;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockRedstoneOre;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.oredict.OreDictionary;

import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.utils.IConfigurableItem.ProfileHelper;
import com.xyp.gtneverreset.common.gui.InsectStaffConfig;
import com.xyp.gtneverreset.common.item.items.InsectStaff;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.common.blocks.GTBlockOre;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public final class InsectStaffMining {

    public static final InsectStaffMining INSTANCE = new InsectStaffMining();
    private static final int SCANS_PER_TICK = 2048;
    private static final int BLOCKS_PER_TICK = 64;
    private static final int MAX_VISITED = 1048576;
    private static final int[][] NEIGHBORS = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };
    private final Map<UUID, Job> jobs = new HashMap<>();
    private EntityPlayerMP harvesting;

    private InsectStaffMining() {}

    public boolean isHarvesting(EntityPlayer player) {
        return harvesting == player;
    }

    public void start(EntityPlayerMP player, ItemStack stack, int x, int y, int z) {
        if (jobs.containsKey(player.getUniqueID()) || !player.worldObj.blockExists(x, y, z)) return;
        if (!(stack.getItem() instanceof InsectStaff) || !InsectStaffConfig.veinEnabled(stack)) return;
        jobs.put(player.getUniqueID(), new Job(player, stack, x, y, z));
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Iterator<Job> iterator = jobs.values()
            .iterator();
        while (iterator.hasNext()) {
            Job job = iterator.next();
            if (!job.valid()) {
                iterator.remove();
                continue;
            }
            harvesting = job.player;
            try {
                job.step();
            } finally {
                harvesting = null;
            }
            if (job.queue.isEmpty() || job.mined >= job.limit) iterator.remove();
        }
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (!event.world.isRemote) jobs.values()
            .removeIf(job -> job.world == event.world);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void drops(BlockEvent.HarvestDropsEvent event) {
        if (harvesting == null || event.harvester != harvesting || event.world.isRemote) return;
        ItemStack tool = harvesting.getHeldItem();
        if (tool != null && tool.getItem() instanceof InsectStaff staff
            && ProfileHelper.getBoolean(tool, References.OBLITERATE, false)) {
            Integer filterMeta = staff.getObliterationList(tool)
                .get(event.block);
            if (filterMeta != null && filterMeta == event.blockMetadata) {
                event.drops.clear();
                return;
            }
        }
        for (ItemStack stack : event.drops) {
            if (stack == null || event.world.rand.nextFloat() > event.dropChance) continue;
            EntityItem drop = new EntityItem(
                event.world,
                harvesting.posX,
                harvesting.posY + 0.5,
                harvesting.posZ,
                stack.copy());
            drop.delayBeforeCanPickup = 0;
            event.world.spawnEntityInWorld(drop);
        }
        event.drops.clear();
    }

    private static boolean isOre(World world, int x, int y, int z, Block block) {
        if (block instanceof GTBlockOre || block instanceof BlockOre || block instanceof BlockRedstoneOre) return true;
        if (Item.getItemFromBlock(block) == null) return false;
        for (int id : OreDictionary.getOreIDs(new ItemStack(block, 1, block.getDamageValue(world, x, y, z)))) {
            if (OreDictionary.getOreName(id)
                .startsWith("ore")) return true;
        }
        return false;
    }

    private static long key(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (y & 0xFFFL);
    }

    private static final class Job {

        private final EntityPlayerMP player;
        private final ItemStack stack;
        private final World world;
        private final Block target;
        private final int targetMeta;
        private final int[] targetOres;
        private final int gapLimit;
        private final int limit;
        private final boolean precise;
        private final boolean backfill;
        private final Queue<Node> queue = new ArrayDeque<>();
        private final Long2IntOpenHashMap gaps = new Long2IntOpenHashMap();
        private int mined;

        private Job(EntityPlayerMP player, ItemStack stack, int x, int y, int z) {
            this.player = player;
            this.stack = stack;
            world = player.worldObj;
            target = world.getBlock(x, y, z);
            targetMeta = target.getDamageValue(world, x, y, z);
            targetOres = OreDictionary.getOreIDs(new ItemStack(target, 1, targetMeta));
            gapLimit = InsectStaffConfig.range(stack);
            limit = InsectStaffConfig.amount(stack);
            precise = ProfileHelper.getBoolean(stack, InsectStaffConfig.PRECISE, false);
            backfill = ProfileHelper.getBoolean(stack, InsectStaffConfig.BACKFILL, true);
            gaps.defaultReturnValue(Integer.MAX_VALUE);
            enqueue(x, y, z, 0);
        }

        private boolean valid() {
            InsectStaff staff = (InsectStaff) stack.getItem();
            return !player.isDead && player.worldObj == world
                && player.getCurrentEquippedItem() == stack
                && player.isSneaking()
                && stack.stackSize > 0
                && InsectStaffConfig.veinEnabled(stack)
                && (player.capabilities.isCreativeMode || staff.getEnergyStored(stack) >= staff.energyPerOperation);
        }

        private void step() {
            int scanned = 0;
            int broken = 0;
            while (!queue.isEmpty() && mined < limit && scanned++ < SCANS_PER_TICK && broken < BLOCKS_PER_TICK) {
                if (!valid()) {
                    queue.clear();
                    return;
                }
                Node node = queue.remove();
                if (node.gap != gaps.get(key(node.x, node.y, node.z)) || !world.blockExists(node.x, node.y, node.z))
                    continue;
                Block block = world.getBlock(node.x, node.y, node.z);
                int nextGap = node.gap;
                boolean ore = isOre(world, node.x, node.y, node.z, block);
                if (matches(node, block) && block.getBlockHardness(world, node.x, node.y, node.z) >= 0) {
                    if (world.getTileEntity(node.x, node.y, node.z) != null && !ore) continue;
                    if (!player.theItemInWorldManager.tryHarvestBlock(node.x, node.y, node.z)) continue;
                    mined++;
                    broken++;
                    nextGap = 0;
                    if (backfill && ore && world.isAirBlock(node.x, node.y, node.z)) {
                        world.setBlock(node.x, node.y, node.z, Blocks.stone, 0, 3);
                    }
                } else {
                    if (nextGap >= gapLimit) continue;
                    nextGap++;
                }
                for (int[] offset : NEIGHBORS)
                    enqueue(node.x + offset[0], node.y + offset[1], node.z + offset[2], nextGap);
            }
        }

        private boolean matches(Node node, Block block) {
            if (block.isAir(world, node.x, node.y, node.z) || Item.getItemFromBlock(block) == null) return false;
            int meta = block.getDamageValue(world, node.x, node.y, node.z);
            if (block == target && (!precise || meta == targetMeta)) return true;
            for (int id : OreDictionary.getOreIDs(new ItemStack(block, 1, meta))) {
                String name = OreDictionary.getOreName(id);
                for (int targetId : targetOres) {
                    String targetName = OreDictionary.getOreName(targetId);
                    if (precise ? targetId == id
                        : (name.startsWith("ore") && targetName.startsWith("ore")) || targetName.startsWith(name))
                        return true;
                }
            }
            return false;
        }

        private void enqueue(int x, int y, int z, int gap) {
            if (y < 0 || y >= world.getHeight() || gaps.size() >= MAX_VISITED || !world.blockExists(x, y, z)) return;
            long position = key(x, y, z);
            if (gap >= gaps.get(position)) return;
            gaps.put(position, gap);
            queue.add(new Node(x, y, z, gap));
        }
    }

    private static final class Node {

        private final int x;
        private final int y;
        private final int z;
        private final int gap;

        private Node(int x, int y, int z, int gap) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.gap = gap;
        }
    }
}
