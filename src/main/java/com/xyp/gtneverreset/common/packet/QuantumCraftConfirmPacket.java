package com.xyp.gtneverreset.common.packet;

import net.minecraft.client.Minecraft;

import com.xyp.gtneverreset.common.crafting.quantum.ae2.IQuantumCraftConfirm;
import com.xyp.gtnrlib.network.ClientboundPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class QuantumCraftConfirmPacket extends ClientboundPacket {

    private boolean quantumJob;

    public QuantumCraftConfirmPacket() {}

    public QuantumCraftConfirmPacket(boolean quantumJob) {
        this.quantumJob = quantumJob;
    }

    @Override
    protected void read(ByteBuf buf) {
        this.quantumJob = buf.readBoolean();
    }

    @Override
    protected void write(ByteBuf buf) {
        buf.writeBoolean(this.quantumJob);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft minecraft) {
        var connection = minecraft.getNetHandler();
        minecraft.func_152344_a(() -> {
            if (minecraft.getNetHandler() == connection && minecraft.thePlayer != null
                && minecraft.thePlayer.openContainer instanceof IQuantumCraftConfirm container) {
                container.gtnr$setQuantumJob(quantumJob);
            }
        });
    }

}
