package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.xyp.gtneverreset.common.item.items.ItemWirelessConnector;
import com.xyp.gtnrlib.network.ServerboundPacket;

import cpw.mods.fml.common.eventhandler.Event;
import io.netty.buffer.ByteBuf;

/** The request contains only the hit position; selection, ownership and sneak state come from the server. */
public final class PackagedConnectorPacket extends ServerboundPacket {

    private int x, y, z, face;

    public PackagedConnectorPacket() {}

    public PackagedConnectorPacket(int x, int y, int z, int face) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
    }

    @Override
    protected void read(ByteBuf buffer) {
        x = buffer.readInt();
        y = buffer.readInt();
        z = buffer.readInt();
        face = buffer.readUnsignedByte();
    }

    @Override
    protected void write(ByteBuf buffer) {
        buffer.writeInt(x);
        buffer.writeInt(y);
        buffer.writeInt(z);
        buffer.writeByte(face);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        ItemStack held = player.getHeldItem();
        var world = player.getServerForPlayer();
        if (player.isDead || held == null
            || !(held.getItem() instanceof ItemWirelessConnector)
            || this.face > 5
            || this.y < 0
            || this.y >= world.getHeight()
            || !world.getChunkProvider()
                .chunkExists(this.x >> 4, this.z >> 4))
            return;
        double reach = player.theItemInWorldManager.getBlockReachDistance() + 1;
        if (player.getDistanceSq(this.x + .5, this.y + .5, this.z + .5) > reach * reach || MinecraftServer.getServer()
            .isBlockProtected(world, this.x, this.y, this.z, player)
            || !player.canPlayerEdit(this.x, this.y, this.z, this.face, held)) return;
        PlayerInteractEvent event = ForgeEventFactory.onPlayerInteract(
            player,
            PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK,
            this.x,
            this.y,
            this.z,
            this.face,
            world);
        if (!event.isCanceled() && event.useItem != Event.Result.DENY) {
            ItemWirelessConnector.use(player, this.x, this.y, this.z, this.face);
            player.inventory.markDirty();
            player.inventoryContainer.detectAndSendChanges();
        }
    }
}
