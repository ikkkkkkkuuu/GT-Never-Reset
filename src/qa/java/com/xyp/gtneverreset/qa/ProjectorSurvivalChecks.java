package com.xyp.gtneverreset.qa;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent;

import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.gtnewhorizon.gtnhlib.util.map.ItemStackMap;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.client.projector.ProjectorClient;
import com.xyp.gtneverreset.client.projector.ProjectorGui;
import com.xyp.gtneverreset.common.gui.WirelessTerminalGuiHandler;
import com.xyp.gtneverreset.common.packet.ProjectorBuildActionPacket.Action;
import com.xyp.gtneverreset.common.projector.*;
import com.xyp.gtneverreset.utils.enums.WirelessTerminalGuiType;
import com.xyp.gtnrlib.gui.widget.AE2Button;
import com.xyp.gtnrlib.gui.widget.IconRowList;
import com.xyp.gtnrlib.gui.widget.PageTabs;
import com.xyp.gtnrlib.gui.widget.ThemedButton;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.definitions.ITileDefinition;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.*;
import appeng.api.storage.data.IAEItemStack;
import appeng.items.tools.powered.ToolWirelessTerminal;
import appeng.tile.misc.TileInterface;
import appeng.tile.networking.TileWireless;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

@Mod(
    modid = "gtnrprojectorsurvivalqa",
    name = "Survival projector checks",
    version = "1",
    dependencies = "after:gtneverreset")
public class ProjectorSurvivalChecks {

    private static final int NETHER = -1;

    private enum Stage {
        SETUP,
        POWER,
        CROSS_DIMENSION,
        CROSS_PREVIEW,
        CROSS_BUILD,
        MATERIAL_CRAFTING,
        STOCK,
        STOCK_CHANGED,
        SHORT,
        RACE,
        POWER_SHORT,
        CHEST,
        SINGLE,
        PREBUILT,
        BATCH_SHORT,
        CANCEL,
        REFUND,
        DRIVE,
        BATCH,
        RETRY,
        LOOK,
        DISMANTLE
    }

    private volatile Stage stage = Stage.SETUP;
    private volatile String feedback = "";
    private volatile int built, shortCount;
    private volatile boolean done;
    private boolean launched, waiting, selectedMaterials;
    private volatile boolean stockChecked;
    private final boolean crossDimension = Boolean.getBoolean("gtnr.projector.crossDimensionQa");
    private final boolean crossDimensionBaseline = Boolean.getBoolean("gtnr.projector.crossDimensionBaseline");
    private final MaterialCraftingScenario materialCrafting = Boolean.getBoolean("gtnr.projector.materialCraftingQa")
        ? new MaterialCraftingScenario()
        : null;
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

    private long nextRequestTick;

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
                if (viewTicks == 25) stage = Stage.DISMANTLE;
            } catch (Throwable failure) {
                finish(failure);
            }
            return;
        }
        if ((stage == Stage.STOCK || stage == Stage.STOCK_CHANGED
            || stage == Stage.CROSS_PREVIEW
            || stage == Stage.MATERIAL_CRAFTING) && !stockChecked) {
            try {
                if (stage == Stage.CROSS_PREVIEW && (mc.thePlayer == null || mc.thePlayer.dimension != NETHER)) return;
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
                if (stage == Stage.MATERIAL_CRAFTING) {
                    materialCrafting.checkClient(gui, rows);
                    return;
                }
                for (var row : rows) {
                    if (row.header()) continue;
                    int needed = need.get(row.icon());
                    int available = stage == Stage.CROSS_PREVIEW && crossDimensionBaseline ? 0 : needed * 4;
                    if (stage == Stage.STOCK_CHANGED && casing.isItemEqual(row.icon())) available = 10;
                    if (!row.detail()
                        .equals(available + "/" + needed)
                        || row.color() != (available >= needed ? IconRowList.GOOD : IconRowList.BAD)) return;
                }
                if (++stableStockTicks < 6) return;
                stableStockTicks = 0;
                net.minecraft.util.ScreenShotHelper.saveScreenshot(
                    new java.io.File("."),
                    stage == Stage.CROSS_PREVIEW
                        ? "projector-cross-dimension-preview-" + (crossDimensionBaseline ? "before" : "after") + ".png"
                        : "materials-" + stage + ".png",
                    mc.displayWidth,
                    mc.displayHeight,
                    mc.getFramebuffer());
                if (stage == Stage.STOCK_CHANGED || stage == Stage.CROSS_PREVIEW) screen.close();
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
        if (key.equals("gtneverreset.projector.removal.done")) {
            built = Integer.parseInt(String.valueOf(msg.getFormatArgs()[0]));
            feedback = "removed";
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
            if (p.worldObj.getTotalWorldTime() < nextRequestTick) return;
            switch (stage) {
                case SETUP -> {
                    require(Loader.isModLoaded("structurecompat"), "StructureCompat missing");
                    p.worldObj.getGameRules()
                        .setOrCreateGameRule("doMobSpawning", "false");
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
                    if (materialCrafting != null) materialCrafting.setUp(p, ax, ay, az);
                    stage = Stage.POWER;
                }
                case POWER -> {
                    if (!access.isActive() || access.getLocatableSerial() == 0) return;
                    IStorageGrid storage = access.getGrid()
                        .getCache(IStorageGrid.class);
                    me = storage.getItemInventory();
                    if (materialCrafting != null && !materialCrafting.ready(p)) return;
                    terminal = new ItemStack(GameRegistry.findItem("gtneverreset", "wireless_dual_interface_terminal"));
                    ((IWirelessTermHandler) terminal.getItem())
                        .setEncryptionKey(terminal, String.valueOf(access.getLocatableSerial()), "Projector QA");
                    ((IAEItemPowerStorage) terminal.getItem()).injectAEPower(terminal, 100000);
                    p.inventory.setInventorySlotContents(1, terminal);
                    if (materialCrafting != null) {
                        materialCrafting.seed(p);
                        p.inventoryContainer.detectAndSendChanges();
                        stage = Stage.MATERIAL_CRAFTING;
                        ProjectorServer.openGui(p, 0);
                        return;
                    }
                    for (var entry : need.entrySet()) {
                        var batch = AEItemStack.create(entry.getKey())
                            .setStackSize(entry.getValue() * 4L);
                        require(
                            me.injectItems(batch, Actionable.MODULATE, new PlayerSource(p, null)) == null,
                            "seed material page");
                    }
                    previewCharge = ((IAEItemPowerStorage) terminal.getItem()).getAECurrentPower(terminal);
                    p.inventoryContainer.detectAndSendChanges();
                    if (crossDimension) {
                        int stock = need.get(casing) * 4;
                        require(nativeWirelessInRange(p), "unboosted terminal can reach nearby overworld network");
                        require(
                            ProjectorBuildMaterials.available(p, casing, stock + 1) == stock,
                            "projector sees available items on the same dimension without upgrade");
                        reportCross("same dimension unupgraded: dimension=" + p.dimension + ", available=" + stock);
                        moveToNether(p);
                        stage = Stage.CROSS_DIMENSION;
                        return;
                    }
                    stage = Stage.STOCK;
                    ProjectorServer.openGui(p, 0);
                }
                case CROSS_DIMENSION -> {
                    if (p.dimension != NETHER || !access.isActive()) return;
                    prepareCrossPreview(p);
                }
                case CROSS_PREVIEW -> {
                    if (!stockChecked) return;
                    require(
                        ((IAEItemPowerStorage) terminal.getItem()).getAECurrentPower(terminal) == previewCharge,
                        "cross-dimension preview spends no wireless power");
                    for (var entry : need.entrySet()) {
                        require(gridCount(p, entry.getKey()) == entry.getValue() * 4, "preview consumes no ME items");
                    }
                    reportCross(
                        "client material preview synchronized; stored item counts and terminal charge unchanged");
                    stage = Stage.CROSS_BUILD;
                    request(p, false);
                }
                case MATERIAL_CRAFTING -> {
                    if (stockChecked) materialCrafting.checkServer(p);
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
                    ((IAEItemPowerStorage) terminal.getItem()).injectAEPower(terminal, 100000);
                    p.worldObj.setBlock(x, y, z, Blocks.chest);
                    var chest = (net.minecraft.tileentity.TileEntityChest) p.worldObj.getTileEntity(x, y, z);
                    chest.setInventorySlotContents(0, new ItemStack(net.minecraft.init.Items.diamond));
                    obstruction = chest;
                    request(p, false);
                }
                case SINGLE -> {
                    putHeat(p, x, y, z);
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
                    stage = Stage.BATCH_SHORT;
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
                    stage = Stage.DRIVE;
                }
                case DRIVE -> {
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
                    request(p, true);
                }
                case BATCH -> {
                    putHeat(p, x + 2, y, z);
                    request(p, true);
                }
                case RETRY -> {
                    var draft = checkedBatch(p);
                    require(
                        draft.state == ProjectorPlan.State.LOCKED && draft.batch.machines() == 2,
                        "the completed two-copy projection must stay locked for the retry");
                    request(p, true);
                }
                case LOOK -> {}
                case DISMANTLE -> {
                    var draft = checkedBatch(p);
                    draft.batch.setAdditional(ForgeDirection.SOUTH, 0);
                    require(draft.batch.machines() == 2, "fixture lost the two-copy batch");
                    draft.state = ProjectorPlan.State.LOCKED;
                    draft.write(projector);
                    startRequest(p);
                    ProjectorBuildActions.request(p, 0, Action.DISMANTLE, true);
                }
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
            plan.batch.setGap(ForgeDirection.EAST, -1);
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

    private void moveToNether(EntityPlayerMP p) {
        if (DimensionManager.getWorld(NETHER) == null) DimensionManager.initDimension(NETHER);
        var nether = DimensionManager.getWorld(NETHER);
        require(nether != null, "Nether test dimension loaded");
        int standY = 110;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            nether.setBlock(dx, standY - 1, dz, Blocks.cobblestone);
            for (int dy = 0; dy < 4; dy++) nether.setBlockToAir(dx, standY + dy, dz);
        }
        MinecraftServer.getServer()
            .getConfigurationManager()
            .transferPlayerToDimension(p, NETHER, new Teleporter(nether) {

                @Override
                public void placeInPortal(Entity entity, double x, double y, double z, float yaw) {
                    entity.setLocationAndAngles(0.5, standY, 0.5, yaw, 0);
                }
            });
    }

    private void prepareCrossPreview(EntityPlayerMP p) throws Exception {
        require(access.getWorldObj().provider.dimensionId == 0, "ME network remains in overworld");
        require(p.worldObj.provider.dimensionId == NETHER, "player is actually in the Nether");
        int stock = need.get(casing) * 4;
        require(!nativeWirelessInRange(p), "unupgraded terminal has no range into remote dimension");
        require(
            ProjectorBuildMaterials.available(p, casing, stock + 1) == 0,
            "projector cannot see remote items without range upgrade");
        reportCross("cross-dimension unupgraded: player=-1, ME=0, native=false, available=0");
        terminal.getTagCompound()
            .setBoolean(ToolWirelessTerminal.infinityBoosterCard, true);
        require(nativeWirelessInRange(p), "native wireless host can reach remote ME after range upgrade");
        int seen = ProjectorBuildMaterials.available(p, casing, stock + 1);
        reportCross("cross-dimension upgraded: native=true, ME stock=" + stock + ", projector available=" + seen);
        require(seen == (crossDimensionBaseline ? 0 : stock), "projector remote availability differs from expectation");
        x = 8;
        y = 110;
        z = 7;
        makePlan(p, false);
        var plan = ProjectorPlan.read(projector);
        int[] off = new int[3];
        for (int i = 0; i < shape.size; i++) {
            plan.facing.getWorldOffset(new int[] { shape.a[i], shape.b[i], shape.c[i] }, off);
            p.worldObj.setBlockToAir(x + off[0], y + off[1], z + off[2]);
        }
        for (long cell : shape.emptyCells) {
            plan.facing.getWorldOffset(
                new int[] { ProjectorPlan.unpackA(cell), ProjectorPlan.unpackB(cell), ProjectorPlan.unpackC(cell) },
                off);
            p.worldObj.setBlockToAir(x + off[0], y + off[1], z + off[2]);
        }
        p.inventory.markDirty();
        p.inventoryContainer.detectAndSendChanges();
        stage = Stage.CROSS_PREVIEW;
        ProjectorServer.openGui(p, 0);
    }

    private static boolean nativeWirelessInRange(EntityPlayerMP p) {
        var host = WirelessTerminalGuiHandler
            .buildHost(p, p.worldObj, 1, WirelessTerminalGuiType.WirelessCraftingTerminal);
        return host != null && host.rangeCheck() && host.getItemInventory() != null;
    }

    private static void reportCross(String line) throws Exception {
        System.out.println("[projector-cross-dimension-qa] " + line);
        Files.writeString(
            Path.of("projector-cross-dimension-report.txt"),
            line + System.lineSeparator(),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
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
        return n + gridCount(p, stack);
    }

    private int gridCount(EntityPlayerMP p, ItemStack stack) {
        var have = me.extractItems(
            AEItemStack.create(stack)
                .setStackSize(100000),
            Actionable.SIMULATE,
            new PlayerSource(p, null));
        return have == null ? 0 : (int) have.getStackSize();
    }

    private void balances(EntityPlayerMP p, boolean shortOne, boolean empty) {
        for (var e : need.entrySet()) {
            int want = empty ? 0 : e.getValue() - (shortOne && casing.isItemEqual(e.getKey()) ? 1 : 0);
            require(
                total(p, e.getKey()) == want,
                "balance " + e.getKey() + " expected " + want + " got " + total(p, e.getKey()));
        }
    }

    private void balancesAfterSkippingController(EntityPlayerMP p) {
        var controller = ItemList.Machine_Multi_BlastFurnace.get(1);
        for (var stack : need.keySet()) {
            int expected = controller.isItemEqual(stack) ? 1 : 0;
            require(total(p, stack) == expected, "skipped controller balance " + stack + " expected " + expected);
        }
        require(
            ((IAEItemPowerStorage) terminal.getItem()).getAECurrentPower(terminal) < chargeBefore,
            "placing around an obstacle used wireless terminal power");
    }

    private void request(EntityPlayerMP p, boolean batch) {
        startRequest(p);
        ProjectorBuilder.request(p, 0, batch);
    }

    private ProjectorPlan checkedBatch(EntityPlayerMP player) {
        require(player.getCurrentEquippedItem() == projector, "fixture lost the selected projector");
        var draft = ProjectorPlan.read(projector);
        require(
            draft.machine == blastFurnaceId() && draft.dimension == player.dimension
                && draft.x == x
                && draft.y == y
                && draft.z == z,
            "fixture lost the saved batch site");
        return draft;
    }

    private void startRequest(EntityPlayerMP p) {
        nextRequestTick = p.worldObj.getTotalWorldTime() + 41;
        feedback = "";
        shortCount = -1;
        placedEvents = 0;
        waiting = true;
        chargeBefore = ((IAEItemPowerStorage) terminal.getItem()).getAECurrentPower(terminal);
    }

    private boolean hasRefund(EntityPlayerMP p) {
        return p.getEntityData()
            .getCompoundTag(net.minecraft.entity.player.EntityPlayer.PERSISTED_NBT_TAG)
            .hasKey("gtneverresetProjectorMaterials");
    }

    private void verify(EntityPlayerMP p) throws Exception {
        switch (stage) {
            case CROSS_BUILD -> {
                double charge = ((IAEItemPowerStorage) terminal.getItem()).getAECurrentPower(terminal);
                if (crossDimensionBaseline) {
                    require(
                        feedback.equals("missing") && placedEvents == 0 && p.worldObj.isAirBlock(x, y, z),
                        "baseline projector reports missing items and places nothing across dimensions");
                    require(charge == chargeBefore, "baseline consumes no terminal energy");
                } else {
                    require(
                        feedback.equals("done") && built == 34 && placedEvents == 34,
                        "cross-dimension generation places all 34 blocks");
                    require(
                        p.worldObj.getTileEntity(x, y, z) instanceof IGregTechTileEntity tile
                            && tile.getMetaTileID() == blastFurnaceId(),
                        "Nether furnace controller is really placed");
                    require(charge < chargeBefore, "cross-dimension extraction spends actual terminal energy");
                }
                for (var entry : need.entrySet()) {
                    require(
                        gridCount(p, entry.getKey()) == entry.getValue() * (crossDimensionBaseline ? 4 : 3),
                        "exact ME balance after cross-dimension build");
                }
                reportCross(
                    "generation feedback=" + feedback
                        + ", placed="
                        + placedEvents
                        + ", charge spent="
                        + (chargeBefore - charge)
                        + ", exact remote storage balance verified");
                finish(null);
            }
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
                    feedback.equals("done") && built == 33 && placedEvents == 33,
                    "occupied chest did not allow the surrounding 33 blocks to be placed");
                require(
                    p.worldObj.getTileEntity(x, y, z) == obstruction
                        && ((net.minecraft.tileentity.TileEntityChest) obstruction).getStackInSlot(0)
                            .getItem() == net.minecraft.init.Items.diamond,
                    "occupied chest was not preserved");
                balancesAfterSkippingController(p);
                p.worldObj.setBlockToAir(x, y, z);
                stage = Stage.SINGLE;
            }
            case DRIVE -> {
                require(
                    feedback.equals("done") && built == 54 && placedEvents == 54,
                    "occupied ME drive did not allow the other 54 missing batch blocks to be placed");
                require(p.worldObj.getTileEntity(x + 2, y, z) == obstruction, "batch did not preserve ME drive");
                balancesAfterSkippingController(p);
                p.worldObj.setBlockToAir(x + 2, y, z);
                stage = Stage.BATCH;
            }
            case CANCEL -> {
                require(
                    feedback.equals("done") && built == 0 && placedEvents == 55,
                    "protection should roll back each denied placement and continue to the next");
                require(p.worldObj.isAirBlock(x, y, z), "protection rollback left a controller in the world");
                require(hasRefund(p), "full inventory retains unused reserved items");
                for (int slot = 0; slot < p.inventory.mainInventory.length; slot++) {
                    ItemStack item = p.inventory.getStackInSlot(slot);
                    if (item != null && item.getItem() == net.minecraft.item.Item.getItemFromBlock(Blocks.dirt))
                        p.inventory.setInventorySlotContents(slot, null);
                }
                stage = Stage.REFUND;
            }
            case SINGLE, BATCH, RETRY -> {
                int expected = stage == Stage.RETRY ? 0 : 1;
                require(
                    feedback.equals("done") && built == expected && placedEvents == expected,
                    "built " + built + " events " + placedEvents + " expected " + expected);
                require(
                    p.worldObj.getTileEntity(x, y, z) instanceof IGregTechTileEntity tile
                        && tile.getMetaTileID() == blastFurnaceId(),
                    "controller missing");
                balances(p, false, true);
                if (stage == Stage.BATCH) require(
                    p.worldObj.getTileEntity(x + 2, y, z) instanceof IGregTechTileEntity second
                        && second.getMetaTileID() == blastFurnaceId(),
                    "second batch controller missing");

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
            case DISMANTLE -> {
                require(feedback.equals("removed") && built == 56, "dismantled " + built + " of 56 array blocks");
                require(
                    p.worldObj.isAirBlock(x, y, z) && p.worldObj.isAirBlock(x + 2, y, z),
                    "dismantling left a controller in the world");
                int recoveredCasings = need.get(casing) + 1;
                require(
                    gridCount(p, casing) == recoveredCasings && total(p, casing) == recoveredCasings,
                    "recovered casings did not go straight to ME storage");
                require(
                    ((IAEItemPowerStorage) terminal.getItem()).getAECurrentPower(terminal) < chargeBefore,
                    "dismantling did not use the wireless terminal");
                finish(null);
            }
            default -> throw new AssertionError(stage);
        }
    }

    private static int blastFurnaceId() {
        return ProjectorMachines.idOf(ItemList.Machine_Multi_BlastFurnace.get(1));
    }

    private void putHeat(EntityPlayerMP player, int wx, int wy, int wz) {
        var heat = GameRegistry.findBlock("Railcraft", "residual.heat");
        require(
            heat != null && player.worldObj.setBlock(wx, wy, wz, heat)
                && player.worldObj.getTileEntity(wx, wy, wz) != null,
            "installed Railcraft heat");
    }

    private final class MaterialCraftingScenario {

        private static final int BACKPACK_CASINGS = 3;
        private static final int STORED_CASINGS = 4;

        private List<CraftOrder> orders;
        private final int[] processed = new int[3];
        private TileInterface provider;
        private TileEntityChest processingChest;
        private int completeTicks;

        private void setUp(EntityPlayerMP player, int x, int y, int z) {
            var defs = AEApi.instance()
                .definitions();
            place(
                player,
                x + 2,
                y,
                z,
                defs.blocks()
                    .craftingStorage1k());
            place(
                player,
                x + 3,
                y,
                z,
                defs.blocks()
                    .iface());
            provider = (TileInterface) player.worldObj.getTileEntity(x + 3, y, z);
            provider.setSide(ForgeDirection.WEST);
            player.worldObj.setBlock(x + 4, y, z, Blocks.chest);
            processingChest = (TileEntityChest) player.worldObj.getTileEntity(x + 4, y, z);
            var otherMaterials = shape.materials.stream()
                .map(ProjectorStructure.Material::stack)
                .filter(stack -> !casing.isItemEqual(stack))
                .toList();
            require(otherMaterials.size() == 3, "fixture has four different material rows");
            orders = List.of(
                new CraftOrder(casing, Items.stick, BACKPACK_CASINGS + STORED_CASINGS),
                new CraftOrder(otherMaterials.get(0), Items.paper, 0),
                new CraftOrder(otherMaterials.get(1), Items.string, need.get(otherMaterials.get(1))));
            for (int i = 0; i < orders.size(); i++) {
                CraftOrder order = orders.get(i);
                ItemStack pattern = defs.items()
                    .encodedPattern()
                    .maybeStack(1)
                    .get();
                NBTTagCompound data = new NBTTagCompound();
                NBTTagList inputs = new NBTTagList();
                inputs.appendTag(
                    AEItemStack.create(new ItemStack(order.input()))
                        .toNBTGeneric());
                data.setTag("in", inputs);
                NBTTagList outputs = new NBTTagList();
                outputs.appendTag(
                    AEItemStack.create(order.output())
                        .setStackSize(1)
                        .toNBTGeneric());
                data.setTag("out", outputs);
                data.setBoolean("crafting", false);
                pattern.setTagCompound(data);
                provider.getPatterns()
                    .setInventorySlotContents(i, pattern);
            }
        }

        private boolean ready(EntityPlayerMP player) throws Exception {
            ICraftingGrid crafting = access.getGrid()
                .getCache(ICraftingGrid.class);
            if (crafting.getCpus()
                .isEmpty()) return false;
            for (CraftOrder order : orders) {
                if (!crafting.getCraftingFor(AEItemStack.create(order.output()), null, 0, player.worldObj)
                    .isEmpty()) continue;
                provider.getInterfaceDuality()
                    .updateCraftingList();
                return false;
            }
            return true;
        }

        private void seed(EntityPlayerMP player) {
            give(player, casing, BACKPACK_CASINGS);
            for (CraftOrder order : orders) {
                inject(new ItemStack(order.input()), 32);
                int stored = order.initiallyAvailable() - (order.output()
                    .isItemEqual(casing) ? BACKPACK_CASINGS : 0);
                if (stored > 0) inject(order.output(), stored);
            }
        }

        private void checkClient(ProjectorGui gui, List<IconRowList.Row> rows) throws Exception {
            for (var row : rows) {
                if (row.header()) continue;
                var order = orders.stream()
                    .filter(
                        entry -> entry.output()
                            .isItemEqual(row.icon()))
                    .findFirst();
                int available = order.map(CraftOrder::initiallyAvailable)
                    .orElse(0);
                int required = need.get(row.icon());
                var badgeMethod = row.key()
                    .getClass()
                    .getDeclaredMethod("craftable");
                badgeMethod.setAccessible(true);
                boolean badged = (boolean) badgeMethod.invoke(row.key());
                if (!row.detail()
                    .equals(available + "/" + required)
                    || row.color() != (available >= required ? IconRowList.GOOD : IconRowList.BAD)
                    || badged != order.isPresent()) {
                    stableStockTicks = 0;
                    return;
                }
            }
            if (++stableStockTicks < 6) return;
            var mc = Minecraft.getMinecraft();
            String screenshot = "materials-crafting.png";
            net.minecraft.util.ScreenShotHelper.saveScreenshot(
                new java.io.File("."),
                screenshot,
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            require(Files.isRegularFile(Path.of("screenshots", screenshot)), "material crafting screenshot written");
            AE2Button craft = WidgetTree.findFirstWithName(gui, "craft_missing_materials", AE2Button.class);
            require(craft != null && craft.areAncestorsEnabled(), "one-click crafting button on material page");
            craft.onMousePressed(0);
            craft.onMousePressed(0);
            stockChecked = true;
        }

        private void checkServer(EntityPlayerMP player) {
            for (int slot = 0; slot < processingChest.getSizeInventory(); slot++) {
                ItemStack stack = processingChest.getStackInSlot(slot);
                if (stack == null) continue;
                boolean matched = false;
                for (int i = 0; i < orders.size(); i++) {
                    CraftOrder order = orders.get(i);
                    if (stack.getItem() != order.input()) continue;
                    processed[i] += stack.stackSize;
                    require(processed[i] <= missing(order), "duplicate or surplus order for " + order.output());
                    processingChest.setInventorySlotContents(slot, null);
                    inject(order.output(), stack.stackSize);
                    matched = true;
                    break;
                }
                require(matched, "unexpected processing input " + stack);
            }
            for (int i = 0; i < orders.size(); i++) if (processed[i] != missing(orders.get(i))) return;
            if (++completeTicks < 40) return;
            for (var entry : need.entrySet()) {
                boolean hasPattern = orders.stream()
                    .anyMatch(
                        order -> order.output()
                            .isItemEqual(entry.getKey()));
                require(
                    total(player, entry.getKey()) == (hasPattern ? entry.getValue() : 0),
                    "final material balance for " + entry.getKey());
            }
            System.out.println(
                "[projector-material-crafting-qa] synchronized craftable markers and verified "
                    + Arrays.toString(processed)
                    + " exact missing outputs via the real ME CPU");
            finish(null);
        }

        private int missing(CraftOrder order) {
            return need.get(order.output()) - order.initiallyAvailable();
        }

        private void inject(ItemStack stack, int count) {
            require(
                me.injectItems(
                    AEItemStack.create(stack)
                        .setStackSize(count),
                    Actionable.MODULATE,
                    new MachineSource(provider)) == null,
                "inject into crafting network: " + stack);
        }

        private static void place(EntityPlayerMP player, int x, int y, int z, ITileDefinition def) {
            ItemStack stack = def.maybeStack(1)
                .get();
            player.worldObj.setBlock(x, y, z, Block.getBlockFromItem(stack.getItem()), stack.getItemDamage(), 3);
        }

        private record CraftOrder(ItemStack output, Item input, int initiallyAvailable) {}
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
