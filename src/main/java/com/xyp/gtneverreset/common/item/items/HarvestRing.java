package com.xyp.gtneverreset.common.item.items;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;

import com.gtnewhorizon.cropsnh.api.ICropStickTile;
import com.xyp.gtneverreset.client.GTNRCreativeTabs;
import com.xyp.gtneverreset.utils.enums.ModList;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class HarvestRing extends Item implements IBauble {

    private static final int RANGE = 8;
    private static final int INTERVAL = 20;
    private static final String LAST_SCAN = "gtneverreset.harvestLastScan";

    public HarvestRing() {
        // #tr item.gtneverreset.harvest_ring.name
        // # Harvest Ring
        // # zh_CN 收获戒指
        setUnlocalizedName("gtneverreset.harvest_ring");
        setTextureName("gtneverreset:harvest_ring");
        setMaxStackSize(1);
        setCreativeTab(GTNRCreativeTabs.ITEMS);
        GameRegistry.registerItem(this, "HarvestRing");
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.RING;
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase wearer) {
        if (!(wearer instanceof EntityPlayer player) || player.worldObj.isRemote
            || player.isDead
            || player.isSneaking()
            || player.ticksExisted % INTERVAL != 0) return;
        long tick = player.worldObj.getTotalWorldTime();
        if (player.getEntityData()
            .hasKey(LAST_SCAN)
            && player.getEntityData()
                .getLong(LAST_SCAN) == tick)
            return;
        player.getEntityData()
            .setLong(LAST_SCAN, tick);
        World world = player.worldObj;
        int cx = MathHelper.floor_double(player.posX);
        int cy = MathHelper.floor_double(player.boundingBox.minY);
        int cz = MathHelper.floor_double(player.posZ);
        for (int dx = -RANGE; dx <= RANGE; dx++) {
            for (int dz = -RANGE; dz <= RANGE; dz++) {
                if (dx * dx + dz * dz > RANGE * RANGE) continue;
                for (int dy = -3; dy <= 3; dy++) {
                    int x = cx + dx;
                    int y = cy + dy;
                    int z = cz + dz;
                    if (y >= 0 && y < world.getHeight() && world.blockExists(x, y, z))
                        harvest(stack, player, world, x, y, z);
                }
            }
        }
    }

    private void harvest(ItemStack ring, EntityPlayer player, World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        TileEntity tile = world.getTileEntity(x, y, z);
        ICropStickTile crop = ModList.CropsNH.isModLoaded() && tile instanceof ICropStickTile value ? value : null;
        Item seed = block == Blocks.wheat ? Items.wheat_seeds
            : block == Blocks.carrots ? Items.carrot
                : block == Blocks.potatoes ? Items.potato : block == Blocks.nether_wart ? Items.nether_wart : null;
        if (crop != null) {
            if (!crop.canHarvest()) return;
        } else if (seed == null || meta < (block == Blocks.nether_wart ? 3 : 7)) return;
        if (!world.canMineBlock(player, x, y, z) || !player.canPlayerEdit(x, y, z, 1, ring)) return;
        if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(x, y, z, world, block, meta, player))) return;
        if (crop != null) {
            List<ItemStack> drops = crop.harvest(1.0D);
            if (drops != null) {
                for (ItemStack drop : drops) {
                    if (drop != null && drop.stackSize > 0) crop.dropItem(drop);
                }
                tile.markDirty();
            }
            return;
        }
        List<ItemStack> drops = block.getDrops(world, x, y, z, meta, 0);
        boolean reservedSeed = false;
        for (ItemStack drop : drops) {
            if (drop != null && drop.getItem() == seed && drop.stackSize > 0) {
                drop.stackSize--;
                reservedSeed = true;
                break;
            }
        }
        if (!reservedSeed || !world.setBlockMetadataWithNotify(x, y, z, 0, 3)) return;
        for (ItemStack drop : drops) {
            if (drop == null || drop.stackSize <= 0) continue;
            EntityItem entity = new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, drop);
            entity.delayBeforeCanPickup = 10;
            world.spawnEntityInWorld(entity);
        }
    }

    @Override
    public void onEquipped(ItemStack stack, EntityLivingBase wearer) {}

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase wearer) {}

    @Override
    public boolean canEquip(ItemStack stack, EntityLivingBase wearer) {
        return wearer instanceof EntityPlayer;
    }

    @Override
    public boolean canUnequip(ItemStack stack, EntityLivingBase wearer) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean advanced) {
        // #tr tooltip.gtnr.harvest_ring
        // # Harvests Crops NH and vanilla crops within 8 horizontal and 3 vertical blocks. Sneak to pause.
        // # zh_CN 装备于 Baubles 戒指栏：收获 Crops NH 作物架、小麦、胡萝卜、马铃薯和地狱疣。水平半径8格、上下3格，潜行暂停。
        lines.add(StatCollector.translateToLocal("tooltip.gtnr.harvest_ring"));
    }
}
