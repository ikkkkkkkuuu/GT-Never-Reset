package com.xyp.gtneverreset.mixins.late.appliedenergistics2.quantum;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.xyp.gtneverreset.client.quantum.CraftingCycleIndex;
import com.xyp.gtneverreset.client.quantum.CraftingCycleRings;
import com.xyp.gtneverreset.client.quantum.CraftingRingView;
import com.xyp.gtneverreset.client.quantum.CraftingTreeContraction;
import com.xyp.gtneverreset.client.quantum.CraftingTreeRings;
import com.xyp.gtneverreset.client.quantum.ICraftingTreeCycles;
import com.xyp.gtneverreset.client.quantum.RecursivePatternButton;
import com.xyp.gtneverreset.client.quantum.RecursivePatternReport;
import com.xyp.gtneverreset.utils.crafting.cycle.RecursivePatternFinder.Cycle;

import appeng.api.storage.data.IAEStack;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiCraftingTree;
import appeng.core.localization.ColorUtils;
import appeng.crafting.v2.CraftingRequest;
import appeng.crafting.v2.resolvers.CraftableItemResolver.CraftFromPatternTask;
import appeng.crafting.v2.resolvers.CraftingTask;

/**
 * Contracts each crafting cycle of the tree into the request it is entered by, marked with a ring, with what the cycle
 * takes from outside under it, and marks the crafts using the patterns of the cycles found for the request. Clicking a
 * cycle, or a craft of a cycle found, shows the cycle as a ring in place of the tree. In a ring a click folds as in the
 * tree, or opens the ring of what is in a cycle of its own, and a shift-click goes back to the node in the tree.
 */
@Mixin(value = GuiCraftingTree.class, remap = false)
public abstract class MixinGuiCraftingTree implements ICraftingTreeCycles {

    @Unique
    private static final long GTNR$FOCUS_MILLIS = 2000;

    @Shadow
    private CraftingRequest request;

    @Shadow
    private String search;

    @Unique
    private Map<Object, CraftingTreeRings.Ring> gtnr$rings = ImmutableMap.of();

    @Unique
    private CraftingRequest gtnr$foldedRequest;

    @Unique
    private CraftingTreeContraction gtnr$contraction;

    @Unique
    private CraftingCycleIndex gtnr$index;

    // Per craft of the tree, those of its cycles too, the cycle pattern it uses
    @Unique
    private Map<Object, CraftingCycleIndex.Match> gtnr$matches;

    // The rings open, each opened from the one before
    @Unique
    private final List<CraftingRingView> gtnr$views = new ArrayList<>();

    @Unique
    private boolean gtnr$settingRequest;

    @Unique
    private Object gtnr$focus;

    @Unique
    private long gtnr$focusUntil;

    @Unique
    private GuiCraftingTree gtnr$tree() {
        return (GuiCraftingTree) (Object) this;
    }

    @Unique
    private @Nullable CraftingRingView gtnr$view() {
        return this.gtnr$views.isEmpty() ? null : this.gtnr$views.get(this.gtnr$views.size() - 1);
    }

    @Inject(method = "setRequest", at = @At("HEAD"))
    private void gtnr$leaveTree(CraftingRequest request, CallbackInfo ci) {
        this.gtnr$settingRequest = true;
        // A new tree has none of the nodes a ring of the tree moved
        if (request != this.request) {
            this.gtnr$views.removeIf(CraftingRingView::ofTree);
            this.gtnr$contraction = null;
            this.gtnr$focus = null;
        }
    }

    @Inject(method = "setRequest", at = @At("TAIL"))
    private void gtnr$contractRings(CraftingRequest request, CallbackInfo ci) {
        try {
            if (request == this.gtnr$foldedRequest) return;
            this.gtnr$foldedRequest = request;
            var tree = gtnr$tree();
            this.gtnr$rings = CraftingTreeRings.fold(tree);
            this.gtnr$matches = null;
            if (this.gtnr$rings.isEmpty()) return;
            this.gtnr$contraction = CraftingTreeContraction.of(tree, this.gtnr$rings);
            CraftingTreeRings.rebuildLayout(tree);
            // What was found was found among the nodes now taken out
            tree.updateSearchGoToList(this.search);
        } finally {
            this.gtnr$settingRequest = false;
        }
    }

    @Inject(method = "rebuildLayout", at = @At("HEAD"), cancellable = true)
    private void gtnr$layoutContracted(CallbackInfo ci) {
        if (this.gtnr$contraction == null) return;
        this.gtnr$contraction.layout(gtnr$tree());
        ci.cancel();
    }

    @Override
    public void gtnr$setCycles(CraftingCycleIndex index) {
        this.gtnr$index = index;
        this.gtnr$matches = null;
    }

    @Unique
    private Map<Object, CraftingCycleIndex.Match> gtnr$matches() {
        if (this.gtnr$matches == null) {
            this.gtnr$matches = new IdentityHashMap<>();
            if (this.gtnr$index != null && this.request != null) {
                var nodes = this.gtnr$contraction != null ? this.gtnr$contraction.allNodes()
                    : CraftingTreeRings.allNodes(gtnr$tree());
                for (var node : nodes) {
                    if (!CraftingTreeRings.isTask(node)
                        || !(CraftingTreeRings.task(node) instanceof CraftFromPatternTask craft)) continue;
                    var match = this.gtnr$index.match(craft.pattern);
                    if (match != null) this.gtnr$matches.put(node, match);
                }
            }
        }
        return this.gtnr$matches;
    }

    @Override
    public @Nullable CraftingRingView gtnr$ringView() {
        return gtnr$view();
    }

    @Override
    public void gtnr$closeRingView() {
        if (this.gtnr$views.isEmpty()) return;
        this.gtnr$views.remove(this.gtnr$views.size() - 1)
            .close();
        var outer = gtnr$view();
        if (outer != null) outer.refresh();
    }

    @Unique
    private void gtnr$closeAllRings() {
        while (!this.gtnr$views.isEmpty()) {
            this.gtnr$views.remove(this.gtnr$views.size() - 1)
                .close();
        }
    }

    @Override
    public boolean gtnr$openCycle(Cycle cycle, @Nullable IAEStack<?> start) {
        if (this.gtnr$index == null) return false;
        var world = Minecraft.getMinecraft().theWorld;
        var built = world == null ? null : CraftingCycleRings.build(gtnr$tree(), this.gtnr$index, cycle, start, world);
        if (built == null) return false;
        this.gtnr$views.add(new CraftingRingView(gtnr$tree(), built));
        return true;
    }

    /**
     * What opens from a node of the ring shown, as a cycle of its own: a cycle of the tree it enters, or a cycle found
     * that what it asks for is in, other than the one shown.
     */
    @Unique
    private @Nullable Runnable gtnr$nested(Object node) {
        var view = gtnr$view();
        if (view == null || !view.contains(node)
            || node == view.ring()
                .entry()
            || !CraftingTreeRings.isRequest(node)) return null;
        var ring = this.gtnr$rings.get(node);
        if (ring != null) return () -> this.gtnr$views.add(new CraftingRingView(gtnr$tree(), ring));
        if (this.gtnr$index == null || view.ring()
            .members()
            .contains(node)) return null;
        var stack = CraftingTreeRings.request(node).stack;
        var shown = view.cycle();
        var entryStack = CraftingTreeRings.request(
            view.ring()
                .entry()).stack;
        for (var cycle : this.gtnr$index.cyclesWith(stack)) {
            if (cycle == shown || shown == null && cycle.items()
                .stream()
                .anyMatch(entryStack::isSameType)) continue;
            return () -> gtnr$openCycle(cycle, stack);
        }
        return null;
    }

    @Unique
    private CraftingCycleIndex.Match gtnr$matchOf(Object node) {
        var match = gtnr$matches().get(node);
        var view = gtnr$view();
        if (match == null && view != null) match = view.matches()
            .get(node);
        return match;
    }

    @Override
    public List<CraftingCycleIndex.Match> gtnr$hoveredPatterns() {
        var node = CraftingTreeRings.tooltipNode(gtnr$tree());
        var view = gtnr$view();
        if (node != null) {
            var match = gtnr$matchOf(node);
            if (match != null) return ImmutableList.of(match);
            var ring = this.gtnr$rings.get(node);
            if (ring != null) return gtnr$matchesOf(ring.members());
        }
        if (view != null) {
            return gtnr$matchesOf(
                view.ring()
                    .members());
        }
        return ImmutableList.of();
    }

    @Unique
    private List<CraftingCycleIndex.Match> gtnr$matchesOf(List<Object> nodes) {
        var result = new ArrayList<CraftingCycleIndex.Match>();
        for (var node : nodes) {
            var match = gtnr$matchOf(node);
            if (match != null && result.stream()
                .noneMatch(m -> m.location() == match.location())) {
                result.add(match);
            }
        }
        return result;
    }

    @Inject(method = "draw(IIZ)V", at = @At("HEAD"), cancellable = true)
    private void gtnr$drawRing(int guiMouseX, int guiMouseY, boolean inScreenshotMode, CallbackInfo ci) {
        var view = gtnr$view();
        if (view == null || inScreenshotMode) return;
        view.draw(guiMouseX, guiMouseY, () -> gtnr$drawMarks(view.nodes()));
        ci.cancel();
    }

    @Inject(
        method = "draw(IIZ)V",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPopMatrix()V", ordinal = 0))
    private void gtnr$markTree(int guiMouseX, int guiMouseY, boolean inScreenshotMode, CallbackInfo ci) {
        gtnr$drawMarks(null);
    }

    // The cycles, the crafts of cycles found, and the node gone back to from a ring
    @Unique
    private void gtnr$drawMarks(@Nullable List<Object> shown) {
        var minecraft = Minecraft.getMinecraft();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1, 1, 1, 1);
        if (shown == null) {
            for (var entry : this.gtnr$rings.keySet()) {
                if (!CraftingTreeRings.visible(entry)) continue;
                RecursivePatternButton
                    .drawIcon(minecraft, CraftingTreeRings.x(entry) + 11, CraftingTreeRings.y(entry) - 5, 9, 300);
            }
        } else {
            for (var node : shown) {
                if (gtnr$nested(node) == null) continue;
                RecursivePatternButton
                    .drawIcon(minecraft, CraftingTreeRings.x(node) + 11, CraftingTreeRings.y(node) - 5, 9, 300);
            }
        }
        var view = gtnr$view();
        var matches = view != null && !view.ofTree() ? view.matches() : gtnr$matches();
        for (var entry : matches.entrySet()) {
            var node = entry.getKey();
            if (shown == null ? !CraftingTreeRings.visible(node) : !shown.contains(node)) continue;
            if (entry.getValue()
                .blocking()) GL11.glColor4f(1, 0.4f, 0.4f, 1);
            RecursivePatternButton
                .drawIcon(minecraft, CraftingTreeRings.x(node) - 5, CraftingTreeRings.y(node) - 5, 8, 300);
            GL11.glColor4f(1, 1, 1, 1);
        }
        var focus = this.gtnr$focus;
        if (focus != null && shown == null) {
            if (System.currentTimeMillis() > this.gtnr$focusUntil) {
                this.gtnr$focus = null;
            } else if (CraftingTreeRings.visible(focus)) {
                GL11.glPushMatrix();
                GL11.glTranslatef(0, 0, 250);
                CraftingTreeRings.drawSlotOutline(
                    gtnr$tree(),
                    CraftingTreeRings.x(focus),
                    CraftingTreeRings.y(focus),
                    ColorUtils.searchGoToHighlight.getColor(),
                    CraftingTreeRings.isTask(focus));
                GL11.glPopMatrix();
            }
        }
        GL11.glColor4f(1, 1, 1, 1);
    }

    @Inject(method = "getNodeAt", at = @At("HEAD"), cancellable = true)
    private void gtnr$nodeInRing(int guiMouseX, int guiMouseY, CallbackInfoReturnable<Object> cir) {
        var view = gtnr$view();
        if (view != null) cir.setReturnValue(view.nodeAt(guiMouseX, guiMouseY));
    }

    @Inject(method = "drawTooltip", at = @At("HEAD"), cancellable = true)
    private void gtnr$cycleTooltip(int guiMouseX, int guiMouseY, CallbackInfo ci) {
        var tree = gtnr$tree();
        var node = CraftingTreeRings.tooltipNode(tree);
        if (node == null) return;
        var view = gtnr$view();
        var extra = new StringBuilder();
        var ring = view == null || view.ring()
            .entry() != node ? this.gtnr$rings.get(node) : null;
        if (ring != null) {
            extra.append('\n')
                .append(
                    StatCollector.translateToLocalFormatted(
                        // #tr gtnr.gui.crafting_ring.tooltip
                        // # §bCycle: %s crafts, %s loops back
                        // # zh_CN §b合成循环：%s 次合成，%s 处绕回
                        "gtnr.gui.crafting_ring.tooltip",
                        ring.crafts(),
                        ring.backEdges()
                            .size()))
                .append('\n')
                // #tr gtnr.gui.crafting_ring.open
                // # §7Click: view as ring
                // # zh_CN §7点击：以环形查看这个循环
                .append(StatCollector.translateToLocal("gtnr.gui.crafting_ring.open"));
            if (view == null) {
                extra.append('\n')
                    // #tr gtnr.gui.crafting_ring.fold_inputs
                    // # §7Shift-click: fold inputs
                    // # zh_CN §7Shift+左键：收起/展开这个循环从外部需要的原料
                    .append(StatCollector.translateToLocal("gtnr.gui.crafting_ring.fold_inputs"));
            }
        } else if (gtnr$nested(node) != null) {
            extra.append('\n')
                // #tr gtnr.gui.crafting_ring.nested
                // # §bAlso made in a cycle
                // # zh_CN §b这个原料也来自一个合成循环
                .append(StatCollector.translateToLocal("gtnr.gui.crafting_ring.nested"))
                .append('\n')
                // #tr gtnr.gui.crafting_ring.open
                // # §7Click: view as ring
                // # zh_CN §7点击：以环形查看这个循环
                .append(StatCollector.translateToLocal("gtnr.gui.crafting_ring.open"));
        }
        var match = gtnr$matchOf(node);
        if (match != null) {
            extra.append('\n')
                .append(
                    StatCollector.translateToLocalFormatted(
                        match.cycle()
                            // #tr gtnr.gui.recursive_patterns.tooltip.net
                            // # §aCycle %s (net output)
                            // # zh_CN §a递归样板：循环 %s（有净产出）
                            .netOutput() ? "gtnr.gui.recursive_patterns.tooltip.net"
                                // #tr gtnr.gui.recursive_patterns.tooltip.no_net
                                // # §cCycle %s (no net output)
                                // # zh_CN §c递归样板：循环 %s（无净产出）
                                : "gtnr.gui.recursive_patterns.tooltip.no_net",
                        match.number()))
                .append('\n')
                .append(
                    StatCollector.translateToLocalFormatted(
                        // #tr gtnr.gui.recursive_patterns.tooltip.machines
                        // # §7%s machines, press %s to highlight
                        // # zh_CN §7存放在 %s 台机器中，按 %s 高亮
                        "gtnr.gui.recursive_patterns.tooltip.machines",
                        match.location()
                            .machines()
                            .size(),
                        RecursivePatternReport.highlightKeyName()));
            if (view == null) {
                extra.append('\n')
                    // #tr gtnr.gui.crafting_ring.open
                    // # §7Click: view as ring
                    // # zh_CN §7点击：以环形查看这个循环
                    .append(StatCollector.translateToLocal("gtnr.gui.crafting_ring.open"));
            }
        }
        if (view != null && view.contains(node)) {
            int folded = view.foldedCrafts(node);
            if (folded > 0) {
                extra.append('\n')
                    // #tr gtnr.gui.crafting_ring.folded
                    // # §7%s more crafts folded, click to unfold
                    // # zh_CN §7这里收起了循环后面的 %s 次合成，点击展开
                    .append(StatCollector.translateToLocalFormatted("gtnr.gui.crafting_ring.folded", folded));
            }
            extra.append('\n')
                // #tr gtnr.gui.crafting_ring.locate
                // # §7Shift-click: find in tree
                // # zh_CN §7Shift+左键：回到合成树中的这个节点（不在树中时高亮它的机器）
                .append(StatCollector.translateToLocal("gtnr.gui.crafting_ring.locate"));
        }
        if (extra.length() == 0) return;
        var text = CraftingTreeRings.isRequest(node) ? CraftingTreeRings.request(node)
            .getTooltipText() : ((CraftingTask) CraftingTreeRings.task(node)).getTooltipText();
        ((AEBaseGui) CraftingTreeRings.parentGui(tree)).drawTooltip(guiMouseX, guiMouseY, text + extra);
        ci.cancel();
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void gtnr$clickRing(int guiMouseX, int guiMouseY, int button, CallbackInfo ci) {
        if (button != 0) return;
        var tree = gtnr$tree();
        var view = gtnr$view();
        if (view != null) {
            tree.mouseDragged(guiMouseX, guiMouseY);
            var pressed = CraftingTreeRings.pressedNode(tree);
            CraftingTreeRings.releasePress(tree);
            if (pressed != null && !CraftingTreeRings.dragging(tree) && pressed == view.nodeAt(guiMouseX, guiMouseY)) {
                var nested = gtnr$nested(pressed);
                if (GuiScreen.isShiftKeyDown()) gtnr$locate(pressed);
                else if (nested != null) nested.run();
                else view.toggle(pressed);
            }
            ci.cancel();
            return;
        }
        var pressed = CraftingTreeRings.pressedNode(tree);
        if (pressed == null || CraftingTreeRings.dragging(tree)
            || pressed != CraftingTreeRings.nodeAt(tree, guiMouseX, guiMouseY)) {
            return;
        }
        var ring = this.gtnr$rings.get(pressed);
        var match = gtnr$matches().get(pressed);
        if (ring != null) {
            // A shift-click folds what the cycle takes, as a click does in the tree
            if (GuiScreen.isShiftKeyDown()) return;
            CraftingTreeRings.releasePress(tree);
            this.gtnr$views.add(new CraftingRingView(tree, ring));
            ci.cancel();
        } else if (match != null) {
            // The tree has no way back for it to show, so the ring is made of the cycle found
            var parent = CraftingTreeRings.parent(pressed);
            var start = parent != null && CraftingTreeRings.isRequest(parent) ? CraftingTreeRings.request(parent).stack
                : null;
            CraftingTreeRings.releasePress(tree);
            gtnr$openCycle(match.cycle(), start);
            ci.cancel();
        }
    }

    // Back to the tree, opened down to the node, or for a craft not in the tree, its machines
    @Unique
    private void gtnr$locate(Object node) {
        var view = gtnr$view();
        if (view != null && !view.ofTree()) {
            var match = view.matches()
                .get(node);
            if (match == null) return;
            Object inTree = null;
            for (var entry : gtnr$matches().entrySet()) {
                if (entry.getValue()
                    .location() == match.location()) {
                    inTree = entry.getKey();
                    break;
                }
            }
            if (inTree == null) {
                RecursivePatternReport
                    .highlight(Minecraft.getMinecraft(), this.gtnr$index.request(), ImmutableList.of(match));
                return;
            }
            node = inTree;
        }
        gtnr$closeAllRings();
        var tree = gtnr$tree();
        // A node of a cycle is in the tree as the cycle
        var cycleNode = this.gtnr$contraction == null ? null : this.gtnr$contraction.cycleNodeOf(node);
        if (cycleNode != null) node = cycleNode;
        for (Object ancestor = gtnr$parentOf(node); ancestor != null; ancestor = gtnr$parentOf(ancestor)) {
            CraftingTreeRings.setCollapsed(ancestor, false);
        }
        CraftingTreeRings.rebuildLayout(tree);
        float zoom = CraftingTreeRings.zoom(tree);
        tree.scrollX = CraftingTreeRings.x(node) + 8 - tree.widgetW / zoom / 2;
        tree.scrollY = CraftingTreeRings.y(node) + 8 - tree.widgetH / zoom / 2;
        this.gtnr$focus = node;
        this.gtnr$focusUntil = System.currentTimeMillis() + GTNR$FOCUS_MILLIS;
    }

    @Unique
    private @Nullable Object gtnr$parentOf(Object node) {
        return this.gtnr$contraction != null ? this.gtnr$contraction.parentOf(node) : CraftingTreeRings.parent(node);
    }

    @Inject(method = { "hideAvailable", "updateSearchGoToList", "searchGoTo", "saveScreenshot" }, at = @At("HEAD"))
    private void gtnr$backToTree(CallbackInfo ci) {
        if (!this.gtnr$settingRequest) gtnr$closeAllRings();
    }
}
