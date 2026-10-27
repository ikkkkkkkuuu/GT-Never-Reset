package com.xyp.gtneverreset.mixins.late.appliedenergistics2;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.client.gui.CompactTerminalSwitchButton;
import com.xyp.gtneverreset.client.gui.QuickTerminalButtonTextures;
import com.xyp.gtneverreset.common.item.items.ItemWirelessDualInterfaceTerminal;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.terminal.CPacketSwitchGuis;
import com.xyp.gtneverreset.utils.enums.WirelessTerminalGuiType;

import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.storage.ITerminalHost;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiCraftingTerm;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.helpers.WirelessTerminalGuiObject;

/**
 * Adds a "switch back to the ME Wireless Dual Interface Terminal" button to the left side of AE2's crafting terminal
 * GUI, but only when that GUI was opened from our dual interface terminal (i.e. its host is a wireless crafting
 * terminal wrapping an {@link ItemWirelessDualInterfaceTerminal}). This is the return path of the button added to the
 * dual interface terminal's pattern panel; see {@link WirelessTerminalGuiType#WirelessCraftingTerminal}.
 */
@Mixin(GuiCraftingTerm.class)
public abstract class MixinGuiCraftingTerm extends AEBaseGui {

    private CompactTerminalSwitchButton gtneverreset$switchBackButton = null;

    public MixinGuiCraftingTerm(Container container) {
        super(container);
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    public void gtneverreset$addSwitchBackButton(CallbackInfo ci) {
        if (!gtneverreset$openedFromDualInterfaceTerminal()) return;
        QuickTerminalButtonTextures.refresh();
        // Append both controls to AE2's existing left-side column. Deriving the
        // next position from the live buttons keeps the layout valid for every
        // terminal height and terminal-style setting.
        int x = this.guiLeft - 18;
        int y = this.guiTop;
        GuiImgButton pinsButton = null;
        for (Object o : this.buttonList) {
            if (o instanceof GuiImgButton pinsBtn && pinsBtn.getSetting() == Settings.ACTIONS
                && pinsBtn.getCurrentValue() == ActionItems.PINS) {
                pinsButton = pinsBtn;
            } else if (o instanceof GuiButton button && button.visible && button.xPosition == x) {
                y = Math.max(y, button.yPosition + 20);
            }
        }
        if (pinsButton != null) {
            pinsButton.xPosition = x;
            pinsButton.yPosition = y;
            y += 20;
        }
        // #tr gtneverreset.tooltip.switch_to_dual_interface_terminal
        // # Switch to Dual Interface Terminal
        // # zh_CN 切换到二合一接口终端
        this.gtneverreset$switchBackButton = new CompactTerminalSwitchButton(
            I18n.format("gtneverreset.tooltip.switch_to_dual_interface_terminal"));
        this.gtneverreset$switchBackButton.xPosition = x;
        this.gtneverreset$switchBackButton.yPosition = y;
        this.buttonList.add(this.gtneverreset$switchBackButton);
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void gtneverreset$onSwitchBack(GuiButton btn, CallbackInfo ci) {
        if (this.gtneverreset$switchBackButton != null && btn == this.gtneverreset$switchBackButton) {
            AEBaseGui.setSwitchingGuis(true);
            NetWorkHandler.sendToServer(new CPacketSwitchGuis(WirelessTerminalGuiType.WirelessDualInterfaceTerminal));
        }
    }

    private boolean gtneverreset$openedFromDualInterfaceTerminal() {
        if (!(this.inventorySlots instanceof ContainerMEMonitorable)) return false;
        ITerminalHost host = ((AccessorContainerMEMonitorable) this.inventorySlots).getHost();
        if (!(host instanceof WirelessTerminalGuiObject wireless)) return false;
        ItemStack terminal = wireless.getItemStack();
        return terminal != null && terminal.getItem() instanceof ItemWirelessDualInterfaceTerminal;
    }
}
