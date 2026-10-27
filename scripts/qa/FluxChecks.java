package com.xyp.gtneverreset.qa;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityFluxConnector;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityFluxPlug;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityFluxPoint;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;
import com.xyp.gtneverreset.utils.machine.flux.FluxDropData;
import com.xyp.gtneverreset.utils.machine.flux.FluxTransferBuffer;
import com.xyp.gtneverreset.utils.machine.flux.FluxTransferScheduler;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMaps;
import gregtech.common.misc.WirelessNetworkManager;

public final class FluxChecks {

    private TileEntityFluxPlug plug;
    private TileEntityFluxPoint point;
    private BaseMetaTileEntity sink;
    private int assertions;

    public void setup(EntityPlayerMP player) throws Exception {
        verifyBuffer();
        verifyPersistence();
        verifyRecipes();
        World world = player.worldObj;
        verifyScheduler(world);
        nativeTile(world, -1, ItemList.Hatch_Dynamo_LV.get(1), ForgeDirection.EAST);
        sink = nativeTile(world, 3, ItemList.Hatch_Energy_LV.get(1), ForgeDirection.WEST);
        place(world, 0, GTNRItemList.FluxPlug.get(1), player);
        place(world, 2, GTNRItemList.FluxPoint.get(1), player);
        plug = (TileEntityFluxPlug) world.getTileEntity(0, 10, 0);
        point = (TileEntityFluxPoint) world.getTileEntity(2, 10, 0);
        FluxTransferScheduler.remove(point);
        new FluxTransferScheduler().onChunkLoad(new ChunkEvent.Load(world.getChunkFromBlockCoords(2, 0)));
        require(nodes().contains(point), "loading a dormant chunk restores its existing connector to the scheduler");
        require(plug.voltage() == 32 && point.voltage() == 32, "placement detects native LV endpoint ratings");
        require(plug.canConfigure(player) && point.canConfigure(player), "placement binds connectors to actual player");
        point.toggleEnabled();
        player.playerNetServerHandler.setPlayerLocation(1.5, 10, -2.5, 0, 15);
    }

    public void verifyNative(EntityPlayerMP player) {
        WirelessNetworkManager.setUserEU(player.getUniqueID(), BigInteger.valueOf(1000));
        plug.configure(32, 3);
        require(plug.injectEnergyUnits(ForgeDirection.WEST, 32, 2) == 2, "plug accepts two valid GT packets");
        require(plug.injectEnergyUnits(ForgeDirection.UP, 32, 20) == 1, "all plug faces share one amperage budget");
        require(plug.injectEnergyUnits(ForgeDirection.NORTH, 128, 1) == 0, "plug rejects overvoltage packets");
        require(plug.stored() == 96, "plug buffer contains exactly accepted EU");
        plug.updateEntity();
        require(plug.stored() == 0 && balance(player) == 1096, "plug settles accepted EU exactly once");
        plug.updateEntity();
        require(balance(player) == 1096, "second settlement cannot duplicate EU");
        point.configure(32, 2);
        sink.setStoredEU(0);
        point.toggleEnabled();
        long total = balance(player) + sink.getStoredEU();
        point.updateEntity();
        require(sink.getStoredEU() == 64, "point delivers two whole LV packets to a native GT energy hatch");
        require(balance(player) + sink.getStoredEU() + point.stored() == total, "native GT delivery conserves EU");
        sink.setStoredEU(sink.getEUCapacity());
        long before = balance(player);
        point.updateEntity();
        require(balance(player) == before && point.stored() == 0, "full receiver refunds every rejected EU");
        point.toggleEnabled();
        require(
            !point.outputsEnergyTo(ForgeDirection.EAST) && point.outputsEnergyTo(ForgeDirection.EAST, false),
            "disabled point preserves physical cable topology without delivering EU");
        verifyBreakRefund(player);
        System.out.println("FLUX_QA server checks: " + assertions + " assertions");
    }

    public TileEntityFluxPoint point() {
        return point;
    }

    private void verifyBuffer() {
        FluxTransferBuffer buffer = new FluxTransferBuffer();
        buffer.beginTick(10);
        require(
            buffer.receive(8, 2, 32, 3) == 2 && buffer.receive(32, 9, 32, 3) == 1,
            "mixed packet voltages share the configured current budget");
        require(buffer.stored() == 48 && buffer.receive(1, 99, 32, 3) == 0, "tick amperage cannot be exceeded");
        buffer.remove(48);
        buffer.beginTick(10);
        require(buffer.receive(32, 1, 32, 3) == 0, "settlement does not reset the same-tick budget");
        buffer.beginTick(11);
        require(buffer.receive(32, 3, 32, 3) == 3, "the next tick resets current budget");
        buffer.restore(8192);
        buffer.beginTick(12);
        require(
            buffer.receive(32, 1, 32, 1) == 0 && buffer.stored() == 8192,
            "lower limits preserve cargo and prevent additional input");
        buffer.remove(8192);
        require(
            buffer.receive(0, 1, 32, 4) == 0 && buffer.receive(-1, 1, 32, 4) == 0
                && buffer.receive(128, 1, 32, 4) == 0
                && buffer.receive(32, Long.MIN_VALUE, 32, 4) == 0,
            "invalid packet arithmetic cannot change EU");
        buffer.beginTick(13);
        require(
            buffer.receive(2_147_483_648L, Long.MAX_VALUE, 2_147_483_648L, 1_048_576) == 1_048_576,
            "large offers stay within whole-packet limits without overflow");
        require(buffer.stored() == 2_147_483_648L * 1_048_576, "large packet EU is exact");
    }

    private void verifyPersistence() {
        NBTTagCompound saved = new NBTTagCompound();
        saved.setLong("fluxBuffer", 987654321);
        saved.setLong("fluxVoltage", Long.MAX_VALUE);
        saved.setLong("fluxAmperage", Long.MAX_VALUE);
        saved.setInteger("fluxPriority", Integer.MIN_VALUE);
        saved.setString("fluxOwner", "invalid UUID");
        TileEntityFluxPlug loaded = new TileEntityFluxPlug();
        loaded.readFromNBT(saved);
        NBTTagCompound rewritten = new NBTTagCompound();
        loaded.writeToNBT(rewritten);
        require(
            loaded.stored() == 987654321 && rewritten.getLong("fluxBuffer") == 987654321,
            "real TileEntity NBT preserves buffered EU");
        require(
            loaded.voltage() == TileEntityFluxConnector.MAX_VOLTAGE && loaded.amperage() == TileEntityFluxConnector.MAX_AMPERAGE
                && loaded.priority() == -9999,
            "corrupt numeric settings clamp without wrapping");
        NBTTagCompound portable = new NBTTagCompound();
        loaded.writeSettings(portable);
        require(
            !portable.hasKey("fluxBuffer") && !portable.hasKey("fluxOwner") && !portable.hasKey("fluxChunkLoading"),
            "portable settings cannot copy stored EU, ownership or chunk tickets");
        ItemStack drop = GTNRItemList.FluxPlug.get(1);
        portable.setString("customExtension", "keep");
        drop.setTagCompound(portable);
        FluxDropData.normalize(drop);
        require(
            drop.getTagCompound()
                .getString("customExtension")
                .equals("keep")
                && !drop.getTagCompound()
                    .hasKey("fluxVoltage"),
            "drop cleanup resets settings and preserves unknown NBT");
        TileEntityFluxPoint defaults = new TileEntityFluxPoint();
        defaults.readFromNBT(new NBTTagCompound());
        require(
            defaults.voltage() == 32 && defaults.amperage() == 1
                && defaults.enabled()
                && defaults.connected()
                && !defaults.chunkLoading(),
            "default connectors are LV 1A with opt-in chunk loading");
    }

    private void verifyRecipes() {
        for (GTNRItemList connector : new GTNRItemList[] { GTNRItemList.FluxPlug, GTNRItemList.FluxPoint }) {
            long matches = RecipeMaps.assemblerRecipes.getAllRecipes()
                .stream()
                .filter(
                    recipe -> recipe.mOutputs.length == 1 && recipe.mOutputs[0] != null
                        && recipe.mOutputs[0].isItemEqual(connector.get(1)))
                .peek(
                    recipe -> require(
                        recipe.mDuration == 200 && recipe.mEUt > 0 && recipe.mEUt <= 128,
                        "connector assembler recipe preserves MV cost and duration"))
                .count();
            require(matches == 1, "each connector has exactly one assembler recipe");
        }
    }

    private void verifyScheduler(World world) throws Exception {
        TileEntityFluxPlug old = new TileEntityFluxPlug();
        old.setWorldObj(world);
        FluxTransferScheduler.add(old);
        new FluxTransferScheduler().onWorldUnload(new WorldEvent.Unload(world));
        require(!nodes().contains(old), "world unload removes old connector references");
        FluxTransferScheduler.add(old);
        FluxTransferScheduler.clear();
        Field ordered = FluxTransferScheduler.class.getDeclaredField("ordered");
        ordered.setAccessible(true);
        require(
            nodes().isEmpty() && ((List<?>) ordered.get(null)).isEmpty(),
            "server stop clears both scheduler caches");
    }

    private void verifyBreakRefund(EntityPlayerMP player) {
        World world = player.worldObj;
        place(world, 6, GTNRItemList.FluxPlug.get(1), player);
        TileEntityFluxPlug removed = (TileEntityFluxPlug) world.getTileEntity(6, 10, 0);
        NBTTagCompound saved = new NBTTagCompound();
        removed.writeToNBT(saved);
        saved.setLong("fluxBuffer", 64);
        removed.readFromNBT(saved);
        long before = balance(player);
        Block block = world.getBlock(6, 10, 0);
        List<ItemStack> drops = block.getDrops(world, 6, 10, 0, 0, 0);
        require(
            drops.size() == 1 && !drops.get(0)
                .hasTagCompound(),
            "real energy drops stack with freshly crafted blocks");
        require(removed.stored() == 64 && balance(player) == before, "drop queries cannot duplicate a refund");
        world.setBlockToAir(6, 10, 0);
        require(removed.stored() == 0 && balance(player) == before + 64, "block removal refunds saved EU exactly once");
        removed.returnBufferToNetwork();
        require(balance(player) == before + 64, "repeat removal cleanup cannot refund the same EU twice");
    }

    private static Collection<?> nodes() throws Exception {
        Field nodes = FluxTransferScheduler.class.getDeclaredField("NODES");
        nodes.setAccessible(true);
        return (Collection<?>) nodes.get(null);
    }

    private static long balance(EntityPlayerMP player) {
        return WirelessNetworkManager.getUserEU(player.getUniqueID())
            .longValueExact();
    }

    private static void place(World world, int x, ItemStack stack, EntityPlayerMP player) {
        Block block = Block.getBlockFromItem(stack.getItem());
        world.setBlock(x, 10, 0, block, 0, 3);
        block.onBlockPlacedBy(world, x, 10, 0, player, stack);
    }

    private static BaseMetaTileEntity nativeTile(World world, int x, ItemStack stack, ForgeDirection facing) {
        world.setBlock(x, 10, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 10, 0);
        tile.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        tile.setFrontFacing(facing);
        tile.enableWorking();
        return tile;
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        assertions++;
    }
}
