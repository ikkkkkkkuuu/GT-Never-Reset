package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtneverreset.common.crafting.quantum.ae2.QuantumCraftingMode;

import appeng.api.networking.IGrid;
import appeng.container.implementations.ContainerCraftAmount;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class QuantumCraftingModePacket implements IMessage {

    private boolean enabled;

    public QuantumCraftingModePacket() {}

    public QuantumCraftingModePacket(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.enabled = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.enabled);
    }

    public void handleServer(EntityPlayerMP player) {
        QuantumCraftingMode.setEnabled(player, this.enabled);
        if (!(player.openContainer instanceof ContainerCraftAmount container)) return;

        IGrid grid;
        try {
            grid = container.getGrid();
        } catch (RuntimeException e) {
            grid = null;
        }
        NetWorkHandler.sendTo(new QuantumCraftingAvailabilityPacket(QuantumCraftingMode.isAvailable(grid)), player);
    }

}
