package com.xyp.gtneverreset.common.me.wildcard.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.glodblock.github.common.item.ItemFluidDrop;

import bartworks.system.material.Werkstoff;
import gregtech.api.enums.FluidState;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.SubTag;
import gregtech.api.objects.ItemData;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;

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

    /** Use registered stacks: doGenerateItem rejects valid forms whose generation bits are zero. */
    public static ItemStack makePrefixStack(OrePrefixes prefix, Materials material, int amount) {
        if (prefix == null || !isRealMaterial(material) || amount <= 0) return null;
        ItemStack stack = GTOreDictUnificator.get(prefix, material, amount);
        if (stack == null || stack.getItem() == null) return null;
        stack.stackSize = amount;
        return stack;
    }

    /** Uses registered ore forms for addon materials absent from GT's Materials registry. */
    public static ItemStack makePrefixStack(OrePrefixes prefix, String materialName, int amount) {
        if (prefix == null || materialName == null || materialName.isEmpty() || amount <= 0) return null;
        Materials material = findByName(materialName);
        if (isRealMaterial(material)) return makePrefixStack(prefix, material, amount);
        List<ItemStack> ores = OreDictionary.getOres(prefix.name() + materialName);
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

    /** Resolves addon fluids by the standard GTNH fluid registry names. */
    public static ItemStack makeFluidStack(FluidState state, String materialName, long amount) {
        if (state == null || materialName == null || materialName.isEmpty() || amount <= 0) return null;
        Materials material = findByName(materialName);
        if (isRealMaterial(material)) return makeFluidStack(state, material, amount);
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
        return null;
    }

    /** 列出某材料实际拥有的属性（对齐原版：拖入示例物品后只在它真有的属性里选）。 */
    public static List<Property> propertiesOf(Materials material) {
        List<Property> result = new ArrayList<>();
        if (material == null) return result;
        for (Property property : Property.values()) {
            if (property == Property.Metal || property == Property.ToolHead) continue;
            if (property.test(material)) result.add(property);
        }
        return result;
    }

    /** 列出某材料实际拥有的 SubTag。 */
    public static List<SubTag> subTagsOf(Materials material) {
        List<SubTag> result = new ArrayList<>();
        if (material == null) return result;
        for (SubTag tag : SubTag.sSubTags.values()) {
            if (hasSubTag(material, tag)) result.add(tag);
        }
        return result;
    }

    /** 材料是否带指定 SubTag。 */
    public static boolean hasSubTag(Materials material, SubTag tag) {
        if (!isRealMaterial(material) || tag == null) return false;
        if (material.contains(tag)) return true;
        Werkstoff werkstoff = werkstoffOf(material);
        return werkstoff != null && werkstoff.contains(tag);
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
            return new PrefixMaterial(data.mPrefix, isRealMaterial(material) ? material : null);
        }
        for (int oreId : OreDictionary.getOreIDs(stack)) {
            String oreName = OreDictionary.getOreName(oreId);
            OrePrefixes prefix = prefixOfOreName(oreName);
            if (prefix != null) {
                String materialName = oreName.substring(
                    prefix.name()
                        .length());
                return new PrefixMaterial(prefix, findByName(materialName));
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

    /** Queries BartWorks generation rules, including explicit prefix overrides. */
    private static boolean hasWerkstoffForm(Materials material, OrePrefixes prefix) {
        Werkstoff werkstoff = werkstoffOf(material);
        return werkstoff != null && werkstoff.hasItemType(prefix);
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

    /** Registered fluid-pipe forms are the GT5U equivalent of GTCEu's pipe property. */
    private static boolean hasFluidPipe(Materials material) {
        return makePrefixStack(OrePrefixes.pipeTiny, material, 1) != null
            || makePrefixStack(OrePrefixes.pipeSmall, material, 1) != null
            || makePrefixStack(OrePrefixes.pipeMedium, material, 1) != null
            || makePrefixStack(OrePrefixes.pipeLarge, material, 1) != null
            || makePrefixStack(OrePrefixes.pipeHuge, material, 1) != null;
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
        FluidPipe("FLUID_PIPE");

        public final String serializedName;

        Property(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String toString() {
            return serializedName;
        }

        public String displayName() {
            if (this == Metal) return "ingot";
            if (this == ToolHead) return "tool";
            return serializedName.toLowerCase(Locale.ROOT);
        }

        public boolean test(Materials material) {
            if (!isRealMaterial(material)) return false;
            switch (this) {
                case Dust:
                    return material.hasDustItems() || hasWerkstoffForm(material, OrePrefixes.dust);
                case Metal:
                case Ingot:
                    return material.hasMetalItems() || hasWerkstoffForm(material, OrePrefixes.ingot);
                case Gem:
                    return material.hasGemItems() || hasWerkstoffForm(material, OrePrefixes.gem);
                case Ore:
                    return material.hasOresItems() || hasWerkstoffForm(material, OrePrefixes.ore);
                case Cell:
                    return material.hasCell() || hasWerkstoffForm(material, OrePrefixes.cell);
                case Plasma:
                    return material.hasPlasma();
                case ToolHead:
                case Tool:
                    return material.hasToolHeadItems() || hasWerkstoffForm(material, OrePrefixes.toolHeadHammer)
                        || hasWerkstoffForm(material, OrePrefixes.toolHeadWrench)
                        || hasWerkstoffForm(material, OrePrefixes.toolHeadSaw);
                case Gear:
                    return material.hasGearItems() || hasWerkstoffForm(material, OrePrefixes.gearGt);
                case Fluid:
                    return material.mFluid != null || material.mStandardMoltenFluid != null;
                case Gas:
                    return material.mGas != null;
                case Polymer:
                    return isPolymer(material);
                case FluidPipe:
                    return hasFluidPipe(material);
                default:
                    return false;
            }
        }
    }

    /** (前缀, 材料) 解析结果。 */
    public static final class PrefixMaterial {

        public static final PrefixMaterial EMPTY = new PrefixMaterial(null, null);

        public final OrePrefixes prefix;
        public final Materials material;

        public PrefixMaterial(OrePrefixes prefix, Materials material) {
            this.prefix = prefix;
            this.material = material;
        }
    }
}
