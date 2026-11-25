package com.xyp.gtneverreset.mixins.late.appliedenergistics2;

import java.util.List;

import net.minecraft.inventory.Container;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.xyp.gtneverreset.api.mixinHelper.CraftingMonitorScreen;
import com.xyp.gtneverreset.client.gui.crafting.CraftingMonitorTooltip;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

import appeng.api.storage.data.IAEStack;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiCraftingCPU;
import appeng.container.implementations.CraftingCpuEntry;
import appeng.core.AEConfig;

@Mixin(value = GuiCraftingCPU.class, remap = false)
public abstract class CraftingMonitorGuiMixin extends AEBaseGui implements CraftingMonitorScreen {

    @Shadow
    @Final
    protected static int SECTION_LENGTH;
    @Shadow
    @Final
    protected static int SECTION_HEIGHT;
    @Shadow
    @Final
    private static int ITEMSTACK_LEFT_OFFSET;
    @Shadow
    @Final
    private static int ITEMSTACK_TOP_OFFSET;

    @Unique
    private final CraftingMonitorTooltip gtnr$monitorTooltip = new CraftingMonitorTooltip();

    private CraftingMonitorGuiMixin(Container container) {
        super(container);
    }

    @Inject(method = "drawEntry", at = @At(value = "NEW", target = "java/util/LinkedList"))
    private void gtnr$drawCellProgress(CraftingCpuEntry entry, int visibleIndex, int column, int row,
        IAEStack<?> lastHoveredStack, CallbackInfoReturnable<?> ci,
        @Share("gtnr$paintedCell") LocalBooleanRef painted) {
        if (!entry.hasActiveAmount() || !AEConfig.instance.useColoredCraftingStatus) return;
        // AE draws this part of the cell with a half-scale matrix.
        int x = 2 * (column * (1 + SECTION_LENGTH) + ITEMSTACK_LEFT_OFFSET);
        int y = 2 * (row * SECTION_HEIGHT + ITEMSTACK_TOP_OFFSET - 3);
        painted.set(
            gtnr$monitorTooltip.drawCellProgress(entry.getStack(), x, y, 2 * SECTION_LENGTH, 2 * SECTION_HEIGHT - 2));
    }

    @WrapOperation(
        method = "drawEntry",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/client/gui/implementations/GuiCraftingCPU;drawAmountLine(Ljava/lang/String;JIIIIIILjava/util/List;)I"))
    private int gtnr$drawReadableCellAmount(GuiCraftingCPU screen, String label, long amount, int column, int row,
        int negY, int downY, int color, int visibleIndex, List<String> lines, Operation<Integer> original,
        @Share("gtnr$paintedCell") LocalBooleanRef painted) {
        int textColor = painted.get() ? Variant.DARK_PURPLE.colors.fieldText() : color;
        return original.call(screen, label, amount, column, row, negY, downY, textColor, visibleIndex, lines);
    }

    @WrapOperation(
        method = "drawEntry",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/client/gui/implementations/GuiCraftingCPU;buildTooltipMessage(Lappeng/api/storage/data/IAEStack;Ljava/util/List;Z)Ljava/lang/String;"))
    private String gtnr$prepareMonitorCard(GuiCraftingCPU screen, IAEStack<?> visual, List<String> lines,
        boolean changed, Operation<String> original, @Local(argsOnly = true) CraftingCpuEntry entry) {
        int amountLines = lines.size();
        String nativeTooltip = original.call(screen, visual, lines, changed);
        IAEStack<?> monitored = entry.hasActiveAmount() || entry.hasPendingAmount() ? entry.getStack() : null;
        return gtnr$monitorTooltip.prepareCard(
            monitored,
            inventorySlots.windowId,
            nativeTooltip,
            lines,
            amountLines,
            width,
            height,
            fontRendererObj);
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), remap = true)
    private void gtnr$beginMonitorFrame(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        gtnr$monitorTooltip.beginFrame();
    }

    @Inject(method = "drawScreen", at = @At("RETURN"), remap = true)
    private void gtnr$drawMonitorOverlay(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        gtnr$monitorTooltip.endFrame(mouseX, mouseY, width, height, inventorySlots.windowId, fontRendererObj);
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
