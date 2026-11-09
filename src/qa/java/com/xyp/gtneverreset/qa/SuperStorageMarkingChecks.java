package com.xyp.gtneverreset.qa;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.factory.SimpleGuiFactory;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import com.xyp.gtneverreset.common.machine.hatch.SuperAdvancedMEInputBus;
import com.xyp.gtneverreset.common.machine.hatch.SuperAdvancedMEInputHatch;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;

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

    private SimpleGuiFactory busFactory;
    private SimpleGuiFactory hatchFactory;
    private SuperAdvancedMEInputBus serverBus;
    private SuperAdvancedMEInputHatch serverHatch;
    private SuperAdvancedMEInputBus clientBus;
    private SuperAdvancedMEInputHatch clientHatch;
    private boolean launched, finished;
    private int ticks, stage, serverTicks, frames;
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
                require(serverHatch.getSlotConfig(0) == null, "empty-hand click clears the fluid mark");
                acknowledged = 10;
                finish(null);
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
            if (!launched && mc.currentScreen instanceof GuiMainMenu) {
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
            if (++ticks > 2400) throw new AssertionError("Marking QA timeout at stage " + stage);
            ModularScreen screen = ModularScreen.getCurrent();
            if (screen == null) return;
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
            } else if (acknowledged >= 10) {
                finish(null);
            }
        } catch (Throwable error) {
            failure = error;
            finish(error);
        }
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
