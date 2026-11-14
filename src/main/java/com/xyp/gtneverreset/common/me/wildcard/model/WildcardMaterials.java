package com.xyp.gtneverreset.common.me.wildcard.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.glodblock.github.common.item.ItemFluidDrop;

import bartworks.system.material.Werkstoff;
import bartworks.system.material.WerkstoffLoader;
import gregtech.api.enchants.EnchantmentRadioactivity;
import gregtech.api.enums.FluidState;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.SubTag;
import gregtech.api.interfaces.IOreMaterial;
import gregtech.api.objects.ItemData;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gtPlusPlus.core.material.Material;
import gtPlusPlus.core.material.state.MaterialState;

public final class WildcardMaterials {

    private static final Map<String, Materials> MATERIAL_NAME_CACHE = new ConcurrentHashMap<>();
    private static volatile List<String> cachedExpandableNames;
    private static volatile int cachedOreNameCount = -1;

    /** 判断材料是否为可用的真实材料（排除 _NULL / Empty）。 */
    public static boolean isRealMaterial(Materials material) {
        return material != null && material != Materials._NULL
            && material != Materials.Empty
            && material.mName != null
            && !material.mName.isEmpty();
    }

    /** Ore-dictionary families such as AnyCopper are matching aliases, not concrete material axes. */
    public static boolean isExpandableMaterial(Materials material) {
        // Netherite parts use dedicated arc furnace recipes instead of standard material forming.
        return isRealMaterial(material) && material.mUnifiable && material != Materials.Netherite;
    }

    /** 按名（大小写不敏感）查找材料，找不到返回 {@link Materials#_NULL}。 */
    public static Materials findByName(String name) {
        if (name == null || name.isEmpty()) return Materials._NULL;
        String normalized = name.toLowerCase(Locale.ROOT);
        Materials cached = MATERIAL_NAME_CACHE.get(normalized);
        if (cached != null) return cached;
        for (Materials material : Materials.getAll()) {
            if (isRealMaterial(material)) {
                MATERIAL_NAME_CACHE.putIfAbsent(material.mName.toLowerCase(Locale.ROOT), material);
            }
        }
        Materials material = MATERIAL_NAME_CACHE.get(normalized);
        if (material == null) {
            MATERIAL_NAME_CACHE.putIfAbsent(normalized, Materials._NULL);
            return Materials._NULL;
        }
        return material;
    }

    /** Preserve GT identities on name collisions, but recover the native BartWorks object for its own bridge. */
    public static IOreMaterial findOreMaterial(String name) {
        if (name == null || name.isEmpty()) return null;
        Materials gt = findByName(name);
        if (isRealMaterial(gt)) {
            Werkstoff werkstoff = werkstoffOf(gt);
            return werkstoff == null ? gt : werkstoff;
        }
        IOreMaterial material = IOreMaterial.findMaterial(name);
        return material instanceof Materials ? null : material;
    }

    /** Use registered stacks: doGenerateItem rejects valid forms whose generation bits are zero. */
    public static ItemStack makePrefixStack(OrePrefixes prefix, Materials material, int amount) {
        if (prefix == null || !isRealMaterial(material) || amount <= 0) return null;
        ItemStack stack = material.getPart(prefix, amount);
        if (stack == null || stack.getItem() == null) return null;
        stack.stackSize = amount;
        return stack;
    }

    /** Uses registered ore forms for addon materials absent from GT's Materials registry. */
    public static ItemStack makePrefixStack(OrePrefixes prefix, String materialName, int amount) {
        if (prefix == null || materialName == null || materialName.isEmpty() || amount <= 0) return null;
        Materials material = findByName(materialName);
        if (isRealMaterial(material)) return makePrefixStack(prefix, material, amount);
        List<ItemStack> ores = OreDictionary.getOres(prefix.name() + materialName, false);
        // BartWorks getPart can construct a stack even for disabled forms; require an actual ore registration.
        IOreMaterial nativeMaterial = findOreMaterial(materialName);
        if (!ores.isEmpty() && nativeMaterial != null) {
            ItemStack part = nativeMaterial.getPart(prefix, amount);
            if (part != null && part.getItem() != null) {
                for (ItemStack ore : ores) {
                    if (ore != null && OreDictionary.itemMatches(ore, part, false)) return part;
                }
            }
        }
        for (ItemStack ore : ores) {
            if (ore != null && ore.getItem() != null) {
                ItemStack stack = ore.copy();
                stack.stackSize = amount;
                return stack;
            }
        }
        return null;
    }

    /** GT materials followed by addon materials identified by a registered GT ore prefix. */
    public static synchronized List<String> expandableMaterialNames() {
        String[] oreNames = OreDictionary.getOreNames();
        if (cachedExpandableNames != null && cachedOreNameCount == oreNames.length) return cachedExpandableNames;
        Map<String, String> names = new LinkedHashMap<>();
        for (Materials material : Materials.getAll()) {
            if (isExpandableMaterial(material)) names.put(material.mName.toLowerCase(Locale.ROOT), material.mName);
        }
        for (String oreName : oreNames) {
            OrePrefixes prefix = prefixOfOreName(oreName);
            if (prefix == null) continue;
            String materialName = oreName.substring(
                prefix.name()
                    .length());
            if (materialName.isEmpty() || isRealMaterial(findByName(materialName))) continue;
            names.putIfAbsent(materialName.toLowerCase(Locale.ROOT), materialName);
        }
        cachedOreNameCount = oreNames.length;
        cachedExpandableNames = Collections.unmodifiableList(new ArrayList<>(names.values()));
        return cachedExpandableNames;
    }

    /**
     * 材料 + 流体状态 → AE2FC 的 ItemFluidDrop（CPU 通过 instanceof 识别流体请求）。找不到返回 null。
     */
    public static ItemStack makeFluidStack(FluidState state, Materials material, long amount) {
        if (state == null || !isRealMaterial(material) || amount <= 0) return null;
        FluidStack fluid = getMaterialFluid(material, state, amount);
        if (fluid == null || fluid.getFluid() == null) return null;
        return ItemFluidDrop.newStack(fluid);
    }

    /** Read registered native fluids first; retain the naming fallback for other ore-dictionary addons. */
    public static ItemStack makeFluidStack(FluidState state, String materialName, long amount) {
        if (state == null || materialName == null || materialName.isEmpty() || amount <= 0) return null;
        IOreMaterial material = findOreMaterial(materialName);
        if (material != null) {
            Fluid fluid = nativeFluid(material, state);
            return fluid == null ? null
                : ItemFluidDrop.newStack(new FluidStack(fluid, (int) Math.min(Integer.MAX_VALUE, amount)));
        }
        String prefix;
        switch (state) {
            case MOLTEN:
                prefix = "molten.";
                break;
            case PLASMA:
                prefix = "plasma.";
                break;
            case GAS:
                prefix = "gas.";
                break;
            case LIQUID:
            default:
                prefix = "liquid.";
                break;
        }
        Fluid fluid = FluidRegistry.getFluid(prefix + materialName.toLowerCase(Locale.ROOT));
        if (fluid == null) return null;
        return ItemFluidDrop.newStack(new FluidStack(fluid, (int) Math.min(Integer.MAX_VALUE, amount)));
    }

    /**
     * 显示用：若是 AE2FC 的 ItemFluidDrop，转成 GTNH 原生流体显示物品（正确的流体图标）；否则原样返回。
     * 仅用于 GUI 显示，不能用于展开样板（样板需要 ItemFluidDrop）。
     */
    public static ItemStack toDisplayStack(ItemStack stack) {
        if (stack == null) return null;
        FluidStack fluid = ItemFluidDrop.getFluidStack(stack);
        if (fluid != null && fluid.getFluid() != null) {
            ItemStack display = GTUtility.getFluidDisplayStack(fluid, true);
            if (display != null) {
                display.stackSize = 1;
                return display;
            }
        }
        return stack;
    }

    /** 材料是否有指定状态的流体。 */
    public static boolean hasFluid(Materials material, FluidState state) {
        if (material == null || state == null) return false;
        switch (state) {
            case MOLTEN:
                return material.mStandardMoltenFluid != null;
            case PLASMA:
                return material.mPlasma != null;
            case GAS:
                return material.mGas != null;
            case LIQUID:
            default:
                return material.mFluid != null;
        }
    }

    /** 按名解析属性，找不到返回 null。 */
    public static Property findProperty(String name) {
        if (name == null || name.isEmpty()) return null;
        String serializedName = name.trim()
            .toUpperCase(Locale.ROOT);
        for (Property property : Property.values()) {
            if (property.serializedName.equals(serializedName)) return property;
        }
        for (Property property : Property.values()) {
            if (property == Property.Metal || property == Property.ToolHead) continue;
            if (property.displayName()
                .equalsIgnoreCase(name.trim())) return property;
        }
        return null;
    }

    /** 列出某材料实际拥有的属性（对齐原版：拖入示例物品后只在它真有的属性里选）。 */
    public static List<Property> propertiesOf(Materials material) {
        return propertiesOf(isRealMaterial(material) ? material.mName : null);
    }

    public static List<Property> propertiesOf(String materialName) {
        List<Property> result = new ArrayList<>();
        if (materialName == null || materialName.isEmpty()) return result;
        for (Property property : Property.values()) {
            if (property == Property.Metal || property == Property.ToolHead) continue;
            if (property.test(materialName)) result.add(property);
        }
        return result;
    }

    public static List<SubTag> subTagChoices() {
        List<SubTag> tags = new ArrayList<>(SubTag.sSubTags.values());
        tags.sort(Comparator.comparing(tag -> tag.mName));
        return tags;
    }

    public static List<SubTag> subTagsOf(Materials material) {
        return subTagsOf(isRealMaterial(material) ? material.mName : null);
    }

    public static List<SubTag> subTagsOf(String materialName) {
        List<SubTag> result = new ArrayList<>();
        for (SubTag tag : subTagChoices()) {
            if (hasSubTag(materialName, tag)) result.add(tag);
        }
        return result;
    }

    public static boolean hasSubTag(Materials material, SubTag tag) {
        return isRealMaterial(material) && hasSubTag(material.mName, tag);
    }

    public static boolean hasSubTag(String materialName, SubTag tag) {
        if (tag == null) return false;
        IOreMaterial material = findOreMaterial(materialName);
        if (material != null && material.contains(tag)) return true;
        Materials gt = findByName(materialName);
        return isRealMaterial(gt) && gt.contains(tag);
    }

    /** 按名解析 SubTag，找不到返回 null。 */
    public static SubTag findSubTag(String name) {
        if (name == null || name.isEmpty()) return null;
        for (SubTag tag : SubTag.sSubTags.values()) {
            if (tag.mName.equalsIgnoreCase(name.trim())) return tag;
        }
        return null;
    }

    /** 按名解析 OrePrefix，找不到返回 null。 */
    public static OrePrefixes findPrefix(String name) {
        if (name == null || name.isEmpty()) return null;
        OrePrefixes exact = OrePrefixes.getPrefix(name.trim());
        if (exact != null) return exact;
        for (OrePrefixes prefix : OrePrefixes.VALUES) {
            if (prefix.name()
                .equalsIgnoreCase(name.trim())) return prefix;
        }
        return null;
    }

    /** 从一个物品解析出它的 GT (前缀, 材料) 关联；无法识别时两者为 null。 */
    public static PrefixMaterial parseItem(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return PrefixMaterial.EMPTY;
        ItemData data = GTOreDictUnificator.getAssociation(stack);
        if (data != null && data.hasValidPrefixMaterialData()) {
            Materials material = data.mMaterial.mMaterial;
            if (isRealMaterial(material)) return new PrefixMaterial(data.mPrefix, material);
        }
        for (int oreId : OreDictionary.getOreIDs(stack)) {
            String oreName = OreDictionary.getOreName(oreId);
            OrePrefixes prefix = prefixOfOreName(oreName);
            if (prefix != null) {
                String materialName = oreName.substring(
                    prefix.name()
                        .length());
                IOreMaterial nativeMaterial = findOreMaterial(materialName);
                String name = nativeMaterial == null ? materialName : nativeMaterial.getInternalName();
                return new PrefixMaterial(prefix, findByName(name), name);
            }
        }
        return PrefixMaterial.EMPTY;
    }

    /** 常用属性名列表（供 GUI 下拉），保持插入顺序。 */
    public static Map<String, Property> propertyChoices() {
        Map<String, Property> map = new LinkedHashMap<>();
        for (Property property : Property.values()) {
            if (property == Property.Metal || property == Property.ToolHead) continue;
            map.put(property.displayName(), property);
        }
        return map;
    }

    private static OrePrefixes prefixOfOreName(String oreName) {
        if (oreName == null) return null;
        OrePrefixes best = null;
        for (OrePrefixes prefix : OrePrefixes.VALUES) {
            String name = prefix.name();
            if (!name.isEmpty() && oreName.startsWith(name)
                && oreName.length() > name.length()
                && (best == null || name.length() > best.name()
                    .length()))
                best = prefix;
        }
        return best;
    }

    private static FluidStack getMaterialFluid(Materials material, FluidState state, long amount) {
        switch (state) {
            case MOLTEN:
                return material.getMolten(amount);
            case PLASMA:
                return material.getPlasma(amount);
            case GAS:
                return material.getGas(amount);
            case LIQUID:
            default:
                return material.getFluid(amount);
        }
    }

    /**
     * Resolves the original BartWorks material through its registered bridge identity. Bridge Materials do not
     * copy all generation flags or SubTags; reading only their GT fields would lose those capabilities.
     * Uses the existing name index without scanning the material registry on every filter evaluation.
     *
     * @param material GT material used by the expansion model
     * @return the matching Werkstoff, or null for ordinary GT materials and unbound bridges
     */
    private static Werkstoff werkstoffOf(Materials material) {
        if (!isRealMaterial(material)) return null;
        Werkstoff werkstoff = Werkstoff.werkstoffVarNameHashMap.get(material.mName);
        return werkstoff != null && werkstoff.getBridgeMaterial() == material ? werkstoff : null;
    }

    private static boolean hasNativeForm(IOreMaterial material, String name, OrePrefixes... prefixes) {
        for (OrePrefixes prefix : prefixes) {
            if (material != null && material.generatesPrefix(prefix)) return true;
            if (makePrefixStack(prefix, name, 1) != null) return true;
        }
        return false;
    }

    private static Fluid nativeFluid(IOreMaterial material, FluidState state) {
        if (material instanceof Materials gt) {
            FluidStack stack = getMaterialFluid(gt, state, 1);
            return stack == null ? null : stack.getFluid();
        }
        if (material instanceof Werkstoff werkstoff) {
            Fluid fluid = switch (state) {
                case MOLTEN -> WerkstoffLoader.molten.get(werkstoff);
                case GAS -> werkstoff.getStats()
                    .isGas() ? WerkstoffLoader.fluids.get(werkstoff) : null;
                case LIQUID -> !werkstoff.getStats()
                    .isGas() ? WerkstoffLoader.fluids.get(werkstoff) : null;
                default -> null;
            };
            return fluid != null ? fluid : nativeFluid(werkstoff.getGTMaterial(), state);
        }
        if (material instanceof Material gtpp) {
            MaterialState nativeState = gtpp.getState();
            return switch (state) {
                case MOLTEN -> nativeState == MaterialState.SOLID || nativeState == MaterialState.ORE ? gtpp.getFluid()
                    : null;
                case LIQUID -> nativeState == MaterialState.LIQUID || nativeState == MaterialState.PURE_LIQUID
                    ? gtpp.getFluid()
                    : null;
                case GAS -> nativeState == MaterialState.GAS || nativeState == MaterialState.PURE_GAS ? gtpp.getFluid()
                    : null;
                case PLASMA -> gtpp.getPlasma() != null ? gtpp.getPlasma()
                    : nativeState == MaterialState.PLASMA ? gtpp.getFluid() : null;
                default -> null;
            };
        }
        return null;
    }

    private static boolean requiresBlastFurnace(IOreMaterial material) {
        if (material instanceof Werkstoff werkstoff) return werkstoff.getStats()
            .isBlastFurnace();
        if (material instanceof Material gtpp) return gtpp.requiresBlastFurnace();
        return material instanceof Materials gt && gt.mBlastFurnaceRequired;
    }

    private static boolean isRadioactive(IOreMaterial material) {
        if (material instanceof Werkstoff werkstoff) return werkstoff.getStats()
            .isRadioactive();
        if (material instanceof Material gtpp) return gtpp.isRadioactive;
        return material instanceof Materials gt
            && (gt.mArmorEnchantment instanceof EnchantmentRadioactivity && gt.mArmorEnchantmentLevel > 0
                || gt.mToolEnchantment instanceof EnchantmentRadioactivity && gt.mToolEnchantmentLevel > 0);
    }

    /**
     * GT5U has no GTCEu PolymerProperty. This compatibility list identifies known GT5U polymer materials;
     * arbitrary addon polymers need an explicit mapping and cannot be inferred from COMPOUND or texture sets.
     * NO_SMASHING is not used because it also includes paper and other non-polymer materials.
     */
    private static boolean isPolymer(Materials material) {
        return material == Materials.Polyethylene || material == Materials.Polytetrafluoroethylene
            || material == Materials.PolyvinylChloride
            || material == Materials.Polybenzimidazole
            || material == Materials.Polycaprolactam
            || material == Materials.PolyphenyleneSulfide
            || material == Materials.Polystyrene
            || material == Materials.PolyvinylAcetate
            || material == Materials.Polydimethylsiloxane
            || material == Materials.Rubber
            || material == Materials.RubberRaw
            || material == Materials.RubberSilicone
            || material == Materials.StyreneButadieneRubber
            || material == Materials.RawStyreneButadieneRubber
            || material == Materials.RadoxPolymer
            || material == Materials.Kevlar
            || material == Materials.PolyurethaneResin
            || material == Materials.Epoxid
            || material == Materials.EpoxidFiberReinforced;
    }

    /** 支持的材料属性（用于 property 过滤）。 */
    public enum Property {

        Dust("DUST"),
        Metal("METAL"),
        Gem("GEM"),
        Ore("ORE"),
        Cell("CELL"),
        Plasma("PLASMA"),
        ToolHead("TOOL_HEAD"),
        Gear("GEAR"),
        Fluid("FLUID"),
        Gas("GAS"),
        Polymer("POLYMER"),
        Ingot("INGOT"),
        Tool("TOOL"),
        FluidPipe("FLUID_PIPE"),
        // #tr gtnr.gui.wildcardpattern.property.gtplusplus
        // # GT++ materials
        // # zh_CN GT++ 材料
        GTPlusPlus("GT_PLUS_PLUS", "gtnr.gui.wildcardpattern.property.gtplusplus"),
        // #tr gtnr.gui.wildcardpattern.property.blast_furnace
        // # Requires blast furnace
        // # zh_CN 需要高炉
        BlastFurnace("BLAST_FURNACE", "gtnr.gui.wildcardpattern.property.blast_furnace"),
        // #tr gtnr.gui.wildcardpattern.property.radioactive
        // # Radioactive
        // # zh_CN 放射性
        Radioactive("RADIOACTIVE", "gtnr.gui.wildcardpattern.property.radioactive"),
        // #tr gtnr.gui.wildcardpattern.property.solid
        // # Solid form
        // # zh_CN 固态
        Solid("SOLID", "gtnr.gui.wildcardpattern.property.solid"),
        // #tr gtnr.gui.wildcardpattern.property.liquid
        // # Liquid form
        // # zh_CN 液态
        Liquid("LIQUID", "gtnr.gui.wildcardpattern.property.liquid"),
        // #tr gtnr.gui.wildcardpattern.property.toxic
        // # Toxic (BW)
        // # zh_CN 有毒（BW）
        Toxic("TOXIC", "gtnr.gui.wildcardpattern.property.toxic"),
        // #tr gtnr.gui.wildcardpattern.property.electrolysis
        // # Electrolysis enabled (BW)
        // # zh_CN 启用电解（BW）
        Electrolysis("ELECTROLYSIS", "gtnr.gui.wildcardpattern.property.electrolysis"),
        // #tr gtnr.gui.wildcardpattern.property.centrifuge
        // # Centrifuge enabled (BW)
        // # zh_CN 启用离心（BW）
        Centrifuge("CENTRIFUGE", "gtnr.gui.wildcardpattern.property.centrifuge"),
        // #tr gtnr.gui.wildcardpattern.property.bw_material
        // # Material (BW)
        // # zh_CN 材料（BW）
        BWMaterial("BW_MATERIAL", "gtnr.gui.wildcardpattern.property.bw_material"),
        // #tr gtnr.gui.wildcardpattern.property.bw_compound
        // # Compound (BW)
        // # zh_CN 化合物（BW）
        BWCompound("BW_COMPOUND", "gtnr.gui.wildcardpattern.property.bw_compound"),
        // #tr gtnr.gui.wildcardpattern.property.bw_mixture
        // # Mixture (BW)
        // # zh_CN 混合物（BW）
        BWMixture("BW_MIXTURE", "gtnr.gui.wildcardpattern.property.bw_mixture"),
        // #tr gtnr.gui.wildcardpattern.property.bw_biological
        // # Biological (BW)
        // # zh_CN 生物质（BW）
        BWBiological("BW_BIOLOGICAL", "gtnr.gui.wildcardpattern.property.bw_biological"),
        // #tr gtnr.gui.wildcardpattern.property.bw_element
        // # Element (BW)
        // # zh_CN 元素（BW）
        BWElement("BW_ELEMENT", "gtnr.gui.wildcardpattern.property.bw_element"),
        // #tr gtnr.gui.wildcardpattern.property.bw_isotope
        // # Isotope (BW)
        // # zh_CN 同位素（BW）
        BWIsotope("BW_ISOTOPE", "gtnr.gui.wildcardpattern.property.bw_isotope"),
        // #tr gtnr.gui.wildcardpattern.property.bw_undefined
        // # Undefined type (BW)
        // # zh_CN 未定义类型（BW）
        BWUndefined("BW_UNDEFINED", "gtnr.gui.wildcardpattern.property.bw_undefined");

        public final String serializedName;
        private final String translationKey;

        Property(String serializedName) {
            this(serializedName, null);
        }

        Property(String serializedName, String translationKey) {
            this.serializedName = serializedName;
            this.translationKey = translationKey;
        }

        @Override
        public String toString() {
            return serializedName;
        }

        public String displayName() {
            if (translationKey != null) return StatCollector.translateToLocal(translationKey);
            if (this == Metal) return "ingot";
            if (this == ToolHead) return "tool";
            return serializedName.toLowerCase(Locale.ROOT);
        }

        public boolean test(String materialName) {
            if (materialName == null || materialName.isEmpty()) return false;
            IOreMaterial material = findOreMaterial(materialName);
            Materials gt = findByName(materialName);
            Werkstoff werkstoff = material instanceof Werkstoff bw ? bw : null;
            switch (this) {
                case GTPlusPlus:
                    return material instanceof Material;
                case Dust:
                    return isRealMaterial(gt) && gt.hasDustItems()
                        || hasNativeForm(material, materialName, OrePrefixes.dust);
                case Metal:
                case Ingot:
                    return isRealMaterial(gt) && gt.hasMetalItems()
                        || hasNativeForm(material, materialName, OrePrefixes.ingot);
                case Gem:
                    return isRealMaterial(gt) && gt.hasGemItems()
                        || hasNativeForm(material, materialName, OrePrefixes.gem);
                case Ore:
                    return isRealMaterial(gt) && gt.hasOresItems()
                        || hasNativeForm(material, materialName, OrePrefixes.ore);
                case Cell:
                    return isRealMaterial(gt) && gt.hasCell()
                        || hasNativeForm(material, materialName, OrePrefixes.cell);
                case Plasma:
                    return isRealMaterial(gt) && gt.hasPlasma() || nativeFluid(material, FluidState.PLASMA) != null
                        || material == null && makeFluidStack(FluidState.PLASMA, materialName, 1) != null;
                case ToolHead:
                case Tool:
                    return isRealMaterial(gt) && gt.hasToolHeadItems() || hasNativeForm(
                        material,
                        materialName,
                        OrePrefixes.toolHeadHammer,
                        OrePrefixes.toolHeadWrench,
                        OrePrefixes.toolHeadSaw);
                case Gear:
                    return isRealMaterial(gt) && gt.hasGearItems()
                        || hasNativeForm(material, materialName, OrePrefixes.gearGt);
                case Fluid:
                    return nativeFluid(material, FluidState.LIQUID) != null
                        || nativeFluid(material, FluidState.MOLTEN) != null
                        || material == null && (makeFluidStack(FluidState.LIQUID, materialName, 1) != null
                            || makeFluidStack(FluidState.MOLTEN, materialName, 1) != null);
                case Gas:
                    return nativeFluid(material, FluidState.GAS) != null
                        || material == null && makeFluidStack(FluidState.GAS, materialName, 1) != null;
                case Polymer:
                    return isPolymer(gt);
                case FluidPipe:
                    return hasNativeForm(
                        material,
                        materialName,
                        OrePrefixes.pipeTiny,
                        OrePrefixes.pipeSmall,
                        OrePrefixes.pipeMedium,
                        OrePrefixes.pipeLarge,
                        OrePrefixes.pipeHuge);
                case BlastFurnace:
                    return requiresBlastFurnace(material);
                case Radioactive:
                    return isRadioactive(material);
                case Solid:
                    if (material instanceof Material gtpp)
                        return gtpp.getState() == MaterialState.SOLID || gtpp.getState() == MaterialState.ORE;
                    return hasNativeForm(material, materialName, OrePrefixes.dust, OrePrefixes.ingot, OrePrefixes.gem);
                case Liquid:
                    return nativeFluid(material, FluidState.LIQUID) != null
                        || material == null && makeFluidStack(FluidState.LIQUID, materialName, 1) != null;
                case Toxic:
                    return werkstoff != null && werkstoff.getStats()
                        .isToxic();
                case Electrolysis:
                    return werkstoff != null && werkstoff.getStats()
                        .isElektrolysis();
                case Centrifuge:
                    return werkstoff != null && werkstoff.getStats()
                        .isCentrifuge();
                case BWMaterial:
                    return werkstoff != null && werkstoff.getType() == Werkstoff.Types.MATERIAL;
                case BWCompound:
                    return werkstoff != null && werkstoff.getType() == Werkstoff.Types.COMPOUND;
                case BWMixture:
                    return werkstoff != null && werkstoff.getType() == Werkstoff.Types.MIXTURE;
                case BWBiological:
                    return werkstoff != null && werkstoff.getType() == Werkstoff.Types.BIOLOGICAL;
                case BWElement:
                    return werkstoff != null && werkstoff.getType() == Werkstoff.Types.ELEMENT;
                case BWIsotope:
                    return werkstoff != null && werkstoff.getType() == Werkstoff.Types.ISOTOPE;
                case BWUndefined:
                    return werkstoff != null && werkstoff.getType() == Werkstoff.Types.UNDEFINED;
                default:
                    return false;
            }
        }

        public boolean test(Materials material) {
            return isRealMaterial(material) && test(material.mName);
        }
    }

    /** (前缀, 材料) 解析结果。 */
    public static final class PrefixMaterial {

        public static final PrefixMaterial EMPTY = new PrefixMaterial(null, null);

        public final OrePrefixes prefix;
        public final Materials material;
        public final String materialName;

        public PrefixMaterial(OrePrefixes prefix, Materials material) {
            this(prefix, material, isRealMaterial(material) ? material.mName : null);
        }

        private PrefixMaterial(OrePrefixes prefix, Materials material, String materialName) {
            this.prefix = prefix;
            this.material = material;
            this.materialName = materialName;
        }
    }
}
