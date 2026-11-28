package com.xyp.gtneverreset.qa;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.common.packet.ProjectorBuildActionPacket;
import com.xyp.gtneverreset.common.packet.ProjectorBuildActionPacket.Action;
import com.xyp.gtneverreset.common.projector.ProjectorBuildLedger;
import com.xyp.gtneverreset.common.projector.ProjectorBuilder;
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorSimulation;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtnrlib.animation.StructureDeploymentStyle;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import io.netty.buffer.Unpooled;

@Mod(
    modid = "gtnrprojectorconstructionqa",
    name = "Projector construction action QA",
    version = "1",
    dependencies = "required-after:gtneverreset")
public final class ProjectorConstructionActionChecks {

    private static final String LEDGER_NAME = "GTNR_ProjectorBuilds";
    private static final int Y = 12;
    private MinecraftServer server;
    private WorldServer world;
    private EntityPlayerMP player;
    private ProjectorPlan plan;
    private List<Cell> casings;
    private Phase phase;
    private long started;
    private boolean deniedBreak;

    @Mod.EventHandler
    public void ready(FMLServerStartedEvent event) throws Exception {
        server = MinecraftServer.getServer();
        try {
            world = server.worldServerForDimension(0);
            player = FakePlayerFactory.getMinecraft(world);
            new NetHandlerPlayServer(server, new NetworkManager(false) {

                @Override
                public boolean isChannelOpen() {
                    return true;
                }
            }, player) {

                @Override
                public void sendPacket(Packet packet) {}
            };
            var spawn = world.getSpawnPoint();
            plan = new ProjectorPlan();
            plan.selectMachine(ProjectorMachines.idOf(ItemList.Machine_Multi_BlastFurnace.get(1)));
            plan.state = ProjectorPlan.State.LOCKED;
            plan.dimension = 0;
            plan.x = spawn.posX;
            plan.y = Y;
            plan.z = spawn.posZ;
            plan.facing = ExtendedFacing.of(ForgeDirection.NORTH);
            plan.buildStyle = StructureDeploymentStyle.NONE;
            player.setPosition(plan.x + 0.5, Y, plan.z - 4);
            ItemStack projector = new ItemStack(GameRegistry.findItem("gtneverreset", "multiblock_projector"));
            plan.write(projector);
            player.inventory.setInventorySlotContents(player.inventory.currentItem, projector);
            casings = casingPositions();
            place(casings.get(0));
            Cell obstructed = casings.get(1);
            world.setBlock(obstructed.x, obstructed.y, obstructed.z, Blocks.dirt, 0, 3);
            FMLCommonHandler.instance()
                .bus()
                .register(this);
            MinecraftForge.EVENT_BUS.register(this);
            setMode(GameType.SURVIVAL);
            ProjectorBuilder.request(player, 0, false);
            transition(Phase.MATERIAL_CHECK);
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || phase == null) return;
        try {
            long elapsed = world.getTotalWorldTime() - started;
            require(elapsed < 480, "projector did not complete phase " + phase);
            Cell obstruction = casings.get(1);
            switch (phase) {
                case MATERIAL_CHECK -> {
                    if (elapsed < 42) return;
                    require(controllerIsAir(), "materials shortage placed the controller");
                    require(blockAt(obstruction) == Blocks.dirt, "materials shortage cleared an obstruction");
                    setMode(GameType.CREATIVE);
                    ProjectorBuilder.request(player, 0, false);
                    action(Action.CANCEL);
                    transition(Phase.CANCELLED);
                }
                case CANCELLED -> {
                    if (elapsed < 42) return;
                    require(controllerIsAir(), "cancelled build placed the controller");
                    require(blockAt(obstruction) == Blocks.dirt, "cancelled build cleared an obstruction");
                    ProjectorBuilder.request(player, 0, false);
                    transition(Phase.BUILDING);
                }
                case BUILDING -> {
                    if (!(world.getTileEntity(plan.x, Y, plan.z) instanceof IGregTechTileEntity controller)) return;
                    require(controller.getMetaTileID() == plan.machine, "wrong controller was placed");
                    require(matches(casings.get(0)), "pre-existing casing was changed");
                    require(matches(obstruction), "ordinary obstruction was not cleared and built over");
                    Cell changed = casings.get(2);
                    require(matches(changed), "expected casing was not placed");
                    world.setBlock(changed.x, changed.y, changed.z, Blocks.gold_block, 0, 3);
                    reloadLedger();
                    action(Action.UNDO);
                    transition(Phase.UNDOING);
                }
                case UNDOING -> {
                    if (elapsed < 41) return;
                    require(controllerIsAir(), "undo did not remove the recorded controller");
                    require(blockAt(obstruction) == Blocks.air, "undo did not remove the recorded casing");
                    require(matches(casings.get(0)), "undo removed a hand-placed casing");
                    require(blockAt(casings.get(2)) == Blocks.gold_block, "undo removed a changed block");
                    place(casings.get(3));
                    setMode(GameType.SURVIVAL);
                    transition(Phase.DISMANTLING);
                    action(Action.DISMANTLE);
                }
                case DISMANTLING -> {
                    if (!deniedBreak || blockAt(casings.get(0)) != Blocks.air || elapsed < 4) return;
                    require(matches(casings.get(3)), "dismantle ignored Forge break protection");
                    require(blockAt(casings.get(2)) == Blocks.gold_block, "dismantle removed an unrelated block");
                    Item recovered = Item.getItemFromBlock(casings.get(0).block);
                    boolean received = false;
                    for (ItemStack stack : player.inventory.mainInventory)
                        if (stack != null && stack.getItem() == recovered) received = true;
                    require(received, "dismantle did not return the recovered casing");
                    finish(null);
                }
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    @SubscribeEvent
    public void denyOneBreak(BlockEvent.BreakEvent event) {
        if (phase != Phase.DISMANTLING || event.getPlayer() != player) return;
        Cell denied = casings.get(3);
        if (event.x == denied.x && event.y == denied.y && event.z == denied.z) {
            deniedBreak = true;
            event.setCanceled(true);
        }
    }

    private List<Cell> casingPositions() {
        ProjectorStructure shape = ProjectorSimulation.run(plan.settings());
        int[] offset = new int[3];
        List<Cell> found = new ArrayList<>();
        for (int i = 0; i < shape.size && found.size() < 4; i++) {
            var block = shape.blocks[i];
            int meta = shape.metas[i];
            if (shape.machines[i] != 0 || meta == ProjectorStructure.ANY_META
                || block == Blocks.air
                || block.hasTileEntity(meta)
                || Item.getItemFromBlock(block) == null) continue;
            plan.facing.getWorldOffset(new int[] { shape.a[i], shape.b[i], shape.c[i] }, offset);
            int x = plan.x + offset[0], y = Y + offset[1], z = plan.z + offset[2];
            if (world.isAirBlock(x, y, z)) found.add(new Cell(x, y, z, block, meta));
        }
        require(found.size() == 4, "test blueprint did not contain four unobstructed casing positions");
        return found;
    }

    private void reloadLedger() {
        var stored = (ProjectorBuildLedger) world.mapStorage.loadData(ProjectorBuildLedger.class, LEDGER_NAME);
        require(stored != null, "construction did not create a saved ledger");
        NBTTagCompound nbt = new NBTTagCompound();
        stored.writeToNBT(nbt);
        ProjectorBuildLedger reloaded = new ProjectorBuildLedger(LEDGER_NAME);
        reloaded.readFromNBT(nbt);
        world.mapStorage.setData(LEDGER_NAME, reloaded);
    }

    private void action(Action action) {
        var bytes = Unpooled.buffer();
        try {
            new ProjectorBuildActionPacket(0, action, false).toBytes(bytes);
            var received = new ProjectorBuildActionPacket();
            received.fromBytes(bytes);
            require(!received.isInvalid() && !bytes.isReadable(), "invalid construction action packet");
            received.handleServer(player);
        } finally {
            bytes.release();
        }
    }

    private void transition(Phase nextPhase) {
        phase = nextPhase;
        started = world.getTotalWorldTime();
    }

    private void setMode(GameType mode) {
        player.theItemInWorldManager.setGameType(mode);
        player.capabilities.allowEdit = true;
    }

    private boolean controllerIsAir() {
        return world.isAirBlock(plan.x, Y, plan.z);
    }

    private Block blockAt(Cell cell) {
        return world.getBlock(cell.x, cell.y, cell.z);
    }

    private boolean matches(Cell cell) {
        return blockAt(cell) == cell.block && world.getBlockMetadata(cell.x, cell.y, cell.z) == cell.meta;
    }

    private void place(Cell cell) {
        world.setBlock(cell.x, cell.y, cell.z, cell.block, cell.meta, 3);
    }

    private void finish(Throwable failure) throws Exception {
        phase = null;
        try {
            Files.writeString(
                Path.of(System.getProperty("gtnr.projector.server.result")),
                failure == null ? "PASS\n" : "FAIL " + failure + "\n");
            if (failure != null) throw new AssertionError("projector construction action regression", failure);
        } finally {
            server.initiateShutdown();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private enum Phase {
        MATERIAL_CHECK,
        CANCELLED,
        BUILDING,
        UNDOING,
        DISMANTLING
    }

    private static final class Cell {

        private final int x, y, z;
        private final Block block;
        private final int meta;

        private Cell(int x, int y, int z, Block block, int meta) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.meta = meta;
        }
    }
}
