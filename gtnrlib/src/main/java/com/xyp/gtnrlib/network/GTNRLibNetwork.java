package com.xyp.gtnrlib.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.mod.ModList;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class GTNRLibNetwork {

    private static SimpleNetworkWrapper channel;

    private GTNRLibNetwork() {}

    public static void register() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(GTNRLib.MODID);
        channel
            .registerMessage(new PayloadHandler.Client<CommandTreePacket>(), CommandTreePacket.class, 0, Side.CLIENT);
        if (ModList.GregTech.isModLoaded()) GregTechToolUsePacket.register(channel, 1);
    }

    static void sendTo(ClientboundPacket packet, EntityPlayerMP player) {
        channel.sendTo(packet, player);
    }

    public static void sendCommands(byte[] data, EntityPlayerMP player) {
        channel.sendTo(new CommandTreePacket(data), player);
    }
}
