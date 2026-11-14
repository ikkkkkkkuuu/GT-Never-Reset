package com.xyp.gtneverreset.common.wand;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.item.items.ItemUniversalWand;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.SecurityPermissions;
import appeng.api.features.ILocatable;
import appeng.api.implementations.tiles.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;

/** Uses an already loaded wireless access point; never creates nodes, connections or chunk tickets. */
public final class UniversalWandStorage {

    public static final class Destination {

        final boolean aePriority;
        final boolean inventory;
        final NBTTagCompound binding;

        public Destination(ItemStack wand) {
            aePriority = ItemUniversalWand.isAePriority(wand);
            // AE priority sends any remainder to the player's inventory before dropping it.
            inventory = ItemUniversalWand.isInventoryCollection(wand) || aePriority;
            binding = ItemUniversalWand.getAeBinding(wand);
        }

        boolean collects() {
            return aePriority || inventory;
        }
    }

    private UniversalWandStorage() {}

    static final class UnknownInsertionOutcome extends RuntimeException {

        UnknownInsertionOutcome(Throwable cause) {
            super("AE insertion did not return a trustworthy remainder", cause);
        }
    }

    public static void toggleBinding(EntityPlayerMP player, int x, int y, int z) {
        ItemStack wand = player.getHeldItem();
        if (player.isDead || !ItemUniversalWand.isHeldBy(player)
            || !player.worldObj.blockExists(x, y, z)
            || !UniversalWandActions.isTarget(player, x, y, z)) {
            // #tr gtnr.universal_wand.ae_invalid
            // # Target must be an online AE access point with insertion permission.
            // # zh_CN 目标不是在线且有注入权限的 AE 无线访问点。
            player.addChatMessage(new ChatComponentTranslation("gtnr.universal_wand.ae_invalid"));
            return;
        }
        TileEntity tile = player.worldObj.getTileEntity(x, y, z);
        if (!(tile instanceof IWirelessAccessPoint point) || tile.isInvalid()) {
            player.addChatMessage(new ChatComponentTranslation("gtnr.universal_wand.ae_invalid"));
            return;
        }
        NBTTagCompound existing = ItemUniversalWand.getAeBinding(wand);
        if (sameLocation(existing, player.dimension, x, y, z) && matchesBinding(tile, existing)) {
            if (UniversalWandRightClick.handled(player, ItemUniversalWand.setAeBinding(player, wand, null)))
                // #tr gtnr.universal_wand.ae_unbound
                // # AE wireless access point unbound.
                // # zh_CN 已取消绑定 AE 无线访问点。
                player.addChatMessage(new ChatComponentTranslation("gtnr.universal_wand.ae_unbound"));
            return;
        }
        if (!point.isActive() || !canInsert(player, point.getGrid())) {
            player.addChatMessage(new ChatComponentTranslation("gtnr.universal_wand.ae_invalid"));
            return;
        }
        NBTTagCompound binding = new NBTTagCompound();
        binding.setInteger("dimension", player.dimension);
        binding.setInteger("x", x);
        binding.setInteger("y", y);
        binding.setInteger("z", z);
        if (tile instanceof ILocatable locatable) binding.setLong("serial", locatable.getLocatableSerial());
        if (UniversalWandRightClick.handled(player, ItemUniversalWand.setAeBinding(player, wand, binding)))
            player.addChatMessage(
                // #tr gtnr.universal_wand.ae_bound
                // # Bound AE access point: dimension %s, %s, %s, %s.
                // # zh_CN 已绑定 AE 无线访问点：维度 %s，%s，%s，%s。
                new ChatComponentTranslation("gtnr.universal_wand.ae_bound", player.dimension, x, y, z));
    }

    private static boolean sameLocation(NBTTagCompound binding, int dimension, int x, int y, int z) {
        return hasLocation(binding) && binding.getInteger("dimension") == dimension
            && binding.getInteger("x") == x
            && binding.getInteger("y") == y
            && binding.getInteger("z") == z;
    }

    /** Clears only confirmed removal or replacement; offline and unloaded targets retain their binding. */
    public static void validateHeldBinding(EntityPlayerMP player, ItemStack wand) {
        if (player.getHeldItem() != wand || !ItemUniversalWand.isHeldBy(player)) return;
        NBTTagCompound binding = ItemUniversalWand.getAeBinding(wand);
        if (binding.hasNoTags()) return;
        if (!hasLocation(binding)) {
            ItemUniversalWand.setAeBinding(player, wand, null);
            return;
        }
        World world = DimensionManager.getWorld(binding.getInteger("dimension"));
        int x = binding.getInteger("x"), y = binding.getInteger("y"), z = binding.getInteger("z");
        if (world == null || !world.blockExists(x, y, z)) return;
        if (world.isAirBlock(x, y, z) || !matchesBinding(world.getTileEntity(x, y, z), binding))
            ItemUniversalWand.setAeBinding(player, wand, null);
    }

    private static boolean hasLocation(NBTTagCompound binding) {
        return binding.hasKey("dimension", 3) && binding.hasKey("x", 3)
            && binding.hasKey("y", 3)
            && binding.hasKey("z", 3);
    }

    private static boolean matchesBinding(TileEntity tile, NBTTagCompound binding) {
        return tile instanceof IWirelessAccessPoint && !tile.isInvalid()
            && (!binding.hasKey("serial", 4) || (tile instanceof ILocatable locatable
                && binding.getLong("serial") == locatable.getLocatableSerial()));
    }

    private static IWirelessAccessPoint resolve(NBTTagCompound binding) {
        if (!hasLocation(binding)) return null;
        World world = DimensionManager.getWorld(binding.getInteger("dimension"));
        int x = binding.getInteger("x"), y = binding.getInteger("y"), z = binding.getInteger("z");
        if (world == null || !world.blockExists(x, y, z) || world.isAirBlock(x, y, z)) return null;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!matchesBinding(tile, binding)) return null;
        IWirelessAccessPoint point = (IWirelessAccessPoint) tile;
        return point.isActive() ? point : null;
    }

    private static boolean canInsert(EntityPlayerMP player, IGrid grid) {
        if (grid == null) return false;
        ISecurityGrid security = grid.getCache(ISecurityGrid.class);
        return security != null && security.hasPermission(player, SecurityPermissions.INJECT);
    }

    /** Returns the actual remainder. Commit exceptions must be quarantined by the caller. */
    static long insert(EntityPlayerMP player, Destination destination, ItemStack template, long count) {
        if (!destination.aePriority || count <= 0) return count;
        IMEMonitor<IAEItemStack> inventory;
        IAEItemStack request;
        PlayerSource source;
        try {
            IWirelessAccessPoint point = resolve(destination.binding);
            if (point == null) return count;
            IGrid grid = point.getGrid();
            if (!canInsert(player, grid)) return count;
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            if (storage == null) return count;
            inventory = storage.getItemInventory();
            if (inventory == null) return count;
            request = AEApi.instance()
                .storage()
                .createItemStack(template.copy());
            if (request == null) return count;
            request.setStackSize(count);
            source = new PlayerSource(player, point);
        } catch (RuntimeException failure) {
            GTNeverReset.LOG.warn("Universal Wand AE unavailable before insertion; using fallback", failure);
            return count;
        }
        try {
            IAEItemStack remainder = inventory.injectItems(request, Actionable.MODULATE, source);
            if (remainder == null) return 0;
            long remaining = remainder.getStackSize();
            if (remaining < 0 || remaining > count
                || !ItemInputKey.of(template)
                    .equals(ItemInputKey.of(remainder.getItemStack())))
                throw new IllegalStateException("Invalid AE insertion remainder");
            return remaining;
        } catch (RuntimeException failure) {
            throw new UnknownInsertionOutcome(failure);
        }
    }
}
