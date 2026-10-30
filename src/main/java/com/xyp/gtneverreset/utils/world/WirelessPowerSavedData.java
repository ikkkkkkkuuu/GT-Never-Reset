package com.xyp.gtneverreset.utils.world;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.common.DimensionManager;

import com.mojang.authlib.GameProfile;
import com.xyp.gtneverreset.utils.machine.IWirelessPowerReceiver;
import com.xyp.gtneverreset.utils.machine.WirelessPowerTransfer.TransferRate;

import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.common.misc.GlobalVariableStorage;
import gregtech.common.misc.spaceprojects.SpaceProjectManager;

public final class WirelessPowerSavedData extends WorldSavedData {

    private static final String DATA_NAME = "GTNR_WirelessPower";
    private static WirelessPowerSavedData instance;
    private final Map<UUID, Connection> connections = new LinkedHashMap<>();
    private final Map<UUID, String> names = new LinkedHashMap<>();

    public WirelessPowerSavedData() {
        super(DATA_NAME);
    }

    public WirelessPowerSavedData(String name) {
        super(name);
    }

    public static WirelessPowerSavedData get() {
        if (instance != null) return instance;
        World world = DimensionManager.getWorld(0);
        if (world == null || world.mapStorage == null) return null;
        instance = (WirelessPowerSavedData) world.mapStorage.loadData(WirelessPowerSavedData.class, DATA_NAME);
        if (instance == null) {
            instance = new WirelessPowerSavedData();
            world.mapStorage.setData(DATA_NAME, instance);
        }
        return instance;
    }

    public static void clear() {
        instance = null;
    }

    public Connection connection(UUID id) {
        return connections.get(id);
    }

    public Connection connect(BaseMetaTileEntity tile, UUID owner, UUID source) {
        connections.values()
            .removeIf(connection -> connection.matchesPosition(tile));
        Connection connection = new Connection(UUID.randomUUID(), owner, source, tile);
        connections.put(connection.id, connection);
        ((IWirelessPowerReceiver) tile).gtnr$setPowerConnection(connection.id);
        tile.markDirty();
        markDirty();
        return connection;
    }

    public List<Connection> connections(UUID owner) {
        List<Connection> result = new ArrayList<>();
        Iterator<Connection> iterator = connections.values()
            .iterator();
        while (iterator.hasNext()) {
            Connection connection = iterator.next();
            if (!owner.equals(connection.owner)) continue;
            // An unloaded chunk is not evidence of destruction. Only inspect already loaded positions.
            if (connection.missing()) {
                iterator.remove();
                markDirty();
            } else result.add(connection);
        }
        return result;
    }

    public void remove(UUID id, UUID owner) {
        Connection connection = connections.get(id);
        if (connection == null || !owner.equals(connection.owner)) return;
        BaseMetaTileEntity tile = connection.loadedTile();
        if (tile != null) {
            ((IWirelessPowerReceiver) tile).gtnr$setPowerConnection(null);
            tile.markDirty();
        }
        connections.remove(id);
        markDirty();
    }

    public void toggle(UUID id, UUID owner) {
        Connection connection = connections.get(id);
        if (connection == null || !owner.equals(connection.owner)) return;
        connection.paused = !connection.paused;
        markDirty();
    }

    public void remember(UUID id, String name) {
        if (id != null && name != null && !name.isEmpty() && !name.equals(names.put(id, name))) markDirty();
    }

    public String name(UUID id) {
        MinecraftServer server = MinecraftServer.getServer();
        GameProfile profile = server == null ? null
            : server.func_152358_ax()
                .func_152652_a(id);
        if (profile != null) remember(id, profile.getName());
        return names.getOrDefault(
            id,
            id.toString()
                .substring(0, 8));
    }

    public List<UUID> sources() {
        List<UUID> sources = new ArrayList<>();
        for (UUID player : GlobalVariableStorage.GlobalEnergy.keySet()) {
            UUID leader = SpaceProjectManager.getLeader(player);
            if (!sources.contains(leader)) sources.add(leader);
        }
        sources.sort(
            Comparator.comparing(this::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(UUID::toString));
        return sources;
    }

    public static boolean sourceExists(UUID id) {
        return id != null && GlobalVariableStorage.GlobalEnergy.containsKey(SpaceProjectManager.getLeader(id));
    }

    public static UUID uuid(NBTTagCompound tag, String key) {
        if (tag == null || !tag.hasKey(key, 8)) return null;
        try {
            return UUID.fromString(tag.getString(key));
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        connections.clear();
        names.clear();
        NBTTagList list = tag.getTagList("connections", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound saved = list.getCompoundTagAt(i);
            UUID id = uuid(saved, "id"), owner = uuid(saved, "owner"), source = uuid(saved, "source");
            if (id == null || owner == null || source == null) continue;
            Connection connection = new Connection(id, owner, source, saved);
            connections.put(id, connection);
        }
        NBTTagList players = tag.getTagList("names", 10);
        for (int i = 0; i < players.tagCount(); i++) {
            NBTTagCompound player = players.getCompoundTagAt(i);
            UUID id = uuid(player, "id");
            if (id != null) names.put(id, player.getString("name"));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (Connection connection : connections.values()) list.appendTag(connection.write());
        tag.setTag("connections", list);
        NBTTagList players = new NBTTagList();
        names.forEach((id, name) -> {
            NBTTagCompound player = new NBTTagCompound();
            player.setString("id", id.toString());
            player.setString("name", name);
            players.appendTag(player);
        });
        tag.setTag("names", players);
    }

    public static final class Connection {

        public final UUID id, owner, source;
        public final int dimension, x, y, z, metaId;
        public final String name;
        private boolean paused;
        private WeakReference<BaseMetaTileEntity> loaded = new WeakReference<>(null);
        private final TransferRate transferRate = new TransferRate();

        private Connection(UUID id, UUID owner, UUID source, BaseMetaTileEntity tile) {
            this.id = id;
            this.owner = owner;
            this.source = source;
            dimension = tile.getWorld().provider.dimensionId;
            x = tile.xCoord;
            y = tile.yCoord;
            z = tile.zCoord;
            metaId = tile.getMetaTileID();
            name = tile.getMetaTileEntity()
                .getLocalName();
            loaded = new WeakReference<>(tile);
        }

        private Connection(UUID id, UUID owner, UUID source, NBTTagCompound tag) {
            this.id = id;
            this.owner = owner;
            this.source = source;
            dimension = tag.getInteger("dimension");
            x = tag.getInteger("x");
            y = tag.getInteger("y");
            z = tag.getInteger("z");
            metaId = tag.getInteger("metaId");
            name = tag.getString("name");
            paused = tag.getBoolean("paused");
        }

        public boolean paused() {
            return paused;
        }

        public boolean matchesPosition(BaseMetaTileEntity tile) {
            return tile.getWorld().provider.dimensionId == dimension && tile.xCoord == x
                && tile.yCoord == y
                && tile.zCoord == z;
        }

        public boolean matches(BaseMetaTileEntity tile) {
            return matchesPosition(tile) && tile.getMetaTileID() == metaId;
        }

        public void observed(BaseMetaTileEntity tile, long transferred) {
            if (loaded.get() != tile) loaded = new WeakReference<>(tile);
            transferRate.record(
                tile.getWorld()
                    .getTotalWorldTime(),
                transferred);
        }

        public BaseMetaTileEntity loadedTile() {
            World world = DimensionManager.getWorld(dimension);
            if (world == null || !world.getChunkProvider()
                .chunkExists(x >> 4, z >> 4)) return null;
            TileEntity current = world.getTileEntity(x, y, z);
            if (!(current instanceof BaseMetaTileEntity tile) || tile.isInvalid()
                || !matches(tile)
                || !id.equals(((IWirelessPowerReceiver) tile).gtnr$powerConnection())) return null;
            if (loaded.get() != tile) loaded = new WeakReference<>(tile);
            return tile;
        }

        private boolean missing() {
            World world = DimensionManager.getWorld(dimension);
            if (world == null || !world.getChunkProvider()
                .chunkExists(x >> 4, z >> 4)) return false;
            TileEntity current = world.getTileEntity(x, y, z);
            return !(current instanceof BaseMetaTileEntity tile) || !matches(tile)
                || !id.equals(((IWirelessPowerReceiver) tile).gtnr$powerConnection());
        }

        public double averageTransfer() {
            BaseMetaTileEntity tile = loadedTile();
            return tile == null ? 0
                : transferRate.average(
                    tile.getWorld()
                        .getTotalWorldTime());
        }

        public NBTTagCompound write() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("id", id.toString());
            tag.setString("owner", owner.toString());
            tag.setString("source", source.toString());
            tag.setInteger("dimension", dimension);
            tag.setInteger("x", x);
            tag.setInteger("y", y);
            tag.setInteger("z", z);
            tag.setInteger("metaId", metaId);
            tag.setString("name", name);
            tag.setBoolean("paused", paused);
            return tag;
        }
    }
}
