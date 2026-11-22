package com.xyp.gtneverreset.qa;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent;

import com.cleanroommc.modularui.screen.ModularScreen;
import com.gtnewhorizon.gtnhlib.util.map.ItemStackMap;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.client.projector.ProjectorClient;
import com.xyp.gtneverreset.client.projector.ProjectorGui;
import com.xyp.gtneverreset.common.projector.*;
import com.xyp.gtnrlib.gui.widget.IconRowList;
import com.xyp.gtnrlib.gui.widget.PageTabs;
import com.xyp.gtnrlib.gui.widget.ThemedButton;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.*;
import appeng.api.storage.data.IAEItemStack;
import appeng.tile.networking.TileWireless;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;

@Mod(
    modid = "gtnrprojectorsurvivalqa",
    name = "Survival projector checks",
    version = "1",
    dependencies = "after:gtneverreset")
public class ProjectorSurvivalChecks {

    private enum Stage {
        SETUP,
        POWER,
        STOCK,
        STOCK_CHANGED,
        SHORT,
        RACE,
        POWER_SHORT,
        CHEST,
        SINGLE,
        PREBUILT,
        DRIVE,
        BATCH_SHORT,
        CANCEL,
        REFUND,
        BATCH,
        RETRY,
        LOOK
    }

    private volatile Stage stage = Stage.SETUP;
    private volatile String feedback = "";
    private volatile int built, shortCount;
    private volatile boolean done;
    private boolean launched, waiting, selectedMaterials;
    private volatile boolean stockChecked;
    private double previewCharge;
    private int stableStockTicks, viewTicks;
    private ProjectorStructure originalPreview;
    private net.minecraft.tileentity.TileEntity obstruction;
    private int x, y, z, ticks, placedEvents;
    private ItemStack terminal, projector, casing;
    private double chargeBefore;
    private TileWireless access;
    private IMEMonitor<IAEItemStack> me;
    private ProjectorStructure shape;
    private final Map<ItemStack, Integer> need = new ItemStackMap<>(true);

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getMinecraft();
        if (done) {
            mc.shutdown();
            return;
        }
        if (!launched && mc.currentScreen != null
            && (mc.currentScreen instanceof GuiMainMenu || mc.currentScreen.getClass()
                .getSimpleName()
                .equals("GSGuiMainMenu"))) {
            launched = true;
            mc.launchIntegratedServer(
                "survival-projector-" + System.currentTimeMillis(),
                "Survival projector QA",
                new WorldSettings(4L, WorldSettings.GameType.SURVIVAL, false, false, WorldType.FLAT).enableCommands());
        }
        if (stage == Stage.LOOK) {
            try {
                var plan = ProjectorClient.plan;
                if (plan == null || plan.state != ProjectorPlan.State.FOLLOW
                    || plan.batch.machines() != 4
                    || !plan.batchProjection) return;
                mc.thePlayer.rotationPitch = 45;
                mc.thePlayer.rotationYaw = ((viewTicks / 5) % 4) * 90;
                if (++viewTicks % 5 != 0) return;
                var field = ProjectorClient.class.getDeclaredField("projection");
                field.setAccessible(true);
                var preview = (ProjectorStructure) field.get(null);
                require(
                    ProjectorClient.visible && preview != null
                        && preview.size > 34
                        && ProjectorClient.facing == ExtendedFacing.of(ForgeDirection.NORTH),
                    "four furnaces changed facing with the camera");
                if (originalPreview == null) originalPreview = preview;
                require(originalPreview == preview, "four furnaces were rearranged when turning");
                if (viewTicks == 25) finish(null);
            } catch (Throwable failure) {
                finish(failure);
            }
            return;
        }
        if ((stage == Stage.STOCK || stage == Stage.STOCK_CHANGED) && !stockChecked) {
            try {
                var screen = ModularScreen.getCurrent();
                if (screen == null || !(screen.getMainPanel() instanceof ProjectorGui gui) || gui.structure() == null)
                    return;
                if (!selectedMaterials) {
                    PageTabs tabs = (PageTabs) gui.getChildren()
                        .stream()
                        .filter(PageTabs.class::isInstance)
                        .findFirst()
                        .orElseThrow();
                    ((ThemedButton<?>) tabs.getChildren()
                        .get(ProjectorGui.MATERIALS)).onMousePressed(0);
                    selectedMaterials = true;
                    return;
                }
                var method = ProjectorGui.class.getDeclaredMethod("materialRows");
                method.setAccessible(true);
                @SuppressWarnings("unchecked")
                var rows = (List<IconRowList.Row>) method.invoke(gui);
                if (rows.size() != 5) return;
                for (var row : rows) {
                    if (row.header()) continue;
                    int needed = need.get(row.icon());
                    int available = stage == Stage.STOCK_CHANGED && casing.isItemEqual(row.icon()) ? 10 : needed * 4;
                    if (!row.detail()
                        .equals(available + "/" + needed)
                        || row.color() != (available >= needed ? IconRowList.GOOD : IconRowList.BAD)) return;
                }
                if (++stableStockTicks < 6) return;
                stableStockTicks = 0;
                net.minecraft.util.ScreenShotHelper.saveScreenshot(
                    new java.io.File("."),
                    "materials-" + stage + ".png",
                    mc.displayWidth,
                    mc.displayHeight,
                    mc.getFramebuffer());
                if (stage == Stage.STOCK_CHANGED) screen.close();
                stockChecked = true;
            } catch (Throwable error) {
                finish(error);
            }
        }
    }

    @SubscribeEvent
    public void chat(ClientChatReceivedEvent e) {
        if (!(e.message instanceof ChatComponentTranslation msg)) return;
        String key = msg.getKey();
        if (key.equals("gtneverreset.projector.build.missing_item")) {
            shortCount = Integer.parseInt(String.valueOf(msg.getFormatArgs()[1]));
            feedback = "missing";
        }
        if (key.equals("gtneverreset.projector.build.done")) {
            built = Integer.parseInt(String.valueOf(msg.getFormatArgs()[0]));
            feedback = "done";
        }
        if (key.equals("gtneverreset.projector.build.stopped")) feedback = "stopped";
    }

    @SubscribeEvent
    public void place(BlockEvent.PlaceEvent e) {
        if (!(e.player instanceof EntityPlayerMP) || !(e.world instanceof net.minecraft.world.WorldServer) || !waiting)
            return;
        placedEvents++;
        if (stage == Stage.CANCEL) {
            var p = (EntityPlayerMP) e.player;
            for (int slot = 0; slot < p.inventory.mainInventory.length; slot++)
                if (p.inventory.getStackInSlot(slot) == null)
                    p.inventory.setInventorySlotContents(slot, new ItemStack(Blocks.dirt, 64));
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent e) {
        if (done || e.phase != TickEvent.Phase.END) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP p = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            require(++ticks < 2400, "timeout stage=" + stage + " feedback=" + feedback);
            if (waiting && feedback.isEmpty()) return;
            if (waiting) {
                waiting = false;
                verify(p);
                if (done) return;
            }
            switch (stage) {
                case SETUP -> {
                    require(Loader.isModLoaded("structurecompat"), "StructureCompat missing");
                    x = (int) p.posX + 12;
                    y = 14;
                    z = (int) p.posZ + 7;
                    var defs = AEApi.instance()
                        .definitions();
                    int ax = (int) p.posX + 2, ay = (int) p.posY + 1, az = (int) p.posZ;
                    p.worldObj.setBlock(
                        ax,
                        ay,
                        az,
                        defs.blocks()
                            .energyCellCreative()
                            .maybeBlock()
                            .get());
                    p.worldObj.setBlock(
                        ax + 1,
                        ay,
                        az,
                        defs.blocks()
                            .drive()
                            .maybeBlock()
                            .get());
                    p.worldObj.setBlock(
                        ax - 1,
                        ay,
                        az,
                        defs.blocks()
                            .wireless()
                            .maybeBlock()
                            .get());
                    var drive = (TileDrive) p.worldObj.getTileEntity(ax + 1, ay, az);
                    drive.getInternalInventory()
                        .setInventorySlotContents(
                            0,
                            defs.items()
                                .cell4k()
                                .maybeStack(1)
                                .get());
                    access = (TileWireless) p.worldObj.getTileEntity(ax - 1, ay, az);
                    access.setOrientation(ForgeDirection.WEST, ForgeDirection.UP);
                    projector = new ItemStack(GameRegistry.findItem("gtneverreset", "multiblock_projector"));
                    Arrays.fill(p.inventory.mainInventory, null);
                    p.inventory.currentItem = 0;
                    p.inventory.setInventorySlotContents(0, projector);
                    makePlan(p, false);
                    stage = Stage.POWER;
                }
                case POWER -> {
                    if (!access.isActive() || access.getLocatableSerial() == 0) return;
                    IStorageGrid storage = access.getGrid()
                        .getCache(IStorageGrid.class);
                    me = storage.getItemInventory();
                    terminal = new ItemStack(GameRegistry.findItem("gtneverreset", "wireless_dual_interface_terminal"));
                    ((IWirelessTermHandler) terminal.getItem())
                        .setEncryptionKey(terminal, String.valueOf(access.getLocatableSerial()), "Projector QA");
                    ((IAEItemPowerStorage) terminal.getItem()).injectAEPower(terminal, 100000);
                    p.inventory.setInventorySlotContents(1, terminal);
                    for (var entry : need.entrySet()) {
                        var batch = AEItemStack.create(entry.getKey())
                            .setStackSize(entry.getValue() * 4L);
                        require(
                            me.injectItems(batch, Actionable.MODULATE, new PlayerSource(p, null)) == null,
                            "seed material page");
                    }
                    previewCharge = ((IAEItemPowerStorage) terminal.getItem()).getAECurrentPower(terminal);
                    p.inventoryContainer.detectAndSendChanges();
                    stage = Stage.STOCK;
                    ProjectorServer.openGui(p, 0);
                }
                case STOCK -> {
                    if (!stockChecked) return;
                    require(need.get(casing) == 16, "test casing count");
                    for (var entry : need.entrySet())
                        require(total(p, entry.getKey()) == entry.getValue() * 4, "material preview spent stock");
                    var decrement = me.extractItems(
                        AEItemStack.create(casing)
                            .setStackSize(57),
                        Actionable.MODULATE,
                        new PlayerSource(p, null));
                    require(decrement != null && decrement.getStackSize() == 57, "change material page stock");
                    give(p, casing, 3);
                    stage = Stage.STOCK_CHANGED;
                    stockChecked = false;
                }
                case STOCK_CHANGED -> {
                    if (!stockChecked) return;
                    require(total(p, casing) == 10, "material preview changed mixed stock");
                    require(
                        ((IAEItemPowerStorage) terminal.getItem()).getAECurrentPower(terminal) == previewCharge,
                        "material preview consumed wireless power");
                    for (var entry : need.entrySet()) me.extractItems(
                        AEItemStack.create(entry.getKey())
                            .setStackSize(entry.getValue() * 4L),
                        Actionable.MODULATE,
                        new PlayerSource(p, null));
                    for (int slot = 2; slot < p.inventory.mainInventory.length; slot++)
                        if (p.inventory.getStackInSlot(slot) != null
                            && casing.isItemEqual(p.inventory.getStackInSlot(slot)))
                            p.inventory.setInventorySlotContents(slot, null);
                    stage = Stage.SHORT;
                    seed(p, true);
                    request(p, false);
                }
                case RACE -> {
                    require(p.inventory.getStackInSlot(8) == null, "duplicate terminal slot is empty");
                    p.inventory.setInventorySlotContents(8, terminal.copy());
                    request(p, false);
                }
                case POWER_SHORT -> {
                    p.inventory.setInventorySlotContents(8, null);
                    moveRefundsToGrid(p);
                    give(p, casing, 1);
                    var battery = (IAEItemPowerStorage) terminal.getItem();
                    battery.extractAEPower(terminal, battery.getAECurrentPower(terminal));
                    request(p, false);
                }
                case CHEST -> {
                    p.worldObj.setBlock(x, y, z, Blocks.chest);
                    var chest = (net.minecraft.tileentity.TileEntityChest) p.worldObj.getTileEntity(x, y, z);
                    chest.setInventorySlotContents(0, new ItemStack(net.minecraft.init.Items.diamond));
                    obstruction = chest;
                    request(p, false);
                }
                case SINGLE -> {
                    putHeat(p, x, y, z);
                    ((IAEItemPowerStorage) terminal.getItem()).injectAEPower(terminal, 100000);
                    request(p, false);
                }
                case PREBUILT -> {
                    y += 12;
                    makePlan(p, true);
                    int cell = -1;
                    for (int i = 0; i < shape.size; i++) if (casing.isItemEqual(shape.items[i])) {
                        cell = i;
                        break;
                    }
                    require(cell >= 0, "casing cell");
                    int[] off = new int[3];
                    shape.canonical.getWorldOffset(new int[] { shape.a[cell], shape.b[cell], shape.c[cell] }, off);
                    p.worldObj.setBlock(x + off[0], y + off[1], z + off[2], shape.blocks[cell], shape.metas[cell], 3);
                    decrement(casing);
                    seed(p, true);
                    p.worldObj.setBlock(
                        x + 2,
                        y,
                        z,
                        AEApi.instance()
                            .definitions()
                            .blocks()
                            .drive()
                            .maybeBlock()
                            .get());
                    obstruction = p.worldObj.getTileEntity(x + 2, y, z);
                    require(obstruction instanceof TileDrive, "batch obstacle is a real ME drive");
                    stage = Stage.DRIVE;
                    request(p, true);
                }
                case BATCH_SHORT -> {
                    putHeat(p, x + 2, y, z);
                    request(p, true);
                }
                case CANCEL -> {
                    give(p, casing, 1);
                    request(p, true);
                }
                case REFUND -> {
                    if (hasRefund(p)) return;
                    balances(p, false, false);
                    moveRefundsToGrid(p);
                    stage = Stage.BATCH;
                    request(p, true);
                }
                case RETRY -> request(p, true);
                case LOOK -> {}
                default -> throw new AssertionError(stage);
            }
        } catch (Throwable error) {
            finish(error);
        }
    }

    private void makePlan(EntityPlayerMP p, boolean batch) {
        var plan = new ProjectorPlan();
        plan.selectMachine(ProjectorMachines.idOf(ItemList.Machine_Multi_BlastFurnace.get(1)));
        plan.state = ProjectorPlan.State.LOCKED;
        plan.dimension = p.dimension;
        plan.x = x;
        plan.y = y;
        plan.z = z;
        plan.facing = ExtendedFacing.of(ForgeDirection.NORTH);
        if (batch) {
            plan.batch.setAdditional(ForgeDirection.EAST, 1);
            plan.batch.setShared(ForgeDirection.EAST, true);
        }
        plan.write(projector);
        shape = ProjectorSimulation.run(plan.settings());
        require(shape.size == 34 && shape.failure.isEmpty(), "EBF simulation");
        casing = shape.materials.stream()
            .filter(
                m -> m.stack()
                    .getDisplayName()
                    .toLowerCase(Locale.ROOT)
                    .contains("casing"))
            .map(ProjectorStructure.Material::stack)
            .findFirst()
            .orElse(
                shape.materials.get(0)
                    .stack());
        if (batch) require(shape.canonical == plan.facing, "fixture facing");
        need.clear();
        var seen = new HashSet<Long>();
        for (int n = 0; n < (batch ? 2 : 1); n++) for (int i = 0; i < shape.size; i++) {
            int[] off = new int[3];
            plan.facing.getWorldOffset(new int[] { shape.a[i], shape.b[i], shape.c[i] }, off);
            if (seen.add(ProjectorPlan.pack(off[0] + n * 2, off[1], off[2])) && shape.items[i] != null)
                need.merge(shape.items[i], 1, Integer::sum);
        }
        require(
            need.values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum() == (batch ? 56 : 34),
            "unique footprint");
        p.inventoryContainer.detectAndSendChanges();
    }

    private void decrement(ItemStack stack) {
        need.put(stack, need.get(stack) - 1);
    }

    private void seed(EntityPlayerMP p, boolean shortOne) {
        for (var entry : need.entrySet()) {
            ItemStack stack = entry.getKey();
            int count = entry.getValue() - (shortOne && casing.isItemEqual(stack) ? 1 : 0);
            if (stack.isItemEqual(ItemList.Machine_Multi_BlastFurnace.get(1))) give(p, stack, count);
            else {
                var ae = AEItemStack.create(stack)
                    .setStackSize(count);
                require(me.injectItems(ae, Actionable.MODULATE, new PlayerSource(p, null)) == null, "seed ME " + stack);
            }
        }
    }

    private void moveRefundsToGrid(EntityPlayerMP p) {
        for (int i = 0; i < p.inventory.mainInventory.length; i++) {
            var stack = p.inventory.mainInventory[i];
            if (stack == null || !need.containsKey(stack)
                || stack.isItemEqual(ItemList.Machine_Multi_BlastFurnace.get(1))) continue;
            require(
                me.injectItems(AEItemStack.create(stack), Actionable.MODULATE, new PlayerSource(p, null)) == null,
                "reseed network");
            p.inventory.setInventorySlotContents(i, null);
        }
    }

    private void give(EntityPlayerMP p, ItemStack stack, int count) {
        var copy = stack.copy();
        copy.stackSize = count;
        require(p.inventory.addItemStackToInventory(copy), "seed backpack");
    }

    private int total(EntityPlayerMP p, ItemStack stack) {
        int n = 0;
        for (var item : p.inventory.mainInventory)
            if (item != null && item.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(item, stack))
                n += item.stackSize;
        var have = me.extractItems(
            AEItemStack.create(stack)
                .setStackSize(100000),
            Actionable.SIMULATE,
            new PlayerSource(p, null));
        return n + (have == null ? 0 : (int) have.getStackSize());
    }

    private void balances(EntityPlayerMP p, boolean shortOne, boolean empty) {
        for (var e : need.entrySet()) {
            int want = empty ? 0 : e.getValue() - (shortOne && casing.isItemEqual(e.getKey()) ? 1 : 0);
            require(
                total(p, e.getKey()) == want,
                "balance " + e.getKey() + " expected " + want + " got " + total(p, e.getKey()));
        }
    }

    private void request(EntityPlayerMP p, boolean batch) {
        feedback = "";
        shortCount = -1;
        placedEvents = 0;
        waiting = true;
        chargeBefore = ((IAEItemPowerStorage) terminal.getItem()).getAECurrentPower(terminal);
        ProjectorBuilder.request(p, 0, batch);
    }

    private boolean hasRefund(EntityPlayerMP p) {
        return p.getEntityData()
            .getCompoundTag(net.minecraft.entity.player.EntityPlayer.PERSISTED_NBT_TAG)
            .hasKey("gtneverresetProjectorMaterials");
    }

    private void verify(EntityPlayerMP p) {
        switch (stage) {
            case SHORT, RACE, BATCH_SHORT -> {
                require(
                    feedback.equals("missing") && shortCount == 1 && placedEvents == 0,
                    "shortage " + stage + " " + feedback + " " + shortCount + " placed " + placedEvents);
                require(p.worldObj.isAirBlock(x, y, z), "partial structure");
                balances(p, true, false);
                stage = stage == Stage.SHORT ? Stage.RACE : stage == Stage.RACE ? Stage.POWER_SHORT : Stage.CANCEL;
            }
            case POWER_SHORT -> {
                require(
                    feedback.equals("missing") && placedEvents == 0 && p.worldObj.isAirBlock(x, y, z),
                    "unpowered wireless terminal");
                balances(p, false, false);
                stage = Stage.CHEST;
            }
            case CHEST -> {
                require(
                    feedback.equals("stopped") && placedEvents == 0
                        && p.worldObj.getTileEntity(x, y, z) == obstruction
                        && ((net.minecraft.tileentity.TileEntityChest) obstruction).getStackInSlot(0)
                            .getItem() == net.minecraft.init.Items.diamond,
                    "occupied chest was not preserved");
                p.worldObj.setBlockToAir(x, y, z);
                stage = Stage.SINGLE;
            }
            case DRIVE -> {
                require(
                    feedback.equals("stopped") && placedEvents == 0
                        && p.worldObj.getTileEntity(x + 2, y, z) == obstruction,
                    "batch did not preserve ME drive");
                p.worldObj.setBlockToAir(x + 2, y, z);
                stage = Stage.BATCH_SHORT;
            }
            case CANCEL -> {
                require(
                    feedback.equals("stopped") && placedEvents == 1 && p.worldObj.isAirBlock(x, y, z),
                    "protection rollback");
                require(hasRefund(p), "full inventory retains unused reserved items");
                for (int slot = 0; slot < p.inventory.mainInventory.length; slot++) {
                    ItemStack item = p.inventory.getStackInSlot(slot);
                    if (item != null && item.getItem() == net.minecraft.item.Item.getItemFromBlock(Blocks.dirt))
                        p.inventory.setInventorySlotContents(slot, null);
                }
                stage = Stage.REFUND;
            }
            case SINGLE, BATCH, RETRY -> {
                int expected = stage == Stage.SINGLE ? 34 : stage == Stage.BATCH ? 55 : 0;
                require(
                    feedback.equals("done") && built == expected && placedEvents == expected,
                    "built " + built + " events " + placedEvents + " expected " + expected);
                require(
                    p.worldObj
                        .getTileEntity(x, y, z) instanceof gregtech.api.interfaces.tileentity.IGregTechTileEntity tile
                        && tile.getMetaTileID() == ProjectorMachines.idOf(ItemList.Machine_Multi_BlastFurnace.get(1)),
                    "controller missing");
                balances(p, false, true);
                if (stage != Stage.RETRY) require(
                    ((IAEItemPowerStorage) terminal.getItem()).getAECurrentPower(terminal) < chargeBefore,
                    "wireless extraction used terminal power");

                if (stage == Stage.RETRY) {
                    var draft = ProjectorPlan.read(projector);
                    draft.batch.setAdditional(ForgeDirection.SOUTH, 1);
                    draft.batchProjection = true;
                    draft.state = ProjectorPlan.State.FOLLOW;
                    draft.write(projector);
                    p.inventory.markDirty();
                    p.inventoryContainer.detectAndSendChanges();
                    stage = Stage.LOOK;
                    return;
                }
                stage = stage == Stage.SINGLE ? Stage.PREBUILT : Stage.RETRY;
            }
            default -> throw new AssertionError(stage);
        }
    }

    private void putHeat(EntityPlayerMP player, int wx, int wy, int wz) {
        var heat = GameRegistry.findBlock("Railcraft", "residual.heat");
        require(
            heat != null && player.worldObj.setBlock(wx, wy, wz, heat)
                && player.worldObj.getTileEntity(wx, wy, wz) != null,
            "installed Railcraft heat");
    }

    private static void require(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }

    private void finish(Throwable t) {
        try {
            var sw = new StringWriter();
            if (t != null) t.printStackTrace(new PrintWriter(sw));
            Files.writeString(Path.of("result.txt"), t == null ? "PASS" : "FAIL " + stage + " " + sw);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        done = true;
    }
}
