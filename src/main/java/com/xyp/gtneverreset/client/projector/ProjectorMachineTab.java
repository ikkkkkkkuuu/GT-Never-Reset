package com.xyp.gtneverreset.client.projector;

import static com.xyp.gtneverreset.client.projector.ProjectorWidgets.ROW;
import static com.xyp.gtneverreset.client.projector.ProjectorWidgets.column;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.util.data.ItemId;
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.widget.GhostItemSlot;
import com.xyp.gtnrlib.gui.widget.IconRowList;
import com.xyp.gtnrlib.gui.widget.IconRowList.Row;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;

@SideOnly(Side.CLIENT)
public class ProjectorMachineTab {

    private static final String LANG = ProjectorGui.LANG;
    private static List<Machine> machineOrder;
    private static String machineLanguage;

    private final ProjectorGui gui;
    private final GhostItemSlot machineSlot = new GhostItemSlot();
    private final IconRowList machineList = new IconRowList(this::machineRows);
    private final Int2IntLinkedOpenHashMap carried = new Int2IntLinkedOpenHashMap();
    private String search = "";
    private int inventoryVersion;
    private MachineRowsKey machineRowsKey;
    private List<Row> machineRows = Collections.emptyList();

    @Desugar
    private record Machine(int id, ItemStack stack, String name, String searchName) {}

    @Desugar
    private record MachineRowsKey(String search, int machine, int inventory) {}

    public ProjectorMachineTab(ProjectorGui gui) {
        this.gui = gui;
    }

    public Flow build() {
        machineSlot.clearable(false)
            .filter(this::acceptsController)
            .onChange(item -> gui.selectMachine(ProjectorMachines.supportedIdOf(item.toStack())));
        showMachine();
        TextFieldWidget field = new TextFieldWidget().setMaxLength(64)
            // #tr gtneverreset.projector.gui.search
            // # Search machines
            // # zh_CN 搜索机器
            .hintText(StatCollector.translateToLocal(LANG + "search"));
        field.autoUpdateOnChange(true)
            .value(new StringValue.Dynamic(() -> search, text -> {
                if (search.equals(text)) return;
                search = text;
                machineList.scrollToTop();
            }));
        int width = ProjectorGui.RIGHT_WIDTH, height = ProjectorGui.PAGE_HEIGHT;
        return column().size(width, height)
            .childPadding(4)
            .child(
                Flow.row()
                    .size(width, ROW)
                    .childPadding(4)
                    .child(machineSlot)
                    .child(
                        column().size(width - ROW - 4, ROW)
                            .child(
                                IKey.dynamic(this::machineName)
                                    .asWidget()
                                    .alignment(Alignment.CenterLeft)
                                    .widthRel(1f)
                                    .height(ROW / 2))
                            .child(
                                // #tr gtneverreset.projector.gui.drag_hint
                                // # Drag a controller here
                                // # zh_CN 把控制器拖到这里
                                IKey.lang(LANG + "drag_hint")
                                    .asWidget()
                                    .color(gui.muted)
                                    .alignment(Alignment.CenterLeft)
                                    .widthRel(1f)
                                    .height(ROW / 2))))
            .child(
                field.width(gui.wellWidth)
                    .height(AE2Theme.TEXT_FIELD_HEIGHT))
            .child(machineList.size(width, height - ROW - AE2Theme.TEXT_FIELD_HEIGHT - 8));
    }

    public void showMachine() {
        ItemStack stack = ProjectorMachines.stackOf(gui.working.machine);
        machineSlot.item(stack == null ? null : ItemId.createWithoutNBT(stack));
    }

    public void refreshCarried(EntityPlayer player) {
        carried.clear();
        inventoryVersion++;
        for (ItemStack stack : ProjectorClient.carried(player)) {
            int id = ProjectorMachines.supportedIdOf(stack);
            if (id > 0) carried.addTo(id, stack.stackSize);
        }
    }

    public int firstCarried() {
        return carried.isEmpty() ? 0 : carried.firstIntKey();
    }

    private boolean acceptsController(ItemStack stack) {
        if (ProjectorMachines.supportedIdOf(stack) > 0) return true;
        // #tr gtneverreset.projector.gui.not_controller
        // # Not a multiblock controller the projector can build
        // # zh_CN 这不是投影仪能搭的多方块控制器
        gui.notice("not_controller");
        return false;
    }

    private String machineName() {
        ItemStack stack = ProjectorMachines.stackOf(gui.working.machine);
        if (stack == null) return EnumChatFormatting.GRAY + StatCollector.translateToLocal(LANG + "no_machine");
        return stack.getDisplayName();
    }

    private static List<Machine> machineOrder() {
        String language = Minecraft.getMinecraft()
            .getLanguageManager()
            .getCurrentLanguage()
            .getLanguageCode();
        if (machineOrder == null || !language.equals(machineLanguage)) {
            List<Machine> order = new ArrayList<>();
            for (int id : ProjectorMachines.supported()) {
                ItemStack stack = ProjectorMachines.stackOf(id);
                String name = stack.getDisplayName();
                order.add(new Machine(id, stack, name, name.toLowerCase(Locale.ROOT)));
            }
            order.sort(Comparator.comparing(Machine::name));
            machineOrder = order;
            machineLanguage = language;
        }
        return machineOrder;
    }

    private List<Row> machineRows() {
        MachineRowsKey key = new MachineRowsKey(search, gui.working.machine, inventoryVersion);
        if (key.equals(machineRowsKey)) return machineRows;
        machineRowsKey = key;
        List<Row> rows = new ArrayList<>();
        String wanted = search.toLowerCase(Locale.ROOT);
        if (!carried.isEmpty()) {
            // #tr gtneverreset.projector.gui.carried
            // # In your inventory
            // # zh_CN 背包里有
            rows.add(Row.header(StatCollector.translateToLocal(LANG + "carried")));
            for (Int2IntLinkedOpenHashMap.Entry entry : carried.int2IntEntrySet()) {
                ItemStack stack = ProjectorMachines.stackOf(entry.getIntKey());
                String name = stack.getDisplayName();
                if (name.toLowerCase(Locale.ROOT)
                    .contains(wanted)) rows.add(machineRow(entry.getIntKey(), stack, name, "x" + entry.getIntValue()));
            }
        }
        // #tr gtneverreset.projector.gui.all_machines
        // # All machines
        // # zh_CN 全部机器
        rows.add(Row.header(StatCollector.translateToLocal(LANG + "all_machines")));
        for (Machine machine : machineOrder()) {
            if (machine.searchName()
                .contains(wanted)) rows.add(machineRow(machine.id(), machine.stack(), machine.name(), ""));
        }
        return machineRows = rows;
    }

    private Row machineRow(int id, ItemStack stack, String name, String detail) {
        return Row.item(stack, name, detail, id == gui.working.machine, () -> gui.selectMachine(id));
    }
}
