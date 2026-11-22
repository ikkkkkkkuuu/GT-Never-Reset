package com.xyp.gtneverreset.client.projector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Choice;
import com.xyp.gtneverreset.config.ClientConfig;
import com.xyp.gtneverreset.config.ConfigRegistry;
import com.xyp.gtneverreset.config.ScreenThemeConfig;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;
import com.xyp.gtnrlib.gui.widget.AE2Button;
import com.xyp.gtnrlib.gui.widget.IconRowList;
import com.xyp.gtnrlib.gui.widget.IconRowList.Row;
import com.xyp.gtnrlib.gui.widget.ListRow;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

@SideOnly(Side.CLIENT)
public class ProjectorSpotScreen extends ModularScreen {

    public static final String LANG = "gtneverreset.projector.spot.";
    private static final String NAME = "projector_spot";
    private static final int PAD = 7, TOP = 6, LINE = 10, ROW = IconRowList.ROW, VISIBLE = 7, KIND_WIDTH = 76,
        MODEL_WIDTH = 150, TIER_WIDTH = 40, AMPERES_WIDTH = 64, CHOICE_WIDTH = 160, GAP = 2, BUTTON = 16,
        BUTTON_WIDTH = 60, CONFIRM_WIDTH = 50, SWITCH_ROW_WIDTH = 120, CROSSHAIR_GAP = 12, SCREEN_MARGIN = 4,
        HATCH_COLOR = 0x40D8FF, GROUP_COLOR = 0xB070FF, PLAIN_MUTED = 0xFF808080;
    private static final int SCROLL = IconRowList.AE2_SCROLL_COLUMN;

    private final int location;
    private final ProjectorPlan working;
    private final NBTTagCompound before;
    private final Runnable quickPick;
    private final ProjectorHatchPage hatches;
    private boolean confirmed;

    private ProjectorSpotScreen(ModularPanel panel, ProjectorPlan working, Runnable quickPick,
        ProjectorHatchPage hatches) {
        super(GTNeverReset.MODID, panel);
        location = ProjectorClient.location;
        this.working = working;
        before = working.writeToNBT();
        this.quickPick = quickPick;
        this.hatches = hatches;
    }

    public static void openHatch(ProjectorStructure shown, int cell) {
        ProjectorPlan working = ProjectorClient.plan.copy();
        ProjectorHatchPage hatches = new ProjectorHatchPage(() -> shown, working, () -> cell, spot -> {});
        int kinds = KIND_WIDTH + SCROLL, models = MODEL_WIDTH + SCROLL, tiers = TIER_WIDTH + SCROLL,
            amperes = AMPERES_WIDTH + SCROLL;
        int inner = kinds + GAP + models + GAP + tiers + GAP + amperes, listHeight = 2 + VISIBLE * ROW;
        ModularPanel panel = panel(inner, listHeight + 4 + ROW);
        // #tr gtneverreset.projector.spot.hatch
        // # Hatch spot
        // # zh_CN 仓位
        title(panel, inner, StatCollector.translateToLocal(LANG + "hatch"), hatches.placeLine());
        int top = TOP + LINE + 4;
        panel.child(
            new IconRowList(() -> kindRows(hatches)).revealSelected()
                .pos(PAD, top)
                .size(kinds, listHeight));
        panel.child(
            new IconRowList(() -> modelRows(hatches)).revealSelected()
                .pos(PAD + kinds + GAP, top)
                .size(models, listHeight));
        panel.child(
            new IconRowList(() -> tierRows(hatches)).revealSelected()
                .pos(PAD + kinds + GAP + models + GAP, top)
                .size(tiers, listHeight));
        panel.child(
            new IconRowList(() -> amperesRows(hatches)).revealSelected()
                .pos(PAD + kinds + GAP + models + GAP + tiers + GAP, top)
                .size(amperes, listHeight));
        int bottom = top + listHeight + 4;
        BooleanSupplier casing = () -> ProjectorHatchPage.CASING.equals(hatches.kind());
        panel.child(
            new ListRow(
                // #tr gtneverreset.projector.spot.same_kind
                // # Same kind together
                // # zh_CN 同类仓位一起改
                // #tr gtneverreset.projector.spot.same_casing
                // # Whole casing group
                // # zh_CN 同组外壳一起换
                () -> StatCollector.translateToLocal(LANG + (casing.getAsBoolean() ? "same_casing" : "same_kind")))
                    .checkbox(() -> casing.getAsBoolean() ? hatches.sameCasing() : hatches.sameKind(), () -> {
                        if (casing.getAsBoolean()) hatches.toggleSameCasing();
                        else hatches.toggleSameKind();
                    })
                    .pos(PAD, bottom)
                    .size(SWITCH_ROW_WIDTH, ROW)
                    .setEnabledIf(w -> !hatches.channelCasing()));
        panel.child(
            muted(StatCollector.translateToLocal(LANG + "channel_casing")).alignment(Alignment.CenterLeft)
                .pos(PAD + ListRow.TEXT_INSET, bottom)
                .size(inner - CONFIRM_WIDTH - BUTTON_WIDTH - 2 * GAP - ListRow.TEXT_INSET, ROW)
                .setEnabledIf(w -> hatches.channelCasing()));
        // #tr gtneverreset.projector.spot.full
        // # Full view
        // # zh_CN 完整界面
        panel.child(new AE2Button(() -> StatCollector.translateToLocal(LANG + "full"), () -> {
            confirm(panel);
            ProjectorClient.openAt(shown, cell);
        }).pos(PAD + inner - CONFIRM_WIDTH - GAP - BUTTON_WIDTH, bottom + 1)
            .size(BUTTON_WIDTH, BUTTON));
        panel.child(
            // #tr gtneverreset.projector.spot.confirm
            // # Confirm
            // # zh_CN 确认
            new AE2Button(() -> StatCollector.translateToLocal(LANG + "confirm"), () -> confirm(panel))
                .pos(PAD + inner - CONFIRM_WIDTH, bottom + 1)
                .size(CONFIRM_WIDTH, BUTTON));
        open(panel, working, null, hatches, new int[] { cell }, HATCH_COLOR);
    }

    public static void openChoice(ProjectorStructure shown, int cell) {
        Choice choice = shown.choiceAt(cell);
        int group = shown.choiceOf[cell];
        ProjectorPlan working = ProjectorClient.plan.copy();
        int need = shown.countOf(group);
        Int2IntOpenHashMap have = ProjectorClient.inventory(Minecraft.getMinecraft().thePlayer);
        List<Row> rows = new ArrayList<>();
        String chosen = working.chosenBlock(choice);
        ModularPanel panel = panel(CHOICE_WIDTH + SCROLL, 0);
        if (choice.air()) rows.add(
            Row.item(
                null,
                StatCollector.translateToLocal(LANG + "empty"),
                null,
                ProjectorPlan.AIR.equals(chosen),
                () -> {
                    working.chooseBlock(choice, ProjectorPlan.AIR);
                    confirm(panel);
                }));
        for (ItemStack block : choice.blocks()) {
            String key = ProjectorPlan.blockKey(block);
            rows.add(countedRow(block, ProjectorChannels.name(block), key.equals(chosen), have, need, () -> {
                working.chooseBlock(choice, key);
                confirm(panel);
            }));
        }
        int listHeight = 2 + Math.min(VISIBLE, rows.size()) * ROW, inner = CHOICE_WIDTH + SCROLL;
        panel.size(PAD * 2 + inner, TOP + LINE + 4 + listHeight + 4 + LINE + PAD);
        title(
            panel,
            inner,
            // #tr gtneverreset.projector.spot.block
            // # Block
            // # zh_CN 方块
            StatCollector.translateToLocal(LANG + "block"),
            // #tr gtneverreset.projector.spot.cells
            // # %s spots
            // # zh_CN 共 %s 格
            StatCollector.translateToLocalFormatted(LANG + "cells", need));
        int top = TOP + LINE + 4;
        panel.child(
            new IconRowList(() -> rows).revealSelected()
                .pos(PAD, top)
                .size(inner, listHeight));
        panel.child(
            // #tr gtneverreset.projector.spot.group_hint
            // # Click a row to apply; all %s spots change together
            // # zh_CN 点一行就生效，这 %s 格一起改
            muted(StatCollector.translateToLocalFormatted(LANG + "group_hint", need)).pos(PAD, top + listHeight + 5)
                .size(inner, LINE));
        open(panel, working, null, null, cellsOf(shown, group), GROUP_COLOR);
    }

    public static void openChannel(ProjectorStructure shown, int group, String channel, ItemStack clicked) {
        ProjectorPlan working = ProjectorClient.plan.copy();
        if (group < 0) group = shown.groupOfChannel(channel);
        Choice choice = group < 0 ? null : shown.choices.get(group);
        TreeMap<Integer, ItemStack> tiers = choice == null ? ProjectorChannels.tiers(channel)
            : ProjectorChannels.tiers(choice);
        int current = working.channels.getOrDefault(channel, 0);
        Integer clickedTier = clicked == null ? null : tierOf(choice, clicked, channel);
        int preselected = clickedTier != null ? clickedTier : current;
        int need = group < 0 ? shown.countChannel(channel) : shown.countOf(group);
        Int2IntOpenHashMap have = ProjectorClient.inventory(Minecraft.getMinecraft().thePlayer);
        ModularPanel panel = panel(CHOICE_WIDTH + SCROLL, 0);
        List<Row> rows = new ArrayList<>();
        rows.add(
            Row.item(
                null,
                // #tr gtneverreset.projector.spot.default
                // # Default
                // # zh_CN 默认
                marked(StatCollector.translateToLocal(LANG + "default"), 0, current, preselected),
                null,
                preselected == 0,
                () -> {
                    working.setChannel(channel, 0);
                    confirm(panel);
                }));
        for (Map.Entry<Integer, ItemStack> tier : tiers.entrySet()) {
            int value = tier.getKey();
            String name = marked(ProjectorChannels.name(tier.getValue()), value, current, preselected);
            rows.add(countedRow(tier.getValue(), name, value == preselected, have, need, () -> {
                working.setChannel(channel, value);
                confirm(panel);
            }));
        }
        int listHeight = 2 + Math.min(VISIBLE, rows.size()) * ROW, inner = CHOICE_WIDTH + SCROLL;
        panel.size(PAD * 2 + inner, TOP + LINE + 4 + listHeight + 4 + LINE + PAD);
        title(
            panel,
            inner,
            ProjectorChannels.title(channel),
            // #tr gtneverreset.projector.spot.blocks
            // # %s blocks
            // # zh_CN 共 %s 块
            StatCollector.translateToLocalFormatted(LANG + "blocks", need));
        int top = TOP + LINE + 4;
        panel.child(
            new IconRowList(() -> rows).revealSelected()
                .pos(PAD, top)
                .size(inner, listHeight));
        // #tr gtneverreset.projector.spot.pick_hint
        // # Click a row to apply
        // # zh_CN 点一行就生效
        // #tr gtneverreset.projector.spot.pick_again_hint
        // # Click a row to apply · middle-click again: match this block
        // # zh_CN 点一行就生效 · 再按中键：照这块设置
        String hint = StatCollector.translateToLocal(LANG + (clickedTier != null ? "pick_again_hint" : "pick_hint"));
        panel.child(
            muted(hint).pos(PAD, top + listHeight + 5)
                .size(inner, LINE));
        Runnable quickPick = clickedTier == null ? null : () -> working.setChannel(channel, preselected);
        open(panel, working, quickPick, null, channelCells(shown, channel), GROUP_COLOR);
    }

    @Override
    public boolean onMousePressed(int mouseButton) {
        if (mouseButton - 100 == Minecraft.getMinecraft().gameSettings.keyBindPickBlock.getKeyCode()) {
            pickAgain();
            return true;
        }
        return super.onMousePressed(mouseButton);
    }

    @Override
    public boolean onKeyPressed(char typedChar, int keyCode) {
        int pick = Minecraft.getMinecraft().gameSettings.keyBindPickBlock.getKeyCode();
        if (pick > 0 && keyCode == pick) {
            pickAgain();
            return true;
        }
        return super.onKeyPressed(typedChar, keyCode);
    }

    @Override
    public void onClose() {
        super.onClose();
        ProjectorClient.focus = new int[0];
        if (hatches != null && hatches.configChanged()) ConfigRegistry.save(ClientConfig.class);
        if (confirmed && !working.writeToNBT()
            .equals(before)) ProjectorClient.send(location, working);
    }

    private void pickAgain() {
        if (quickPick != null) quickPick.run();
        confirmed = true;
        close();
    }

    private static void open(ModularPanel panel, ProjectorPlan working, Runnable quickPick, ProjectorHatchPage hatches,
        int[] focus, int focusColor) {
        ProjectorClient.focus = focus;
        ProjectorClient.focusColor = focusColor;
        ClientGUI.open(
            new ProjectorSpotScreen(panel, working, quickPick, hatches).useTheme(ScreenThemeConfig.projector.themeId));
    }

    private static ModularPanel panel(int inner, int body) {
        int width = PAD * 2 + inner;
        Minecraft mc = Minecraft.getMinecraft();
        int screen = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaledWidth();
        ModularPanel panel = new ModularPanel(NAME).size(width, TOP + LINE + 4 + body + PAD)
            .topRel(0.5f, 0, 0.5f);
        if (screen / 2 + CROSSHAIR_GAP + width + SCREEN_MARGIN <= screen) return panel.leftRel(0.5f, CROSSHAIR_GAP, 0f);
        return panel.left(Math.max(SCREEN_MARGIN, screen - width - SCREEN_MARGIN));
    }

    private static void confirm(ModularPanel panel) {
        if (!panel.isValid() || !(panel.getScreen() instanceof ProjectorSpotScreen screen)) return;
        screen.confirmed = true;
        screen.close();
    }

    private static void title(ModularPanel panel, int inner, String main, String detail) {
        panel.child(
            Flow.row()
                .pos(PAD, TOP)
                .size(inner, LINE)
                .childPadding(6)
                .child(
                    IKey.str(main)
                        .asWidget()
                        .alignment(Alignment.CenterLeft)
                        .height(LINE))
                .child(
                    muted(detail).alignment(Alignment.CenterLeft)
                        .expanded()
                        .height(LINE))
                .child(
                    // #tr gtneverreset.projector.spot.close
                    // # Esc cancels
                    // # zh_CN Esc 取消
                    muted(StatCollector.translateToLocal(LANG + "close")).alignment(Alignment.CenterRight)
                        .height(LINE)));
    }

    private static TextWidget<?> muted(String text) {
        Variant variant = AE2Theme.variant(ScreenThemeConfig.projector.themeId);
        return IKey.str(text)
            .asWidget()
            .color(variant == null ? PLAIN_MUTED : variant.colors.muted());
    }

    private static List<Row> kindRows(ProjectorHatchPage hatches) {
        List<Row> rows = new ArrayList<>();
        Object current = hatches.kind();
        for (Object kind : hatches.kinds()) {
            // #tr gtneverreset.projector.spot.casing
            // # Casing
            // # zh_CN 外壳
            String name = ProjectorHatchPage.CASING.equals(kind) ? StatCollector.translateToLocal(LANG + "casing")
                : hatches.kindName(kind);
            rows.add(
                Row.item(hatches.kindIcon(kind), name, null, kind.equals(current), () -> hatches.chooseKind(kind)));
        }
        return rows;
    }

    private static List<Row> modelRows(ProjectorHatchPage hatches) {
        List<Row> rows = new ArrayList<>();
        if (ProjectorHatchPage.CASING.equals(hatches.kind())) {
            String chosen = hatches.casingKey();
            for (ItemStack block : hatches.casings()) {
                String key = ProjectorPlan.blockKey(block);
                rows.add(
                    Row.item(
                        block,
                        ProjectorChannels.name(block),
                        null,
                        key.equals(chosen),
                        () -> hatches.chooseCasing(key)));
            }
            return rows;
        }
        Integer current = hatches.variant();
        for (int id : hatches.variants()) rows.add(
            Row.item(
                hatches.variantIcon(id),
                hatches.variantLabel(id),
                null,
                current != null && current == id,
                () -> hatches.chooseVariant(id)));
        return rows;
    }

    private static List<Row> amperesRows(ProjectorHatchPage hatches) {
        List<Integer> amperes = ProjectorHatchPage.CASING.equals(hatches.kind()) ? List.of() : hatches.amperes();
        if (amperes.size() < 2)
            // #tr gtneverreset.projector.spot.no_tier
            // # —
            // # zh_CN —
            return List.of(Row.item(null, StatCollector.translateToLocal(LANG + "no_tier"), null, false, null));
        List<Row> rows = new ArrayList<>();
        int held = hatches.held();
        for (int id : amperes) rows
            .add(Row.item(null, ProjectorHatchPage.amperesName(id), null, id == held, () -> hatches.chooseAmperes(id)));
        return rows;
    }

    private static List<Row> tierRows(ProjectorHatchPage hatches) {
        List<Integer> tiers = ProjectorHatchPage.CASING.equals(hatches.kind()) ? List.of() : hatches.tiers();
        if (tiers.size() < 2)
            return List.of(Row.item(null, StatCollector.translateToLocal(LANG + "no_tier"), null, false, null));
        List<Row> rows = new ArrayList<>();
        int held = hatches.held();
        for (int id : tiers)
            rows.add(Row.item(null, ProjectorHatchPage.tierName(id), null, id == held, () -> hatches.chooseTier(id)));
        return rows;
    }

    private static Row countedRow(ItemStack icon, String name, boolean selected, Int2IntOpenHashMap have, int need,
        Runnable click) {
        int owned = have.get(Choice.idOf(icon.getItem(), icon.getItemDamage()));
        return Row.item(icon, name, owned + "/" + need, selected, click)
            .withDetailColor(owned >= need ? IconRowList.GOOD : IconRowList.BAD);
    }

    private static Integer tierOf(Choice choice, ItemStack clicked, String channel) {
        int tier = choice == null ? 0 : choice.tierOf(clicked);
        return tier > 0 ? tier : ProjectorChannels.tierOf(clicked, channel);
    }

    private static String marked(String name, int value, int current, int preselected) {
        if (value != current || value == preselected) return name;
        // #tr gtneverreset.projector.spot.current
        // # %s · current
        // # zh_CN %s · 当前
        return StatCollector.translateToLocalFormatted(LANG + "current", name);
    }

    private static int[] cellsOf(ProjectorStructure shown, int group) {
        List<Integer> cells = new ArrayList<>();
        for (int cell = 0; cell < shown.size; cell++) {
            if (shown.choiceOf[cell] == group) cells.add(cell);
        }
        return cells.stream()
            .mapToInt(Integer::intValue)
            .toArray();
    }

    private static int[] channelCells(ProjectorStructure shown, String channel) {
        List<Integer> cells = new ArrayList<>();
        for (int cell = 0; cell < shown.size; cell++) {
            Choice choice = shown.choiceAt(cell);
            if (choice != null && channel.equals(choice.channel())) cells.add(cell);
        }
        return cells.stream()
            .mapToInt(Integer::intValue)
            .toArray();
    }
}
