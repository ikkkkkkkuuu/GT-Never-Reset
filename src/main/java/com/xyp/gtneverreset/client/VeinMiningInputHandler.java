package com.xyp.gtneverreset.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.MouseEvent;

import org.lwjgl.input.Mouse;

import com.xyp.gtneverreset.common.item.items.VeinMiningPickaxe;
import com.xyp.gtneverreset.common.item.items.VeinMiningSettings;
import com.xyp.gtneverreset.common.item.items.VeinMiningSettings.Setting;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.VeinMiningSettingPacket;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class VeinMiningInputHandler {

    /**
     * 处理鼠标滚轮事件
     * Handle mouse wheel events for Vein Mining Pickaxe adjustments
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onMouseEvent(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || event.dwheel == 0) return;

        ItemStack held = player.getCurrentEquippedItem();
        if (held == null) return;

        if (!(held.getItem() instanceof VeinMiningPickaxe)) return;

        boolean rightClickHeld = Mouse.isButtonDown(1);
        Setting setting;
        String subtitle;
        if (player.isSneaking() && !rightClickHeld) {
            setting = Setting.Range;
            // #tr Tooltip_VeinMiningPickaxe_00
            // # Maximum gap between matching blocks: %d
            // # zh_CN 符合条件的方块之间最大间隔：%d 格
            subtitle = "Tooltip_VeinMiningPickaxe_00";
        } else if (!player.isSneaking() && rightClickHeld) {
            setting = Setting.Amount;
            // #tr Tooltip_VeinMiningPickaxe_01
            // # Maximum blocks per vein: %d
            // # zh_CN 单次连锁挖掘上限：%d 个方块
            subtitle = "Tooltip_VeinMiningPickaxe_01";
        } else {
            return;
        }

        int value = setting.scroll(held, event.dwheel);
        if (value == setting.read(held)) return;
        int slot = player.inventory.currentItem;
        if (!VeinMiningSettings.apply(player.inventory, slot, setting, value)) return;
        if (mc.theWorld.isRemote && held.getItem() instanceof VeinMiningPickaxe display) {
            display.showSubtitle(subtitle, value);
        }
        NetWorkHandler.sendToServer(new VeinMiningSettingPacket(slot, setting, value));
        event.setCanceled(true);
    }

}
