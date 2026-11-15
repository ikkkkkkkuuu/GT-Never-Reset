package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.client.gui.QuickEncodingTerminalGui;
import com.xyp.gtneverreset.common.gui.container.QuickEncodingTerminalContainer;
import com.xyp.gtneverreset.common.machine.hatch.CrossRecipeMEPatternBuffer;
import com.xyp.gtneverreset.common.machine.multiblock.UniversalJointFactory;
import com.xyp.gtneverreset.common.machine.multiblock.UniversalJointFactoryProcessingLogic;
import com.xyp.gtneverreset.common.me.CircuitPatternCodec;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;
import com.xyp.gtneverreset.common.packet.RecipeIngredientReplacement;
import com.xyp.gtneverreset.common.packet.RecipeTransferPayload;
import com.xyp.gtneverreset.common.terminal.DualTerminalGuiObject;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;

import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.implementations.tiles.IWirelessAccessPoint;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.implementations.GuiInterfaceTerminal;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.tile.networking.TileCreativeEnergyCell;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;

@Mod(
    modid = "terminalrecipeqa",
    name = "Terminal Recipe Regression QA",
    version = "1",
    dependencies = "after:gtneverreset")
public final class TerminalRecipeChecks {

    private static CrossRecipeMEPatternBuffer provider;
    private boolean launched, checked, finished;
    private int ticks, frames, networkTicks;
    private volatile PacketInterfaceTerminalUpdate preview;
    private volatile Throwable failure;
    private volatile int transportStage;
    private int transportTicks;
    private DualTerminalGuiObject transportTerminal;
    private QuickEncodingTerminalContainer transportContainer;
    private QuickEncodingTerminalGui transportGui;
    private RecipeTransferPayload transportRecipe;
    private IAEStack<?> transportWater, transportLubricant;
    private GTRecipe transportCuttingRecipe;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || checked || !GregTechAPI.sFullLoadFinished) return;
        var players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        try {
            EntityPlayerMP player = (EntityPlayerMP) players.get(0);
            if (provider == null) {
                provider = createProvider(player);
                return;
            }
            if (!provider.getProxy()
                .isActive()) {
                if (++networkTicks > 200) throw new AssertionError("ME network did not become active");
                return;
            }
            if (transportStage == 0) {
                verifyEncoding(player);
                verifyCuttingEncoding(player, ItemList.Circuit_Silicon_Ingot.get(1));
                verifyCuttingEncoding(player, ItemList.Circuit_Wafer_NOR.get(1));
                preview = verifyInterfaceDiscovery(player);
                prepareTransportCheck(player);
            } else if (transportStage == 2) {
                player.openContainer = transportContainer;
                transportContainer.addCraftingToCrafters(player);
                transportContainer.detectAndSendChanges();
                transportStage = 3;
            } else if (transportStage == 4 && transportTerminal.getRecipeMapName() != null) {
                require(
                    RecipeMaps.cutterRecipes.unlocalizedName.equals(transportTerminal.getRecipeMapName()),
                    "real C2S NEI transfer establishes cutter recipe map");
                transportStage = 5;
            } else if (transportStage == 6 && encoded(transportTerminal) != null) {
                require(
                    RecipeMaps.cutterRecipes.unlocalizedName.equals(PatternRecipeMap.name(encoded(transportTerminal))),
                    "real C2S coolant cycle and byproduct removal preserve encoded recipe map");
                provider.onPatternChange(0, encoded(transportTerminal).copy());
                var factory = new UniversalJointFactory("qa.cutter.transport");
                factory.setMachineMode(18);
                var logic = new UniversalJointFactoryProcessingLogic(factory);
                logic.setMachine(factory)
                    .setRecipeMapSupplier(factory::getRecipeMap)
                    .setAvailableVoltage(512)
                    .setAvailableAmperage(1)
                    .setMaxParallel(1)
                    .setVoidProtection(false, false);
                require(
                    logic.tryCachePossibleRecipesFromPattern(
                        provider.inventories()
                            .next()),
                    "network-encoded pattern enters cross-recipe assembly");
                ItemStack material = transportCuttingRecipe.mInputs[0].copy();
                FluidStack coolant = transportCuttingRecipe.mFluidInputs[0].copy();
                logic.setInputItems(material)
                    .setInputFluids(coolant);
                require(
                    logic.process()
                        .wasSuccessful() && material.stackSize == 0
                        && coolant.amount == 0,
                    "network-encoded NOR pattern cuts and consumes exact inputs in extractor mode");
                require(
                    logic.getOutputItems()[0].isItemEqual(transportCuttingRecipe.mOutputs[0])
                        && logic.getOutputItems()[0].stackSize == transportCuttingRecipe.mOutputs[0].stackSize,
                    "network-encoded NOR pattern produces expected chips");
                checked = true;
                transportStage = 7;
            }
        } catch (Throwable error) {
            checked = true;
            failure = error;
        }
    }

    private static DualTerminalGuiObject terminal(EntityPlayer player) throws Exception {
        ItemStack stack = GTNRItemList.WirelessDualInterfaceTerminal.get(1);
        stack.setTagCompound(new NBTTagCompound());
        player.inventory.setInventorySlotContents(0, stack);
        var terminal = new DualTerminalGuiObject(
            (IWirelessTermHandler) stack.getItem(),
            stack,
            player,
            player.worldObj,
            0);
        terminal.setInventorySize(256, 256);
        terminal.readInventory();
        terminal.setCraftingRecipe(false);
        if (!player.worldObj.isRemote) {
            // Supply the real ME node without requiring wireless security pairing in this isolated fixture.
            var accessPoint = Proxy.newProxyInstance(
                IWirelessAccessPoint.class.getClassLoader(),
                new Class<?>[] { IWirelessAccessPoint.class },
                (proxy, method, args) -> provider.getProxy()
                    .getNode());
            field(WirelessTerminalGuiObject.class, "myWap").set(terminal, accessPoint);
        }
        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(
                0,
                AEApi.instance()
                    .definitions()
                    .materials()
                    .blankPattern()
                    .maybeStack(64)
                    .get());
        return terminal;
    }

    private void prepareTransportCheck(EntityPlayerMP player) throws Exception {
        transportTerminal = terminal(player);
        var grid = provider.getProxy()
            .getNode()
            .getGrid();
        field(WirelessTerminalGuiObject.class, "targetGrid").set(transportTerminal, grid);
        field(WirelessTerminalGuiObject.class, "infinityRange").setBoolean(transportTerminal, true);
        var accessPoint = Proxy.newProxyInstance(
            IWirelessAccessPoint.class.getClassLoader(),
            new Class<?>[] { IWirelessAccessPoint.class },
            (proxy, method, args) -> method.getName()
                .equals("getGrid") ? grid
                    : provider.getProxy()
                        .getNode());
        field(WirelessTerminalGuiObject.class, "myWap").set(transportTerminal, accessPoint);
        transportContainer = new QuickEncodingTerminalContainer(player.inventory, transportTerminal);
        transportContainer.windowId = 71;
        transportContainer.setPowerSource(
            provider.getProxy()
                .getEnergy());
        ItemStack wafer = ItemList.Circuit_Wafer_NOR.get(1);
        transportCuttingRecipe = RecipeMaps.cutterRecipes.findRecipeQuery()
            .items(wafer)
            .fluids(Materials.Lubricant.getFluid(1000))
            .find();
        GTRecipe waterRecipe = RecipeMaps.cutterRecipes.findRecipeQuery()
            .items(wafer)
            .fluids(Materials.Water.getFluid(10000))
            .find();
        require(transportCuttingRecipe != null && waterRecipe != null, "native NOR coolant variants exist");
        transportWater = AEFluidStack.create(waterRecipe.mFluidInputs[0]);
        transportLubricant = AEFluidStack.create(transportCuttingRecipe.mFluidInputs[0]);
        transportRecipe = new RecipeTransferPayload(
            false,
            false,
            4,
            false,
            new IAEStack<?>[] { AEItemStack.create(wafer), transportWater },
            java.util.Arrays.stream(waterRecipe.mOutputs)
                .map(AEItemStack::create)
                .toArray(IAEStack<?>[]::new),
            null,
            PatternRecipeMap.nameForImport(RecipeMaps.cutterFakeRecipes));
        transportStage = 1;
    }

    private boolean checkClientTransport(Minecraft mc) throws Exception {
        if (transportStage == 0) return true;
        if (transportStage == 8) return false;
        if (transportStage == 1) {
            transportGui = new QuickEncodingTerminalGui(mc.thePlayer.inventory, terminal(mc.thePlayer));
            mc.displayGuiScreen(transportGui);
            transportGui.inventorySlots.windowId = 71;
            mc.thePlayer.openContainer = transportGui.inventorySlots;
            transportStage = 2;
        } else if (transportStage == 3 && ++transportTicks >= 10) {
            transportGui.transferRecipe(transportRecipe, null);
            transportStage = 4;
        } else if (transportStage == 5) {
            var container = (QuickEncodingTerminalContainer) transportGui.inventorySlots;
            if (container.inputsSync.get()
                .getAEStackInSlot(1) == null) return true;
            // Same public entry point as Shift + mouse wheel, using the real AE2 C2S transport and sync flush.
            transportGui.replaceRecipeIngredient(transportWater, transportLubricant);
            for (int i = 1; i < DualTerminalGuiObject.PATTERN_SLOT_COUNT; i++) {
                container.updateVirtualSlot(StorageName.CRAFTING_OUTPUT, i, null);
            }
            container.quickEncodeAction.send();
            transportStage = 6;
        } else if (transportStage == 7) {
            var terminal = (DualTerminalGuiObject) ((QuickEncodingTerminalContainer) transportGui.inventorySlots)
                .getTarget();
            ItemStack received = encoded(terminal);
            if (received == null) return true;
            require(
                RecipeMaps.cutterRecipes.unlocalizedName.equals(PatternRecipeMap.name(received)),
                "client receives encoded cutter recipe map over S2C");
            require(
                received.getTooltip(mc.thePlayer, false)
                    .stream()
                    .anyMatch(line -> ((String) line).contains("配方池")),
                "client encoded-pattern tooltip displays recipe map");
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "terminal-cutter-network-encoding.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            transportStage = 8;
            mc.displayGuiScreen(null);
            return false;
        }
        return true;
    }

    private static RecipeTransferPayload recipe(String map, boolean encode) {
        return new RecipeTransferPayload(
            false,
            encode,
            4,
            false,
            new IAEStack<?>[] { AEItemStack.create(new ItemStack(Items.iron_ingot)) },
            new IAEStack<?>[] { AEItemStack.create(new ItemStack(Items.stick)),
                AEItemStack.create(new ItemStack(Items.redstone)),
                AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 100)) },
            null,
            map);
    }

    private static void transfer(QuickEncodingTerminalContainer container, RecipeTransferPayload recipe)
        throws Exception {
        Method method = QuickEncodingTerminalContainer.class
            .getDeclaredMethod("applyRecipeTransfer", RecipeTransferPayload.class);
        method.setAccessible(true);
        method.invoke(container, recipe);
    }

    private static ItemStack encoded(DualTerminalGuiObject terminal) {
        return terminal.getInventoryByName("pattern")
            .getStackInSlot(1);
    }

    private static void encode(DualTerminalGuiObject terminal, EntityPlayer player, String map, String check) {
        require(terminal.encode(null, null, null, "QA", player.worldObj), check + " encodes");
        require(java.util.Objects.equals(map, PatternRecipeMap.name(encoded(terminal))), check + " recipe type");
    }

    private static void verifyEncoding(EntityPlayerMP player) throws Exception {
        var terminal = terminal(player);
        var container = new QuickEncodingTerminalContainer(player.inventory, terminal);
        String lathe = RecipeMaps.latheRecipes.unlocalizedName;
        transfer(container, recipe(lathe, true));
        require(lathe.equals(PatternRecipeMap.name(encoded(terminal))), "NEI automatic encoding writes recipe type");

        var outputs = terminal.getAEInventoryByName(StorageName.CRAFTING_OUTPUT);
        outputs.putAEStackInSlot(1, null);
        encode(terminal, player, lathe, "remove item byproduct and overwrite");
        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(1, null);
        require(lathe.equals(terminal.getRecipeMapName()), "taking encoded pattern preserves draft");
        for (int slot = 0; slot < outputs.getSizeInventory(); slot++) {
            IAEStack<?> output = outputs.getAEStackInSlot(slot);
            if (output != null && output.isFluid()) outputs.putAEStackInSlot(slot, null);
        }
        encode(terminal, player, lathe, "remove fluid byproduct and encode again");
        require(
            CircuitPatternCodec.decode(encoded(terminal), player.worldObj)
                .getCondensedAEOutputs().length == 1,
            "removed byproducts stay absent from encoded pattern");
        ItemStack reducedPattern = encoded(terminal).copy();

        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(1, null);
        outputs.putAEStackInSlot(0, AEItemStack.create(new ItemStack(Items.stick, 4)));
        terminal.getAEInventoryByName(StorageName.CRAFTING_INPUT)
            .putAEStackInSlot(0, AEItemStack.create(new ItemStack(Items.iron_ingot, 4)));
        terminal.writeInventory();
        terminal.readInventory();
        container = new QuickEncodingTerminalContainer(player.inventory, terminal);
        outputs = terminal.getAEInventoryByName(StorageName.CRAFTING_OUTPUT);
        encode(terminal, player, lathe, "quantity edits and terminal reload");

        outputs.putAEStackInSlot(1, AEItemStack.create(new ItemStack(Items.diamond)));
        encode(terminal, player, null, "unrelated output rejects stale recipe type");
        outputs.putAEStackInSlot(1, null);
        terminal.getAEInventoryByName(StorageName.CRAFTING_INPUT)
            .putAEStackInSlot(0, AEItemStack.create(new ItemStack(Items.gold_ingot)));
        encode(terminal, player, null, "changed input rejects stale recipe type");

        transfer(container, recipe(lathe, false));
        outputs.putAEStackInSlot(1, AEItemStack.create(new ItemStack(Items.stick)));
        require(terminal.getRecipeMapName() == null, "extra duplicate output is not a removed byproduct");
        transfer(container, recipe(lathe, false));
        outputs.putAEStackInSlot(0, null);
        outputs.putAEStackInSlot(1, null);
        outputs.putAEStackInSlot(2, null);
        require(terminal.getRecipeMapName() == null, "empty output has no recipe type");

        transfer(container, recipe(lathe, false));
        container.clear();
        terminal.getAEInventoryByName(StorageName.CRAFTING_INPUT)
            .putAEStackInSlot(0, AEItemStack.create(new ItemStack(Items.iron_ingot)));
        outputs.putAEStackInSlot(0, AEItemStack.create(new ItemStack(Items.stick)));
        require(terminal.getRecipeMapName() == null, "clear removes metadata even after rebuilding the same recipe");

        transfer(container, recipe(RecipeMaps.assemblerRecipes.unlocalizedName, true));
        require(
            RecipeMaps.assemblerRecipes.unlocalizedName.equals(PatternRecipeMap.name(encoded(terminal))),
            "next NEI transfer replaces recipe type");
        transfer(container, recipe(null, true));
        require(PatternRecipeMap.name(encoded(terminal)) == null, "untyped transfer clears recipe type");

        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(1, reducedPattern.copy());
        encode(terminal, player, lathe, "reopening annotated pattern restores recipe type");
        ItemStack untyped = reducedPattern.copy();
        PatternRecipeMap.write(untyped, null);
        terminal.getInventoryByName("pattern")
            .setInventorySlotContents(1, untyped);
        encode(terminal, player, null, "reopening untyped pattern clears recipe type");
        container.onContainerClosed(player);
    }

    private static void verifyCuttingEncoding(EntityPlayerMP player, ItemStack input) throws Exception {
        var terminal = terminal(player);
        var container = new QuickEncodingTerminalContainer(player.inventory, terminal);
        var recipe = RecipeMaps.cutterRecipes.findRecipeQuery()
            .items(input)
            .fluids(Materials.Lubricant.getFluid(1000))
            .find();
        require(recipe != null, "native lubricant cutting recipe exists for " + input.getDisplayName());
        IAEStack<?>[] outputs = java.util.Arrays.stream(recipe.mOutputs)
            .map(AEItemStack::create)
            .toArray(IAEStack<?>[]::new);
        IAEStack<?> water = AEFluidStack.create(Materials.Water.getFluid(28));
        IAEStack<?> lubricant = AEFluidStack.create(recipe.mFluidInputs[0]);
        String map = PatternRecipeMap.nameForImport(RecipeMaps.cutterFakeRecipes);
        transfer(
            container,
            new RecipeTransferPayload(
                false,
                false,
                4,
                false,
                new IAEStack<?>[] { AEItemStack.create(input), water },
                outputs,
                null,
                map));
        Method replace = QuickEncodingTerminalContainer.class
            .getDeclaredMethod("applyRecipeIngredient", RecipeIngredientReplacement.class);
        replace.setAccessible(true);
        replace.invoke(container, new RecipeIngredientReplacement(water, lubricant));
        require(map.equals(terminal.getRecipeMapName()), "NEI coolant alternative preserves cutting recipe map");
        require(
            terminal.getAEInventoryByName(StorageName.CRAFTING_INPUT)
                .getAEStackInSlot(1)
                .getStackSize() == lubricant.getStackSize(),
            "coolant alternative uses its own amount");
        var outputInventory = terminal.getAEInventoryByName(StorageName.CRAFTING_OUTPUT);
        for (int i = 1; i < outputInventory.getSizeInventory(); i++) outputInventory.putAEStackInSlot(i, null);
        encode(terminal, player, map, "coolant cycle and byproduct removal");
        ItemStack pattern = ItemStack.loadItemStackFromNBT(encoded(terminal).writeToNBT(new NBTTagCompound()));
        provider.onPatternChange(0, pattern);
        var factory = new UniversalJointFactory("qa.cutter.encoding");
        factory.setMachineMode(18);
        var logic = new UniversalJointFactoryProcessingLogic(factory);
        logic.setMachine(factory)
            .setRecipeMapSupplier(factory::getRecipeMap)
            .setAvailableVoltage(512)
            .setAvailableAmperage(1)
            .setMaxParallel(1)
            .setVoidProtection(false, false);
        require(
            logic.tryCachePossibleRecipesFromPattern(
                provider.inventories()
                    .next()),
            "encoded coolant variant routes through cross-recipe assembly");
        ItemStack material = input.copy();
        FluidStack coolant = recipe.mFluidInputs[0].copy();
        logic.setInputItems(material)
            .setInputFluids(coolant);
        require(
            logic.process()
                .wasSuccessful(),
            "encoded cutting recipe runs with extractor selected");
        require(material.stackSize == 0 && coolant.amount == 0, "cutting consumes material and selected coolant");
        require(
            logic.getOutputItems()[0].isItemEqual(recipe.mOutputs[0])
                && logic.getOutputItems()[0].stackSize == recipe.mOutputs[0].stackSize,
            "cutting produces the expected wafers");
        require(factory.getMachineMode() == 18, "cross-recipe cutting preserves controller mode");
        terminal.getAEInventoryByName(StorageName.CRAFTING_INPUT)
            .putAEStackInSlot(0, AEItemStack.create(new ItemStack(Items.gold_ingot)));
        replace.invoke(container, new RecipeIngredientReplacement(lubricant, water));
        require(terminal.getRecipeMapName() == null, "NEI cycling cannot restore metadata after unrelated input edit");
        container.onContainerClosed(player);
    }

    private static CrossRecipeMEPatternBuffer createProvider(EntityPlayerMP player) throws Exception {
        var tile = new BaseMetaTileEntity();
        tile.setWorldObj(player.worldObj);
        tile.xCoord = (int) player.posX;
        tile.yCoord = 100;
        tile.zCoord = (int) player.posZ;
        tile.setInitialValuesAsNBT(null, (short) GTNRMachineID.CrossRecipeMEPatternBuffer.id);
        var hatch = (CrossRecipeMEPatternBuffer) tile.getMetaTileEntity();
        require(!hatch.hasCustomName(), "cross-recipe hatch uses its default name");
        if (!hatch.shouldDisplay()) hatch.toggleTerminalVisibility();
        hatch.getProxy()
            .onReady();
        var power = new TileCreativeEnergyCell();
        power.setWorldObj(player.worldObj);
        power.xCoord = tile.xCoord + 1;
        power.yCoord = tile.yCoord;
        power.zCoord = tile.zCoord;
        power.getProxy()
            .onReady();
        AEApi.instance()
            .createGridConnection(
                hatch.getProxy()
                    .getNode(),
                power.getProxy()
                    .getNode());
        return hatch;
    }

    private static PacketInterfaceTerminalUpdate verifyInterfaceDiscovery(EntityPlayerMP player) throws Exception {
        var hatch = provider;
        var host = terminal(player);
        var container = new ContainerInterfaceTerminal(player.inventory, host);
        Method update = ContainerInterfaceTerminal.class.getDeclaredMethod("updateList");
        update.setAccessible(true);
        var packet = (PacketInterfaceTerminalUpdate) field(ContainerInterfaceTerminal.class, "dirty").get(container);
        Map<?, ?> tracked = (Map<?, ?>) field(ContainerInterfaceTerminal.class, "tracked").get(container);
        require(tracked.containsKey(hatch), "native interface terminal discovers cross-recipe hatch");
        Object entry = tracked.get(hatch);
        require(field(entry.getClass(), "online").getBoolean(entry), "discovered hatch is online");
        require(field(entry.getClass(), "shouldDisplay").getBoolean(entry), "display toggle is honored");
        hatch.toggleTerminalVisibility();
        update.invoke(container);
        require(!field(entry.getClass(), "shouldDisplay").getBoolean(entry), "hide toggle is honored");
        hatch.toggleTerminalVisibility();
        return packet;
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void require(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
        System.out.println("[terminal-recipe-qa] PASS " + message);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (!launched && mc.currentScreen instanceof GuiMainMenu) {
                launched = true;
                mc.launchIntegratedServer(
                    "terminal-" + System.currentTimeMillis(),
                    "Terminal Recipe QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("Server checks failed", failure);
            if (mc.thePlayer == null) return;
            if (++ticks > 1800) throw new AssertionError("QA timeout");
            if (checkClientTransport(mc)) return;
            if (preview == null) return;
            if (!(mc.currentScreen instanceof GuiInterfaceTerminal)) {
                var gui = new GuiInterfaceTerminal(mc.thePlayer.inventory, terminal(mc.thePlayer));
                mc.displayGuiScreen(gui);
                @SuppressWarnings("unchecked")
                var entries = (List<PacketInterfaceTerminalUpdate.PacketEntry>) field(
                    PacketInterfaceTerminalUpdate.class,
                    "commands").get(preview);
                gui.postUpdate(entries, 0);
            }
            if (++frames < 16) return;
            Object masterList = field(GuiInterfaceTerminal.class, "masterList").get(mc.currentScreen);
            Map<?, ?> displayed = (Map<?, ?>) field(masterList.getClass(), "list").get(masterList);
            require(displayed.size() == 1, "client receives the cross-recipe hatch entry");
            Object entry = displayed.values()
                .iterator()
                .next();
            String name = (String) field(entry.getClass(), "dispName").get(entry);
            require(name.contains("跨配方类型"), "client displays default Chinese cross-recipe name: " + name);
            List<?> sections = (List<?>) field(masterList.getClass(), "visibleSections").get(masterList);
            require(!sections.isEmpty(), "cross-recipe interface name is visible in the terminal");
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "terminal-cross-recipe-default-name.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            finish(null);
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void finish(Throwable error) {
        finished = true;
        if (error != null) error.printStackTrace();
        try {
            Files.writeString(Path.of("result.txt"), error == null ? "PASS" : "FAIL " + error);
        } catch (Exception io) {
            io.printStackTrace();
        }
        Thread exit = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
                Thread.currentThread()
                    .interrupt();
            }
            FMLCommonHandler.instance()
                .exitJava(error == null ? 0 : 1, true);
        }, "terminal-recipe-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }
}
