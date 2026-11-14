package com.xyp.gtneverreset.client.quantum;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.xyp.gtneverreset.common.crafting.cycle.RecursivePatternFinder.Cycle;

import appeng.api.storage.data.IAEStack;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiCraftingTree;
import appeng.core.localization.ColorUtils;
import appeng.crafting.v2.CraftingRequest;
import appeng.crafting.v2.resolvers.CraftableItemResolver.CraftFromPatternTask;
import appeng.crafting.v2.resolvers.CraftingTask;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class CraftingTreeCycleController implements ICraftingTreeCycles {

    private static final long FOCUS_MILLIS = 2000;
    private Map<Object, CraftingTreeRings.Ring> rings = ImmutableMap.of();
    private CraftingRequest foldedRequest;
    private CraftingTreeContraction contraction;
    private CraftingCycleIndex index;

    // Per craft of the tree, those of its cycles too, the cycle pattern it uses
    private Map<Object, CraftingCycleIndex.Match> matches;

    // The rings open, each opened from the one before
    private final List<CraftingRingView> views = new ArrayList<>();
    private boolean settingRequest;
    private Object focus;
    private long focusUntil;
    private final GuiCraftingTree tree;
    private final Supplier<CraftingRequest> request;
    private final Supplier<String> search;

    public CraftingTreeCycleController(GuiCraftingTree tree, Supplier<CraftingRequest> request,
        Supplier<String> search) {
        this.tree = tree;
        this.request = request;
        this.search = search;
    }

    private @Nullable CraftingRingView view() {
        return this.views.isEmpty() ? null : this.views.get(this.views.size() - 1);
    }

    public void leaveTree(CraftingRequest request) {
        this.settingRequest = true;
        // A new tree has none of the nodes a ring of the tree moved
        if (request != this.request.get()) {
            this.views.removeIf(CraftingRingView::ofTree);
            this.contraction = null;
            this.focus = null;
        }
    }

    public void contractRings(CraftingRequest request) {
        try {
            if (request == this.foldedRequest) return;
            this.foldedRequest = request;
            var tree = this.tree;
            this.rings = CraftingTreeRings.fold(tree);
            this.matches = null;
            if (this.rings.isEmpty()) return;
            this.contraction = CraftingTreeContraction.of(tree, this.rings);
            CraftingTreeRings.rebuildLayout(tree);
            // What was found was found among the nodes now taken out
            tree.updateSearchGoToList(this.search.get());
        } finally {
            this.settingRequest = false;
        }
    }

    public boolean layoutContracted() {
        if (this.contraction == null) return false;
        this.contraction.layout(this.tree);
        return true;
    }

    @Override
    public void gtnr$setCycles(CraftingCycleIndex index) {
        this.index = index;
        this.matches = null;
    }

    private Map<Object, CraftingCycleIndex.Match> matches() {
        if (this.matches == null) {
            this.matches = new IdentityHashMap<>();
            if (this.index != null && this.request.get() != null) {
                var nodes = this.contraction != null ? this.contraction.allNodes()
                    : CraftingTreeRings.allNodes(this.tree);
                for (var node : nodes) {
                    if (!CraftingTreeRings.isTask(node)
                        || !(CraftingTreeRings.task(node) instanceof CraftFromPatternTask craft)) continue;
                    var match = this.index.match(craft.pattern);
                    if (match != null) this.matches.put(node, match);
                }
            }
        }
        return this.matches;
    }

    @Override
    public @Nullable CraftingRingView gtnr$ringView() {
        return view();
    }

    @Override
    public void gtnr$closeRingView() {
        if (this.views.isEmpty()) return;
        this.views.remove(this.views.size() - 1)
            .close();
        var outer = view();
        if (outer != null) outer.refresh();
    }

    private void closeAllRings() {
        while (!this.views.isEmpty()) {
            this.views.remove(this.views.size() - 1)
                .close();
        }
    }

    @Override
    public boolean gtnr$openCycle(Cycle cycle, @Nullable IAEStack<?> start) {
        if (this.index == null) return false;
        var world = Minecraft.getMinecraft().theWorld;
        var built = world == null ? null : CraftingCycleRings.build(this.tree, this.index, cycle, start, world);
        if (built == null) return false;
        this.views.add(new CraftingRingView(this.tree, built));
        return true;
    }

    /**
     * What opens from a node of the ring shown, as a cycle of its own: a cycle of the tree it enters, or a cycle found
     * that what it asks for is in, other than the one shown.
     */
    private @Nullable Runnable nested(Object node) {
        var view = view();
        if (view == null || !view.contains(node)
            || node == view.ring()
                .entry()
            || !CraftingTreeRings.isRequest(node)) return null;
        var ring = this.rings.get(node);
        if (ring != null) return () -> this.views.add(new CraftingRingView(this.tree, ring));
        if (this.index == null || view.ring()
            .members()
            .contains(node)) return null;
        var stack = CraftingTreeRings.request(node).stack;
        var shown = view.cycle();
        var entryStack = CraftingTreeRings.request(
            view.ring()
                .entry()).stack;
        for (var cycle : this.index.cyclesWith(stack)) {
            if (cycle == shown || shown == null && cycle.items()
                .stream()
                .anyMatch(entryStack::isSameType)) continue;
            return () -> gtnr$openCycle(cycle, stack);
        }
        return null;
    }

    private CraftingCycleIndex.Match matchOf(Object node) {
        var match = matches().get(node);
        var view = view();
        if (match == null && view != null) match = view.matches()
            .get(node);
        return match;
    }

    @Override
    public List<CraftingCycleIndex.Match> gtnr$hoveredPatterns() {
        var node = CraftingTreeRings.tooltipNode(this.tree);
        var view = view();
        if (node != null) {
            var match = matchOf(node);
            if (match != null) return ImmutableList.of(match);
            var ring = this.rings.get(node);
            if (ring != null) return matchesOf(ring.members());
        }
        if (view != null) {
            return matchesOf(
                view.ring()
                    .members());
        }
        return ImmutableList.of();
    }

    private List<CraftingCycleIndex.Match> matchesOf(List<Object> nodes) {
        var result = new ArrayList<CraftingCycleIndex.Match>();
        for (var node : nodes) {
            var match = matchOf(node);
            if (match != null && result.stream()
                .noneMatch(m -> m.location() == match.location())) {
                result.add(match);
            }
        }
        return result;
    }

    public boolean drawRing(int guiMouseX, int guiMouseY, boolean inScreenshotMode) {
        var view = view();
        if (view == null || inScreenshotMode) return false;
        view.draw(guiMouseX, guiMouseY, () -> drawMarks(view.nodes()));
        return true;
    }

    // The cycles, the crafts of cycles found, and the node gone back to from a ring
    public void drawMarks(@Nullable List<Object> shown) {
        var minecraft = Minecraft.getMinecraft();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1, 1, 1, 1);
        if (shown == null) {
            for (var entry : this.rings.keySet()) {
                if (!CraftingTreeRings.visible(entry)) continue;
                RecursivePatternButton
                    .drawIcon(minecraft, CraftingTreeRings.x(entry) + 11, CraftingTreeRings.y(entry) - 5, 9, 300);
            }
        } else {
            for (var node : shown) {
                if (nested(node) == null) continue;
                RecursivePatternButton
                    .drawIcon(minecraft, CraftingTreeRings.x(node) + 11, CraftingTreeRings.y(node) - 5, 9, 300);
            }
        }
        var view = view();
        var matches = view != null && !view.ofTree() ? view.matches() : matches();
        for (var entry : matches.entrySet()) {
            var node = entry.getKey();
            if (shown == null ? !CraftingTreeRings.visible(node) : !shown.contains(node)) continue;
            if (entry.getValue()
                .blocking()) GL11.glColor4f(1, 0.4f, 0.4f, 1);
            RecursivePatternButton
                .drawIcon(minecraft, CraftingTreeRings.x(node) - 5, CraftingTreeRings.y(node) - 5, 8, 300);
            GL11.glColor4f(1, 1, 1, 1);
        }
        var focus = this.focus;
        if (focus != null && shown == null) {
            if (System.currentTimeMillis() > this.focusUntil) {
                this.focus = null;
            } else if (CraftingTreeRings.visible(focus)) {
                GL11.glPushMatrix();
                GL11.glTranslatef(0, 0, 250);
                CraftingTreeRings.drawSlotOutline(
                    this.tree,
                    CraftingTreeRings.x(focus),
                    CraftingTreeRings.y(focus),
                    ColorUtils.searchGoToHighlight.getColor(),
                    CraftingTreeRings.isTask(focus));
                GL11.glPopMatrix();
            }
        }
        GL11.glColor4f(1, 1, 1, 1);
    }

    public boolean cycleTooltip(int guiMouseX, int guiMouseY) {
        var tree = this.tree;
        var node = CraftingTreeRings.tooltipNode(tree);
        if (node == null) return false;
        var view = view();
        var extra = new StringBuilder();
        var ring = view == null || view.ring()
            .entry() != node ? this.rings.get(node) : null;
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
        } else if (nested(node) != null) {
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
        var match = matchOf(node);
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
        if (extra.length() == 0) return false;
        var text = CraftingTreeRings.isRequest(node) ? CraftingTreeRings.request(node)
            .getTooltipText() : ((CraftingTask) CraftingTreeRings.task(node)).getTooltipText();
        ((AEBaseGui) CraftingTreeRings.parentGui(tree)).drawTooltip(guiMouseX, guiMouseY, text + extra);
        return true;
    }

    public boolean clickRing(int guiMouseX, int guiMouseY, int button) {
        if (button != 0) return false;
        var tree = this.tree;
        var view = view();
        if (view != null) {
            tree.mouseDragged(guiMouseX, guiMouseY);
            var pressed = CraftingTreeRings.pressedNode(tree);
            CraftingTreeRings.releasePress(tree);
            if (pressed != null && !CraftingTreeRings.dragging(tree) && pressed == view.nodeAt(guiMouseX, guiMouseY)) {
                var nested = nested(pressed);
                if (GuiScreen.isShiftKeyDown()) locate(pressed);
                else if (nested != null) nested.run();
                else view.toggle(pressed);
            }
            return true;
        }
        var pressed = CraftingTreeRings.pressedNode(tree);
        if (pressed == null || CraftingTreeRings.dragging(tree)
            || pressed != CraftingTreeRings.nodeAt(tree, guiMouseX, guiMouseY)) {
            return false;
        }
        var ring = this.rings.get(pressed);
        var match = matches().get(pressed);
        if (ring != null) {
            // A shift-click folds what the cycle takes, as a click does in the tree
            if (GuiScreen.isShiftKeyDown()) return false;
            CraftingTreeRings.releasePress(tree);
            this.views.add(new CraftingRingView(tree, ring));
            return true;
        } else if (match != null) {
            // The tree has no way back for it to show, so the ring is made of the cycle found
            var parent = CraftingTreeRings.parent(pressed);
            var start = parent != null && CraftingTreeRings.isRequest(parent) ? CraftingTreeRings.request(parent).stack
                : null;
            CraftingTreeRings.releasePress(tree);
            gtnr$openCycle(match.cycle(), start);
            return true;
        }

        return false;
    }

    // Back to the tree, opened down to the node, or for a craft not in the tree, its machines
    private void locate(Object node) {
        var view = view();
        if (view != null && !view.ofTree()) {
            var match = view.matches()
                .get(node);
            if (match == null) return;
            Object inTree = null;
            for (var entry : matches().entrySet()) {
                if (entry.getValue()
                    .location() == match.location()) {
                    inTree = entry.getKey();
                    break;
                }
            }
            if (inTree == null) {
                RecursivePatternReport
                    .highlight(Minecraft.getMinecraft(), this.index.request(), ImmutableList.of(match));
                return;
            }
            node = inTree;
        }
        closeAllRings();
        var tree = this.tree;
        // A node of a cycle is in the tree as the cycle
        var cycleNode = this.contraction == null ? null : this.contraction.cycleNodeOf(node);
        if (cycleNode != null) node = cycleNode;
        for (Object ancestor = parentOf(node); ancestor != null; ancestor = parentOf(ancestor)) {
            CraftingTreeRings.setCollapsed(ancestor, false);
        }
        CraftingTreeRings.rebuildLayout(tree);
        float zoom = CraftingTreeRings.zoom(tree);
        tree.scrollX = CraftingTreeRings.x(node) + 8 - tree.widgetW / zoom / 2;
        tree.scrollY = CraftingTreeRings.y(node) + 8 - tree.widgetH / zoom / 2;
        this.focus = node;
        this.focusUntil = System.currentTimeMillis() + FOCUS_MILLIS;
    }

    private @Nullable Object parentOf(Object node) {
        return this.contraction != null ? this.contraction.parentOf(node) : CraftingTreeRings.parent(node);
    }

    public void backToTree() {
        if (!this.settingRequest) closeAllRings();
    }
}
