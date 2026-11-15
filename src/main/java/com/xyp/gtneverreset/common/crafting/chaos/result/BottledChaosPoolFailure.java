package com.xyp.gtneverreset.common.crafting.chaos.result;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.formatNumber;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.StringJoiner;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizons.modularui.common.internal.network.NetworkUtils;
import com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosMachineConfiguration;

import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;

/** Keep pool names and failure arguments untranslated until they reach the viewing client. */
public final class BottledChaosPoolFailure implements CheckRecipeResult {

    private NBTTagCompound data = new NBTTagCompound();

    public static BottledChaosPoolFailure missingFluid(String pool, FluidStack fluid, BigInteger required) {
        BottledChaosPoolFailure failure = new BottledChaosPoolFailure();
        failure.data.setString("pool", pool);
        failure.data.setTag("fluid", fluid.writeToNBT(new NBTTagCompound()));
        failure.data.setString("amount", required.toString());
        return failure;
    }

    public static BottledChaosPoolFailure missingItem(String pool, ItemStack item, long required) {
        BottledChaosPoolFailure failure = new BottledChaosPoolFailure();
        failure.data.setString("pool", pool);
        failure.data.setTag("item", item.writeToNBT(new NBTTagCompound()));
        failure.data.setString("amount", Long.toString(required));
        return failure;
    }

    public static BottledChaosPoolFailure of(String pool, CheckRecipeResult reason) {
        BottledChaosPoolFailure failure = new BottledChaosPoolFailure();
        failure.data.setString("pool", pool);
        BottledChaosResultData.write(failure.data, reason);
        return failure;
    }

    public static BottledChaosPoolFailure parameter(String pool, String parameter, long available, long required) {
        BottledChaosPoolFailure failure = new BottledChaosPoolFailure();
        failure.data.setString("pool", pool);
        failure.data.setString("parameter", parameter);
        failure.data.setLong("available", available);
        failure.data.setLong("required", required);
        return failure;
    }

    @Override
    public @NotNull String getID() {
        return "gtnr_bottled_chaos_pool_failure";
    }

    @Override
    public boolean wasSuccessful() {
        return false;
    }

    @Override
    public @NotNull String getDisplayString() {
        String reason;
        if (data.hasKey("fluid", 10)) {
            FluidStack fluid = FluidStack.loadFluidStackFromNBT(data.getCompoundTag("fluid"));
            reason = StatCollector.translateToLocalFormatted(
                "GT5U.gui.text.out_of_fluid",
                // #tr gtnr.machine.BottleInChaos.result.unknown_material
                // # Unknown material
                // # zh_CN 未知材料
                fluid == null ? StatCollector.translateToLocal("gtnr.machine.BottleInChaos.result.unknown_material")
                    : fluid.getLocalizedName(),
                formatNumber(new BigInteger(data.getString("amount"))));
        } else if (data.hasKey("item", 10)) {
            ItemStack item = ItemStack.loadItemStackFromNBT(data.getCompoundTag("item"));
            reason = StatCollector.translateToLocalFormatted(
                "GT5U.gui.text.recipe_result.out_of_item",
                item == null ? StatCollector.translateToLocal("gtnr.machine.BottleInChaos.result.unknown_material")
                    : item.getDisplayName(),
                formatNumber(Long.parseLong(data.getString("amount"))));
        } else if (BottledChaosResultData.hasResult(data)) {
            reason = BottledChaosResultData.read(data)
                .getDisplayString();
        } else if ("heat".equals(data.getString("parameter"))) {
            reason = CheckRecipeResultRegistry.insufficientHeat((int) data.getLong("required"))
                .getDisplayString();
        } else if ("voltage".equals(data.getString("parameter"))) {
            reason = CheckRecipeResultRegistry.insufficientVoltage(data.getLong("required"))
                .getDisplayString();
        } else {
            reason = StatCollector.translateToLocalFormatted(
                // #tr gtnr.machine.BottleInChaos.result.parameter_limit
                // # %s insufficient: %s / %s
                // # zh_CN %s不足: %s / %s
                "gtnr.machine.BottleInChaos.result.parameter_limit",
                // #tr gtnr.bottleinchaos.shared.parameter.glass
                // # Glass tier
                // # zh_CN 玻璃等级
                // #tr gtnr.bottleinchaos.shared.parameter.casing
                // # Casing tier
                // # zh_CN 机械方块等级
                // #tr gtnr.bottleinchaos.shared.parameter.compression
                // # Compression tier
                // # zh_CN 压缩等级
                // #tr gtnr.bottleinchaos.shared.parameter.field
                // # Field generator tier
                // # zh_CN 力场发生器等级
                // #tr gtnr.bottleinchaos.shared.parameter.plasma
                // # Plasma tier
                // # zh_CN 等离子等级
                // #tr gtnr.bottleinchaos.shared.parameter.heavy_centrifuge
                // # Heavy centrifugation capability
                // # zh_CN 重型离心能力
                // #tr gtnr.bottleinchaos.shared.parameter.multi_step_plasma
                // # Multi-step plasma capability
                // # zh_CN 多步等离子能力
                // #tr gtnr.bottleinchaos.shared.parameter.purification_capacity
                // # Main purification plant capacity
                // # zh_CN 主净化厂堆叠组数
                // #tr gtnr.bottleinchaos.shared.parameter.culture
                // # Culture dishes
                // # zh_CN 培养皿
                // #tr gtnr.bottleinchaos.shared.parameter.catalyst
                // # Catalyst
                // # zh_CN 催化剂
                // #tr gtnr.bottleinchaos.shared.parameter.coolant
                // # Coolant
                // # zh_CN 冷却液
                StatCollector.translateToLocal("gtnr.bottleinchaos.shared.parameter." + data.getString("parameter")),
                formatNumber(data.getLong("available")),
                formatNumber(data.getLong("required")));
        }
        StringJoiner pools = new StringJoiner(", ");
        for (String pool : data.getString("pool")
            .split(";")) pools.add(BottledChaosMachineConfiguration.poolName(pool));
        return pools + ": " + reason;
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(@NotNull NBTTagCompound tag) {
        tag.setTag("detail", data.copy());
        return tag;
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound tag) {
        data = (NBTTagCompound) tag.getCompoundTag("detail")
            .copy();
    }

    @Override
    public @NotNull CheckRecipeResult newInstance() {
        return new BottledChaosPoolFailure();
    }

    @Override
    public void encode(@NotNull PacketBuffer buffer) {
        try {
            NetworkUtils.writeNBTBase(buffer, data);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void decode(@NotNull PacketBuffer buffer) {
        try {
            data = (NBTTagCompound) NetworkUtils.readNBTBase(buffer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BottledChaosPoolFailure failure && data.equals(failure.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

}
