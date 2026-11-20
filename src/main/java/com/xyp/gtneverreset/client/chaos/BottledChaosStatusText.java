package com.xyp.gtneverreset.client.chaos;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.formatNumber;
import static com.xyp.gtneverreset.common.machine.multiMachineBase.BottleInChaosMachineBase.THREAD_COUNT;
import static com.xyp.gtneverreset.common.machine.multiMachineBase.BottleInChaosMachineBase.getProcessingGroupCount;
import static com.xyp.gtneverreset.common.machine.multiMachineBase.BottleInChaosMachineBase.getProcessingGroupData;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.crafting.chaos.material.BottledChaosMaterialSummary;
import com.xyp.gtneverreset.common.crafting.chaos.processing.BottledChaosStatus;
import com.xyp.gtneverreset.common.crafting.chaos.result.BottledChaosResultData;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.GTValues;
import gregtech.api.util.GTUtility;
import gregtech.api.util.GTWaila;
import mcp.mobius.waila.api.SpecialChars;
import mcp.mobius.waila.overlay.tooltiprenderers.TTRenderStack;

@SideOnly(Side.CLIENT)
public final class BottledChaosStatusText {

    private BottledChaosStatusText() {}

    public static String getCredentialHatchBoundInputs(int peers) {
        return StatCollector.translateToLocalFormatted(
            // #tr gtnr.machine.BottleInChaosRecipeTokenHatch.gui.bound_inputs
            // # Bound to %s
            // # zh_CN 与%s绑定
            "gtnr.machine.BottleInChaosRecipeTokenHatch.gui.bound_inputs",
            formatProcessingGroups(peers));
    }

    public static String getCredentialHatchTitle(NBTTagCompound data) {
        if (data == null || !data.getBoolean("connected")) return "#-";
        int peers = data.getInteger("boundSlots");
        return "#" + data.getInteger("slot") + (peers == 0 ? "" : " | " + getCredentialHatchBoundInputs(peers));
    }

    public static void appendCredentialHatchWaila(List<String> currentTip, NBTTagCompound data, boolean showCache) {
        currentTip.add(
            EnumChatFormatting.WHITE + getCredentialHatchTitle(data)
                + (data.getBoolean("connected")
                    ? " | " + getStatusColor(data.getString("status")) + getWailaStatusText(data)
                    : "")
                + EnumChatFormatting.RESET);
        if (showCache) {
            String keys = data.getString("tags");
            if (!keys.isEmpty()) {
                for (String key : keys.split(", "))
                    currentTip.add(EnumChatFormatting.WHITE + StatCollector.translateToLocal(key));
            } else {
                currentTip.add(EnumChatFormatting.WHITE + localizeKeyList(data.getString("family")));
            }
        } else {
            currentTip.add(EnumChatFormatting.WHITE + localizeKeyList(data.getString("family")));
        }
        int duration = data.getInteger("duration");
        if (!data.getBoolean("connected")) {
            currentTip.add(
                EnumChatFormatting.YELLOW
                    // #tr gtnr.machine.BottleInChaosRecipeTokenHatch.info.disconnected
                    // # Not connected to a formed Bottle in Chaos controller.
                    // # zh_CN 未连接到成型的混沌中瓶主机.
                    + StatCollector.translateToLocal("gtnr.machine.BottleInChaosRecipeTokenHatch.info.disconnected"));
            if (!showCache) return;
        } else {
            String blockedRoute = getBlockedRouteText(data);
            if (!blockedRoute.isEmpty()) currentTip.add(blockedRoute);
            BigInteger eut = processingGroupPower(data);
            byte energyTier = (byte) data.getLong("energyTier");
            currentTip.add(
                EnumChatFormatting.WHITE
                    + (energyTier > 0
                        ? StatCollector.translateToLocalFormatted(
                            "GT5U.waila.energy.use_with_amperage",
                            formatNumber(eut),
                            processingGroupAmperage(data),
                            GTUtility.getColoredTierNameFromTier(energyTier))
                        : StatCollector.translateToLocalFormatted(
                            "GT5U.waila.energy.use",
                            formatNumber(eut),
                            GTUtility.getColoredTierNameFromVoltage(eut.longValue())))
                    + EnumChatFormatting.WHITE
                    + " | "
                    + EnumChatFormatting.AQUA
                    + StatCollector.translateToLocalFormatted(
                        // #tr gtnr.machine.BottleInChaosRecipeTokenHatch.gui.power_share
                        // # Power share: %s%%
                        // # zh_CN 电力份额: %s%%
                        "gtnr.machine.BottleInChaosRecipeTokenHatch.gui.power_share",
                        String.format(Locale.ROOT, "%.2f", data.getDouble("powerRatio") * 100)));
            if (duration > 0) {
                currentTip.add(GTWaila.getMachineProgressString(duration, data.getInteger("progress")));
            }
        }
        NBTTagCompound outputs = showCache ? data.getCompoundTag("cache") : data.getCompoundTag("outputs");
        if (showCache) {
            currentTip.add(
                EnumChatFormatting.GOLD + StatCollector.translateToLocalFormatted(
                    // #tr gtnr.machine.BottleInChaosRecipeTokenHatch.gui.cached_materials
                    // # Cached materials (%s types):
                    // # zh_CN 缓存材料(%s种):
                    "gtnr.machine.BottleInChaosRecipeTokenHatch.gui.cached_materials",
                    formatNumber(outputs.getInteger("total"))));
            currentTip.addAll(getMaterialSummaryLines(outputs, true));
        } else {
            appendMaterialSummary(currentTip, getProcessingGroupOutputTitle(data), outputs);
            appendMaterialSummary(
                currentTip,
                // #tr gtnr.machine.BottleInChaos.info.intermediate_outputs
                // # Intermediate products:
                // # zh_CN 中间产物:
                StatCollector.translateToLocal("gtnr.machine.BottleInChaos.info.intermediate_outputs"),
                outputs.getCompoundTag("intermediates"));
            appendMaterialSummary(
                currentTip,
                // #tr gtnr.machine.BottleInChaos.info.byproducts
                // # Byproducts:
                // # zh_CN 副产物:
                StatCollector.translateToLocal("gtnr.machine.BottleInChaos.info.byproducts"),
                outputs.getCompoundTag("byproducts"));
        }
    }

    private static void appendMaterialSummary(List<String> lines, String title, NBTTagCompound materials) {
        if (materials.getInteger("total") <= 0) return;
        lines.add(EnumChatFormatting.WHITE + title);
        lines.addAll(getMaterialSummaryLines(materials, true));
    }

    public static List<String> getMaterialSummaryLines(NBTTagCompound materials, boolean wailaIcons) {
        List<String> lines = new ArrayList<>();
        NBTTagList entries = materials.getTagList("entries", 10);
        int shown = 0;
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            ItemStack item = entry.hasKey("item", 10) ? ItemStack.loadItemStackFromNBT(entry.getCompoundTag("item"))
                : null;
            FluidStack fluid = entry.hasKey("fluid", 10)
                ? FluidStack.loadFluidStackFromNBT(entry.getCompoundTag("fluid"))
                : null;
            if (item == null && fluid == null) continue;
            lines.add(
                (wailaIcons
                    ? "  "
                        + TTRenderStack.create(item != null ? item : GTUtility.getFluidDisplayStack(fluid, false), true)
                    : "") + formatGroupOutput(item, fluid, entry));
            shown++;
        }
        if (materials.getInteger("total") > shown) lines.add(
            StatCollector
                .translateToLocalFormatted("GT5U.waila.producing.andmore", materials.getInteger("total") - shown));
        return lines;
    }

    public static String getWailaStatusText(NBTTagCompound job) {
        return BottledChaosResultData.displayText(job, "status");
    }

    public static String formatProcessingGroups(int mask) {
        StringJoiner groups = new StringJoiner(", ");
        for (int slot = 0; slot < THREAD_COUNT; slot++) if ((mask & (1 << slot)) != 0) groups.add("#" + (slot + 1));
        return groups.toString();
    }

    public static void appendProcessingGroups(List<String> currentTip, NBTTagCompound tag) {
        List<Integer> groups = new ArrayList<>();
        for (int i = 0; i < getProcessingGroupCount(tag); i++) groups.add(i);
        groups.sort(
            (first, second) -> processingGroupPower(getProcessingGroupData(tag, second))
                .compareTo(processingGroupPower(getProcessingGroupData(tag, first))));
        List<Integer> shown = groups.subList(0, Math.min(5, groups.size()));
        shown.sort(Integer::compare);
        for (int index : shown) currentTip.addAll(getProcessingGroupLines(tag, index));
        if (groups.size() > shown.size()) currentTip.add(
            StatCollector.translateToLocalFormatted(
                // #tr gtnr.machine.BottleInChaos.waila.more_processing_groups
                // # And %d More Processing Groups...
                // # zh_CN 并且还有 %d 个加工组...
                "gtnr.machine.BottleInChaos.waila.more_processing_groups",
                groups.size() - shown.size()));
    }

    public static List<String> getProcessingGroupLines(NBTTagCompound summary, int index) {
        return getProcessingGroupLines(summary, index, true);
    }

    public static List<String> getProcessingGroupLines(NBTTagCompound summary, int index, boolean waila) {
        List<String> lines = new ArrayList<>();
        if (index < 0 || index >= getProcessingGroupCount(summary)) return lines;
        NBTTagCompound job = getProcessingGroupData(summary, index);
        String energy = waila ? "" : getProcessingGroupEnergyText(job);
        String separator = EnumChatFormatting.WHITE + (waila ? SpecialChars.TAB : " | ");
        String share = waila ? SpecialChars.ALIGNRIGHT + EnumChatFormatting.AQUA
        // #tr gtnr.machine.BottleInChaos.info.input_share
        // # Power share: %s
        // # zh_CN 电力份额: %s
            + StatCollector.translateToLocalFormatted("gtnr.machine.BottleInChaos.info.input_share", "")
            + SpecialChars.TAB
            + EnumChatFormatting.AQUA
            + getProcessingGroupShareValue(job) : getProcessingGroupShareText(summary, job);
        lines.add(
            EnumChatFormatting.WHITE + "#"
                + job.getInteger("slot")
                + (waila ? separator : " ")
                + localizeKeyList(job.getString("family"))
                + separator
                + share
                + (energy.isEmpty() ? "" : separator + energy)
                + separator
                + getStatusColor(job.getString("status"))
                + getWailaStatusText(job)
                + EnumChatFormatting.RESET);
        String blockedRoute = getBlockedRouteText(job);
        if (!blockedRoute.isEmpty()) lines.add(blockedRoute);
        if (waila && job.getInteger("duration") > 0) {
            lines.add(GTWaila.getMachineProgressString(job.getInteger("duration"), job.getInteger("progress")));
        }
        String outputs = getProcessingGroupOutputText(job, waila);
        if (!outputs.isEmpty()) lines.add(outputs);
        return lines;
    }

    public static String getProcessingGroupShareText(NBTTagCompound summary, NBTTagCompound job) {
        return EnumChatFormatting.AQUA + StatCollector.translateToLocalFormatted(
            "gtnr.machine.BottleInChaos.info.input_share",
            getProcessingGroupShareValue(job));
    }

    public static String getProcessingGroupShareValue(NBTTagCompound job) {
        return String.format(Locale.ROOT, "%.2f%%", job.getDouble("powerRatio") * 100);
    }

    private static BigInteger processingGroupPower(NBTTagCompound job) {
        byte[] power = job.getByteArray("power");
        return power.length == 0 ? BigInteger.ZERO : new BigInteger(1, power);
    }

    private static BigInteger processingGroupAmperage(NBTTagCompound job) {
        BigInteger voltage = BigInteger.valueOf(GTValues.V[(int) job.getLong("energyTier")]);
        return processingGroupPower(job).add(voltage)
            .subtract(BigInteger.ONE)
            .divide(voltage);
    }

    public static String getProcessingGroupEnergyText(NBTTagCompound job) {
        return processingGroupPower(job).signum() <= 0 ? ""
            : getProcessingGroupPowerText(job) + " " + getProcessingGroupVoltageText(job);
    }

    public static String getProcessingGroupPowerText(NBTTagCompound job) {
        BigInteger eut = processingGroupPower(job);
        if (eut.signum() <= 0) return "";
        return EnumChatFormatting.YELLOW + String.format(Locale.ROOT, "%.1e", eut.doubleValue())
            .replace("e+0", "e")
            .replace("e+", "e") + EnumChatFormatting.WHITE + " EU/t";
    }

    public static String getProcessingGroupVoltageText(NBTTagCompound job) {
        if (processingGroupPower(job).signum() <= 0) return "";
        byte tier = (byte) job.getLong("energyTier");
        return EnumChatFormatting.WHITE + "("
            + processingGroupAmperage(job)
            + "A "
            + GTUtility.getColoredTierNameFromTier(tier)
            + EnumChatFormatting.WHITE
            + ")";
    }

    public static String getBlockedRouteText(NBTTagCompound job) {
        return job.hasKey("blockedRoute", 10) ? EnumChatFormatting.YELLOW + StatCollector.translateToLocalFormatted(
            // #tr gtnr.machine.BottleInChaos.info.partial_route_blocked
            // # Some routes blocked: %s
            // # zh_CN 部分路线受阻: %s
            "gtnr.machine.BottleInChaos.info.partial_route_blocked",
            BottledChaosResultData.read(job.getCompoundTag("blockedRoute"))
                .getDisplayString())
            : "";
    }

    public static String getProcessingGroupOutputTitle(NBTTagCompound job) {
        return StatCollector.translateToLocal(
            job.getInteger("duration") > 0 && job.getInteger("progress") >= job.getInteger("duration")
                // #tr gtnr.machine.BottleInChaos.info.pending_outputs
                // # Pending outputs:
                // # zh_CN 待输出:
                ? "gtnr.machine.BottleInChaos.info.pending_outputs"
                : "GT5U.waila.producing");
    }

    public static String getProcessingGroupOutputText(NBTTagCompound job) {
        return getProcessingGroupOutputText(job, false);
    }

    private static String getProcessingGroupOutputText(NBTTagCompound job, boolean waila) {
        String outputs = formatGroupOutputs(job.getCompoundTag("outputs"), waila);
        return outputs.isEmpty() ? "" : EnumChatFormatting.WHITE + getProcessingGroupOutputTitle(job) + " " + outputs;
    }

    private static String formatGroupOutputs(NBTTagCompound outputs, boolean waila) {
        NBTTagList entries = outputs.getTagList("entries", 10);
        StringJoiner text = new StringJoiner(EnumChatFormatting.WHITE + ", ");
        int shown = 0;
        int limit = waila ? BottledChaosMaterialSummary.GROUP_OUTPUT_LIMIT : entries.tagCount();
        for (int i = 0; i < Math.min(limit, entries.tagCount()); i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            ItemStack item = entry.hasKey("item", 10) ? ItemStack.loadItemStackFromNBT(entry.getCompoundTag("item"))
                : null;
            FluidStack fluid = entry.hasKey("fluid", 10)
                ? FluidStack.loadFluidStackFromNBT(entry.getCompoundTag("fluid"))
                : null;
            if (item == null && fluid == null) continue;
            text.add(formatGroupOutput(item, fluid, entry));
            shown++;
        }
        if (outputs.getInteger("total") > shown) text.add(
            waila
                ? StatCollector
                    .translateToLocalFormatted("GT5U.waila.producing.andmore", outputs.getInteger("total") - shown)
                : "...");
        return text.toString();
    }

    public static String formatGroupOutput(ItemStack item, FluidStack fluid, NBTTagCompound entry) {
        return EnumChatFormatting.AQUA + (item != null ? item.getDisplayName() : fluid.getLocalizedName())
            + EnumChatFormatting.WHITE
            + " x "
            + EnumChatFormatting.GOLD
            + formatNumber(new BigInteger(entry.getString("amount")))
            + (fluid == null ? "" : "L")
            + EnumChatFormatting.RESET;
    }

    public static String localizeKeyList(String keys) {
        StringJoiner localized = new StringJoiner(", ");
        for (String key : keys.split(", ")) localized.add(StatCollector.translateToLocal(key));
        return localized.toString();
    }

    public static EnumChatFormatting getStatusColor(String status) {
        return BottledChaosStatus.fromKey(status)
            .textColor();
    }
}
