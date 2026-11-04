package com.xyp.gtneverreset.common.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.xyp.gtneverreset.compat.gregtech.GregTechWrenchAnimation;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.items.IDMetaTool01;
import gregtech.common.items.MetaGeneratedTool01;
import io.netty.buffer.ByteBuf;

public final class WrenchTurnPacket implements IMessage {

    private int dimension, slot;

    public WrenchTurnPacket() {}

    private WrenchTurnPacket(EntityPlayer player) {
        dimension = player.dimension;
        slot = player.inventory.currentItem;
    }

    public static boolean isManualWrench(ItemStack stack) {
        return stack != null && stack.stackSize > 0
            && stack.getItem() == MetaGeneratedTool01.INSTANCE
            && stack.getItemDamage() == IDMetaTool01.WRENCH.ID;
    }

    public static void notifyUse(EntityPlayer player) {
        if (player instanceof EntityPlayerMP mp && !(mp instanceof FakePlayer) && isManualWrench(mp.getHeldItem()))
            NetWorkHandler.sendTo(new WrenchTurnPacket(mp), mp);
    }

    @Override
    public void fromBytes(ByteBuf in) {
        dimension = in.readInt();
        slot = in.readUnsignedByte();
    }

    @Override
    public void toBytes(ByteBuf out) {
        out.writeInt(dimension);
        out.writeByte(slot);
    }

    @SideOnly(Side.CLIENT)
    private void handleClient() {
        var mc = Minecraft.getMinecraft();
        var connection = mc.getNetHandler();
        mc.func_152344_a(() -> {
            if (connection != null && mc.getNetHandler() == connection)
                GregTechWrenchAnimation.INSTANCE.accepted(dimension, slot);
        });
    }

    public static final class Handler implements IMessageHandler<WrenchTurnPacket, IMessage> {

        @Override
        public IMessage onMessage(WrenchTurnPacket message, MessageContext context) {
            message.handleClient();
            return null;
        }
    }
}
