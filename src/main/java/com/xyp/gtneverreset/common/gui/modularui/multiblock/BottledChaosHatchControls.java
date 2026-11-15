package com.xyp.gtneverreset.common.gui.modularui.multiblock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IGuiAction;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtneverreset.common.machine.hatch.ChaosCredentialHatch;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuis;

/** Both GUIs use the same action order, labels, limit editor and server-side hatch operations. */
public final class BottledChaosHatchControls {

    public enum Action {

        ROUTES,
        EXECUTION,
        LIMIT,
        EXPORT,
        CLEAR;

        public String label(boolean enabled) {
            return switch (this) {
                // #tr gtnr.machine.BottleInChaosRecipeTokenHatch.gui.take_button
                // # Select routes
                // # zh_CN 选择产线
                case ROUTES -> "gtnr.machine.BottleInChaosRecipeTokenHatch.gui.take_button";
                // #tr gtnr.machine.BottleInChaosRecipeTokenHatch.gui.pause_button
                // # Pause
                // # zh_CN 暂停
                // #tr gtnr.machine.BottleInChaosRecipeTokenHatch.gui.resume_button
                // # Resume
                // # zh_CN 恢复
                case EXECUTION -> "gtnr.machine.BottleInChaosRecipeTokenHatch.gui."
                    + (enabled ? "pause_button" : "resume_button");
                // #tr gtnr.machine.BottleInChaosRecipeTokenHatch.gui.cache_limit
                // # Cache limit
                // # zh_CN 缓存上限
                case LIMIT -> "gtnr.machine.BottleInChaosRecipeTokenHatch.gui.cache_limit";
                // #tr gtnr.machine.BottleInChaosRecipeTokenHatch.gui.export_button
                // # Export cache
                // # zh_CN 排出缓存
                case EXPORT -> "gtnr.machine.BottleInChaosRecipeTokenHatch.gui.export_button";
                // #tr gtnr.machine.BottleInChaosRecipeTokenHatch.gui.clear_button
                // # Clear
                // # zh_CN 清空
                case CLEAR -> "gtnr.machine.BottleInChaosRecipeTokenHatch.gui.clear_button";
            };
        }

        private String tooltip() {
            return switch (this) {
                // #tr gtnr.machine.BottleInChaos.gui.export_material_cache
                // # Export this hatch's cache to controller outputs. Blocked outputs preserve the cache.
                // # zh_CN 将本仓缓存排入主机输出. 输出空间不足时保留缓存.
                case EXPORT -> "gtnr.machine.BottleInChaos.gui.export_material_cache";
                // #tr gtnr.machine.BottleInChaosRecipeTokenHatch.gui.clear
                // # Clear this processing group's current job and cached materials.
                // # zh_CN 清空此加工组的当前运行状态和缓存材料.
                case CLEAR -> "gtnr.machine.BottleInChaosRecipeTokenHatch.gui.clear";
                default -> null;
            };
        }

        private IDrawable icon(boolean enabled) {
            return switch (this) {
                case ROUTES -> GTGuiTextures.OVERLAY_BUTTON_EXPORT;
                case EXECUTION -> enabled ? GTGuiTextures.OVERLAY_BUTTON_POWER_SWITCH_ON
                    : GTGuiTextures.OVERLAY_BUTTON_POWER_SWITCH_OFF;
                case LIMIT -> GTGuiTextures.OVERLAY_BUTTON_CYCLIC;
                case EXPORT -> GTGuiTextures.OVERLAY_BUTTON_EXPORT;
                case CLEAR -> GTGuiTextures.OVERLAY_BUTTON_CROSS;
            };
        }

        private void execute(ChaosCredentialHatch hatch, EntityPlayer player) {
            switch (this) {
                case ROUTES -> {}
                case EXPORT -> hatch.exportMaterialCache();
                case CLEAR -> hatch.clearRunningState();
                case EXECUTION, LIMIT -> {}
            }
        }
    }

    public static List<ButtonWidget<?>> create(ModularPanel parent, PanelSyncManager manager, String id,
        Supplier<ChaosCredentialHatch> target, BooleanSupplier enabled, IntSyncValue selection, IntSupplier labelColor,
        Action... actions) {
        BooleanSyncValue execution = new BooleanSyncValue(() -> {
            if (manager.isClient()) return enabled.getAsBoolean();
            ChaosCredentialHatch hatch = target.get();
            return hatch != null && hatch.isValid() && hatch.isExecutionEnabled();
        }, value -> {
            if (manager.isClient()) return;
            ChaosCredentialHatch hatch = target.get();
            if (hatch != null && hatch.isValid()) hatch.setExecutionEnabled(value);
        }) {

            @Override
            public void write(PacketBuffer buffer) {
                buffer.writeInt(selection == null ? 0 : selection.getIntValue());
                super.write(buffer);
            }

            @Override
            public void readOnClient(int packetId, PacketBuffer buffer) throws IOException {
                if (packetId == SYNC_VALUE && buffer.readInt() == (selection == null ? 0 : selection.getIntValue()))
                    super.readOnClient(packetId, buffer);
            }

            @Override
            public void readOnServer(int packetId, PacketBuffer buffer) throws IOException {
                if (packetId != SYNC_VALUE) return;
                if (buffer.readInt() == (selection == null ? 0 : selection.getIntValue()))
                    super.readOnServer(packetId, buffer);
                // Always acknowledge the actual result, including rejected or normalized requests.
                notifyUpdate();
            }
        }.allowC2S();
        manager.syncValue(id + "ExecutionState", execution);
        if (selection != null) selection.setChangeListener(() -> {
            if (manager.isClient()) execution.setBoolValue(enabled.getAsBoolean(), false, false);
            else execution.notifyUpdate();
        });
        IPanelHandler settings = manager.syncedPanel(id + "Limit", true, (sync, handler) -> {
            // Freeze the selected hatch when opening: later selection/removal cannot redirect an edit.
            ChaosCredentialHatch opened = target.get();
            return createLimitPanel(
                parent,
                () -> opened != null && opened.isValid() && target.get() == opened ? opened : null);
        });
        IPanelHandler routes = manager.syncedPanel(id + "Routes", true, (sync, handler) -> {
            ChaosCredentialHatch opened = target.get();
            return createRoutesPanel(
                parent,
                sync,
                () -> opened != null && opened.isValid() && target.get() == opened ? opened : null);
        });
        List<ButtonWidget<?>> buttons = new ArrayList<>();
        for (Action action : actions) {
            InteractionSyncHandler interaction = new InteractionSyncHandler().setOnMouseReleased(mouse -> {
                if (mouse.mouseButton != 0 || mouse.isClient()) return;
                ChaosCredentialHatch hatch = target.get();
                if (hatch != null && hatch.isValid()) action.execute(hatch, manager.getPlayer());
            });
            manager.syncValue(id + action.name(), interaction);
            buttons.add(
                new ActionButton().name(id + action.name())
                    .size(18, 18)
                    .syncHandler(interaction)
                    .onMouseReleased(mouse -> {
                        if (mouse != 0) return false;
                        if (action == Action.EXECUTION) {
                            execution.setBoolValue(!execution.getBoolValue(), false, true);
                            return true;
                        }
                        if (action == Action.ROUTES) {
                            if (routes.isPanelOpen()) routes.closePanel();
                            else {
                                routes.deleteCachedPanel();
                                routes.openPanel();
                            }
                            return true;
                        }
                        if (action != Action.LIMIT) return interaction.onMouseReleased(mouse);
                        if (settings.isPanelOpen()) settings.closePanel();
                        else {
                            // MUI caches closed panels; rebuild this slot-free editor for the newly selected hatch.
                            settings.deleteCachedPanel();
                            settings.openPanel();
                        }
                        return true;
                    })
                    .playClickSound(true)
                    .background(
                        new DynamicDrawable(
                            () -> action == Action.EXECUTION && !execution.getBoolValue()
                                ? GTGuiTextures.BUTTON_STANDARD_PRESSED
                                : GTGuiTextures.BUTTON_STANDARD))
                    .overlay(
                        labelColor == null ? new DynamicDrawable(() -> action.icon(execution.getBoolValue()))
                            : IKey.dynamic(() -> StatCollector.translateToLocal(action.label(execution.getBoolValue())))
                                .color(labelColor))
                    .tooltipBuilder(tooltip -> {
                        String key = action.tooltip();
                        if (key != null) tooltip.addLine(IKey.lang(key));
                        else if (labelColor == null) tooltip.addLine(IKey.lang(action.label(execution.getBoolValue())));
                    })
                    .tooltipAutoUpdate(true));
        }
        return buttons;
    }

    private static final class ActionButton extends ButtonWidget<ActionButton> {

        private boolean pressed;

        private ActionButton() {
            // MUI may not dispatch releases outside this button. A new press starts a new gesture.
            listenGuiAction((IGuiAction.MousePressed) mouseButton -> {
                if (mouseButton == 0) pressed = false;
                return false;
            });
            listenGuiAction((IGuiAction.MouseReleased) mouseButton -> {
                if (mouseButton == 0 && !isHovering()) pressed = false;
                return false;
            });
        }

        @Override
        public Result onMousePressed(int mouseButton) {
            if (mouseButton != 0) return Result.IGNORE;
            pressed = true;
            playClickSound();
            return Result.SUCCESS;
        }

        @Override
        public boolean onMouseRelease(int mouseButton) {
            if (mouseButton != 0 || !pressed) return false;
            pressed = false;
            return isHovering() && super.onMouseRelease(mouseButton);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IDrawable getCurrentBackground(WidgetThemeEntry<?> theme) {
            // Keep the pressed frame until the same release event updates the button's value.
            return pressed && isHovering() ? GTGuiTextures.BUTTON_STANDARD_PRESSED : super.getCurrentBackground(theme);
        }
    }

    private static ModularPanel createRoutesPanel(ModularPanel parent, PanelSyncManager sync,
        Supplier<ChaosCredentialHatch> target) {
        LongSyncValue mask = new LongSyncValue(
            () -> target.get() == null ? 0
                : target.get()
                    .getSelectedRoutes(),
            value -> {
                if (target.get() != null) target.get()
                    .setSelectedRoutes(value);
            });
        sync.syncValue("selectedRoutes", mask);
        var panel = GTGuis.createPopUpPanel("chaosRoutes")
            .relative(parent)
            .leftRel(0.5f)
            .topRel(0.5f)
            .size(286, 262);
        panel.child(
            // #tr gtnr.chaos.routes.unlocked
            // # All preset routes unlocked — no credential required
            // # zh_CN 全部预设已解锁，无需凭证
            IKey.lang("gtnr.chaos.routes.unlocked")
                .asWidget()
                .pos(8, 18)
                .size(270, 18));
        com.cleanroommc.modularui.widgets.ListWidget<com.cleanroommc.modularui.api.widget.IWidget, ?> list = new com.cleanroommc.modularui.widgets.ListWidget<>()
            .pos(8, 58)
            .size(270, 194);
        panel.child(
            IKey.dynamic(
                () -> StatCollector.translateToLocalFormatted(
                    // #tr gtnr.chaos.routes.weight
                    // # Selected route capacity: %s / 1024
                    // # zh_CN 已选路线容量：%s / 1024
                    "gtnr.chaos.routes.weight",
                    com.xyp.gtneverreset.common.item.items.ChaosRouteItem.getRequiredWeight(mask.getLongValue())))
                .asWidget()
                .pos(8, 36)
                .size(270, 16));
        for (int index : com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CredentialModel
            .displayOrder()) {
            long bit = 1L << index;
            String key = com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CredentialModel
                .translationKey(index);
            list.child(
                new ButtonWidget<>().size(258, 20)
                    .setEnabledIf(
                        widget -> com.xyp.gtneverreset.common.item.items.ChaosRouteItem
                            .getRequiredWeight(mask.getLongValue() ^ bit) <= 1024)
                    .overlay(
                        IKey.dynamic(
                            () -> ((mask.getLongValue() & bit) != 0 ? "[+] " : "[ ] ")
                                + StatCollector.translateToLocal(key)))
                    .syncHandler(new InteractionSyncHandler().setOnMousePressed(mouse -> {
                        if (!mouse.isClient() && mouse.mouseButton == 0 && target.get() != null) {
                            target.get()
                                .setSelectedRoutes(
                                    target.get()
                                        .getSelectedRoutes() ^ bit);
                            mask.notifyUpdate();
                        }
                    })));
        }
        return panel.child(list);
    }

    private static ModularPanel createLimitPanel(ModularPanel parent, Supplier<ChaosCredentialHatch> target) {
        LongSyncValue limit = new LongSyncValue(
            () -> target.get() == null ? 0
                : target.get()
                    .getMaterialCache()
                    .amountLimit(),
            value -> {
                if (target.get() != null) target.get()
                    .setCacheLimit(value);
            }).allowC2S();
        return GTGuis.createPopUpPanel("bottledChaosCacheSettings")
            .relative(parent)
            .leftRel(0.5f)
            .topRel(0.5f)
            .size(164, 78)
            .child(
                IKey.lang("gtnr.machine.BottleInChaosRecipeTokenHatch.gui.cache_limit")
                    .asWidget()
                    .pos(8, 18)
                    .size(148, 14))
            .child(new TextFieldWidget() {

                @Override
                protected void onTextChanged() {
                    String text = getText();
                    if ("int".equalsIgnoreCase(text) || "long".equalsIgnoreCase(text)) {
                        setText(Long.toString("int".equalsIgnoreCase(text) ? Integer.MAX_VALUE : Long.MAX_VALUE));
                        handler.setCursor(0, getText().length(), false);
                    }
                    super.onTextChanged();
                }
            }.value(limit)
                .setPattern(Pattern.compile("[0-9]*|[a-zA-Z]*"))
                .setMaxLength(19)
                .setValidator(text -> {
                    try {
                        return Long.toString(Math.max(0, Long.parseLong(text)));
                    } catch (NumberFormatException invalid) {
                        return Long.toString(limit.getLongValue());
                    }
                })
                .setTextAlignment(Alignment.Center)
                .tooltipBuilder(
                    tooltip -> tooltip.addLine(
                        IKey.lang(
                            // #tr gtnr.machine.BottleInChaosRecipeTokenHatch.gui.cache_limit.range
                            // # Range: %s - %s
                            // # zh_CN 范围: %s - %s
                            "gtnr.machine.BottleInChaosRecipeTokenHatch.gui.cache_limit.range",
                            0,
                            Long.MAX_VALUE))
                        // #tr gtnr.machine.BottleInChaosRecipeTokenHatch.gui.cache_limit.info
                        // # Each item or fluid has this limit. Excess is sent to normal outputs.
                        // # zh_CN 每种物品或流体分别使用此上限, 超出部分正常输出.
                        .addLine(IKey.lang("gtnr.machine.BottleInChaosRecipeTokenHatch.gui.cache_limit.info")))
                .pos(8, 40)
                .size(148, 18));
    }
}
