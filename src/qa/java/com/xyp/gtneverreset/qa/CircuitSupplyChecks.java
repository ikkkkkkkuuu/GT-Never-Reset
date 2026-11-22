package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
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
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.glodblock.github.loader.ItemAndBlockHolder;
import com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.storage.data.IAEStack;
import appeng.container.ContainerNull;
import appeng.tile.networking.TileCreativeEnergyCell;
import appeng.tile.storage.TileDrive;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.util.GTRecipe;

@Mod(modid = "circuitsupplyqa", name = "Circuit Supply QA", version = "1", dependencies = "after:gtneverreset")
public final class CircuitSupplyChecks {

    private static final boolean CROSS_RECIPE = Boolean.getBoolean("gtnr.cross.recipe.supplies.qa");
    private static final GTNRMachineID HATCH = CROSS_RECIPE ? GTNRMachineID.CrossRecipeMEPatternBuffer
        : GTNRMachineID.CircuitMEPatternBuffer;

    private CircuitMEPatternBuffer guiHatch;
    private int waitForTile;
    private volatile boolean edited;
    private CircuitMEPatternBuffer serverHatch;
    private boolean launched, verified, opened, finished;
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
                edited = guiHatch.getSharedSupplies()
                    .target(16)
                    .getStackSize() == 1750;
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
                drive.setInventorySlotContents(1, new ItemStack(ItemAndBlockHolder.CELL1K));
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
            var orders = serverHatch.inventories();
            orders.next();
            orders.next()
                .getItemInputs();
            require(
                supplies.itemInputs()
                    .get(0).stackSize == 8
                    && supplies.fluidInputs()
                        .get(0).amount == 1000,
                "recipe check replenishes both channels with the timer disabled");
            supplies.setInterval(5);
            int x = (int) player.posX + 2, y = (int) player.posY, z = (int) player.posZ;
            player.worldObj.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
            var tile = (BaseMetaTileEntity) player.worldObj.getTileEntity(x, y, z);
            tile.setInitialValuesAsNBT(null, (short) HATCH.id);
            guiHatch = (CircuitMEPatternBuffer) tile.getMetaTileEntity();
            var state = new NBTTagCompound();
            supplies.write(state);
            guiHatch.getSharedSupplies()
                .read(state);
            player.worldObj.markBlockForUpdate(x, y, z);
        } catch (Throwable error) {
            failure = error;
        }
    }

    private void verify(EntityPlayerMP player) throws Exception {
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
        supplies.setInterval(1);
        for (int tick = 1; tick <= 20; tick++) serverHatch.onPostTick(serverHatch.getBaseMetaTileEntity(), tick);
        require(
            supplies.itemInputs()
                .get(0).stackSize == 8
                && supplies.fluidInputs()
                    .get(0).amount == 1000,
            "timer filled both targets");
        require(
            storage.getItemInventory()
                .extractItems(iron, Actionable.SIMULATE, source)
                .getStackSize() == 4
                && storage.getFluidInventory()
                    .extractItems(water, Actionable.SIMULATE, source)
                    .getStackSize() == 1500,
            "ME debited exactly the stock received");
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
        supplies.setInterval(0);
        supplies.setOnRecipeCheck(true);
        var saved = new NBTTagCompound();
        serverHatch.saveNBTData(saved);
        var restored = hatch(player.worldObj);
        restored.loadNBTData(saved);
        require(
            restored.getSharedSupplies()
                .interval() == 0 && restored.getSharedSupplies()
                    .onRecipeCheck()
                && restored.getSharedSupplies()
                    .itemInputs()
                    .get(0).stackSize == 6
                && restored.getSharedSupplies()
                    .fluidInputs()
                    .get(0).amount == 750,
            "save/load preserves policy and exact remainder");
        serverHatch.setItemNBT(saved);
        restored.loadNBTData(saved);
        require(
            restored.getSharedSupplies()
                .itemInputs()
                .get(0).stackSize == 6,
            "dropped hatch retains reserve");
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
                shot("circuit-supplies.png");
                fluid.onMousePressed(1);
            } else if (stage == 2) {
                if (!screen.isPanelOpen("supply_limit_16")) return;
                shot("circuit-supplies-fluid-limit.png");
                var field = (TextFieldWidget) find(panel, "supply_limit");
                field.setText("1750");
                field.onRemoveFocus(screen.getContext());
            } else {
                if (!edited) return;
                finish(null);
            }
            stage++;
            frames = 0;
        } catch (Throwable error) {
            finish(error);
        }
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
