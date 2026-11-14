package com.xyp.gtneverreset.qa;

import java.nio.file.Files;
import java.nio.file.Paths;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(modid = "packetcontractqa", name = "Packet Contract QA", version = "1", dependencies = "after:gtneverreset")
public final class PacketContractChecks {

    private boolean done;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END || Minecraft.getMinecraft().currentScreen == null) return;
        done = true;
        try {
            verifyBytes(new com.xyp.gtneverreset.common.packet.SwitchGuisPacket(), new byte[] { -1, 1 });
            verifyBytes(
                new com.xyp.gtneverreset.common.packet.UniversalWandNetwork.TravelFallbackRequest(),
                new byte[] { 1, 2, 3, 4 });
            verifyBytes(
                new com.xyp.gtneverreset.common.packet.UniversalWandNetwork.TravelFallbackGrant(),
                new byte[] { 1, 2, 3, 4 });
            verifyBytes(
                new com.xyp.gtneverreset.common.packet.UniversalWandNetwork.TravelFallbackFinish(),
                new byte[] { 1, 2, 3, 4 });
            verifyBytes(
                new com.xyp.gtneverreset.common.packet.UniversalWandNetwork.FeatureMessage(),
                new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
            var malformed = new com.xyp.gtneverreset.common.packet.SwitchGuisPacket();
            malformed.fromBytes(io.netty.buffer.Unpooled.EMPTY_BUFFER);
            if (!malformed.isInvalid()) throw new AssertionError("truncated server payload accepted");
            new com.xyp.gtnrlib.network.PayloadHandler.Server<com.xyp.gtneverreset.common.packet.SwitchGuisPacket>()
                .onMessage(malformed, null);
            boolean[] contextual = { false };
            var probe = new com.xyp.gtnrlib.network.ClientboundPacket() {

                @Override
                public void handleClient(Minecraft minecraft) {
                    throw new AssertionError("connection context bypassed");
                }

                @Override
                public void handleClient(Minecraft minecraft,
                    cpw.mods.fml.common.network.simpleimpl.MessageContext context) {
                    contextual[0] = true;
                }
            };
            new com.xyp.gtnrlib.network.PayloadHandler.Client<com.xyp.gtnrlib.network.ClientboundPacket>()
                .onMessage(probe, null);
            if (!contextual[0]) throw new AssertionError("client payload not dispatched");
            Files.writeString(Paths.get("result.txt"), "PASS");
            System.out
                .println("PACKET_CONTRACT_QA PASS: wire bytes, truncated packet rejection and contextual dispatch");
            FMLCommonHandler.instance()
                .exitJava(0, false);
        } catch (Throwable failure) {
            failure.printStackTrace();
            FMLCommonHandler.instance()
                .exitJava(1, false);
        }
    }

    private static void verifyBytes(cpw.mods.fml.common.network.simpleimpl.IMessage packet, byte[] expected) {
        var input = io.netty.buffer.Unpooled.wrappedBuffer(expected);
        var output = io.netty.buffer.Unpooled.buffer();
        try {
            packet.fromBytes(input);
            packet.toBytes(output);
            byte[] actual = new byte[output.readableBytes()];
            output.readBytes(actual);
            if (!java.util.Arrays.equals(actual, expected))
                throw new AssertionError("wire bytes changed: " + packet.getClass());
        } finally {
            input.release();
            output.release();
        }
    }
}
