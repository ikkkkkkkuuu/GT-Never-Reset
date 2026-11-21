package com.xyp.gtneverreset.common.item.items;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CredentialModel;
import com.xyp.gtneverreset.common.item.ChaosMetaItem;
import com.xyp.gtneverreset.loader.ItemLoader;

/** Capacity is selected by metadata; the route binding is stored on each individual stack. */
public final class ChaosRouteItem extends ChaosMetaItem {

    public static final int MAX_META = 5;
    private static final String DISPLAY_TAG_MASK_NBT_KEY = "bottledChaosDisplayTagMask";

    private final IIcon[] boundIcons = new IIcon[MAX_META + 1];

    public ChaosRouteItem() {
        super("BottledChaosCredential", "bottledchaos.credential");
        setMaxStackSize(1);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (isBound(stack) && !stack.getTagCompound()
            .hasKey(DISPLAY_TAG_MASK_NBT_KEY)) {
            return StatCollector
                // #tr item.gtnr.bottleinchaos.credential.bound.name
                // # Bottle in Chaos Credential %s
                // # zh_CN 混沌中瓶凭证 %s
                .translateToLocalFormatted("item.gtnr.bottleinchaos.credential.bound.name", getCapacity(stack));
        }
        // #tr item.gtnr.bottleinchaos.credential.0.name
        // # Bottle in Chaos Credential Frame 1
        // # zh_CN 混沌中瓶凭证框架 1
        // #tr item.gtnr.bottleinchaos.credential.1.name
        // # Bottle in Chaos Credential Frame 4
        // # zh_CN 混沌中瓶凭证框架 4
        // #tr item.gtnr.bottleinchaos.credential.2.name
        // # Bottle in Chaos Credential Frame 16
        // # zh_CN 混沌中瓶凭证框架 16
        // #tr item.gtnr.bottleinchaos.credential.3.name
        // # Bottle in Chaos Credential Frame 64
        // # zh_CN 混沌中瓶凭证框架 64
        // #tr item.gtnr.bottleinchaos.credential.4.name
        // # Bottle in Chaos Credential Frame 256
        // # zh_CN 混沌中瓶凭证框架 256
        // #tr item.gtnr.bottleinchaos.credential.5.name
        // # Bottle in Chaos Credential Frame 1024
        // # zh_CN 混沌中瓶凭证框架 1024
        return StatCollector.translateToLocal("item.gtnr.bottleinchaos.credential." + stack.getItemDamage() + ".name");
    }

    public static int getCapacity(ItemStack stack) {
        if (stack == null || stack.getItem() != ItemLoader.chaosRoute || stack.stackSize != 1) return 0;
        int meta = stack.getItemDamage();
        return meta >= 0 && meta <= MAX_META ? 1 << (2 * meta) : 0;
    }

    public static ItemStack getRecipeFamily(ItemStack stack) {
        return isBound(stack) ? stack.copy() : null;
    }

    private static final String CUSTOM_PATTERNS = "chaosCustomPatterns";

    public static ItemStack createPatternAuthorization(NBTTagList patterns) {
        ItemStack stack = createEmptyFrame(MAX_META);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(CUSTOM_PATTERNS, patterns);
        stack.setTagCompound(tag);
        return stack;
    }

    public static boolean hasPatternAuthorization(ItemStack stack) {
        return getCapacity(stack) > 0 && stack.hasTagCompound()
            && stack.getTagCompound()
                .hasKey(CUSTOM_PATTERNS, 9)
            && stack.getTagCompound()
                .getTagList(CUSTOM_PATTERNS, 10)
                .tagCount() > 0;
    }

    /** Old paid AE jobs stored the name and weight of whichever preset occupied bit zero. */
    public static void migratePatternAuthorization(ItemStack stack) {
        if (hasPatternAuthorization(stack)) {
            stack.getTagCompound()
                .removeTag(CredentialModel.TAGS_NBT_KEY);
            stack.getTagCompound()
                .removeTag(CredentialModel.OCCUPIED_WEIGHT_NBT_KEY);
        }
    }

    /** A custom graph has exactly one local route, independent of the global preset catalog. */
    public static long executionMask(ItemStack stack) {
        return hasPatternAuthorization(stack) ? 1L : getRouteMask(stack);
    }

    public static int authorizationWeight(ItemStack stack) {
        return hasPatternAuthorization(stack) ? 1 : getRequiredWeight(getRouteMask(stack));
    }

    public static ItemStack createEmptyFrame(int meta) {
        return meta >= 0 && meta <= MAX_META ? new ItemStack(ItemLoader.chaosRoute, 1, meta) : null;
    }

    public static ItemStack createCredential(int meta, long routeMask, int occupiedWeight) {
        ItemStack stack = createEmptyFrame(meta);
        return setBinding(stack, routeMask, occupiedWeight) ? stack : null;
    }

    public static ItemStack createDisplayCredential(int meta, long displayTagMask) {
        ItemStack stack = createCredential(meta, displayTagMask, getRequiredWeight(displayTagMask));
        if (stack != null) stack.getTagCompound()
            .setLong(DISPLAY_TAG_MASK_NBT_KEY, displayTagMask);
        return stack;
    }

    public static boolean setBinding(ItemStack stack, long routeMask, int occupiedWeight) {
        int capacity = getCapacity(stack);
        if (capacity == 0 || (routeMask & ~CredentialModel.ALL_ROUTE_MASK) != 0
            || (routeMask == 0) != (occupiedWeight == 0)
            || occupiedWeight < getRequiredWeight(routeMask)
            || occupiedWeight > capacity) {
            return false;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (routeMask == 0) {
            if (tag != null) {
                tag.removeTag(CredentialModel.TAGS_NBT_KEY);
                tag.removeTag(CredentialModel.OCCUPIED_WEIGHT_NBT_KEY);
            }
            return true;
        }
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        CredentialModel.writeRouteMask(tag, routeMask);
        tag.setInteger(CredentialModel.OCCUPIED_WEIGHT_NBT_KEY, occupiedWeight);
        return true;
    }

    public static long getRouteMask(ItemStack stack) {
        return getCapacity(stack) == 0 ? 0 : CredentialModel.readRouteMask(stack.getTagCompound());
    }

    public static int getOccupiedWeight(ItemStack stack) {
        if (getCapacity(stack) == 0 || stack.getTagCompound() == null) return 0;
        return stack.getTagCompound()
            .getInteger(CredentialModel.OCCUPIED_WEIGHT_NBT_KEY);
    }

    public static int getRequiredWeight(long routeMask) {
        return CredentialModel.getRequiredWeight(routeMask);
    }

    public static boolean isBound(ItemStack stack) {
        if (hasPatternAuthorization(stack)) return true;
        int weight = getOccupiedWeight(stack);
        long mask = getRouteMask(stack);
        return mask != 0 && weight >= getRequiredWeight(mask) && weight <= getCapacity(stack);
    }

    @Override
    public void registerIcons(IIconRegister register) {
        String path = GTNeverReset.MODID + ":BottledChaosCredential/";
        for (int meta : usedMetaIds) {
            iconMap.put(meta, register.registerIcon(path + "credential_t" + meta + "_blank"));
            boundIcons[meta] = register.registerIcon(path + "credential_t" + meta + "_bound");
        }
        itemIcon = iconMap.get(0);
    }

    @Override
    public IIcon getIconIndex(ItemStack stack) {
        return isBound(stack) ? boundIcons[stack.getItemDamage()] : getIconFromDamage(stack.getItemDamage());
    }

    @Override
    public IIcon getIcon(ItemStack stack, int pass) {
        return getIconIndex(stack);
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean advanced) {
        lines.add(
            StatCollector.translateToLocalFormatted(
                // #tr item.gtnr.bottleinchaos.credential.tooltip.capacity
                // # Capacity: %s/%s
                // # zh_CN 容量: %s/%s
                "item.gtnr.bottleinchaos.credential.tooltip.capacity",
                getOccupiedWeight(stack),
                getCapacity(stack)));
        if (!isBound(stack)) {
            // #tr item.gtnr.bottleinchaos.credential.tooltip.empty
            // # Legacy item; select routes directly in a processing group hatch. No credential is required.
            // # zh_CN 旧版物品；请直接在加工组仓选择产线，无需凭证。
            lines.add(StatCollector.translateToLocal("item.gtnr.bottleinchaos.credential.tooltip.empty"));
        } else {
            long displayTagMask = stack.getTagCompound()
                .getLong(DISPLAY_TAG_MASK_NBT_KEY);
            if (displayTagMask == 0) {
                for (String key : getRouteKeyList(stack)) lines.add(StatCollector.translateToLocal(key));
            } else {
                // #tr item.gtnr.bottleinchaos.credential.tooltip.example.any_tag_1
                // # Any Tag 1
                // # zh_CN 任意标签 1

                // #tr item.gtnr.bottleinchaos.credential.tooltip.example.any_tag_2
                // # Any Tag 2
                // # zh_CN 任意标签 2
                for (int index = 0; index < 2; index++) {
                    if ((displayTagMask & (1L << index)) != 0) {
                        lines.add(
                            StatCollector.translateToLocal(
                                "item.gtnr.bottleinchaos.credential.tooltip.example.any_tag_" + (index + 1)));
                    }
                }
            }
        }
    }

    /** Grouped names for compact summaries; the item tooltip retains every tag in registration order. */
    public static String getDisplayRouteKeys(ItemStack stack) {
        if (hasPatternAuthorization(stack)) return "AE";
        return String.join(", ", CredentialModel.groupedTranslationKeys(getRouteMask(stack)));
    }

    public static List<String> getRouteKeyList(ItemStack stack) {
        // #tr gtnr.bottleinchaos.shared.route.wood
        // # Wood Chemical Processing
        // # zh_CN 木化

        // #tr gtnr.bottleinchaos.shared.route.petrochemical
        // # Petrochemical Processing
        // # zh_CN 石化

        // #tr gtnr.bottleinchaos.shared.route.honeycomb
        // # Honeycomb Processing
        // # zh_CN 蜂窝处理

        // #tr gtnr.bottleinchaos.shared.route.titanium
        // # Titanium
        // # zh_CN 钛

        // #tr gtnr.bottleinchaos.shared.route.tungsten
        // # Tungsten
        // # zh_CN 钨

        // #tr gtnr.bottleinchaos.shared.route.indium
        // # Indium
        // # zh_CN 铟

        // #tr gtnr.bottleinchaos.shared.route.platinum_metallic_powder
        // # Platinum Metallic Powder
        // # zh_CN 铂金属粉

        // #tr gtnr.bottleinchaos.shared.route.palladium_metallic_powder
        // # Palladium Metallic Powder
        // # zh_CN 钯金属粉

        // #tr gtnr.bottleinchaos.shared.route.platinum_residue
        // # Platinum Residue Dust
        // # zh_CN 铂渣粉

        // #tr gtnr.bottleinchaos.shared.route.leach_residue
        // # Leach Residue Dust
        // # zh_CN 浸出渣粉

        // #tr gtnr.bottleinchaos.shared.route.rarest_metal_residue
        // # Rare Metal Residue Dust
        // # zh_CN 稀有金属渣粉

        // #tr gtnr.bottleinchaos.shared.route.iridium_metal_residue
        // # Iridium Metal Residue Dust
        // # zh_CN 铱金属渣粉

        // #tr gtnr.bottleinchaos.shared.route.crude_rhodium_metal
        // # Crude Rhodium Metal Dust
        // # zh_CN 粗制铑金属粉

        // #tr gtnr.bottleinchaos.shared.route.monazite
        // # Monazite
        // # zh_CN 独居石

        // #tr gtnr.bottleinchaos.shared.route.bastnasite
        // # Bastnasite
        // # zh_CN 氟碳镧铈

        // #tr gtnr.bottleinchaos.shared.route.cerium
        // # Cerium
        // # zh_CN 铈

        // #tr gtnr.bottleinchaos.shared.route.samarium
        // # Samarium
        // # zh_CN 钐

        // #tr gtnr.bottleinchaos.shared.route.nano_extraction
        // # Nano Resin Extraction
        // # zh_CN 纳米萃取

        // #tr gtnr.bottleinchaos.shared.route.naquadah
        // # Naquadah
        // # zh_CN 硅岩

        // #tr gtnr.bottleinchaos.shared.route.enriched_naquadah
        // # Enriched Naquadah
        // # zh_CN 富集硅岩

        // #tr gtnr.bottleinchaos.shared.route.naquadria
        // # Naquadria
        // # zh_CN 超能硅岩

        // #tr gtnr.bottleinchaos.shared.route.rocket_fuel
        // # Rocket Fuel
        // # zh_CN 火箭燃料

        // #tr gtnr.bottleinchaos.shared.route.naquadah_fuel_1
        // # Naquadah Fuel I
        // # zh_CN 硅岩燃料 I

        // #tr gtnr.bottleinchaos.shared.route.naquadah_fuel_2
        // # Naquadah Fuel II
        // # zh_CN 硅岩燃料 II

        // #tr gtnr.bottleinchaos.shared.route.naquadah_fuel_preparation
        // # Naquadah Fuel Precursors
        // # zh_CN 硅岩燃料前置

        // #tr gtnr.bottleinchaos.shared.route.naquadah_fuel_3
        // # Naquadah Fuel III
        // # zh_CN 硅岩燃料 III

        // #tr gtnr.bottleinchaos.shared.route.naquadah_fuel_4
        // # Naquadah Fuel IV
        // # zh_CN 硅岩燃料 IV

        // #tr gtnr.bottleinchaos.shared.route.naquadah_fuel_5
        // # Naquadah Fuel V
        // # zh_CN 硅岩燃料 V

        // #tr gtnr.bottleinchaos.shared.route.naquadah_fuel_6
        // # Naquadah Fuel VI
        // # zh_CN 硅岩燃料 VI

        // #tr gtnr.bottleinchaos.shared.route.fusion
        // # Fusion
        // # zh_CN 聚变

        // #tr gtnr.bottleinchaos.shared.route.ancient_debris
        // # Netherite
        // # zh_CN 下界合金

        // #tr gtnr.bottleinchaos.shared.route.isamill
        // # IsaMill Processing
        // # zh_CN 艾萨矿物处理

        // #tr gtnr.bottleinchaos.shared.route.crystalline_alloy_molten
        // # Molten Crystalline Alloy
        // # zh_CN 熔融晶化合金

        // #tr gtnr.bottleinchaos.shared.route.purified_water_1
        // # Grade 1 Purified Water
        // # zh_CN 1级净化水

        // #tr gtnr.bottleinchaos.shared.route.purified_water_2
        // # Grade 2 Purified Water
        // # zh_CN 2级净化水

        // #tr gtnr.bottleinchaos.shared.route.prismatic_acid
        // # Prismatic Acid
        // # zh_CN 海晶酸

        // #tr gtnr.bottleinchaos.shared.route.purified_water_3
        // # Grade 3 Purified Water
        // # zh_CN 3级净化水

        // #tr gtnr.bottleinchaos.shared.route.purified_water_4
        // # Grade 4 Purified Water
        // # zh_CN 4级净化水

        // #tr gtnr.bottleinchaos.shared.route.kevlar
        // # Kevlar
        // # zh_CN 凯芙拉

        // #tr gtnr.bottleinchaos.shared.route.super_heavy_radox
        // # Superheavy Radox
        // # zh_CN 超重拉多X

        // #tr gtnr.bottleinchaos.shared.route.heavy_radox
        // # Heavy Radox
        // # zh_CN 重拉多X

        // #tr gtnr.bottleinchaos.shared.route.light_radox
        // # Light Radox
        // # zh_CN 轻拉多X

        // #tr gtnr.bottleinchaos.shared.route.super_light_radox
        // # Superlight Radox
        // # zh_CN 超轻拉多X

        // #tr gtnr.bottleinchaos.shared.route.purified_water_5
        // # Grade 5 Purified Water
        // # zh_CN 5级净化水

        // #tr gtnr.bottleinchaos.shared.route.purified_water_6
        // # Grade 6 Purified Water
        // # zh_CN 6级净化水

        // #tr gtnr.bottleinchaos.shared.route.growth_medium_sterilized
        // # Sterile Growth Medium
        // # zh_CN 无菌培养基

        // #tr gtnr.bottleinchaos.shared.route.stem_cells
        // # Stem Cells
        // # zh_CN 干细胞

        // #tr gtnr.bottleinchaos.shared.route.seaweed_broth
        // # Seaweed Broth
        // # zh_CN 海藻基质

        // #tr gtnr.bottleinchaos.shared.route.tau_ceti_e_seaweed_extract
        // # Tau Ceti E Seaweed Extract
        // # zh_CN 鲸鱼座T星E藻类提取物

        // #tr gtnr.bottleinchaos.shared.route.bio_medium_sterilized
        // # Sterile Biological Medium
        // # zh_CN 灭菌生物培养基

        // #tr gtnr.bottleinchaos.shared.route.bio_cells
        // # Biological Cells
        // # zh_CN 生物细胞

        // #tr gtnr.bottleinchaos.shared.route.xenoxene
        // # Xenoxene Fractionation
        // # zh_CN 异氙分馏

        // #tr gtnr.bottleinchaos.shared.route.purified_water_7
        // # Grade 7 Purified Water
        // # zh_CN 7级净化水

        // #tr gtnr.bottleinchaos.shared.route.purified_water_8
        // # Grade 8 Purified Water
        // # zh_CN 8级净化水

        // #tr gtnr.bottleinchaos.shared.route.crystal_cycle
        // # Crystal Cycle
        // # zh_CN 晶体循环

        // #tr gtnr.bottleinchaos.shared.route.cerium_lutetium_garnet
        // # Cerium-doped Lutetium Aluminium Garnet
        // # zh_CN 掺铈镥铝石榴石

        // #tr gtnr.bottleinchaos.shared.route.tesseract
        // # Raw Tesseract
        // # zh_CN 粗制超立方体

        // #tr gtnr.bottleinchaos.shared.route.eternity
        // # Eternity
        // # zh_CN 永恒

        // #tr gtnr.bottleinchaos.shared.route.magmatter
        // # Magmatter
        // # zh_CN 磁物质

        // #tr gtnr.bottleinchaos.shared.route.phonon_medium
        // # Lossless Phonon Transfer Medium
        // # zh_CN 无损声子传输介质

        // #tr gtnr.bottleinchaos.shared.route.quark_gluon_plasma
        // # Quark-Gluon Plasma
        // # zh_CN 夸克胶子等离子体
        List<String> keys = new ArrayList<>();
        if (hasPatternAuthorization(stack)) return java.util.Collections.singletonList("AE");
        long mask = isBound(stack) ? getRouteMask(stack) : 0;
        for (int index : CredentialModel.displayOrder()) {
            if ((mask & (1L << index)) != 0) keys.add(CredentialModel.translationKey(index));
        }
        return keys;
    }
}
