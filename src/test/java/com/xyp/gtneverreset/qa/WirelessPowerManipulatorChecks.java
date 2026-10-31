package com.xyp.gtneverreset.qa;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.recursive_pineapple.matter_manipulator.common.building.BlockAnalyzer;
import com.recursive_pineapple.matter_manipulator.common.building.BlockSpec;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBuild;
import com.recursive_pineapple.matter_manipulator.common.building.PendingMove;
import com.recursive_pineapple.matter_manipulator.common.items.MMItemList;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.ItemMatterManipulator;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.ItemMatterManipulator.ManipulatorTier;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Location;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;
import com.xyp.gtneverreset.common.wireless.IWirelessPowerReceiver;
import com.xyp.gtneverreset.common.wireless.WirelessPowerSavedData;
import com.xyp.gtneverreset.common.wireless.WirelessPowerTransfer;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.common.misc.WirelessNetworkManager;

@Mod(modid = "wirelesspowermmqa", name = "Wireless power MM QA", version = "1", dependencies = "after:gtneverreset")
public final class WirelessPowerManipulatorChecks {

    private final UUID source = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final UUID otherSource = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private boolean started, finished;
    private int ticks;
    private UUID machineId, hatchId, copyId, hatchCopyId;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || started) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        if (mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "wireless-power-mm-" + System.currentTimeMillis(),
                "Wireless power MM QA",
                new WorldSettings(71, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            World world = player.worldObj;
            WirelessPowerSavedData data = WirelessPowerSavedData.get();
            ticks++;
            if (ticks == 1) {
                player.playerNetServerHandler.setPlayerLocation(1, 12, -2, 0, 0);
                player.inventory.clearInventory(null, -1);
                player.inventory.mainInventory[0] = MMItemList.MK3.get();
                player.inventory.currentItem = 0;
                WirelessNetworkManager.strongCheckOrAddUser(source);
                WirelessNetworkManager.strongCheckOrAddUser(otherSource);
                WirelessNetworkManager.setUserEU(source, BigInteger.valueOf(1000000));
                WirelessNetworkManager.setUserEU(otherSource, BigInteger.valueOf(1000000));
                place(player, 0, meta -> meta instanceof MTEBasicMachine machine && machine.maxEUInput() == 32);
                place(player, 2, meta -> meta instanceof MTEHatchEnergy hatch && hatch.mTier == 1);
            }
            if (ticks == 10) {
                player.playerNetServerHandler.setPlayerLocation(1, 11, -2, 0, 0);
                require(WirelessPowerTransfer.supports(tile(world, 0)), "selected registered machine accepts EU");
                require(WirelessPowerTransfer.connect(tile(world, 0), player, source), "bind singleblock receiver");
                require(WirelessPowerTransfer.connect(tile(world, 2), player, source), "bind multiblock energy hatch");
                machineId = id(tile(world, 0));
                hatchId = id(tile(world, 2));
                data.toggle(hatchId, player.getUniqueID());
                world.getChunkFromBlockCoords(22, 0);
                MMState state = state(player);
                state.config.coordA = location(world, 0);
                state.config.coordB = location(world, 2);
                state.config.coordC = location(world, 20);
                PendingMove move = new PendingMove(player, state, ManipulatorTier.Tier3);
                move.tryPlaceBlocks(player.getHeldItem(), player);
                require(world.isAirBlock(0, 10, 0) && world.isAirBlock(2, 10, 0), "MM removed original blocks");
                require(
                    machineId.equals(id(tile(world, 20))) && hatchId.equals(id(tile(world, 22))),
                    "real MM move preserves both connection identities");
                require(
                    data.connections(player.getUniqueID())
                        .size() == 2 && data.connection(machineId).x == 20
                        && data.connection(hatchId).x == 22,
                    "device list updates coordinates immediately without losing moved records");
                require(
                    data.connection(hatchId)
                        .paused(),
                    "move preserves paused energy hatch");
            }
            if (ticks == 45) {
                require(
                    tile(world, 20).getStoredEU() > 0 && tile(world, 22).getStoredEU() == 0,
                    "moved machine receives EU while paused hatch remains paused");
                var region = BlockAnalyzer.analyzeRegion(world, location(world, 20), location(world, 22), true);
                List<PendingBlock> copies = new ArrayList<>();
                Transform transform = new Transform();
                transform.forward = ForgeDirection.EAST;
                for (PendingBlock analyzed : region.blocks) {
                    PendingBlock copy = analyzed.clone();
                    copy.transform(transform);
                    copy.x += 40;
                    copy.y += 10;
                    copies.add(copy);
                }
                build(player, copies);
                copyId = id(tile(world, 40));
                hatchCopyId = id(tile(world, 42));
                require(
                    copyId != null && hatchCopyId != null && !copyId.equals(machineId) && !hatchCopyId.equals(hatchId),
                    "MM rotated copies get independent bindings without a carried card");
                require(
                    data.connection(copyId).source.equals(source) && data.connection(hatchCopyId).source.equals(source)
                        && data.connection(hatchCopyId)
                            .paused(),
                    "copies retain original source and pause state");
                build(player, copies);
                require(copyId.equals(id(tile(world, 40))), "reapplying MM settings keeps existing matching identity");
                PendingBlock reset = copies.get(0)
                    .clone();
                reset.setBlock(reset.spec);
                reset.x = 44;
                build(player, List.of(reset));
                require(id(tile(world, 44)) == null, "reused pending block cannot leak old power settings");
            }
            if (ticks == 80) {
                require(
                    tile(world, 40).getStoredEU() > 0 && tile(world, 42).getStoredEU() == 0,
                    "actual copied machine receives EU, paused copy does not");
                var original = data.connection(machineId);
                NBTTagCompound snapshot = WirelessPowerTransfer.copySettings(tile(world, 20));
                world.setBlockToAir(20, 10, 0);
                data.connections(player.getUniqueID());
                require(
                    data.connection(machineId) == null && data.connection(copyId) != null,
                    "breaking original removes only original connection");
                WirelessPowerTransfer.pasteSettings(player, tile(world, 44), snapshot);
                require(
                    data.connection(id(tile(world, 44))).source.equals(original.source),
                    "captured settings remain usable after original is removed");
                var other = data.connect(tile(world, 40), player.getUniqueID(), otherSource);
                copyId = other.id;
                move(world, location(world, 22), location(world, 40));
                require(
                    hatchId.equals(id(tile(world, 40))) && copyId.equals(id(tile(world, 22)))
                        && data.connection(hatchId).x == 40
                        && data.connection(copyId).x == 22,
                    "MM swap preserves both owners and connections without clobbering either record");
                require(
                    data.connection(hatchId)
                        .paused() && data.connection(copyId).source.equals(otherSource),
                    "swap preserves independent source and pause settings");
                World nether = server.worldServerForDimension(-1);
                nether.getChunkFromBlockCoords(60, 0);
                nether.setBlockToAir(60, 10, 0);
                move(world, location(world, 22), location(nether, 60));
                require(
                    copyId.equals(id(tile(nether, 60))) && data.connection(copyId).dimension == -1,
                    "MM core cross-dimension move keeps source and updates dimension");
                world.getChunkFromBlockCoords(60, 0);
                move(world, location(nether, 60), location(world, 60));
                require(
                    copyId.equals(id(tile(world, 60))) && data.connection(copyId).dimension == 0,
                    "return move replaces previous dimension entry");
                NBTTagCompound saved = new NBTTagCompound();
                data.writeToNBT(saved);
                data.readFromNBT(saved);
                NBTTagCompound machine = new NBTTagCompound();
                tile(world, 60).writeToNBT(machine);
                world.removeTileEntity(60, 10, 0);
                world.setTileEntity(60, 10, 0, TileEntity.createAndLoadEntity(machine));
                tile(world, 60).setStoredEU(0);
                data.remove(hatchCopyId, player.getUniqueID());
                require(
                    data.connection(hatchId) != null && data.connection(hatchCopyId) == null,
                    "unlinking a copy does not unlink the moved original");
                BaseMetaTileEntity foreign = tile(world, 44);
                UUID foreignId = data.connect(foreign, UUID.randomUUID(), otherSource).id;
                WirelessPowerTransfer.pasteSettings(player, foreign, snapshot);
                require(foreignId.equals(id(foreign)), "MM copy cannot replace another player's binding");
            }
            if (ticks == 115) {
                require(
                    copyId.equals(id(tile(world, 60))) && tile(world, 60).getStoredEU() > 0,
                    "moved connection survives world and tile NBT reload and continues receiving EU");
                require(
                    WirelessNetworkManager.getUserEU(source)
                        .compareTo(BigInteger.valueOf(1000000)) < 0
                        && WirelessNetworkManager.getUserEU(otherSource)
                            .compareTo(BigInteger.valueOf(1000000)) < 0,
                    "receivers debit their selected real wireless accounts");
                finish("PASS");
            }
            if (ticks > 200) throw new AssertionError("MM QA timed out");
        } catch (Throwable failure) {
            failure.printStackTrace();
            finish("FAIL");
        }
    }

    private static void build(EntityPlayerMP player, List<PendingBlock> blocks) {
        for (PendingBlock block : blocks) player.worldObj.getChunkFromBlockCoords(block.x, block.z);
        PendingBuild build = new PendingBuild(player, state(player), ManipulatorTier.Tier3, blocks);
        for (int i = 0; i < blocks.size() + 1; i++) build.tryPlaceBlocks(player.getHeldItem(), player);
    }

    private static MMState state(EntityPlayerMP player) {
        MMState state = new MMState();
        state.manipulator = (ItemMatterManipulator) player.getHeldItem()
            .getItem();
        return state;
    }

    private static void move(World world, Location source, Location target) {
        World sourceWorld = source.getWorld(), targetWorld = target.getWorld();
        require(
            PendingMove.moveBlock(
                null,
                world,
                source,
                BlockSpec.fromBlock(null, sourceWorld, source.x, source.y, source.z),
                target,
                BlockSpec.fromBlock(null, targetWorld, target.x, target.y, target.z)),
            "real MM core move succeeds");
    }

    private static Location location(World world, int x) {
        return new Location(world, x, 10, 0);
    }

    private static BaseMetaTileEntity tile(World world, int x) {
        return (BaseMetaTileEntity) world.getTileEntity(x, 10, 0);
    }

    private static UUID id(BaseMetaTileEntity tile) {
        return ((IWirelessPowerReceiver) tile).gtnr$powerConnection();
    }

    private static void place(EntityPlayerMP player, int x, Predicate<MetaTileEntity> predicate) {
        for (var meta : GregTechAPI.METATILEENTITIES) {
            if (!(meta instanceof MetaTileEntity candidate) || !predicate.test(candidate)) continue;
            var stack = candidate.getStackForm(1);
            player.worldObj.setBlock(x, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
            BaseMetaTileEntity tile = tile(player.worldObj, x);
            tile.setInitialValuesAsNBT(stack.getTagCompound(), (short) stack.getItemDamage());
            tile.setOwnerName(player.getCommandSenderName());
            tile.setOwnerUuid(player.getUniqueID());
            return;
        }
        throw new AssertionError("Missing QA receiver");
    }

    private void finish(String result) {
        finished = true;
        System.out.println("WIRELESS_POWER_MM_QA: " + result);
        Minecraft.getMinecraft()
            .func_152344_a(
                () -> Minecraft.getMinecraft()
                    .shutdown());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("WIRELESS_POWER_MM_QA: " + message);
    }
}
