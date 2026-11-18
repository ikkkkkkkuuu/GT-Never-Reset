package com.xyp.gtneverreset.common.crafting.chaos.preset;

import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Anthracene;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Benzene;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.ButadieneGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.ButaneGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.ButeneGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CoalTar;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CoalTarOil;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Creosote;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Dimethylbenzene;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.EthaneGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Ethylbenzene;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.EthyleneGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.ExtraHeavyOil;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HeavyFuel;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HeavyOil;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.HeliumGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.III_Dimethylbenzene;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.IVDimethylbenzene;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Kerosene;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.LightFuel;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.LightOil;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Lubricant;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.MediumOil;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.MethaneGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Naphtha;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Naphthalene;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NaphthenicAcid;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.NaturalGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Octane;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Oil;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Phenol;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PropaneGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.PropeneGas;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.RecipePools.OilCracker;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Steam;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.Toluene;
import static com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.WoodTar;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeCatalog;
import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeCatalog.StepRef;

import gregtech.api.enums.TierEU;
import gregtech.api.util.GTRecipe;

/** EV petrochemical line: the GTNL petrochemical plant's oil, tar and refining recipes without GTNL. */
public final class BottledChaosPetrochemicalPreset {

    private static final int CRACK_TICKS = 120;

    public static void registerRecipeChains(BottledChaosRecipeCatalog catalog) {
        // spotless:off
        catalog.registerChain("petrochemical", 2, TierEU.RECIPE_EV, Oil,
            new Object[] { EthyleneGas, MethaneGas, HeliumGas, PropaneGas, PropeneGas, EthaneGas, ButaneGas,
                ButeneGas, ButadieneGas, Toluene, Benzene, Octane },
            crackSteam(catalog, Oil, 1_000, CRACK_TICKS,
                new FluidStack(EthyleneGas, 200), new FluidStack(MethaneGas, 200), new FluidStack(HeliumGas, 10),
                new FluidStack(PropaneGas, 40), new FluidStack(PropeneGas, 200), new FluidStack(EthaneGas, 40),
                new FluidStack(ButaneGas, 40), new FluidStack(ButeneGas, 50), new FluidStack(ButadieneGas, 40),
                new FluidStack(Toluene, 30), new FluidStack(Benzene, 100), new FluidStack(Octane, 40)));
        alternativeCrackSteam(catalog, HeavyOil, 1_000, CRACK_TICKS,
            new FluidStack(EthyleneGas, 225), new FluidStack(MethaneGas, 225), new FluidStack(HeliumGas, 10),
            new FluidStack(PropaneGas, 20), new FluidStack(PropeneGas, 150), new FluidStack(EthaneGas, 25),
            new FluidStack(ButaneGas, 30), new FluidStack(ButeneGas, 180), new FluidStack(ButadieneGas, 120),
            new FluidStack(Toluene, 120), new FluidStack(Benzene, 600), new FluidStack(Octane, 20));
        alternativeCrackSteam(catalog, LightOil, 1_000, CRACK_TICKS,
            new FluidStack(EthyleneGas, 125), new FluidStack(MethaneGas, 1_000), new FluidStack(HeliumGas, 20),
            new FluidStack(PropaneGas, 75), new FluidStack(PropeneGas, 50), new FluidStack(EthaneGas, 100),
            new FluidStack(ButaneGas, 80), new FluidStack(ButeneGas, 40), new FluidStack(ButadieneGas, 40),
            new FluidStack(Toluene, 10), new FluidStack(Benzene, 50), new FluidStack(Octane, 20));
        alternativeCrackSteam(catalog, MediumOil, 1_000, CRACK_TICKS,
            new FluidStack(EthyleneGas, 500), new FluidStack(MethaneGas, 500), new FluidStack(HeliumGas, 10),
            new FluidStack(PropaneGas, 20), new FluidStack(PropeneGas, 300), new FluidStack(EthaneGas, 80),
            new FluidStack(ButaneGas, 30), new FluidStack(ButeneGas, 60), new FluidStack(ButadieneGas, 50),
            new FluidStack(Toluene, 20), new FluidStack(Benzene, 100), new FluidStack(Octane, 20));
        alternativeRefine(catalog, HeavyOil, 2_000, 150,
            new FluidStack(Naphtha, 75), new FluidStack(NaphthenicAcid, 25), new FluidStack(NaturalGas, 300),
            new FluidStack(HeavyFuel, 500), new FluidStack(LightFuel, 300), new FluidStack(Lubricant, 400));
        alternativeRefine(catalog, LightOil, 2_000, 150,
            new FluidStack(Naphtha, 100), new FluidStack(NaphthenicAcid, 10), new FluidStack(NaturalGas, 800),
            new FluidStack(HeavyFuel, 40), new FluidStack(LightFuel, 70), new FluidStack(Lubricant, 120));
        alternativeRefine(catalog, Oil, 1_000, 100,
            new FluidStack(Naphtha, 200), new FluidStack(NaphthenicAcid, 30), new FluidStack(NaturalGas, 600),
            new FluidStack(HeavyFuel, 150), new FluidStack(LightFuel, 400));
        alternativeRefine(catalog, MediumOil, 2_000, 100,
            new FluidStack(Naphtha, 700), new FluidStack(NaphthenicAcid, 15), new FluidStack(NaturalGas, 300),
            new FluidStack(HeavyFuel, 50), new FluidStack(LightFuel, 300), new FluidStack(Lubricant, 300));
        alternativeRefine(catalog, ExtraHeavyOil, 2_000, 200,
            new FluidStack(Naphtha, 220), new FluidStack(NaphthenicAcid, 60), new FluidStack(NaturalGas, 500),
            new FluidStack(HeavyFuel, 750), new FluidStack(LightFuel, 350), new FluidStack(Lubricant, 600));
        alternativeCrackSteam(catalog, WoodTar, 1_000, 100,
            new FluidStack(Creosote, 150), new FluidStack(Phenol, 100), new FluidStack(Benzene, 250),
            new FluidStack(Toluene, 50), new FluidStack(Dimethylbenzene, 150),
            new FluidStack(III_Dimethylbenzene, 150), new FluidStack(IVDimethylbenzene, 150));
        alternativeCrackSteam(catalog, CoalTar, 1_000, 200,
            new FluidStack(CoalTarOil, 150), new FluidStack(Naphtha, 75), new FluidStack(Ethylbenzene, 100),
            new FluidStack(Anthracene, 25), new FluidStack(Kerosene, 300), new FluidStack(Naphthalene, 180));
        // spotless:on
    }

    /** Steam cracking: one part feed and 1,000 L steam, as another input variant of the same credential tag. */
    private static StepRef crackSteam(BottledChaosRecipeCatalog catalog, Fluid feed, int feedAmount, int duration,
        FluidStack... outputs) {
        FluidStack[] inputs = { new FluidStack(feed, feedAmount), new FluidStack(Steam, 1_000) };
        return catalog.markFixedRecipe(OilCracker, feed, outputs[0].getFluid(), recipe(inputs, outputs, duration));
    }

    private static void alternativeCrackSteam(BottledChaosRecipeCatalog catalog, Fluid feed, int feedAmount,
        int duration, FluidStack... outputs) {
        FluidStack[] inputs = { new FluidStack(feed, feedAmount), new FluidStack(Steam, 1_000) };
        alternative(catalog, feed, outputs, recipe(inputs, outputs, duration));
    }

    /** Straight-run refining: the feed alone is split into the fuel and lubricant fraction. */
    private static void alternativeRefine(BottledChaosRecipeCatalog catalog, Fluid feed, int feedAmount, int duration,
        FluidStack... outputs) {
        FluidStack[] inputs = { new FluidStack(feed, feedAmount) };
        alternative(catalog, feed, outputs, recipe(inputs, outputs, duration));
    }

    private static void alternative(BottledChaosRecipeCatalog catalog, Fluid feed, FluidStack[] outputs,
        GTRecipe recipe) {
        catalog.registerAlternativeChain(
            "petrochemical",
            TierEU.RECIPE_EV,
            feed,
            fluids(outputs),
            catalog.markFixedRecipe(OilCracker, feed, outputs[0].getFluid(), recipe));
    }

    private static GTRecipe recipe(FluidStack[] inputs, FluidStack[] outputs, int duration) {
        return new GTRecipe(
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            inputs,
            outputs,
            duration,
            (int) TierEU.RECIPE_EV,
            0);
    }

    private static Object[] fluids(FluidStack[] stacks) {
        Object[] keys = new Object[stacks.length];
        for (int i = 0; i < stacks.length; i++) keys[i] = stacks[i].getFluid();
        return keys;
    }
}
