package com.xyp.gtneverreset.client.quantum;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.ImmutableList;
import com.xyp.gtneverreset.common.crafting.cycle.RecursivePatternFinder.Cycle;
import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.RecursivePatternQueryPacket;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.client.gui.widgets.GuiAeButton;
import appeng.client.gui.widgets.GuiCraftingTree;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.core.localization.ColorUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class CraftConfirmCycles implements ICraftingCycleHost {

    // #tr gtnr.quantum_crafting.message.crafting_no_net_output
    // # Patterns cannot yield a net output. Check cycles and missing external inputs.
    // # zh_CN 样板无法净产出请求物品，请检查循环样板及缺少的外部输入。
    private static final String NO_NET_OUTPUT = "gtnr.quantum_crafting.message.crafting_no_net_output";
    private RecursivePatternButton recursivePatterns;
    private GuiAeButton backToTree;
    private boolean queried;
    private CraftingCycleIndex index;

    // The ring shown was opened from the list, to which leaving it goes back
    private boolean ringFromList;
    private boolean listMarked;
    private final Host host;

    public CraftConfirmCycles(Host host) {
        this.host = host;
    }

    private ICraftingTreeCycles tree() {
        return (ICraftingTreeCycles) host.tree();
    }

    public void addButtons() {
        this.recursivePatterns = new RecursivePatternButton(host.left() + host.width() + 2, host.top() + 8 + 20 * 3);
        host.buttons()
            .add(this.recursivePatterns);
        // #tr gtnr.gui.crafting_ring.back
        // # Back
        // # zh_CN 返回
        var back = StatCollector.translateToLocal("gtnr.gui.crafting_ring.back");
        this.backToTree = new GuiAeButton(
            0,
            host.left() + 9,
            host.top() + 20,
            Math.min(
                host.tree().widgetW,
                host.font()
                    .getStringWidth(back) + 10),
            18,
            back,
            "");
        this.backToTree.visible = false;
        host.buttons()
            .add(this.backToTree);
    }

    public boolean buttons(GuiButton button) {
        if (button == this.recursivePatterns) {
            NetWorkHandler.sendToServer(new RecursivePatternQueryPacket(true));
            return true;
        } else if (button == this.backToTree) {
            leaveRing();
            return true;
        }

        return false;
    }

    public void update(int mouseX, int mouseY, float partialTicks) {
        var error = host.container()
            .getErrorMessage();
        if (!this.queried && (host.container()
            .getUsedBytes() > 0 || !error.isEmpty())) {
            this.queried = true;
            NetWorkHandler.sendToServer(new RecursivePatternQueryPacket(false));
        }
        if (this.recursivePatterns != null) {
            this.recursivePatterns.setAttention(
                NO_NET_OUTPUT.equals(error) || this.index != null && !this.index.blocking()
                    .isEmpty());
        }
        if (tree().gtnr$ringView() == null) this.ringFromList = false;
        boolean inRing = host.displayMode() == GuiCraftConfirm.DisplayMode.TREE && tree().gtnr$ringView() != null;
        if (this.backToTree != null) this.backToTree.visible = inRing;
        if (host.hideAvailable() != null) host.hideAvailable().visible = !inRing;
    }

    public void ringSummary(int offsetX, int offsetY, int mouseX, int mouseY) {
        var view = tree().gtnr$ringView();
        if (view == null || host.displayMode() != GuiCraftConfirm.DisplayMode.TREE || this.backToTree == null) {
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
        int x = this.backToTree.xPosition - host.left() + this.backToTree.width + 4;
        host.font()
            .drawString(text, x, 25, ColorUtils.guiTextColorGray.getColor());
    }

    private static boolean has(IItemList<IAEStack<?>> list, IAEStack<?> item) {
        var found = list.findPrecise(item);
        return found != null && (found.getStackSize() > 0 || found.getCountRequestable() > 0);
    }

    /**
     * The cycles the plan cannot start because of: those without net output when that is the error, those short of
     * something of their own, and when the plan cannot start while showing nothing missing, as the lightweight and
     * standard calculations leave a cycle they could not close, those the plan goes through.
     */
    private List<Cycle> blocking(List<Cycle> cycles) {
        var error = host.container()
            .getErrorMessage();
        boolean noNetOutput = NO_NET_OUTPUT.equals(error);
        var blocking = new ArrayList<Cycle>();
        if (!noNetOutput && !host.container()
            .isSimulation()) return blocking;
        for (var cycle : cycles) {
            boolean blocks = noNetOutput && !cycle.netOutput();
            for (var item : cycle.items()) {
                if (blocks) break;
                blocks = has(host.missing(), item);
            }
            if (blocks) blocking.add(cycle);
        }
        if (blocking.isEmpty() && (noNetOutput || host.missing()
            .isEmpty())) {
            for (var cycle : cycles) {
                for (var item : cycle.items()) {
                    if (has(host.storage(), item) || has(host.pending(), item)) {
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
        var blocking = blocking(cycles);
        this.index = CraftingCycleIndex.of(request, cycles, blocking);
        tree().gtnr$setCycles(this.index);
        if (manual) {
            RecursivePatternReport.show(host.minecraft(), request, cycles);
        } else {
            RecursivePatternReport.showBlocking(host.minecraft(), this.index);
        }
    }

    // One ring back, and from the last one back to where it was opened from
    private void leaveRing() {
        tree().gtnr$closeRingView();
        if (tree().gtnr$ringView() != null) return;
        if (this.ringFromList && host.displayMode() == GuiCraftConfirm.DisplayMode.TREE) {
            host.click(host.displaySwitch());
        }
        this.ringFromList = false;
    }

    // What the mouse is over in the list
    private IAEStack<?> listStack() {
        if (host.displayMode() != GuiCraftConfirm.DisplayMode.LIST || host.tooltipIndex() < 0
            || host.scrollbar() == null) {
            return null;
        }
        int index = host.scrollbar()
            .getCurrentScroll() * 3 + host.tooltipIndex();
        return index < host.visual()
            .size() ? host.visual()
                .get(index) : null;
    }

    public boolean drawRingWithoutTree(int offsetX, int offsetY, int mouseX, int mouseY) {
        // A ring of a cycle found is shown whether or not there is a tree
        var view = tree().gtnr$ringView();
        if (view != null && !view.ofTree()) {
            host.tree()
                .draw(mouseX, mouseY);
            return true;
        }

        return false;
    }

    public void startList() {
        this.listMarked = false;
    }

    public void markListUnderTooltip() {
        markList();
    }

    public void markListWithoutTooltip() {
        markList();
    }

    private void markList() {
        if (this.listMarked || this.index == null || host.scrollbar() == null) return;
        this.listMarked = true;
        int viewStart = host.scrollbar()
            .getCurrentScroll() * 3;
        int viewEnd = Math.min(
            viewStart + 3 * host.rows(),
            host.visual()
                .size());
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        for (int z = viewStart; z < viewEnd; z++) {
            var stack = host.visual()
                .get(z);
            if (stack == null || this.index.cyclesWith(stack)
                .isEmpty()) continue;
            int x = (z - viewStart) % 3 * 68 + 57;
            int y = (z - viewStart) / 3 * 23 + 22;
            if (this.index.blocks(stack)) GL11.glColor4f(1, 0.4f, 0.4f, 1);
            else GL11.glColor4f(1, 1, 1, 1);
            RecursivePatternButton.drawIcon(host.minecraft(), x - 5, y - 5, 8, 300);
        }
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glPopAttrib();
    }

    public String listTooltip(String text) {
        var stack = listStack();
        if (stack == null || this.index == null) return text;
        var cycles = this.index.cyclesWith(stack);
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
                        this.index.cycles()
                            .indexOf(cycle) + 1));
        }
        int machines = 0;
        for (var match : this.index.matchesFor(stack)) {
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

    public boolean openRingFromList(int mouseX, int mouseY, int button) {
        if (button != 0 || !GuiScreen.isShiftKeyDown() || this.index == null) return false;
        var stack = listStack();
        var cycles = stack == null ? ImmutableList.<Cycle>of() : this.index.cyclesWith(stack);
        if (cycles.isEmpty()) return false;
        host.click(host.displaySwitch());
        if (tree().gtnr$openCycle(cycles.get(0), stack)) {
            this.ringFromList = true;
        } else {
            host.click(host.displaySwitch());
        }
        return true;
    }

    public boolean keys(char character, int key) {
        var tree = tree();
        if (key == Keyboard.KEY_ESCAPE && tree.gtnr$ringView() != null) {
            leaveRing();
            return true;
        }
        if (!RecursivePatternReport.isHighlightKey(key) || host.searchField()
            .isFocused()) return false;
        var listStack = listStack();
        var hovered = host.displayMode() == GuiCraftConfirm.DisplayMode.TREE ? tree.gtnr$hoveredPatterns()
            : listStack != null && this.index != null ? this.index.matchesFor(listStack)
                : ImmutableList.<CraftingCycleIndex.Match>of();
        if (!hovered.isEmpty()) {
            RecursivePatternReport.highlight(host.minecraft(), this.index.request(), hovered);
        } else if (this.index != null) {
            RecursivePatternReport.show(host.minecraft(), this.index.request(), this.index.cycles());
        } else {
            NetWorkHandler.sendToServer(new RecursivePatternQueryPacket(true));
        }
        return true;
    }

    public interface Host {

        ContainerCraftConfirm container();

        GuiCraftingTree tree();

        IItemList<IAEStack<?>> missing();

        IItemList<IAEStack<?>> storage();

        IItemList<IAEStack<?>> pending();

        List<IAEStack<?>> visual();

        int tooltipIndex();

        int rows();

        GuiTabButton displaySwitch();

        GuiCraftConfirm.DisplayMode displayMode();

        GuiAeButton hideAvailable();

        MEGuiTextField searchField();

        int left();

        int top();

        int width();

        List<GuiButton> buttons();

        FontRenderer font();

        Minecraft minecraft();

        GuiScrollbar scrollbar();

        void click(GuiButton button);
    }
}
