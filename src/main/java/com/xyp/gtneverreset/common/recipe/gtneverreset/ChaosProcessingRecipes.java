package com.xyp.gtneverreset.common.recipe.gtneverreset;

import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Fruit;
import static com.xyp.gtneverreset.common.gui.recipe.BottledChaosExpandedFrontend.usesExpandedLayout;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.crafting.chaos.BottledChaosRecipeMetadata;
import com.xyp.gtneverreset.common.crafting.chaos.material.ChaosItemKey;
import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipePage;
import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeProcessor;
import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CredentialModel;
import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CredentialRoutes;
import com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps;

import gregtech.api.enums.GTValues;
import gregtech.api.recipe.RecipeCategory;
import gregtech.api.util.GTUtility;

/** Display-only batches read from the same prepared routes used by machine execution. */
public final class ChaosProcessingRecipes {

    public static void registerRecipes() {
        GTNRRecipeMaps.BottledChaosRecipes.getBackend()
            .clearRecipes();
        var displays = BottledChaosRecipeProcessor.displayRecipes();
        int registered = 0;
        for (var display : displays) {
            try {
                var plan = display.plan();
                ItemStack[][] inputGroups = display.inputAlternatives();
                var recipeMap = GTNRRecipeMaps.BottledChaosRecipes;
                var ui = recipeMap.getFrontend()
                    .getUIProperties();
                if (display.itemInputs()
                    .size() > ui.maxItemInputs
                    || display.fluidInputs()
                        .size() > ui.maxFluidInputs
                    || plan.itemOutputs()
                        .size() > ui.maxItemOutputs
                    || plan.fluidOutputs()
                        .size() > ui.maxFluidOutputs) {
                    throw new IllegalArgumentException("display batch exceeds NEI slot capacity");
                }
                ItemStack[] itemInputs = new ItemStack[display.itemInputs()
                    .size()];
                Object[] alternativeInputs = null;
                for (int i = 0; i < itemInputs.length; i++) {
                    var input = display.itemInputs()
                        .get(i);
                    itemInputs[i] = GTUtility.copyAmountUnsafe(displayAmount(input.stackSize()), input.itemStack());
                    if (Fruit.equalItemStack(input.itemStack())) {
                        if (alternativeInputs == null) alternativeInputs = new Object[itemInputs.length];
                        alternativeInputs[i] = new Object[] { "listAllfruit", itemInputs[i].stackSize };
                    }
                    if (inputGroups != null && input.stackSize() > 0) for (ItemStack[] candidates : inputGroups) {
                        if (!ChaosItemKey.create(candidates[0])
                            .equalItemStack(input.itemStack())) continue;
                        if (input.stackSize() > Integer.MAX_VALUE) continue;
                        if (input.stackSize() % candidates[0].stackSize != 0)
                            throw new IllegalArgumentException("fractional display input alternative");
                        ItemStack[] scaled = new ItemStack[candidates.length];
                        for (int j = 0; j < candidates.length; j++) {
                            long amount = Math
                                .multiplyExact(input.stackSize() / candidates[0].stackSize, candidates[j].stackSize);
                            if (amount > Integer.MAX_VALUE) {
                                scaled = null;
                                break;
                            }
                            scaled[j] = GTUtility.copyAmountUnsafe((int) amount, candidates[j]);
                        }
                        if (scaled == null) continue;
                        if (alternativeInputs == null) alternativeInputs = new Object[itemInputs.length];
                        alternativeInputs[i] = scaled;
                        break;
                    }
                }
                if (alternativeInputs != null) for (int i = 0; i < itemInputs.length; i++) {
                    if (alternativeInputs[i] == null) alternativeInputs[i] = itemInputs[i];
                }
                FluidStack[] fluidInputs = new FluidStack[display.fluidInputs()
                    .size()];
                for (int i = 0; i < fluidInputs.length; i++) {
                    var input = display.fluidInputs()
                        .get(i);
                    fluidInputs[i] = GTUtility.copyAmount(displayAmount(input.amount()), input.fluidStack());
                }
                ItemStack[] itemOutputs = new ItemStack[plan.itemOutputs()
                    .size()];
                for (int i = 0; i < itemOutputs.length; i++) {
                    var output = plan.itemOutputs()
                        .get(i);
                    itemOutputs[i] = GTUtility.copyAmountUnsafe(displayAmount(output.stackSize()), output.itemStack());
                }
                FluidStack[] fluidOutputs = new FluidStack[plan.fluidOutputs()
                    .size()];
                for (int i = 0; i < fluidOutputs.length; i++) {
                    var output = plan.fluidOutputs()
                        .get(i);
                    fluidOutputs[i] = GTUtility.copyAmount(displayAmount(output.amount()), output.fluidStack());
                }
                if (!Double.isFinite(plan.durationTicks()) || plan.durationTicks() <= 0) {
                    throw new IllegalArgumentException("invalid display batch duration");
                }
                var route = CredentialRoutes.get(display.tagIndex());
                if (route == null) throw new IllegalStateException("missing credential route " + display.tagIndex());
                var recipeBuilder = GTValues.RA.stdBuilder();
                recipeBuilder.recipeCategory(categoryForRoute(display));
                if (alternativeInputs == null) recipeBuilder.itemInputsUnsafe(itemInputs);
                else recipeBuilder.itemInputs(alternativeInputs);
                registered += recipeBuilder.fluidInputs(fluidInputs)
                    .itemOutputs(itemOutputs)
                    .fluidOutputs(fluidOutputs)
                    .metadata(BottledChaosRecipeMetadata.DISPLAY_BATCH, display)
                    .duration((int) Math.min(Integer.MAX_VALUE, Math.ceil(plan.durationTicks())))
                    .eut(display.baseEUt())
                    .fake()
                    .ignoreCollision()
                    .addTo(recipeMap)
                    .size();
            } catch (RuntimeException exception) {
                GTNeverReset.LOG.error(
                    "Could not register Bottle in Chaos NEI sample for route " + display.tagIndex() + '.',
                    exception);
            }
        }
        GTNeverReset.LOG.info("Registered {} Bottle in Chaos NEI route examples.", registered);
    }

    /** Dedicated family tabs use the stable registered tags, including families with a single recipe. */
    private static RecipeCategory categoryForRoute(BottledChaosRecipeProcessor.DisplayRecipe display) {
        int tagIndex = display.tagIndex();
        BottledChaosRecipePage page = CredentialModel.displayPage(tagIndex);
        if (!page.keepExpanded() && usesExpandedLayout(
            display.itemInputs()
                .size(),
            display.plan()
                .itemOutputs()
                .size(),
            display.fluidInputs()
                .size(),
            display.plan()
                .fluidOutputs()
                .size()))
            return BottledChaosRecipePage.DEFAULT.category();
        if ("honeycomb".equals(CredentialModel.tag(tagIndex)))
            tagIndex = CredentialModel.ownerTagIndex(display.requiredTagMask());
        return CredentialModel.displayPage(tagIndex)
            .category();
    }

    private static int displayAmount(long amount) {
        // The frontend reads exact long quantities from DISPLAY_BATCH for oversized stacks.
        return amount > Integer.MAX_VALUE ? 1 : Math.toIntExact(amount);
    }
}
