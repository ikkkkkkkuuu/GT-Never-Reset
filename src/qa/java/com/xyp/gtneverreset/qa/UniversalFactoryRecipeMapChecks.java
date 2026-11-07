package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.xyp.gtneverreset.common.machine.hatch.CrossRecipeMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.multiblock.UniversalJointFactory;
import com.xyp.gtneverreset.common.machine.multiblock.UniversalJointFactoryProcessingLogic;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;

import appeng.api.AEApi;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.RecipeCatalysts;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.SingleRecipeCheck;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTRecipe;

@Mod(
    modid = "universalfactoryrecipemapqa",
    name = "Universal Factory Recipe Map Regression QA",
    version = "1",
    dependencies = "after:gtneverreset")
public final class UniversalFactoryRecipeMapChecks {

    private final StringBuilder report = new StringBuilder();
    private boolean launched, checked, opened, finished;
    private volatile boolean ready;
    private volatile Throwable failure;
    private int ticks, frames, serverTicks, neiStage;
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
        try {
            EntityPlayerMP player = (EntityPlayerMP) players.get(0);
            if (!checked) {
                checked = true;
                World world = player.worldObj;
                tile = place(world, 0, GTNRMachineID.UniversalJointFactory.id);
                tile.setOwnerName(player.getCommandSenderName());
                tile.setOwnerUuid(player.getUniqueID());
                tile.disableWorking();
                factory = (UniversalJointFactory) tile.getMetaTileEntity();
                factory.fixAllIssues();
                var hatch = (CrossRecipeMEPatternBuffer) place(world, 1, GTNRMachineID.CrossRecipeMEPatternBuffer.id)
                    .getMetaTileEntity();
                verifyIndustrialRecipes(factory, hatch);
                verifyCutting(factory, hatch);
                factory.setMachineMode(3);
                player.capabilities.allowFlying = true;
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                player.playerNetServerHandler.setPlayerLocation(0.5, 80, -3, 0, 0);
                ready = true;
            }
            if (ready && !opened && ++serverTicks >= 60) {
                require(factory.onRightclick(tile, player), "factory GUI opened after regression checks");
                opened = true;
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
            if (!launched && mc.currentScreen instanceof GuiMainMenu) {
                launched = true;
                mc.launchIntegratedServer(
                    "factory-recipe-map-" + System.currentTimeMillis(),
                    "Factory Recipe Map QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("Server regression checks", failure);
            if (launched && ++ticks > 1800) throw new AssertionError("QA timeout");
        } catch (Throwable error) {
            finish(error);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ready || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiContainerWrapper || mc.currentScreen instanceof GuiCraftingRecipe)
            || ++frames < 40) return;
        try {
            frames = 0;
            if (neiStage == 0) {
                verifyNeiCatalysts();
                require(
                    GuiCraftingRecipe.openRecipeGui(RecipeMaps.mixerNonCellRecipes.unlocalizedName),
                    "industrial mixer NEI page opened");
                neiStage = 1;
                return;
            }
            ScreenShotHelper.saveScreenshot(
                new File("."),
                neiStage == 1 ? "factory-industrial-mixer-nei.png"
                    : neiStage == 2 ? "factory-small-mixer-nei.png" : "factory-cutter-nei.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            if (neiStage == 1) {
                require(
                    GuiCraftingRecipe.openRecipeGui(RecipeMaps.mixerRecipes.unlocalizedName),
                    "small mixer NEI page opened");
                neiStage = 2;
                return;
            }
            if (neiStage == 2) {
                require(
                    GuiCraftingRecipe.openRecipeGui(RecipeMaps.cutterFakeRecipes.unlocalizedName),
                    "cutter NEI page opened");
                neiStage = 3;
                return;
            }
            finish(null);
        } catch (Throwable error) {
            finish(error);
        }
    }

    private static RecipeMap<?>[][] industrialMaps() {
        return new RecipeMap<?>[][] { { RecipeMaps.mixerRecipes, RecipeMaps.mixerNonCellRecipes },
            { RecipeMaps.chemicalReactorRecipes, RecipeMaps.multiblockChemicalReactorRecipes },
            { RecipeMaps.centrifugeRecipes, RecipeMaps.centrifugeNonCellRecipes },
            { RecipeMaps.electrolyzerRecipes, RecipeMaps.electrolyzerNonCellRecipes },
            { RecipeMaps.chemicalDehydratorRecipes, RecipeMaps.chemicalDehydratorNonCellRecipes } };
    }

    private void verifyNeiCatalysts() {
        ItemStack stack = GTNRItemList.UniversalJointFactory.get(1);
        for (RecipeMap<?>[] maps : industrialMaps()) {
            require(
                !RecipeCatalysts.containsCatalyst(maps[0].unlocalizedName, stack),
                "factory absent from small-machine NEI page " + maps[0].unlocalizedName);
            require(
                RecipeCatalysts.containsCatalyst(maps[1].unlocalizedName, stack),
                "factory present on industrial NEI page " + maps[1].unlocalizedName);
        }
        require(
            RecipeCatalysts.containsCatalyst(RecipeMaps.cutterFakeRecipes.unlocalizedName, stack),
            "factory present on shared cutter NEI page");
    }

    private void verifyIndustrialRecipes(UniversalJointFactory factory, CrossRecipeMEPatternBuffer hatch) {
        for (RecipeMap<?>[] maps : industrialMaps()) {
            RecipeMap<?> map = maps[1];
            String name = map.unlocalizedName;
            GTRecipe recipe = map.getAllRecipes()
                .stream()
                .filter(UniversalFactoryRecipeMapChecks::deterministicRecipe)
                .findFirst()
                .orElseThrow(() -> new AssertionError("native recipe missing: " + name));
            factory.setMachineMode(18);
            UniversalJointFactoryProcessingLogic logic = new UniversalJointFactoryProcessingLogic(factory);
            logic.setMachine(factory)
                .setRecipeMapSupplier(factory::getRecipeMap)
                .setAvailableVoltage(Math.max(32, recipe.mEUt))
                .setAvailableAmperage(1)
                .setMaxParallel(1)
                .setVoidProtection(false, false);
            hatch.onPatternChange(0, pattern(recipe, maps[0].unlocalizedName));
            require(
                !logic.tryCachePossibleRecipesFromPattern(
                    hatch.inventories()
                        .next()),
                maps[0].unlocalizedName + " is rejected without legacy pattern remapping");
            require(
                PatternRecipeMap.nameForImport(map)
                    .equals(name),
                name + " import retains industrial map");
            hatch.onPatternChange(0, pattern(recipe, PatternRecipeMap.nameForImport(map)));
            logic.prepareRecipe(Integer.MAX_VALUE);
            require(
                logic.tryCachePossibleRecipesFromPattern(
                    hatch.inventories()
                        .next()),
                name + " accepts industrial pattern with extractor selected");
            ItemStack[] inputs = new ItemStack[recipe.mInputs.length];
            for (int i = 0; i < inputs.length; i++) {
                inputs[i] = recipe.mInputs[i].copy();
                inputs[i].stackSize = Math.max(1, inputs[i].stackSize);
            }
            FluidStack[] fluids = Arrays.stream(recipe.mFluidInputs)
                .map(FluidStack::copy)
                .toArray(FluidStack[]::new);
            logic.setInputItems(inputs)
                .setInputFluids(fluids);
            logic.setRecipeLocking(factory, true);
            require(
                logic.process()
                    .wasSuccessful(),
                name + " executes native industrial recipe");
            ItemStack[] itemOutputs = logic.getOutputItems() == null ? new ItemStack[0] : logic.getOutputItems();
            FluidStack[] fluidOutputs = logic.getOutputFluids() == null ? new FluidStack[0] : logic.getOutputFluids();
            require(
                itemOutputs.length == recipe.mOutputs.length && fluidOutputs.length == recipe.mFluidOutputs.length,
                name + " preserves output types");
            for (int i = 0; i < recipe.mOutputs.length; i++) require(
                itemOutputs[i].isItemEqual(recipe.mOutputs[i])
                    && itemOutputs[i].stackSize == recipe.mOutputs[i].stackSize,
                name + " produces exact item output " + i);
            for (int i = 0; i < recipe.mFluidOutputs.length; i++) require(
                fluidOutputs[i].isFluidStackIdentical(recipe.mFluidOutputs[i]),
                name + " produces exact fluid output " + i);
            require(logic.commitInputs(), name + " commits inputs");
            for (int i = 0; i < inputs.length; i++) require(
                inputs[i].stackSize == (recipe.mInputs[i].stackSize == 0 ? 1 : 0),
                name + " consumes material and preserves catalyst " + i);
            for (FluidStack fluid : fluids) require(fluid.amount == 0, name + " consumes fluid input");
            require(
                factory.getSingleRecipeCheck()
                    .getRecipeMap() == map,
                name + " locks executable industrial map");
            require(factory.getMachineMode() == 18, name + " restores selected extractor mode");
            factory.setSingleRecipeCheck(null);
        }
    }

    private static boolean deterministicRecipe(GTRecipe recipe) {
        if (!recipe.mEnabled || recipe.mFakeRecipe
            || recipe.mDuration <= 0
            || recipe.mEUt <= 0
            || recipe.mEUt > 32
            || recipe.mOutputs.length + recipe.mFluidOutputs.length == 0) return false;
        for (ItemStack input : recipe.mInputs) if (input == null || input.getItemDamage() == 32767) return false;
        for (FluidStack input : recipe.mFluidInputs)
            if (input == null || input.getClass() != FluidStack.class) return false;
        for (int i = 0; i < recipe.mOutputs.length; i++)
            if (recipe.mOutputs[i] == null || recipe.getOutputChance(i) != 10000) return false;
        for (int i = 0; i < recipe.mFluidOutputs.length; i++)
            if (recipe.mFluidOutputs[i] == null || recipe.getFluidOutputChance(i) != 10000) return false;
        return true;
    }

    private void verifyCutting(UniversalJointFactory factory, CrossRecipeMEPatternBuffer hatch) {
        require(
            PatternRecipeMap.nameForImport(RecipeMaps.cutterFakeRecipes)
                .equals(RecipeMaps.cutterRecipes.unlocalizedName),
            "new NEI cutter import stores executable recipe map");
        require(
            PatternRecipeMap.resolve(RecipeMaps.cutterFakeRecipes.unlocalizedName) == RecipeMaps.cutterFakeRecipes,
            "old cutter pattern identifiers are not remapped");
        require(PatternRecipeMap.resolve(null) == null, "unannotated patterns remain unannotated");
        require(PatternRecipeMap.resolve("") == null, "empty recipe map remains invalid");
        require(PatternRecipeMap.resolve("gt.recipe.missing") == null, "unknown recipe map remains invalid");
        require(
            PatternRecipeMap.resolve(RecipeMaps.compressorRecipes.unlocalizedName) == RecipeMaps.compressorRecipes,
            "unrelated recipe maps keep their identity");
        ItemStack silicon = ItemList.Circuit_Silicon_Ingot.get(1);
        FluidStack[] coolants = { Materials.Water.getFluid(1000), GTModHandler.getDistilledWater(1000),
            Materials.Lubricant.getFluid(1000), Materials.DimensionallyShiftedSuperfluid.getFluid(1000) };
        for (FluidStack coolant : coolants) {
            GTRecipe recipe = RecipeMaps.cutterRecipes.findRecipeQuery()
                .items(silicon)
                .fluids(coolant)
                .find();
            require(
                recipe != null,
                "native silicon cutting recipe exists for " + coolant.getFluid()
                    .getName());
            verifyCuttingRecipe(factory, hatch, recipe);
        }
    }

    private void verifyCuttingRecipe(UniversalJointFactory factory, CrossRecipeMEPatternBuffer hatch, GTRecipe recipe) {
        hatch.onPatternChange(0, pattern(recipe, RecipeMaps.cutterFakeRecipes.unlocalizedName));
        require(
            !new UniversalJointFactoryProcessingLogic(factory).tryCachePossibleRecipesFromPattern(
                hatch.inventories()
                    .next()),
            "old display-map cutting pattern requires re-encoding");
        for (String map : new String[] { null, PatternRecipeMap.nameForImport(RecipeMaps.cutterFakeRecipes) }) {
            boolean usePattern = map != null;
            factory.setMachineMode(usePattern ? 18 : 3);
            factory.setSingleRecipeCheck(null);
            UniversalJointFactoryProcessingLogic logic = new UniversalJointFactoryProcessingLogic(factory);
            logic.setMachine(factory)
                .setRecipeMapSupplier(factory::getRecipeMap)
                .setAvailableVoltage(32)
                .setAvailableAmperage(512)
                .setMaxParallel(64)
                .setVoidProtection(false, false);
            ItemStack material = recipe.mInputs[0].copy();
            material.stackSize *= 3;
            FluidStack fluid = recipe.mFluidInputs[0].copy();
            fluid.amount *= 2;
            String mode = (usePattern ? map : "ordinary cutter") + " / "
                + fluid.getFluid()
                    .getName();
            logic.prepareRecipe(16384);
            if (usePattern) {
                ItemStack encoded = pattern(recipe, map);
                // Newly imported recipe identifiers survive normal pattern NBT persistence.
                encoded = ItemStack.loadItemStackFromNBT(encoded.writeToNBT(new NBTTagCompound()));
                require(map.equals(PatternRecipeMap.name(encoded)), mode + " preserves stored recipe-map identifier");
                hatch.onPatternChange(0, encoded);
                require(
                    logic.tryCachePossibleRecipesFromPattern(
                        hatch.inventories()
                            .next()),
                    mode + " accepts cutting pattern with extractor selected");
            }
            logic.setRecipeLocking(factory, usePattern);
            logic.setInputItems(material)
                .setInputFluids(fluid);
            require(
                logic.process()
                    .wasSuccessful(),
                mode + " cuts silicon");
            require(logic.getCurrentParallels() == 2, mode + " parallel count limited by coolant");
            require(logic.getOutputItems().length == recipe.mOutputs.length, mode + " preserves all outputs");
            for (int i = 0; i < recipe.mOutputs.length; i++) {
                require(
                    logic.getOutputItems()[i].isItemEqual(recipe.mOutputs[i])
                        && logic.getOutputItems()[i].stackSize == recipe.mOutputs[i].stackSize * 2,
                    mode + " produces exact output " + i);
            }
            require(
                material.stackSize == recipe.mInputs[0].stackSize * 3
                    && fluid.amount == recipe.mFluidInputs[0].amount * 2,
                mode + " keeps inputs until commit");
            require(
                logic.commitInputs() && material.stackSize == recipe.mInputs[0].stackSize && fluid.amount == 0,
                mode + " consumes exact material and coolant amounts");
            require(factory.getMachineMode() == (usePattern ? 18 : 3), mode + " preserves selected mode");
            if (usePattern) {
                require(
                    factory.getSingleRecipeCheck()
                        .getRecipeMap() == RecipeMaps.cutterRecipes,
                    mode + " locks the executable cutter map");
                require(
                    SingleRecipeCheck.tryLoad(
                        RecipeMaps.cutterRecipes,
                        factory.getSingleRecipeCheck()
                            .writeToNBT())
                        != null,
                    mode + " recipe lock survives NBT");
            }
            logic.prepareRecipe(16384);
            if (usePattern) logic.tryCachePossibleRecipesFromPattern(
                hatch.inventories()
                    .next());
            fluid.amount = recipe.mFluidInputs[0].amount - 1;
            logic.setInputItems(material)
                .setInputFluids(fluid);
            require(
                !logic.process()
                    .wasSuccessful(),
                mode + " rejects insufficient coolant");
            require(
                material.stackSize == recipe.mInputs[0].stackSize && fluid.amount == recipe.mFluidInputs[0].amount - 1,
                mode + " failed start preserves inputs");
            logic.finishRecipe();
            if (usePattern) {
                logic.prepareRecipe(16384);
                require(
                    logic.tryCachePossibleRecipesFromPattern(
                        hatch.inventories()
                            .next()),
                    mode + " accepts locked pattern again");
                fluid.amount = recipe.mFluidInputs[0].amount;
                logic.setInputItems(material)
                    .setInputFluids(fluid);
                require(
                    logic.process()
                        .wasSuccessful() && logic.commitInputs()
                        && material.stackSize == 0
                        && fluid.amount == 0,
                    mode + " executes locked recipe with exact consumption");
            }
            factory.setSingleRecipeCheck(null);
        }
    }

    private static ItemStack pattern(GTRecipe recipe, String map) {
        ItemStack encoded = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList in = new NBTTagList(), out = new NBTTagList();
        for (ItemStack input : recipe.mInputs) in.appendTag(
            AEItemStack.create(input)
                .toNBTGeneric());
        for (FluidStack input : recipe.mFluidInputs) in.appendTag(
            AEFluidStack.create(input)
                .toNBTGeneric());
        for (ItemStack output : recipe.mOutputs) out.appendTag(
            AEItemStack.create(output)
                .toNBTGeneric());
        for (FluidStack output : recipe.mFluidOutputs) out.appendTag(
            AEFluidStack.create(output)
                .toNBTGeneric());
        tag.setTag("in", in);
        tag.setTag("out", out);
        tag.setString(PatternRecipeMap.KEY, map);
        encoded.setTagCompound(tag);
        return encoded;
    }

    private static BaseMetaTileEntity place(World world, int x, int id) {
        world.setBlock(x, 80, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 80, 0);
        tile.setInitialValuesAsNBT(null, (short) id);
        return tile;
    }

    private void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        report.append("PASS ")
            .append(message)
            .append('\n');
        System.out.println("[factory-recipe-map-qa] PASS " + message);
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
        }, "factory-recipe-map-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }

}
