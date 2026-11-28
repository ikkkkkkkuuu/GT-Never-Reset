package com.xyp.gtneverreset.qa;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.common.projector.ProjectorBuilder;
import com.xyp.gtneverreset.common.projector.ProjectorMachines;
import com.xyp.gtneverreset.common.projector.ProjectorOrientation;
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

@Mod(
    modid = "gtnrprojectorhatchfacingqa",
    name = "Projector hatch facing QA",
    version = "1",
    dependencies = "required-after:gtneverreset")
public final class ProjectorHatchFacingChecks {

    private static final int BUILD_REQUEST_INTERVAL = 40;
    private static final Comparator<ChunkCoordinates> PLACEMENT_ORDER = Comparator
        .comparingInt((ChunkCoordinates p) -> p.posY)
        .thenComparingInt(p -> p.posZ)
        .thenComparingInt(p -> p.posX);

    private MinecraftServer server;
    private WorldServer world;
    private EntityPlayerMP player;
    private ProjectorPlan plan;
    private HatchSite site;
    private int hatchId;
    private long requestedAt;
    private Phase phase;
    private ForgeDirection frontAtPlacement;
    private boolean pendingWasAir;
    private IGregTechTileEntity existing;
    private Block supplementBlock;
    private int supplementMeta;

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
            player.theItemInWorldManager.setGameType(GameType.CREATIVE);
            player.capabilities.allowEdit = true;

            var spawn = world.getSpawnPoint();
            plan = new ProjectorPlan();
            plan.selectMachine(ProjectorMachines.idOf(ItemList.Machine_Multi_BlastFurnace.get(1)));
            plan.state = ProjectorPlan.State.LOCKED;
            plan.dimension = 0;
            plan.x = spawn.posX;
            plan.y = 12;
            plan.z = spawn.posZ;
            plan.facing = ExtendedFacing.of(ForgeDirection.EAST);
            plan.buildStyle = StructureDeploymentStyle.NONE;
            hatchId = ProjectorMachines.idOf(ItemList.Hatch_Energy_MV.get(1));
            require(hatchId > 0 && ProjectorMachines.isHatch(hatchId), "test energy hatch unavailable");
            site = configureSite();
            for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                ChunkCoordinates adjacent = adjacent(site.hatch, side);
                if (side != site.open && !site.solids.contains(adjacent))
                    world.setBlock(adjacent.posX, adjacent.posY, adjacent.posZ, Blocks.stone, 0, 3);
            }
            require(blockAt(adjacent(site.hatch, site.preferred)) == Blocks.stone, "template side was not blocked");
            require(isAir(adjacent(site.hatch, site.open)), "expected external face is not exposed");
            require(isAir(site.pending), "planned neighbor must initially be air");

            ItemStack projector = new ItemStack(GameRegistry.findItem("gtneverreset", "multiblock_projector"));
            plan.write(projector);
            player.inventory.setInventorySlotContents(player.inventory.currentItem, projector);
            player.setPosition(plan.x + 0.5, plan.y, plan.z - 6);
            FMLCommonHandler.instance()
                .bus()
                .register(this);
            MinecraftForge.EVENT_BUS.register(this);
            request(Phase.BUILDING);
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    @SubscribeEvent
    public void placed(BlockEvent.PlaceEvent event) {
        ChunkCoordinates position = site == null ? null : site.hatch;
        if (phase != Phase.BUILDING || event.player != player
            || position == null
            || event.x != position.posX
            || event.y != position.posY
            || event.z != position.posZ
            || !(world.getTileEntity(event.x, event.y, event.z) instanceof IGregTechTileEntity hatch)) return;
        frontAtPlacement = hatch.getFrontFacing();
        pendingWasAir = isAir(site.pending);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || phase == null) return;
        try {
            long elapsed = world.getTotalWorldTime() - requestedAt;
            require(elapsed < 480, "projector did not finish hatch test phase " + phase);
            ChunkCoordinates position = site.hatch;
            var current = world.getTileEntity(position.posX, position.posY, position.posZ);
            if (phase == Phase.BUILDING) {
                if (!(world.getTileEntity(plan.x, plan.y, plan.z) instanceof IGregTechTileEntity controller)) return;
                require(controller.getFrontFacing() == plan.facing.getDirection(), "rotated controller changed facing");
                require(current instanceof IGregTechTileEntity, "projector did not place the selected hatch");
                var hatch = (IGregTechTileEntity) current;
                require(hatch.getMetaTileID() == hatchId, "projector used the wrong hatch");
                require(pendingWasAir, "planned neighbor was already solid during hatch placement");
                require(
                    frontAtPlacement == site.open,
                    "hatch selected " + frontAtPlacement + " instead of " + site.open);
                require(!isAir(site.pending), "planned neighbor was not filled after hatch placement");
                require(hatch.getFrontFacing() == site.open, "hatch did not keep the exposed face after building");
                if (elapsed < BUILD_REQUEST_INTERVAL + 1) return;
                hatch.setFrontFacing(site.preferred);
                require(hatch.getFrontFacing() == site.preferred, "could not turn the existing hatch for the test");
                existing = hatch;
                ChunkCoordinates casing = site.supplement;
                supplementBlock = blockAt(casing);
                supplementMeta = world.getBlockMetadata(casing.posX, casing.posY, casing.posZ);
                require(supplementBlock != Blocks.air, "test casing was not built");
                world.setBlockToAir(casing.posX, casing.posY, casing.posZ);
                request(Phase.SUPPLEMENTING);
            } else {
                ChunkCoordinates casing = site.supplement;
                if (blockAt(casing) != supplementBlock) return;
                require(
                    world.getBlockMetadata(casing.posX, casing.posY, casing.posZ) == supplementMeta,
                    "supplement built the wrong casing metadata");
                require(current == existing, "supplement replaced an existing hatch");
                require(existing.getFrontFacing() == site.preferred, "supplement reoriented an existing hatch");
                finish(null);
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private HatchSite configureSite() {
        ProjectorStructure template = ProjectorSimulation.run(plan.settings());
        Set<ChunkCoordinates> solids = new HashSet<>();
        Set<ChunkCoordinates> plannedAir = new HashSet<>();
        ChunkCoordinates[] positions = new ChunkCoordinates[template.size];
        List<ChunkCoordinates> casings = new ArrayList<>();
        for (int i = 0; i < template.size; i++) {
            ChunkCoordinates position = position(template.a[i], template.b[i], template.c[i]);
            positions[i] = position;
            Block block = template.blocks[i];
            if (block == Blocks.air) plannedAir.add(position);
            else {
                solids.add(position);
                if (template.machines[i] == 0 && template.items[i] != null) casings.add(position);
            }
        }
        for (long key : template.emptyCells) {
            plannedAir
                .add(position(ProjectorPlan.unpackA(key), ProjectorPlan.unpackB(key), ProjectorPlan.unpackC(key)));
        }
        ChunkCoordinates controller = position(0, 0, 0);
        for (int i = 0; i < template.size; i++) {
            ChunkCoordinates hatch = positions[i];
            var slot = template.slotAt(i);
            if (slot == null || !solids.contains(hatch)
                || !Arrays.stream(slot.candidates())
                    .anyMatch(id -> id == hatchId))
                continue;
            if (Arrays.stream(ForgeDirection.VALID_DIRECTIONS)
                .anyMatch(side -> plannedAir.contains(adjacent(hatch, side)))) continue;

            int a = template.a[i], b = template.b[i], c = template.c[i];
            long key = ProjectorPlan.pack(a, b, c);
            plan.overrides.put(key, hatchId);
            ProjectorStructure fitted = ProjectorSimulation.run(plan.settings());
            int index = fitted.indexOf(a, b, c);
            require(index >= 0 && fitted.machines[index] == hatchId, "projection did not accept the fixture hatch");
            int[][] simulated = fitted.simulationPositions();
            var tile = (IGregTechTileEntity) fitted.world
                .getTileEntity(simulated[0][index], simulated[1][index], simulated[2][index]);
            ForgeDirection preferred = ProjectorOrientation
                .changeBasis(tile.getFrontFacing(), fitted.canonical, plan.facing);
            if (preferred != ForgeDirection.UNKNOWN && !solids.contains(adjacent(hatch, preferred))) {
                for (ForgeDirection open : ForgeDirection.VALID_DIRECTIONS) {
                    if (open.offsetY != 0 || open == preferred || solids.contains(adjacent(hatch, open))) continue;
                    for (ForgeDirection side : ProjectorOrientation.PREFERRED_FRONTS) {
                        if (side == open) break;
                        ChunkCoordinates pending = adjacent(hatch, side);
                        if (!solids.contains(pending)
                            || !pending.equals(controller) && PLACEMENT_ORDER.compare(pending, hatch) <= 0) continue;
                        ChunkCoordinates casing = casings.stream()
                            .filter(point -> !point.equals(hatch))
                            .findFirst()
                            .orElseThrow(() -> new AssertionError("no ordinary casing for supplement"));
                        return new HatchSite(hatch, pending, casing, solids, preferred, open);
                    }
                }
            }
            plan.overrides.remove(key);
        }
        throw new AssertionError("no rotated hatch slot with a blocked template face and a later neighbor");
    }

    private void request(Phase next) {
        requestedAt = world.getTotalWorldTime();
        phase = next;
        ProjectorBuilder.request(player, 0, false);
    }

    private ChunkCoordinates position(int a, int b, int c) {
        int[] offset = new int[3];
        plan.facing.getWorldOffset(new int[] { a, b, c }, offset);
        return new ChunkCoordinates(plan.x + offset[0], plan.y + offset[1], plan.z + offset[2]);
    }

    private static ChunkCoordinates adjacent(ChunkCoordinates point, ForgeDirection side) {
        return new ChunkCoordinates(point.posX + side.offsetX, point.posY + side.offsetY, point.posZ + side.offsetZ);
    }

    private boolean isAir(ChunkCoordinates point) {
        return world.isAirBlock(point.posX, point.posY, point.posZ);
    }

    private Block blockAt(ChunkCoordinates point) {
        return world.getBlock(point.posX, point.posY, point.posZ);
    }

    private void finish(Throwable failure) throws Exception {
        phase = null;
        try {
            Files.writeString(
                Path.of(System.getProperty("gtnr.projector.server.result")),
                failure == null ? "PASS\n" : "FAIL " + failure + "\n");
            if (failure != null) throw new AssertionError("projector hatch facing regression", failure);
        } finally {
            server.initiateShutdown();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private enum Phase {
        BUILDING,
        SUPPLEMENTING
    }

    private static final class HatchSite {

        private final ChunkCoordinates hatch, pending, supplement;
        private final Set<ChunkCoordinates> solids;
        private final ForgeDirection preferred, open;

        private HatchSite(ChunkCoordinates hatch, ChunkCoordinates pending, ChunkCoordinates supplement,
            Set<ChunkCoordinates> solids, ForgeDirection preferred, ForgeDirection open) {
            this.hatch = hatch;
            this.pending = pending;
            this.supplement = supplement;
            this.solids = solids;
            this.preferred = preferred;
            this.open = open;
        }
    }
}
