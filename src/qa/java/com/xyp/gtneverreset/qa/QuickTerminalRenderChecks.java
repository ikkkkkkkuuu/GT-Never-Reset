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
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
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
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import com.glodblock.github.loader.ItemAndBlockHolder;
import com.xyp.gtneverreset.client.gui.QuickEncodingTerminalGui;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.packet.InterfacePatternTarget;
import com.xyp.gtneverreset.common.terminal.DualTerminalGuiObject;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.GTNRMachineID;

import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.implementations.tiles.IWirelessAccessPoint;
import appeng.api.storage.ITerminalHost;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.IInterfaceTerminalPostUpdate;
import appeng.client.gui.implementations.GuiInterfaceTerminal;
import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.core.AEConfig;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate.PacketEntry;
import appeng.helpers.InventoryAction;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.tile.networking.TileCreativeEnergyCell;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
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

    private static final int SCROLL_MODE = 8;
    private static final int SCROLL_ENTRIES = 48;
    private static final int SLOTS_PER_ROW = 9;
    private static final boolean NEO_ECO_QA = Boolean.getBoolean("gtnr.quick.terminal.neoeco.qa");
    private static final String NEO_ECO_UPLOAD_BUTTON = "cn.dancingsnow.neoecoae.platform.ui.upload.PatternUploadButton";
    private static SuperMTEHatchCraftingInputME provider;
    private volatile List<List<PacketEntry>> updates;
    private volatile Throwable failure;
    private boolean launched, checked, finished;
    private int ticks, networkTicks, step, frames;
    private int mode = Integer.getInteger("gtnr.quick.terminal.render.mode", 0);
    private int bottomScroll;
    private AEBaseGui gui;
    private List<GuiButton> neoEcoButtonsBeforeFiltering;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        if (NEO_ECO_QA) MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void captureUploadButtons(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiPatternTerm) {
            neoEcoButtonsBeforeFiltering = uploadButtons(event.buttonList);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (NEO_ECO_QA || event.phase != TickEvent.Phase.END || checked || !GregTechAPI.sFullLoadFinished) return;
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

    private static Method method(Class<?> type, String name, Class<?>... parameters) throws Exception {
        Method method = type.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (!launched && mc.currentScreen != null
                && (mc.currentScreen instanceof GuiMainMenu || mc.currentScreen.getClass()
                    .getName()
                    .equals("galaxyspace.core.gui.GSGuiMainMenu"))) {
                launched = true;
                mc.launchIntegratedServer(
                    "terminal-" + System.currentTimeMillis(),
                    "Quick Terminal Render QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("Server checks failed", failure);
            if (mc.thePlayer == null) return;
            if (++ticks > 1800) throw new AssertionError("QA timeout");
            if (NEO_ECO_QA) {
                if (++frames >= 8) {
                    frames = 0;
                    checkNeoEcoUploadButtons(mc);
                }
                return;
            }
            if (updates == null) return;
            if (gui == null) {
                AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal = mode == 1 || mode == 5;
                gui = mode >= 6 && mode != SCROLL_MODE
                    ? new GuiInterfaceTerminal(mc.thePlayer.inventory, terminal(mc.thePlayer))
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
                if (mode >= 3 && mode != SCROLL_MODE) {
                    Object embedded = interfaceGui();
                    var search = (MEGuiTextField) field(
                        GuiInterfaceTerminal.class,
                        mode == 4 || mode == 7 ? "searchFieldOutputs" : "searchFieldInputs").get(embedded);
                    search.setText(mode == 4 || mode == 7 ? "stick" : "iron");
                }
            }
            if (++frames < 8) return;
            frames = 0;
            if (mode == SCROLL_MODE) {
                checkScrolledPatterns();
                return;
            }
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
            if (++mode <= SCROLL_MODE) {
                mc.displayGuiScreen(null);
                gui = null;
                step = 0;
            } else finish(null);
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void checkScrolledPatterns() throws Exception {
        Object embedded = interfaceGui();
        Class<?> type = embedded.getClass();
        var scrollbar = (GuiScrollbar) method(AEBaseGui.class, "getScrollBar").invoke(embedded);
        int left = (int) method(type, "left").invoke(embedded);
        int top = (int) method(type, "top").invoke(embedded) + GuiInterfaceTerminal.HEADER_HEIGHT;
        int height = field(GuiInterfaceTerminal.class, "viewHeight").getInt(embedded);
        int slotX = left + GuiInterfaceTerminal.VIEW_LEFT + GuiInterfaceTerminal.VIEW_WIDTH - SLOTS_PER_ROW * 18 + 6;
        int firstRowY = top + 18;
        int bottomRowY = top + height - 12;
        var terminal = (IInterfaceTerminalPostUpdate) gui;
        switch (step++) {
            case 0 -> {
                for (String name : new String[] { "searchFieldInputs", "searchFieldNames" }) {
                    ((MEGuiTextField) field(GuiInterfaceTerminal.class, name).get(embedded)).setText("");
                }
                var search = (MEGuiTextField) field(GuiInterfaceTerminal.class, "searchFieldOutputs").get(embedded);
                search.setText(new ItemStack(Items.stick).getDisplayName());
                var packet = new PacketInterfaceTerminalUpdate();
                for (int id = 1; id <= SCROLL_ENTRIES; id++) {
                    var items = new NBTTagList();
                    items.appendTag(pattern(id).writeToNBT(new NBTTagCompound()));
                    for (int slot = 1; slot < SLOTS_PER_ROW; slot++) items.appendTag(new NBTTagCompound());
                    packet.addNewEntry(id, "QA Scroll " + id, true)
                        .setTerminalVisible(true)
                        .setSupportedStackTypes(provider.getSupportedStackTypes())
                        .setItems(1, SLOTS_PER_ROW, SLOTS_PER_ROW, items);
                }
                terminal.postUpdate(commands(packet), PacketInterfaceTerminalUpdate.CLEAR_ALL_BIT);
            }
            case 1 -> {
                assertPatternTarget(embedded, slotX, firstRowY, 1);
                Method wheel = method(
                    QuickEncodingTerminalGui.class,
                    "mouseWheelEvent",
                    int.class,
                    int.class,
                    int.class);
                // Scroll 624 px so the twenty-first section's slot overlaps the first section's old hit box.
                for (int i = 0; i < 52; i++) wheel.invoke(gui, slotX, firstRowY, -120);
            }
            case 2 -> {
                assertPatternTarget(embedded, slotX, firstRowY, 21);
                screenshot("scroll-target");
                scrollbar.setCurrentScroll(Integer.MAX_VALUE);
                bottomScroll = scrollbar.getCurrentScroll();
            }
            case 3 -> {
                assertPatternTarget(embedded, slotX, bottomRowY, SCROLL_ENTRIES);
                method(type, "freezeSearchResults").invoke(embedded);
                var packet = new PacketInterfaceTerminalUpdate();
                var items = new NBTTagList();
                items.appendTag(new NBTTagCompound());
                packet.addOverwriteEntry(SCROLL_ENTRIES)
                    .setItems(new int[] { 0 }, items);
                terminal.postUpdate(commands(packet), 0);
                if (scrollbar.getCurrentScroll() != bottomScroll) {
                    throw new AssertionError(
                        "Removing the last matching pattern moved the interface scrollbar from " + bottomScroll
                            + " to "
                            + scrollbar.getCurrentScroll());
                }
            }
            case 4 -> {
                assertPatternTarget(embedded, slotX, bottomRowY, SCROLL_ENTRIES);
                screenshot("scroll-after-pattern-removal");
                System.out.println("[quick-terminal-render-qa] PASS scrolled pattern targets and scroll retention");
                finish(null);
            }
        }
    }

    private static void assertPatternTarget(Object embedded, int x, int y, long expectedEntry) throws Exception {
        var target = (InterfacePatternTarget) method(embedded.getClass(), "patternSlotAt", int.class, int.class)
            .invoke(embedded, x, y);
        if (target == null || target.getEntryId() != expectedEntry || target.getSlot() != 0) {
            String actual = target == null ? "none" : target.getEntryId() + " slot " + target.getSlot();
            throw new AssertionError("Expected interface " + expectedEntry + " slot 0, got " + actual);
        }
    }

    private void checkNeoEcoUploadButtons(Minecraft minecraft) throws Exception {
        EntityPlayer player = minecraft.thePlayer;
        neoEcoButtonsBeforeFiltering = null;
        boolean combined = step == 1 || step == 2;
        switch (step) {
            case 0, 3 -> {
                if (!Loader.isModLoaded("neoecoae")) throw new AssertionError("Neo ECO dev JAR did not load");
                if (step == 3) screenshot("neoeco-combined-wireless");
                var item = ItemAndBlockHolder.WIRELESS_PATTERN_TERM;
                var stack = new ItemStack(item);
                stack.setTagCompound(new NBTTagCompound());
                player.inventory.setInventorySlotContents(0, stack);
                var host = (ITerminalHost) item.getGuiObject(stack, player.worldObj, player, 0, 1, 0);
                gui = new GuiPatternTerm(player.inventory, host);
                minecraft.displayGuiScreen(gui);
            }
            case 1 -> {
                screenshot("neoeco-original-wireless");
                gui = new QuickEncodingTerminalGui(player.inventory, terminal(player));
                minecraft.displayGuiScreen(gui);
                field(QuickEncodingTerminalGui.class, "pendingPinRefresh").setBoolean(gui, false);
            }
            case 2 -> {
                gui.setWorldAndResolution(minecraft, gui.width, gui.height);
                field(QuickEncodingTerminalGui.class, "pendingPinRefresh").setBoolean(gui, false);
            }
            case 4 -> {
                finish(null);
                return;
            }
        }
        if (neoEcoButtonsBeforeFiltering == null || neoEcoButtonsBeforeFiltering.size() != 2) {
            throw new AssertionError("Neo ECO did not create both upload buttons at step " + step);
        }
        List<GuiButton> remaining = uploadButtons((List<?>) field(GuiScreen.class, "buttonList").get(gui));
        if (remaining.size() != (combined ? 0 : 2) || neoEcoButtonsBeforeFiltering.stream()
            .anyMatch(button -> button.visible == combined)) {
            throw new AssertionError("Unexpected upload button availability at step " + step);
        }
        System.out.println("[neoeco-terminal-buttons-qa] PASS step=" + step + " buttons=" + remaining.size());
        step++;
    }

    private static List<GuiButton> uploadButtons(List<?> buttons) {
        var upload = new ArrayList<GuiButton>();
        for (Object button : buttons) {
            if (button instanceof GuiButton guiButton && guiButton.getClass()
                .getName()
                .equals(NEO_ECO_UPLOAD_BUTTON)) {
                upload.add(guiButton);
            }
        }
        return upload;
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
