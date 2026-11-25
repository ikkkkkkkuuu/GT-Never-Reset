package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtneverreset.api.mixinHelper.CraftingCompletionRepoAccess;
import com.xyp.gtneverreset.client.gui.QuickEncodingTerminalGui;
import com.xyp.gtneverreset.client.terminal.CraftingCompletionClient;
import com.xyp.gtneverreset.common.crafting.CraftingCompletionService;
import com.xyp.gtneverreset.common.gui.container.QuickEncodingTerminalContainer;
import com.xyp.gtneverreset.common.terminal.WirelessTerminalSession;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.enums.WirelessTerminalGuiType;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.definitions.ITileDefinition;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.client.gui.implementations.GuiCraftingTerm;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.slots.VirtualMESlot;
import appeng.tile.misc.TileInterface;
import appeng.tile.networking.TileWireless;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;

@Mod(
    modid = "gtnrcraftingcompletionqa",
    name = "GTNR Crafting Completion Rows QA",
    version = "1",
    dependencies = "after:gtneverreset")
public final class CraftingCompletionChecks {

    private static final int TERMINAL_SLOT = 0;

    private final boolean donor = Boolean.getBoolean("gtnr.crafting.completion.donor.qa");
    private volatile Phase phase = Phase.PREPARE;
    private volatile Throwable failure;
    private TileWireless access, otherAccess;
    private TileInterface provider;
    private TileEntityChest machineInput;
    private IMEMonitor<IAEItemStack> storage;
    private IGrid grid, otherGrid;
    private ICraftingGrid crafting;
    private ICraftingCPU cpu;
    private Future<ICraftingJob> plan;
    private ICraftingLink link;
    private ItemStack realOutput;
    private List<ItemStack> expectedRows;
    private QuickEncodingTerminalGui quickScreen;
    private GuiCraftingTerm normalScreen;
    private boolean launched, finished, returned, expiryChecked;
    private int networkTicks, clientTicks, stableTicks;
    private long firstVisibleAt;
    private Method donorItems, donorRecord, donorPrefixSlots;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || failure != null || !GregTechAPI.sFullLoadFinished) return;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            switch (phase) {
                case PREPARE -> prepareNetworks(player);
                case ACTUAL_JOB -> completeActualJob(player);
                case PAD_RESULTS -> padResults(player);
                case FRESH_RESULT -> {
                    recordFixtureResult(player, new ItemStack(Items.leather));
                    phase = Phase.FRESH_VISIBLE;
                }
                case CLOSED -> {
                    if (player.openContainer != player.inventoryContainer) return;
                    recordFixtureResult(player, new ItemStack(Items.bone));
                    phase = Phase.NORMAL_OPEN;
                    WirelessTerminalSession
                        .open(player, TERMINAL_SLOT, WirelessTerminalGuiType.WirelessCraftingTerminal);
                }
                case NORMAL_RESULT -> {
                    recordFixtureResult(player, new ItemStack(Items.feather));
                    phase = Phase.NORMAL_VISIBLE;
                }
                default -> {}
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
            if (!launched && mc.currentScreen != null
                && (mc.currentScreen instanceof GuiMainMenu || mc.currentScreen.getClass()
                    .getName()
                    .equals("galaxyspace.core.gui.GSGuiMainMenu"))) {
                launched = true;
                mc.launchIntegratedServer(
                    "completion-rows-" + System.currentTimeMillis(),
                    "Crafting Completion Rows QA",
                    new WorldSettings(42, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (failure != null) throw new AssertionError("server fixture failed", failure);
            if (mc.thePlayer == null) {
                require(clientTicks == 0, "client disconnected at " + phase);
                return;
            }
            require(++clientTicks < 3200, "client fixture timed out at " + phase);
            switch (phase) {
                case OPEN -> {
                    if (!(mc.currentScreen instanceof QuickEncodingTerminalGui quick)) return;
                    quickScreen = quick;
                    if (donor) selectDonorRepo();
                    require(visibleItems(quickScreen).isEmpty(), "Quick terminal starts without rows");
                    phase = Phase.ACTUAL_JOB;
                }
                case ACTUAL_VISIBLE -> {
                    if (visibleItems(quickScreen).isEmpty()) return;
                    requireRows(quickScreen, List.of(realOutput), "actual CPU result arrives over the real network");
                    report("actual CPU completion visible in an opened quick terminal");
                    phase = Phase.PAD_RESULTS;
                }
                case ROWS_VISIBLE -> {
                    if (visibleItems(quickScreen).size() < expectedRows.size()) return;
                    requireRows(quickScreen, expectedRows, "newest first and no foreign-grid completion");
                    if (++stableTicks < 8) return;
                    int slots = completionSlots(quickScreen);
                    require(slots == 12, "seven results reserve two six-column rows, got " + slots);
                    firstVisibleAt = System.nanoTime();
                    report("newest-first Quick results: 7 items, 12 padded slots, and foreign item absent");
                    phase = Phase.SCREENSHOT;
                }
                case EXPIRE -> verifyExpiry();
                case FRESH_VISIBLE -> {
                    if (CraftingCompletionClient.items(quickScreen)
                        .isEmpty()) return;
                    requireRows(quickScreen, List.of(new ItemStack(Items.leather)), "fresh row exists before close");
                    mc.thePlayer.closeScreen();
                    phase = Phase.CLOSED;
                }
                case NORMAL_OPEN -> {
                    if (!(mc.currentScreen instanceof GuiCraftingTerm normal)) return;
                    normalScreen = normal;
                    if (CraftingCompletionClient.items(normalScreen)
                        .isEmpty() || completionSlots(quickScreen) != 0) return;
                    requireRows(
                        normalScreen,
                        List.of(new ItemStack(Items.bone)),
                        "only the newly queued result appears after reopening");
                    report("closing clears the previous visit; newly queued same-grid completion appears upon opening");
                    phase = Phase.NORMAL_RESULT;
                }
                case NORMAL_VISIBLE -> {
                    if (CraftingCompletionClient.items(normalScreen)
                        .size() < 2) return;
                    int slots = completionSlots(normalScreen);
                    if (slots == 0) return;
                    requireRows(
                        normalScreen,
                        List.of(new ItemStack(Items.feather), new ItemStack(Items.bone)),
                        "regular AE crafting terminal receives fresh rows in order");
                    require(slots >= 9, "regular terminal reserves a complete row");
                    report("regular AE crafting terminal receives a fresh result after reopen");
                    finish(null);
                }
                default -> {}
            }
        } catch (Throwable error) {
            finish(error);
        }
    }

    @SubscribeEvent
    public void screenshot(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (phase != Phase.SCREENSHOT || event.gui != quickScreen || finished) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (donor) checkDonorGeometry();
            String name = donor ? "crafting-completion-donor-quick-terminal.png"
                : "crafting-completion-quick-terminal.png";
            ScreenShotHelper
                .saveScreenshot(new File("."), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            require(Files.exists(Path.of("screenshots", name)), "Quick screenshot was written");
            if (donor) finish(null);
            else phase = Phase.EXPIRE;
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void prepareNetworks(EntityPlayerMP player) throws Exception {
        if (access == null) {
            int x = (int) player.posX + 2;
            int y = (int) player.posY + 1;
            int z = (int) player.posZ;
            World world = player.worldObj;
            var defs = AEApi.instance()
                .definitions();
            var blocks = defs.blocks();
            place(world, x, y, z, blocks.energyCellCreative());
            place(world, x - 1, y, z, blocks.wireless());
            access = (TileWireless) world.getTileEntity(x - 1, y, z);
            access.setOrientation(ForgeDirection.WEST, ForgeDirection.UP);
            place(world, x + 1, y, z, blocks.drive());
            ((TileDrive) world.getTileEntity(x + 1, y, z)).getInternalInventory()
                .setInventorySlotContents(
                    0,
                    defs.items()
                        .cell1k()
                        .maybeStack(1)
                        .get());
            place(world, x + 2, y, z, blocks.craftingStorage1k());
            place(world, x + 3, y, z, blocks.iface());
            provider = (TileInterface) world.getTileEntity(x + 3, y, z);
            // AE2 points an initially omnidirectional interface opposite to the side used to configure it.
            provider.setSide(ForgeDirection.WEST);
            world.setBlock(x + 4, y, z, Blocks.chest);
            machineInput = (TileEntityChest) world.getTileEntity(x + 4, y, z);
            place(world, x, y, z + 5, blocks.energyCellCreative());
            place(world, x - 1, y, z + 5, blocks.wireless());
            otherAccess = (TileWireless) world.getTileEntity(x - 1, y, z + 5);
            otherAccess.setOrientation(ForgeDirection.WEST, ForgeDirection.UP);
            realOutput = new ItemStack(Items.book);
            provider.getPatterns()
                .setInventorySlotContents(
                    0,
                    processingPattern(AEItemStack.create(new ItemStack(Items.stick)), AEItemStack.create(realOutput)));
            return;
        }
        require(++networkTicks < 240, "real ME network did not activate");
        if (!access.isActive() || access.getLocatableSerial() == 0 || !otherAccess.isActive()) return;
        grid = access.getGrid();
        otherGrid = otherAccess.getGrid();
        require(grid != null && otherGrid != null && grid != otherGrid, "fixture has two separate active ME networks");
        crafting = grid.getCache(ICraftingGrid.class);
        if (crafting.getCpus()
            .isEmpty()) return;
        if (crafting.getCraftingFor(AEItemStack.create(realOutput), null, 0, player.worldObj)
            .isEmpty()) {
            provider.getInterfaceDuality()
                .updateCraftingList();
            return;
        }
        cpu = crafting.getCpus()
            .iterator()
            .next();
        storage = grid.<IStorageGrid>getCache(IStorageGrid.class)
            .getItemInventory();
        require(
            storage.injectItems(
                AEItemStack.create(new ItemStack(Items.stick)),
                Actionable.MODULATE,
                new MachineSource(provider)) == null,
            "inputs seeded into real ME storage");
        ItemStack terminal = GTNRItemList.WirelessDualInterfaceTerminal.get(1);
        ((IWirelessTermHandler) terminal.getItem())
            .setEncryptionKey(terminal, String.valueOf(access.getLocatableSerial()), "Completion rows QA");
        ((IAEItemPowerStorage) terminal.getItem()).injectAEPower(terminal, 100000);
        player.inventory.setInventorySlotContents(TERMINAL_SLOT, terminal);
        player.inventoryContainer.detectAndSendChanges();
        phase = Phase.OPEN;
        WirelessTerminalSession.open(player, TERMINAL_SLOT, WirelessTerminalGuiType.WirelessDualInterfaceTerminal);
    }

    private void completeActualJob(EntityPlayerMP player) throws Exception {
        var source = new PlayerSource(player, access);
        if (plan == null) {
            plan = crafting.beginCraftingJob(player.worldObj, grid, source, AEItemStack.create(realOutput), null);
            return;
        }
        if (link == null) {
            if (!plan.isDone()) return;
            ICraftingJob job = plan.get();
            require(!job.isSimulation(), "real CPU job can be planned");
            link = crafting.submitJob(job, null, cpu, false, source);
            require(link != null, "real ME CPU accepts the job");
            report("actual player-owned job submitted to the real ME CPU");
            return;
        }
        require(!link.isCanceled(), "actual CPU job never canceled");
        if (!returned) {
            for (int slot = 0; slot < machineInput.getSizeInventory(); slot++) {
                ItemStack stack = machineInput.getStackInSlot(slot);
                if (stack == null) continue;
                require(
                    stack.getItem() == Items.stick && stack.stackSize == 1,
                    "AE CPU dispatched input into real chest");
                machineInput.setInventorySlotContents(slot, null);
                require(
                    storage
                        .injectItems(AEItemStack.create(realOutput), Actionable.MODULATE, new MachineSource(provider))
                        == null,
                    "real ME network accepted output from its adjacent machine");
                returned = true;
                report("adjacent machine received the input and returned its book to the network");
                return;
            }
            return;
        }
        // Standalone crafting links have no nexus, so markDone does not change link.isDone().
        if (cpu.isBusy()) return;
        IAEItemStack stored = storage
            .extractItems(AEItemStack.create(realOutput), Actionable.SIMULATE, new MachineSource(provider));
        require(stored != null && stored.getStackSize() == 1, "completed product arrived in real ME inventory");
        report("actual AE job dispatched a stick, accepted one book, and completed into real ME storage");
        phase = Phase.ACTUAL_VISIBLE;
    }

    private void padResults(EntityPlayerMP player) throws ReflectiveOperationException {
        var results = List.of(
            new ItemStack(Items.apple),
            new ItemStack(Items.melon),
            new ItemStack(Items.carrot),
            new ItemStack(Items.potato),
            new ItemStack(Items.wheat),
            new ItemStack(Items.bread));
        var order = new ArrayList<ItemStack>();
        order.add(realOutput);
        for (ItemStack result : results) {
            recordFixtureResult(player, result);
            order.add(0, result);
        }
        recordCompletion(otherGrid, player, new ItemStack(Items.egg));
        expectedRows = order;
        phase = Phase.ROWS_VISIBLE;
    }

    private void recordFixtureResult(EntityPlayerMP player, ItemStack item) throws ReflectiveOperationException {
        require(
            storage.injectItems(AEItemStack.create(item), Actionable.MODULATE, new MachineSource(provider)) == null,
            "fixture row's result is obtainable from real storage");
        recordCompletion(grid, player, item);
    }

    private void recordCompletion(IGrid origin, EntityPlayerMP player, ItemStack item)
        throws ReflectiveOperationException {
        if (donor) donorRecord.invoke(null, origin, player.getCommandSenderName(), item);
        else CraftingCompletionService.completed(origin, player.getCommandSenderName(), item);
    }

    private void selectDonorRepo() throws Exception {
        require(Loader.isModLoaded("ae2conveniencefeatures"), "actual AE2 Convenience Features donor is installed");
        Class<?> repoType = Class.forName("com.ae2conveniencefeatures.client.crafting.RecentRepo");
        Object repo = repo(quickScreen);
        require(repoType.isInstance(repo), "the donor owns the actual ItemRepo completion prefix");
        require(!(repo instanceof CraftingCompletionRepoAccess), "GTNR prefix mixin is disabled with donor loaded");
        donorItems = Class.forName("com.ae2conveniencefeatures.client.crafting.RecentTerminalClient")
            .getMethod("items", Object.class);
        donorRecord = Class.forName("com.ae2conveniencefeatures.crafting.CompletedCraftsService")
            .getMethod("completed", IGrid.class, String.class, ItemStack.class);
        donorPrefixSlots = repoType.getMethod("ae2cf$prefixSlots");
        ((QuickEncodingTerminalContainer) quickScreen.inventorySlots).requestPinRows(1, 0);
        report("donor installed; only donor's real ItemRepo completion mixin is active; one pin row requested");
    }

    private void checkDonorGeometry() throws Exception {
        VirtualMESlot[] results = slots(quickScreen, "monitorableSlots");
        VirtualMESlot[] pins = slots(quickScreen, "pinSlots");
        require(pins.length > 0 && !pins[0].isHidden(), "donor fixture has an actual visible pin row");
        require(
            results[0].getY() == results[5].getY() && results[5].getX() - results[0].getX() == 5 * 18,
            "first donor row uses all six Quick columns");
        require(
            results[6].getX() == results[0].getX() && results[6].getY() == results[0].getY() + 18,
            "seventh donor result wraps to the next Quick row");
        require(pins[0].getY() == results[6].getY() + 18, "pin row is directly below both donor rows");
        require(
            CraftingCompletionClient.items(quickScreen)
                .isEmpty(),
            "GTNR client completion history is dormant");
        report("donor prefix geometry: two six-column rows, then one real pin row; GTNR history remains empty");
    }

    private void verifyExpiry() throws Exception {
        long seconds = (System.nanoTime() - firstVisibleAt) / 1_000_000_000;
        if (seconds < 55) return;
        if (!expiryChecked) {
            requireRows(quickScreen, expectedRows, "completion rows are still visible after 55 seconds");
            expiryChecked = true;
            return;
        }
        if (!CraftingCompletionClient.items(quickScreen)
            .isEmpty() || completionSlots(quickScreen) != 0) {
            require(seconds <= 65, "completion rows did not expire after their 60-second lifetime");
            return;
        }
        report("Quick rows still visible at 55 seconds, then expired after " + seconds + " seconds");
        phase = Phase.FRESH_RESULT;
    }

    private static ItemStack processingPattern(IAEItemStack input, IAEItemStack output) {
        ItemStack pattern = AEApi.instance()
            .definitions()
            .items()
            .encodedPattern()
            .maybeStack(1)
            .get();
        NBTTagCompound data = new NBTTagCompound();
        NBTTagList ins = new NBTTagList();
        NBTTagList outs = new NBTTagList();
        ins.appendTag(input.toNBTGeneric());
        outs.appendTag(output.toNBTGeneric());
        data.setTag("in", ins);
        data.setTag("out", outs);
        data.setBoolean("crafting", false);
        pattern.setTagCompound(data);
        return pattern;
    }

    private static void place(World world, int x, int y, int z, ITileDefinition def) {
        ItemStack stack = def.maybeStack(1)
            .get();
        world.setBlock(x, y, z, Block.getBlockFromItem(stack.getItem()), stack.getItemDamage(), 3);
    }

    private int completionSlots(GuiMEMonitorable gui) throws ReflectiveOperationException {
        Object itemRepo = repo(gui);
        return donor ? (int) donorPrefixSlots.invoke(itemRepo)
            : ((CraftingCompletionRepoAccess) itemRepo).gtnr$completionSlots();
    }

    private static Object repo(GuiMEMonitorable gui) throws ReflectiveOperationException {
        Field field = GuiMEMonitorable.class.getDeclaredField("repo");
        field.setAccessible(true);
        return field.get(gui);
    }

    private static VirtualMESlot[] slots(GuiMEMonitorable gui, String name) throws ReflectiveOperationException {
        Field field = GuiMEMonitorable.class.getDeclaredField(name);
        field.setAccessible(true);
        return (VirtualMESlot[]) field.get(gui);
    }

    @SuppressWarnings("unchecked")
    private List<ItemStack> visibleItems(Object screen) throws ReflectiveOperationException {
        return donor ? (List<ItemStack>) donorItems.invoke(null, screen) : CraftingCompletionClient.items(screen);
    }

    private void requireRows(Object screen, List<ItemStack> expected, String message)
        throws ReflectiveOperationException {
        List<ItemStack> actual = visibleItems(screen);
        require(actual.size() == expected.size(), message + ": wanted " + expected.size() + ", got " + actual.size());
        for (int i = 0; i < expected.size(); i++) {
            require(
                actual.get(i)
                    .isItemEqual(expected.get(i)) && ItemStack.areItemStackTagsEqual(actual.get(i), expected.get(i)),
                message + " at index " + i);
        }
    }

    private static void report(String line) throws Exception {
        System.out.println("[crafting-completion-qa] " + line);
        Files.writeString(
            Path.of("report.txt"),
            line + System.lineSeparator(),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
    }

    private static void require(boolean check, String message) {
        if (!check) throw new AssertionError(message);
    }

    private void finish(Throwable error) {
        finished = true;
        if (error != null) error.printStackTrace();
        try {
            Files.writeString(Path.of("result.txt"), error == null ? "PASS" : "FAIL " + phase + " " + error);
            Field running = Minecraft.class.getDeclaredField("running");
            running.setAccessible(true);
            running.setBoolean(Minecraft.getMinecraft(), false);
        } catch (Exception secondary) {
            secondary.printStackTrace();
            FMLCommonHandler.instance()
                .exitJava(1, true);
        }
    }

    private enum Phase {
        PREPARE,
        OPEN,
        ACTUAL_JOB,
        ACTUAL_VISIBLE,
        PAD_RESULTS,
        ROWS_VISIBLE,
        SCREENSHOT,
        EXPIRE,
        FRESH_RESULT,
        FRESH_VISIBLE,
        CLOSED,
        NORMAL_OPEN,
        NORMAL_RESULT,
        NORMAL_VISIBLE
    }
}
