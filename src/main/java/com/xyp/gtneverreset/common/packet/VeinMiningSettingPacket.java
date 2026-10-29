package com.xyp.gtneverreset.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.network.play.server.S2FPacketSetSlot;

import com.xyp.gtneverreset.common.item.items.VeinMiningSettings;
import com.xyp.gtneverreset.common.item.items.VeinMiningSettings.Setting;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** Requests one bounded pickaxe setting; arbitrary item NBT is never accepted. */
public class VeinMiningSettingPacket implements IMessage {

    private int slot;
    private Setting setting;
    private int value;

    public VeinMiningSettingPacket() {}

    public VeinMiningSettingPacket(int slot, Setting setting, int value) {
        this.slot = slot;
        this.setting = setting;
        this.value = value;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        if (buf.readableBytes() != 6) throw new IllegalArgumentException("Invalid pickaxe setting payload");
        slot = buf.readUnsignedByte();
        int settingId = buf.readUnsignedByte();
        if (settingId >= Setting.values().length) throw new IllegalArgumentException("Invalid pickaxe setting");
        setting = Setting.values()[settingId];
        value = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(slot);
        buf.writeByte(setting.ordinal());
        buf.writeInt(value);
    }

    boolean apply(InventoryPlayer inventory) {
        return VeinMiningSettings.apply(inventory, slot, setting, value);
    }

    public void handle(EntityPlayerMP player) {
        if (!player.isDead && apply(player.inventory)) {
            player.inventoryContainer.detectAndSendChanges();
            Slot target = player.inventoryContainer.getSlotFromInventory(player.inventory, slot);
            player.playerNetServerHandler.sendPacket(
                new S2FPacketSetSlot(player.inventoryContainer.windowId, target.slotNumber, target.getStack()));
        }
    }
}
