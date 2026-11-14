package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.gui.GuiQuickEncodingTerminal;
import com.xyp.gtneverreset.client.terminal.WirelessTerminalSorting;
import com.xyp.gtneverreset.compat.ae2.TerminalInventorySort;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.WirelessTerminalGuiType;

import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.IConfigurableObject;
import appeng.client.gui.implementations.GuiCraftingTerm;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.me.ItemRepo;
import appeng.util.InvTweakSortingModule;
import appeng.util.ItemSorters;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
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
                verifySortPerformance("bundled");
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
                verifySortPerformance("custom");
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

    private void verifySortPerformance(String rules) throws Exception {
        Random random = new Random(614);
        Item[] items = { Items.iron_sword, Items.diamond_sword, Items.apple, Items.coal, Items.iron_ingot,
            Items.enchanted_book, Items.potionitem };
        List<IAEStack<?>> entries = new ArrayList<>();
        for (int i = 0; i < 8192; i++) {
            ItemStack stack = new ItemStack(items[i % items.length], 1 + random.nextInt(64), random.nextInt(32));
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("qaUnique", i);
            tag.setByteArray("qaPayload", new byte[1024]);
            stack.setTagCompound(tag);
            if (i % 3 == 0) stack.setStackDisplayName("Named " + random.nextInt(64));
            if (i % 4 == 0) stack.addEnchantment(Enchantment.sharpness, 1 + random.nextInt(5));
            if (i % 8 == 0) stack.addEnchantment(Enchantment.unbreaking, 1 + random.nextInt(3));
            IAEItemStack entry = AEItemStack.create(stack);
            entry.setStackSize(i % 11 == 0 ? (long) Integer.MAX_VALUE + i : stack.stackSize);
            entries.add(entry);
        }
        entries.add(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1000)));
        entries.add(AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 32000)));
        Collections.shuffle(entries, random);
        long baselineNanos = 0;
        long cachedNanos = 0;
        for (int pass = 0; pass < 10; pass++) {
            SortDir direction = pass % 2 == 0 ? SortDir.ASCENDING : SortDir.DESCENDING;
            ItemSorters.setDirection(direction);
            for (IAEStack<?> entry : entries)
                entry.setStackSize(Math.max(1, entry.getStackSize() + random.nextInt(7) - 3));
            List<IAEStack<?>> baseline = new ArrayList<>(entries);
            List<IAEStack<?>> cached = new ArrayList<>(entries);
            long start = System.nanoTime();
            baseline.sort(ItemSorters.CONFIG_BASED_SORT_BY_INV_TWEAKS);
            long baselineTime = System.nanoTime() - start;
            start = System.nanoTime();
            cached.sort(new TerminalInventorySort(direction.sortHint));
            long cachedTime = System.nanoTime() - start;
            for (int i = 0; i < baseline.size(); i++) {
                require(baseline.get(i) == cached.get(i), "sort differs from AE2 at " + rules + "/" + pass + "/" + i);
            }
            if (pass >= 2) {
                baselineNanos += baselineTime;
                cachedNanos += cachedTime;
            }
        }
        // Count conversions through the actual repository hook, not just the standalone comparator.
        int[] conversions = { 0 };
        List<IAEStack<?>> counted = new ArrayList<>();
        for (IAEStack<?> entry : entries) {
            counted.add(
                (IAEStack<?>) Proxy.newProxyInstance(
                    IAEStack.class.getClassLoader(),
                    new Class<?>[] { IAEStack.class },
                    (proxy, method, args) -> {
                        if (method.getName()
                            .equals("getItemStackForNEI")) conversions[0]++;
                        return method.invoke(entry, args);
                    }));
        }
        ItemRepo repo = (ItemRepo) field(GuiMEMonitorable.class, "repo").get(gui);
        var hook = Arrays.stream(ItemRepo.class.getDeclaredMethods())
            .filter(
                method -> method.getName()
                    .contains("gtnr$cacheInventorySortKeys"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("repository sorting hook was not applied"));
        hook.setAccessible(true);
        Comparator<IAEStack<?>> cached = (Comparator<IAEStack<?>>) hook
            .invoke(repo, ItemSorters.CONFIG_BASED_SORT_BY_INV_TWEAKS);
        require(cached instanceof TerminalInventorySort, "repository did not install cached comparator");
        require(
            hook.invoke(repo, ItemSorters.CONFIG_BASED_SORT_BY_NAME) == ItemSorters.CONFIG_BASED_SORT_BY_NAME,
            "unrelated comparator replaced");
        counted.sort(cached);
        require(conversions[0] <= entries.size(), "more than one stack conversion per entry");
        ItemSorters.setDirection(SortDir.ASCENDING);
        var manager = ((IConfigurableObject) gui.inventorySlots).getConfigManager();
        manager.putSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);
        gui.postUpdate(entries);
        repo.updateView();
        require(repo.size() >= entries.size(), "large inventory did not reach terminal view");
        var view = (List<IAEStack<?>>) field(ItemRepo.class, "view").get(repo);
        for (int i = 1; i < view.size(); i++) {
            require(
                ItemSorters.CONFIG_BASED_SORT_BY_INV_TWEAKS.compare(view.get(i - 1), view.get(i)) <= 0,
                "repository hook produced an incorrect ordering");
        }
        System.out.println(
            "[terminal-sorting-perf] rules=" + rules
                + " entries="
                + entries.size()
                + " refreshes=8 baseline_ms="
                + baselineNanos / 1_000_000.0
                + " cached_ms="
                + cachedNanos / 1_000_000.0
                + " conversions="
                + conversions[0]);
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
