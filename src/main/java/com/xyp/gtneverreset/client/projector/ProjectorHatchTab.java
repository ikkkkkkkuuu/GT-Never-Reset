package com.xyp.gtneverreset.client.projector;

import static com.xyp.gtneverreset.client.projector.ProjectorWidgets.CONTROL;
import static com.xyp.gtneverreset.client.projector.ProjectorWidgets.MENU;
import static com.xyp.gtneverreset.client.projector.ProjectorWidgets.MENU_WIDTH;
import static com.xyp.gtneverreset.client.projector.ProjectorWidgets.ROW;
import static com.xyp.gtneverreset.client.projector.ProjectorWidgets.SMALL;
import static com.xyp.gtneverreset.client.projector.ProjectorWidgets.column;
import static com.xyp.gtneverreset.client.projector.ProjectorWidgets.overlay;
import static com.xyp.gtneverreset.client.projector.ProjectorWidgets.trim;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.value.ObjectValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.config.ClientConfig;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.widget.ActionButton;
import com.xyp.gtnrlib.gui.widget.CycleDropdown;
import com.xyp.gtnrlib.gui.widget.IconRowList;
import com.xyp.gtnrlib.gui.widget.IconRowList.Row;
import com.xyp.gtnrlib.gui.widget.ListRow;
import com.xyp.gtnrlib.gui.widget.ValueStepper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ProjectorHatchTab {

    private static final String LANG = ProjectorGui.LANG;
    private static final int HEADER_HEIGHT = 26, CARD_HEIGHT = HEADER_HEIGHT + 5 * ROW + 2, NAME_Y = 9, TEXT_INSET = 17,
        ICON_WIDTH = 19;

    private final ProjectorGui gui;
    private final ProjectorHatchPage hatches;
    private final IconRowList hatchList = new IconRowList(this::hatchRows);

    public ProjectorHatchTab(ProjectorGui gui) {
        this.gui = gui;
        this.hatches = gui.hatches;
    }

    public Flow build() {
        int wellWidth = gui.wellWidth;
        BooleanSupplier editable = hatches::editable, block = hatches::blockCell,
            casing = () -> editable.getAsBoolean() && ProjectorHatchPage.CASING.equals(hatches.kind()),
            model = () -> editable.getAsBoolean() && !casing.getAsBoolean(),
            casings = () -> casing.getAsBoolean() && hatches.casings()
                .size() > 1;
        Flow card = column().size(wellWidth, CARD_HEIGHT)
            .padding(0, 0, 1, 1)
            .background(gui.well())
            .child(new SpotHeader())
            .child(
                new ListRow(
                    // #tr gtneverreset.projector.gui.hatch_kind
                    // # Kind
                    // # zh_CN 种类
                    () -> StatCollector.translateToLocal(LANG + (block.getAsBoolean() ? "block" : "hatch_kind")))
                        .width(wellWidth)
                        .control(
                            overlay(
                                kindDropdown().setEnabledIf(w -> editable.getAsBoolean()),
                                blockDropdown().setEnabledIf(w -> block.getAsBoolean()))
                                    .setEnabledIf(w -> editable.getAsBoolean() || block.getAsBoolean()),
                            CONTROL,
                            SMALL))
            .child(
                gui.controlRow(
                    // #tr gtneverreset.projector.gui.hatch_variant
                    // # Model
                    // # zh_CN 型号
                    LANG + "hatch_variant",
                    overlay(
                        variantDropdown().setEnabledIf(w -> model.getAsBoolean()),
                        casingDropdown().setEnabledIf(w -> casings.getAsBoolean()))
                            .setEnabledIf(w -> model.getAsBoolean() || casings.getAsBoolean())))
            .child(
                gui.controlRow(
                    // #tr gtneverreset.projector.gui.hatch_level
                    // # Tier
                    // # zh_CN 等级
                    LANG + "hatch_level",
                    new ValueStepper(hatches::tierLabel, hatches::stepTier).wheel(true)
                        .size(CONTROL, SMALL)
                        .setEnabledIf(w -> model.getAsBoolean() && hatches.hasTiers())))
            .child(
                gui.controlRow(
                    // #tr gtneverreset.projector.gui.hatch_amperes
                    // # Amperage
                    // # zh_CN 电流
                    LANG + "hatch_amperes",
                    new ValueStepper(hatches::amperesLabel, hatches::stepAmperes).wheel(true)
                        .size(CONTROL, SMALL)
                        .setEnabledIf(w -> model.getAsBoolean() && hatches.hasAmperes())))
            .child(
                new ParentWidget<>().size(wellWidth, ROW)
                    .child(
                        new ListRow(
                            () -> StatCollector
                                // #tr gtneverreset.projector.gui.same_kind
                                // # Change spots of the same kind together
                                // # zh_CN 同类仓位一起改
                                // #tr gtneverreset.projector.gui.same_casing
                                // # Change the whole casing group
                                // # zh_CN 同组外壳一起换
                                .translateToLocal(LANG + (casing.getAsBoolean() ? "same_casing" : "same_kind")))
                                    .width(wellWidth)
                                    .toggle(
                                        () -> casing.getAsBoolean() ? hatches.sameCasing() : hatches.sameKind(),
                                        () -> {
                                            if (casing.getAsBoolean()) hatches.toggleSameCasing();
                                            else hatches.toggleSameKind();
                                        })
                                    .setEnabledIf(w -> !block.getAsBoolean() && !hatches.channelCasing()))
                    .child(
                        new ListRow(
                            () -> block.getAsBoolean()
                                // #tr gtneverreset.projector.gui.group_cells
                                // # All %s spots of this group change together
                                // # zh_CN 同一组 %s 格一起换
                                ? StatCollector.translateToLocalFormatted(LANG + "group_cells", hatches.groupSize())
                                // #tr gtneverreset.projector.spot.channel_casing
                                // # Casing grade changes for the whole machine
                                // # zh_CN 外壳等级整台机器一起换
                                : StatCollector.translateToLocal(ProjectorSpotScreen.LANG + "channel_casing"))
                                    .width(wellWidth)
                                    .setEnabledIf(w -> block.getAsBoolean() || hatches.channelCasing())));
        int width = ProjectorGui.RIGHT_WIDTH, height = ProjectorGui.PAGE_HEIGHT;
        return column().size(width, height)
            .childPadding(4)
            .child(card)
            .child(hatchList.size(width, height - CARD_HEIGHT - 4));
    }

    private CycleDropdown<String> casingDropdown() {
        CycleDropdown<String> dropdown = new CycleDropdown<>(MENU + "casing", String.class).wheel(true);
        dropdown.optionToWidget((key, forShown) -> blockOption(key))
            .value(new ObjectValue.Dynamic<>(String.class, hatches::casingKey, hatches::chooseCasing))
            .menuWidth(MENU_WIDTH)
            .size(CONTROL, SMALL)
            .onUpdateListener(widget -> widget.choices(hatches.casingKeys()), true);
        return dropdown;
    }

    private CycleDropdown<String> blockDropdown() {
        CycleDropdown<String> dropdown = new CycleDropdown<>(MENU + "block", String.class).wheel(true);
        dropdown.optionToWidget((key, forShown) -> blockOption(key))
            .value(new ObjectValue.Dynamic<>(String.class, hatches::blockKey, hatches::chooseBlock))
            .menuWidth(MENU_WIDTH)
            .size(CONTROL, SMALL)
            .onUpdateListener(widget -> widget.choices(hatches.blockKeys()), true);
        return dropdown;
    }

    private IWidget blockOption(String key) {
        if (key == null) return CycleDropdown.iconOption(null, "");
        if (ProjectorPlan.AIR.equals(key))
            return CycleDropdown.iconOption(null, StatCollector.translateToLocal(ProjectorSpotScreen.LANG + "empty"));
        ItemStack stack = hatches.stackOf(key);
        return CycleDropdown.iconOption(stack, stack == null ? key : ProjectorChannels.name(stack));
    }

    private CycleDropdown<Object> kindDropdown() {
        CycleDropdown<Object> dropdown = new CycleDropdown<>(MENU + "kind", Object.class).wheel(true);
        dropdown
            .optionToWidget(
                (kind, forShown) -> CycleDropdown.iconOption(hatches.kindIcon(kind), hatches.kindName(kind)))
            .value(new ObjectValue.Dynamic<>(Object.class, hatches::kind, hatches::chooseKind))
            .menuWidth(MENU_WIDTH)
            .size(CONTROL, SMALL)
            .onUpdateListener(widget -> widget.choices(hatches.kinds()), true);
        return dropdown;
    }

    private CycleDropdown<Integer> variantDropdown() {
        CycleDropdown<Integer> dropdown = new CycleDropdown<>(MENU + "variant", Integer.class).wheel(true);
        dropdown
            .optionToWidget(
                (first, forShown) -> CycleDropdown.iconOption(
                    first == null ? null : hatches.variantIcon(first),
                    first == null ? "" : hatches.variantLabel(first)))
            .value(new ObjectValue.Dynamic<>(Integer.class, hatches::variant, hatches::chooseVariant))
            .menuWidth(MENU_WIDTH)
            .size(CONTROL, SMALL)
            .onUpdateListener(widget -> widget.choices(hatches.variants()), true);
        return dropdown;
    }

    private List<Row> hatchRows() {
        List<Row> rows = new ArrayList<>();
        // #tr gtneverreset.projector.gui.outline
        // # Hatch spot outlines
        // # zh_CN 仓位描边
        rows.add(Row.header(StatCollector.translateToLocal(LANG + "outline")));
        rows.add(
            Row.toggle(
                // #tr gtneverreset.projector.gui.hatch_outlines
                // # Hatch outlines
                // # zh_CN 仓室描边
                StatCollector.translateToLocal(LANG + "hatch_outlines"),
                ClientConfig.projectorHatchOutlines,
                () -> {
                    ClientConfig.projectorHatchOutlines = !ClientConfig.projectorHatchOutlines;
                    gui.configChanged = true;
                }));
        rows.add(
            Row.toggle(
                // #tr gtneverreset.projector.gui.outline_merge
                // # Join inner edges
                // # zh_CN 连接内边
                StatCollector.translateToLocal(LANG + "outline_merge"),
                ClientConfig.projectorMergeOutlines,
                () -> {
                    ClientConfig.projectorMergeOutlines = !ClientConfig.projectorMergeOutlines;
                    gui.configChanged = true;
                }));
        rows.addAll(hatches.spotRows());
        rows.addAll(hatches.choiceRows());
        return rows;
    }

    private class SpotHeader extends ParentWidget<SpotHeader> {

        private SpotHeader() {
            size(gui.wellWidth, HEADER_HEIGHT);
            child(
                ActionButton.text(() -> "<", () -> hatches.step(-1))
                    .size(ValueStepper.ARROW_WIDTH, ProjectorWidgets.BUTTON)
                    .left(2)
                    .top(3));
            child(
                ActionButton.text(() -> ">", () -> hatches.step(1))
                    .size(ValueStepper.ARROW_WIDTH, ProjectorWidgets.BUTTON)
                    .right(2)
                    .top(3));
        }

        @Override
        public boolean canHover() {
            return true;
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            int left = TEXT_INSET, right = getArea().width - TEXT_INSET;
            String name = hatches.nameLine();
            if (name.isEmpty()) {
                String hint = trim(hatches.placeLine(), right - left);
                AE2Theme.text(hint, left + (right - left - font.getStringWidth(hint)) / 2, NAME_Y, gui.muted, false);
                return;
            }
            ItemStack icon = hatches.icon();
            String count = hatches.countLine();
            int countWidth = count.isEmpty() ? 0 : font.getStringWidth(count) + 3;
            int iconWidth = icon == null ? 0 : ICON_WIDTH;
            String shown = trim(name, right - left - countWidth - iconWidth);
            int group = iconWidth + font.getStringWidth(shown);
            int start = Math.max(left, Math.min(left + (right - left - group) / 2, right - countWidth - group));
            if (icon != null) new ItemDrawable(icon).draw(context, start, 5, 16, 16, widgetTheme.getTheme());
            int text = gui.variant == null ? IconRowList.TEXT : gui.variant.colors.text();
            AE2Theme.text(shown, start + iconWidth, NAME_Y, text, false);
            if (!count.isEmpty()) AE2Theme.text(count, right - countWidth + 3, NAME_Y, gui.muted, false);
        }
    }
}
