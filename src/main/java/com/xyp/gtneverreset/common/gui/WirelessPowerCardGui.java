package com.xyp.gtneverreset.common.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
import com.xyp.gtneverreset.client.gui.WirelessPowerCardPanel;
import com.xyp.gtneverreset.common.item.items.WirelessPowerCard;
import com.xyp.gtneverreset.container.WirelessPowerCardContainer;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessCardTeleport;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessChannelManager.Address;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessHatchOwner;
import com.xyp.gtneverreset.utils.world.WirelessPowerSavedData;
import com.xyp.gtneverreset.utils.world.WirelessPowerSavedData.Connection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import galacticgreg.api.enums.DimensionDef;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.common.misc.WirelessNetworkManager;

public final class WirelessPowerCardGui {

    public static final int MIN_ROWS = 5, MAX_ROWS = 9;
    public static final int SOURCES = 1, DEVICES = 2, PREVIOUS = 3, NEXT = 4, AUTO = 5, SELECT = 6, PAUSE = 7,
        REMOVE = 8, REFRESH = 9, RESIZE = 10, FILTER = 11, TELEPORT = 12, HIGHLIGHT = 13, OPEN = 14, CLOSE = 15;

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
            if (detail.isEmpty()) devices = devices.stream()
                .filter(row -> row.matches(filter, saved))
                .toList();
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
            if (action < SOURCES || action > CLOSE) return;
            String target = buffer.readStringFromBuffer(64);
            ServerThreadUtil.addScheduledTask(() -> {
                if (!isValid() || !hasCard() || data.getPlayer().openContainer != container) return;
                ItemStack card = container.card();
                long now = data.getPlayer().worldObj.getTotalWorldTime();
                if (lastAction == now && action != RESIZE && action != FILTER) return;
                lastAction = now;
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
                    else com.xyp.gtneverreset.client.render.WirelessWorldHighlightRender
                        .show(net.minecraft.client.Minecraft.getMinecraft(), next.getTagList("positions", 10));
                });
        }

        private static final class DeviceRow {

            private final BaseMetaTileEntity controller;
            private final List<Connection> members = new ArrayList<>();

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

            private boolean matches(String filter, WirelessPowerSavedData saved) {
                String query = filter.toLowerCase(Locale.ROOT);
                if (controller != null && controller.getMetaTileEntity()
                    .getLocalName()
                    .toLowerCase(Locale.ROOT)
                    .contains(query)) return true;
                return members.stream()
                    .anyMatch(
                        connection -> (connection.name + " "
                            + saved.name(connection.source)
                            + " "
                            + connection.dimension
                            + " "
                            + connection.x
                            + ", "
                            + connection.y
                            + ", "
                            + connection.z).toLowerCase(Locale.ROOT)
                                .contains(query));
            }

            private NBTTagCompound write(WirelessPowerSavedData saved) {
                Connection first = members.get(0);
                NBTTagCompound row = first.write();
                row.setString("id", key());
                row.setBoolean("group", controller != null);
                row.setInteger("members", members.size());
                row.setInteger(
                    "online",
                    (int) members.stream()
                        .filter(member -> member.loadedTile() != null)
                        .count());
                row.setBoolean("loaded", row.getInteger("online") > 0);
                row.setBoolean(
                    "paused",
                    members.stream()
                        .allMatch(Connection::paused));
                row.setBoolean(
                    "ready",
                    members.stream()
                        .allMatch(
                            member -> WirelessNetworkManager.getUserEU(member.source)
                                .signum() > 0));
                row.setLong(
                    "transfer",
                    members.stream()
                        .mapToLong(Connection::lastTransfer)
                        .reduce(0L, (sum, value) -> Long.MAX_VALUE - sum < value ? Long.MAX_VALUE : sum + value));
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
                var world = DimensionManager.getWorld(first.dimension);
                row.setString(
                    "dimensionName",
                    world == null ? "" : Objects.toString(DimensionDef.getDimensionName(world), ""));
                return row;
            }
        }
    }
}
