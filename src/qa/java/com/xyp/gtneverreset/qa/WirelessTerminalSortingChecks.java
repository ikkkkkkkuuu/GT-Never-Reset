package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.gui.GuiQuickEncodingTerminal;
import com.xyp.gtneverreset.client.terminal.WirelessTerminalSorting;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.WirelessTerminalGuiType;

import appeng.api.config.Settings;
import appeng.api.config.SortOrder;
import appeng.api.util.IConfigurableObject;
import appeng.client.gui.implementations.GuiCraftingTerm;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.me.ItemRepo;
import appeng.util.InvTweakSortingModule;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;

@Mod(
    modid = "wirelessterminalsortingqa",
    name = "Wireless Terminal Sorting QA",
    version = "1",
    dependencies = "after:gtneverreset")
public final class WirelessTerminalSortingChecks {

    private static final byte[] CUSTOM = ("<stuff><apple id=\"minecraft:apple\"/>"
        + "<sword id=\"minecraft:iron_sword\"/></stuff>\n").getBytes(StandardCharsets.UTF_8);
    private boolean launched, finished;
    private int ticks, phase, frames;
    private GuiMEMonitorable gui;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (!launched && mc.currentScreen instanceof GuiMainMenu) {
                launched = true;
                require(!InvTweakSortingModule.CONFIG_TREE_FILE.exists(), "test must start without a tree file");
                mc.launchIntegratedServer(
                    "sorting-" + System.currentTimeMillis(),
                    "Terminal Sorting QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (mc.thePlayer == null || !GregTechAPI.sFullLoadFinished) return;
            if (++ticks > 1200) throw new AssertionError("QA timeout");
            if (gui == null) {
                verify(phase);
                frames = 0;
            }
            if (++frames < 12) return;
            ScreenShotHelper.saveScreenshot(
                new File("."),
                "terminal-sorting-" + phase + ".png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            System.out.println("[terminal-sorting-qa] PASS phase=" + phase);
            mc.displayGuiScreen(null);
            gui = null;
            if (++phase == 6) finish(null);
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void verify(int phase) throws Exception {
        Path tree = InvTweakSortingModule.CONFIG_TREE_FILE.toPath();
        switch (phase) {
            case 0 -> {
                require(!InvTweakSortingModule.isLoaded(), "AE2 unexpectedly has a tree before opening the terminal");
                open(WirelessTerminalGuiType.WirelessDualInterfaceTerminal);
                require(InvTweakSortingModule.isLoaded(), "bundled rules did not load");
                try (var input = WirelessTerminalSorting.class
                    .getResourceAsStream("/assets/gtneverreset/terminal/InvTweaksTree.txt")) {
                    require(
                        input != null && Arrays.equals(input.readAllBytes(), Files.readAllBytes(tree)),
                        "installed file differs from the bundled tree");
                }
                require(compare() < 0, "bundled equipment category should precede food");
                showInventorySorting();
            }
            case 1 -> {
                Files.write(tree, CUSTOM);
                FileTime stamp = FileTime.fromMillis(1234567890000L);
                Files.setLastModifiedTime(tree, stamp);
                reset(true);
                open(WirelessTerminalGuiType.WirelessDualInterfaceTerminal);
                require(Arrays.equals(CUSTOM, Files.readAllBytes(tree)), "custom file was replaced");
                require(stamp.equals(Files.getLastModifiedTime(tree)), "custom file was rewritten");
                require(compare() > 0, "custom ordering was not applied");
                showInventorySorting();
            }
            case 2 -> {
                Object loaded = field(InvTweakSortingModule.class, "tree").get(null);
                Files.delete(tree);
                reset(false);
                open(WirelessTerminalGuiType.WirelessCraftingTerminal);
                require(gui instanceof GuiCraftingTerm, "crafting mode did not open");
                require(!Files.exists(tree), "already loaded rules caused a new file to be written");
                require(
                    loaded == field(InvTweakSortingModule.class, "tree").get(null),
                    "already loaded rules replaced");
                require(compare() > 0, "loaded custom ordering changed");
                showInventorySorting();
            }
            case 3 -> {
                byte[] invalid = "not an XML sorting tree".getBytes(StandardCharsets.UTF_8);
                Files.write(tree, invalid);
                reset(true);
                open(WirelessTerminalGuiType.WirelessDualInterfaceTerminal);
                require(Arrays.equals(invalid, Files.readAllBytes(tree)), "invalid user file was overwritten");
                require(!InvTweakSortingModule.isLoaded(), "invalid rules enabled inventory sorting");
                require(
                    Platform.rotateEnum(SortOrder.MOD, false, Settings.SORT_BY.getPossibleValues()) == SortOrder.NAME,
                    "unavailable sorting mode should still be skipped");
            }
            case 4 -> {
                Files.delete(tree);
                Files.createDirectory(tree);
                reset(true);
                open(WirelessTerminalGuiType.WirelessDualInterfaceTerminal);
                require(Files.isDirectory(tree), "non-file path was overwritten");
                require(!InvTweakSortingModule.isLoaded(), "unreadable rules enabled inventory sorting");
                Files.delete(tree);
            }
            case 5 -> {
                reset(true);
                open(WirelessTerminalGuiType.WirelessCraftingTerminal);
                require(InvTweakSortingModule.isLoaded(), "crafting-first open did not install bundled rules");
                require(compare() < 0, "crafting mode did not use bundled order");
                showInventorySorting();
                byte[] before = Files.readAllBytes(tree);
                Object loaded = field(InvTweakSortingModule.class, "tree").get(null);
                WirelessTerminalSorting.initialize();
                require(Arrays.equals(before, Files.readAllBytes(tree)), "repeat initialization rewrote rules");
                require(
                    loaded == field(InvTweakSortingModule.class, "tree").get(null),
                    "repeat initialization reloaded rules");
                try (var paths = Files.list(tree.getParent())) {
                    require(
                        paths.noneMatch(
                            path -> path.getFileName()
                                .toString()
                                .startsWith("gtnr-invtweaks-")),
                        "temporary files leaked");
                }
            }
            default -> throw new AssertionError("unexpected phase " + phase);
        }
    }

    private void open(WirelessTerminalGuiType type) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        ItemStack terminal = GTNRItemList.WirelessDualInterfaceTerminal.get(1);
        terminal.setTagCompound(new NBTTagCompound());
        mc.thePlayer.inventory.setInventorySlotContents(0, terminal);
        gui = (GuiMEMonitorable) GTNeverReset.proxy.createTerminalGui(type.ordinal(), mc.thePlayer, mc.theWorld, 0);
        require(gui != null, "terminal GUI was not created");
        mc.displayGuiScreen(gui);
        if (gui instanceof GuiQuickEncodingTerminal) {
            field(GuiQuickEncodingTerminal.class, "pendingPinRefresh").setBoolean(gui, false);
        }
    }

    private void showInventorySorting() throws Exception {
        require(
            Platform.rotateEnum(SortOrder.MOD, false, Settings.SORT_BY.getPossibleValues()) == SortOrder.INVTWEAKS,
            "forward sort cycling skips inventory sorting");
        require(
            Platform.rotateEnum(SortOrder.NAME, true, Settings.SORT_BY.getPossibleValues()) == SortOrder.INVTWEAKS,
            "backward sort cycling skips inventory sorting");
        ((IConfigurableObject) gui.inventorySlots).getConfigManager()
            .putSetting(Settings.SORT_BY, SortOrder.INVTWEAKS);
        GuiImgButton button = (GuiImgButton) field(GuiMEMonitorable.class, "SortByBox").get(gui);
        require(button.getCurrentValue() == SortOrder.INVTWEAKS, "native sort button did not update");
        gui.postUpdate(
            List.of(
                AEItemStack.create(new ItemStack(Items.apple, 32)),
                AEItemStack.create(new ItemStack(Items.iron_sword)),
                AEItemStack.create(new ItemStack(Items.coal, 64))));
        ItemRepo repo = (ItemRepo) field(GuiMEMonitorable.class, "repo").get(gui);
        repo.updateView();
        require(repo.size() == 3, "item list did not render test stacks");
    }

    private static int compare() {
        return InvTweakSortingModule.compareItems(new ItemStack(Items.iron_sword), new ItemStack(Items.apple));
    }

    private static void reset(boolean clearTree) throws Exception {
        field(WirelessTerminalSorting.class, "attempted").setBoolean(null, false);
        if (clearTree) field(InvTweakSortingModule.class, "tree").set(null, null);
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
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
        }, "terminal-sorting-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }
}
