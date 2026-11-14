package com.xyp.gtneverreset.mixins.late.appliedenergistics2.quantum;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.client.quantum.CraftConfirmCycles;
import com.xyp.gtneverreset.client.quantum.ICraftingCycleHost;
import com.xyp.gtneverreset.common.crafting.cycle.RecursivePatternFinder.Cycle;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.GuiSub;
import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.client.gui.widgets.GuiAeButton;
import appeng.client.gui.widgets.GuiCraftingTree;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.implementations.ContainerCraftConfirm;

/**
 * Finds the crafting cycles of the request once it is calculated, marks them in the crafting tree and the list, and
 * tells once where the machines of those the plan cannot start because of are. The button lists them all, the highlight
 * key highlights the cycle of what is under the mouse, and shift-clicking an item of a cycle in the list shows the
 * cycle as a ring in the tree view, left by its button or Esc.
 */
@Mixin(value = GuiCraftConfirm.class, remap = false)
public abstract class MixinGuiCraftConfirm extends GuiSub implements ICraftingCycleHost, CraftConfirmCycles.Host {

    @Shadow
    @Final
    private ContainerCraftConfirm ccc;

    @Shadow
    @Final
    private GuiCraftingTree craftingTree;

    @Shadow
    @Final
    private IItemList<IAEStack<?>> missing;

    @Shadow
    @Final
    private IItemList<IAEStack<?>> storage;

    @Shadow
    @Final
    private IItemList<IAEStack<?>> pending;

    @Shadow
    @Final
    private List<IAEStack<?>> filteredVisual;

    @Shadow
    private int tooltip;

    @Shadow
    private int rows;

    @Shadow
    private GuiTabButton switchDisplayMode;

    @Shadow
    private GuiCraftConfirm.DisplayMode displayMode;

    @Shadow
    private GuiAeButton hideAvailable;

    @Shadow
    private MEGuiTextField searchField;
    @Unique
    private CraftConfirmCycles gtnr$cycleController;

    @Unique
    private CraftConfirmCycles gtnr$cycles() {
        if (gtnr$cycleController == null) gtnr$cycleController = new CraftConfirmCycles(this);
        return gtnr$cycleController;
    }

    public MixinGuiCraftConfirm() {
        super(null);
    }

    @Inject(method = "initGui", at = @At("TAIL"), remap = true)
    private void gtnr$addButtons(CallbackInfo ci) {
        gtnr$cycles().addButtons();
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true, remap = true)
    private void gtnr$buttons(GuiButton button, CallbackInfo ci) {
        if (gtnr$cycles().buttons(button)) ci.cancel();
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), remap = true)
    private void gtnr$update(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        gtnr$cycles().update(mouseX, mouseY, partialTicks);
    }

    @Inject(method = "drawFG", at = @At("TAIL"))
    private void gtnr$ringSummary(int offsetX, int offsetY, int mouseX, int mouseY, CallbackInfo ci) {
        gtnr$cycles().ringSummary(offsetX, offsetY, mouseX, mouseY);
    }

    @Override
    public void gtnr$cyclesFound(IAEStack<?> request, List<Cycle> cycles, boolean manual) {
        gtnr$cycles().gtnr$cyclesFound(request, cycles, manual);
    }

    @Inject(method = "drawTreeFG", at = @At("HEAD"), cancellable = true)
    private void gtnr$drawRingWithoutTree(int offsetX, int offsetY, int mouseX, int mouseY, CallbackInfo ci) {
        if (gtnr$cycles().drawRingWithoutTree(offsetX, offsetY, mouseX, mouseY)) ci.cancel();
    }

    @Inject(method = "drawListFG", at = @At("HEAD"))
    private void gtnr$startList(CallbackInfo ci) {
        gtnr$cycles().startList();
    }

    @Inject(
        method = "drawListFG",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/client/gui/implementations/GuiCraftConfirm;drawTooltip(IILjava/lang/String;)V"))
    private void gtnr$markListUnderTooltip(CallbackInfo ci) {
        gtnr$cycles().markListUnderTooltip();
    }

    @Inject(method = "drawListFG", at = @At("TAIL"))
    private void gtnr$markListWithoutTooltip(CallbackInfo ci) {
        gtnr$cycles().markListWithoutTooltip();
    }

    @ModifyArg(
        method = "drawListFG",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/client/gui/implementations/GuiCraftConfirm;drawTooltip(IILjava/lang/String;)V"),
        index = 2)
    private String gtnr$listTooltip(String text) {
        return gtnr$cycles().listTooltip(text);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = true)
    private void gtnr$openRingFromList(int mouseX, int mouseY, int button, CallbackInfo ci) {
        if (gtnr$cycles().openRingFromList(mouseX, mouseY, button)) ci.cancel();
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true, remap = true)
    private void gtnr$keys(char character, int key, CallbackInfo ci) {
        if (gtnr$cycles().keys(character, key)) ci.cancel();
    }

    @Override
    public ContainerCraftConfirm container() {
        return this.ccc;
    }

    @Override
    public GuiCraftingTree tree() {
        return this.craftingTree;
    }

    @Override
    public IItemList<IAEStack<?>> missing() {
        return this.missing;
    }

    @Override
    public IItemList<IAEStack<?>> storage() {
        return this.storage;
    }

    @Override
    public IItemList<IAEStack<?>> pending() {
        return this.pending;
    }

    @Override
    public List<IAEStack<?>> visual() {
        return this.filteredVisual;
    }

    @Override
    public int tooltipIndex() {
        return this.tooltip;
    }

    @Override
    public int rows() {
        return this.rows;
    }

    @Override
    public GuiTabButton displaySwitch() {
        return this.switchDisplayMode;
    }

    @Override
    public GuiCraftConfirm.DisplayMode displayMode() {
        return this.displayMode;
    }

    @Override
    public GuiAeButton hideAvailable() {
        return this.hideAvailable;
    }

    @Override
    public MEGuiTextField searchField() {
        return this.searchField;
    }

    @Override
    public int left() {
        return this.guiLeft;
    }

    @Override
    public int top() {
        return this.guiTop;
    }

    @Override
    public int width() {
        return this.xSize;
    }

    @Override
    public List<GuiButton> buttons() {
        return this.buttonList;
    }

    @Override
    public FontRenderer font() {
        return this.fontRendererObj;
    }

    @Override
    public Minecraft minecraft() {
        return this.mc;
    }

    @Override
    public GuiScrollbar scrollbar() {
        return getScrollBar();
    }

    @Override
    public void click(GuiButton button) {
        actionPerformed(button);
    }

}
