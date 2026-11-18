package com.xyp.gtneverreset.common.crafting.chaos.preset;

import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.AceticAcid;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Acetone;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.AshDarkDust;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Benzene;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CarbonMonoxideGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CoalGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Creosote;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Dimethylbenzene;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Ethanol;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.EthyleneGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HydrogenGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.MethaneGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Methanol;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.MethylAcetate;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NitrogenGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Phenol;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.IndustrialCokeOven;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Toluene;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Water;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.WoodLog;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.WoodLog2;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeCatalog;

import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTRecipe;

/** MV wood chemical line: the GTNL wood chemical plant's distillation recipe, registered without GTNL. */
public final class BottledChaosWoodPreset {

    public static void registerRecipeChains(BottledChaosRecipeCatalog catalog) {
        // spotless:off
        catalog.registerChain("wood", 1, TierEU.RECIPE_MV, WoodLog,
            new Object[] { AshDarkDust, Water, Methanol, Benzene, CarbonMonoxideGas, Creosote, Dimethylbenzene,
                AceticAcid, MethaneGas, Acetone, Phenol, Toluene, EthyleneGas, HydrogenGas, MethylAcetate, CoalGas,
                Ethanol },
            catalog.markFixedRecipe(IndustrialCokeOven, WoodLog, AshDarkDust, woodDistillation()));
        // Acacia and dark oak share the same recipe through the folded input alternative.
        catalog.registerInputAlternative("wood", WoodLog2, 1);
        // spotless:on
    }

    /** 16 logs and 1,000 L nitrogen yield ash and every wood-distillation fluid at the GTNL quantities. */
    private static GTRecipe woodDistillation() {
        return new GTRecipe(
            false,
            new ItemStack[] { WoodLog.getItemStack(16) },
            new ItemStack[] { Materials.AshDark.getDust(4) },
            null,
            null,
            null,
            null,
            null,
            new FluidStack[] { new FluidStack(NitrogenGas, 1_000) },
            new FluidStack[] { new FluidStack(Water, 400), new FluidStack(Methanol, 240), new FluidStack(Benzene, 175),
                new FluidStack(CarbonMonoxideGas, 170), new FluidStack(Creosote, 150),
                new FluidStack(Dimethylbenzene, 120), new FluidStack(AceticAcid, 80), new FluidStack(MethaneGas, 60),
                new FluidStack(Acetone, 40), new FluidStack(Phenol, 35), new FluidStack(Toluene, 35),
                new FluidStack(EthyleneGas, 10), new FluidStack(HydrogenGas, 10), new FluidStack(MethylAcetate, 8),
                new FluidStack(CoalGas, 8), new FluidStack(Ethanol, 8) },
            200,
            (int) TierEU.RECIPE_MV,
            0);
    }
}
