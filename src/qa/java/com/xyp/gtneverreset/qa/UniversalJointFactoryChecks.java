package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.xyp.gtneverreset.api.machine.MultiblockTooltipSource.TieredPart;
import com.xyp.gtneverreset.api.me.RecipeMapPatternInventory;
import com.xyp.gtneverreset.common.machine.hatch.CrossRecipeMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.hatch.ParameterModuleHatch;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.machine.multiblock.UniversalJointFactory;
import com.xyp.gtneverreset.common.machine.multiblock.UniversalJointFactoryProcessingLogic;
import com.xyp.gtneverreset.common.machine.tooltip.AutoMultiblockTooltipBuilder;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;
import com.xyp.gtneverreset.utils.enums.MachineModuleType;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.SingleRecipeCheck;
import gregtech.api.structure.error.StructureError;

@Mod(modid = "universalfactoryqa", name = "Universal Factory QA", version = "1", dependencies = "after:gtneverreset")
public final class UniversalJointFactoryChecks {

    private final StringBuilder report = new StringBuilder();
    private boolean launched, checked, opened, finished;
    private volatile boolean ready, modeClicked;
    private volatile Throwable failure;
    private int ticks, frames, stage;
    private BaseMetaTileEntity tile;
    private UniversalJointFactory factory;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !GregTechAPI.sFullLoadFinished) return;
        var players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) players.get(0);
        try {
            if (!checked) {
                checked = true;
                verify(player);
                ready = true;
            }
            if (ready && ++ticks > 60 && !opened) {
                require(factory.onRightclick(tile, player), "native factory GUI opened");
                opened = true;
            }
            if (modeClicked && factory.getMachineMode() == 1) {
                modeClicked = false;
                require(factory.getRecipeMap() == RecipeMaps.compressorRecipes, "GUI mode synchronized to server");
                stage = 2;
            }
            if (stage == 3) {
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(47, 111, -37, 45, 23);
                stage = 4;
            }
        } catch (Throwable error) {
            failure = error;
        }
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.renderDistanceChunks = 6;
            if (!launched && mc.currentScreen instanceof GuiMainMenu) {
                launched = true;
                mc.launchIntegratedServer(
                    "factory-" + System.currentTimeMillis(),
                    "Factory QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("Server checks", failure);
            if (ticks > 1800) throw new AssertionError("QA timeout at " + stage);
        } catch (Throwable error) {
            finish(error);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ready || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (stage == 4 && ++frames >= 100) {
                capture("factory-structure.png");
                mc.displayGuiScreen(new FactoryTooltipScreen(false));
                stage = 5;
                frames = 0;
                return;
            }
            if (stage == 5 && ++frames >= 40) {
                capture("factory-tooltip.png");
                mc.displayGuiScreen(new FactoryTooltipScreen(true));
                stage = 6;
                frames = 0;
                return;
            }
            if (stage == 6 && ++frames >= 40) {
                capture("factory-structure-tooltip.png");
                finish(null);
                return;
            }
            if (!(mc.currentScreen instanceof GuiContainerWrapper gui) || ++frames < 40) return;
            frames = 0;
            if (stage == 0) {
                List<String> tooltip = GTNRItemList.UniversalJointFactory.get(1)
                    .getTooltip(mc.thePlayer, false);
                Files.write(Path.of("factory-tooltip.txt"), tooltip);
                String text = String.join("\n", tooltip);
                require(
                    text.contains("32") && text.contains("64") && !text.contains("{var:"),
                    "automatic tooltip values resolved");
                require(
                    !text.contains("可用配方类型") && !text.contains("Recipe types:")
                        && !text.contains("不接受参数")
                        && !text.contains("Does not accept parameter"),
                    "tooltip omits recipe list and obsolete module restriction");
                require(
                    text.contains("20%") && text.contains("0.9")
                        && !text.contains("无损超频")
                        && !text.contains("Perfect overclock"),
                    "tooltip derives glass bonuses without an automatic perfect overclock");
                String structureText = String.join("\n", new FactoryTooltipScreen(true).lines);
                Files.writeString(Path.of("factory-structure-tooltip.txt"), structureText);
                require(
                    structureText.contains("参数模块") && structureText.contains("每台机器最多 1 个"),
                    "structure tooltip includes parameter ports and limits");
                capture("factory-gui.png");
                CycleButtonWidget button = findModeButton(
                    gui.getScreen()
                        .getMainPanel());
                require(button != null, "mode switch widget exists");
                button.onMousePressed(0);
                modeClicked = true;
                stage = 1;
            } else if (stage == 2) {
                capture("factory-gui-compressor.png");
                stage = 3;
            }
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void verify(EntityPlayerMP player) throws Exception {
        UniversalJointFactory detached = new UniversalJointFactory("qa");
        for (int tier = 1; tier <= 14; tier++) {
            var stats = detached.getTooltipStatsAtTier(TieredPart.VOLTAGE, tier);
            require(stats.parallels() == (64 << (tier - 1)), "tier " + tier + " parallel scaling");
            require(stats.recipeThreads() == 32 + (tier - 1) * 8, "tier " + tier + " thread scaling");
        }
        require(UniversalJointFactory.parallelsForTier(100) == Integer.MAX_VALUE, "parallel overflow saturates");
        require(
            UniversalJointFactory.threadsForTier(Integer.MAX_VALUE) == Integer.MAX_VALUE,
            "thread overflow saturates");
        for (MachineModuleType type : MachineModuleType.values())
            require(detached.getParameterModuleLimit(type) == 1, "one module per type " + type);
        for (int tier = VoltageIndex.HV; tier <= VoltageIndex.UXV; tier++) {
            var stats = detached.getTooltipStatsAtTier(TieredPart.GLASS, tier);
            int upgrades = tier - VoltageIndex.HV;
            require(Math.abs(stats.speed() - (1 + 0.2 * upgrades)) < 1e-9, "glass speed tier " + tier);
            require(Math.abs(stats.energyUsage() - Math.pow(0.9, upgrades)) < 1e-9, "glass EU tier " + tier);
            require(!stats.perfectOverclock(), "glass does not grant perfect overclock at tier " + tier);
        }
        require(UniversalJointFactory.RECIPE_MAPS.size() == 24, "all 24 recipe maps available");
        World world = player.worldObj;
        tile = place(world, 0, 80, 0, GTNRMachineID.UniversalJointFactory.id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        tile.disableWorking();
        factory = (UniversalJointFactory) tile.getMetaTileEntity();
        factory.fixAllIssues();
        factory.construct(new ItemStack(Items.stick), false);
        require(
            factory.getStructureDefinition()
                .check(factory, "main", world, factory.getExtendedFacing(), 0, 80, 0, 24, 15, 0, false),
            "49 x 31 x 49 world structure matches MBS1");
        String[][] shape = MultiblockStructureLoader.load("universal_joint_factory");
        require(
            shape.length == 31 && shape[0].length == 49 && shape[15][0].charAt(24) == '~',
            "shape dimensions and controller offset");
        int[] ids = { ItemList.Hatch_Energy_LV.get(1)
            .getItemDamage(),
            ItemList.Hatch_Input_Bus_LV.get(1)
                .getItemDamage(),
            ItemList.Hatch_Output_Bus_LV.get(1)
                .getItemDamage(),
            ItemList.Hatch_Maintenance.get(1)
                .getItemDamage(),
            ItemList.Hatch_Input_LV.get(1)
                .getItemDamage(),
            ItemList.Hatch_Output_LV.get(1)
                .getItemDamage() };
        int[] xs = { -4, -3, -2, -1, 1, 2 };
        for (int i = 0; i < ids.length; i++) place(world, xs[i], 80, 0, ids[i]);
        List<StructureError> errors = new ArrayList<>();
        factory.checkMachine(tile, null, errors);
        require(errors.isEmpty(), "native hatches form factory: " + errors);
        factory.mMachine = true;
        require(
            factory.getMaxParallelRecipes() == 64 && factory.getTotalRecipeThreads() == 32,
            "ordinary LV hatches have 64 parallels and 32 threads");
        verifyThreads(world, null);
        BaseMetaTileEntity patternTile = place(world, 3, 80, 0, GTNRMachineID.SuperCraftingInputME.id);
        var hatch = (SuperMTEHatchCraftingInputME) patternTile.getMetaTileEntity();
        require(factory.checkStructure(true, tile), "factory reforms with pattern hatch");
        require(factory.getTotalRecipeThreads() == 32, "pattern hatch enables 32 LV threads");
        verifyMatching(world, hatch);
        verifyThreads(world, hatch);
        var crossHatch = (CrossRecipeMEPatternBuffer) place(
            world,
            3,
            80,
            0,
            GTNRMachineID.CrossRecipeMEPatternBuffer.id).getMetaTileEntity();
        require(
            factory.checkStructure(true, tile) && factory.getTotalRecipeThreads() == 32,
            "cross-recipe assembly has the same 32 LV threads");
        verifyThreads(world, crossHatch);
        verifyGlassAndModules(world);
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(0.5, 80, -4, 0, 0);
    }

    private void verifyGlassAndModules(World world) {
        replaceGlass(world, Blocks.glass, 0);
        require(factory.checkStructure(true, tile), "ordinary glass forms factory at HV glass tier");
        require(
            factory.getSpeedMultiplier() == 1 && factory.getEUtMultiplier() == 1
                && factory.getOverclockTimeReduction() == 2,
            "ordinary glass has baseline processing parameters");
        replaceGlass(world, GregTechAPI.sBlockGlass1, 0);
        require(factory.checkStructure(true, tile), "EV glass forms factory");
        require(
            Math.abs(factory.getSpeedMultiplier() - 1.2) < 1e-9 && Math.abs(factory.getEUtMultiplier() - 0.9) < 1e-9
                && factory.getOverclockTimeReduction() == 2,
            "EV glass gains speed and EU efficiency without perfect OC");
        String[][] shape = MultiblockStructureLoader.load("universal_joint_factory");
        boolean changed = false;
        for (int y = 0; y < shape.length && !changed; y++) {
            for (int z = 0; z < shape[y].length && !changed; z++) {
                int x = shape[y][z].indexOf('I');
                if (x >= 0) {
                    int[] position = glassPosition(x, y, z);
                    world.setBlock(position[0], position[1], position[2], GregTechAPI.sBlockGlass1, 1, 3);
                    changed = true;
                }
            }
        }
        require(changed && !factory.checkStructure(true, tile), "mixed glass tiers cannot form factory");
        replaceGlass(world, GregTechAPI.sBlockGlass1, 1);
        require(factory.checkStructure(true, tile), "UHV glass forms factory");
        double uhvEu = Math.pow(0.9, 6);
        require(
            Math.abs(factory.getSpeedMultiplier() - 2.2) < 1e-9 && Math.abs(factory.getEUtMultiplier() - uhvEu) < 1e-9
                && factory.getOverclockTimeReduction() == 2
                && factory.getOverclockPowerIncrease() == 4,
            "UHV glass gives 220 percent speed and 53.1441 percent EU without perfect OC");
        var parallel = (ParameterModuleHatch) place(world, -3, 82, 0, GTNRMachineID.DynamicParallelControllerT1.id)
            .getMetaTileEntity();
        var speed = (ParameterModuleHatch) place(world, -2, 82, 0, GTNRMachineID.DynamicSpeedControllerT1.id)
            .getMetaTileEntity();
        place(world, -1, 82, 0, GTNRMachineID.PowerConsumptionControllerT1.id);
        place(world, 0, 82, 0, GTNRMachineID.PerfectOverclockController.id);
        parallel.setParameter(3);
        speed.setParameter(2);
        require(
            factory.checkStructure(true, tile) && factory.getParameterModules()
                .size() == 4,
            "all four module types form in both Q and X positions");
        require(
            factory.getMaxParallelRecipes() == 256 && factory.getTotalRecipeThreads() == 32,
            "parallel module scales LV parallels without changing cross-recipe capacity");
        require(
            Math.abs(factory.getSpeedMultiplier() - 4.4) < 1e-9
                && Math.abs(factory.getEUtMultiplier() - uhvEu * 0.95) < 1e-9,
            "speed and EU modules multiply glass bonuses");
        require(
            factory.getOverclockTimeReduction() == 8 && factory.getOverclockPowerIncrease() == 4,
            "overclock module keeps its stronger overclock effect");
        parallel.setParameter(5);
        require(factory.getMaxParallelRecipes() == 384, "dynamic module parameter updates effective parallels");
        place(world, 1, 82, 0, GTNRMachineID.StaticParallelControllerT1.id);
        require(!factory.checkStructure(true, tile), "duplicate parallel module blocks formation");
        world.setBlock(1, 82, 0, GregTechAPI.sBlockCasings2, 0, 3);
        require(factory.checkStructure(true, tile), "removing duplicate module restores formation");
        place(
            world,
            -4,
            80,
            0,
            ItemList.Hatch_Energy_MV.get(1)
                .getItemDamage());
        require(
            factory.checkStructure(true, tile) && factory.getMaxParallelRecipes() == 768
                && factory.getTotalRecipeThreads() == 40,
            "MV voltage scaling combines with parameter module");
        place(
            world,
            -4,
            80,
            0,
            ItemList.Hatch_Energy_LV.get(1)
                .getItemDamage());
        for (int x = -3; x <= 0; x++) world.setBlock(x, 82, 0, GregTechAPI.sBlockCasings2, 0, 3);
        require(
            factory.checkStructure(true, tile) && factory.getParameterModules()
                .isEmpty() && factory.getMaxParallelRecipes() == 64 && factory.getTotalRecipeThreads() == 32,
            "removed modules do not leave stale multipliers");
        require(
            Math.abs(factory.getSpeedMultiplier() - 2.2) < 1e-9 && Math.abs(factory.getEUtMultiplier() - uhvEu) < 1e-9
                && factory.getOverclockTimeReduction() == 2,
            "module removal preserves glass bonuses and restores ordinary overclocking");
    }

    private void replaceGlass(World world, net.minecraft.block.Block block, int meta) {
        String[][] shape = MultiblockStructureLoader.load("universal_joint_factory");
        for (int y = 0; y < shape.length; y++) {
            for (int z = 0; z < shape[y].length; z++) {
                for (int x = 0; x < shape[y][z].length(); x++) {
                    if (shape[y][z].charAt(x) == 'I') {
                        int[] position = glassPosition(x, y, z);
                        world.setBlock(position[0], position[1], position[2], block, meta, 3);
                    }
                }
            }
        }
    }

    private int[] glassPosition(int x, int y, int z) {
        int[] position = new int[3];
        factory.getExtendedFacing()
            .getWorldOffset(new int[] { x - 24, y - 15, z }, position);
        position[1] += 80;
        return position;
    }

    private void verifyMatching(World world, SuperMTEHatchCraftingInputME hatch) throws Exception {
        ItemStack input = new ItemStack(Items.firework_charge);
        GTValues.RA.stdBuilder()
            .itemInputs(input.copy())
            .itemOutputs(new ItemStack(Items.paper))
            .duration(200)
            .eut(8)
            .addTo(RecipeMaps.extractorRecipes);
        GTValues.RA.stdBuilder()
            .itemInputs(input.copy())
            .itemOutputs(new ItemStack(Items.emerald))
            .duration(200)
            .eut(8)
            .addTo(RecipeMaps.compressorRecipes);
        UniversalJointFactoryProcessingLogic logic = new UniversalJointFactoryProcessingLogic(factory);
        logic.setMachine(factory)
            .setRecipeMapSupplier(factory::getRecipeMap)
            .setAvailableVoltage(32)
            .setAvailableAmperage(512)
            .setMaxParallel(64)
            .setVoidProtection(false, false);
        factory.setMachineMode(18);
        ItemStack material = input.copy();
        material.stackSize = 64;
        var ordinary = liveSlot(
            hatch,
            pattern(input, new ItemStack(Items.emerald)),
            RecipeMaps.compressorRecipes,
            material);
        logic.prepareRecipe(16384);
        require(logic.tryCachePossibleRecipesFromPattern(ordinary), "ordinary assembly pattern accepted");
        logic.setInputItems(ordinary.getItemInputs())
            .setInputFluids();
        require(
            !logic.process()
                .wasSuccessful() && material.stackSize == 64,
            "ordinary assembly cannot override selected mode even with recipe-map metadata");
        logic.finishRecipe();
        factory.setMachineMode(1);
        logic.prepareRecipe(16384);
        logic.tryCachePossibleRecipesFromPattern(ordinary);
        logic.setInputItems(ordinary.getItemInputs());
        require(
            logic.process()
                .wasSuccessful() && logic.getOutputItems()[0].getItem() == Items.emerald,
            "ordinary assembly follows controller mode switch");
        logic.finishRecipe();
        factory.setMachineMode(18);
        QaSlot slot = new QaSlot(pattern(input, new ItemStack(Items.emerald)), hatch, world, material);
        logic.prepareRecipe(16384);
        logic.tryCachePossibleRecipesFromPattern(slot);
        logic.setInputItems(material);
        require(
            !logic.process()
                .wasSuccessful() && material.stackSize == 64,
            "unannotated cross-recipe pattern stays in selected mode");
        logic.finishRecipe();
        slot.recipeMapName = RecipeMaps.compressorRecipes.unlocalizedName;
        logic.prepareRecipe(16384);
        logic.tryCachePossibleRecipesFromPattern(slot);
        logic.setInputItems(slot.getItemInputs())
            .setInputFluids();
        require(
            logic.process()
                .wasSuccessful(),
            "cross-map recipe matches while selected mode is extractor");
        require(
            logic.getCurrentParallels() == 64 && logic.getOutputItems()[0].getItem() == Items.emerald,
            "correct output distinguishes identical inputs in two maps");
        require(material.stackSize == 64, "input consumption remains transactional");
        require(logic.commitInputs() && material.stackSize == 0, "successful parallel batch commits once");
        require(factory.getRecipeMap() == RecipeMaps.extractorRecipes, "temporary map does not alter selected mode");
        material = input.copy();
        logic.prepareRecipe(16384);
        logic.setInputItems(material)
            .setInputFluids();
        require(
            logic.process()
                .wasSuccessful() && logic.getOutputItems()[0].getItem() == Items.paper,
            "ordinary inputs stay in selected map after pattern processing");
        logic.finishRecipe();
        // A pattern listing two ingredients must not settle for an output-identical recipe using only one of them.
        factory.setMachineMode(1);
        ItemStack base = new ItemStack(Items.firework_charge);
        ItemStack extra = new ItemStack(Items.gunpowder);
        ItemStack product = new ItemStack(Items.gold_ingot);
        GTValues.RA.stdBuilder()
            .itemInputs(base.copy())
            .itemOutputs(product.copy())
            .duration(200)
            .eut(8)
            .addTo(RecipeMaps.compressorRecipes);
        GTValues.RA.stdBuilder()
            .itemInputs(base.copy(), extra.copy())
            .itemOutputs(product.copy())
            .duration(200)
            .eut(8)
            .addTo(RecipeMaps.compressorRecipes);
        ItemStack baseStock = base.copy(), extraStock = extra.copy();
        baseStock.stackSize = 4;
        extraStock.stackSize = 4;
        var combined = liveSlot(
            hatch,
            pattern(new ItemStack[] { base, extra }, product),
            RecipeMaps.compressorRecipes,
            baseStock,
            extraStock);
        logic.prepareRecipe(16384);
        require(logic.tryCachePossibleRecipesFromPattern(combined), "two-ingredient pattern accepted");
        logic.setInputItems(combined.getItemInputs())
            .setInputFluids();
        require(
            logic.process()
                .wasSuccessful(),
            "two-ingredient pattern finds a recipe");
        require(
            logic.getPlannedConsumption(baseStock) > 0 && logic.getPlannedConsumption(extraStock) > 0,
            "pattern never uses an output-identical recipe that skips a listed ingredient");
        require(logic.commitInputs(), "two-ingredient pattern commits inputs");
        logic.finishRecipe();
        // A stale pattern whose extra ingredient no recipe consumes must not produce through a partial recipe.
        ItemStack unused = new ItemStack(Items.record_11);
        ItemStack baseStock2 = base.copy(), extraStock2 = extra.copy(), unusedStock = unused.copy();
        baseStock2.stackSize = 4;
        extraStock2.stackSize = 4;
        unusedStock.stackSize = 1;
        var stale = liveSlot(
            hatch,
            pattern(new ItemStack[] { base, extra, unused }, product),
            RecipeMaps.compressorRecipes,
            baseStock2,
            extraStock2,
            unusedStock);
        logic.prepareRecipe(16384);
        require(logic.tryCachePossibleRecipesFromPattern(stale), "stale pattern with an unused ingredient accepted");
        logic.setInputItems(stale.getItemInputs())
            .setInputFluids();
        require(
            !logic.process()
                .wasSuccessful() && baseStock2.stackSize == 4
                && extraStock2.stackSize == 4
                && unusedStock.stackSize == 1,
            "pattern with an unused ingredient keeps inputs and produces nothing");
        logic.finishRecipe();
        factory.setMachineMode(18);
        slot = new QaSlot(pattern(input, new ItemStack(Items.diamond)), hatch, world, material);
        logic.prepareRecipe(16384);
        logic.tryCachePossibleRecipesFromPattern(slot);
        logic.setInputItems(material);
        require(
            !logic.process()
                .wasSuccessful() && material.stackSize == 1,
            "wrong requested output cannot consume inputs");
        logic.finishRecipe();
        slot = new QaSlot(pattern(input, new ItemStack(Items.emerald)), hatch, world, material);
        slot.recipeMapName = RecipeMaps.compressorRecipes.unlocalizedName;
        logic.setRecipeLocking(factory, true);
        logic.prepareRecipe(16384);
        logic.tryCachePossibleRecipesFromPattern(slot);
        logic.setInputItems(material);
        require(
            logic.process()
                .wasSuccessful(),
            "cross-map recipe can be locked");
        require(
            factory.getSingleRecipeCheck()
                .getRecipeMap() == RecipeMaps.compressorRecipes,
            "recipe lock stores the actual pool");
        NBTTagCompound saved = factory.getSingleRecipeCheck()
            .writeToNBT();
        require(
            SingleRecipeCheck.tryLoad(RecipeMaps.benderRecipes, saved) != null,
            "cross-map recipe lock survives NBT");
        logic.finishRecipe();
        ordinary = liveSlot(
            hatch,
            pattern(input, new ItemStack(Items.emerald)),
            RecipeMaps.compressorRecipes,
            material);
        logic.prepareRecipe(16384);
        logic.tryCachePossibleRecipesFromPattern(ordinary);
        logic.setInputItems(material);
        require(
            !logic.process()
                .wasSuccessful() && material.stackSize == 1,
            "cross-map recipe lock cannot bypass ordinary assembly selected mode");
        logic.finishRecipe();
        factory.setSingleRecipeCheck(null);
        logic.setRecipeLocking(factory, false);
        slot.recipeMapName = RecipeMaps.extractorRecipes.unlocalizedName;
        logic.prepareRecipe(16384);
        require(logic.tryCachePossibleRecipesFromPattern(slot), "supported explicit recipe map accepted");
        logic.setInputItems(material);
        require(
            !logic.process()
                .wasSuccessful() && material.stackSize == 1,
            "explicit pool cannot fall back to another pool with matching outputs");
        logic.finishRecipe();
        slot.recipeMapName = RecipeMaps.compressorRecipes.unlocalizedName;
        logic.prepareRecipe(16384);
        require(logic.tryCachePossibleRecipesFromPattern(slot), "matching explicit pool accepted");
        logic.setInputItems(material);
        require(
            logic.process()
                .wasSuccessful(),
            "explicit pool processes the requested recipe");
        logic.finishRecipe();
        slot.recipeMapName = RecipeMaps.blastFurnaceRecipes.unlocalizedName;
        require(
            !logic.tryCachePossibleRecipesFromPattern(slot) && material.stackSize == 1,
            "unsupported explicit pool rejected without consumption");
        slot.recipeMapName = "gtnr.qa.missing_recipe_map";
        require(
            !logic.tryCachePossibleRecipesFromPattern(slot) && material.stackSize == 1,
            "unknown explicit pool rejected without consumption");
        factory.setMachineMode(23);
        NBTTagCompound machineTag = new NBTTagCompound();
        factory.saveNBTData(machineTag);
        UniversalJointFactory restored = (UniversalJointFactory) place(
            world,
            60,
            80,
            0,
            GTNRMachineID.UniversalJointFactory.id).getMetaTileEntity();
        restored.loadNBTData(machineTag);
        require(
            restored.getRecipeMap() == RecipeMaps.oreWasherRecipes && restored.nextMachineMode() == 0,
            "selected mode persists and wraps");
        factory.setMachineMode(0);
        factory.setMachineMode(-1);
        factory.setMachineMode(24);
        require(factory.getMachineMode() == 0, "invalid synchronized mode rejected");
    }

    private void verifyThreads(World world, SuperMTEHatchCraftingInputME hatch) throws Exception {
        boolean crossRecipe = hatch instanceof CrossRecipeMEPatternBuffer;
        String inputs = hatch == null ? "ordinary hatches"
            : crossRecipe ? "cross-recipe assembly" : "ordinary assembly";
        ItemStack ingredient = new ItemStack(
            hatch == null ? Items.record_13 : crossRecipe ? Items.record_cat : Items.ghast_tear);
        GTValues.RA.stdBuilder()
            .itemInputs(ingredient.copy())
            .itemOutputs(new ItemStack(Items.stick))
            .duration(200)
            .eut(1)
            .addTo(RecipeMaps.extractorRecipes);
        GTValues.RA.stdBuilder()
            .itemInputs(ingredient.copy())
            .itemOutputs(new ItemStack(Items.gold_nugget))
            .duration(200)
            .eut(1)
            .addTo(RecipeMaps.compressorRecipes);
        SuperMTEHatchCraftingInputME.PatternSlot<?>[] slots = null;
        if (hatch != null) {
            java.lang.reflect.Field field = SuperMTEHatchCraftingInputME.class.getDeclaredField("internalInventory");
            field.setAccessible(true);
            slots = (SuperMTEHatchCraftingInputME.PatternSlot<?>[]) field.get(hatch);
        }
        ItemStack first = ingredient.copy(), second = ingredient.copy();
        first.stackSize = 64;
        second.stackSize = 64;
        var inputBus = ((BaseMetaTileEntity) world.getTileEntity(-3, 80, 0)).getMetaTileEntity();
        if (slots == null) {
            inputBus.setInventorySlotContents(0, first);
            inputBus.setInventorySlotContents(1, second);
        } else {
            slots[0] = liveSlot(
                hatch,
                pattern(ingredient, new ItemStack(Items.stick)),
                RecipeMaps.extractorRecipes,
                first);
            slots[1] = liveSlot(
                hatch,
                pattern(ingredient, new ItemStack(crossRecipe ? Items.gold_nugget : Items.stick)),
                RecipeMaps.compressorRecipes,
                second);
        }
        NBTTagCompound before = new NBTTagCompound();
        factory.saveNBTData(before);
        factory.setMachineMode(18);
        factory.setBatchMode(false);
        for (int i = 0; i < 32; i++) require(
            factory.checkProcessing()
                .wasSuccessful(),
            inputs + ": start live thread " + (i + 1));
        require(
            factory.getRecipeThreadTasks()
                .size() == 32,
            inputs + ": 32 independent jobs remain active together");
        int remaining = first.stackSize + second.stackSize;
        require(
            !factory.checkProcessing()
                .wasSuccessful() && remaining == first.stackSize + second.stackSize,
            inputs + ": 33rd job rejected without consuming inputs");
        List<ItemStack> products = new ArrayList<>();
        factory.getRecipeThreadTasks()
            .forEach(
                task -> task.getOutputs()
                    .appendDisplayItems(products));
        require(
            products.stream()
                .anyMatch(stack -> stack.getItem() == Items.stick)
                && products.stream()
                    .anyMatch(stack -> stack.getItem() == Items.gold_nugget) == crossRecipe,
            inputs + ": only cross-recipe assembly runs different recipe types together");
        NBTTagCompound saved = new NBTTagCompound();
        factory.saveNBTData(saved);
        UniversalJointFactory restored = (UniversalJointFactory) place(
            world,
            62,
            80,
            0,
            GTNRMachineID.UniversalJointFactory.id).getMetaTileEntity();
        restored.loadNBTData(saved);
        require(
            restored.getRecipeThreadTasks()
                .size() == 32,
            inputs + ": all 32 jobs survive NBT reload");
        factory.loadNBTData(before);
        if (slots == null) {
            inputBus.setInventorySlotContents(0, null);
            inputBus.setInventorySlotContents(1, null);
        } else {
            slots[0] = null;
            slots[1] = null;
        }
    }

    @SuppressWarnings("unchecked")
    private SuperMTEHatchCraftingInputME.PatternSlot<?> liveSlot(SuperMTEHatchCraftingInputME hatch, ItemStack encoded,
        RecipeMap<?> map, ItemStack... materials) throws Exception {
        PatternRecipeMap.write(encoded, map == null ? null : map.unlocalizedName);
        var create = hatch.getClass()
            .getDeclaredMethod("createPatternSlot", ItemStack.class, NBTTagCompound.class);
        create.setAccessible(true);
        var slot = (SuperMTEHatchCraftingInputME.PatternSlot<?>) create.invoke(hatch, encoded, null);
        var inventory = SuperMTEHatchCraftingInputME.PatternSlot.class.getDeclaredField("itemInventory");
        inventory.setAccessible(true);
        for (ItemStack material : materials) ((List<ItemStack>) inventory.get(slot)).add(material);
        return slot;
    }

    private static ItemStack pattern(ItemStack input, ItemStack output) {
        return pattern(new ItemStack[] { input }, output);
    }

    private static ItemStack pattern(ItemStack[] inputs, ItemStack output) {
        ItemStack encoded = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList in = new NBTTagList(), out = new NBTTagList();
        for (ItemStack input : inputs) in.appendTag(
            AEItemStack.create(input)
                .toNBTGeneric());
        out.appendTag(
            AEItemStack.create(output)
                .toNBTGeneric());
        tag.setTag("in", in);
        tag.setTag("out", out);
        encoded.setTagCompound(tag);
        return encoded;
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, int id) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity result = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        result.setInitialValuesAsNBT(null, (short) id);
        return result;
    }

    private static CycleButtonWidget findModeButton(IWidget widget) {
        if (widget instanceof CycleButtonWidget cycle) return cycle;
        for (IWidget child : widget.getChildren()) {
            CycleButtonWidget found = findModeButton(child);
            if (found != null) return found;
        }
        return null;
    }

    private void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        report.append("PASS ")
            .append(message)
            .append('\n');
        System.out.println("[factory-qa] PASS " + message);
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(new File("."), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private void finish(Throwable error) {
        finished = true;
        if (error != null) error.printStackTrace();
        try {
            Files.writeString(Path.of("report.txt"), report);
            Files.writeString(Path.of("result.txt"), error == null ? "PASS" : "FAIL " + error);
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        Thread exit = new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                Thread.currentThread()
                    .interrupt();
            }
            FMLCommonHandler.instance()
                .exitJava(error == null ? 0 : 1, true);
        }, "factory-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }

    private static final class FactoryTooltipScreen extends GuiScreen {

        private final List<String> lines;

        private FactoryTooltipScreen(boolean structure) {
            lines = structure
                ? List.of(
                    new AutoMultiblockTooltipBuilder().addAutoTooltip(new UniversalJointFactory("qa-tooltip"))
                        .toolTipFinisher()
                        .getStructureInformation())
                : GTNRItemList.UniversalJointFactory.get(1)
                    .getTooltip(Minecraft.getMinecraft().thePlayer, false);
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            drawHoveringText(lines, 20, 20, fontRendererObj);
        }
    }

    private static final class QaSlot extends SuperMTEHatchCraftingInputME.PatternSlot<SuperMTEHatchCraftingInputME>
        implements RecipeMapPatternInventory {

        private String recipeMapName;

        private QaSlot(ItemStack encoded, SuperMTEHatchCraftingInputME hatch, World world, ItemStack material) {
            super(encoded, null, hatch, ((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded, world));
            itemInventory.add(material);
        }

        @Override
        public String getRecipeMapName() {
            return recipeMapName;
        }
    }
}
