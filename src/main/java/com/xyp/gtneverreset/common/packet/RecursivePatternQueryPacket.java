package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.crafting.cycle.RecursivePatternFinder;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.me.cache.CraftingGridCache;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/**
 * Asks for the crafting cycles of the request the player is confirming: by the player, to be shown, or by the screen
 * once the calculation is done, to mark them in the crafting tree.
 */
public class RecursivePatternQueryPacket implements IMessage {

    private boolean manual;

    public RecursivePatternQueryPacket() {}

    public RecursivePatternQueryPacket(boolean manual) {
        this.manual = manual;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.manual);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.manual = buf.readBoolean();
    }

    public void handleServer(EntityPlayerMP player) {
        if (!(player.openContainer instanceof ContainerCraftConfirm container)) return;
        var request = container.getItemToCraft();
        IGrid grid;
        try {
            grid = container.getGrid();
        } catch (RuntimeException e) {
            grid = null;
        }
        if (request == null || grid == null
            || !(grid.getCache(ICraftingGrid.class) instanceof CraftingGridCache crafting)) {
            return;
        }
        var cycles = RecursivePatternFinder.find(crafting, request, player.worldObj);
        NetWorkHandler.sendTo(new RecursivePatternResultPacket(request, cycles, this.manual), player);
    }

}
