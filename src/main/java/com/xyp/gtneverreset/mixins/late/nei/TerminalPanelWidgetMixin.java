package com.xyp.gtneverreset.mixins.late.nei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.client.gui.GuiQuickEncodingTerminal;
import com.xyp.gtneverreset.common.item.items.ItemWirelessDualInterfaceTerminal;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.terminal.TerminalItemRequestPacket;
import com.xyp.gtneverreset.utils.inventory.WirelessTerminalSession;

import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.client.gui.AEBaseGui;
import appeng.container.AEBaseContainer;
import codechicken.nei.PanelWidget;

@Mixin(value = PanelWidget.class, remap = false)
public abstract class TerminalPanelWidgetMixin {

    @Shadow
    public ItemStack draggedStack;

    @Shadow
    public abstract ItemStack getStackMouseOver(int mouseX, int mouseY);

    @Inject(method = "handleClick", at = @At("HEAD"), cancellable = true)
    private void requestNetworkItem(int mouseX, int mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (button != 2) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (!gtneverreset$canRequestFromScreen(minecraft.currentScreen)) return;
        ItemStack stack = getStackMouseOver(mouseX, mouseY);
        if (stack == null || minecraft.thePlayer == null) return;
        boolean shift = GuiScreen.isShiftKeyDown();
        boolean control = GuiScreen.isCtrlKeyDown();
        if (shift || control) {
            if (WirelessTerminalSession.findDualInterfaceTerminal(minecraft.thePlayer) < 0) return;
            NetWorkHandler.sendToServer(new TerminalItemRequestPacket(stack, control));
        } else if (!(minecraft.currentScreen instanceof GuiQuickEncodingTerminal terminal)
            || !terminal.requestNeiCraft(stack)) {
                return;
            }
        draggedStack = null;
        callback.setReturnValue(true);
    }

    private static boolean gtneverreset$canRequestFromScreen(GuiScreen screen) {
        if (screen instanceof GuiQuickEncodingTerminal) return true;
        if (!(screen instanceof AEBaseGui)) return screen instanceof GuiContainer;
        return screen instanceof AEBaseGui gui && gui.inventorySlots instanceof AEBaseContainer container
            && container.getTarget() instanceof IGuiItemObject host
            && host.getItemStack() != null
            && host.getItemStack()
                .getItem() instanceof ItemWirelessDualInterfaceTerminal;
    }
}
