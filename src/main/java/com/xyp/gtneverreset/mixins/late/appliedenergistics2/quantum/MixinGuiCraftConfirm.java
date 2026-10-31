package com.xyp.gtneverreset.mixins.late.appliedenergistics2.quantum;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.ImmutableList;
import com.xyp.gtneverreset.client.quantum.CraftingCycleIndex;
import com.xyp.gtneverreset.client.quantum.ICraftingCycleHost;
import com.xyp.gtneverreset.client.quantum.ICraftingTreeCycles;
import com.xyp.gtneverreset.client.quantum.RecursivePatternButton;
import com.xyp.gtneverreset.client.quantum.RecursivePatternReport;
import com.xyp.gtneverreset.common.crafting.cycle.RecursivePatternFinder.Cycle;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.RecursivePatternQueryPacket;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.GuiSub;
import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.client.gui.widgets.GuiAeButton;
import appeng.client.gui.widgets.GuiCraftingTree;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.core.localization.ColorUtils;

/**
 * Finds the crafting cycles of the request once it is calculated, marks them in the crafting tree and the list, and
 * tells once where the machines of those the plan cannot start because of are. The button lists them all, the highlight
 * key highlights the cycle of what is under the mouse, and shift-clicking an item of a cycle in the list shows the
 * cycle as a ring in the tree view, left by its button or Esc.
 */
@Mixin(value = GuiCraftConfirm.class, remap = false)
public abstract class MixinGuiCraftConfirm extends GuiSub implements ICraftingCycleHost {

    @Unique
    // #tr gtnr.quantum_crafting.message.CraftingNoNetOutput
    // # Patterns cannot yield a net output. Check cycles and missing external inputs.
    // # zh_CN 样板无法净产出请求物品，请检查循环样板及缺少的外部输入。
    private static final String GTNR$NO_NET_OUTPUT = "gtnr.quantum_crafting.message.CraftingNoNetOutput";

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
    private RecursivePatternButton gtnr$recursivePatterns;

    @Unique
    private GuiAeButton gtnr$backToTree;

    @Unique
    private boolean gtnr$queried;

    @Unique
    private CraftingCycleIndex gtnr$index;

    // The ring shown was opened from the list, to which leaving it goes back
    @Unique
    private boolean gtnr$ringFromList;

    @Unique
    private boolean gtnr$listMarked;

    public MixinGuiCraftConfirm() {
        super(null);
    }

    @Unique
    private ICraftingTreeCycles gtnr$tree() {
        return (ICraftingTreeCycles) this.craftingTree;
    }

    @Inject(method = "initGui", at = @At("TAIL"), remap = true)
    private void gtnr$addButtons(CallbackInfo ci) {
        this.gtnr$recursivePatterns = new RecursivePatternButton(
            this.guiLeft + this.xSize + 2,
            this.guiTop + 8 + 20 * 3);
        this.buttonList.add(this.gtnr$recursivePatterns);
        // #tr gtnr.gui.crafting_ring.back
        // # Back
        // # zh_CN 返回
        var back = StatCollector.translateToLocal("gtnr.gui.crafting_ring.back");
        this.gtnr$backToTree = new GuiAeButton(
            0,
            this.guiLeft + 9,
            this.guiTop + 20,
            Math.min(this.craftingTree.widgetW, this.fontRendererObj.getStringWidth(back) + 10),
            18,
            back,
            "");
        this.gtnr$backToTree.visible = false;
        this.buttonList.add(this.gtnr$backToTree);
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true, remap = true)
    private void gtnr$buttons(GuiButton button, CallbackInfo ci) {
        if (button == this.gtnr$recursivePatterns) {
            NetWorkHandler.sendToServer(new RecursivePatternQueryPacket(true));
            ci.cancel();
        } else if (button == this.gtnr$backToTree) {
            gtnr$leaveRing();
            ci.cancel();
        }
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), remap = true)
    private void gtnr$update(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        var error = this.ccc.getErrorMessage();
        if (!this.gtnr$queried && (this.ccc.getUsedBytes() > 0 || !error.isEmpty())) {
            this.gtnr$queried = true;
            NetWorkHandler.sendToServer(new RecursivePatternQueryPacket(false));
        }
        if (this.gtnr$recursivePatterns != null) {
            this.gtnr$recursivePatterns.setAttention(
                GTNR$NO_NET_OUTPUT.equals(error) || this.gtnr$index != null && !this.gtnr$index.blocking()
                    .isEmpty());
        }
        if (gtnr$tree().gtnr$ringView() == null) this.gtnr$ringFromList = false;
        boolean inRing = this.displayMode == GuiCraftConfirm.DisplayMode.TREE && gtnr$tree().gtnr$ringView() != null;
        if (this.gtnr$backToTree != null) this.gtnr$backToTree.visible = inRing;
        if (this.hideAvailable != null) this.hideAvailable.visible = !inRing;
    }

    @Inject(method = "drawFG", at = @At("TAIL"))
    private void gtnr$ringSummary(int offsetX, int offsetY, int mouseX, int mouseY, CallbackInfo ci) {
        var view = gtnr$tree().gtnr$ringView();
        if (view == null || this.displayMode != GuiCraftConfirm.DisplayMode.TREE || this.gtnr$backToTree == null) {
            return;
        }
        var ring = view.ring();
        var text = StatCollector.translateToLocalFormatted(
            // #tr gtnr.gui.crafting_ring.summary
            // # %s crafts, %s loops back, %s inputs
            // # zh_CN %s 次合成，%s 处绕回，%s 项外部输入
            "gtnr.gui.crafting_ring.summary",
            ring.crafts(),
            ring.backEdges()
                .size(),
            ring.externalInputs()
                .size());
        if (view.omitted() > 0) {
            // #tr gtnr.gui.crafting_ring.omitted
            // # , %s patterns not shown
            // # zh_CN ，另有 %s 个样板不在这一圈上
            text += StatCollector.translateToLocalFormatted("gtnr.gui.crafting_ring.omitted", view.omitted());
        }
        int x = this.gtnr$backToTree.xPosition - this.guiLeft + this.gtnr$backToTree.width + 4;
        this.fontRendererObj.drawString(text, x, 25, ColorUtils.guiTextColorGray.getColor());
    }

    @Unique
    private static boolean gtnr$has(IItemList<IAEStack<?>> list, IAEStack<?> item) {
        var found = list.findPrecise(item);
        return found != null && (found.getStackSize() > 0 || found.getCountRequestable() > 0);
    }

    /**
     * The cycles the plan cannot start because of: those without net output when that is the error, those short of
     * something of their own, and when the plan cannot start while showing nothing missing, as the lightweight and
     * standard calculations leave a cycle they could not close, those the plan goes through.
     */
    @Unique
    private List<Cycle> gtnr$blocking(List<Cycle> cycles) {
        var error = this.ccc.getErrorMessage();
        boolean noNetOutput = GTNR$NO_NET_OUTPUT.equals(error);
        var blocking = new ArrayList<Cycle>();
        if (!noNetOutput && !this.ccc.isSimulation()) return blocking;
        for (var cycle : cycles) {
            boolean blocks = noNetOutput && !cycle.netOutput();
            for (var item : cycle.items()) {
                if (blocks) break;
                blocks = gtnr$has(this.missing, item);
            }
            if (blocks) blocking.add(cycle);
        }
        if (blocking.isEmpty() && (noNetOutput || this.missing.isEmpty())) {
            for (var cycle : cycles) {
                for (var item : cycle.items()) {
                    if (gtnr$has(this.storage, item) || gtnr$has(this.pending, item)) {
                        blocking.add(cycle);
                        break;
                    }
                }
            }
        }
        if (blocking.isEmpty() && noNetOutput) blocking.addAll(cycles);
        return blocking;
    }

    @Override
    public void gtnr$cyclesFound(IAEStack<?> request, List<Cycle> cycles, boolean manual) {
        var blocking = gtnr$blocking(cycles);
        this.gtnr$index = CraftingCycleIndex.of(request, cycles, blocking);
        gtnr$tree().gtnr$setCycles(this.gtnr$index);
        if (manual) {
            RecursivePatternReport.show(this.mc, request, cycles);
        } else {
            RecursivePatternReport.showBlocking(this.mc, this.gtnr$index);
        }
    }

    // One ring back, and from the last one back to where it was opened from
    @Unique
    private void gtnr$leaveRing() {
        gtnr$tree().gtnr$closeRingView();
        if (gtnr$tree().gtnr$ringView() != null) return;
        if (this.gtnr$ringFromList && this.displayMode == GuiCraftConfirm.DisplayMode.TREE) {
            this.actionPerformed(this.switchDisplayMode);
        }
        this.gtnr$ringFromList = false;
    }

    // What the mouse is over in the list
    @Unique
    private IAEStack<?> gtnr$listStack() {
        if (this.displayMode != GuiCraftConfirm.DisplayMode.LIST || this.tooltip < 0 || this.getScrollBar() == null) {
            return null;
        }
        int index = this.getScrollBar()
            .getCurrentScroll() * 3 + this.tooltip;
        return index < this.filteredVisual.size() ? this.filteredVisual.get(index) : null;
    }

    @Inject(method = "drawTreeFG", at = @At("HEAD"), cancellable = true)
    private void gtnr$drawRingWithoutTree(int offsetX, int offsetY, int mouseX, int mouseY, CallbackInfo ci) {
        // A ring of a cycle found is shown whether or not there is a tree
        var view = gtnr$tree().gtnr$ringView();
        if (view != null && !view.ofTree()) {
            this.craftingTree.draw(mouseX, mouseY);
            ci.cancel();
        }
    }

    @Inject(method = "drawListFG", at = @At("HEAD"))
    private void gtnr$startList(CallbackInfo ci) {
        this.gtnr$listMarked = false;
    }

    @Inject(
        method = "drawListFG",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/client/gui/implementations/GuiCraftConfirm;drawTooltip(IILjava/lang/String;)V"))
    private void gtnr$markListUnderTooltip(CallbackInfo ci) {
        gtnr$markList();
    }

    @Inject(method = "drawListFG", at = @At("TAIL"))
    private void gtnr$markListWithoutTooltip(CallbackInfo ci) {
        gtnr$markList();
    }

    @Unique
    private void gtnr$markList() {
        if (this.gtnr$listMarked || this.gtnr$index == null || this.getScrollBar() == null) return;
        this.gtnr$listMarked = true;
        int viewStart = this.getScrollBar()
            .getCurrentScroll() * 3;
        int viewEnd = Math.min(viewStart + 3 * this.rows, this.filteredVisual.size());
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        for (int z = viewStart; z < viewEnd; z++) {
            var stack = this.filteredVisual.get(z);
            if (stack == null || this.gtnr$index.cyclesWith(stack)
                .isEmpty()) continue;
            int x = (z - viewStart) % 3 * 68 + 57;
            int y = (z - viewStart) / 3 * 23 + 22;
            if (this.gtnr$index.blocks(stack)) GL11.glColor4f(1, 0.4f, 0.4f, 1);
            else GL11.glColor4f(1, 1, 1, 1);
            RecursivePatternButton.drawIcon(this.mc, x - 5, y - 5, 8, 300);
        }
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glPopAttrib();
    }

    @ModifyArg(
        method = "drawListFG",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/client/gui/implementations/GuiCraftConfirm;drawTooltip(IILjava/lang/String;)V"),
        index = 2)
    private String gtnr$listTooltip(String text) {
        var stack = gtnr$listStack();
        if (stack == null || this.gtnr$index == null) return text;
        var cycles = this.gtnr$index.cyclesWith(stack);
        if (cycles.isEmpty()) return text;
        var lines = new StringBuilder(text);
        for (var cycle : cycles) {
            lines.append('\n')
                .append(
                    StatCollector.translateToLocalFormatted(
                        // #tr gtnr.gui.recursive_patterns.tooltip.net
                        // # §aCycle %s (net output)
                        // # zh_CN §a递归样板：循环 %s（有净产出）
                        cycle.netOutput() ? "gtnr.gui.recursive_patterns.tooltip.net"
                            // #tr gtnr.gui.recursive_patterns.tooltip.no_net
                            // # §cCycle %s (no net output)
                            // # zh_CN §c递归样板：循环 %s（无净产出）
                            : "gtnr.gui.recursive_patterns.tooltip.no_net",
                        this.gtnr$index.cycles()
                            .indexOf(cycle) + 1));
        }
        int machines = 0;
        for (var match : this.gtnr$index.matchesFor(stack)) {
            machines += match.location()
                .machines()
                .size();
        }
        lines.append('\n')
            .append(
                StatCollector.translateToLocalFormatted(
                    // #tr gtnr.gui.recursive_patterns.tooltip.machines
                    // # §7%s machines, press %s to highlight
                    // # zh_CN §7存放在 %s 台机器中，按 %s 高亮
                    "gtnr.gui.recursive_patterns.tooltip.machines",
                    machines,
                    RecursivePatternReport.highlightKeyName()))
            .append('\n')
            // #tr gtnr.gui.crafting_ring.open_list
            // # §7Shift-click: view as ring
            // # zh_CN §7Shift+左键：以环形查看这个循环
            .append(StatCollector.translateToLocal("gtnr.gui.crafting_ring.open_list"));
        return lines.toString();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = true)
    private void gtnr$openRingFromList(int mouseX, int mouseY, int button, CallbackInfo ci) {
        if (button != 0 || !isShiftKeyDown() || this.gtnr$index == null) return;
        var stack = gtnr$listStack();
        var cycles = stack == null ? ImmutableList.<Cycle>of() : this.gtnr$index.cyclesWith(stack);
        if (cycles.isEmpty()) return;
        this.actionPerformed(this.switchDisplayMode);
        if (gtnr$tree().gtnr$openCycle(cycles.get(0), stack)) {
            this.gtnr$ringFromList = true;
        } else {
            this.actionPerformed(this.switchDisplayMode);
        }
        ci.cancel();
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true, remap = true)
    private void gtnr$keys(char character, int key, CallbackInfo ci) {
        var tree = gtnr$tree();
        if (key == Keyboard.KEY_ESCAPE && tree.gtnr$ringView() != null) {
            gtnr$leaveRing();
            ci.cancel();
            return;
        }
        if (!RecursivePatternReport.isHighlightKey(key) || this.searchField.isFocused()) return;
        var listStack = gtnr$listStack();
        var hovered = this.displayMode == GuiCraftConfirm.DisplayMode.TREE ? tree.gtnr$hoveredPatterns()
            : listStack != null && this.gtnr$index != null ? this.gtnr$index.matchesFor(listStack)
                : ImmutableList.<CraftingCycleIndex.Match>of();
        if (!hovered.isEmpty()) {
            RecursivePatternReport.highlight(this.mc, this.gtnr$index.request(), hovered);
        } else if (this.gtnr$index != null) {
            RecursivePatternReport.show(this.mc, this.gtnr$index.request(), this.gtnr$index.cycles());
        } else {
            NetWorkHandler.sendToServer(new RecursivePatternQueryPacket(true));
        }
        ci.cancel();
    }
}
