package com.xyp.gtneverreset.mixins.late.appliedenergistics2;

import java.util.List;

import net.minecraft.inventory.Container;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.xyp.gtneverreset.api.mixinHelper.CraftingMonitorScreen;
import com.xyp.gtneverreset.client.gui.crafting.CraftingMonitorTooltip;

import appeng.api.storage.data.IAEStack;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiCraftingCPU;
import appeng.container.implementations.CraftingCpuEntry;

@Mixin(value = GuiCraftingCPU.class, remap = false)
public abstract class CraftingMonitorGuiMixin extends AEBaseGui implements CraftingMonitorScreen {

    @Unique
    private final CraftingMonitorTooltip gtnr$monitorTooltip = new CraftingMonitorTooltip();

    private CraftingMonitorGuiMixin(Container container) {
        super(container);
    }

    @WrapOperation(
        method = "drawEntry",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/client/gui/implementations/GuiCraftingCPU;buildTooltipMessage(Lappeng/api/storage/data/IAEStack;Ljava/util/List;Z)Ljava/lang/String;"))
    private String gtnr$appendMachineDetails(GuiCraftingCPU screen, IAEStack<?> visual, List<String> lines,
        boolean changed, Operation<String> original, @Local(argsOnly = true) CraftingCpuEntry entry) {
        if (entry.hasActiveAmount() || entry.hasPendingAmount()) {
            gtnr$monitorTooltip.append(entry.getStack(), lines, inventorySlots.windowId);
        }
        return original.call(screen, visual, lines, changed);
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), remap = true)
    private void gtnr$beginMonitorFrame(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        gtnr$monitorTooltip.beginFrame();
    }

    @Inject(method = "drawScreen", at = @At("RETURN"), remap = true)
    private void gtnr$drawMonitorMenu(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        gtnr$monitorTooltip.endFrame(mouseX, mouseY, fontRendererObj);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = true)
    private void gtnr$clickMonitorMenu(int mouseX, int mouseY, int button, CallbackInfo ci) {
        if (gtnr$monitorTooltip.click(mouseX, mouseY, button, inventorySlots.windowId, guiLeft, guiTop, xSize, ySize)) {
            ci.cancel();
        }
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true, remap = true)
    private void gtnr$closeMonitorMenu(char character, int key, CallbackInfo ci) {
        if (gtnr$monitorTooltip.keyPressed(key)) ci.cancel();
    }

    @Inject(method = "initGui", at = @At("RETURN"), remap = true)
    private void gtnr$resetMonitorMenu(CallbackInfo ci) {
        gtnr$monitorTooltip.clear();
    }

    @Inject(method = "clearItems", at = @At("HEAD"))
    private void gtnr$clearMonitor(CallbackInfo ci) {
        gtnr$monitorTooltip.clear();
    }

    @Inject(method = "postVisualEntryUpdate", at = @At("HEAD"))
    private void gtnr$resetMonitorOnCpuChange(CraftingCpuEntry[] updates, boolean clearFirst, int remainingOperations,
        CallbackInfo ci) {
        if (clearFirst) gtnr$monitorTooltip.clear();
    }

    @Override
    public CraftingMonitorTooltip gtnr$craftingMonitor() {
        return gtnr$monitorTooltip;
    }
}
