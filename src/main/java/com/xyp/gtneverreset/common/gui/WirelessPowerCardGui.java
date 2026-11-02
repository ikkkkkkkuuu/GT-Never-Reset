package com.xyp.gtneverreset.common.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.factory.inventory.InventoryTypes;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.xyp.gtneverreset.client.wireless.WirelessPowerCardPanel;
import com.xyp.gtneverreset.common.gui.container.WirelessPowerCardContainer;
import com.xyp.gtneverreset.common.item.items.WirelessPowerCard;
import com.xyp.gtneverreset.common.wireless.WirelessCardTeleport;
import com.xyp.gtneverreset.common.wireless.WirelessChannelManager.Address;
import com.xyp.gtneverreset.common.wireless.WirelessHatchOwner;
import com.xyp.gtneverreset.common.wireless.WirelessPowerSavedData;
import com.xyp.gtneverreset.common.wireless.WirelessPowerSavedData.Connection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import galacticgreg.api.enums.DimensionDef;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.common.misc.WirelessNetworkManager;

public final class WirelessPowerCardGui {

    public static final int MIN_ROWS = 5, MAX_ROWS = 9;
    public static final int SOURCES = 1, DEVICES = 2, PREVIOUS = 3, NEXT = 4, AUTO = 5, SELECT = 6, PAUSE = 7,
        REMOVE = 8, REFRESH = 9, RESIZE = 10, FILTER = 11, TELEPORT = 12, HIGHLIGHT = 13, OPEN = 14, CLOSE = 15,
        STATE = 16, DIMENSION = 17, MERGE_TYPES = 18, EXPAND_TYPE = 19;

    public enum PowerState {
        ALL,
        CONNECTED,
        OFFLINE,
        NO_POWER,
        PAUSED
    }

    private WirelessPowerCardGui() {}

    public static ModularPanel build(PlayerInventoryGuiData data, PanelSyncManager sync, UISettings settings) {
        int index = data.getSlotIndex();
        int location = data.getInventoryType() == InventoryTypes.BAUBLES ? -index - 1
            : index == data.getPlayer().inventory.currentItem ? 0 : index + 1;
        var container = new WirelessPowerCardContainer(data.getPlayer(), location);
        Session session = new Session(data, container).allowC2S();
        sync.syncValue("power", session);
        settings.customContainer(() -> container);
        settings.canInteractWith(container::canInteractWith);
        return sync.isClient() ? clientPanel(session) : new ModularPanel("wireless_power_card").size(320, 232);
    }

    @SideOnly(Side.CLIENT)
    private static ModularPanel clientPanel(Session session) {
        return new WirelessPowerCardPanel(session);
    }

    @SideOnly(Side.CLIENT)
    public static ModularScreen createScreen(ModularPanel panel) {
        return ((WirelessPowerCardPanel) panel).screen();
    }

    public static final class Session extends SyncHandler<Session> {

        private final PlayerInventoryGuiData data;
        private final WirelessPowerCardContainer container;
        private int page, ticks, visibleRows = MIN_ROWS;
        private boolean sources;
        private Integer dimension;
        private PowerState state = PowerState.ALL;
        private final Set<Integer> expandedTypes = new HashSet<>();
        private String filter = "";
        private String detail = "";
        private int overviewPage;
        private long lastAction = Long.MIN_VALUE;
        private NBTTagCompound display = new NBTTagCompound();

        private Session(PlayerInventoryGuiData data, WirelessPowerCardContainer container) {
            this.data = data;
            this.container = container;
        }

        public boolean hasCard() {
            return container.canInteractWith(data.getPlayer());
        }

        public NBTTagCompound display() {
            return display;
        }

        public NBTTagCompound row(int index) {
            return display.getTagList("rows", 10)
                .getCompoundTagAt(index);
        }

        @Override
        public void detectAndSendChanges(boolean init) {
            if (!hasCard()) return;
            if (init || ++ticks % 20 == 0) update();
        }

        private void update() {
            WirelessPowerSavedData saved = WirelessPowerSavedData.get();
            if (saved == null) return;
            var player = data.getPlayer();
            ItemStack card = container.card();
            saved.remember(player.getUniqueID(), player.getCommandSenderName());
            UUID selected = WirelessPowerCard.source(card, player, true);
            NBTTagCompound next = new NBTTagCompound();
            next.setBoolean("sources", sources);
            next.setBoolean("auto", WirelessPowerCard.automatic(card));
            next.setString("selected", selected.toString());
            next.setString("sourceName", saved.name(selected));
            next.setString(
                "balance",
                WirelessNetworkManager.getUserEU(selected)
                    .toString());
            List<UUID> networks = sources ? saved.sources() : List.of();
            List<DeviceRow> devices = sources ? List.of() : deviceRows(saved);
            if (!detail.isEmpty()) {
                DeviceRow group = devices.stream()
                    .filter(
                        row -> row.key()
                            .equals(detail))
                    .findFirst()
                    .orElse(null);
                if (group == null) {
                    detail = "";
                    page = overviewPage;
                } else {
                    next.setTag("controller", group.write(saved));
                    devices = group.members.stream()
                        .map(DeviceRow::new)
                        .toList();
                }
            }
            next.setBoolean("detail", !detail.isEmpty());
            if (!sources && detail.isEmpty()) devices = overview(devices, saved, next);
            int count = visibleRows;
            int total = sources ? networks.size() : devices.size();
            int pages = Math.max(1, (total + count - 1) / count);
            page = Math.max(0, Math.min(page, pages - 1));
            next.setInteger("page", page);
            next.setInteger("pages", pages);
            next.setInteger("total", total);
            next.setInteger("rowsPerPage", visibleRows);
            next.setString("filter", filter);
            NBTTagList rows = new NBTTagList();
            for (int i = page * count; i < Math.min(total, (page + 1) * count); i++) {
                NBTTagCompound row;
                if (sources) {
                    UUID id = networks.get(i);
                    row = new NBTTagCompound();
                    row.setString("id", id.toString());
                    row.setString("name", saved.name(id));
                    row.setString(
                        "balance",
                        WirelessNetworkManager.getUserEU(id)
                            .toString());
                } else {
                    row = devices.get(i)
                        .write(saved);
                }
                rows.appendTag(row);
            }
            next.setTag("rows", rows);
            display = next;
            syncToClient(0, buffer -> buffer.writeNBTTagCompoundToBuffer(next));
        }

        private List<DeviceRow> overview(List<DeviceRow> devices, WirelessPowerSavedData saved, NBTTagCompound next) {
            Map<Integer, Integer> dimensions = new TreeMap<>();
            for (DeviceRow row : devices) dimensions.merge(row.dimension(), 1, Integer::sum);
            NBTTagList options = new NBTTagList();
            if (dimension != null) dimensions.putIfAbsent(dimension, 0);
            dimensions.forEach((id, count) -> {
                NBTTagCompound option = new NBTTagCompound();
                option.setInteger("dimension", id);
                option.setInteger("count", count);
                option.setString("name", dimensionName(id));
                options.appendTag(option);
            });
            next.setTag("dimensions", options);
            if (dimension != null) next.setInteger("dimension", dimension);
            boolean mergeTypes = WirelessPowerCard.mergeTypes(container.card());
            next.setBoolean("mergeTypes", mergeTypes);
            next.setInteger("state", state.ordinal());
            next.setInteger("deviceCount", devices.size());

            int[] counts = new int[PowerState.values().length];
            List<DeviceRow> matches = new ArrayList<>();
            String query = filter.toLowerCase(Locale.ROOT);
            for (DeviceRow row : devices) {
                if (dimension != null && row.dimension() != dimension || !query.isEmpty() && !row.matches(query, saved))
                    continue;
                PowerState status = row.status();
                counts[PowerState.ALL.ordinal()]++;
                counts[status.ordinal()]++;
                if (state == PowerState.ALL || state == status) matches.add(row);
            }
            next.setIntArray("stateCounts", counts);
            if (!mergeTypes) return matches;

            Map<Integer, List<DeviceRow>> types = new LinkedHashMap<>();
            for (DeviceRow row : matches) types.computeIfAbsent(row.metaId(), ignored -> new ArrayList<>())
                .add(row);
            List<DeviceRow> shown = new ArrayList<>();
            for (List<DeviceRow> type : types.values()) {
                DeviceRow first = type.get(0);
                if (type.size() == 1) {
                    shown.add(first);
                    continue;
                }
                DeviceRow folder = new DeviceRow(first.members.get(0), first.controller);
                folder.typeCount = type.size();
                folder.expanded = expandedTypes.contains(first.metaId());
                shown.add(folder);
                if (folder.expanded) {
                    for (DeviceRow row : type) row.indented = true;
                    shown.addAll(type);
                }
            }
            return shown;
        }

        private static String dimensionName(int id) {
            var world = DimensionManager.getWorld(id);
            return world == null ? "" : Objects.toString(DimensionDef.getDimensionName(world), "");
        }

        private List<DeviceRow> deviceRows(WirelessPowerSavedData saved) {
            Map<String, DeviceRow> rows = new LinkedHashMap<>();
            for (Connection connection : saved.connections(
                data.getPlayer()
                    .getUniqueID())) {
                BaseMetaTileEntity controller = controller(connection.loadedTile());
                DeviceRow row = new DeviceRow(connection, controller);
                DeviceRow previous = rows.putIfAbsent(row.key(), row);
                if (previous != null) previous.members.add(connection);
            }
            return new ArrayList<>(rows.values());
        }

        private static BaseMetaTileEntity controller(BaseMetaTileEntity tile) {
            if (tile == null || !(tile.getMetaTileEntity() instanceof WirelessHatchOwner hatch)) return null;
            var owners = hatch.wirelessFrequency$getControllers();
            // Shared energy hatches must not be assigned to an arbitrary controller.
            if (owners.size() != 1) return null;
            var machine = owners.iterator()
                .next();
            var base = machine.getBaseMetaTileEntity();
            if (!(base instanceof BaseMetaTileEntity controller) || controller.isInvalid()
                || controller.getWorld() == null
                || !controller.getWorld()
                    .getChunkProvider()
                    .chunkExists(controller.xCoord >> 4, controller.zCoord >> 4)
                || controller.getWorld()
                    .getTileEntity(controller.xCoord, controller.yCoord, controller.zCoord) != controller)
                return null;
            return controller;
        }

        @SideOnly(Side.CLIENT)
        public void request(int action, String target) {
            if (!isValid()) return;
            syncToServer(action, buffer -> buffer.writeStringToBuffer(target));
        }

        @Override
        public void readOnServer(int action, PacketBuffer buffer) throws IOException {
            if (action < SOURCES || action > EXPAND_TYPE) return;
            String target = buffer.readStringFromBuffer(64);
            ServerThreadUtil.addScheduledTask(() -> {
                if (!isValid() || !hasCard() || data.getPlayer().openContainer != container) return;
                ItemStack card = container.card();
                long now = data.getPlayer().worldObj.getTotalWorldTime();
                if (action != RESIZE && action != FILTER && action < STATE) {
                    if (lastAction == now) return;
                    lastAction = now;
                }
                if (action >= STATE && (sources || !detail.isEmpty())) return;
                WirelessPowerSavedData saved = WirelessPowerSavedData.get();
                if (saved == null) return;
                UUID id = null;
                if (action >= SELECT && action <= REMOVE || action == TELEPORT || action == HIGHLIGHT) {
                    try {
                        id = UUID.fromString(target);
                    } catch (IllegalArgumentException invalid) {
                        return;
                    }
                }
                switch (action) {
                    case SOURCES -> {
                        sources = true;
                        detail = "";
                        page = 0;
                    }
                    case DEVICES -> {
                        sources = false;
                        detail = "";
                        page = 0;
                    }
                    case PREVIOUS -> page = Math.max(0, page - 1);
                    case NEXT -> page = Math.min(display.getInteger("pages") - 1, page + 1);
                    case AUTO -> WirelessPowerCard.toggleAutomatic(card, data.getPlayer());
                    case SELECT -> {
                        WirelessPowerCard.selectSource(card, data.getPlayer(), id);
                        sources = false;
                        page = 0;
                    }
                    case PAUSE -> saved.toggle(
                        id,
                        data.getPlayer()
                            .getUniqueID());
                    case REMOVE -> saved.remove(
                        id,
                        data.getPlayer()
                            .getUniqueID());
                    case OPEN -> {
                        DeviceRow group = deviceRows(saved).stream()
                            .filter(
                                row -> row.controller != null && row.key()
                                    .equals(target))
                            .findFirst()
                            .orElse(null);
                        if (group == null) return;
                        overviewPage = page;
                        detail = group.key();
                        sources = false;
                        page = 0;
                    }
                    case CLOSE -> {
                        detail = "";
                        page = overviewPage;
                    }
                    case TELEPORT, HIGHLIGHT -> {
                        Connection connection = saved.connection(id);
                        if (connection == null || !connection.owner.equals(
                            data.getPlayer()
                                .getUniqueID())
                            || connection.loadedTile() == null) {
                            update();
                            return;
                        }
                        var player = data.getPlayer();
                        if (action == TELEPORT && player instanceof EntityPlayerMP serverPlayer) {
                            WirelessCardTeleport.teleport(
                                serverPlayer,
                                new Address(
                                    connection.dimension,
                                    connection.x,
                                    connection.y,
                                    connection.z,
                                    ForgeDirection.UNKNOWN));
                        } else if (action == HIGHLIGHT) {
                            if (player.dimension != connection.dimension) {
                                player.addChatMessage(
                                    new ChatComponentTranslation(
                                        "gtneverreset.wireless.gui.highlight_dimension",
                                        connection.dimension));
                            } else {
                                NBTTagList positions = new NBTTagList();
                                positions.appendTag(connection.write());
                                NBTTagCompound highlight = new NBTTagCompound();
                                highlight.setTag("positions", positions);
                                syncToClient(1, packet -> packet.writeNBTTagCompoundToBuffer(highlight));
                            }
                        }
                        return;
                    }
                    case STATE -> {
                        try {
                            state = PowerState.valueOf(target);
                        } catch (IllegalArgumentException invalid) {
                            return;
                        }
                        page = 0;
                    }
                    case DIMENSION -> {
                        try {
                            dimension = target.isEmpty() ? null : Integer.valueOf(target);
                        } catch (NumberFormatException invalid) {
                            return;
                        }
                        page = 0;
                    }
                    case MERGE_TYPES -> {
                        WirelessPowerCard.toggleGrouping(card, data.getPlayer());
                        expandedTypes.clear();
                        page = 0;
                    }
                    case EXPAND_TYPE -> {
                        if (!WirelessPowerCard.mergeTypes(card)) return;
                        var rows = display.getTagList("rows", 10);
                        for (int i = 0; i < rows.tagCount(); i++) {
                            var row = rows.getCompoundTagAt(i);
                            if (row.getInteger("typeCount") <= 1 || !row.getString("id")
                                .equals(target)) continue;
                            int type = row.getInteger("metaId");
                            if (!expandedTypes.remove(type)) expandedTypes.add(type);
                            break;
                        }
                    }
                    case REFRESH -> {}
                    case RESIZE -> {
                        try {
                            visibleRows = Math.max(MIN_ROWS, Math.min(MAX_ROWS, Integer.parseInt(target)));
                        } catch (NumberFormatException invalid) {
                            return;
                        }
                    }
                    case FILTER -> {
                        filter = target.trim();
                        page = 0;
                    }
                    default -> {}
                }
                update();
            });
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void readOnClient(int id, PacketBuffer buffer) throws IOException {
            if (id != 0 && id != 1) return;
            NBTTagCompound next = buffer.readNBTTagCompoundFromBuffer();
            net.minecraft.client.Minecraft.getMinecraft()
                .func_152344_a(() -> {
                    if (!isValid() || next == null) return;
                    if (id == 0) display = next;
                    else com.xyp.gtneverreset.client.wireless.WirelessWorldHighlightRender
                        .show(net.minecraft.client.Minecraft.getMinecraft(), next.getTagList("positions", 10));
                });
        }

        private static final class DeviceRow {

            private final BaseMetaTileEntity controller;
            private final List<Connection> members = new ArrayList<>();

            private int typeCount;
            private boolean expanded, indented;
            private PowerState status;
            private int online;
            private boolean paused, ready;

            private DeviceRow(Connection connection) {
                this(connection, null);
            }

            private DeviceRow(Connection connection, BaseMetaTileEntity controller) {
                this.controller = controller;
                members.add(connection);
            }

            private String key() {
                return controller == null ? members.get(0).id.toString()
                    : controller.getWorld().provider.dimensionId + ":"
                        + controller.xCoord
                        + ":"
                        + controller.yCoord
                        + ":"
                        + controller.zCoord
                        + ":"
                        + controller.getMetaTileID();
            }

            private int metaId() {
                return controller == null ? members.get(0).metaId : controller.getMetaTileID();
            }

            private int dimension() {
                return members.get(0).dimension;
            }

            private PowerState status() {
                if (status == null) {
                    online = 0;
                    paused = true;
                    ready = true;
                    for (Connection member : members) {
                        if (member.loadedTile() != null) online++;
                        paused &= member.paused();
                        ready &= WirelessNetworkManager.getUserEU(member.source)
                            .signum() > 0;
                    }
                    status = paused ? PowerState.PAUSED
                        : online == 0 ? PowerState.OFFLINE : !ready ? PowerState.NO_POWER : PowerState.CONNECTED;
                }
                return status;
            }

            private boolean matches(String query, WirelessPowerSavedData saved) {
                if (controller != null && (controller.getMetaTileEntity()
                    .getLocalName() + " "
                    + searchPosition(dimension(), controller.xCoord, controller.yCoord, controller.zCoord))
                        .toLowerCase(Locale.ROOT)
                        .contains(query))
                    return true;
                return members.stream()
                    .anyMatch(
                        connection -> (connection.name + " "
                            + saved.name(connection.source)
                            + " "
                            + searchPosition(connection.dimension, connection.x, connection.y, connection.z))
                                .toLowerCase(Locale.ROOT)
                                .contains(query));
            }

            private static String searchPosition(int dimension, int x, int y, int z) {
                return "D" + dimension + " " + x + ", " + y + ", " + z;
            }

            private NBTTagCompound write(WirelessPowerSavedData saved) {
                Connection first = members.get(0);
                if (typeCount > 1) {
                    NBTTagCompound folder = new NBTTagCompound();
                    folder.setString("id", "type:" + metaId());
                    folder.setInteger("metaId", metaId());
                    folder.setString(
                        "name",
                        controller == null ? first.name
                            : controller.getMetaTileEntity()
                                .getLocalName());
                    folder.setInteger("typeCount", typeCount);
                    folder.setBoolean("expanded", expanded);
                    return folder;
                }
                NBTTagCompound row = first.write();
                row.setBoolean("indented", indented);
                row.setString("id", key());
                row.setBoolean("group", controller != null);
                row.setInteger("members", members.size());
                row.setInteger("state", status().ordinal());
                row.setInteger("online", online);
                row.setBoolean("loaded", online > 0);
                row.setBoolean("paused", paused);
                row.setBoolean("ready", ready);
                row.setDouble(
                    "transfer",
                    members.stream()
                        .mapToDouble(Connection::averageTransfer)
                        .sum());
                BaseMetaTileEntity tile = first.loadedTile();
                if (controller == null && tile != null && tile.getMetaTileEntity() instanceof MTEBasicMachine machine)
                    row.setLong("recipeEUt", machine.mMaxProgresstime > 0 ? Math.max(0, machine.mEUt) : 0);
                row.setString("sourceName", saved.name(first.source));
                if (controller != null) {
                    row.setInteger("metaId", controller.getMetaTileID());
                    row.setString(
                        "name",
                        controller.getMetaTileEntity()
                            .getLocalName());
                    row.setInteger("x", controller.xCoord);
                    row.setInteger("y", controller.yCoord);
                    row.setInteger("z", controller.zCoord);
                }
                row.setString("dimensionName", dimensionName(first.dimension));
                return row;
            }
        }
    }
}
