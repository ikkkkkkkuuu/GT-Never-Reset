package com.xyp.gtneverreset.common.me;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.TileEntityGuiFactory;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.parts.IPartHost;
import appeng.core.sync.GuiBridge;
import appeng.helpers.IInterfaceHost;
import appeng.tile.crafting.TileMolecularAssembler;
import appeng.util.Platform;
import cpw.mods.fml.common.eventhandler.Event;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.CommonMetaTileEntity;

public final class CraftingMonitorGuiAccess {

    private static final double MUI_REACH_SQUARED = 64D;
    private static final Map<EntityPlayerMP, OpenTarget> OPEN_TARGETS = new WeakHashMap<>();

    private CraftingMonitorGuiAccess() {}

    static boolean canOpen(EntityPlayerMP player, TileEntity target, ForgeDirection side) {
        if (target == null || !isWatching(player, target) || !mayUseBlock(player, target, side)) return false;
        if (target instanceof IGregTechTileEntity host) {
            return host.canAccessData() && host.getMetaTileEntity() instanceof CommonMetaTileEntity
                && host.getMetaTileEntity()
                    .isAccessAllowed(player)
                && (!(target instanceof BaseMetaTileEntity base) || !base.privateAccess()
                    || player.getUniqueID()
                        .equals(base.getOwnerUuid()));
        }
        GuiBridge gui = aeGui(target, side);
        return gui == null ? target instanceof IGuiHolder<?>
            : gui.hasPermissions(
                target,
                target.xCoord,
                target.yCoord,
                target.zCoord,
                side == null ? ForgeDirection.UNKNOWN : side,
                player);
    }

    static boolean open(EntityPlayerMP player, IGrid grid, TileEntity target, ForgeDirection side) {
        if (!canOpen(player, target, side)) return false;
        ForgeDirection face = side == null || side == ForgeDirection.UNKNOWN ? ForgeDirection.UP : side;
        World world = target.getWorldObj();
        Container previous = player.openContainer;
        PlayerInteractEvent interaction = ForgeEventFactory.onPlayerInteract(
            player,
            PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK,
            target.xCoord,
            target.yCoord,
            target.zCoord,
            face.ordinal(),
            world);
        if (interaction.isCanceled() || interaction.useBlock == Event.Result.DENY
            || player.openContainer != previous
            || !canOpen(player, target, side)) return false;

        GuiBridge gui = aeGui(target, side);
        if (gui != null) {
            Platform.openGUI(player, target, side == null ? ForgeDirection.UNKNOWN : side, gui);
            return player.openContainer != previous;
        }

        OpenTarget permit = new OpenTarget(target, grid, face, previous);
        OPEN_TARGETS.put(player, permit);
        boolean opened = false;
        try {
            if (target instanceof IGregTechTileEntity host) {
                ((CommonMetaTileEntity) host.getMetaTileEntity()).openGui(player);
            } else {
                TileEntityGuiFactory.INSTANCE.open(player, target.xCoord, target.yCoord, target.zCoord);
            }
            opened = player.openContainer != previous;
            if (opened) permit.container = new WeakReference<>(player.openContainer);
            return opened;
        } finally {
            permit.opening = false;
            if (!opened) OPEN_TARGETS.remove(player);
        }
    }

    /** Applies only to the normal distance check for the exact tile and container opened by the monitor. */
    public static double interactionDistance(double nativeDistance, EntityPlayer player, World world, int x, int y,
        int z) {
        if (nativeDistance <= MUI_REACH_SQUARED || !(player instanceof EntityPlayerMP serverPlayer))
            return nativeDistance;
        OpenTarget permit = OPEN_TARGETS.get(serverPlayer);
        if (permit == null) return nativeDistance;

        TileEntity target = permit.tile.get();
        IGrid grid = permit.grid.get();
        Container container = permit.container.get();
        ISecurityGrid security = grid == null ? null : grid.getCache(ISecurityGrid.class);
        if (target == null || security == null
            || container == null
            || !permit.opening && serverPlayer.openContainer != container
            || !canOpen(serverPlayer, target, permit.face)
            || !security.hasPermission(serverPlayer, SecurityPermissions.BUILD)) {
            OPEN_TARGETS.remove(serverPlayer);
            return nativeDistance;
        }
        return target.getWorldObj() == world && target.xCoord == x && target.yCoord == y && target.zCoord == z ? 0D
            : nativeDistance;
    }

    private static boolean isWatching(EntityPlayerMP player, TileEntity tile) {
        return tile.getWorldObj() == player.worldObj && player.worldObj instanceof WorldServer world
            && !tile.isInvalid()
            && world.getPlayerManager()
                .isPlayerWatchingChunk(player, tile.xCoord >> 4, tile.zCoord >> 4)
            && world.getChunkProvider()
                .chunkExists(tile.xCoord >> 4, tile.zCoord >> 4)
            && world.getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) == tile;
    }

    private static boolean mayUseBlock(EntityPlayerMP player, TileEntity tile, ForgeDirection side) {
        int face = side == null || side == ForgeDirection.UNKNOWN ? ForgeDirection.UP.ordinal() : side.ordinal();
        return !MinecraftServer.getServer()
            .isBlockProtected(tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord, player)
            && player.canPlayerEdit(tile.xCoord, tile.yCoord, tile.zCoord, face, null);
    }

    private static GuiBridge aeGui(TileEntity target, ForgeDirection side) {
        if (target instanceof TileMolecularAssembler) return GuiBridge.GUI_MAC;
        if (target instanceof IInterfaceHost || target instanceof IPartHost host && side != null
            && side != ForgeDirection.UNKNOWN
            && host.getPart(side) instanceof IInterfaceHost) return GuiBridge.GUI_INTERFACE;
        return null;
    }

    private static final class OpenTarget {

        private final WeakReference<TileEntity> tile;
        private final WeakReference<IGrid> grid;
        private final ForgeDirection face;
        private WeakReference<Container> container;
        private boolean opening = true;

        private OpenTarget(TileEntity tile, IGrid grid, ForgeDirection face, Container container) {
            this.tile = new WeakReference<>(tile);
            this.grid = new WeakReference<>(grid);
            this.face = face;
            this.container = new WeakReference<>(container);
        }
    }
}
