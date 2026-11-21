package com.xyp.gtneverreset.common.recipe;

import java.util.Collections;
import java.util.Comparator;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.widgets.ProgressWidget;
import com.gtnewhorizons.modularui.common.widget.ProgressBar;
import com.xyp.gtneverreset.common.crafting.chaos.BottledChaosRecipeMetadata;
import com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructureDisplay;
import com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeProcessor;
import com.xyp.gtneverreset.common.gui.recipe.BottledChaosCredentialFrontend;
import com.xyp.gtneverreset.common.gui.recipe.BottledChaosExpandedFrontend;
import com.xyp.gtneverreset.common.gui.recipe.BottledChaosStructureFrontend;
import com.xyp.gtneverreset.common.gui.recipe.FragmentWorldCollectionFrontend;
import com.xyp.gtneverreset.common.recipe.gtneverreset.ChaosRecipeBackend;
import com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectionBackend;
import com.xyp.gtneverreset.config.ChaosConfig;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import goodgenerator.client.GUI.GGUITextures;
import goodgenerator.items.GGMaterial;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.recipe.RecipeCategory;
import gregtech.api.recipe.RecipeCategorySetting;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.recipe.maps.LargeNEIFrontend;
import gregtech.api.util.GTRecipe;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;
import gtnhlanth.common.register.BotWerkstoffMaterialPool;
import gtnhlanth.common.register.LanthItemList;

public final class GTNRRecipeMaps {

    public static final RecipeMap<RecipeMapBackend> LightHunterRockBreakingRecipes = RecipeMapBuilder
        // #tr gtnr.recipe.satellite_rock_breaking
        // # Satellite Rock Breaking
        // # zh_CN 卫星岩石粉碎
        .of("gtnr.recipe.satellite_rock_breaking")
        .maxIO(2, 1, 2, 0)
        .neiHandlerInfo(b -> b.setDisplayStack(GTNRItemList.MineralProcessingSatelliteMkI.get(1)))
        .build();
    public static final RecipeMap<RecipeMapBackend> LightningProcessorRecipes = RecipeMapBuilder
        // #tr gtnr.recipe.satellite_lightning
        // # Lightning Processing
        // # zh_CN 闪电处理
        .of("gtnr.recipe.satellite_lightning")
        .maxIO(6, 1, 6, 1)
        .minInputs(0, 1)
        .neiHandlerInfo(b -> b.setDisplayStack(GTNRItemList.AssemblySatelliteMkI.get(1)))
        .build();

    public static final RecipeMap<RecipeMapBackend> CHAOS_CREDENTIAL_RECIPES = ChaosConfig.enableBottleInChaos
        ? RecipeMapBuilder
            // #tr gtnr.recipe.chaos_credential
            // # Bottle in Chaos: credential workbench (select preset; marker not consumed)
            // # zh_CN 混沌中瓶：凭证工作区（选择预设；标志物不消耗）
            .of("gtnr.recipe.chaos_credential")
            .maxIO(2, 1, 0, 0)
            .minInputs(1, 0)
            .build()
        : null;

    public static final RecipeMap<RecipeMapBackend> CHAOS_MODULE_RECIPES = ChaosConfig.enableBottleInChaos
        ? RecipeMapBuilder
            // #tr gtnr.recipe.chaos_module
            // # Bottle in Chaos: structure packaging workbench
            // # zh_CN 混沌中瓶：结构封装工作区
            .of("gtnr.recipe.chaos_module")
            .maxIO(3, 1, 0, 0)
            .minInputs(1, 0)
            .build()
        : null;

    public static final RecipeMap<RecipeMapBackend> DISASSEMBLER_RECIPES = RecipeMapBuilder
        // #tr gtnr.recipe.disassembler
        // # Disassembler
        // # zh_CN 拆解机
        .of("gtnr.recipe.disassembler")
        .maxIO(1, 9, 0, 0)
        .minInputs(1, 0)
        .useCustomFilterForNEI()
        .neiSpecialInfoFormatter(
            info -> Collections.singletonList(
                StatCollector.translateToLocalFormatted(
                    // #tr gtnr.recipe.disassembler.tier
                    // # Minimum disassembler tier: %s
                    // # zh_CN 最低拆解机等级：%s
                    "gtnr.recipe.disassembler.tier",
                    GTValues.VN[info.recipe.mSpecialValue])))
        .build();

    public static final RecipeMap<RecipeMapBackend> MODERN_MIXER_RECIPES = RecipeMapBuilder
        // #tr gtnr.recipe.modern_mixer
        // # Small Mixer (Direct Fluids)
        // # zh_CN 小型搅拌机（直接流体）
        .of("gtnr.recipe.modern_mixer")
        .maxIO(6, 1, 2, 1)
        .progressBar(GTUITextures.PROGRESSBAR_MIXER, ProgressBar.Direction.CIRCULAR_CW)
        .progressBarMUI2(GTGuiTextures.PROGRESSBAR_MIXER, ProgressWidget.Direction.CIRCULAR_CW)
        .frontend(LargeNEIFrontend::new)
        .build();

    public static final RecipeMap<RecipeMapBackend> MODERN_ELECTROLYZER_RECIPES = RecipeMapBuilder
        // #tr gtnr.recipe.modern_electrolyzer
        // # Small Electrolyzer (Direct Fluids)
        // # zh_CN 小型电解机（直接流体）
        .of("gtnr.recipe.modern_electrolyzer")
        .maxIO(2, 6, 1, 6)
        .progressBar(GTUITextures.PROGRESSBAR_EXTRACT)
        .progressBarMUI2(GTGuiTextures.PROGRESSBAR_EXTRACT)
        .frontend(LargeNEIFrontend::new)
        .build();

    public static final RecipeMap<RecipeMapBackend> MODERN_CHEMICAL_RECIPES = RecipeMapBuilder
        // #tr gtnr.recipe.modern_chemical_reactor
        // # Small Chemical Reactor (Direct Fluids)
        // # zh_CN 小型化学反应釜（直接流体）
        .of("gtnr.recipe.modern_chemical_reactor")
        .maxIO(6, 6, 6, 6)
        .progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
        .progressBarMUI2(GTGuiTextures.PROGRESSBAR_ARROW_MULTIPLE)
        .frontend(LargeNEIFrontend::new)
        .build();

    public static final RecipeMap<RecipeMapBackend> MODERN_CENTRIFUGE_RECIPES = RecipeMapBuilder
        // #tr gtnr.recipe.modern_centrifuge
        // # Small Centrifuge (Direct Fluids)
        // # zh_CN 小型离心机（直接流体）
        .of("gtnr.recipe.modern_centrifuge")
        .maxIO(2, 6, 1, 6)
        .progressBar(GTUITextures.PROGRESSBAR_EXTRACT)
        .progressBarMUI2(GTGuiTextures.PROGRESSBAR_EXTRACT)
        .frontend(LargeNEIFrontend::new)
        .build();

    public static final RecipeMap<FragmentWorldCollectionBackend> FRAGMENT_WORLD_COLLECTION_RECIPES = RecipeMapBuilder
        // #tr recipe.gtneverreset.fragment_world_collection
        // # Fragment World Collection
        // # zh_CN 碎片世界采集
        .of("recipe.gtneverreset.fragment_world_collection", FragmentWorldCollectionBackend::new)
        .maxIO(3, 12, 1, 1)
        .minInputs(1, 0)
        .progressBar(GTUITextures.PROGRESSBAR_MACERATE)
        .progressBarPos(62, 35)
        .logoPos(62, 80)
        .neiRecipeBackgroundSize(170, 108)
        .frontend(FragmentWorldCollectionFrontend::new)
        .neiHandlerInfo(
            builder -> builder.setDisplayStack(GTNRItemList.FragmentWorldCollector.get(1))
                .setHeight(160)
                .setMaxRecipesPerPage(1))
        .build();

    public static final RecipeMap<RecipeMapBackend> ORE_PROCESSING_RECIPES = RecipeMapBuilder
        // #tr recipe.gtneverreset.ore_processing
        // # Ore Processing
        // # zh_CN 矿石处理
        .of("recipe.gtneverreset.ore_processing")
        .maxIO(1, 18, 0, 0)
        .minInputs(1, 0)
        .progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
        .frontend(LargeNEIFrontend::new)
        .neiHandlerInfo(
            builder -> builder.setDisplayStack(GTNRItemList.LargeOreProcessor.get(1))
                .setHeight(160)
                .setMaxRecipesPerPage(1))
        .build();

    private static final Comparator<GTRecipe> BOTTLED_CHAOS_RECIPE_ORDER = Comparator.comparingInt(recipe -> {
        var display = recipe.getMetadata(BottledChaosRecipeMetadata.DISPLAY_BATCH);
        return display == null ? Integer.MAX_VALUE : display.displayOrder();
    });

    public static final RecipeMap<ChaosRecipeBackend> BottledChaosRecipes = ChaosConfig.enableBottleInChaos
        ? RecipeMapBuilder
            // #tr gtnr.bottleinchaos.recipe.name
            // # Bottle in Chaos
            // # zh_CN 混沌中瓶
            .of("gtnr.bottleinchaos.recipe.name", ChaosRecipeBackend::new)
            .maxIO(72, 18, 18, 18)
            .disableRegisterNEI()
            .frontend(BottledChaosExpandedFrontend::new)
            .neiRecipeComparator(BOTTLED_CHAOS_RECIPE_ORDER)
            .useCustomFilterForNEI()
            .useSpecialSlot()
            .progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
            .neiHandlerInfo(builder -> {
                int maxMaps = BottledChaosRecipeProcessor.maxDisplaySourcePools();
                if (GTNRItemList.BottleInChaos.hasBeenSet()) builder.setDisplayStack(GTNRItemList.BottleInChaos.get(1));
                return builder.setHeight(350 + 10 * maxMaps)
                    .setMultipleWidgetsAllowed(true);
            })
            .build()
        : null;

    public static final RecipeCategory BottledChaosNanoExtractionCategory = ChaosConfig.enableBottleInChaos
        ? new RecipeCategory(
            // #tr gtnr.bottleinchaos.recipe.category.nano_extraction.name
            // # Nano Resin Extraction
            // # zh_CN 纳米萃取
            "gtnr.bottleinchaos.recipe.category.nano_extraction.name",
            BottledChaosRecipes,
            RecipeCategorySetting::getDefault,
            builder -> BottledChaosRecipes.getDefaultRecipeCategory().handlerInfoCreator.apply(builder)
                .setDisplayStack(LanthItemList.DIGESTER.copy()))
        : null;

    public static final RecipeCategory BottledChaosPrismaticCategory = ChaosConfig.enableBottleInChaos
        ? new RecipeCategory(
            // #tr gtnr.bottleinchaos.recipe.category.prismatic.name
            // # Prismatic Acid
            // # zh_CN 海晶酸
            "gtnr.bottleinchaos.recipe.category.prismatic.name",
            BottledChaosRecipes,
            RecipeCategorySetting::getDefault,
            builder -> BottledChaosRecipes.getDefaultRecipeCategory().handlerInfoCreator.apply(builder)
                .setDisplayStack(Materials.PrismaticAcid.getCells(1)))
        : null;

    public static final RecipeMap<ChaosRecipeBackend> RapidCoolingDownRecipeMap = ChaosConfig.enableBottleInChaos
        ? RecipeMapBuilder
            // #tr gtnr.chaos.recipe.RapidCoolingDownRecipeMap.name
            // # Rapid Cooling Down
            // # zh_CN 快速冷却
            .of("gtnr.chaos.recipe.RapidCoolingDownRecipeMap.name", ChaosRecipeBackend::new)
            .maxIO(0, 0, 1, 1)
            .dontUseProgressBar()
            .neiHandlerInfo(builder -> builder.setDisplayStack(GTNRItemList.BottleInChaos.get(1)))
            .build()
        : null;

    public static final RecipeMap<ChaosRecipeBackend> NeutronActivatorWithEURecipeMap = ChaosConfig.enableBottleInChaos
        ? RecipeMapBuilder
            // #tr gtnr.chaos.recipe.NeutronActivatorWithEURecipeMap.name
            // # Neutron Oscillator - Electric Neutron Activator
            // # zh_CN 中子振荡器 - 电力版中子活化器
            .of("gtnr.chaos.recipe.NeutronActivatorWithEURecipeMap.name", ChaosRecipeBackend::new)
            .maxIO(9, 9, 1, 1)
            .dontUseProgressBar()
            .addSpecialTexture(73, 22, 31, 21, GGUITextures.PICTURE_NEUTRON_ACTIVATOR)
            .build()
        : null;

    public static final RecipeCategory BottledChaosFusionCategory = ChaosConfig.enableBottleInChaos
        ? new RecipeCategory(
            // #tr gtnr.bottleinchaos.recipe.category.fusion.name
            // # Fusion
            // # zh_CN 聚变
            "gtnr.bottleinchaos.recipe.category.fusion.name",
            BottledChaosRecipes,
            RecipeCategorySetting::getDefault,
            builder -> BottledChaosRecipes.getDefaultRecipeCategory().handlerInfoCreator.apply(builder)
                .setDisplayStack(ItemList.FusionComputer_LuV.get(1)))
        : null;

    public static final RecipeCategory BottledChaosRadoxCategory = ChaosConfig.enableBottleInChaos ? new RecipeCategory(
        // #tr gtnr.bottleinchaos.recipe.category.radox.name
        // # Radox
        // # zh_CN 拉多X
        "gtnr.bottleinchaos.recipe.category.radox.name",
        BottledChaosRecipes,
        RecipeCategorySetting::getDefault,
        builder -> BottledChaosRecipes.getDefaultRecipeCategory().handlerInfoCreator.apply(builder)
            .setDisplayStack(Materials.RadoxPolymer.getPlates(1)))
        : null;

    public static final RecipeCategory BottledChaosIndiumCategory = ChaosConfig.enableBottleInChaos
        ? new RecipeCategory(
            // #tr gtnr.bottleinchaos.recipe.category.indium.name
            // # Indium
            // # zh_CN 铟
            "gtnr.bottleinchaos.recipe.category.indium.name",
            BottledChaosRecipes,
            RecipeCategorySetting::getDefault,
            builder -> BottledChaosRecipes.getDefaultRecipeCategory().handlerInfoCreator.apply(builder)
                .setDisplayStack(Materials.Indium.getDust(1)))
        : null;

    public static final RecipeCategory BottledChaosTitaniumCategory = ChaosConfig.enableBottleInChaos
        ? new RecipeCategory(
            // #tr gtnr.bottleinchaos.recipe.category.titanium.name
            // # Titanium
            // # zh_CN 钛
            "gtnr.bottleinchaos.recipe.category.titanium.name",
            BottledChaosRecipes,
            RecipeCategorySetting::getDefault,
            builder -> BottledChaosRecipes.getDefaultRecipeCategory().handlerInfoCreator.apply(builder)
                .setDisplayStack(Materials.Rutile.getDust(1)))
        : null;

    public static final RecipeCategory BottledChaosKevlarCategory = ChaosConfig.enableBottleInChaos
        ? new RecipeCategory(
            // #tr gtnr.bottleinchaos.recipe.category.kevlar.name
            // # Kevlar
            // # zh_CN 凯芙拉
            "gtnr.bottleinchaos.recipe.category.kevlar.name",
            BottledChaosRecipes,
            RecipeCategorySetting::getDefault,
            builder -> BottledChaosRecipes.getDefaultRecipeCategory().handlerInfoCreator.apply(builder)
                .setDisplayStack(Materials.Kevlar.getPlates(1)))
        : null;

    public static final RecipeMap<RecipeMapBackend> StellarForgeAlloySmelterRecipeMap = ChaosConfig.enableBottleInChaos
        ? RecipeMapBuilder
            // #tr gtnr.chaos.recipe.StellarForgeAlloySmelterRecipeMap.name
            // # Stellar Forge: Alloy Smelter
            // # zh_CN 恒星锻炉: 合金冶炼
            .of("gtnr.chaos.recipe.StellarForgeAlloySmelterRecipeMap.name")
            .maxIO(9, 9, 3, 3)
            .progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
            .neiHandlerInfo(builder -> builder.setDisplayStack(GTNRItemList.BottleInChaos.get(1)))
            .useSpecialSlot()
            .build()
        : null;

    public static final RecipeCategory BottledChaosRocketFuelCategory = ChaosConfig.enableBottleInChaos
        ? new RecipeCategory(
            // #tr gtnr.bottleinchaos.recipe.category.rocket_fuel.name
            // # Rocket Fuel
            // # zh_CN 火箭燃料
            "gtnr.bottleinchaos.recipe.category.rocket_fuel.name",
            BottledChaosRecipes,
            RecipeCategorySetting::getDefault,
            builder -> BottledChaosRecipes.getDefaultRecipeCategory().handlerInfoCreator.apply(builder)
                .setDisplayStack(Materials.Carbon.getDust(1)))
        : null;

    public static final RecipeCategory BottledChaosNaquadahFuelCategory = ChaosConfig.enableBottleInChaos
        ? new RecipeCategory(
            // #tr gtnr.bottleinchaos.recipe.category.naquadah_fuel.name
            // # Naquadah Fuel
            // # zh_CN 硅岩燃料
            "gtnr.bottleinchaos.recipe.category.naquadah_fuel.name",
            BottledChaosRecipes,
            RecipeCategorySetting::getDefault,
            builder -> BottledChaosRecipes.getDefaultRecipeCategory().handlerInfoCreator.apply(builder)
                .setDisplayStack(GGMaterial.extremelyUnstableNaquadah.get(OrePrefixes.dust, 1)))
        : null;

    public static final RecipeCategory BottledChaosTungstenCategory = ChaosConfig.enableBottleInChaos
        ? new RecipeCategory(
            // #tr gtnr.bottleinchaos.recipe.category.tungsten.name
            // # Tungsten
            // # zh_CN 钨
            "gtnr.bottleinchaos.recipe.category.tungsten.name",
            BottledChaosRecipes,
            RecipeCategorySetting::getDefault,
            builder -> BottledChaosRecipes.getDefaultRecipeCategory().handlerInfoCreator.apply(builder)
                .setDisplayStack(BotWerkstoffMaterialPool.TungstenTrioxide.get(OrePrefixes.dust, 1)))
        : null;

    public static final RecipeCategory BottledChaosNetheriteCategory = ChaosConfig.enableBottleInChaos
        ? new RecipeCategory(
            // #tr gtnr.bottleinchaos.recipe.category.netherite.name
            // # Netherite
            // # zh_CN 下界合金
            "gtnr.bottleinchaos.recipe.category.netherite.name",
            BottledChaosRecipes,
            RecipeCategorySetting::getDefault,
            builder -> BottledChaosRecipes.getDefaultRecipeCategory().handlerInfoCreator.apply(builder)
                .setDisplayStack(ItemList.Intensely_Bonded_Netherite_Nanoparticles.get(1)))
        : null;

    public static final RecipeCategory BottledChaosPlatinumCategory = ChaosConfig.enableBottleInChaos
        ? new RecipeCategory(
            // #tr gtnr.bottleinchaos.recipe.category.platinum.name
            // # Platinum Group
            // # zh_CN 铂系
            "gtnr.bottleinchaos.recipe.category.platinum.name",
            BottledChaosRecipes,
            RecipeCategorySetting::getDefault,
            builder -> BottledChaosRecipes.getDefaultRecipeCategory().handlerInfoCreator.apply(builder)
                .setDisplayStack(Materials.Platinum.getDust(1)))
        : null;

    public static final RecipeMap<RecipeMapBackend> StellarForgeRecipeMap = ChaosConfig.enableBottleInChaos
        ? RecipeMapBuilder
            // #tr gtnr.chaos.recipe.StellarForgeRecipeMap.name
            // # Stellar Forge
            // # zh_CN 恒星锻炉
            .of("gtnr.chaos.recipe.StellarForgeRecipeMap.name")
            .maxIO(6, 6, 1, 2)
            .progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
            .neiHandlerInfo(builder -> builder.setDisplayStack(GTNRItemList.BottleInChaos.get(1)))
            .useSpecialSlot()
            .build()
        : null;

    public static final RecipeCategory BottledChaosNaquadahCategory = ChaosConfig.enableBottleInChaos
        ? new RecipeCategory(
            // #tr gtnr.bottleinchaos.recipe.category.naquadah.name
            // # Naquadah Processing
            // # zh_CN 硅岩处理
            "gtnr.bottleinchaos.recipe.category.naquadah.name",
            BottledChaosRecipes,
            RecipeCategorySetting::getDefault,
            builder -> BottledChaosRecipes.getDefaultRecipeCategory().handlerInfoCreator.apply(builder)
                .setDisplayStack(Materials.Naquadria.getDust(1)))
        : null;

    public static final RecipeCategory BottledChaosLanthanideCategory = ChaosConfig.enableBottleInChaos
        ? new RecipeCategory(
            // #tr gtnr.bottleinchaos.recipe.category.lanthanide.name
            // # Lanthanides
            // # zh_CN 镧系
            "gtnr.bottleinchaos.recipe.category.lanthanide.name",
            BottledChaosRecipes,
            RecipeCategorySetting::getDefault,
            builder -> BottledChaosRecipes.getDefaultRecipeCategory().handlerInfoCreator.apply(builder)
                .setDisplayStack(Materials.Lanthanum.getDust(1)))
        : null;

    public static final RecipeCategory BottledChaosPurifiedWaterCategory = ChaosConfig.enableBottleInChaos
        ? new RecipeCategory(
            // #tr gtnr.bottleinchaos.recipe.category.purified_water.name
            // # Water Purification
            // # zh_CN 净化水
            "gtnr.bottleinchaos.recipe.category.purified_water.name",
            BottledChaosRecipes,
            RecipeCategorySetting::getDefault,
            builder -> BottledChaosRecipes.getDefaultRecipeCategory().handlerInfoCreator.apply(builder)
                .setDisplayStack(Materials.Grade8PurifiedWater.getCells(1)))
        : null;

    public static final RecipeCategory BottledChaosBiologicalCategory = ChaosConfig.enableBottleInChaos
        ? new RecipeCategory(
            // #tr gtnr.bottleinchaos.recipe.category.biological.name
            // # Biological Processing
            // # zh_CN 生物线
            "gtnr.bottleinchaos.recipe.category.biological.name",
            BottledChaosRecipes,
            RecipeCategorySetting::getDefault,
            builder -> BottledChaosRecipes.getDefaultRecipeCategory().handlerInfoCreator.apply(builder)
                .setDisplayStack(ItemList.Circuit_Chip_Biocell.get(1)))
        : null;

    public static final RecipeCategory BottledChaosIsaMillCategory = ChaosConfig.enableBottleInChaos
        ? new RecipeCategory(
            // #tr gtnr.bottleinchaos.recipe.category.isamill.name
            // # IsaMill Processing
            // # zh_CN 艾萨矿物处理
            "gtnr.bottleinchaos.recipe.category.isamill.name",
            BottledChaosRecipes,
            RecipeCategorySetting::getDefault,
            builder -> BottledChaosRecipes.getDefaultRecipeCategory().handlerInfoCreator.apply(builder)
                .setDisplayStack(GregtechItemList.Controller_IsaMill.get(1)))
        : null;
    public static final RecipeCategory BottledChaosWoodCategory = ChaosConfig.enableBottleInChaos ? new RecipeCategory(
        // #tr gtnr.bottleinchaos.recipe.category.wood.name
        // # Wood Chemical Processing
        // # zh_CN 木化
        "gtnr.bottleinchaos.recipe.category.wood.name",
        BottledChaosRecipes,
        RecipeCategorySetting::getDefault,
        builder -> BottledChaosRecipes.getDefaultRecipeCategory().handlerInfoCreator.apply(builder)
            .setDisplayStack(Materials.AshDark.getDust(1)))
        : null;

    public static final RecipeCategory BottledChaosPetrochemicalCategory = ChaosConfig.enableBottleInChaos
        ? new RecipeCategory(
            // #tr gtnr.bottleinchaos.recipe.category.petrochemical.name
            // # Petrochemical Processing
            // # zh_CN 石化
            "gtnr.bottleinchaos.recipe.category.petrochemical.name",
            BottledChaosRecipes,
            RecipeCategorySetting::getDefault,
            builder -> BottledChaosRecipes.getDefaultRecipeCategory().handlerInfoCreator.apply(builder)
                .setDisplayStack(Materials.Oil.getCells(1)))
        : null;

    public static final RecipeMap<ChaosRecipeBackend> ChaosRouteRecipes = ChaosConfig.enableBottleInChaos
        ? RecipeMapBuilder
            // #tr gtnr.bottleinchaos.recipe.credential.name
            // # Bottle in Chaos - Credential Injection
            // # zh_CN 混沌中瓶 - 凭证注入
            .of("gtnr.bottleinchaos.recipe.credential.name", ChaosRecipeBackend::new)
            .maxIO(9, 2, 0, 0)
            .disableRegisterNEI()
            .frontend(BottledChaosCredentialFrontend::new)
            .progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
            .neiRecipeComparator((r1, r2) -> 0)
            .neiHandlerInfo(builder -> {
                if (GTNRItemList.BottleInChaos.hasBeenSet()) builder.setDisplayStack(GTNRItemList.BottleInChaos.get(1));
                return builder.setHeight(64)
                    .setMultipleWidgetsAllowed(true);
            })
            .build()
        : null;

    public static final RecipeMap<ChaosRecipeBackend> ChaosStructureModuleRecipes = ChaosConfig.enableBottleInChaos
        ? RecipeMapBuilder
            // #tr gtnr.bottleinchaos.recipe.structure.name
            // # Bottle in Chaos - Machine Structure Packaging
            // # zh_CN 混沌中瓶 - 机器结构封装
            .of("gtnr.bottleinchaos.recipe.structure.name", ChaosRecipeBackend::new)
            .maxIO(BottledChaosStructureFrontend.INPUT_SLOTS, 2, 0, 0)
            .disableRegisterNEI()
            .frontend(BottledChaosStructureFrontend::new)
            .progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
            .neiRecipeComparator(Comparator.comparingInt(recipe -> {
                var display = recipe.getMetadata(BottledChaosStructureDisplay.DISPLAY);
                return display == null ? Integer.MAX_VALUE : display.poolOrder();
            }))
            .neiHandlerInfo(builder -> {
                if (GTNRItemList.BottleInChaos.hasBeenSet()) builder.setDisplayStack(GTNRItemList.BottleInChaos.get(1));
                return builder.setHeight(BottledChaosStructureFrontend.PAGE_HEIGHT)
                    .setMultipleWidgetsAllowed(true);
            })
            .build()
        : null;
}
