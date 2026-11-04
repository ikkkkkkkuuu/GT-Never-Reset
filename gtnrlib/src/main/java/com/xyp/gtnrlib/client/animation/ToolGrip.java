package com.xyp.gtnrlib.client.animation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemFlintAndSteel;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;

import com.xyp.gtnrlib.GTNRLib;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** First-person melee and hand-tool families. Classification never changes the actual rendered stack. */
@SideOnly(Side.CLIENT)
public enum ToolGrip {

    SWORD("swords"),
    PICKAXE("pickaxes"),
    AXE("axes"),
    SHOVEL("shovels"),
    HOE("hoes"),
    HANDHELD(null);

    final String tag;
    final String reference;
    private static final Map<Class<?>, Function<ItemStack, ToolGrip>> families = new HashMap<>();
    private static final Set<Item> reportedItems = new HashSet<>();
    private static ItemStack lastStack;
    private static int lastDamage, lastTick;
    private static ToolGrip lastGrip;

    ToolGrip(String tag) {
        this.tag = tag;
        reference = tag == null ? null : "minecraft:iron_" + name().toLowerCase(Locale.ROOT);
    }

    /** Supply the physical shape of a mod's tool. A generic handheld hint still allows a more specific Forge family. */
    public static void registerFamily(Class<? extends Item> base, Function<ItemStack, ToolGrip> resolver) {
        if (families.putIfAbsent(Objects.requireNonNull(base), Objects.requireNonNull(resolver)) != null)
            throw new IllegalArgumentException("Tool family already registered for " + base.getName());
        invalidate();
    }

    public static void unregisterFamily(Class<? extends Item> base) {
        families.remove(base);
        invalidate();
    }

    static void invalidate() {
        lastStack = null;
    }

    public static ToolGrip resolve(ItemStack stack) {
        if (stack == null) return null;
        var player = Minecraft.getMinecraft().thePlayer;
        int tick = player == null ? 0 : player.ticksExisted;
        int damage = stack.getItemDamage();
        if (stack != lastStack || damage != lastDamage || tick != lastTick) {
            lastGrip = classify(stack);
            lastStack = stack;
            lastDamage = damage;
            lastTick = tick;
        }
        return lastGrip;
    }

    static ToolGrip vanilla(Item item) {
        if (item instanceof ItemSword) return SWORD;
        if (item instanceof ItemPickaxe) return PICKAXE;
        if (item instanceof ItemAxe) return AXE;
        if (item instanceof ItemSpade) return SHOVEL;
        if (item instanceof ItemHoe) return HOE;
        return null;
    }

    private static ToolGrip classify(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ItemBlock || item instanceof ItemMap
            || item instanceof ItemArmor
            || item instanceof ItemBow
            || item instanceof ItemFishingRod) return null;
        ToolGrip grip = vanilla(item);
        if (grip != null) return grip;
        ToolGrip registered = registeredFamily(stack);
        if (registered != null && registered != HANDHELD) return registered;
        Set<String> classes = harvestClasses(stack);
        // A multi-tool can report several harvest types. Choose a stable family regardless of Set iteration order.
        if (classes.contains("sword")) return SWORD;
        if (classes.contains("pickaxe")) return PICKAXE;
        if (classes.contains("axe")) return AXE;
        if (classes.contains("shovel") || classes.contains("spade")) return SHOVEL;
        if (classes.contains("hoe")) return HOE;
        ToolGrip ore = OreGrip.tool(stack);
        if (ore != null) return ore;
        if (registered != null || !classes.isEmpty()
            || item instanceof ItemTool
            || item instanceof ItemShears
            || item instanceof ItemFlintAndSteel) return HANDHELD;
        return null;
    }

    private static ToolGrip registeredFamily(ItemStack stack) {
        for (Class<?> type = stack.getItem()
            .getClass(); type != null; type = type.getSuperclass()) {
            var resolver = families.get(type);
            if (resolver == null) continue;
            try {
                ToolGrip grip = resolver.apply(stack);
                if (grip != null) return grip;
            } catch (RuntimeException ex) {
                families.remove(type);
                GTNRLib.LOG.error("Disabled failing tool family resolver for {}", type.getName(), ex);
            }
        }
        return null;
    }

    private static Set<String> harvestClasses(ItemStack stack) {
        // A foreign item can require NBT that is absent on a creative or broken stack.
        try {
            var classes = stack.getItem()
                .getToolClasses(stack);
            if (classes != null) return classes;
        } catch (RuntimeException ex) {
            if (reportedItems.add(stack.getItem()))
                GTNRLib.LOG.warn("Could not read harvest classes for {}", stack.getItem(), ex);
        }
        return Set.of();
    }
}
