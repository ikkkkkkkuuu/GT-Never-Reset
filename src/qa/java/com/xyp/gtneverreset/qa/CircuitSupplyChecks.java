package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
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
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.viewport.LocatedWidget;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.glodblock.github.loader.ItemAndBlockHolder;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer;
import com.xyp.gtneverreset.common.me.MERecipeFluidStack;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStack;
import appeng.container.ContainerNull;
import appeng.tile.networking.TileCreativeEnergyCell;
import appeng.tile.storage.TileDrive;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.nei.ItemPanels;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

@Mod(modid = "circuitsupplyqa", name = "Circuit Supply QA", version = "1", dependencies = "after:gtneverreset")
public final class CircuitSupplyChecks {

    private static final boolean CROSS_RECIPE = Boolean.getBoolean("gtnr.cross.recipe.supplies.qa");
    private static final GTNRMachineID HATCH = CROSS_RECIPE ? GTNRMachineID.CrossRecipeMEPatternBuffer
        : GTNRMachineID.CircuitMEPatternBuffer;
    private static final long LIVE_ITEM_BALANCE = 640;
    private static final long LIVE_FLUID_BALANCE = 14_400_000;
    private static final long ITEM_DEBIT = 128;
    private static final long FLUID_DEBIT = 400_000;
    private static final int ITEM_SLOT = 1;
    private static final int FLUID_SLOT = 16;

    private CircuitMEPatternBuffer guiHatch;
    private int waitForTile;
    private volatile boolean dropsReceived;
    private CircuitMEPatternBuffer serverHatch;
    private volatile boolean firstBalanceSeen;
    private boolean launched, verified, opened, finished, networkSeeded, balanceDebited;
    private int ticks, stage, frames;
    private volatile Throwable failure;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || failure != null || !GregTechAPI.sFullLoadFinished) return;
        var players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) players.get(0);
        try {
            if (guiHatch != null) {
                if (!opened && ++waitForTile >= 30) {
                    require(guiHatch.onRightclick(guiHatch.getBaseMetaTileEntity(), player), "native hatch GUI opens");
                    opened = true;
                }
                if (!dropsReceived) {
                    var samples = guiHatch.getSharedSupplies();
                    var item = samples.target(1);
                    var bucket = samples.target(17);
                    var display = samples.target(16);
                    if (item != null && bucket != null && display != null) {
                        require(
                            item.isSameType(AEItemStack.create(taggedSample())),
                            "NEI item mark preserves NBT and defaults to unlimited");
                        require(
                            bucket.isSameType(AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 1))),
                            "NEI filled bucket marks its fluid without a limit");
                        require(
                            display.isSameType(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1))),
                            "NEI GT fluid display replaces the fluid mark");
                        require(player.inventory.getItemStack() == null, "ghost drops leave the real cursor empty");
                        dropsReceived = true;
                    }
                }
                verifyDisplayBalances();
                return;
            }
            if (serverHatch == null) {
                serverHatch = hatch(player.worldObj);
                var power = new TileCreativeEnergyCell();
                power.setWorldObj(player.worldObj);
                power.xCoord = 1;
                var drive = new TileDrive();
                drive.setWorldObj(player.worldObj);
                drive.xCoord = 2;
                drive.setInventorySlotContents(
                    0,
                    AEApi.instance()
                        .definitions()
                        .items()
                        .cell1k()
                        .maybeStack(1)
                        .get());
                drive.setInventorySlotContents(1, new ItemStack(ItemAndBlockHolder.SINGULARITY_CELL));
                power.getProxy()
                    .onReady();
                drive.getProxy()
                    .onReady();
                serverHatch.getProxy()
                    .onReady();
                AEApi.instance()
                    .createGridConnection(
                        serverHatch.getProxy()
                            .getNode(),
                        power.getProxy()
                            .getNode());
                AEApi.instance()
                    .createGridConnection(
                        drive.getProxy()
                            .getNode(),
                        power.getProxy()
                            .getNode());
                return;
            }
            if (!serverHatch.isActive()) return;
            if (!verified) {
                verify(player);
                verified = true;
                return;
            }
            var supplies = serverHatch.getSharedSupplies();
            int x = (int) player.posX + 2, y = (int) player.posY, z = (int) player.posZ;
            player.worldObj.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
            var tile = (BaseMetaTileEntity) player.worldObj.getTileEntity(x, y, z);
            tile.setInitialValuesAsNBT(null, (short) HATCH.id);
            guiHatch = (CircuitMEPatternBuffer) tile.getMetaTileEntity();
            var state = new NBTTagCompound();
            supplies.write(state);
            var copiedSupplies = state.getCompoundTag("circuitSharedSupplies");
            copiedSupplies.removeTag("longItems");
            copiedSupplies.removeTag("longFluids");
            guiHatch.getSharedSupplies()
                .read(state);
            player.worldObj.markBlockForUpdate(x, y, z);
        } catch (Throwable error) {
            failure = error;
        }
    }

    private void verifyDisplayBalances() throws Exception {
        var supplies = guiHatch.getSharedSupplies();
        if (!dropsReceived) return;
        var proxy = guiHatch.getProxy();
        var source = guiHatch.getMEOutputActionSource();
        if (!networkSeeded) {
            require(
                supplies.target(ITEM_SLOT) != null && supplies.target(FLUID_SLOT) != null,
                "both item and fluid are marked");
            proxy.onReady();
            AEApi.instance()
                .createGridConnection(
                    proxy.getNode(),
                    serverHatch.getProxy()
                        .getNode());
            var storage = proxy.getStorage();
            var item = AEItemStack.create(taggedSample())
                .setStackSize(LIVE_ITEM_BALANCE);
            var water = AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1));
            var fluids = storage.getFluidInventory();
            long existing = fluidStock(fluids, water, source);
            require(
                storage.getItemInventory()
                    .injectItems(item, Actionable.MODULATE, source) == null,
                "seed 640 marked items into the display test grid");
            require(
                fluids.injectItems(water.setStackSize(LIVE_FLUID_BALANCE - existing), Actionable.MODULATE, source)
                    == null,
                "seed 14.4 million mB of marked fluid into the display test grid");
            networkSeeded = true;
        }
        if (firstBalanceSeen && !balanceDebited) {
            var storage = proxy.getStorage();
            var item = AEItemStack.create(taggedSample())
                .setStackSize(ITEM_DEBIT);
            var water = AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1))
                .setStackSize(FLUID_DEBIT);
            require(
                storage.getItemInventory()
                    .extractItems(item, Actionable.MODULATE, source)
                    .getStackSize() == ITEM_DEBIT
                    && storage.getFluidInventory()
                        .extractItems(water, Actionable.MODULATE, source)
                        .getStackSize() == FLUID_DEBIT,
                "debit part of each displayed balance from ME");
            balanceDebited = true;
        }
    }

    private void verify(EntityPlayerMP player) throws Exception {
        verifyWideFluidSupply(player.worldObj);
        var supplies = serverHatch.getSharedSupplies();
        var grid = serverHatch.getProxy();
        var storage = grid.getStorage();
        var source = serverHatch.getMEOutputActionSource();
        var iron = AEItemStack.create(new ItemStack(Items.iron_ingot, 12));
        var water = AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 2500));
        require(
            storage.getItemInventory()
                .injectItems(iron, Actionable.MODULATE, source) == null,
            "seed item cell");
        require(
            storage.getFluidInventory()
                .injectItems(water, Actionable.MODULATE, source) == null,
            "seed fluid cell");
        supplies.setTarget(
            0,
            iron.copy()
                .setStackSize(8));
        supplies.setTarget(
            16,
            water.copy()
                .setStackSize(1000));
        require(supplies.beginNetworkCheck(), "open a shared supply recipe check");
        FluidStack offer = supplies.networkFluidInputs(grid, source)
            .get(0);
        require(
            offer.amount == 2500 && supplies.networkFluidInputs(grid, source)
                .get(0) == offer,
            "recipe sees the whole ME fluid balance in a stable view");
        var offeredItems = supplies.networkItemInputs(grid, source);
        require(
            offeredItems.size() == 1 && offeredItems.get(0).stackSize == 12,
            "recipe sees the whole ME item balance");
        offeredItems.get(0).stackSize -= 2;
        var fluids = storage.getFluidInventory();
        var moved = fluids.extractItems(
            water.copy()
                .setStackSize(2499),
            Actionable.MODULATE,
            source);
        offer.amount -= 5;
        require(
            !supplies.endNetworkCheck(grid, source) && fluids.extractItems(water, Actionable.SIMULATE, source)
                .getStackSize() == 1
                && supplies.fluidInputs()
                    .isEmpty(),
            "lost network stock fails settlement without taking a partial payment");
        require(fluids.injectItems(moved, Actionable.MODULATE, source) == null, "restore fixture stock");
        require(
            storage.getItemInventory()
                .extractItems(iron, Actionable.SIMULATE, source)
                .getStackSize() == 12 && supplies.itemInputs()
                    .isEmpty(),
            "failed mixed check leaves items in ME");
        var migratedSupplies = new NBTTagCompound();
        supplies.write(migratedSupplies);
        var legacy = migratedSupplies.getCompoundTag("circuitSharedSupplies");
        legacy.setBoolean("bufferedItems", true);
        legacy.setBoolean("bufferedFluids", true);
        var items = new NBTTagList();
        items.appendTag(
            iron.copy()
                .setStackSize(8)
                .toNBTGeneric());
        legacy.setTag("longItems", items);
        var fluidList = new NBTTagList();
        fluidList.appendTag(
            water.copy()
                .setStackSize(1000)
                .toNBTGeneric());
        legacy.setTag("longFluids", fluidList);
        supplies.read(migratedSupplies);
        require(
            supplies.itemInputs()
                .get(0).stackSize == 8
                && supplies.fluidInputs()
                    .get(0).amount == 1000,
            "migration retains both kinds of legacy reserves");
        var clay = AEItemStack.create(new ItemStack(Items.clay_ball));
        for (int slot = 0; slot < 3; slot++) serverHatch
            .onPatternChange(slot, pattern(new IAEStack<?>[] { clay }, new ItemStack(Items.paper, slot + 1)));
        var slots = serverHatch.inventories();
        var first = slots.next();
        var second = slots.next();
        var idle = slots.next();
        require(
            idle.isEmpty() && idle.getFluidInputs().length == 0 && idle.getItemInputs().length == 0,
            "shared stock cannot activate an empty order");
        for (var slot : List.of(first, second)) {
            var delivery = new MEInventoryCrafting(new ContainerNull(), 1, 1);
            delivery.setInventorySlotContents(0, clay.copy());
            require(slot.insertItemsAndFluids(delivery), "pattern delivery");
        }
        ItemStack shared = first.getItemInputs()[1];
        var recipe = new GTRecipe(
            false,
            new ItemStack[] { new ItemStack(Items.clay_ball), new ItemStack(Items.iron_ingot, 2) },
            new ItemStack[] { new ItemStack(Items.paper) },
            null,
            null,
            null,
            null,
            null,
            new FluidStack[] { new FluidStack(FluidRegistry.WATER, 250) },
            null,
            20,
            8,
            0);
        require(
            recipe.isRecipeInputEqual(true, first.getFluidInputs(), first.getItemInputs()) && first.isEmpty(),
            "GT consumes the delivered input and both shared ingredients");
        require(
            second.getItemInputs()[1] == shared && shared.stackSize == 6 && second.getFluidInputs()[0].amount == 750,
            "second order sees first order's real debit");
        var saved = new NBTTagCompound();
        serverHatch.saveNBTData(saved);
        var restored = hatch(player.worldObj);
        restored.loadNBTData(saved);
        require(
            restored.getSharedSupplies()
                .itemInputs()
                .get(0).stackSize == 6
                && restored.getSharedSupplies()
                    .fluidInputs()
                    .get(0).amount == 750,
            "save/load preserves the exact remainder after consuming legacy reserves");
        serverHatch.setItemNBT(saved);
        restored.loadNBTData(saved);
        require(
            restored.getSharedSupplies()
                .itemInputs()
                .get(0).stackSize == 6,
            "dropped hatch retains reserve");
    }

    private void verifyWideFluidSupply(World world) throws Exception {
        long stocked = 6_000_000_000L;
        int parallel = 60, perRecipe = 50_000_000;
        long consumed = (long) parallel * perRecipe;
        var supplies = serverHatch.getSharedSupplies();
        var proxy = serverHatch.getProxy();
        var storage = proxy.getStorage()
            .getFluidInventory();
        var source = serverHatch.getMEOutputActionSource();
        var lava = AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 1));
        require(
            storage.injectItems(
                lava.copy()
                    .setStackSize(stocked),
                Actionable.MODULATE,
                source) == null,
            "ME stores six billion mB of lava");
        // Emulate a saved cap from the old amount editor, then verify that the mark offers the full network balance.
        var saved = new NBTTagCompound();
        var shared = new NBTTagCompound();
        var filters = new NBTTagList();
        var oldMark = lava.copy()
            .setStackSize(3_500_000_000L)
            .toNBTGeneric();
        oldMark.setInteger("slot", 17);
        filters.appendTag(oldMark);
        shared.setTag("targets", filters);
        shared.setBoolean("bufferedFluids", true);
        saved.setTag("circuitSharedSupplies", shared);
        supplies.read(saved);
        require(
            supplies.target(17)
                .isSameType(lava),
            "loading an old cap preserves the fluid mark");
        var input = new ItemStack(Items.clay_ball);
        var product = new ItemStack(Items.paper);
        serverHatch.onPatternChange(0, pattern(new IAEStack<?>[] { AEItemStack.create(input) }, product));
        var order = serverHatch.inventories()
            .next();
        var recipe = new GTRecipe(
            false,
            new ItemStack[] { input },
            new ItemStack[] { product },
            null,
            null,
            null,
            null,
            null,
            new FluidStack[] { new FluidStack(FluidRegistry.LAVA, perRecipe) },
            null,
            20,
            8,
            0);

        var reactorTile = new BaseMetaTileEntity();
        reactorTile.setWorldObj(world);
        reactorTile.setInitialValuesAsNBT(
            null,
            (short) ItemList.MegaChemicalReactor.get(1)
                .getItemDamage());
        var controller = (MTEMultiBlockBase) reactorTile.getMetaTileEntity();
        controller.mDualInputHatches.add(serverHatch);
        controller.startRecipeProcessing();
        try {
            require(supplies.isCheckingNetwork(), "multiblock opens the pattern hatch network check");
            var delivery = new MEInventoryCrafting(new ContainerNull(), 1, 1);
            delivery.setInventorySlotContents(
                0,
                AEItemStack.create(input)
                    .setStackSize(parallel));
            require(order.insertItemsAndFluids(delivery), "pattern receives 60 delivered recipe inputs");
            var fluids = order.getFluidInputs();
            require(fluidAmount(fluids) == stocked, "old cap does not limit the six-billion-mB offer");
            var items = order.getItemInputs();
            require(
                recipe.maxParallelCalculatedByInputs(parallel, fluids, items) == parallel,
                "GT computes 60 parallels");
            recipe.consumeInput(parallel, fluids, items);
            require(fluidAmount(fluids) == stocked - consumed, "GT consumes three billion mB from the offer");
            require(fluidStock(storage, lava, source) == stocked, "the recipe check has not debited physical ME stock");
        } finally {
            controller.endRecipeProcessing();
        }
        require(!supplies.isCheckingNetwork(), "settlement closes the pattern hatch network check");
        require(fluidStock(storage, lava, source) == stocked - consumed, "settlement debits exactly three billion mB");
        controller.endRecipeProcessing();
        require(fluidStock(storage, lava, source) == stocked - consumed, "repeating settlement cannot debit ME twice");

        supplies.setTarget(17, null);
        serverHatch.onPatternChange(0, null);
        storage.extractItems(
            lava.copy()
                .setStackSize(stocked - consumed),
            Actionable.MODULATE,
            source);
        require(
            fluidStock(storage, lava, source) == 0,
            "clear lava from the single-type ME cell before the water checks");
    }

    private static long fluidAmount(FluidStack[] stacks) {
        return Arrays.stream(stacks)
            .mapToLong(MERecipeFluidStack::getAmount)
            .sum();
    }

    private static long fluidStock(IMEMonitor<IAEFluidStack> storage, IAEFluidStack fluid, BaseActionSource source) {
        var stock = storage.extractItems(
            fluid.copy()
                .setStackSize(Long.MAX_VALUE),
            Actionable.SIMULATE,
            source);
        return stock == null ? 0 : stock.getStackSize();
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var mc = Minecraft.getMinecraft();
        try {
            if (!launched && mc.theWorld == null && mc.currentScreen != null && GregTechAPI.sFullLoadFinished) {
                launched = true;
                mc.launchIntegratedServer(
                    "supply-" + System.currentTimeMillis(),
                    "Circuit supplies",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("Server fixture", failure);
            if (mc.thePlayer == null) return;
            if (++ticks > 1200) throw new AssertionError("Timeout at " + stage);
            var screen = ModularScreen.getCurrent();
            if (screen == null || ++frames < 12) return;
            var panel = screen.getPanelManager()
                .getTopMostPanel();
            if (stage == 0) {
                var button = (ButtonWidget<?>) find(panel, "shared_supplies");
                if (button == null) return;
                if (CROSS_RECIPE) {
                    require(
                        find(panel, "auto_assign_recipe_names") != null && find(panel, "terminal_recipe_map") != null,
                        "shared supplies open alongside the cross-recipe hatch controls");
                }
                mc.ingameGUI.getChatGUI()
                    .clearChatMessages();
                shot("circuit-supplies-button.png");
                button.onMousePressed(0);
            } else if (stage == 1) {
                var fluid = (ButtonWidget<?>) find(panel, "supply_sample_16");
                if (fluid == null) return;
                require(
                    find(panel, "pull_interval") == null && find(panel, "pull_on_check") == null
                        && find(panel, "direct_items") == null
                        && find(panel, "direct_fluids") == null,
                    "shared supply page exposes only ME marks");
                drop(panel, 1, taggedSample(), false, true);
                drop(panel, 2, new ItemStack(Items.apple), false, true);
                drop(panel, 17, new ItemStack(Items.lava_bucket), false, true);
                drop(
                    panel,
                    16,
                    GTUtility.getFluidDisplayStack(new FluidStack(FluidRegistry.WATER, 500), true),
                    true,
                    true);
                drop(panel, 17, new ItemStack(Items.stick), false, false);
            } else if (stage == 2) {
                if (!dropsReceived || displayAmount(screen, panel, ITEM_SLOT) != LIVE_ITEM_BALANCE
                    || displayAmount(screen, panel, FLUID_SLOT) != LIVE_FLUID_BALANCE) return;
                shot("circuit-supplies-network-balances.png");
                ((ButtonWidget<?>) find(panel, "supply_sample_" + ITEM_SLOT)).onMousePressed(1);
                require(
                    !screen.isPanelOpen("supply_limit_" + ITEM_SLOT),
                    "right-click does not open an item limit editor");
                ((ButtonWidget<?>) find(panel, "supply_sample_" + FLUID_SLOT)).onMousePressed(1);
                require(
                    !screen.isPanelOpen("supply_limit_" + FLUID_SLOT),
                    "right-click does not open a fluid limit editor");
                require(true, "marked slots show the full ME item and fluid balances");
                firstBalanceSeen = true;
            } else {
                if (displayAmount(screen, panel, ITEM_SLOT) != LIVE_ITEM_BALANCE - ITEM_DEBIT
                    || displayAmount(screen, panel, FLUID_SLOT) != LIVE_FLUID_BALANCE - FLUID_DEBIT) return;
                require(true, "the open GUI updates after ME stock changes");
                shot("circuit-supplies-network-balances-after-debit.png");
                finish(null);
            }
            stage++;
            frames = 0;
        } catch (Throwable error) {
            finish(error);
        }
    }

    private static long displayAmount(ModularScreen screen, ModularPanel panel, int slot) {
        var sync = screen.getSyncManager()
            .getPanelSyncManager(panel.getName());
        return ((LongSyncValue) sync.findSyncHandlerNullable("amount", slot)).getLongValue();
    }

    private static ItemStack taggedSample() {
        return new ItemStack(Items.gold_ingot, 3).setStackDisplayName("NEI sample");
    }

    private static void drop(ModularPanel panel, int slot, ItemStack sample, boolean bookmark, boolean accepted) {
        var source = bookmark ? ItemPanels.bookmarkPanel : ItemPanels.itemPanel;
        source.draggedStack = sample;
        panel.getHovering()
            .clear();
        panel.getHovering()
            .add(LocatedWidget.of(find(panel, "supply_sample_" + slot)));
        panel.onMousePressed(0);
        require((source.draggedStack == null) == accepted, "NEI drag result for slot " + slot);
        panel.onMouseRelease(0);
        source.draggedStack = null;
    }

    private static IWidget find(IWidget widget, String name) {
        if (name.equals(widget.getName())) return widget;
        for (IWidget child : widget.getChildren()) {
            var found = find(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private static CircuitMEPatternBuffer hatch(World world) {
        BaseMetaTileEntity tile = new BaseMetaTileEntity();
        tile.setWorldObj(world);
        tile.setInitialValuesAsNBT(null, (short) HATCH.id);
        return (CircuitMEPatternBuffer) tile.getMetaTileEntity();
    }

    private static ItemStack pattern(IAEStack<?>[] inputs, ItemStack output) {
        ItemStack result = AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList in = new NBTTagList(), out = new NBTTagList();
        for (IAEStack<?> input : inputs) in.appendTag(input.toNBTGeneric());
        out.appendTag(
            AEItemStack.create(output)
                .toNBTGeneric());
        tag.setTag("in", in);
        tag.setTag("out", out);
        result.setTagCompound(tag);
        return result;
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(new File("."), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("[circuit-supply-qa] PASS " + message);
    }

    private void finish(Throwable error) {
        if (finished) return;
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
            } catch (InterruptedException interrupted) {
                Thread.currentThread()
                    .interrupt();
            }
            FMLCommonHandler.instance()
                .exitJava(error == null ? 0 : 1, true);
        }, "circuit-supply-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }

}
