package com.xyp.gtneverreset.mixins.late.appliedenergistics2.quantum;

import net.minecraft.client.gui.GuiButton;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.client.quantum.QuantumCraftingClientState;
import com.xyp.gtneverreset.client.quantum.QuantumCraftingModeButton;
import com.xyp.gtneverreset.common.crafting.quantum.ae2.IQuantumCraftAmountGui;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.QuantumCraftingModePacket;

import appeng.client.gui.implementations.GuiAmount;
import appeng.client.gui.implementations.GuiCraftAmount;
import appeng.client.gui.widgets.GuiToggleButton;
import appeng.core.AEConfig;

@Mixin(value = GuiCraftAmount.class, remap = false)
public abstract class MixinGuiCraftAmount extends GuiAmount implements IQuantumCraftAmountGui {

    @Shadow(remap = false)
    private GuiToggleButton liteMode;

    @Shadow(remap = false)
    private boolean isControlButtonPressed;

    @Unique
    private QuantumCraftingModeButton gtnr$quantumMode;

    @Unique
    private boolean gtnr$quantumAvailable;

    public MixinGuiCraftAmount() {
        super(null);
    }

    @Inject(method = "initGui", at = @At("TAIL"), remap = true)
    private void gtnr$addQuantumModeButton(CallbackInfo ci) {
        this.gtnr$quantumMode = new QuantumCraftingModeButton(this.guiLeft - 18, this.guiTop + 48);
        this.gtnr$quantumMode.setState(QuantumCraftingClientState.isQuantumCraftingMode());
        this.buttonList.add(this.gtnr$quantumMode);
        this.gtnr$updateQuantumModeVisibility();
        // The answer tells whether the network can run quantum jobs
        NetWorkHandler.sendToServer(new QuantumCraftingModePacket(this.gtnr$quantumMode.getState()));
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true, remap = true)
    private void gtnr$toggleQuantumMode(GuiButton btn, CallbackInfo ci) {
        if (this.gtnr$quantumMode == null) return;

        if (btn == this.gtnr$quantumMode) {
            final boolean enabled = !this.gtnr$quantumMode.getState();
            if (enabled && AEConfig.instance.getUseLiteCraftingMode()) {
                this.liteMode.setState(AEConfig.instance.toggleUseLiteCraftingMode());
            }
            this.gtnr$setQuantumMode(enabled);
            ci.cancel();
        } else if (btn == this.liteMode && this.gtnr$quantumMode.getState()
            && !AEConfig.instance.getUseLiteCraftingMode()) {
                // Lite mode is about to turn on
                this.gtnr$setQuantumMode(false);
            }
    }

    @Inject(method = "setControlButtonPressed", at = @At("TAIL"))
    private void gtnr$hideQuantumModeWhileEditing(boolean pressed, CallbackInfo ci) {
        this.gtnr$updateQuantumModeVisibility();
    }

    @Override
    public void gtnr$setQuantumAvailable(boolean available) {
        this.gtnr$quantumAvailable = available;
        if (available && this.gtnr$quantumMode != null
            && this.gtnr$quantumMode.getState()
            && AEConfig.instance.getUseLiteCraftingMode()) {
            this.liteMode.setState(AEConfig.instance.toggleUseLiteCraftingMode());
        }
        this.gtnr$updateQuantumModeVisibility();
    }

    @Unique
    private void gtnr$setQuantumMode(boolean enabled) {
        this.gtnr$quantumMode.setState(enabled);
        QuantumCraftingClientState.setQuantumCraftingMode(enabled);
        NetWorkHandler.sendToServer(new QuantumCraftingModePacket(enabled));
    }

    @Unique
    private void gtnr$updateQuantumModeVisibility() {
        if (this.gtnr$quantumMode == null) return;
        final boolean visible = this.gtnr$quantumAvailable && !this.isControlButtonPressed;
        this.gtnr$quantumMode.visible = visible;
        this.gtnr$quantumMode.enabled = visible;
    }
}
