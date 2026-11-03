package com.xyp.gtneverreset.common.gui;

import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.PageButton;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtneverreset.common.item.items.ItemUniversalWand;
import com.xyp.gtneverreset.common.packet.UniversalWandNetwork;
import com.xyp.gtneverreset.common.wand.UniversalWandAccelerationHandler;
import com.xyp.gtneverreset.common.wand.UniversalWandFeature;
import com.xyp.gtneverreset.config.ScreenThemeConfig;
import com.xyp.gtneverreset.config.UniversalWandConfig;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.ScreenTheme;
import com.xyp.gtnrlib.gui.widget.SlideSwitch;

/** Item settings always refer to the exact stack that opened this screen. */
public final class UniversalWandGui {

    private UniversalWandGui() {}

    public static ModularPanel build(GuiData data, PanelSyncManager syncManager) {
        EntityPlayer player = data.getPlayer();
        ItemStack expected = data instanceof PlayerInventoryGuiData inventoryData ? inventoryData.getUsedItemStack()
            : player.getHeldItem();
        BooleanSyncValue fortune = new BooleanSyncValue(
            () -> ItemUniversalWand.getEnchantMode(expected) == ItemUniversalWand.EnchantMode.FORTUNE,
            value -> ItemUniversalWand.setEnchantMode(
                player,
                expected,
                value ? ItemUniversalWand.EnchantMode.FORTUNE : ItemUniversalWand.EnchantMode.SILK)).allowC2S();
        BooleanSyncValue protection = new BooleanSyncValue(
            () -> ItemUniversalWand.isProtectionMode(expected),
            value -> ItemUniversalWand.setProtectionMode(player, expected, value)).allowC2S();
        BooleanSyncValue bedrock = new BooleanSyncValue(
            () -> ItemUniversalWand.isForceBedrock(expected),
            value -> ItemUniversalWand.setForceBedrock(player, expected, value)).allowC2S();
        BooleanSyncValue enhanced = new BooleanSyncValue(
            () -> ItemUniversalWand.isEnhancedChain(expected),
            value -> ItemUniversalWand.setEnhancedChain(player, expected, value)).allowC2S();
        BooleanSyncValue small = new BooleanSyncValue(
            () -> ItemUniversalWand.isGtSmallChainEnabled(expected),
            value -> ItemUniversalWand.setGtSmallChainEnabled(player, expected, value)).allowC2S();
        IntSyncValue cooldown = new IntSyncValue(
            () -> ItemUniversalWand.getBreakCooldown(expected),
            value -> ItemUniversalWand.setBreakCooldown(player, expected, value)).allowC2S();
        IntSyncValue radius = new IntSyncValue(
            () -> ItemUniversalWand.getEnhancedChainRadius(expected),
            value -> ItemUniversalWand.setEnhancedChainRadius(player, expected, value)).allowC2S();
        IntSyncValue max = new IntSyncValue(
            () -> ItemUniversalWand.getMaxMiningCount(expected),
            value -> ItemUniversalWand.setMaxMiningCount(player, expected, value)).allowC2S();
        IntSyncValue toolMode = new IntSyncValue(
            () -> ItemUniversalWand.getGtToolMode(expected)
                .ordinal());
        syncManager.syncValue("wand_tool_mode", 0, toolMode);

        BooleanSyncValue ae = new BooleanSyncValue(
            () -> ItemUniversalWand.isAePriority(expected),
            value -> ItemUniversalWand.setAePriority(player, expected, value)).allowC2S();
        BooleanSyncValue inventory = new BooleanSyncValue(
            () -> ItemUniversalWand.isInventoryCollection(expected),
            value -> ItemUniversalWand.setInventoryCollection(player, expected, value)).allowC2S();
        BooleanSyncValue shears = new BooleanSyncValue(
            () -> ItemUniversalWand.isShearsEnabled(expected),
            value -> ItemUniversalWand.setShearsEnabled(player, expected, value)).allowC2S();
        BooleanSyncValue hoe = new BooleanSyncValue(
            () -> ItemUniversalWand.isHoeEnabled(expected),
            value -> ItemUniversalWand.setHoeEnabled(player, expected, value)).allowC2S();
        BooleanSyncValue crops = new BooleanSyncValue(
            () -> ItemUniversalWand.isCropHarvestEnabled(expected),
            value -> ItemUniversalWand.setCropHarvestEnabled(player, expected, value)).allowC2S();
        BooleanSyncValue cropSticks = new BooleanSyncValue(
            () -> ItemUniversalWand.isCropSticksEnabled(expected),
            value -> ItemUniversalWand.setCropSticksEnabled(player, expected, value)).allowC2S();
        BooleanSyncValue forceKill = new BooleanSyncValue(
            () -> ItemUniversalWand.isForceKillEnabled(expected),
            value -> ItemUniversalWand.setForceKillEnabled(player, expected, value)).allowC2S();
        BooleanSyncValue beheading = new BooleanSyncValue(
            () -> ItemUniversalWand.isBeheadingEnabled(expected),
            value -> ItemUniversalWand.setBeheadingEnabled(player, expected, value)).allowC2S();
        BooleanSyncValue spawnEgg = new BooleanSyncValue(
            () -> ItemUniversalWand.isSpawnEggEnabled(expected),
            value -> ItemUniversalWand.setSpawnEggEnabled(player, expected, value)).allowC2S();
        BooleanSyncValue teleport = new BooleanSyncValue(
            () -> ItemUniversalWand.isTeleportEnabled(expected),
            value -> ItemUniversalWand.setTeleportEnabled(player, expected, value)).allowC2S();
        BooleanSyncValue playerProtection = new BooleanSyncValue(
            () -> ItemUniversalWand.isPlayerProtectionEnabled(expected),
            value -> ItemUniversalWand.setPlayerProtectionEnabled(player, expected, value)).allowC2S();
        BooleanSyncValue noAggro = new BooleanSyncValue(
            () -> ItemUniversalWand.isNoAggroEnabled(expected),
            value -> ItemUniversalWand.setNoAggroEnabled(player, expected, value)).allowC2S();
        BooleanSyncValue flightOverride = new BooleanSyncValue(
            () -> ItemUniversalWand.isFlightSpeedOverrideEnabled(expected),
            value -> ItemUniversalWand.setFlightSpeedOverrideEnabled(player, expected, value)).allowC2S();
        IntSyncValue aoeSize = new IntSyncValue(
            () -> (ItemUniversalWand.getAoeSize(expected) - 1) / 2,
            value -> ItemUniversalWand.setAoeSize(
                player,
                expected,
                Math.max(0, Math.min((ItemUniversalWand.MAX_AOE_SIZE - 1) / 2, value)) * 2 + 1)).allowC2S();
        BooleanSyncValue acceleration = new BooleanSyncValue(
            () -> ItemUniversalWand.isTimeAccelerationEnabled(expected),
            value -> ItemUniversalWand.setTimeAccelerationEnabled(player, expected, value)).allowC2S();
        syncManager.syncValue("wand_fortune", 0, fortune);
        syncManager.syncValue("wand_protection", 0, protection);
        syncManager.syncValue("wand_force_bedrock", 0, bedrock);
        syncManager.syncValue("wand_enhanced_chain", 0, enhanced);
        syncManager.syncValue("wand_gt_small_chain", 0, small);
        syncManager.syncValue("wand_break_cooldown", 0, cooldown);
        syncManager.syncValue("wand_enhanced_radius", 0, radius);
        syncManager.syncValue("wand_max_mining_count", 0, max);
        syncManager.syncValue("wand_collect_items", 0, inventory);
        syncManager.syncValue("wand_ae_priority", 0, ae);
        syncManager.syncValue("wand_shears", 0, shears);
        syncManager.syncValue("wand_hoe", 0, hoe);
        syncManager.syncValue("wand_crop_harvest", 0, crops);
        syncManager.syncValue("wand_crop_sticks", 0, cropSticks);
        syncManager.syncValue("wand_force_kill", 0, forceKill);
        syncManager.syncValue("wand_beheading", 0, beheading);
        syncManager.syncValue("wand_spawn_egg", 0, spawnEgg);
        syncManager.syncValue("wand_teleport", 0, teleport);
        syncManager.syncValue("wand_player_protection", 0, playerProtection);
        syncManager.syncValue("wand_no_aggro", 0, noAggro);
        syncManager.syncValue("wand_flight_speed_override", 0, flightOverride);
        syncManager.syncValue("wand_time_acceleration", 0, acceleration);
        StringSyncValue accelerationRates = new StringSyncValue(
            () -> Arrays.stream(UniversalWandConfig.accelerationRates())
                .mapToObj(Integer::toString)
                .collect(Collectors.joining(",")));
        syncManager.syncValue("wand_acceleration_rates", 0, accelerationRates);
        IPanelHandler accelerationSettings = syncManager.syncedPanel(
            "gtneverreset_wand_acceleration_config",
            true,
            (manager, handler) -> accelerationPanel(player, expected, manager, accelerationRates.getStringValue()));
        syncManager.syncValue("wand_aoe_size", 0, aoeSize);
        // #tr gtneverreset.gui.universal_wand.section.tools
        // # Tool settings
        // # zh_CN 工具设置
        // #tr gtneverreset.gui.universal_wand.tab.tools
        // # Tools
        // # zh_CN 工具
        ParentWidget<?> tools = section("tools", 0, 84)
            // #tr gtneverreset.gui.universal_wand.harvest_mode
            // # Harvest mode
            // # zh_CN 采集模式
            .child(
                IKey.lang("gtneverreset.gui.universal_wand.harvest_mode")
                    .asWidget()
                    .pos(6, 25))
            // #tr gtneverreset.gui.universal_wand.silk
            // # Silk Touch
            // # zh_CN 精准采集
            .child(
                radioOption(syncManager, "silk", fortune, false).pos(80, 22)
                    .size(74, 16))
            // #tr gtneverreset.gui.universal_wand.fortune
            // # Fortune
            // # zh_CN 时运
            .child(
                radioOption(syncManager, "fortune", fortune, true).pos(158, 22)
                    .size(70, 16))
            .child(
                // #tr gtneverreset.gui.universal_wand.protection
                // # Protection mode
                // # zh_CN 保护模式
                // #tr gtneverreset.gui.universal_wand.protection.tooltip
                // # Prevent accidental mining and pause automatic harvesting.
                // # zh_CN 防止误挖，暂停自动收菜。
                stateSwitch("protection", protection).pos(6, 42)
                    .size(222, 16))
            // #tr gtneverreset.gui.universal_wand.cooldown
            // # Break cooldown (ticks)
            // # zh_CN 破坏冷却（tick）
            // #tr gtneverreset.gui.universal_wand.cooldown.tooltip
            // # Mining cooldown after breaking a block: 0–20 ticks. Zero adds no cooldown.
            // # zh_CN 破坏方块后的挖掘冷却，范围 0–20 tick；0 表示不添加冷却。
            .child(numberRow("cooldown", cooldown, 0, 20, 62, false));
        // #tr gtneverreset.gui.universal_wand.section.combat
        // # Combat
        // # zh_CN 战斗
        // #tr gtneverreset.gui.universal_wand.tab.combat
        // # Abilities
        // # zh_CN 战斗 / 能力
        ParentWidget<?> combat = section("combat", 0, 84)
            // #tr gtneverreset.gui.universal_wand.force_kill
            // # Force kill
            // # zh_CN 强制击杀
            // #tr gtneverreset.gui.universal_wand.force_kill.tooltip
            // # Attempt to kill the target directly, excluding players and known allies.
            // # zh_CN 尝试直接击杀目标，排除玩家与已识别友方。
            .child(stateSwitch("force_kill", forceKill).pos(6, 22))
            // #tr gtneverreset.gui.universal_wand.beheading
            // # Beheading
            // # zh_CN 斩首
            // #tr gtneverreset.gui.universal_wand.beheading.tooltip
            // # Get an extra skull from vanilla or Special Mobs skeletons, wither skeletons, zombies and creepers.
            // # zh_CN 击杀原版及 Special Mobs 的骷髅、凋灵骷髅、僵尸或苦力怕，额外获得头颅。
            .child(stateSwitch("beheading", beheading).pos(6, 42))
            // #tr gtneverreset.gui.universal_wand.spawn_egg
            // # Spawn Egg
            // # zh_CN 生物捕捉
            // #tr gtneverreset.gui.universal_wand.spawn_egg.tooltip
            // # Get a spawn egg from supported mobs on kill. Original mob data is not retained.
            // # zh_CN 击杀支持的生物时获得刷怪蛋，不保留原生物数据。
            .child(stateSwitch("spawn_egg", spawnEgg).pos(119, 42))
            // #tr gtneverreset.gui.universal_wand.aoe_size
            // # Attack area
            // # zh_CN 范围攻击大小
            // #tr gtneverreset.gui.universal_wand.aoe_size.tooltip
            // # Attack area: 1x1 to 27x27, extending 3 blocks above and below. 1x1 disables area attacks.
            // # zh_CN 攻击范围 1×1–27×27，高度上下各 3 格；1×1 关闭范围攻击。
            .child(numberRow("aoe_size", aoeSize, 0, 13, 62, true));
        // #tr gtneverreset.gui.universal_wand.section.auxiliary
        // # Player abilities
        // # zh_CN 玩家能力
        ParentWidget<?> auxiliary = section("auxiliary", 88, 84)
            // #tr gtneverreset.gui.universal_wand.player_protection
            // # Player protection
            // # zh_CN 玩家保护
            // #tr gtneverreset.gui.universal_wand.player_protection.tooltip
            // # While carried, prevent damage and death and clear harmful effects.
            // # zh_CN 携带时免疫伤害与死亡，并清除负面药效。
            .child(stateSwitch("player_protection", playerProtection).pos(6, 22))
            // #tr gtneverreset.gui.universal_wand.no_aggro
            // # Advanced stealth
            // # zh_CN 高级隐身
            // #tr gtneverreset.gui.universal_wand.no_aggro.tooltip
            // # While carried, hide from view and mobs; no natural spawning or vanilla spawner activation. Interaction
            // stays available.
            // # zh_CN 携带时隐身、免仇恨，不触发自然刷怪与原版刷怪笼；仍可交互。
            .child(stateSwitch("no_aggro", noAggro).pos(119, 22))
            // #tr gtneverreset.gui.universal_wand.teleport
            // # Teleport
            // # zh_CN 传送
            // #tr gtneverreset.gui.universal_wand.teleport.tooltip
            // # Use Ender IO Staff of Teleportation controls. Other successful right-click actions prevent teleport.
            // # zh_CN 按 Ender IO 传送权杖的操作传送；其他右键操作生效时不传送。
            .child(stateSwitch("teleport", teleport).pos(6, 42))
            // #tr gtneverreset.gui.universal_wand.flight_speed_override
            // # Flight speed override
            // # zh_CN 飞行速度覆盖
            // #tr gtneverreset.gui.universal_wand.flight_speed_override.tooltip
            // # When enabled, prioritize wand flight speed, suppress extra equipment thrust horizontally, and slow
            // faster after releasing movement keys. When disabled, allow equipment boosts with no extra wand slowing.
            // Enabled by default.
            // # zh_CN 开启时优先使用工具飞行速度、抑制装备额外水平推力，松开按键后加快减速；关闭时保留装备加速，停止工具的额外减速。默认开启。
            .child(stateSwitch("flight_speed_override", flightOverride).pos(119, 42))
            .child(
                new ButtonWidget<>().name("acceleration")
                    .overlay(
                        IKey.dynamic(
                            // #tr gtneverreset.gui.universal_wand.time_acceleration.tooltip
                            // # While enabled, sneak-right-click the target to apply the selected acceleration settings
                            // # zh_CN 开启后手持工具 Shift+右键目标，应用当前加速配置
                            () -> (controlAllowed("time_acceleration") ? "" : "§7")
                                // #tr gtneverreset.gui.universal_wand.time_acceleration
                                // # Time acceleration
                                // # zh_CN 时间加速
                                + StatCollector.translateToLocal("gtneverreset.gui.universal_wand.time_acceleration"))
                            .scale(1.0F))
                    .onMousePressed(button -> {
                        if (button != 0 || !UniversalWandFeature.ACCELERATION.allowed()) return false;
                        accelerationSettings.openPanel();
                        return true;
                    })
                    .tooltipDynamic(tooltip -> featureTooltip(tooltip, "time_acceleration"))
                    .pos(6, 62)
                    .size(222, 16));
        // #tr gtneverreset.gui.universal_wand.section.mining
        // # Mining
        // # zh_CN 采矿
        // #tr gtneverreset.gui.universal_wand.tab.mining
        // # Mining
        // # zh_CN 采矿
        ParentWidget<?> mining = section("mining", 0, 164)
            // #tr gtneverreset.gui.universal_wand.enhanced_chain
            // # Enhanced chain
            // # zh_CN 增强连锁
            // #tr gtneverreset.gui.universal_wand.enhanced_chain.tooltip
            // # Expand the chain search area with adjustable radius and block limit.
            // # zh_CN 扩大连锁搜索范围，半径与数量可调。
            .child(stateSwitch("enhanced_chain", enhanced).pos(6, 22))
            // #tr gtneverreset.gui.universal_wand.gt_small_chain
            // # Chain GT small ores
            // # zh_CN 连锁贫瘠矿
            // #tr gtneverreset.gui.universal_wand.gt_small_chain.tooltip
            // # Chain matching GT large ores together with their small ore variants.
            // # zh_CN GT 大矿石与对应的小矿石（贫瘠矿）混合连锁。
            .child(stateSwitch("gt_small_chain", small).pos(119, 22))
            // #tr gtneverreset.gui.universal_wand.crop_harvest
            // # Automatic crop harvesting
            // # zh_CN 全自动收菜
            // #tr gtneverreset.gui.universal_wand.crop_harvest.tooltip
            // # While held, harvest mature crops in a 16x16x3 area every second. Pauses in protection mode.
            // # zh_CN 手持时每秒自动收获周围 16×16×3 范围的成熟作物；保护模式下暂停。
            .child(stateSwitch("crop_harvest", crops).pos(6, 42))
            // #tr gtneverreset.gui.universal_wand.crop_sticks
            // # Crop-stick support
            // # zh_CN 支持作物架
            // #tr gtneverreset.gui.universal_wand.crop_sticks.tooltip
            // # Include Crops NH crops in automatic harvesting, keeping crops and sticks intact.
            // # zh_CN 自动收菜时也收获 Crops NH 作物架，保留作物与架子。
            .child(stateSwitch("crop_sticks", cropSticks).pos(119, 42))
            // #tr gtneverreset.gui.universal_wand.radius
            // # Enhanced radius
            // # zh_CN 增强半径
            // #tr gtneverreset.gui.universal_wand.radius.tooltip
            // # Enhanced chain-mining search radius: 1–128.
            // # zh_CN 增强连锁的搜索半径，范围 1–128。
            .child(numberRow("radius", radius, 1, 128, 62, false))
            .child(
                new TextWidget<>(
                    // #tr gtneverreset.gui.universal_wand.max_count
                    // # Max blocks
                    // # zh_CN 最大挖掘方块
                    IKey.lang("gtneverreset.gui.universal_wand.max_count")
                        .scale(1.0F)).pos(6, 85)
                            .size(105, 16)
                            .maxWidth(105))
            .child(
                new TextFieldWidget().formatAsInteger(true)
                    .setEnabledIf(widget -> UniversalWandFeature.CHAIN.allowed())
                    .numbersInt(1, Integer.MAX_VALUE)
                    .setMaxLength(10)
                    .value(max)
                    .pos(119, 82)
                    // #tr gtneverreset.gui.universal_wand.max_count.tooltip
                    // # Maximum blocks processed by one chain-mining operation. Minimum: 1.
                    // # zh_CN 一次连锁挖掘最多处理的方块数，最小为 1。
                    .addTooltipLine(StatCollector.translateToLocal("gtneverreset.gui.universal_wand.max_count.tooltip"))
                    .size(109, 16))
            .child(
                new ButtonWidget<>().setEnabledIf(widget -> !UniversalWandFeature.CHAIN.allowed())
                    .overlay(IKey.dynamic(() -> "§7" + max.getIntValue()))
                    .onMousePressed(button -> true)
                    .tooltipDynamic(tooltip -> featureTooltip(tooltip, "max_count"))
                    .pos(119, 82)
                    .size(109, 16))
            .child(
                // #tr gtneverreset.gui.universal_wand.force_bedrock
                // # Force bedrock mining
                // # zh_CN 强制挖掘基岩
                // #tr gtneverreset.gui.universal_wand.force_bedrock.tooltip
                // # Allow R to remove the targeted vanilla bedrock.
                // # zh_CN 允许按 R 移除准星指向的原版基岩。
                stateSwitch("force_bedrock", bedrock).pos(6, 102)
                    .size(222, 16))
            // #tr gtneverreset.gui.universal_wand.collect_items
            // # Item collection
            // # zh_CN 物品收集
            // #tr gtneverreset.gui.universal_wand.collect_items.tooltip
            // # Collect harvest and kill drops into your inventory; drop any overflow.
            // # zh_CN 采集与击杀掉落优先收入背包，满时掉落。
            .child(stateSwitch("collect_items", inventory).pos(6, 122))
            // #tr gtneverreset.gui.universal_wand.ae_priority
            // # Store drops in AE first
            // # zh_CN AE优先存入
            // #tr gtneverreset.gui.universal_wand.ae_priority.tooltip
            // # Store drops in bound AE first, then your inventory. Enables item collection.
            // # zh_CN 掉落优先存入已绑定 AE，剩余收入背包；自动开启物品收集。
            .child(stateSwitch("ae_priority", ae).pos(119, 122))
            // #tr gtneverreset.gui.universal_wand.gt_mode.tooltip
            // # Automatic uses the cutter on GT cables and AE parts, and the wrench on machines. Click to override.
            // # zh_CN 自动模式对 GT 电缆和 AE 部件使用剪线钳，对机器使用扳手。点击可切换模式。
            .child(actionButton("gt_mode", () -> {
                var modes = ItemUniversalWand.GtToolMode.values();
                int current = ItemUniversalWand.getGtToolMode(expected)
                    .ordinal();
                ItemUniversalWand.setGtToolMode(player, expected, modes[(current + 1) % modes.length]);
            }).overlay(
                IKey.dynamic(
                    () -> StatCollector.translateToLocalFormatted(
                        "gtneverreset.item.universal_wand.gt_mode",
                        StatCollector.translateToLocal(
                            "gtneverreset.item.universal_wand.gt_mode."
                                + ItemUniversalWand.GtToolMode.values()[toolMode.getIntValue()].name()))))
                .tooltipDynamic(tooltip -> featureTooltip(tooltip, "gt_mode"))
                .pos(6, 142)
                .size(222, 16));
        // #tr gtneverreset.gui.universal_wand.shears
        // # Shears
        // # zh_CN 剪刀
        // #tr gtneverreset.gui.universal_wand.shears.tooltip
        // # Shear entities and shearable blocks; drops follow the item collection settings.
        // # zh_CN 剪切生物和可剪切方块；产物沿用物品收集设置。
        // #tr gtneverreset.gui.universal_wand.section.vanilla
        // # Vanilla tools
        // # zh_CN 原版工具
        ParentWidget<?> vanilla = section("vanilla", 88, 44).child(stateSwitch("shears", shears).pos(6, 22))
            // #tr gtneverreset.gui.universal_wand.hoe
            // # Hoe
            // # zh_CN 锄头
            // #tr gtneverreset.gui.universal_wand.hoe.tooltip
            // # Till soil with the vanilla hoe.
            // # zh_CN 使用原版锄头耕地。
            .child(stateSwitch("hoe", hoe).pos(119, 22));
        PagedWidget.Controller tabs = new PagedWidget.Controller();
        PagedWidget<?> pages = new PagedWidget<>().controller(tabs)
            .pos(6, 40)
            .size(234, 172);
        pages.addPage(
            new ParentWidget<>().size(234, 172)
                .child(tools)
                .child(vanilla));
        pages.addPage(mining);
        pages.addPage(
            new ParentWidget<>().size(234, 172)
                .child(combat)
                .child(auxiliary));
        ModularPanel panel = ModularPanel.defaultPanel("gtneverreset_universal_wand", 246, 216)
            // #tr gtneverreset.gui.universal_wand.title
            // # Universal Wand
            // # zh_CN 无用之杖
            .child(
                IKey.lang("gtneverreset.gui.universal_wand.title")
                    .asWidget()
                    .pos(8, 4)
                    .height(12))
            .child(pages);
        boolean aeTabs = data.isClient() && ScreenThemeConfig.universalWand != ScreenTheme.DEFAULT;
        panel.child(tab(tabs, 0, "tools", aeTabs));
        panel.child(tab(tabs, 1, "mining", aeTabs));
        panel.child(tab(tabs, 2, "combat", aeTabs));
        if (data.isClient()) AE2Theme.sliderHandles(panel, ScreenThemeConfig.universalWand.themeId);
        return panel;
    }

    private static PageButton tab(PagedWidget.Controller tabs, int index, String name, boolean aeTabs) {
        IKey label = IKey.lang("gtneverreset.gui.universal_wand.tab." + name);
        PageButton button = new PageButton(index, tabs).pos(10 + index * 77, 18)
            .size(72, 18);
        if (aeTabs) {
            button.background(AE2Theme.button(button, () -> true, button::isActive))
                .overlay(AE2Theme.label(button, label::get, () -> true, button::isActive));
        } else button.overlay(label);
        return button;
    }

    private static ParentWidget<?> section(String title, int y, int height) {
        return new ParentWidget<>().pos(0, y)
            .size(234, height)
            .widgetTheme(IThemeApi.PANEL)
            .child(
                new TextWidget<>(
                    IKey.lang("gtneverreset.gui.universal_wand.section." + title)
                        .scale(1.0F)).pos(6, 5)
                            .size(222, 14));
    }

    private static SlideSwitch stateSwitch(String key, BooleanSyncValue value) {
        return new SlideSwitch().name(key)
            .value(value::getBoolValue)
            .label(IKey.dynamic(() -> {
                String label = StatCollector.translateToLocal("gtneverreset.gui.universal_wand." + key);
                return ScreenThemeConfig.universalWand == ScreenTheme.DEFAULT && controlAllowed(key)
                    ? EnumChatFormatting.DARK_GRAY + label
                    : label;
            }))
            .availableWhen(() -> controlAllowed(key))
            .onMousePressed(button -> {
                if (button != 0 || !controlAllowed(key)) return false;
                value.setBoolValue(!value.getBoolValue(), true, true);
                return true;
            })
            .tooltipDynamic(tooltip -> featureTooltip(tooltip, key))
            .size(109, 16);
    }

    private static ButtonWidget<?> radioOption(PanelSyncManager manager, String key, BooleanSyncValue value,
        boolean choice) {
        ButtonWidget<?> option = new ButtonWidget<>().name(key)
            .onMousePressed(button -> {
                if (button != 0 || !controlAllowed(key)) return false;
                if (value.getBoolValue() != choice) value.setBoolValue(choice, true, true);
                return true;
            })
            .background(IDrawable.EMPTY)
            .disableHoverBackground();
        if (manager.isClient()) option.overlay(
            AE2Theme.radio(
                option,
                () -> value.getBoolValue() == choice,
                IKey.lang("gtneverreset.gui.universal_wand." + key)::get,
                () -> controlAllowed(key)));
        return option;
    }

    private static ParentWidget<?> numberRow(String key, IntSyncValue value, int min, int max, int y, boolean area) {
        ParentWidget<?> row = new ParentWidget<>().pos(6, y)
            .size(222, 16)
            .child(
                new TextWidget<>(
                    IKey.lang("gtneverreset.gui.universal_wand." + key)
                        .scale(1.0F)).pos(0, 3)
                            .size(105, 16)
                            .maxWidth(105))
            .child(new SliderWidget() {

                @Override
                public Interactable.Result onMousePressed(int button) {
                    return controlAllowed(key) ? super.onMousePressed(button) : Interactable.Result.STOP;
                }

                @Override
                public void onMouseDrag(int button, long time) {
                    if (controlAllowed(key)) super.onMouseDrag(button, time);
                }
            }.tooltipDynamic(tooltip -> featureTooltip(tooltip, key))
                .bounds(min, max)
                .stopper(1)
                .value(value)
                .pos(113, 0)
                .size(76, 16));
        IKey number = IKey.dynamic(
            () -> area ? (value.getIntValue() * 2 + 1) + "x" + (value.getIntValue() * 2 + 1)
                : Integer.toString(value.getIntValue()));
        return row.child(
            number.shadow(false)
                .asWidget()
                .pos(190, 0)
                .size(32, 16)
                .textAlign(Alignment.Center));
    }

    private static ModularPanel accelerationPanel(EntityPlayer player, ItemStack expected, PanelSyncManager manager,
        String configuredRates) {
        BooleanSyncValue enabled = new BooleanSyncValue(
            () -> ItemUniversalWand.isTimeAccelerationEnabled(expected),
            value -> ItemUniversalWand.setTimeAccelerationEnabled(player, expected, value)).allowC2S();
        IntSyncValue rate = new IntSyncValue(() -> ItemUniversalWand.getAccelerationRate(expected));
        IntSyncValue mode = new IntSyncValue(() -> ItemUniversalWand.getAccelerationMode(expected));
        BooleanSyncValue gtMode = new BooleanSyncValue(
            () -> ItemUniversalWand.isAccelerationGtMode(expected),
            value -> ItemUniversalWand.setAccelerationGtMode(player, expected, value)).allowC2S();
        manager.syncValue("acceleration_enabled", 0, enabled);
        manager.syncValue("acceleration_rate", 0, rate);
        manager.syncValue("acceleration_mode", 0, mode);
        manager.syncValue("acceleration_gt_mode", 0, gtMode);
        IPanelHandler history = manager.syncedPanel(
            "gtneverreset_wand_acceleration_history",
            true,
            (historyManager, handler) -> historyPanel(player, historyManager));
        int[] rates = IntStream.concat(
            IntStream.of(0),
            Arrays.stream(configuredRates.split(","))
                .filter(value -> !value.isEmpty())
                .mapToInt(Integer::parseInt))
            .toArray();
        PagedWidget.Controller controller = new PagedWidget.Controller();
        PagedWidget<?> gears = new PagedWidget<>().controller(controller)
            .size(256, 44)
            .pos(12, 74);
        int pageCount = (rates.length + 9) / 10;
        for (int page = 0; page < pageCount; page++) {
            ParentWidget<?> row = new ParentWidget<>().size(256, 44);
            for (int index = page * 10; index < Math.min(rates.length, page * 10 + 10); index++) {
                int selected = rates[index], cell = index % 10;
                row.child(
                    actionButton(
                        "acceleration_off",
                        () -> ItemUniversalWand.setAccelerationRate(player, expected, selected))
                            .overlay(
                                IKey.dynamic(
                                    () -> (rate.getIntValue() == selected ? "✓ " : "") + (selected == 0 ? StatCollector
                                        // #tr gtneverreset.gui.universal_wand.acceleration_off
                                        // # OFF
                                        // # zh_CN 关闭
                                        .translateToLocal("gtneverreset.gui.universal_wand.acceleration_off")
                                        : selected <= 99999 ? "x" + selected
                                            : "x" + String.format(java.util.Locale.ROOT, "%.1e", (double) selected))))
                            .addTooltipLine(
                                selected == 0
                                    ? StatCollector.translateToLocal("gtneverreset.gui.universal_wand.acceleration_off")
                                    : "x" + selected)
                            .pos(cell % 5 * 52, cell / 5 * 22)
                            .size(48, 18));
            }
            gears.addPage(row);
        }
        int extraHeight = pageCount > 1 ? 22 : 0;
        ModularPanel panel = new com.cleanroommc.modularui.widgets.Dialog<>("gtneverreset_wand_acceleration_config")
            .child(ButtonWidget.panelCloseButton())
            .size(280, 214 + extraHeight)
            .child(
                new TextWidget<>(
                    // #tr gtneverreset.gui.universal_wand.acceleration_title
                    // # Universal Wand - Acceleration
                    // # zh_CN 无用之杖 · 加速配置
                    IKey.lang("gtneverreset.gui.universal_wand.acceleration_title")).pos(8, 7)
                        .size(244, 16))
            // #tr gtneverreset.gui.universal_wand.acceleration_engine
            // # Engine
            // # zh_CN 加速方式
            .child(
                IKey.lang("gtneverreset.gui.universal_wand.acceleration_engine")
                    .asWidget()
                    .pos(12, 33))
            .child(
                stateSwitch("time_acceleration", enabled).pos(173, 30)
                    .size(101, 16))
            .child(
                new TextWidget<>(
                    // #tr gtneverreset.gui.universal_wand.acceleration_multiplier
                    // # Acceleration multiplier
                    // # zh_CN 加速倍率
                    IKey.lang("gtneverreset.gui.universal_wand.acceleration_multiplier")).pos(12, 56))
            .child(gears)
            .child(
                new TextWidget<>(
                    // #tr gtneverreset.gui.universal_wand.acceleration_duration
                    // # Duration mode
                    // # zh_CN 持续模式
                    IKey.lang("gtneverreset.gui.universal_wand.acceleration_duration")).pos(12, 128 + extraHeight));
        panel.onUpdateListener(widget -> { if (!UniversalWandFeature.ACCELERATION.allowed()) widget.closeIfOpen(); });
        for (int value = 0; value < 2; value++) {
            boolean selected = value == 1;
            // #tr gtneverreset.gui.universal_wand.acceleration_engine_normal
            // # Normal
            // # zh_CN 普通模式
            // #tr gtneverreset.gui.universal_wand.acceleration_engine_normal.tooltip
            // # Speed up full operation with normal energy use. Reapply to update a target.
            // # zh_CN 完整加速目标运行，正常消耗能源；重新应用后生效。
            // #tr gtneverreset.gui.universal_wand.acceleration_engine_gt
            // # GT
            // # zh_CN GT模式
            // #tr gtneverreset.gui.universal_wand.acceleration_engine_gt.tooltip
            // # Advance GT machine progress; accelerate other targets normally. Reapply to update a target.
            // # zh_CN GT 机器加速进度，其他目标正常加速；重新应用后生效。
            String engineKey = selected ? "acceleration_engine_gt" : "acceleration_engine_normal";
            panel.child(
                radioOption(manager, engineKey, gtMode, selected)
                    .addTooltipLine(IKey.lang("gtneverreset.gui.universal_wand." + engineKey + ".tooltip"))
                    .pos(54 + value * 60, 30)
                    .size(58, 16));
        }
        if (pageCount > 1) {
            for (int page = 0; page < pageCount; page++) panel.child(
                new PageButton(page, controller).overlay(IKey.str(Integer.toString(page + 1)))
                    .pos(12 + page * 24, 118)
                    .size(22, 18));
        }
        for (int value = 0; value < 3; value++) {
            int selected = value;
            panel.child(
                actionButton(
                    "acceleration_mode_" + selected,
                    () -> ItemUniversalWand.setAccelerationMode(player, expected, selected))
                        .overlay(
                            IKey.dynamic(
                                () -> (mode.getIntValue() == selected ? "✓ " : "") + StatCollector
                                    // #tr gtneverreset.gui.universal_wand.acceleration_mode_0
                                    // # Normal (30s, no throttling)
                                    // # zh_CN 普通（30秒，不降频）
                                    // #tr gtneverreset.gui.universal_wand.acceleration_mode_1
                                    // # Permanent (dynamic throttling)
                                    // # zh_CN 永久（动态降频）
                                    // #tr gtneverreset.gui.universal_wand.acceleration_mode_2
                                    // # Permanent (no idle throttling)
                                    // # zh_CN 永久（无休眠降频）
                                    .translateToLocal("gtneverreset.gui.universal_wand.acceleration_mode_" + selected)))
                        .pos(12, 140 + extraHeight + value * 16)
                        .size(256, 16));
        }
        return panel.child(
            new ButtonWidget<>().name("history")
                // #tr gtneverreset.gui.universal_wand.acceleration_history
                // # History (Permanent Targets)
                // # zh_CN 放置历史（永久加速目标）
                .overlay(IKey.lang("gtneverreset.gui.universal_wand.acceleration_history"))
                .onMousePressed(button -> {
                    if (button != 0) return false;
                    history.openPanel();
                    return true;
                })
                .pos(12, 192 + extraHeight)
                .size(256, 20));
    }

    private static ModularPanel historyPanel(EntityPlayer player, PanelSyncManager manager) {
        AtomicInteger page = new AtomicInteger();
        StringSyncValue history = new StringSyncValue(
            () -> player.worldObj.isRemote ? ""
                : UniversalWandAccelerationHandler.history(player.getUniqueID(), page.get()));
        manager.syncValue("permanent_history", 0, history);
        ModularPanel panel = new com.cleanroommc.modularui.widgets.Dialog<>("gtneverreset_wand_acceleration_history")
            .child(ButtonWidget.panelCloseButton())
            .size(296, 206)
            .child(
                // #tr gtneverreset.gui.universal_wand.history_subtitle
                // # My Acceleration > Permanent
                // # zh_CN 我的加速 > 永久加速
                new TextWidget<>(IKey.lang("gtneverreset.gui.universal_wand.history_subtitle")).pos(10, 26)
                    .size(276, 12))
            .child(
                new TextWidget<>(IKey.lang("gtneverreset.gui.universal_wand.acceleration_history")).pos(10, 7)
                    .size(264, 16));
        // #tr gtneverreset.gui.universal_wand.history_empty
        // # No permanent acceleration targets
        // # zh_CN 尚无永久加速目标
        panel.child(
            IKey.lang("gtneverreset.gui.universal_wand.history_empty")
                .asWidget()
                .pos(12, 86)
                .size(272, 16)
                .textAlign(Alignment.Center)
                .setEnabledIf(widget -> historyEntry(history, 0).isEmpty()));
        for (int i = 0; i < UniversalWandAccelerationHandler.HISTORY_PAGE_SIZE; i++) {
            int row = i;
            panel.child(new TextWidget<>(IKey.dynamic(() -> {
                String entry = historyEntry(history, row);
                if (entry.isEmpty()) return "";
                String[] values = entry.split(",", -1);
                return StatCollector.translateToLocalFormatted(
                    // #tr gtneverreset.gui.universal_wand.history_named_entry
                    // # %s · x%s · %s
                    // # zh_CN %s · x%s · %s
                    "gtneverreset.gui.universal_wand.history_named_entry",
                    historyName(values),
                    values[4],
                    // #tr gtneverreset.gui.universal_wand.history_mode_1
                    // # Dynamic
                    // # zh_CN 动态降频
                    // #tr gtneverreset.gui.universal_wand.history_mode_2
                    // # Full speed
                    // # zh_CN 无降频
                    StatCollector.translateToLocal("gtneverreset.gui.universal_wand.history_mode_" + values[5]));
            })).pos(12, 48 + i * 26)
                .size(222, 10));
            panel.child(new TextWidget<>(IKey.dynamic(() -> {
                String entry = historyEntry(history, row);
                if (entry.isEmpty()) return "";
                String[] values = entry.split(",", -1);
                return StatCollector.translateToLocalFormatted(
                    // #tr gtneverreset.gui.universal_wand.history_location
                    // # Dimension %s %s, %s, %s · %s
                    // # zh_CN 维度 %s %s, %s, %s · %s
                    "gtneverreset.gui.universal_wand.history_location",
                    values[0],
                    values[1],
                    values[2],
                    values[3],
                    StatCollector.translateToLocal(
                        "gtneverreset.gui.universal_wand.acceleration_engine_"
                            + ("0".equals(values[8]) ? "normal" : "gt")));
            })).pos(12, 59 + i * 26)
                .size(222, 10));
            panel.child(
                new ButtonWidget<>().setEnabledIf(widget -> !historyEntry(history, row).isEmpty())
                    .overlay(
                        IKey.dynamic(
                            () -> historyEntry(history, row).isEmpty() ? ""
                                // #tr gtneverreset.gui.universal_wand.history_clear
                                // # Clear
                                // # zh_CN 清理
                                : StatCollector.translateToLocal("gtneverreset.gui.universal_wand.history_clear")))
                    .onMousePressed(button -> {
                        if (button != 0) return false;
                        String entry = historyEntry(history, row);
                        if (entry.isEmpty()) return false;
                        String[] values = entry.split(",");
                        UniversalWandNetwork.requestAccelerationManagement(
                            2,
                            Integer.parseInt(values[0]),
                            Integer.parseInt(values[1]),
                            Integer.parseInt(values[2]),
                            Integer.parseInt(values[3]));
                        return true;
                    })
                    .pos(232, 50 + i * 26)
                    .size(56, 16));
        }
        InteractionSyncHandler previous = new InteractionSyncHandler().setOnMousePressed(mouse -> {
            if (!mouse.isClient() && mouse.mouseButton == 0) {
                int current = Integer.parseInt(
                    history.getStringValue()
                        .split(";")[0].split(",")[0]);
                page.set(Math.max(0, current - 1));
            }
        });
        InteractionSyncHandler next = new InteractionSyncHandler().setOnMousePressed(mouse -> {
            if (!mouse.isClient() && mouse.mouseButton == 0) {
                String[] header = history.getStringValue()
                    .split(";")[0].split(",");
                page.set(Math.min(Integer.parseInt(header[1]) - 1, Integer.parseInt(header[0]) + 1));
            }
        });
        panel.child(
            new ButtonWidget<>().syncHandler(previous)
                .overlay(IKey.str("<"))
                .pos(8, 142)
                .size(24, 20));
        panel.child(new TextWidget<>(IKey.dynamic(() -> {
            String[] header = history.getStringValue()
                .split(";")[0].split(",");
            return header.length < 3 ? ""
                : (Integer.parseInt(header[0]) + 1) + " / " + header[1] + "  (" + header[2] + ")";
        })).pos(32, 184)
            .size(60, 16));
        panel.child(
            new ButtonWidget<>().syncHandler(next)
                .overlay(IKey.str(">"))
                .pos(92, 142)
                .size(24, 20));
        panel.child(
            new ButtonWidget<>()
                // #tr gtneverreset.gui.universal_wand.history_refresh
                // # Refresh
                // # zh_CN 刷新
                .overlay(IKey.lang("gtneverreset.gui.universal_wand.history_refresh"))
                .onMousePressed(button -> {
                    if (button != 0) return false;
                    UniversalWandNetwork.requestAccelerationManagement(4, 0, 0, 0, 0);
                    return true;
                })
                .pos(196, 142)
                .size(46, 20));
        panel.child(
            new ButtonWidget<>()
                // #tr gtneverreset.gui.universal_wand.history_back
                // # Back
                // # zh_CN 返回
                .overlay(IKey.lang("gtneverreset.gui.universal_wand.history_back"))
                .onMousePressed(button -> {
                    if (button != 0) return false;
                    panel.closeIfOpen();
                    return true;
                })
                .pos(244, 142)
                .size(46, 20));
        return panel.child(
            new ButtonWidget<>().setEnabledIf(
                widget -> history.getStringValue()
                    .split(";").length > 1)
                // #tr gtneverreset.gui.universal_wand.history_clear_all
                // # Clear All Permanent Targets
                // # zh_CN 清理全部永久目标
                .overlay(IKey.lang("gtneverreset.gui.universal_wand.history_clear_all"))
                .onMousePressed(button -> {
                    if (button != 0) return false;
                    UniversalWandNetwork.requestAccelerationManagement(3, 0, 0, 0, 0);
                    return true;
                })
                .pos(120, 142)
                .size(72, 20));
    }

    private static String historyName(String[] values) {
        String key = new String(
            Base64.getDecoder()
                .decode(values[6]),
            java.nio.charset.StandardCharsets.UTF_8);
        String name = new String(
            Base64.getDecoder()
                .decode(values[7]),
            java.nio.charset.StandardCharsets.UTF_8);
        return !key.isEmpty() && StatCollector.canTranslate(key) ? StatCollector.translateToLocal(key)
            // #tr gtneverreset.gui.universal_wand.history_unknown
            // # Unknown block (unloaded)
            // # zh_CN 未知方块（未加载）
            : name.isEmpty() ? StatCollector.translateToLocal("gtneverreset.gui.universal_wand.history_unknown") : name;
    }

    private static String historyEntry(StringSyncValue history, int row) {
        String[] entries = history.getStringValue()
            .split(";");
        if (entries.length <= row + 1) return "";
        String entry = entries[row + 1];
        return entry.split(",", -1).length == 9 ? entry : "";
    }

    private static ButtonWidget<?> actionButton(String key, Runnable action) {
        InteractionSyncHandler handler = new InteractionSyncHandler().setOnMousePressed(
            mouse -> { if (!mouse.isClient() && mouse.mouseButton == 0 && controlAllowed(key)) action.run(); });
        class FeatureButton extends ButtonWidget<FeatureButton> {

            @Override
            public Interactable.Result onMousePressed(int button) {
                return controlAllowed(key) ? super.onMousePressed(button) : Interactable.Result.STOP;
            }
        }
        return new FeatureButton().syncHandler(handler)
            .overlay(IKey.lang("gtneverreset.gui.universal_wand." + key));
    }

    private static boolean controlAllowed(String key) {
        UniversalWandFeature feature = UniversalWandFeature.forControl(key);
        return feature == null || feature.allowed();
    }

    private static void featureTooltip(RichTooltip tooltip, String key) {
        String translation = "gtneverreset.gui.universal_wand." + key + ".tooltip";
        if (StatCollector.canTranslate(translation)) tooltip.addLine(StatCollector.translateToLocal(translation));
        if (!controlAllowed(key)) tooltip.addLine(ItemUniversalWand.disabledFeatureTooltip());
    }
}
