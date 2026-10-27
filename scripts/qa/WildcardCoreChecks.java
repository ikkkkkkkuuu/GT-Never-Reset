package com.xyp.gtneverreset.qa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.recipes.wildcard.WildcardPatternCache;
import com.xyp.gtneverreset.utils.recipes.wildcard.WildcardPatternEntry;
import com.xyp.gtneverreset.utils.recipes.wildcard.WildcardPatternGenerator;
import com.xyp.gtneverreset.utils.recipes.wildcard.WildcardPatternState;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.IWildcardFilterComponent;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.IWildcardIOComponent;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.WildcardExpansion;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.WildcardModelState;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.filter.StringFilterComponent;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.io.FluidIOComponent;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.io.PrefixIOComponent;
import com.xyp.gtneverreset.utils.recipes.wildcard.model.io.SimpleIOComponent;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStack;
import cpw.mods.fml.common.FMLCommonHandler;
import gregtech.api.enums.FluidState;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

public final class WildcardCoreChecks {

    private WildcardCoreChecks() {}

    public static void verify() {
        World world = FMLCommonHandler.instance()
            .getMinecraftServerInstance()
            .worldServerForDimension(0);
        require(world != null && !world.isRemote, "core checks require the initialized server world");
        verifyLegacyMigration();
        verifyStateCopies();
        verifyQuantities();
        verifyCache();
        verifyExpansion();
        verifyFluidDetails(world);
        System.out.println("WILDCARD_CORE_QA_PASS migration state quantities cache expansion native-fluids");
    }

    private static void verifyLegacyMigration() {
        ItemStack pattern = GTNRItemList.WildcardPattern.get(1);
        WildcardPatternEntry input = WildcardPatternEntry
            .fromStack(GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 2));
        WildcardPatternEntry output = WildcardPatternEntry
            .fromStack(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 3));
        input.convertToOreDict();
        output.convertToOreDict();
        NBTTagList oldInputs = new NBTTagList();
        oldInputs.appendTag(input.toNbt());
        NBTTagList oldOutputs = new NBTTagList();
        oldOutputs.appendTag(output.toNbt());
        NBTTagCompound legacy = new NBTTagCompound();
        legacy.setTag("WildcardInputComponents", oldInputs);
        legacy.setTag("WildcardOutputComponents", oldOutputs);
        legacy.setString("WildcardGlobalExcludeMaterials", "Copper");
        legacy.setString("keepUnrelatedData", "retained");
        pattern.setTagCompound(legacy);
        WildcardPatternGenerator.markAsWildcard(pattern);
        require(WildcardModelState.isNewModel(pattern), "legacy configuration did not migrate");
        PrefixIOComponent migratedInput = (PrefixIOComponent) WildcardModelState.getInputs(pattern)
            .get(0);
        PrefixIOComponent migratedOutput = (PrefixIOComponent) WildcardModelState.getOutputs(pattern)
            .get(0);
        require(
            migratedInput.getPrefix() == OrePrefixes.ingot && migratedInput.getAmount() == 2,
            "legacy input prefix or quantity changed");
        require(
            migratedOutput.getPrefix() == OrePrefixes.dust && migratedOutput.getAmount() == 3,
            "legacy output prefix or quantity changed");
        require(
            !WildcardModelState.getFilters(pattern)
                .get(0)
                .test(Materials.Copper),
            "legacy blacklist did not migrate");
        require(
            !legacy.hasKey("WildcardInputComponents") && !legacy.hasKey("WildcardOutputComponents"),
            "legacy keys survived migration");
        require("retained".equals(legacy.getString("keepUnrelatedData")), "migration discarded unrelated data");
        NBTTagCompound migrated = (NBTTagCompound) legacy.copy();
        WildcardPatternGenerator.markAsWildcard(pattern);
        require(migrated.equals(pattern.getTagCompound()), "migration is not idempotent");
    }

    private static void verifyStateCopies() {
        ItemStack pattern = freshPattern();
        ItemStack ingredient = new ItemStack(Items.stick, 7);
        ingredient.setTagCompound(new NBTTagCompound());
        ingredient.getTagCompound()
            .setString("quality", "original");
        SimpleIOComponent fixed = new SimpleIOComponent(ingredient, 7);
        ingredient.getTagCompound()
            .setString("quality", "outside");
        require(
            "original".equals(
                fixed.getStack()
                    .getTagCompound()
                    .getString("quality")),
            "fixed ingredient aliases its constructor argument");
        ItemStack exposed = fixed.getStack();
        exposed.getTagCompound()
            .setString("quality", "outside");
        require(
            "original".equals(
                fixed.getStack()
                    .getTagCompound()
                    .getString("quality")),
            "fixed ingredient exposes mutable internal NBT");
        WildcardModelState.setInputs(pattern, Arrays.asList(new PrefixIOComponent(OrePrefixes.ingot, 2), fixed));
        WildcardModelState.setOutputs(pattern, Collections.singletonList(new FluidIOComponent(FluidState.MOLTEN, 144)));
        WildcardModelState.setFilters(
            pattern,
            Collections.singletonList(new StringFilterComponent(Materials.Iron.mName, true, true)));
        ItemStack restored = ItemStack.loadItemStackFromNBT(pattern.writeToNBT(new NBTTagCompound()));
        require(ItemStack.areItemStacksEqual(pattern, restored), "pattern NBT roundtrip changed the configuration");

        NBTTagCompound exported = WildcardModelState.exportConfig(pattern);
        NBTTagCompound original = (NBTTagCompound) pattern.getTagCompound()
            .copy();
        ItemStack applied = freshPattern();
        WildcardModelState.applyConfig(applied, exported);
        exported.getTagList(WildcardModelState.KEY_INPUT, 10)
            .getCompoundTagAt(0)
            .getCompoundTag("data")
            .setInteger("Amount", 17);
        require(original.equals(pattern.getTagCompound()), "exported config aliases the source pattern");
        require(
            ((PrefixIOComponent) WildcardModelState.getInputs(applied)
                .get(0)).getAmount() == 2,
            "applied config aliases its input payload");
        List<IWildcardIOComponent> decoded = WildcardModelState.getInputs(applied);
        ((PrefixIOComponent) decoded.get(0)).setAmount(29);
        require(
            ((PrefixIOComponent) WildcardModelState.getInputs(applied)
                .get(0)).getAmount() == 2,
            "decoded components alias stored configuration");
    }

    private static void verifyQuantities() {
        ItemStack pattern = freshPattern();
        WildcardModelState.setInputs(pattern, Collections.singletonList(new FluidIOComponent(FluidState.MOLTEN, 144)));
        WildcardModelState.setOutputs(pattern, Collections.singletonList(new FluidIOComponent(FluidState.MOLTEN, 288)));
        WildcardPatternState.applyBitModification(pattern, 2);
        require(
            fluidAmount(pattern, true) == 576 && fluidAmount(pattern, false) == 1152,
            "matrix scaling did not update both sides together");
        WildcardPatternState.applyBitModification(pattern, -2);
        require(
            fluidAmount(pattern, true) == 144 && fluidAmount(pattern, false) == 288,
            "matrix division did not restore quantities");
        WildcardModelState.setInputs(
            pattern,
            Collections.singletonList(new FluidIOComponent(FluidState.MOLTEN, WildcardPatternEntry.MAX_AMOUNT)));
        WildcardModelState.setOutputs(pattern, Collections.singletonList(new FluidIOComponent(FluidState.MOLTEN, 1)));
        require(WildcardPatternState.getMaxBitMultiplier(pattern) == 0, "overflowing multiplier was offered");
        WildcardPatternState.applyBitModification(pattern, 1);
        require(
            fluidAmount(pattern, true) == WildcardPatternEntry.MAX_AMOUNT && fluidAmount(pattern, false) == 1,
            "overflowing multiplier partially changed the pattern");
    }

    private static void verifyCache() {
        ItemStack pattern = freshPattern();
        WildcardModelState.setInputs(pattern, Collections.singletonList(new FluidIOComponent(FluidState.MOLTEN, 144)));
        WildcardPatternCache<Integer> cache = new WildcardPatternCache<>();
        AtomicInteger builds = new AtomicInteger();
        Object context = new Object();
        for (int i = 0; i < 100; i++) {
            require(cache.get(pattern, context, () -> {
                builds.incrementAndGet();
                WildcardModelState.setExpandedCount(pattern, 0);
                return 0;
            }) == 0, "empty cached result changed");
        }
        require(builds.get() == 1, "empty results or post-generation NBT are not cached");
        pattern.getTagCompound()
            .getTagList(WildcardModelState.KEY_INPUT, 10)
            .getCompoundTagAt(0)
            .getCompoundTag("data")
            .setLong("Amount", 2000);
        require(cache.get(pattern, context, builds::incrementAndGet) == 2, "nested amount edit kept stale cache");
        WildcardModelState.setFilters(
            pattern,
            Collections.singletonList(new StringFilterComponent(Materials.Copper.mName, true, false)));
        require(cache.get(pattern, context, builds::incrementAndGet) == 3, "filter edit kept stale cache");
        pattern.stackSize = 64;
        require(
            cache.get(pattern.copy(), context, builds::incrementAndGet) == 3,
            "unchanged copied configuration invalidated the cache");
        require(cache.get(pattern, new Object(), builds::incrementAndGet) == 4, "world change kept stale cache");
        WildcardPatternCache<List<Integer>> first = new WildcardPatternCache<>();
        WildcardPatternCache<List<Integer>> second = new WildcardPatternCache<>();
        List<Integer> firstResult = first.get(pattern, null, ArrayList::new);
        List<Integer> secondResult = second.get(pattern, null, ArrayList::new);
        firstResult.add(7);
        require(firstResult != secondResult && secondResult.isEmpty(), "slots share mutable cached results");
        pattern.setTagCompound(null);
        try {
            cache.get(pattern, context, () -> { throw new IllegalStateException("expected build failure"); });
            throw new AssertionError("cache swallowed a failed build");
        } catch (IllegalStateException expected) {}
        require(cache.get(pattern, context, builds::incrementAndGet) == 5, "failed build was not retried");
        require(
            cache.get(null, null, () -> 6) == 6 && cache.get(null, null, () -> 7) == 6,
            "null configuration cache transitions failed");
    }

    private static void verifyExpansion() {
        List<IWildcardIOComponent> inputs = Collections.singletonList(new PrefixIOComponent(OrePrefixes.ingot, 2));
        List<IWildcardIOComponent> outputs = Collections.singletonList(new PrefixIOComponent(OrePrefixes.dust, 1));
        List<IWildcardFilterComponent> filters = new ArrayList<>();
        filters.add(new StringFilterComponent("^(Iron|Copper)$", true));
        List<WildcardExpansion.Expanded> expanded = WildcardExpansion.expand(inputs, outputs, filters);
        require(
            expanded.size() == 2 && WildcardExpansion.countExpanded(inputs, outputs, filters) == 2,
            "real iron/copper forms did not expand into exactly two patterns");
        for (WildcardExpansion.Expanded entry : expanded) {
            Materials material = Materials.Iron.mName.equals(entry.materialName) ? Materials.Iron : Materials.Copper;
            require(
                ItemStack
                    .areItemStacksEqual(entry.inputs.get(0), GTOreDictUnificator.get(OrePrefixes.ingot, material, 2)),
                "expansion changed input material or count");
            require(
                ItemStack
                    .areItemStacksEqual(entry.outputs.get(0), GTOreDictUnificator.get(OrePrefixes.dust, material, 1)),
                "expansion changed output material or count");
        }
        filters.add(StringFilterComponent.exactBlacklist(Materials.Copper.mName));
        require(
            WildcardExpansion.expand(inputs, outputs, filters)
                .size() == 1 && WildcardExpansion.countExpanded(inputs, outputs, filters) == 1,
            "blacklist and count disagree");
        require(
            WildcardExpansion.expand(Collections.emptyList(), Collections.emptyList(), filters)
                .isEmpty(),
            "empty configurations generated recipes");
    }

    private static void verifyFluidDetails(World world) {
        ItemStack pattern = freshPattern();
        WildcardModelState.setInputs(pattern, Collections.singletonList(new FluidIOComponent(FluidState.MOLTEN, 144)));
        WildcardModelState.setOutputs(pattern, Collections.singletonList(new PrefixIOComponent(OrePrefixes.ingot, 1)));
        WildcardModelState.setFilters(
            pattern,
            Collections.singletonList(new StringFilterComponent(Materials.Iron.mName, true, true)));
        List<ICraftingPatternDetails> details = WildcardPatternGenerator.generateAllDetails(pattern, world);
        require(details.size() == 1, "iron fluid template did not create one valid processing pattern");
        ICraftingPatternDetails detail = details.get(0);
        IAEStack<?>[] inputs = detail.getAEInputs();
        require(inputs.length == 1 && inputs[0] instanceof IAEFluidStack, "fluid was advertised as an item input");
        IAEFluidStack fluid = (IAEFluidStack) inputs[0];
        require(
            fluid.getStackSize() == 144 && fluid.getFluid() == Materials.Iron.getMolten(144)
                .getFluid(),
            "native fluid identity or quantity changed");
        require(
            !detail.isCraftable() && detail.getAEOutputs().length == 1 && detail.getAEOutputs()[0].getStackSize() == 1,
            "processing output details changed");
        require(WildcardPatternGenerator.isGeneratedPattern(detail.getPattern()), "generated identity was lost");
        require(detail.getAEInputs() == inputs, "pattern details rebuild stable native fluid arrays");
        List<ICraftingPatternDetails> again = WildcardPatternGenerator.generateAllDetails(pattern.copy(), world);
        require(
            WildcardPatternGenerator.getGeneratedPatternId(detail.getPattern())
                .equals(
                    WildcardPatternGenerator.getGeneratedPatternId(
                        again.get(0)
                            .getPattern())),
            "unchanged configuration generated an unstable identity");
        require(
            ItemStack.areItemStacksEqual(
                detail.getOutputs()[0].getItemStack(),
                GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 1)),
            "native fluid pattern output changed");
    }

    private static ItemStack freshPattern() {
        ItemStack pattern = GTNRItemList.WildcardPattern.get(1);
        WildcardPatternGenerator.markAsWildcard(pattern);
        return pattern;
    }

    private static long fluidAmount(ItemStack pattern, boolean input) {
        return ((FluidIOComponent) (input ? WildcardModelState.getInputs(pattern)
            : WildcardModelState.getOutputs(pattern)).get(0)).getAmount();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
