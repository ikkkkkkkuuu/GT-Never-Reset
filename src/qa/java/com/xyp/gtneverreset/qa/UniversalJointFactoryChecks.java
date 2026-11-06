package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
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
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.machine.multiblock.UniversalJointFactory;
import com.xyp.gtneverreset.common.machine.multiblock.UniversalJointFactoryProcessingLogic;
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
import gregtech.api.metatileentity.BaseMetaTileEntity;
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
            require(detached.getParameterModuleLimit(type) == 0, "no module " + type);
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
            factory.getMaxParallelRecipes() == 64 && factory.getTotalRecipeThreads() == 1,
            "ordinary LV mode is 64 parallel / one thread");
        BaseMetaTileEntity patternTile = place(world, 3, 80, 0, GTNRMachineID.SuperCraftingInputME.id);
        var hatch = (SuperMTEHatchCraftingInputME) patternTile.getMetaTileEntity();
        require(factory.checkStructure(true, tile), "factory reforms with pattern hatch");
        require(factory.getTotalRecipeThreads() == 32, "pattern hatch enables 32 LV threads");
        verifyMatching(world, hatch);
        verifyThreads(world, hatch);
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(0.5, 80, -4, 0, 0);
    }

    private void verifyMatching(World world, SuperMTEHatchCraftingInputME hatch) {
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
        QaSlot slot = new QaSlot(pattern(input, new ItemStack(Items.emerald)), hatch, world, material);
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
        ItemStack ingredient = new ItemStack(Items.ghast_tear);
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
        java.lang.reflect.Field field = SuperMTEHatchCraftingInputME.class.getDeclaredField("internalInventory");
        field.setAccessible(true);
        var slots = (SuperMTEHatchCraftingInputME.PatternSlot<?>[]) field.get(hatch);
        ItemStack first = ingredient.copy(), second = ingredient.copy();
        first.stackSize = 64;
        second.stackSize = 64;
        slots[0] = new QaSlot(pattern(ingredient, new ItemStack(Items.stick)), hatch, world, first);
        slots[1] = new QaSlot(pattern(ingredient, new ItemStack(Items.gold_nugget)), hatch, world, second);
        NBTTagCompound before = new NBTTagCompound();
        factory.saveNBTData(before);
        factory.setBatchMode(false);
        for (int i = 0; i < 32; i++) require(
            factory.checkProcessing()
                .wasSuccessful(),
            "start live thread " + (i + 1));
        require(
            factory.getRecipeThreadTasks()
                .size() == 32,
            "32 independent jobs remain active together");
        int remaining = first.stackSize + second.stackSize;
        require(
            !factory.checkProcessing()
                .wasSuccessful() && remaining == first.stackSize + second.stackSize,
            "33rd job rejected without consuming inputs");
        List<ItemStack> products = new ArrayList<>();
        factory.getRecipeThreadTasks()
            .forEach(
                task -> task.getOutputs()
                    .appendDisplayItems(products));
        require(
            products.stream()
                .anyMatch(stack -> stack.getItem() == Items.stick)
                && products.stream()
                    .anyMatch(stack -> stack.getItem() == Items.gold_nugget),
            "different recipe maps run concurrently");
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
            "all 32 jobs survive NBT reload");
        factory.loadNBTData(before);
        slots[0] = null;
        slots[1] = null;
    }

    private static ItemStack pattern(ItemStack input, ItemStack output) {
        ItemStack encoded = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList in = new NBTTagList(), out = new NBTTagList();
        in.appendTag(
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
