package com.xyp.gtnrlib.network;

import net.minecraft.client.Minecraft;

import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.client.command.NetHandlerPlayClientExtras;
import com.xyp.gtnrlib.command.brigadier.CommandDispatcher;
import com.xyp.gtnrlib.command.tree.CommandTreeCodec;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class CommandTreePacket extends ClientboundPacket {

    public static final int MAX_TREE_BYTES = 1024 * 1024;
    private byte[] treeData;

    public CommandTreePacket() {}

    public CommandTreePacket(byte[] data) {
        if (data.length > MAX_TREE_BYTES) throw new IllegalArgumentException("Command tree is too large");
        treeData = data;
    }

    @Override
    protected void read(ByteBuf buffer) {
        int length = buffer.readInt();
        if (length < 0 || length > MAX_TREE_BYTES || length > buffer.readableBytes()) {
            throw new IllegalArgumentException("Invalid command tree size: " + length);
        }
        treeData = new byte[length];
        buffer.readBytes(treeData);
    }

    @Override
    protected void write(ByteBuf buffer) {
        buffer.writeInt(treeData.length);
        buffer.writeBytes(treeData);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft minecraft) {
        var connection = minecraft.getNetHandler();
        if (connection == null) return;
        minecraft.func_152344_a(() -> {
            if (minecraft.getNetHandler() != connection) return;
            try {
                ((NetHandlerPlayClientExtras) connection)
                    .gtnrlib$setCommands(new CommandDispatcher<>(CommandTreeCodec.read(treeData)));
            } catch (RuntimeException exception) {
                GTNRLib.LOG.warn("could not read command tree from server", exception);
            }
        });
    }
}
