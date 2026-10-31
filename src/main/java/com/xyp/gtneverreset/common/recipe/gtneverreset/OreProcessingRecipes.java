package com.xyp.gtneverreset.common.recipe.gtneverreset;

import static com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps.ORE_PROCESSING_RECIPES;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.google.common.collect.Sets;
import com.xyp.gtneverreset.GTNeverReset;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.StoneType;
import gregtech.api.interfaces.IOreMaterial;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gregtech.common.ores.OreInfo;
import gregtech.common.ores.OreManager;

public final class OreProcessingRecipes {

    private static final int EUT = 10;
    private static final int DURATION_TICKS = 20;
    private static final OrePrefixes[] BYPRODUCT_STAGES = { OrePrefixes.crushed, OrePrefixes.crushedPurified,
        OrePrefixes.crushedCentrifuged, OrePrefixes.dustImpure, OrePrefixes.dustPure };
    private static final OrePrefixes[] INPUT_FORMS = { OrePrefixes.crushedPurified, OrePrefixes.crushedCentrifuged,
        OrePrefixes.crushed, OrePrefixes.dustImpure, OrePrefixes.dustPure, OrePrefixes.rawOre,
        OrePrefixes.oreNetherrack, OrePrefixes.oreEndstone, OrePrefixes.oreBasalt, OrePrefixes.oreBlackgranite,
        OrePrefixes.oreRedgranite, OrePrefixes.oreMarble, OrePrefixes.ore };

    private static final Set<OrePrefixes> BASIC_STONE_TYPES = Sets.newHashSet(
        OrePrefixes.ore,
        OrePrefixes.oreBasalt,
        OrePrefixes.oreBlackgranite,
        OrePrefixes.oreRedgranite,
        OrePrefixes.oreMarble,
        OrePrefixes.oreNetherrack,
        OrePrefixes.oreEndstone);

    private static final Set<OrePrefixes> BASIC_STONE_TYPES_EXCEPT_NORMAL = Sets.newHashSet(
        OrePrefixes.oreBasalt,
        OrePrefixes.oreBlackgranite,
        OrePrefixes.oreRedgranite,
        OrePrefixes.oreMarble,
        OrePrefixes.oreNetherrack,
        OrePrefixes.oreEndstone);

    public static void register() {
        processGTMaterials();
        processIntermediateProducts();
        processOtherModOre(new ItemStack(Blocks.iron_ore), Materials.Iron, false);

        GTNeverReset.LOG.info("loaded large ore processor recipes");
    }

    /** Registers external ores and imports native mechanical byproducts after recipe registration completes. */
    public static void registerExternalRecipes() {
        for (String oreName : OreDictionary.getOreNames()) {
            int dustAmount;
            String materialName;
            if (oreName.startsWith("oreNetherrack")) {
                materialName = oreName.substring("oreNetherrack".length());
                dustAmount = 16;
            } else if (oreName.startsWith("oreEndstone")) {
                materialName = oreName.substring("oreEndstone".length());
                dustAmount = 16;
            } else if (oreName.startsWith("rawOre")) {
                materialName = oreName.substring("rawOre".length());
                dustAmount = 8;
            } else if (oreName.startsWith("oreBasalt")) {
                materialName = oreName.substring("oreBasalt".length());
                dustAmount = 8;
            } else if (oreName.startsWith("oreBlackgranite")) {
                materialName = oreName.substring("oreBlackgranite".length());
                dustAmount = 8;
            } else if (oreName.startsWith("oreRedgranite")) {
                materialName = oreName.substring("oreRedgranite".length());
                dustAmount = 8;
            } else if (oreName.startsWith("oreMarble")) {
                materialName = oreName.substring("oreMarble".length());
                dustAmount = 8;
            } else if (oreName.startsWith("ore")) {
                materialName = oreName.substring("ore".length());
                dustAmount = 8;
            } else if (oreName.startsWith("crushedPurified")) {
                materialName = oreName.substring("crushedPurified".length());
                dustAmount = 8;
            } else if (oreName.startsWith("crushedCentrifuged")) {
                materialName = oreName.substring("crushedCentrifuged".length());
                dustAmount = 8;
            } else if (oreName.startsWith("crushed")) {
                materialName = oreName.substring("crushed".length());
                dustAmount = 8;
            } else if (oreName.startsWith("dustImpure")) {
                materialName = oreName.substring("dustImpure".length());
                dustAmount = 6;
            } else if (oreName.startsWith("dustPure")) {
                materialName = oreName.substring("dustPure".length());
                dustAmount = 7;
            } else {
                continue;
            }
            if (materialName.isEmpty()) continue;
            List<ItemStack> dusts = OreDictionary.getOres("dust" + materialName);
            if (dusts.isEmpty()) continue;
            ItemStack dust = dusts.get(0);
            if (dust == null || dust.getItem() == null) continue;

            for (ItemStack input : OreDictionary.getOres(oreName)) {
                if (input == null || input.getItem() == null) continue;
                ItemStack singleInput = GTUtility.copyAmountUnsafe(1, input);
                if (ORE_PROCESSING_RECIPES.findRecipeQuery()
                    .items(singleInput)
                    .notUnificated(true)
                    .find() != null) continue;
                addRecipe(singleInput, GTUtility.copyAmountUnsafe(dustAmount, dust));
            }
        }
        addSiftingOutputs();
        addMechanicalByproducts();
        addOreHostAliases();
    }

    /** Preserve the former controller's Stone fallback without rescanning inputs during processing. */
    private static void addOreHostAliases() {
        int registered = 0;
        for (String oreName : OreDictionary.getOreNames()) {
            if (!oreName.startsWith("ore") && !oreName.startsWith("rawOre")) continue;
            for (ItemStack input : OreDictionary.getOres(oreName)) {
                if (input == null || input.getItem() == null) continue;
                ItemStack singleInput = GTUtility.copyAmountUnsafe(1, input);
                if (ORE_PROCESSING_RECIPES.findRecipeQuery()
                    .items(singleInput)
                    .notUnificated(true)
                    .find() != null) continue;
                try (OreInfo<?> info = OreManager.getOreInfo(singleInput)) {
                    if (info == null || info.material == null) continue;
                    try (OreInfo<IOreMaterial> stone = OreInfo.getNewInfo()) {
                        stone.material = info.material;
                        stone.stoneType = StoneType.Stone;
                        stone.isSmall = false;
                        stone.isNatural = false;
                        ItemStack normalized = OreManager.getStack(stone, 1);
                        if (normalized == null) continue;
                        GTRecipe original = ORE_PROCESSING_RECIPES.findRecipeQuery()
                            .items(normalized)
                            .notUnificated(true)
                            .find();
                        if (original == null) continue;
                        GTRecipe alias = original.copy()
                            .setInputs(singleInput);
                        ORE_PROCESSING_RECIPES.getBackend()
                            .compileRecipe(alias);
                        registered++;
                    }
                }
            }
        }
        GTNeverReset.LOG.info("registered {} ore-host fallback recipes", registered);
    }

    private static void addMechanicalByproducts() {
        Map<String, List<GTRecipe>> sources = new HashMap<>();
        for (RecipeMap<?> map : new RecipeMap<?>[] { RecipeMaps.maceratorRecipes, RecipeMaps.thermalCentrifugeRecipes,
            RecipeMaps.centrifugeRecipes }) {
            for (GTRecipe source : map.getAllRecipes()) {
                if (!source.mEnabled || source.mInputs.length != 1
                    || source.mInputs[0] == null
                    || source.mInputs[0].stackSize != 1
                    || source.mOutputs.length < 2
                    || source.mFluidInputs.length != 0
                    || source.mFluidOutputs.length != 0
                    || source.mSpecialItems != null) continue;
                for (int oreId : OreDictionary.getOreIDs(source.mInputs[0])) {
                    String name = OreDictionary.getOreName(oreId);
                    OrePrefixes prefix = inputPrefix(name);
                    if (prefix == null || BASIC_STONE_TYPES.contains(prefix) || prefix == OrePrefixes.rawOre) continue;
                    if (!matchesMaterial(
                        source.mInputs[0],
                        name.substring(
                            prefix.name()
                                .length())))
                        continue;
                    sources.computeIfAbsent(name, ignored -> new ArrayList<>())
                        .add(source);
                }
            }
        }

        int supplemented = 0;
        for (GTRecipe recipe : ORE_PROCESSING_RECIPES.getAllRecipes()) {
            if (recipe.mInputs.length != 1 || recipe.mInputs[0] == null) continue;
            for (int oreId : OreDictionary.getOreIDs(recipe.mInputs[0])) {
                String name = OreDictionary.getOreName(oreId);
                OrePrefixes prefix = inputPrefix(name);
                if (prefix == null) continue;
                String material = name.substring(
                    prefix.name()
                        .length());
                if (!matchesMaterial(recipe.mInputs[0], material)) continue;
                List<ByproductGroup> selected = new ArrayList<>();
                boolean found = false;
                for (OrePrefixes stage : remainingStages(prefix)) {
                    List<GTRecipe> stageRecipes = sources.get(stage.name() + material);
                    if (stageRecipes == null) continue;
                    found = true;
                    for (GTRecipe source : stageRecipes) {
                        List<ByproductGroup> groups = new ArrayList<>();
                        for (int slot = 1; slot < source.mOutputs.length; slot++) {
                            ItemStack output = source.mOutputs[slot];
                            if (output == null || output.stackSize <= 0
                                || source.getOutputChance(slot) <= 0
                                || isGangue(output)
                                || hasOutput(recipe.mOutputs, output)) continue;
                            ByproductGroup group = findGroup(groups, output);
                            if (group == null) {
                                group = new ByproductGroup(source, output);
                                groups.add(group);
                            }
                            group.slots.add(slot);
                            group.expectedAmount += (long) output.stackSize * source.getOutputChance(slot);
                        }
                        for (ByproductGroup group : groups) {
                            ByproductGroup current = findGroup(selected, group.item);
                            if (current == null) selected.add(group);
                            else if (group.expectedAmount > current.expectedAmount) {
                                selected.set(selected.indexOf(current), group);
                            }
                        }
                    }
                }
                if (!found) continue;
                if (!selected.isEmpty()) {
                    List<ItemStack> outputs = new ArrayList<>();
                    List<Integer> chances = new ArrayList<>();
                    for (int slot = 0; slot < recipe.mOutputs.length; slot++) {
                        outputs.add(recipe.mOutputs[slot]);
                        chances.add(recipe.getOutputChance(slot));
                    }
                    int multiplier = isRichOre(prefix) ? 2 : 1;
                    for (ByproductGroup group : selected) {
                        for (int slot : group.slots) {
                            outputs.add(
                                GTUtility.copyAmountUnsafe(
                                    group.source.mOutputs[slot].stackSize * multiplier,
                                    group.source.mOutputs[slot]));
                            chances.add(group.source.getOutputChance(slot));
                        }
                    }
                    recipe.mOutputs = outputs.toArray(new ItemStack[0]);
                    recipe.mOutputChances = chances.stream()
                        .mapToInt(Integer::intValue)
                        .toArray();
                    supplemented++;
                }
                break;
            }
        }
        GTNeverReset.LOG
            .info("added native grinding and centrifuging products to {} large ore processor recipes", supplemented);
    }

    /** Progressed inputs must not collect byproducts from stages they have already passed. */
    private static OrePrefixes[] remainingStages(OrePrefixes input) {
        if (input == OrePrefixes.crushedPurified) {
            return new OrePrefixes[] { OrePrefixes.crushedPurified, OrePrefixes.crushedCentrifuged,
                OrePrefixes.dustPure };
        }
        if (input == OrePrefixes.crushedCentrifuged || input == OrePrefixes.dustPure
            || input == OrePrefixes.dustImpure) {
            return new OrePrefixes[] { input };
        }
        return BYPRODUCT_STAGES;
    }

    private static OrePrefixes inputPrefix(String name) {
        for (OrePrefixes prefix : INPUT_FORMS) {
            if (name.startsWith(prefix.name())) return prefix;
        }
        return null;
    }

    /** Existing full dust also covers smaller dust fractions of the same registered material. */
    private static boolean hasOutput(ItemStack[] outputs, ItemStack candidate) {
        ItemData expected = GTOreDictUnificator.getAssociation(candidate);
        for (ItemStack output : outputs) {
            if (output == null) continue;
            if (GTUtility.areStacksEqual(output, candidate)) return true;
            ItemData existing = GTOreDictUnificator.getAssociation(output);
            if (existing != null && expected != null
                && isDust(existing.mPrefix)
                && isDust(expected.mPrefix)
                && existing.mMaterial != null
                && expected.mMaterial != null
                && expected.mMaterial.mMaterial != null
                && existing.mMaterial.mMaterial == expected.mMaterial.mMaterial
                && (long) output.stackSize * existing.mPrefix.getMaterialAmount()
                    >= (long) candidate.stackSize * expected.mPrefix.getMaterialAmount())
                return true;
        }
        return false;
    }

    private static boolean isDust(OrePrefixes prefix) {
        return prefix == OrePrefixes.dust || prefix == OrePrefixes.dustSmall || prefix == OrePrefixes.dustTiny;
    }

    /** Host-rock waste is not a valuable ore byproduct. */
    private static boolean isGangue(ItemStack output) {
        for (int oreId : OreDictionary.getOreIDs(output)) {
            String name = OreDictionary.getOreName(oreId);
            for (String prefix : new String[] { "dustSmall", "dustTiny", "dust" }) {
                if (!name.startsWith(prefix)) continue;
                String material = name.substring(prefix.length());
                if (material.equals("Stone") || material.equals("Netherrack") || material.equals("Endstone"))
                    return true;
                break;
            }
        }
        return false;
    }

    private static ByproductGroup findGroup(List<ByproductGroup> groups, ItemStack item) {
        for (ByproductGroup group : groups) {
            if (GTUtility.areStacksEqual(group.item, item)) return group;
        }
        return null;
    }

    private static void addSiftingOutputs() {
        Map<String, GTRecipe> siftingRecipes = new HashMap<>();
        for (GTRecipe recipe : RecipeMaps.sifterRecipes.getAllRecipes()) {
            if (!recipe.mEnabled || recipe.mInputs.length != 1
                || recipe.mInputs[0] == null
                || recipe.mInputs[0].stackSize != 1
                || recipe.mFluidInputs.length != 0
                || recipe.mFluidOutputs.length != 0
                || recipe.mSpecialItems != null) continue;
            for (int oreId : OreDictionary.getOreIDs(recipe.mInputs[0])) {
                String oreName = OreDictionary.getOreName(oreId);
                if (oreName.startsWith("crushedPurified")) {
                    String materialName = oreName.substring("crushedPurified".length());
                    if (matchesMaterial(recipe.mInputs[0], materialName)) {
                        siftingRecipes.putIfAbsent(materialName, recipe);
                    }
                }
            }
        }

        int supplemented = 0;
        for (GTRecipe recipe : ORE_PROCESSING_RECIPES.getAllRecipes()) {
            ItemStack input = recipe.mInputs[0];
            for (int oreId : OreDictionary.getOreIDs(input)) {
                String oreName = OreDictionary.getOreName(oreId);
                String materialName = getSiftingMaterial(oreName);
                if (!matchesMaterial(input, materialName)) continue;
                GTRecipe sifting = siftingRecipes.get(materialName);
                if (sifting == null) continue;

                List<ItemStack> outputs = new ArrayList<>();
                List<Integer> chances = new ArrayList<>();
                for (int i = 0; i < recipe.mOutputs.length; i++) {
                    outputs.add(recipe.mOutputs[i]);
                    chances.add(recipe.getOutputChance(i));
                }
                int multiplier = oreName.startsWith("oreNetherrack") || oreName.startsWith("oreEndstone") ? 2 : 1;
                for (int i = 0; i < sifting.mOutputs.length; i++) {
                    ItemStack output = sifting.mOutputs[i];
                    if (output == null || output.stackSize <= 0 || sifting.getOutputChance(i) <= 0) continue;
                    boolean existing = false;
                    for (ItemStack original : recipe.mOutputs) {
                        if (GTUtility.areStacksEqual(original, output)) {
                            existing = true;
                            break;
                        }
                    }
                    if (existing) continue;
                    outputs.add(GTUtility.copyAmountUnsafe(output.stackSize * multiplier, output));
                    chances.add(sifting.getOutputChance(i));
                }
                if (outputs.size() > recipe.mOutputs.length) {
                    recipe.mOutputs = outputs.toArray(new ItemStack[0]);
                    recipe.mOutputChances = chances.stream()
                        .mapToInt(Integer::intValue)
                        .toArray();
                    supplemented++;
                }
                break;
            }
        }
        GTNeverReset.LOG.info("added native sifting products to {} large ore processor recipes", supplemented);
    }

    /** Broad ore-dictionary groups such as AnyCarbon must not share one material's sifting products. */
    private static boolean matchesMaterial(ItemStack input, String materialName) {
        ItemData association = GTOreDictUnificator.getAssociation(input);
        return association == null || association.mMaterial == null
            || association.mMaterial.mMaterial == null
            || association.mMaterial.mMaterial.mName.equals(materialName);
    }

    /** Only inputs before the sifting branch can receive sifting products; dusts and centrifuged ores cannot. */
    private static String getSiftingMaterial(String oreName) {
        if (oreName.startsWith("crushedCentrifuged")) return null;
        for (OrePrefixes prefix : new OrePrefixes[] { OrePrefixes.crushedPurified, OrePrefixes.crushed,
            OrePrefixes.rawOre, OrePrefixes.oreNetherrack, OrePrefixes.oreEndstone, OrePrefixes.oreBasalt,
            OrePrefixes.oreBlackgranite, OrePrefixes.oreRedgranite, OrePrefixes.oreMarble, OrePrefixes.ore }) {
            String name = prefix.name();
            if (oreName.startsWith(name)) return oreName.substring(name.length());
        }
        return null;
    }

    private static void processGTMaterials() {
        for (Materials material : GregTechAPI.sGeneratedMaterials) {
            if (material == null) continue;
            processGTMaterialOre(material);
        }
    }

    private static void processGTMaterialOre(Materials material) {
        if (GTOreDictUnificator.get(OrePrefixes.ore, material, 1) == null) return;

        ItemStack[] normalOutputs = getOutputs(material, false);
        ItemStack[] richOutputs = getOutputs(material, true);

        addRecipe(GTOreDictUnificator.get(OrePrefixes.ore, material, 1), normalOutputs);
        addRecipe(GTOreDictUnificator.get(OrePrefixes.rawOre, material, 1), normalOutputs);

        for (OrePrefixes prefix : BASIC_STONE_TYPES_EXCEPT_NORMAL) {
            ItemStack ore = GTOreDictUnificator.get(prefix, material, 1);
            if (ore != null) {
                addRecipe(ore, isRichOre(prefix) ? richOutputs : normalOutputs);
            }
        }
    }

    private static ItemStack[] getOutputs(Materials material, boolean isRich) {
        List<ItemStack> outputs = new ArrayList<>();

        if (material.mOreByProducts != null && !material.mOreByProducts.isEmpty()) {
            outputs.add(getDustStack(material, 4));

            if (material.mOreByProducts.size() == 1) {
                for (Materials byproduct : material.mOreByProducts) {
                    if (byproduct != null) {
                        outputs.add(getDustStack(byproduct, 3));
                    }
                }
            } else {
                for (Materials byproduct : material.mOreByProducts) {
                    if (byproduct == null || byproduct == Materials.Netherrack
                        || byproduct == Materials.Endstone
                        || byproduct == Materials.Stone) continue;
                    outputs.add(getDustStack(byproduct, 2));
                }
            }
        } else {
            outputs.add(getDustStack(material, 8));
        }

        addGemOutputs(outputs, material);

        if (isRich) {
            for (ItemStack stack : outputs) {
                if (stack != null) stack.stackSize *= 2;
            }
        }

        return outputs.toArray(new ItemStack[0]);
    }

    private static void addGemOutputs(List<ItemStack> outputs, Materials material) {
        ItemStack gem = GTOreDictUnificator.get(OrePrefixes.gem, material, 1);
        if (gem == null) return;

        ItemStack gemExquisite = GTOreDictUnificator.get(OrePrefixes.gemExquisite, material, 1);
        if (gemExquisite != null) {
            outputs.add(gemExquisite);
            outputs.add(GTOreDictUnificator.get(OrePrefixes.gemFlawless, material, 2));
        }
        outputs.add(gem);
    }

    private static ItemStack getDustStack(Materials material, int amount) {
        ItemStack dust = GTOreDictUnificator.get(OrePrefixes.dust, material, 1);
        if (dust == null) return null;
        return GTUtility.copyAmountUnsafe(amount, dust);
    }

    private static void processIntermediateProducts() {
        for (Materials material : GregTechAPI.sGeneratedMaterials) {
            if (material == null) continue;

            processIntermediateForm(material, OrePrefixes.crushed, input -> getOutputs(input, false));
            processIntermediateForm(material, OrePrefixes.crushedPurified, input -> getOutputs(input, false));
            processIntermediateForm(material, OrePrefixes.crushedCentrifuged, input -> getOutputs(input, false));
            processIntermediateForm(
                material,
                OrePrefixes.dustImpure,
                input -> new ItemStack[] { getDustStack(input, 6) });
            processIntermediateForm(
                material,
                OrePrefixes.dustPure,
                input -> new ItemStack[] { getDustStack(input, 7) });
        }
    }

    private static void processIntermediateForm(Materials material, OrePrefixes prefix,
        Function<Materials, ItemStack[]> outputProvider) {
        ItemStack input = GTOreDictUnificator.get(prefix, material, 1);
        if (input != null) {
            addRecipe(input, outputProvider.apply(material));
        }
    }

    private static void processOtherModOre(ItemStack ore, Materials material, boolean isRich) {
        if (ore != null) {
            addRecipe(ore, getOutputs(material, isRich));
        }
    }

    private static void addRecipe(ItemStack input, ItemStack... outputs) {
        if (input == null || outputs == null) return;

        List<ItemStack> nonNullOutputs = new ArrayList<>();
        for (ItemStack output : outputs) {
            if (output != null) {
                nonNullOutputs.add(output);
            }
        }
        if (nonNullOutputs.isEmpty()) return;

        GTRecipeBuilder.builder()
            .itemInputs(input)
            .itemOutputs(nonNullOutputs.toArray(new ItemStack[0]))
            .eut(EUT)
            .duration(DURATION_TICKS)
            .addTo(ORE_PROCESSING_RECIPES);
    }

    private static boolean isRichOre(OrePrefixes prefix) {
        return prefix == OrePrefixes.oreNetherrack || prefix == OrePrefixes.oreEndstone;
    }

    /** One source recipe's independent slots for a single byproduct item. */
    private static final class ByproductGroup {

        private final GTRecipe source;
        private final ItemStack item;
        private final List<Integer> slots = new ArrayList<>();
        private long expectedAmount;

        private ByproductGroup(GTRecipe source, ItemStack item) {
            this.source = source;
            this.item = item;
        }
    }
}
