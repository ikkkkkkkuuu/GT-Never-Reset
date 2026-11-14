package com.xyp.gtneverreset.common.packet;

import net.minecraft.client.Minecraft;

import com.xyp.gtneverreset.common.crafting.quantum.ae2.IQuantumCraftAmountGui;
import com.xyp.gtnrlib.network.ClientboundPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class QuantumCraftingAvailabilityPacket extends ClientboundPacket {

    private boolean available;

    public QuantumCraftingAvailabilityPacket() {}

    public QuantumCraftingAvailabilityPacket(boolean available) {
        this.available = available;
    }

    @Override
    protected void read(ByteBuf buf) {
        this.available = buf.readBoolean();
    }

    @Override
    protected void write(ByteBuf buf) {
        buf.writeBoolean(this.available);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft minecraft) {
        var connection = minecraft.getNetHandler();
        minecraft.func_152344_a(() -> {
            if (minecraft.getNetHandler() == connection
                && minecraft.currentScreen instanceof IQuantumCraftAmountGui gui) {
                gui.gtnr$setQuantumAvailable(available);
            }
        });
    }

}
