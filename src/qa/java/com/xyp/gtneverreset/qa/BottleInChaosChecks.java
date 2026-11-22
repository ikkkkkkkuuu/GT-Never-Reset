package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.xyp.gtneverreset.common.crafting.chaos.*;
import com.xyp.gtneverreset.common.crafting.chaos.processing.BottledChaosPowerAllocator;
import com.xyp.gtneverreset.common.gui.modularui.multiblock.BottleInChaosGui;
import com.xyp.gtneverreset.common.item.items.*;
import com.xyp.gtneverreset.common.machine.hatch.ChaosCredentialHatch;
import com.xyp.gtneverreset.common.machine.multiblock.BottleInChaos;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;
import com.xyp.gtneverreset.utils.structure.MultiblockStructureLoader;

import appeng.api.AEApi;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.*;
import gregtech.api.recipe.RecipeMap;
import io.netty.buffer.Unpooled;

@Mod(modid = "bottleinchaosqa", name = "Bottle in Chaos QA", version = "1", dependencies = "after:gtneverreset")
public final class BottleInChaosChecks {

    private static final boolean PREVIEW = Boolean.getBoolean("gtnr.bottle.chaos.preview");
    private static final boolean EXPECT_DISABLED = Boolean.getBoolean("gtnr.bottle.chaos.expectDisabled");

    private final StringBuilder report = new StringBuilder();
    private boolean launched, checked, opened, finished;
    private volatile boolean ready;
    private volatile Throwable failure;
    private int ticks, frames, stage, capturedPage;
    private BaseMetaTileEntity tile;
    private BottleInChaos machine;
    private BaseMetaTileEntity inputBus, outputBus, energyHatch;
    private boolean processingVerified;
    private boolean waitingForRoundOutputs;
    private PanelSyncManager[] viewers;
    private final long[] viewerBytes = new long[2];
    private int viewerTicks;
    private int processingTicks;
    private boolean secondGroupStarted, outputBlockVerified, legacyJobReloaded, competingGroups;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    private int menuTicks;

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
            if (ready && !processingVerified) verifyLiveProcessing();
            if (ready && processingVerified && ++ticks > 60 && !opened) {
                require(machine.onRightclick(tile, player), "controller GUI opens");
                opened = true;
            }
            if (stage == 1) {
                player.closeScreen();
                player.playerNetServerHandler.setPlayerLocation(42, 84, -48, 40, 14);
                stage = 2;
            }
            if (stage == 3 && tile.isAllowedToWork()) {
                require(true, "GUI button synchronizes power toggle to server");
                tile.disableWorking();
                stage = 1;
            }
            if (stage == 4) {
                player.playerNetServerHandler.setPlayerLocation(0.5, 80, -4, 0, 0);
                var hatchTile = (BaseMetaTileEntity) player.worldObj.getTileEntity(-3, 80, -3);
                require(
                    ((ChaosCredentialHatch) hatchTile.getMetaTileEntity()).onRightclick(hatchTile, player),
                    "credential hatch GUI opens");
                stage = 5;
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
            if (!launched && mc.theWorld == null && mc.currentScreen != null && ++menuTicks > 100) {
                launched = true;
                if (EXPECT_DISABLED) {
                    verifyDisabledRegistration();
                    capture("chaos-disabled.png");
                    finish(null);
                    return;
                }
                mc.launchIntegratedServer(
                    "chaos-" + System.currentTimeMillis(),
                    "Chaos QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("server verification", failure);
            if (!PREVIEW && ticks > 1800) throw new AssertionError("GUI timeout");
        } catch (Throwable error) {
            finish(error);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ready || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (PREVIEW) {
                if (opened && mc.currentScreen instanceof GuiContainerWrapper && frames++ == 60) {
                    capture("chaos-preview.png");
                    Files.writeString(Path.of("preview-ready.txt"), "Controller GUI ready for interactive inspection");
                }
                return;
            }
            if (stage == 0 && mc.currentScreen instanceof GuiContainerWrapper gui && ++frames > 60) {
                var panel = gui.getScreen()
                    .getSyncManager()
                    .getMainPSM();
                require(
                    pageData(panel, "bottledChaosProcessingGroups").hasKey("bottledChaosThreads") == (capturedPage == 0)
                        && pageData(panel, "bottledChaosHatchSummary").hasKey("hatches") == (capturedPage == 1)
                        && pageData(panel, "bottledChaosPoolUsers").hasKey("users") == (capturedPage == 2),
                    "client receives the synchronized data for terminal page " + capturedPage);
                capture(capturedPage == 0 ? "chaos-gui.png" : "chaos-tab-" + capturedPage + ".png");
                if (capturedPage < 3) {
                    var tab = find(
                        gui.getScreen()
                            .getMainPanel(),
                        "chaos_tab_" + (++capturedPage));
                    require(
                        tab instanceof com.cleanroommc.modularui.widgets.ButtonWidget,
                        "original terminal tab exists");
                    ((com.cleanroommc.modularui.widgets.ButtonWidget<?>) tab).onMousePressed(0);
                    frames = 0;
                    return;
                }
                var tooltip = GTNRItemList.BottleInChaos.get(1)
                    .getTooltip(mc.thePlayer, false);
                Files.write(Path.of("tooltip.txt"), tooltip);
                require(
                    !String.join("\n", tooltip)
                        .contains("{var:"),
                    "tooltip variables resolved");
                stage = 1;
                frames = 0;
            } else if (stage == 2 && ++frames > 80) {
                capture("chaos-structure.png");
                stage = 4;
                frames = 0;
            } else if (stage == 5 && mc.currentScreen instanceof GuiContainerWrapper gui && ++frames > 60) {
                capture("chaos-hatch.png");
                var button = find(
                    gui.getScreen()
                        .getMainPanel(),
                    "chaos_patterns_button");
                require(button instanceof com.cleanroommc.modularui.widgets.ButtonWidget, "AE editor button exists");
                ((com.cleanroommc.modularui.widgets.ButtonWidget<?>) button).onMousePressed(0);
                stage = 6;
                frames = 0;
            } else if (stage == 6 && mc.currentScreen instanceof GuiContainerWrapper gui && ++frames > 60) {
                require(
                    gui.getScreen()
                        .isPanelOpen("chaosPatterns"),
                    "AE editor opens through synchronized UI action");
                capture("chaos-ae-patterns.png");
                gui.getScreen()
                    .getPanelManager()
                    .closeTopPanel();
                gui.getScreen()
                    .getSyncManager()
                    .getMainPSM()
                    .findPanelHandlerNullable("bottledChaosHatchRoutes")
                    .openPanel();
                stage = 7;
                frames = 0;
            } else if (stage == 7 && mc.currentScreen instanceof GuiContainerWrapper gui && ++frames > 60) {
                require(
                    gui.getScreen()
                        .isPanelOpen("chaosRoutes"),
                    "unlocked preset selector opens without a physical credential");
                capture("chaos-presets.png");
                finish(null);
            }
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void verifyDisabledRegistration() {
        require(
            !com.xyp.gtneverreset.config.ChaosConfig.enableBottleInChaos,
            "feature defaults to disabled without a config value");
        require(!GTNRItemList.BottleInChaos.hasBeenSet(), "disabled controller is not registered");
        require(!GTNRItemList.ChaosCredentialHatch.hasBeenSet(), "disabled processing hatch is not registered");
        require(!GTNRItemList.ChaosStructureModule.hasBeenSet(), "disabled structure module is not registered");
        require(!GTNRItemList.ChaosCapacityTier1.hasBeenSet(), "disabled capacity block is not registered");
        require(com.xyp.gtneverreset.loader.ItemLoader.chaosRoute == null, "disabled credential item is absent");
        require(
            com.xyp.gtneverreset.loader.BlockLoader.chaosCapacity == null,
            "disabled capacity block instance is absent");
        require(
            GregTechAPI.METATILEENTITIES[GTNRMachineID.BottleInChaos.id] == null,
            "disabled controller has no meta tile registration");
        require(
            GregTechAPI.METATILEENTITIES[GTNRMachineID.ChaosCredentialHatch.id] == null,
            "disabled processing hatch has no meta tile registration");
        require(
            RecipeMap.ALL_RECIPE_MAPS.keySet()
                .stream()
                .noneMatch(
                    key -> key.startsWith("gtnr.bottleinchaos.") || key.startsWith("gtnr.chaos.")
                        || key.startsWith("gtnr.recipe.chaos_")),
            "disabled dedicated recipe maps are absent");
        require(
            GTNRItemList.LargeFurnace.hasBeenSet() && GTNRItemList.MEDataAccessHatch.hasBeenSet(),
            "unrelated machines remain registered");
        require(
            gregtech.api.recipe.RecipeMaps.assemblerRecipes.getAllRecipes()
                .stream()
                .anyMatch(
                    recipe -> java.util.Arrays.stream(recipe.mOutputs)
                        .anyMatch(
                            output -> output != null && output.isItemEqual(GTNRItemList.MEDataAccessHatch.get(1)))),
            "unrelated assembler recipes remain registered");
    }

    private void verify(EntityPlayerMP player) throws Exception {
        World world = player.worldObj;
        require(
            com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeProcessor.isReady(),
            "full preset catalog initialized without TST");
        String[][] shape = MultiblockStructureLoader.load("bottle_in_chaos");
        require(
            shape.length == 31 && shape[0].length == 29 && shape[0][0].length() == 29,
            "shortened and inverted 29 x 31 x 29 structure loaded");
        tile = place(world, 0, 80, 0, GTNRMachineID.BottleInChaos.id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.disableWorking();
        machine = (BottleInChaos) tile.getMetaTileEntity();
        machine.construct(new ItemStack(Items.feather, 64), false);
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        if (PREVIEW) {
            verifyFormation(world, shape);
            processingVerified = true;
            player.playerNetServerHandler.setPlayerLocation(0.5, 80, -4, 0, 0);
            return;
        }
        var hatchTile = place(world, -3, 80, -3, GTNRMachineID.ChaosCredentialHatch.id);
        var hatch = (ChaosCredentialHatch) hatchTile.getMetaTileEntity();
        require(hatch.getStackInSlot(0) == null, "processing group requires no physical credential");
        require(ChaosRouteItem.isBound(hatch.getAuthorizationStack()), "empty processing hatch has an unlocked route");
        hatch.setSelectedRoutes(2);
        NBTTagCompound saved = new NBTTagCompound();
        hatch.saveNBTData(saved);
        hatch.setSelectedRoutes(1);
        hatch.loadNBTData(saved);
        require(hatch.getSelectedRoutes() == 2, "selected routes survive NBT round trip");
        verifyPoolMasks();
        verifyPowerRounds();
        verifyChemicalRoutes();
        verifyCustomRoutes(hatch, world);
        verifyModuleMaterials();
        verifyManufacturing();
        verifyPackagingTransactions();
        verifyNativeProfiles();
        verifyFormation(world, shape);
        prepareLiveProcessing();
        player.playerNetServerHandler.setPlayerLocation(0.5, 80, -4, 0, 0);
    }

    private void verifyFormation(World world, String[][] shape) {
        var blocks = GregTechAPI.sBlockCasings4;
        var central = new java.util.ArrayList<int[]>();
        var capacityBlocks = new java.util.ArrayList<int[]>();
        for (int layer = 0; layer < shape.length; layer++) {
            for (int row = 0; row < shape[layer].length; row++) {
                for (int column = 0; column < shape[layer][row].length(); column++) {
                    char symbol = shape[layer][row].charAt(column);
                    int x = column - 14, y = 85 - layer, z = row - 12;
                    if (symbol == 'G') capacityBlocks.add(new int[] { x, y, z });
                    if (symbol == 'U') place(world, x, y, z, GTNRMachineID.ChaosCredentialHatch.id);
                    if (symbol == 'V' || symbol == 'W' || symbol == 'X' || symbol == 'Y')
                        world.setBlock(x, y, z, blocks, 0, 3);
                    if (symbol == 'Y') central.add(new int[] { x, y, z });
                }
            }
        }
        place(world, -2, 80, 0, GTNRMachineID.StaticParallelControllerT7.id);
        var hatches = new gregtech.api.enums.ItemList[] { gregtech.api.enums.ItemList.Hatch_Input_Bus_LV,
            gregtech.api.enums.ItemList.Hatch_Output_Bus_LV, gregtech.api.enums.ItemList.Hatch_Input_LV,
            gregtech.api.enums.ItemList.Hatch_Output_LV, gregtech.api.enums.ItemList.Hatch_Energy_LV };
        for (int i = 0; i < hatches.length; i++) {
            int[] position = central.get(i);
            BaseMetaTileEntity placed = place(
                world,
                position[0],
                position[1],
                position[2],
                hatches[i].get(1)
                    .getItemDamage());
            if (i == 0) inputBus = placed;
            if (i == 1) outputBus = placed;
            if (i == 4) energyHatch = placed;
        }
        var errors = new java.util.ArrayList<gregtech.api.structure.error.StructureError>();
        if (!PREVIEW) {
            for (int[] pos : capacityBlocks) world.setBlockToAir(pos[0], pos[1], pos[2]);
            machine.checkMachine(tile, null, errors);
            require(
                errors.isEmpty() && machine.getBottledChaosCapacity() == 4,
                "empty capacity regions form at minimum capacity");
            int[] first = capacityBlocks.get(0);
            world.setBlock(first[0], first[1], first[2], net.minecraft.init.Blocks.stone);
            machine.checkMachine(tile, null, errors);
            require(!errors.isEmpty(), "a foreign block in an otherwise empty capacity region prevents formation");
            for (int[] pos : capacityBlocks)
                world.setBlock(pos[0], pos[1], pos[2], com.xyp.gtneverreset.loader.BlockLoader.chaosCapacity, 4, 3);
            errors.clear();
        }
        machine.checkMachine(tile, null, errors);
        require(errors.isEmpty(), "shortened inverted structure forms with fifteen processing groups: " + errors);
        machine.mMachine = true;
        require(
            machine.getInstalledCredentialHatchCount() == 15,
            "all fifteen independent processing groups are connected");
        require(machine.getBottledChaosCapacity() == 4096, "four top-tier capacity regions provide 4096 capacity");
    }

    private void prepareLiveProcessing() {
        for (int group = 0; group < 15; group++) machine.getManagedCredentialHatch(group)
            .setExecutionEnabled(group == 0);
        var hatch = machine.getManagedCredentialHatch(0);
        hatch.setInventorySlotContents(
            1,
            pattern(
                new ItemStack(Items.apple),
                new ItemStack(Items.blaze_powder),
                gregtech.api.recipe.RecipeMaps.wiremillRecipes));
        hatch.onContentsChanged(1);
        var rule = com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructureCatalog.displayRules()
            .stream()
            .filter(
                r -> r.controller()
                    .getItemDamage()
                    == gregtech.api.enums.ItemList.Machine_LV_Wiremill.get(1)
                        .getItemDamage())
            .findFirst()
            .orElseThrow();
        machine.getMachineConfiguration()
            .machines()
            .setStackInSlot(0, ChaosStructureModuleItem.create(rule.example(), 1));
        var second = machine.getManagedCredentialHatch(1);
        gregtech.api.enums.GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(Items.melon))
            .itemOutputs(new ItemStack(Items.ghast_tear))
            .duration(40)
            .eut(8)
            .addTo(gregtech.api.recipe.RecipeMaps.wiremillRecipes);
        second.setInventorySlotContents(
            1,
            pattern(
                new ItemStack(Items.melon),
                new ItemStack(Items.ghast_tear),
                gregtech.api.recipe.RecipeMaps.wiremillRecipes));
        second.onContentsChanged(1);
        inputBus.setInventorySlotContents(0, new ItemStack(Items.apple, 2));
        inputBus.setInventorySlotContents(1, new ItemStack(Items.melon, 3));
        for (int slot = 0; slot < outputBus.getSizeInventory(); slot++)
            outputBus.setInventorySlotContents(slot, new ItemStack(net.minecraft.init.Blocks.cobblestone, 64));
        energyHatch.increaseStoredEnergyUnits(64000, true);
        tile.enableWorking();
    }

    private void verifyLiveProcessing() throws Exception {
        energyHatch.increaseStoredEnergyUnits(64000, true);
        if (waitingForRoundOutputs) {
            if (viewerTicks < 24) sampleViewers();
            if (viewerTicks < 24) return;
            if (countItems(outputBus, Items.blaze_powder) != 64) {
                if (++processingTicks > 600) throw new AssertionError(
                    "near-full round delivery timeout: outputs=" + countItems(outputBus, Items.blaze_powder)
                        + ", state="
                        + machine.getProcessingGroupSummary());
                return;
            }
            require(
                countItems(inputBus, Items.dye) == 1,
                "fourteen jobs deliver into fourteen free spaces and leave the fifteenth input");
            tile.disableWorking();
            processingVerified = true;
            return;
        }
        if (competingGroups) {
            if (++processingTicks < 120) return;
            int products = countItems(outputBus, Items.blaze_powder) + countItems(outputBus, Items.magma_cream);
            require(
                countItems(inputBus, Items.apple) == 0 && products == 1,
                "two concentrated groups competing for one shared input deliver exactly one product");
            benchmarkSharedRound();
            competingGroups = false;
            waitingForRoundOutputs = true;
            processingTicks = 0;
            return;
        }
        if (!outputBlockVerified) {
            if (++processingTicks < 80) return;
            require(countItems(inputBus, Items.apple) == 2, "blocked output reserves no input and loses no product");
            for (int slot = 0; slot < outputBus.getSizeInventory(); slot++)
                outputBus.setInventorySlotContents(slot, null);
            outputBlockVerified = true;
            processingTicks = 0;
        }
        if (!legacyJobReloaded && countItems(inputBus, Items.apple) < 2
            && countItems(outputBus, Items.blaze_powder) == 0) {
            NBTTagCompound save = new NBTTagCompound();
            machine.saveNBTData(save);
            NBTTagCompound job = save.getTagList("bottledChaosThreads", 10)
                .getCompoundTagAt(0);
            ItemStack paid = gregtech.api.util.GTUtility.loadItem(job, "credential");
            require(ChaosRouteItem.hasPatternAuthorization(paid), "paid AE job uses an independent pattern identity");
            NBTTagList oldTags = new NBTTagList();
            oldTags.appendTag(new net.minecraft.nbt.NBTTagString("titanium"));
            paid.getTagCompound()
                .setTag("bottledChaosTags", oldTags);
            paid.getTagCompound()
                .setInteger("bottledChaosOccupiedWeight", 1);
            gregtech.api.util.GTUtility.saveItem(job, "credential", paid);
            machine.loadNBTData(save);
            var group = machine.getProcessingGroupSummary()
                .getTagList("bottledChaosThreads", 10)
                .getCompoundTagAt(0);
            require(
                group.getCompoundTag("outputs")
                    .getInteger("total") == 1,
                "a restored AE job displays its own result independent of the preset catalog");
            legacyJobReloaded = true;
        }
        if (secondGroupStarted) {
            int total = 0;
            for (int slot = 0; slot < outputBus.getSizeInventory(); slot++) {
                ItemStack item = outputBus.getStackInSlot(slot);
                if (item != null && item.getItem() == Items.ghast_tear) total += item.stackSize;
            }
            if (total == 3) {
                require(
                    countItems(inputBus, Items.melon) == 0,
                    "resumed second group consumes its independent input exactly once");
                require(total == 3, "second processing group produces its own exact output after resume");
                var map = gregtech.api.recipe.RecipeMaps.wiremillRecipes;
                var second = machine.getManagedCredentialHatch(1);
                second.setInventorySlotContents(
                    1,
                    pattern(new ItemStack(Items.apple), new ItemStack(Items.blaze_powder), map));
                second.setInventorySlotContents(
                    2,
                    pattern(new ItemStack(Items.blaze_powder), new ItemStack(Items.magma_cream), map));
                second.onContentsChanged(1);
                require(
                    second.getCustomRoutes() != null && machine.isConcentratedPowerActive(),
                    "both distinct AE routes are eligible for a concentrated power round");
                for (int slot = 0; slot < outputBus.getSizeInventory(); slot++)
                    outputBus.setInventorySlotContents(slot, null);
                inputBus.setInventorySlotContents(0, new ItemStack(Items.apple));
                competingGroups = true;
                processingTicks = 0;
            } else if (++processingTicks > 600) throw new AssertionError("second group resume timeout");
            return;
        }
        int produced = 0;
        for (int slot = 0; slot < outputBus.getSizeInventory(); slot++) {
            ItemStack item = outputBus.getStackInSlot(slot);
            if (item != null && item.getItem() == Items.blaze_powder) produced += item.stackSize;
        }
        if (produced >= 2) {
            require(legacyJobReloaded, "a paid legacy AE job resumes across preset reordering");
            require(produced == 2, "formed machine emits the exact AE output to its real output bus");
            require(
                countItems(inputBus, Items.apple) == 0,
                "formed machine commits the real input bus transaction once");
            require(
                countItems(inputBus, Items.melon) == 3,
                "paused second group retains all of its input while the first group runs");
            machine.setConcentratedPowerEnabled(true);
            machine.getManagedCredentialHatch(1)
                .setExecutionEnabled(true);
            secondGroupStarted = true;
            processingTicks = 0;
        } else if (++processingTicks > 600) {
            Files.writeString(
                Path.of("processing-failure.txt"),
                machine.getProcessingGroupSummary()
                    .toString());
            throw new AssertionError("actual processing timeout: " + machine.getProcessingGroupSummary());
        }
    }

    private void benchmarkSharedRound() throws Exception {
        World world = tile.getWorld();
        world.setBlockToAir(inputBus.xCoord, inputBus.yCoord, inputBus.zCoord);
        inputBus = place(
            world,
            inputBus.xCoord,
            inputBus.yCoord,
            inputBus.zCoord,
            gregtech.api.enums.ItemList.Hatch_Input_Bus_IV.get(1)
                .getItemDamage());
        world.setBlockToAir(energyHatch.xCoord, energyHatch.yCoord, energyHatch.zCoord);
        energyHatch = place(
            world,
            energyHatch.xCoord,
            energyHatch.yCoord,
            energyHatch.zCoord,
            gregtech.api.enums.ItemList.Hatch_Energy_EV.get(1)
                .getItemDamage());
        machine.checkStructure(true, tile);
        require(machine.mMachine, "stress fixture forms with a shared sixteen-slot input bus");
        require(
            machine.getOutputBusses()
                .size() == 1 && inputBus.getSizeInventory() >= 15,
            "stress fixture has one physical output bus and room for fifteen distinct inputs");
        var map = gregtech.api.recipe.RecipeMaps.wiremillRecipes;
        for (int slot = 0; slot < 15; slot++) {
            ItemStack dye = new ItemStack(Items.dye, 1, slot);
            ItemStack product = new ItemStack(Items.blaze_powder);
            gregtech.api.enums.GTValues.RA.stdBuilder()
                .itemInputs(dye.copy())
                .itemOutputs(product.copy())
                .duration(40)
                .eut(8)
                .addTo(map);
            var hatch = machine.getManagedCredentialHatch(slot);
            hatch.setInventorySlotContents(2, null);
            hatch.setInventorySlotContents(1, pattern(dye, product, map));
            hatch.onContentsChanged(1);
            hatch.setExecutionEnabled(true);
            if (hatch.getCustomRoutes() == null) throw new AssertionError("stress pattern " + slot);
        }
        for (int slot = 0; slot < outputBus.getSizeInventory(); slot++)
            outputBus.setInventorySlotContents(slot, new ItemStack(net.minecraft.init.Blocks.cobblestone, 64));
        outputBus.setInventorySlotContents(0, new ItemStack(Items.blaze_powder, 50));
        NBTTagCompound before = new NBTTagCompound();
        machine.saveNBTData(before);
        var allocations = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        long[] nanos = new long[9], bytes = new long[9];
        for (int sample = -3; sample < nanos.length; sample++) {
            machine.loadNBTData(before);
            for (int slot = 0; slot < 15; slot++)
                inputBus.setInventorySlotContents(slot, new ItemStack(Items.dye, 1, slot));
            if (countItems(inputBus, Items.dye) != 15) throw new AssertionError("stress fixture input setup");
            long allocation = allocations.getCurrentThreadAllocatedBytes();
            long start = System.nanoTime();
            var result = machine.checkProcessingMM();
            long elapsed = System.nanoTime() - start;
            if (sample >= 0) {
                nanos[sample] = elapsed;
                bytes[sample] = allocations.getCurrentThreadAllocatedBytes() - allocation;
            }
            int left = countItems(inputBus, Items.dye);
            if (!result.wasSuccessful() || left != 1) throw new AssertionError(
                "stress round: " + result.getID()
                    + ", remaining "
                    + left
                    + ", input bus="
                    + inputBus.getSizeInventory()
                    + ", jobs="
                    + machine.getProcessingGroupSummary());
        }
        energyHatch.increaseStoredEnergyUnits(64000, true);
        tile.enableWorking();
        EntityPlayerMP player = (EntityPlayerMP) world.playerEntities.get(0);
        viewers = new PanelSyncManager[] { openTestViewer(player),
            openTestViewer(FakePlayerFactory.getMinecraft((net.minecraft.world.WorldServer) world)) };
        require(
            countItems(inputBus, Items.dye) == 1,
            "fifteen competing groups reserve exactly fourteen outputs in every measured round");
        Files.writeString(
            Path.of("shared-round-benchmark.txt"),
            "nanos=" + java.util.Arrays.toString(nanos) + "\nbytes=" + java.util.Arrays.toString(bytes));
    }

    private PanelSyncManager openTestViewer(EntityPlayerMP player) {
        var sync = new ModularSyncManager(false);
        var panelSync = new PanelSyncManager(sync, true);
        var settings = new UISettings(RecipeViewerSettings.DUMMY);
        var data = new PosGuiData(player, tile.xCoord, tile.yCoord, tile.zCoord);
        var panel = new BottleInChaosGui<>(machine).build(data, panelSync, settings);
        WidgetTree.collectSyncValues(panelSync, panel);
        settings.createContainer()
            .construct(player, sync, settings, panel.getName(), data);
        sync.onOpen();
        return panelSync;
    }

    /** Serialize the changed page values of two server-side sessions, excluding packet headers. */
    private void sampleViewers() throws Exception {
        int secondPage = viewerTicks < 8 ? 2 : viewerTicks < 16 ? 1 : 0;
        viewers[1].findSyncHandler("bottledChaosTerminalPage", IntSyncValue.class)
            .setIntValue(secondPage, true, false);
        for (int i = 0; i < viewers.length; i++) {
            var panel = viewers[i];
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            try {
                for (String key : new String[] { "bottledChaosStatusSummary", "bottledChaosProcessingGroups",
                    "bottledChaosGroupOutputs", "bottledChaosHatchSummary", "bottledChaosPoolUsers",
                    "bottledChaosWireless", "bottledChaosRequiredPools", "bottledChaosMissingPools",
                    "bottledChaosModuleParameters" }) {
                    writeChanged(panel, key, buffer, viewerTicks == 0);
                }
                viewerBytes[i] += buffer.readableBytes();
            } finally {
                buffer.release();
            }
            int page = i == 0 ? 0 : secondPage;
            if (pageData(panel, "bottledChaosHatchSummary").hasKey("hatches") != (page == 1)
                || pageData(panel, "bottledChaosProcessingGroups").hasKey("bottledChaosThreads") != (page == 0))
                throw new AssertionError("viewer " + i + " received the wrong data for page " + page);
        }
        if (++viewerTicks == 24) {
            for (var panel : viewers) {
                panel.getModularSyncManager()
                    .onClose();
                panel.getModularSyncManager()
                    .dispose();
            }
            Files.writeString(
                Path.of("page-payload-bytes.txt"),
                "runtime=" + viewerBytes[0] + "\nswitching=" + viewerBytes[1]);
            require(
                viewerBytes[0] > 0 && viewerBytes[1] > 0,
                "two independent server GUI sessions serialize their page data over 24 ticks");
        }
    }

    private static NBTTagCompound pageData(PanelSyncManager panel, String name) {
        return (NBTTagCompound) panel.findSyncHandler(name, ValueSyncHandler.class)
            .getValue();
    }

    private static void writeChanged(PanelSyncManager panel, String key, PacketBuffer buffer, boolean initial)
        throws Exception {
        ValueSyncHandler<?, ?> value = panel.findSyncHandler(key, ValueSyncHandler.class);
        if (value.updateCacheFromSource(initial)) value.write(buffer);
    }

    private static int countItems(BaseMetaTileEntity inventory, net.minecraft.item.Item type) {
        int count = 0;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack item = inventory.getStackInSlot(slot);
            if (item != null && item.getItem() == type) count += item.stackSize;
        }
        return count;
    }

    private void verifyCustomRoutes(ChaosCredentialHatch hatch, World world) {
        var map = gregtech.api.recipe.RecipeMaps.wiremillRecipes;
        ItemStack input = new ItemStack(Items.apple), intermediate = new ItemStack(Items.blaze_powder),
            output = new ItemStack(Items.magma_cream);
        gregtech.api.enums.GTValues.RA.stdBuilder()
            .itemInputs(input)
            .itemOutputs(intermediate)
            .duration(20)
            .eut(8)
            .addTo(map);
        gregtech.api.enums.GTValues.RA.stdBuilder()
            .itemInputs(intermediate)
            .itemOutputs(output)
            .duration(40)
            .eut(8)
            .addTo(map);
        hatch.setInventorySlotContents(1, pattern(input, intermediate, map));
        hatch.setInventorySlotContents(2, pattern(intermediate, output, map));
        hatch.onContentsChanged(1);
        com.xyp.gtneverreset.common.me.ChaosPatternCompiler
            .compile(java.util.List.of(hatch.getStackInSlot(1), hatch.getStackInSlot(2)), world);
        var custom = hatch.getCustomRoutes();
        require(custom != null, "AE patterns compile against registered recipes");
        var items = new com.xyp.gtneverreset.common.machine.multiMachineBase.ChaosLongInputMachineBase.ItemStackLong[] {
            new com.xyp.gtneverreset.common.machine.multiMachineBase.ChaosLongInputMachineBase.ItemStackLong(
                input.copy(),
                1) };
        var fluids = new com.xyp.gtneverreset.common.machine.multiMachineBase.ChaosLongInputMachineBase.FluidStackLong[0];
        var planner = com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeProcessor.executionPlanner(
            custom,
            1,
            items,
            fluids,
            com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosSourceParameters.DEFAULT,
            hatch.getMaterialCache(),
            new com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeProcessor.PlanningCache(),
            com.xyp.gtneverreset.common.crafting.chaos.processing.BottledChaosDiagnostics.begin());
        var plan = planner.prepareEntry(0);
        require(plan != null, "AE chain plans from external input without supplying its intermediate");
        plan = planner.completePlan(plan);
        plan.prepareOutputs();
        require(
            plan.outputPlan()
                .itemOutputs()
                .size() == 1 && plan.outputPlan()
                    .itemOutputs()
                    .get(0)
                    .itemStack()
                    .isItemEqual(output)
                && plan.outputPlan()
                    .itemOutputs()
                    .get(0)
                    .stackSize() == 1,
            "AE chain conserves materials and emits only the final output");
        require(plan.durationTicks() >= 60, "AE chain charges both source steps");
        require(items[0].stackSize() == 1, "planning does not consume input before commit");
        plan.consumeInputCopies(items, fluids);
        require(items[0].stackSize() == 0, "commit consumes the external input once");
        gregtech.api.enums.GTValues.RA.stdBuilder()
            .itemInputs(output)
            .itemOutputs(new ItemStack(Items.apple, 2))
            .duration(20)
            .eut(8)
            .addTo(map);
        hatch.setInventorySlotContents(3, pattern(output, new ItemStack(Items.apple, 2), map));
        hatch.onContentsChanged(3);
        var cycle = hatch.getCustomRoutes();
        require(cycle != null, "AE cyclic sequence compiles with its starting seed retained");
        var noSeed = com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeProcessor.executionPlanner(
            cycle,
            1,
            new com.xyp.gtneverreset.common.machine.multiMachineBase.ChaosLongInputMachineBase.ItemStackLong[0],
            fluids,
            com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosSourceParameters.DEFAULT,
            hatch.getMaterialCache(),
            new com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeProcessor.PlanningCache(),
            com.xyp.gtneverreset.common.crafting.chaos.processing.BottledChaosDiagnostics.begin());
        require(noSeed.prepareEntry(0) == null, "AE cycle cannot start without the physical seed");
        hatch.setInventorySlotContents(3, null);
        hatch.onContentsChanged(3);
        NBTTagCompound saved = new NBTTagCompound();
        var savedTile = (BaseMetaTileEntity) hatch.getBaseMetaTileEntity();
        savedTile.writeToNBT(saved);
        hatch.setInventorySlotContents(1, pattern(input, new ItemStack(Items.diamond), map));
        hatch.onContentsChanged(1);
        require(hatch.getCustomRoutes() == null, "forged AE outputs are rejected");
        savedTile.readFromNBT(saved);
        hatch = (ChaosCredentialHatch) savedTile.getMetaTileEntity();
        require(hatch.getCustomRoutes() != null, "AE pattern configuration survives NBT reload");
        hatch.setInventorySlotContents(1, null);
        hatch.setInventorySlotContents(2, null);
        hatch.onContentsChanged(1);
        require(
            !hatch.hasCustomPatterns() && hatch.getCustomRoutes() == null,
            "removing patterns restores preset mode");
    }

    private void verifyPowerRounds() {
        var power = new BottledChaosPowerAllocator(2);
        int[] weights = { 2, 1 };
        power.updateSupply(false, 90, weights);
        power.refreshRound(0, 90, weights);
        require(power.budget(0) == 60 && power.budget(1) == 30, "wired groups share EU/t proportionally");
        power.joinRound(0);
        power.requestConcentrated(true);
        power.refreshRound(1, 90, weights);
        require(!power.concentrated() && power.budget(0) == 60, "a mode change preserves the paid round budget");

        NBTTagCompound saved = new NBTTagCompound();
        power.save(saved);
        power = new BottledChaosPowerAllocator(2);
        power.load(saved);
        require(
            power.roundActive() && power.modeChanging() && power.budget(0) == 60,
            "an active round and its pending mode change survive save/load");
        power.retireFinished(0, weights);
        power.refreshRound(0, 90, weights);
        require(power.concentrated() && power.budget(0) == 0, "concentrated power begins at the round boundary");
        power.startRound(2, new long[] { 0, 90 });
        power.requestConcentrated(false);
        power.retireFinished(0, weights);
        power.refreshRound(0, 90, weights);
        require(
            !power.concentrated() && power.budget(1) == 30,
            "ordinary power shares return after a concentrated round");
        power.updateSupply(true, 90, weights);
        require(
            !power.roundActive() && power.budget(0) == 0 && power.budget(1) == 0,
            "wireless mode clears the wired round and both budgets");
    }

    private void verifyPoolMasks() {
        int count = com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosMachineConfiguration.poolCount();
        require(count > 64, "preset and AE recipe pools coexist above the former 64-pool limit");
        var mask = java.math.BigInteger.ZERO.setBit(0)
            .setBit(63)
            .setBit(64)
            .setBit(count - 1);
        String keys = com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosMachineConfiguration
            .poolKeys(mask);
        var loaded = com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosMachineConfiguration
            .maskFromKeys(keys);
        require(
            loaded.and(mask)
                .equals(mask),
            "pool identities above index 63 survive save/load without aliasing");
        var result = new com.xyp.gtneverreset.common.crafting.chaos.result.BottledChaosMissingCategoriesResult(
            mask,
            true);
        var copy = result.newInstance();
        var buffer = new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.buffer());
        try {
            result.encode(buffer);
            copy.decode(buffer);
            require(result.equals(copy), "all missing pool bits survive client synchronization");
        } finally {
            buffer.release();
        }
        copy.readFromNBT(result.writeToNBT(new NBTTagCompound()));
        require(result.equals(copy), "all missing pool bits survive NBT persistence");
    }

    /** The wood and petrochemical presets keep one tag each while publishing every registered input variant. */
    private void verifyChemicalRoutes() {
        int wood = com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CredentialModel
            .indexForTag("wood");
        int petrochemical = com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CredentialModel
            .indexForTag("petrochemical");
        require(wood >= 0 && petrochemical >= 0, "wood and petrochemical route tags are registered");
        require(
            com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CredentialModel
                .getRequiredWeight((1L << wood) | (1L << petrochemical)) > 0,
            "wood and petrochemical routes publish capacity costs");
        var displays = com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeProcessor.displayRecipes();
        var woodRows = displays.stream()
            .filter(display -> display.tagIndex() == wood)
            .toList();
        var petrochemicalRows = displays.stream()
            .filter(display -> display.tagIndex() == petrochemical)
            .toList();
        require(woodRows.size() == 1, "wood route publishes one distillation recipe");
        require(
            woodRows.get(0)
                .plan()
                .itemOutputs()
                .size() == 1
                && woodRows.get(0)
                    .plan()
                    .fluidOutputs()
                    .size() == 16,
            "wood route exposes ash and sixteen distillation fluids");
        require(petrochemicalRows.size() == 11, "petrochemical route publishes eleven oil and tar recipes");

        int pbi = com.xyp.gtneverreset.common.crafting.chaos.route.BottledChaosRecipeResources.CredentialModel
            .indexForTag("pbi");
        var pbiRows = displays.stream()
            .filter(display -> display.tagIndex() == pbi)
            .toList();
        require(pbiRows.size() == 1, "PBI credential publishes one production chain");
        var row = pbiRows.get(0);
        require(row.baseEUt() == gregtech.api.enums.TierEU.RECIPE_IV, "PBI uses IV voltage");
        require(
            row.fluidInputs()
                .stream()
                .anyMatch(
                    input -> input.fluidStack()
                        .isFluidEqual(Materials.Benzene.getFluid(1))),
            "PBI chain consumes benzene");
        require(
            row.plan()
                .fluidOutputs()
                .stream()
                .anyMatch(
                    output -> output.amount() > 0 && output.fluidStack()
                        .isFluidEqual(Materials.Polybenzimidazole.getMolten(1))),
            "PBI chain produces the GregTech molten fluid");
    }

    private void verifyModuleMaterials() {
        var rules = com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructureCatalog.displayRules();
        require(rules.size() > 100, "native GTNH source structure catalog is populated");
        ItemStack furnace = gregtech.api.enums.ItemList.Machine_Multi_BlastFurnace.get(1);
        ItemStack[] furnaceMaterials = { furnace, new ItemStack(GregTechAPI.sBlockCasings5, 16, 0),
            new ItemStack(GregTechAPI.sBlockCasings1, 17, 11) };
        require(
            !com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructureCatalog
                .matches(furnace, furnaceMaterials)
                .isEmpty(),
            "EBF accepts one controller, 16 coils and 17 heat-proof casings");
        furnaceMaterials[2].stackSize = 18;
        require(
            com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructureCatalog
                .matches(furnace, furnaceMaterials)
                .isEmpty(),
            "EBF rejects one excess heat-proof casing");
        furnaceMaterials[2].stackSize = 10;
        require(
            com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructureCatalog
                .matches(furnace, furnaceMaterials)
                .isEmpty(),
            "EBF rejects an incomplete casing bill");
        var rule = rules.stream()
            .filter(
                r -> r.controller()
                    .getItemDamage()
                    == gregtech.api.enums.ItemList.Machine_LV_Macerator.get(1)
                        .getItemDamage())
            .findFirst()
            .orElseThrow();
        var structure = rule.example();
        require(structure != null, "source structure example resolves its real material bill");
        ItemStack module = ChaosStructureModuleItem.create(structure, 2);
        require(
            ChaosStructureModuleItem.quantity(module) == 2 && ChaosStructureModuleItem.structure(module) != null,
            "packaged module retains its structure and multiplicity");
        require(
            com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructureCatalog
                .match(structure.controller(), new ItemStack[0]) == null,
            "missing structure materials cannot authorize a module");
    }

    private void verifyManufacturing() {
        ItemStack[] evOutputs = { GTNRItemList.ChaosStructureModule.get(1), GTNRItemList.ChaosCapacityTier1.get(1),
            GTNRItemList.ChaosCapacityTier2.get(1), GTNRItemList.ChaosCapacityTier3.get(1),
            GTNRItemList.ChaosCapacityTier4.get(1), GTNRItemList.ChaosCapacityTier5.get(1) };
        for (int index = 0; index < evOutputs.length; index++) {
            ItemStack expected = evOutputs[index];
            long expectedVoltage = gregtech.api.enums.GTValues.VP[gregtech.api.enums.VoltageIndex.EV
                + Math.max(0, index - 1)];
            require(
                gregtech.api.recipe.RecipeMaps.assemblerRecipes.getAllRecipes()
                    .stream()
                    .anyMatch(
                        recipe -> recipe.mEUt == expectedVoltage && java.util.Arrays.stream(recipe.mOutputs)
                            .anyMatch(output -> output != null && output.isItemEqual(expected))),
                "downgraded assembler recipe voltage matches: " + expected.getDisplayName());
            require(
                gregtech.api.util.GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.stream()
                    .noneMatch(recipe -> recipe.mOutput != null && recipe.mOutput.isItemEqual(expected)),
                "obsolete assembly-line recipe removed: " + expected.getDisplayName());
        }
        require(
            com.xyp.gtneverreset.common.recipe.GTNRRecipeMaps.BottledChaosRecipes.getAllRecipes()
                .stream()
                .allMatch(recipe -> recipe.mSpecialItems == null),
            "NEI processing recipes no longer display credentials");
        var outputs = new java.util.HashSet<com.xyp.gtneverreset.common.crafting.chaos.material.ChaosItemKey>();
        for (var map : RecipeMap.ALL_RECIPE_MAPS.values())
            for (var recipe : map.getAllRecipes()) for (var output : recipe.mOutputs) if (output != null)
                outputs.add(com.xyp.gtneverreset.common.crafting.chaos.material.ChaosItemKey.create(output));
        for (var recipe : gregtech.api.util.GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes)
            if (recipe.mOutput != null)
                outputs.add(com.xyp.gtneverreset.common.crafting.chaos.material.ChaosItemKey.create(recipe.mOutput));
        for (ItemStack output : new ItemStack[] { GTNRItemList.BottleInChaos.get(1),
            GTNRItemList.ChaosCredentialHatch.get(1), GTNRItemList.ChaosStructureModule.get(1),
            GTNRItemList.ChaosCriticalPhoton.get(1), GTNRItemList.GravitationalLens.get(1),
            GTNRItemList.WhiteDwarfMold_Ingot.get(1), GTNRItemList.SwelegfyrUpgradeChip.get(1),
            GTNRItemList.ChaosPrototypeProtocol.get(1),
            new ItemStack(com.xyp.gtneverreset.loader.BlockLoader.chaosCasing01, 1, 2) })
            require(
                outputs.contains(com.xyp.gtneverreset.common.crafting.chaos.material.ChaosItemKey.create(output)),
                "manufacturing recipe exists: " + output.getDisplayName());
    }

    private void verifyPackagingTransactions() {
        var rule = com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructureCatalog.displayRules()
            .stream()
            .filter(
                r -> r.controller()
                    .getItemDamage()
                    == gregtech.api.enums.ItemList.Machine_LV_Macerator.get(1)
                        .getItemDamage())
            .findFirst()
            .orElseThrow();
        var inventory = new com.cleanroommc.modularui.utils.item.ItemStackHandler(BottledChaosCrafting.SLOT_COUNT);
        var bench = new BottledChaosCrafting(inventory, () -> {});
        inventory.setStackInSlot(0, GTNRItemList.ChaosStructureModule.get(1));
        inventory.setStackInSlot(
            1,
            rule.controller()
                .copy());
        var preview = bench.preview();
        require(preview != null, "paid controller can be packaged by the real workbench transaction");
        require(inventory.getStackInSlot(1) != null, "preview never consumes physical materials");
        ItemStack module = preview.output(BottledChaosCrafting.OUTPUT_SLOT, 0)
            .copy();
        require(bench.take(preview, 0, BottledChaosCrafting.OUTPUT_SLOT), "packaging commits once");
        require(
            !bench.take(preview, 0, BottledChaosCrafting.OUTPUT_SLOT),
            "stale packaging preview cannot duplicate modules");
        ItemStack disk = AEApi.instance()
            .definitions()
            .items()
            .cell1k()
            .maybeStack(1)
            .get();
        inventory.setStackInSlot(0, module);
        inventory.setStackInSlot(1, disk);
        bench.invalidate();
        var before = AEApi.instance()
            .registries()
            .cell()
            .getCellInventory(disk, null, appeng.api.storage.StorageChannel.ITEMS);
        var unpack = bench.preview();
        require(unpack != null, "paid structure can be returned to a real ME cell");
        var action = new appeng.api.networking.security.BaseActionSource();

        require(
            before.extractItems(AEItemStack.create(rule.controller()), appeng.api.config.Actionable.SIMULATE, action)
                == null,
            "unpack preview leaves the original ME cell unchanged");
        ItemStack returnedDisk = unpack.output(BottledChaosCrafting.SECOND_OUTPUT_SLOT, 0)
            .copy();
        require(bench.take(unpack, 0, BottledChaosCrafting.SECOND_OUTPUT_SLOT), "unpack transaction commits");
        var after = AEApi.instance()
            .registries()
            .cell()
            .getCellInventory(returnedDisk, null, appeng.api.storage.StorageChannel.ITEMS);
        var stored = after
            .extractItems(AEItemStack.create(rule.controller()), appeng.api.config.Actionable.SIMULATE, action);
        require(stored != null && stored.getStackSize() == 1, "ME cell receives exactly the paid controller");
        require(
            !bench.take(unpack, 0, BottledChaosCrafting.SECOND_OUTPUT_SLOT),
            "stale ME unload preview cannot duplicate materials");
    }

    private void verifyNativeProfiles() {
        var profiles = com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosStructureCatalog.displayRules()
            .stream()
            .filter(
                rule -> rule.machineKey()
                    .startsWith("gtnr.chaos.source."))
            .toList();
        require(
            profiles.stream()
                .map(rule -> rule.machineKey())
                .distinct()
                .count() == 29,
            "all 29 original source machine profiles exist independently of TST");
        for (var rule : profiles) {
            var example = rule.example();
            require(
                example != null,
                "native source material bill resolves: " + rule.machineKey() + ":" + rule.profile());
            ItemStack module = ChaosStructureModuleItem.create(example, 1);
            require(
                ChaosStructureModuleItem.structure(module) != null,
                "native source module validates on reload: " + rule.machineKey() + ":" + rule.profile());
        }
        var photon = profiles.stream()
            .filter(
                rule -> rule.machineKey()
                    .endsWith("GT_TILE_ENTITY_PRECISE_HIGH_ENERGY_PHOTONIC_QUANTUM_MASTER"))
            .findFirst()
            .orElseThrow();
        var configuration = new com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosMachineConfiguration(
            () -> {});
        configuration.machines()
            .setStackInSlot(0, ChaosStructureModuleItem.create(photon.example(), 1));
        configuration.updateRequiredPools(photon.poolMask());
        var parameters = configuration.runtime(
            new com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosMachineParameters.Context(
                524288,
                524288,
                new ItemStack[0],
                new net.minecraftforge.fluids.FluidStack[0],
                1,
                1,
                1,
                com.xyp.gtneverreset.common.crafting.chaos.processing.ChaosOverclockType.NONE,
                false,
                false));
        var evaluation = parameters.evaluateCandidates(
            gregtech.api.recipe.RecipeMaps.laserEngraverRecipes,
            32,
            com.xyp.gtneverreset.common.crafting.chaos.machine.BottledChaosSourceRequirements.NONE);
        require(
            evaluation.stream()
                .anyMatch(
                    value -> value.successful() && value.profile() != null
                        && value.profile()
                            .parallel() == 256),
            "native controller with photon structure retains the original 256-parallel profile");
    }

    private static com.cleanroommc.modularui.api.widget.IWidget find(
        com.cleanroommc.modularui.api.widget.IWidget widget, String name) {
        if (name.equals(widget.getName())) return widget;
        for (var child : widget.getChildren()) {
            var found = find(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private static com.cleanroommc.modularui.widgets.ButtonWidget<?> firstButton(
        com.cleanroommc.modularui.api.widget.IWidget widget) {
        if (widget instanceof com.cleanroommc.modularui.widgets.ButtonWidget<?>button) return button;
        for (var child : widget.getChildren()) {
            var found = firstButton(child);
            if (found != null) return found;
        }
        return null;
    }

    private static ItemStack marked(net.minecraft.item.Item item, String name) {
        ItemStack result = new ItemStack(item);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("chaosQa", name);
        result.setTagCompound(tag);
        return result;
    }

    private static ItemStack pattern(ItemStack input, ItemStack output, RecipeMap<?> map) {
        ItemStack result = AEApi.instance()
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
        tag.setString("gtnrPatternRecipeMap", map.unlocalizedName);
        result.setTagCompound(tag);
        return result;
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, int id) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        var result = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        result.setInitialValuesAsNBT(null, (short) id);
        if (!world.playerEntities.isEmpty()) {
            var owner = (net.minecraft.entity.player.EntityPlayer) world.playerEntities.get(0);
            result.setOwnerName(owner.getCommandSenderName());
            result.setOwnerUuid(owner.getUniqueID());
        }
        return result;
    }

    private void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        report.append("PASS ")
            .append(message)
            .append('\n');
        System.out.println("[chaos-qa] PASS " + message);
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
        }, "chaos-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }
}
