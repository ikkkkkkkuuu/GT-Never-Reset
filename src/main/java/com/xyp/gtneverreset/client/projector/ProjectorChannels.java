package com.xyp.gtneverreset.client.projector;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.structurelib.ChannelDescription;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtneverreset.common.projector.ProjectorStructure.Choice;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.misc.GTStructureChannels;

// StructureLib keeps channel descriptions and indicator items only in ChannelDescription, which it marks as internal.
@SideOnly(Side.CLIENT)
public class ProjectorChannels {

    private static final String GT_CHANNEL_KEYS = "channels.";
    private static final String[] SENTENCE_ENDS = { String.valueOf((char) 0x3002), String.valueOf((char) 0xFF0C), ". ",
        ", " };

    private ProjectorChannels() {}

    public static String label(String channel) {
        if (ChannelDescription.has(channel)) {
            for (String key : ChannelDescription.get(channel)
                .getDescriptions()
                .values()) {
                if (StatCollector.canTranslate(key)) return StatCollector.translateToLocal(key);
            }
        }
        return channel;
    }

    public static String title(String channel) {
        for (GTStructureChannels known : GTStructureChannels.values()) {
            if (!known.get()
                .equals(channel)) continue;
            String name = known.getDefaultTooltip();
            if (!name.startsWith(GT_CHANNEL_KEYS)) return name;
        }
        String label = EnumChatFormatting.getTextWithoutFormattingCodes(label(channel));
        int end = label.length();
        for (String stop : SENTENCE_ENDS) {
            int at = label.indexOf(stop);
            if (at > 0) end = Math.min(end, at);
        }
        return label.substring(0, end);
    }

    public static String name(ItemStack stack) {
        try {
            return stack.getDisplayName();
        } catch (RuntimeException e) {
            return stack.getItem()
                .getUnlocalizedName();
        }
    }

    public static TreeMap<Integer, ItemStack> tiers(String channel) {
        TreeMap<Integer, ItemStack> tiers = new TreeMap<>();
        if (!ChannelDescription.has(channel)) return tiers;
        for (Map.Entry<ItemStack, Integer> entry : ChannelDescription.get(channel)
            .getItems()
            .entrySet()) tiers.putIfAbsent(entry.getValue(), entry.getKey());
        return tiers;
    }

    public static String channelOf(ItemStack stack, Collection<String> usable) {
        if (stack == null) return null;
        for (Map.Entry<String, Integer> entry : ChannelDescription.iterate(stack)) {
            if (entry.getValue() != null && usable.contains(entry.getKey())) return entry.getKey();
        }
        return null;
    }

    public static Integer tierOf(ItemStack stack, String channel) {
        for (Map.Entry<String, Integer> entry : ChannelDescription.iterate(stack)) {
            if (entry.getKey()
                .equals(channel)) return entry.getValue();
        }
        return null;
    }

    public static TreeMap<Integer, ItemStack> tiers(String channel, ProjectorStructure structure) {
        int group = structure == null ? -1 : structure.groupOfChannel(channel);
        return group < 0 ? tiers(channel) : tiers(structure.choices.get(group));
    }

    public static TreeMap<Integer, ItemStack> tiers(Choice choice) {
        TreeMap<Integer, ItemStack> tiers = new TreeMap<>();
        for (int i = 0; i < choice.blocks()
            .size() && i < choice.tiers().length; i++) {
            tiers.putIfAbsent(
                choice.tiers()[i],
                choice.blocks()
                    .get(i));
        }
        return tiers.isEmpty() ? tiers(choice.channel()) : tiers;
    }

    public static int max(String channel) {
        int max = 0;
        if (ChannelDescription.has(channel)) {
            for (int value : ChannelDescription.get(channel)
                .getItems()
                .values()) max = Math.max(max, value);
        }
        return max > 0 ? max : ProjectorPlan.MAX_CHANNEL_VALUE;
    }
}
