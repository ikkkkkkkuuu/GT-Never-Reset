package com.xyp.gtneverreset.qa;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.text.RichText;
import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.factory.SimpleGuiFactory;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import com.glodblock.github.loader.ItemAndBlockHolder;
import com.xyp.gtneverreset.common.machine.hatch.SuperAdvancedMEInputBus;
import com.xyp.gtneverreset.common.machine.hatch.SuperAdvancedMEInputHatch;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.tile.networking.TileCreativeEnergyCell;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEFluidStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;

/** Opens the real stocking bus/hatch GUIs and verifies mark slots through native click and drag paths. */
@Mod(
    modid = "superstoragemarkingqa",
    name = "Super Storage Marking QA",
    version = "1",
    dependencies = "after:gtneverreset")
public final class SuperStorageMarkingChecks {

    private static final long INITIAL_LAVA = 6_000_000_000L;
    private static final long LAVA_RESERVE = 2_500_000_000L;
    private static final long EXTERNAL_LAVA_EXTRACTION = 250_000_000L;

    private SimpleGuiFactory busFactory;
    private SimpleGuiFactory hatchFactory;
    private SuperAdvancedMEInputBus serverBus;
    private SuperAdvancedMEInputHatch serverHatch;
    private SuperAdvancedMEInputBus clientBus;
    private SuperAdvancedMEInputHatch clientHatch;
    private ModularScreen availabilityScreen;
    private FluidSlot availabilitySlot;
    private String lastAvailabilityTooltip = "";
    private boolean launched, finished, fluidGridConnected;
    private int menuTicks, ticks, stage, serverTicks, frames;
    private volatile int request;
    private volatile int acknowledged;
    private volatile Throwable failure;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        busFactory = GuiFactories.createSimple("gtnr:super_storage_bus_qa", (data, sync, settings) -> {
            SuperAdvancedMEInputBus hatch = data.getWorld().isRemote ? clientBus : serverBus;
            return hatch.buildUI(new PosGuiData(data.getPlayer(), 0, 64, 0), sync, settings);
        });
        hatchFactory = GuiFactories.createSimple("gtnr:super_storage_hatch_qa", (data, sync, settings) -> {
            SuperAdvancedMEInputHatch hatch = data.getWorld().isRemote ? clientHatch : serverHatch;
            return hatch.buildUI(new PosGuiData(data.getPlayer(), 0, 64, 0), sync, settings);
        });
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !GregTechAPI.sFullLoadFinished) return;
        List<?> players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) players.get(0);
        try {
            if (serverBus == null) setup(player);
            if (++serverTicks >= 20 && serverTicks == 20) busFactory.open(player);
            if (request == 1 && acknowledged < 1) {
                setCursor(player, new ItemStack(Items.iron_ingot, 7));
                acknowledged = 1;
            } else if (request == 2 && acknowledged < 2) {
                requireBus(0, Items.iron_ingot);
                setCursor(player, new ItemStack(Items.gold_ingot, 3));
                acknowledged = 2;
            } else if (request == 3 && acknowledged < 3) {
                requireBus(1, Items.gold_ingot);
                setCursor(player, new ItemStack(Items.redstone, 5));
                acknowledged = 3;
            } else if (request == 4 && acknowledged < 4) {
                requireBus(2, Items.redstone);
                setCursor(player, new ItemStack(Items.dye, 9));
                acknowledged = 4;
            } else if (request == 5 && acknowledged < 5) {
                requireBus(3, Items.dye);
                setCursor(player, null);
                acknowledged = 5;
            } else if (request == 6 && acknowledged < 6) {
                require(serverBus.getSlotConfig(0) == null, "empty-hand click clears the item mark");
                hatchFactory.open(player);
                acknowledged = 6;
            } else if (request == 7 && acknowledged < 7) {
                setCursor(player, new ItemStack(Items.water_bucket));
                acknowledged = 7;
            } else if (request == 8 && acknowledged < 8) {
                requireFluid(0, "water");
                setCursor(player, new ItemStack(Items.lava_bucket));
                acknowledged = 8;
            } else if (request == 9 && acknowledged < 9) {
                requireFluid(1, "lava");
                setCursor(player, null);
                acknowledged = 9;
            } else if (request == 10 && acknowledged < 10) {
                if (!fluidGridConnected) {
                    require(serverHatch.getSlotConfig(0) == null, "empty-hand click clears the fluid mark");
                    connectFluidGrid(player.worldObj);
                    fluidGridConnected = true;
                }
                if (!serverHatch.isAllowedToWork()) return;
                var lava = AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 1));
                lava.setStackSize(INITIAL_LAVA);
                var storage = serverHatch.getProxy()
                    .getStorage()
                    .getFluidInventory();
                require(
                    storage.injectItems(lava, Actionable.MODULATE, new BaseActionSource()) == null,
                    "ME stores six billion mB of marked lava");
                serverHatch.getPolicy(1).reserve = LAVA_RESERVE;
                serverHatch.setLimitedMode(true);
                serverHatch.updateInformationSlot(1);
                acknowledged = 10;
            } else if (request == 11 && acknowledged < 11) {
                var lava = AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 1));
                lava.setStackSize(EXTERNAL_LAVA_EXTRACTION);
                var extracted = serverHatch.getProxy()
                    .getStorage()
                    .getFluidInventory()
                    .extractItems(lava, Actionable.MODULATE, new BaseActionSource());
                require(
                    extracted != null && extracted.getStackSize() == EXTERNAL_LAVA_EXTRACTION,
                    "another ME consumer removes 250 million mB of lava");
                serverHatch.updateInformationSlot(1);
                acknowledged = 11;
            }
        } catch (Throwable error) {
            failure = error;
            finish(error);
        }
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (!launched && mc.theWorld == null && mc.currentScreen != null && ++menuTicks > 100) {
                launched = true;
                mc.launchIntegratedServer(
                    "storage-marking-" + System.currentTimeMillis(),
                    "Super Storage Marking QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("Server check failed", failure);
            if (mc.thePlayer == null || mc.theWorld == null) return;
            if (clientBus == null) {
                clientBus = bus(mc.theWorld);
                clientHatch = hatch(mc.theWorld);
            }
            if (++ticks > 2400) {
                throw new AssertionError(
                    "Marking QA timeout at stage " + stage
                        + ", server acknowledgement "
                        + acknowledged
                        + ", availability tooltip "
                        + lastAvailabilityTooltip);
            }
            ModularScreen screen = ModularScreen.getCurrent();
            if (screen == null) return;
            if (stage >= 9) return;
            if (stage == 5) {
                if (acknowledged >= 6) stage = 6;
                return;
            }
            if (stage <= 4) {
                List<PhantomItemSlot> slots = widgets(screen.getMainPanel(), PhantomItemSlot.class);
                if (slots.size() < 4 || ++frames < 10) return;
                frames = 0;
                if (stage == 0) {
                    if (acknowledged < 1) {
                        request = 1;
                        return;
                    }
                    slots.get(0)
                        .onMousePressed(0);
                    request = 2;
                    stage = 1;
                } else if (stage == 1) {
                    if (acknowledged < 2) return;
                    slots.get(1)
                        .handleDragAndDrop(new ItemStack(Items.gold_ingot), 0);
                    request = 3;
                    stage = 2;
                } else if (stage == 2) {
                    if (acknowledged < 3) return;
                    slots.get(2)
                        .onMousePressed(1);
                    request = 4;
                    stage = 3;
                } else if (stage == 3) {
                    if (acknowledged < 4) return;
                    slots.get(3)
                        .onMousePressed(2);
                    request = 5;
                    stage = 4;
                } else {
                    if (acknowledged < 5) return;
                    slots.get(0)
                        .onMousePressed(0);
                    request = 6;
                    stage = 5;
                }
                return;
            }
            List<FluidSlot> slots = widgets(screen.getMainPanel(), FluidSlot.class);
            if (slots.size() < 2 || ++frames < 10) return;
            frames = 0;
            if (stage == 6) {
                if (acknowledged < 7) {
                    request = 7;
                    return;
                }
                slots.get(0)
                    .onMousePressed(0);
                request = 8;
                stage = 7;
            } else if (stage == 7) {
                if (acknowledged < 8) return;
                slots.get(1)
                    .handleDragAndDrop(new ItemStack(Items.lava_bucket), 0);
                request = 9;
                stage = 8;
            } else if (stage == 8) {
                if (acknowledged < 9) return;
                slots.get(0)
                    .onMousePressed(0);
                request = 10;
                stage = 9;
            }
        } catch (Throwable error) {
            failure = error;
            finish(error);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || stage < 9 || acknowledged < stage + 1) return;
        try {
            var screen = ModularScreen.getCurrent();
            if (screen == null) return;
            if (availabilitySlot == null) {
                var slots = widgets(screen.getMainPanel(), FluidSlot.class);
                int stockIndex = SuperAdvancedMEInputHatch.SLOT_COUNT + 1;
                require(slots.size() > stockIndex, "fluid GUI contains the right availability grid");
                availabilitySlot = slots.get(stockIndex);
                require(
                    availabilitySlot.getArea()
                        .x()
                        > slots.get(1)
                            .getArea()
                            .x(),
                    "availability slot is right of the marked slot");
                availabilityScreen = screen;
            }
            if (screen != availabilityScreen)
                throw new AssertionError("fluid GUI reopened before availability updated");
            var lava = availabilitySlot.getFluidStack();
            if (lava == null || lava.getFluid() != FluidRegistry.LAVA) return;

            long expected = INITIAL_LAVA - LAVA_RESERVE - (stage == 9 ? 0 : EXTERNAL_LAVA_EXTRACTION);
            var tooltip = availabilitySlot.getTooltip();
            tooltip.buildTooltip();
            var lines = ((RichText) tooltip.getRichText()).getAsStrings();
            lastAvailabilityTooltip = lines.toString();
            String formatted = availabilitySlot.formatFluidTooltipAmount(expected);
            if (lines.stream()
                .noneMatch(line -> line.contains(formatted))) return;
            require(true, "right availability tooltip shows " + formatted + " for marked lava");
            require(
                availabilitySlot.getBaseUnitAmount(lava.amount)
                    == expected * availabilitySlot.getBaseUnitSiPrefix().factor,
                "right availability badge uses the full synchronized amount " + formatted);
            if (stage == 9) {
                captureAvailability(
                    screen,
                    availabilitySlot,
                    "fluid-availability-3500000000.png",
                    event.renderTickTime);
                stage = 10;
                request = 11;
            } else {
                require(screen == availabilityScreen, "long availability updates without reopening the fluid GUI");
                captureAvailability(
                    screen,
                    availabilitySlot,
                    "fluid-availability-3250000000.png",
                    event.renderTickTime);
                finish(null);
            }
        } catch (Throwable error) {
            finish(error);
        }
    }

    private static void captureAvailability(ModularScreen screen, FluidSlot slot, String name, float partialTicks) {
        var mc = Minecraft.getMinecraft();
        var context = screen.getContext();
        int previousX = context.getAbsMouseX(), previousY = context.getAbsMouseY();
        var area = slot.getArea();
        try {
            mc.entityRenderer.setupOverlayRendering();
            context.updateState(area.x(0.5f), area.y(0.5f), partialTicks);
            slot.getTooltip()
                .draw(context);
            ScreenShotHelper
                .saveScreenshot(new File("."), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        } finally {
            context.updateState(previousX, previousY, partialTicks);
        }
    }

    private void connectFluidGrid(World world) throws Exception {
        var power = new TileCreativeEnergyCell();
        power.setWorldObj(world);
        power.xCoord = 1;
        var drive = new TileDrive();
        drive.setWorldObj(world);
        drive.xCoord = 2;
        drive.setInventorySlotContents(0, new ItemStack(ItemAndBlockHolder.SINGULARITY_CELL));
        var powerProxy = power.getProxy();
        var driveProxy = drive.getProxy();
        var hatchProxy = serverHatch.getProxy();
        powerProxy.onReady();
        driveProxy.onReady();
        hatchProxy.onReady();
        AEApi.instance()
            .createGridConnection(hatchProxy.getNode(), powerProxy.getNode());
        AEApi.instance()
            .createGridConnection(driveProxy.getNode(), powerProxy.getNode());
        serverHatch.getBaseMetaTileEntity()
            .enableWorking();
    }

    private void setup(EntityPlayerMP player) {
        serverBus = bus(player.worldObj);
        serverHatch = hatch(player.worldObj);
        require(serverBus != null && serverHatch != null, "stocking bus and hatch exist");
    }

    private static SuperAdvancedMEInputBus bus(World world) {
        BaseMetaTileEntity tile = new BaseMetaTileEntity();
        tile.setWorldObj(world);
        tile.setInitialValuesAsNBT(null, (short) GTNRMachineID.SuperAdvancedMEInputBus.id);
        return (SuperAdvancedMEInputBus) tile.getMetaTileEntity();
    }

    private static SuperAdvancedMEInputHatch hatch(World world) {
        BaseMetaTileEntity tile = new BaseMetaTileEntity();
        tile.setWorldObj(world);
        tile.setInitialValuesAsNBT(null, (short) GTNRMachineID.SuperAdvancedMEInputHatch.id);
        return (SuperAdvancedMEInputHatch) tile.getMetaTileEntity();
    }

    private static void setCursor(EntityPlayerMP player, ItemStack stack) {
        player.inventory.setItemStack(stack);
    }

    private void requireBus(int index, net.minecraft.item.Item item) {
        ItemStack mark = serverBus.getSlotConfig(index);
        require(mark != null && mark.getItem() == item && mark.stackSize == 1, "item mark " + index);
    }

    private void requireFluid(int index, String fluidName) {
        var mark = serverHatch.getSlotConfig(index);
        require(
            mark != null && mark.getFluid() != null
                && fluidName.equals(
                    mark.getFluid()
                        .getName()),
            "fluid mark " + index + " contains " + fluidName);
    }

    private static <T extends IWidget> List<T> widgets(IWidget root, Class<T> type) {
        List<T> found = new ArrayList<>();
        collect(root, type, found);
        return found;
    }

    private static <T extends IWidget> void collect(IWidget widget, Class<T> type, List<T> found) {
        if (type.isInstance(widget)) found.add(type.cast(widget));
        for (IWidget child : widget.getChildren()) collect(child, type, found);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("[super-storage-marking-qa] PASS " + message);
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
        }, "super-storage-marking-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }
}
