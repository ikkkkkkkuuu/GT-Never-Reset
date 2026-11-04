package com.xyp.gtnrlib.network;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.xyp.gtnrlib.client.animation.FirstPersonItemActions;
import com.xyp.gtnrlib.client.animation.HmiFirstPerson;
import com.xyp.gtnrlib.compat.gregtech.GregTechWireCutterAnimation;

import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.items.IDMetaTool01;
import gregtech.common.items.MetaGeneratedTool01;
import io.netty.buffer.ByteBuf;

/** The two discriminators retain the original dimension and hotbar slot wire format. */
public abstract class GregTechToolUsePacket extends ClientboundPacket {

    private final IDMetaTool01 tool;
    private int dimension, slot;

    private GregTechToolUsePacket(IDMetaTool01 tool) {
        this.tool = tool;
    }

    public static int register(SimpleNetworkWrapper channel, int nextId) {
        channel.registerMessage(new PayloadHandler.Client<WrenchTurn>(), WrenchTurn.class, nextId++, Side.CLIENT);
        channel
            .registerMessage(new PayloadHandler.Client<WireCutterSnip>(), WireCutterSnip.class, nextId++, Side.CLIENT);
        return nextId;
    }

    public static boolean matches(ItemStack stack, IDMetaTool01 tool) {
        return stack != null && stack.stackSize > 0
            && stack.getItem() == MetaGeneratedTool01.INSTANCE
            && stack.getItemDamage() == tool.ID;
    }

    public static void notifyUse(EntityPlayer player, IDMetaTool01 tool) {
        if (!(player instanceof EntityPlayerMP mp) || mp instanceof FakePlayer || !matches(mp.getHeldItem(), tool))
            return;
        GregTechToolUsePacket packet;
        if (tool == IDMetaTool01.WRENCH) packet = new WrenchTurn();
        else if (tool == IDMetaTool01.WIRECUTTER) packet = new WireCutterSnip();
        else return;
        packet.dimension = mp.dimension;
        packet.slot = mp.inventory.currentItem;
        GTNRLibNetwork.sendTo(packet, mp);
    }

    @Override
    protected void read(ByteBuf buf) {
        dimension = buf.readInt();
        slot = buf.readUnsignedByte();
    }

    @Override
    protected void write(ByteBuf buf) {
        buf.writeInt(dimension);
        buf.writeByte(slot);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft mc) {
        var connection = mc.getNetHandler();
        if (connection == null) return;
        runInWorld(mc, () -> {
            var player = mc.thePlayer;
            if (mc.getNetHandler() != connection || player == null
                || player.dimension != dimension
                || player.inventory.currentItem != slot
                || !matches(player.getHeldItem(), tool)) return;
            if (tool == IDMetaTool01.WRENCH) FirstPersonItemActions.INSTANCE.trigger("gtneverreset:wrench_turn");
            else if (HmiFirstPerson.INSTANCE.handheldTool() instanceof GregTechWireCutterAnimation)
                HmiFirstPerson.INSTANCE.swing("gtneverreset:wire_cutter_snip");
        });
    }

    public static final class WrenchTurn extends GregTechToolUsePacket {

        public WrenchTurn() {
            super(IDMetaTool01.WRENCH);
        }
    }

    public static final class WireCutterSnip extends GregTechToolUsePacket {

        public WireCutterSnip() {
            super(IDMetaTool01.WIRECUTTER);
        }
    }
}
