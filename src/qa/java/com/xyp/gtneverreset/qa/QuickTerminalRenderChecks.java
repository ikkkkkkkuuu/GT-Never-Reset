package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.xyp.gtneverreset.client.gui.QuickEncodingTerminalGui;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.terminal.DualTerminalGuiObject;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;

import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.implementations.tiles.IWirelessAccessPoint;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.IInterfaceTerminalPostUpdate;
import appeng.client.gui.implementations.GuiInterfaceTerminal;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.core.AEConfig;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate.PacketEntry;
import appeng.helpers.InventoryAction;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.tile.networking.TileCreativeEnergyCell;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;

@Mod(
    modid = "quickterminalrenderqa",
    name = "Quick Terminal Render QA",
    version = "1",
    dependencies = "after:gtneverreset")
public final class QuickTerminalRenderChecks {

    private static SuperMTEHatchCraftingInputME provider;
    private volatile List<List<PacketEntry>> updates;
    private volatile Throwable failure;
    private boolean launched, checked, finished;
    private int ticks, networkTicks, step, frames;
    private int mode = Integer.getInteger("gtnr.quick.terminal.render.mode", 0);
    private AEBaseGui gui;

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
                var tile = new BaseMetaTileEntity();
                tile.setWorldObj(player.worldObj);
                tile.xCoord = (int) player.posX;
                tile.yCoord = 100;
                tile.zCoord = (int) player.posZ;
                tile.setInitialValuesAsNBT(null, (short) GTNRMachineID.SuperCraftingInputME.id);
                provider = (SuperMTEHatchCraftingInputME) tile.getMetaTileEntity();
                provider.setCustomName("QA Assembly");
                if (!provider.shouldDisplay()) provider.toggleTerminalVisibility();
                provider.getProxy()
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
                        provider.getProxy()
                            .getNode(),
                        power.getProxy()
                            .getNode());
                return;
            }
            if (!provider.getProxy()
                .isActive()) {
                if (++networkTicks > 200) throw new AssertionError("ME network did not become active");
                return;
            }
            checked = true;
            var container = new ContainerInterfaceTerminal(player.inventory, terminal(player));
            Map<?, ?> tracked = (Map<?, ?>) field(ContainerInterfaceTerminal.class, "tracked").get(container);
            Object tracker = tracked.get(provider);
            if (tracker == null) throw new AssertionError("assembly missing from interface terminal");
            long id = field(tracker.getClass(), "id").getLong(tracker);
            var packets = new ArrayList<List<PacketEntry>>();
            captureDirty(container, packets);
            for (int slot : new int[] { 0, 8, 17, 26, 35, 35, 26, 17, 8, 0 }) {
                player.inventory.setItemStack(
                    provider.getPatterns()
                        .getStackInSlot(slot) == null ? pattern(slot + 1) : null);
                boolean inserting = player.inventory.getItemStack() != null;
                container.doAction(player, InventoryAction.PICKUP_OR_SET_DOWN, slot, id);
                if ((provider.getPatterns()
                    .getStackInSlot(slot) != null) != inserting) {
                    throw new AssertionError("assembly pattern click failed at slot " + slot);
                }
                captureDirty(container, packets);
                Method update = ContainerInterfaceTerminal.class.getDeclaredMethod("updateList");
                update.setAccessible(true);
                var packet = (PacketInterfaceTerminalUpdate) update.invoke(container);
                if (packet != null) packets.add(commands(packet));
                System.out.println("[quick-terminal-render-qa] server slot=" + slot + " rows=" + provider.rows());
            }
            player.inventory.setItemStack(null);
            updates = packets;
        } catch (Throwable error) {
            checked = true;
            failure = error;
        }
    }

    private static void captureDirty(ContainerInterfaceTerminal container, List<List<PacketEntry>> packets)
        throws Exception {
        var packet = (PacketInterfaceTerminalUpdate) field(ContainerInterfaceTerminal.class, "dirty").get(container);
        packets.add(commands(packet));
        field(ContainerInterfaceTerminal.class, "dirty").set(container, new PacketInterfaceTerminalUpdate());
        field(ContainerInterfaceTerminal.class, "isDirty").setBoolean(container, false);
    }

    @SuppressWarnings("unchecked")
    private static List<PacketEntry> commands(PacketInterfaceTerminalUpdate packet) throws Exception {
        return new ArrayList<>((List<PacketEntry>) field(PacketInterfaceTerminalUpdate.class, "commands").get(packet));
    }

    private static ItemStack pattern(int count) {
        ItemStack pattern = AEApi.instance()
            .definitions()
            .items()
            .encodedPattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList inputs = new NBTTagList();
        inputs.appendTag(new ItemStack(Items.iron_ingot).writeToNBT(new NBTTagCompound()));
        NBTTagList outputs = new NBTTagList();
        outputs.appendTag(new ItemStack(Items.stick, count).writeToNBT(new NBTTagCompound()));
        tag.setTag("in", inputs);
        tag.setTag("out", outputs);
        tag.setBoolean("crafting", false);
        pattern.setTagCompound(tag);
        return pattern;
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
            var accessPoint = Proxy.newProxyInstance(
                IWirelessAccessPoint.class.getClassLoader(),
                new Class<?>[] { IWirelessAccessPoint.class },
                (proxy, method, args) -> provider.getProxy()
                    .getNode());
            field(WirelessTerminalGuiObject.class, "myWap").set(terminal, accessPoint);
        }
        return terminal;
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
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
                    "Quick Terminal Render QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("Server checks failed", failure);
            if (mc.thePlayer == null) return;
            if (++ticks > 1800) throw new AssertionError("QA timeout");
            if (updates == null) return;
            if (gui == null) {
                AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal = mode == 1 || mode == 5;
                gui = mode >= 6 ? new GuiInterfaceTerminal(mc.thePlayer.inventory, terminal(mc.thePlayer))
                    : new QuickEncodingTerminalGui(mc.thePlayer.inventory, terminal(mc.thePlayer));
                mc.displayGuiScreen(gui);
                if (gui instanceof QuickEncodingTerminalGui) {
                    field(QuickEncodingTerminalGui.class, "pendingPinRefresh").setBoolean(gui, false);
                }
                if (mode == 2) {
                    Object embedded = interfaceGui();
                    Method search = embedded.getClass()
                        .getDeclaredMethod("setNameSearchText", String.class);
                    search.setAccessible(true);
                    search.invoke(embedded, "QA");
                }
                if (mode >= 3) {
                    Object embedded = interfaceGui();
                    var search = (MEGuiTextField) field(
                        GuiInterfaceTerminal.class,
                        mode == 4 || mode == 7 ? "searchFieldOutputs" : "searchFieldInputs").get(embedded);
                    search.setText(mode == 4 || mode == 7 ? "stick" : "iron");
                }
            }
            if (++frames < 8) return;
            frames = 0;
            if (step == 10) screenshot("expanded-" + mode);
            if (step < updates.size()) {
                Object embedded = interfaceGui();
                if (mode >= 2 && step > 0 && gui instanceof QuickEncodingTerminalGui) {
                    Method freeze = embedded.getClass()
                        .getDeclaredMethod("freezeSearchResults");
                    freeze.setAccessible(true);
                    freeze.invoke(embedded);
                }
                System.out.println("[quick-terminal-render-qa] client mode=" + mode + " step=" + step);
                ((IInterfaceTerminalPostUpdate) gui)
                    .postUpdate(updates.get(step), step == 0 ? PacketInterfaceTerminalUpdate.CLEAR_ALL_BIT : 0);
                step++;
                return;
            }
            screenshot("empty-" + mode);
            System.out.println("[quick-terminal-render-qa] PASS render sequence mode=" + mode);
            if (++mode < 8) {
                mc.displayGuiScreen(null);
                gui = null;
                step = 0;
            } else finish(null);
        } catch (Throwable error) {
            finish(error);
        }
    }

    private Object interfaceGui() throws Exception {
        return gui instanceof QuickEncodingTerminalGui
            ? field(QuickEncodingTerminalGui.class, "interfaceTerminal").get(gui)
            : gui;
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(
            new File("."),
            "quick-terminal-render-" + name + ".png",
            mc.displayWidth,
            mc.displayHeight,
            mc.getFramebuffer());
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
        }, "quick-terminal-render-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }
}
