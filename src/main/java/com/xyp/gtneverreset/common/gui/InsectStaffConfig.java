package com.xyp.gtneverreset.common.gui;

import java.util.List;

import net.minecraft.item.ItemStack;

import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.utils.IConfigurableItem.ProfileHelper;
import com.brandon3055.draconicevolution.common.utils.ItemConfigField;
import com.xyp.gtneverreset.config.VeinMinerPickaxeConfig;

public final class InsectStaffConfig {

    // #tr button.de.GTNRVeinEnabled.name
    // # Vein mining (sneak)
    // # zh_CN 潜行连锁挖掘
    public static final String ENABLED = "GTNRVeinEnabled";
    // #tr button.de.GTNRVeinRange.name
    // # Vein scan gap
    // # zh_CN 连锁范围（最大间隔）
    public static final String RANGE = "GTNRVeinRange";
    // #tr button.de.GTNRVeinAmount.name
    // # Vein block limit
    // # zh_CN 连锁挖掘数量上限
    public static final String AMOUNT = "GTNRVeinAmount";
    // #tr button.de.GTNRVeinPrecise.name
    // # Precise vein matching
    // # zh_CN 连锁精确模式
    public static final String PRECISE = "GTNRVeinPrecise";
    // #tr button.de.GTNRVeinBackfill.name
    // # Backfill mined ores
    // # zh_CN 矿石回填
    public static final String BACKFILL = "GTNRVeinBackfill";
    // #tr button.de.GTNRTeleport.name
    // # Advanced teleport staff
    // # zh_CN 高级传送权杖
    public static final String TELEPORT = "GTNRTeleport";
    // #tr button.de.GTNRSprintSpeed.name
    // # Held sprint speed multiplier
    // # zh_CN 手持奔跑速度倍率
    public static final String SPRINT_SPEED = "GTNRSprintSpeed";
    // #tr button.de.GTNRFlightSpeed.name
    // # Held flight speed multiplier
    // # zh_CN 手持飞行速度倍率
    public static final String FLIGHT_SPEED = "GTNRFlightSpeed";

    private InsectStaffConfig() {}

    public static List<ItemConfigField> fields(List<ItemConfigField> fields, ItemStack stack, int slot) {
        fields.add(new ItemConfigField(References.BOOLEAN_ID, slot, ENABLED).readFromItem(stack, true));
        fields.add(
            new ItemConfigField(References.INT_ID, slot, RANGE)
                .setMinMaxAndIncromente(0, Math.max(0, VeinMinerPickaxeConfig.maxRange), 1)
                .readFromItem(stack, range(stack)));
        fields.add(
            new ItemConfigField(References.INT_ID, slot, AMOUNT)
                .setMinMaxAndIncromente(0, Math.max(0, VeinMinerPickaxeConfig.maxAmount), 1)
                .readFromItem(stack, amount(stack)));
        fields.add(new ItemConfigField(References.BOOLEAN_ID, slot, PRECISE).readFromItem(stack, false));
        fields.add(new ItemConfigField(References.BOOLEAN_ID, slot, BACKFILL).readFromItem(stack, true));
        fields.add(new ItemConfigField(References.BOOLEAN_ID, slot, TELEPORT).readFromItem(stack, true));
        for (String key : new String[] { SPRINT_SPEED, FLIGHT_SPEED }) {
            fields.add(
                new ItemConfigField(References.FLOAT_ID, speedMultiplier(stack, key), slot, key)
                    .setMinMaxAndIncromente(1F, 10F, 0.1F)
                    .setModifier("MULTIPLIER"));
        }
        return fields;
    }

    public static float speedMultiplier(ItemStack stack, String key) {
        float value = ProfileHelper.getFloat(stack, key, 2F);
        return Float.isFinite(value) ? Math.max(1F, Math.min(10F, value)) : 2F;
    }

    public static boolean veinEnabled(ItemStack stack) {
        return ProfileHelper.getBoolean(stack, ENABLED, true) && amount(stack) > 0;
    }

    public static int range(ItemStack stack) {
        return Math.max(0, Math.min(VeinMinerPickaxeConfig.maxRange, ProfileHelper.getInteger(stack, RANGE, 3)));
    }

    public static int amount(ItemStack stack) {
        return Math.max(
            0,
            Math.min(
                VeinMinerPickaxeConfig.maxAmount,
                ProfileHelper.getInteger(stack, AMOUNT, VeinMinerPickaxeConfig.maxAmount)));
    }
}
