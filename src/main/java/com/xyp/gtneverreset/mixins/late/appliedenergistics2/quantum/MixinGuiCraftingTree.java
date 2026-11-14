package com.xyp.gtneverreset.mixins.late.appliedenergistics2.quantum;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.client.quantum.CraftingCycleIndex;
import com.xyp.gtneverreset.client.quantum.CraftingRingView;
import com.xyp.gtneverreset.client.quantum.CraftingTreeCycleController;
import com.xyp.gtneverreset.client.quantum.ICraftingTreeCycles;
import com.xyp.gtneverreset.common.crafting.cycle.RecursivePatternFinder.Cycle;

import appeng.api.storage.data.IAEStack;
import appeng.client.gui.widgets.GuiCraftingTree;
import appeng.crafting.v2.CraftingRequest;

/**
 * Contracts each crafting cycle of the tree into the request it is entered by, marked with a ring, with what the cycle
 * takes from outside under it, and marks the crafts using the patterns of the cycles found for the request. Clicking a
 * cycle, or a craft of a cycle found, shows the cycle as a ring in place of the tree. In a ring a click folds as in the
 * tree, or opens the ring of what is in a cycle of its own, and a shift-click goes back to the node in the tree.
 */
@Mixin(value = GuiCraftingTree.class, remap = false)
public abstract class MixinGuiCraftingTree implements ICraftingTreeCycles {

    @Shadow
    private CraftingRequest request;

    @Shadow
    private String search;
    @Unique
    private CraftingTreeCycleController gtnr$cycleController;

    @Unique
    private CraftingTreeCycleController gtnr$cycles() {
        if (gtnr$cycleController == null) {
            gtnr$cycleController = new CraftingTreeCycleController(
                (GuiCraftingTree) (Object) this,
                () -> request,
                () -> search);
        }
        return gtnr$cycleController;
    }

    @Inject(method = "setRequest", at = @At("HEAD"))
    private void gtnr$leaveTree(CraftingRequest request, CallbackInfo ci) {
        gtnr$cycles().leaveTree(request);
    }

    @Inject(method = "setRequest", at = @At("TAIL"))
    private void gtnr$contractRings(CraftingRequest request, CallbackInfo ci) {
        gtnr$cycles().contractRings(request);
    }

    @Inject(method = "rebuildLayout", at = @At("HEAD"), cancellable = true)
    private void gtnr$layoutContracted(CallbackInfo ci) {
        if (gtnr$cycles().layoutContracted()) ci.cancel();
    }

    @Override
    public void gtnr$setCycles(CraftingCycleIndex index) {
        gtnr$cycles().gtnr$setCycles(index);
    }

    @Override
    public @Nullable CraftingRingView gtnr$ringView() {
        return gtnr$cycles().gtnr$ringView();
    }

    @Override
    public void gtnr$closeRingView() {
        gtnr$cycles().gtnr$closeRingView();
    }

    @Override
    public boolean gtnr$openCycle(Cycle cycle, @Nullable IAEStack<?> start) {
        return gtnr$cycles().gtnr$openCycle(cycle, start);
    }

    @Override
    public List<CraftingCycleIndex.Match> gtnr$hoveredPatterns() {
        return gtnr$cycles().gtnr$hoveredPatterns();
    }

    @Inject(method = "draw(IIZ)V", at = @At("HEAD"), cancellable = true)
    private void gtnr$drawRing(int guiMouseX, int guiMouseY, boolean inScreenshotMode, CallbackInfo ci) {
        if (gtnr$cycles().drawRing(guiMouseX, guiMouseY, inScreenshotMode)) ci.cancel();
    }

    @Inject(
        method = "draw(IIZ)V",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPopMatrix()V", ordinal = 0))
    private void gtnr$markTree(int guiMouseX, int guiMouseY, boolean inScreenshotMode, CallbackInfo ci) {
        gtnr$cycles().drawMarks(null);
    }

    @Inject(method = "getNodeAt", at = @At("HEAD"), cancellable = true)
    private void gtnr$nodeInRing(int guiMouseX, int guiMouseY, CallbackInfoReturnable<Object> cir) {
        var view = gtnr$cycles().gtnr$ringView();
        if (view != null) cir.setReturnValue(view.nodeAt(guiMouseX, guiMouseY));
    }

    @Inject(method = "drawTooltip", at = @At("HEAD"), cancellable = true)
    private void gtnr$cycleTooltip(int guiMouseX, int guiMouseY, CallbackInfo ci) {
        if (gtnr$cycles().cycleTooltip(guiMouseX, guiMouseY)) ci.cancel();
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void gtnr$clickRing(int guiMouseX, int guiMouseY, int button, CallbackInfo ci) {
        if (gtnr$cycles().clickRing(guiMouseX, guiMouseY, button)) ci.cancel();
    }

    @Inject(method = { "hideAvailable", "updateSearchGoToList", "searchGoTo", "saveScreenshot" }, at = @At("HEAD"))
    private void gtnr$backToTree(CallbackInfo ci) {
        gtnr$cycles().backToTree();
    }
}
