// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtneverreset.common.gui;

import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.CLOSE_DETAILS;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.CYCLE_VISUALISATION;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.DELETE_SOURCE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.HIGHLIGHT;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.OPEN_DETAILS;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.REFRESH;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.RENAME;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.RENAME_SOURCE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.SELECT_SOURCE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.TELEPORT;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.TOGGLE_AUTO;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.TOGGLE_GROUPING;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.TOGGLE_PAUSE;
import static com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardActions.isRowAction;
import static com.xyp.gtneverreset.utils.machine.wireless.WirelessMachineGroups.coordinates;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.common.DimensionManager;

import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.factory.inventory.InventoryTypes;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.client.gui.WirelessCardGui;
import com.xyp.gtneverreset.container.WirelessCardContainer;
import com.xyp.gtneverreset.utils.machine.wireless.IWirelessLinkView;
import com.xyp.gtneverreset.utils.machine.wireless.InterfaceNameLocalization;
import com.xyp.gtneverreset.utils.machine.wireless.PhysicalClusterTracker;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessAutoConnect;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessCardBinding;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessCardInventory;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessCardVisualisation;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessChannelManager;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessClusterManager;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessInterfaceView;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessMachineGroups;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessMachineGroups.Row;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessNetworkStatistics;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessOperations;

import appeng.api.AEApi;
import appeng.api.networking.IGridNode;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import galacticgreg.api.enums.DimensionDef;

public final class WirelessFrequencyCardGui {

    private WirelessFrequencyCardGui() {}

    public static ModularPanel build(PlayerInventoryGuiData data, PanelSyncManager syncManager, UISettings settings) {
        int index = data.getSlotIndex();
        int location = data.getInventoryType() == InventoryTypes.BAUBLES ? -index - 1
            : index == data.getPlayer().inventory.currentItem ? 0 : index + 1;
        var container = new WirelessCardContainer(data.getPlayer(), location);
        settings.customContainer(() -> container);
        settings.canInteractWith(container::canInteractWith);
        syncManager.syncValue("wireless", container.sync);
        return syncManager.isClient() ? createPanel(container) : new ModularPanel("wireless_card").size(320, 232);
    }

    @SideOnly(Side.CLIENT)
    private static ModularPanel createPanel(WirelessCardContainer container) {
        return new WirelessCardGui(container);
    }

    @SideOnly(Side.CLIENT)
    public static ModularScreen createScreen(ModularPanel panel) {
        return ((WirelessCardGui) panel).screen();
    }

    public static final class WirelessCardActions {

        public static final int PREVIOUS_PAGE = 0;
        public static final int NEXT_PAGE = 1;
        public static final int TOGGLE_AUTO = 2;
        public static final int REFRESH = 3;
        public static final int BACK = 4;
        public static final int RENAME = 5;
        public static final int APPLY_FILTER = 6;
        public static final int CYCLE_FILTER = 7;
        public static final int CYCLE_VISUALISATION = 9;

        public static final int TOGGLE_PAUSE = 10;
        public static final int OPEN_DETAILS = 20;
        public static final int COPY_POSITION = 30;
        public static final int HIGHLIGHT = 40;
        public static final int TELEPORT = 50;
        public static final int CLOSE_DETAILS = 60;
        public static final int TOGGLE_GROUPING = 61;
        public static final int OPEN_SOURCES = 62;
        public static final int SELECT_SOURCE = 63;
        public static final int RENAME_SOURCE = 70;
        public static final int DELETE_SOURCE = 80;
        private static final int ROW_STRIDE = 10;

        private WirelessCardActions() {}

        public static int type(int id) {
            return isRowAction(id) ? id - rowIndex(id) : id;
        }

        public static int rowIndex(int id) {
            return id % ROW_STRIDE;
        }

        public static boolean isRowAction(int id) {
            return id >= TOGGLE_PAUSE && id < TELEPORT + ROW_STRIDE
                || id >= RENAME_SOURCE && id < DELETE_SOURCE + ROW_STRIDE;
        }
    }

    private static final class WirelessCardActionHandler {

        private final WirelessCardContainer session;
        private long nextRequestTick = Long.MIN_VALUE;

        public WirelessCardActionHandler(WirelessCardContainer session) {
            this.session = session;
        }

        public void execute(long revision, int action, long targetId, String text, long nameRevision) {
            EntityPlayer player = session.player;
            ItemStack card = session.card;
            WirelessCardBinding binding = session.binding;
            if (action == CLOSE_DETAILS) {
                if (session.view.detailNode() != null) {
                    session.view.setDetailNode(null);
                    session.update(true);
                }
                return;
            }
            if (isRowAction(action) && action != OPEN_DETAILS
                && action != TOGGLE_PAUSE
                && action != TELEPORT
                && action != HIGHLIGHT
                && action != RENAME_SOURCE
                && action != DELETE_SOURCE) return;
            if (!acceptRequest(player.worldObj.getTotalWorldTime())) return;
            if (action == OPEN_DETAILS) session.update(false);
            if (session.revision != revision) {
                session.update(true);
                return;
            }
            var row = session.snapshot == null ? null : session.snapshot.target(targetId);
            if (isRowAction(action) && action != RENAME_SOURCE && action != DELETE_SOURCE || action == RENAME) {
                if (binding == null || row == null) {
                    message("stale");
                    session.update(true);
                    return;
                }
            }
            if (action == SELECT_SOURCE || action == RENAME_SOURCE || action == DELETE_SOURCE) {
                var sources = WirelessCardBinding.sources(card.getTagCompound());
                if (targetId < 1 || targetId > sources.size()) return;
                var selected = sources.get((int) targetId - 1);
                if (!selected.belongsTo(player.getUniqueID())) return;
                if (action == DELETE_SOURCE) {
                    session.deleteSource((int) targetId - 1);
                } else if (action == RENAME_SOURCE) {
                    if (!WirelessCardBinding.renameSource(card.getTagCompound(), (int) targetId - 1, text)) return;
                    WirelessCardInventory.sync(player, card);
                } else {
                    var node = selected.source()
                        .node();
                    if (node != null && !WirelessChannelManager.canBuild(node, player)) {
                        message("denied");
                        return;
                    }
                    session.selectSource((int) targetId - 1);
                }
            } else if (action == CYCLE_VISUALISATION) {
                WirelessCardVisualisation.cycle(card, player);
            } else if (action == RENAME) {
                if (session.view.detailNode() == null || !row.members()
                    .contains(session.view.detailNode())) return;
                if (session.view.nameRevision() != nameRevision) {
                    message("stale");
                } else {
                    try {
                        if (!WirelessOperations.rename(player, binding.source(), row, text)) message("denied");
                    } catch (WirelessOperations.Denied denied) {
                        message("denied");
                    } catch (IllegalStateException stale) {
                        message("stale");
                    }
                }
            } else if (action == TELEPORT && player instanceof EntityPlayerMP serverPlayer) {
                try {
                    switch (WirelessOperations.teleport(serverPlayer, binding.source(), row)) {
                        case STALE -> message("stale");
                        case UNAVAILABLE -> message("teleport_unavailable");
                        case DENIED -> message("denied");
                        case APPLIED -> {
                            return;
                        }
                        default -> {}
                    }
                } catch (PhysicalClusterTracker.ScanLimitException limit) {
                    message("stale");
                }
            } else if (action == HIGHLIGHT) {
                try {
                    if (!WirelessClusterManager.isCurrentLink(binding.source(), row)) {
                        message("stale");
                        session.update(true);
                        return;
                    }
                } catch (PhysicalClusterTracker.ScanLimitException limit) {
                    message("stale");
                    return;
                }
                if (row.target()
                    .dimension() != player.dimension) {
                    message(
                        "highlight_dimension",
                        row.target()
                            .dimension());
                } else {
                    session.pendingHighlight = new NBTTagList();
                    var targets = WirelessClusterManager.highlight(row)
                        .stream()
                        .filter(target -> target.dimension() == player.dimension)
                        .sorted(
                            Comparator
                                .comparingDouble(target -> player.getDistanceSq(target.x(), target.y(), target.z())))
                        .limit(512)
                        .collect(Collectors.toList());
                    for (var target : targets) {
                        NBTTagCompound pos = new NBTTagCompound();
                        pos.setInteger("dimension", target.dimension());
                        pos.setInteger("x", target.x());
                        pos.setInteger("y", target.y());
                        pos.setInteger("z", target.z());
                        session.pendingHighlight.appendTag(pos);
                    }
                    message("highlight_started", targets.size());
                }
            } else if (action == TOGGLE_AUTO) {
                WirelessAutoConnect.toggleAutomatic(player, card);
            } else if (action == TOGGLE_GROUPING && session.view.detailNode() == null) {
                WirelessCardBinding.toggleGrouping(card);
                WirelessCardInventory.sync(player, card);
            } else if (action == OPEN_DETAILS) {
                if (session.view.detailNode() == null && row instanceof IWirelessLinkView.ClusterView)
                    session.view.setDetailNode(row.node());
            } else if (action == TOGGLE_PAUSE) {
                try {
                    WirelessOperations.setPaused(player, binding.source(), row);
                } catch (WirelessOperations.Denied denied) {
                    message("denied");
                } catch (IllegalStateException stale) {
                    message("stale");
                }
            } else if (action != REFRESH) return;
            session.update(true);
        }

        private boolean acceptRequest(long tick) {
            if (tick < nextRequestTick) return false;
            nextRequestTick = tick + 4;
            return true;
        }

        private void message(String key, Object... args) {
            session.player.addChatMessage(new ChatComponentTranslation("gtneverreset.wireless.gui." + key, args));
        }
    }

    public static class WirelessCardViewState {

        private IGridNode detailNode;
        private final WirelessMachineGroups.Startup startup = new WirelessMachineGroups.Startup();
        private Map<Target, Long> targetIds = new HashMap<>();
        private long nextTargetId, nameRevision;
        private RenameState renameState;

        public IGridNode detailNode() {
            return detailNode;
        }

        public void setDetailNode(IGridNode detailNode) {
            this.detailNode = detailNode;
        }

        public long nameRevision() {
            return nameRevision;
        }

        public Snapshot build(WirelessCardBinding binding, ItemStack card) {
            var interfaces = new WirelessInterfaceView();
            List<IWirelessLinkView> all = binding == null ? Collections.emptyList()
                : new ArrayList<>(WirelessClusterManager.links(binding.source()));
            boolean initializing = startup.waiting(all);
            var focus = detailNode == null ? null
                : all.stream()
                    .filter(
                        link -> link.members()
                            .contains(detailNode))
                    .findFirst()
                    .orElse(null);
            if (focus == null) detailNode = null;
            RenameState nextRenameState = focus == null ? null : new RenameState(focus.members(), focus.deviceNames());
            if (!Objects.equals(renameState, nextRenameState)) nameRevision++;
            renameState = nextRenameState;
            List<Row> rows = initializing ? Collections.emptyList()
                : focus == null ? WirelessMachineGroups.group(all, interfaces)
                    : WirelessClusterManager.entranceLinks(binding.source(), focus.cluster())
                        .stream()
                        .map(Row::cluster)
                        .collect(Collectors.toList());
            NBTTagCompound data = new NBTTagCompound();
            NBTTagCompound dimensions = new NBTTagCompound();
            data.setBoolean("detail", focus != null);
            data.setBoolean("initializing", initializing);
            data.setBoolean("collapseTypes", WirelessCardBinding.collapseTypes(card.getTagCompound()));
            data.setString("name", focus == null ? "" : focus.name());
            data.setLong("nameRevision", nameRevision);
            data.setString(
                "detailTarget",
                focus == null ? ""
                    : focus.target()
                        .toString());
            data.setBoolean(
                "multipleNames",
                focus != null && focus.names()
                    .size() > 1);
            data.setBoolean(
                "renameable",
                focus != null && !focus.deviceNames()
                    .isEmpty());
            data.setBoolean("auto", WirelessCardBinding.automatic(card.getTagCompound()));
            data.setInteger("visualMode", WirelessCardVisualisation.mode(card.getTagCompound()));
            data.setBoolean("bound", binding != null);
            data.setString(
                "selectedSource",
                binding == null ? ""
                    : binding.source()
                        .toString());
            data.setString("sourceName", WirelessCardBinding.sourceName(card.getTagCompound(), binding));
            NBTTagList sources = new NBTTagList();
            var savedSources = WirelessCardBinding.sources(card.getTagCompound());
            for (int i = 0; i < savedSources.size(); i++) {
                var saved = savedSources.get(i);
                NBTTagCompound entry = new NBTTagCompound();
                entry.setLong("targetId", i + 1);
                entry.setString(
                    "sourceKey",
                    saved.source()
                        .toString());
                entry.setString("name", WirelessCardBinding.sourceName(card.getTagCompound(), saved));
                entry.setString("position", coordinates(saved.source()));
                entry.setBoolean(
                    "selected",
                    binding != null && saved.source()
                        .equals(binding.source()));
                var sourceNode = saved.source()
                    .node();
                entry.setBoolean("loaded", sourceNode != null);
                writeIcon(
                    entry,
                    "icon",
                    sourceNode == null ? AEApi.instance()
                        .definitions()
                        .blocks()
                        .controller()
                        .maybeStack(1)
                        .orNull()
                        : sourceNode.getGridBlock()
                            .getMachineRepresentation());
                writeDimensionName(
                    dimensions,
                    saved.source()
                        .dimension());
                sources.appendTag(entry);
            }
            data.setTag("sources", sources);
            writeStatistics(
                data,
                WirelessMachineGroups.summarize(focus == null ? all : Collections.singletonList(focus)));
            data.setInteger("total", focus == null ? all.size() : 1);
            data.setInteger("entrances", binding == null ? 0 : WirelessChannelManager.entranceCount(binding.source()));
            data.setInteger("entranceLimit", WirelessChannelManager.entranceLimit());
            if (binding != null) {
                var sourceAddress = WirelessChannelManager.controllerSource(binding.source());
                data.setString("source", coordinates(sourceAddress));
                writeDimensionName(dimensions, sourceAddress.dimension());
                IGridNode source = binding.source()
                    .node();
                data.setBoolean("sourceLoaded", source != null);
                data.setBoolean("channels", WirelessChannelManager.channelsEnabled());
                if (source != null && source.getGrid() != null) {
                    var allocation = WirelessChannelManager.allocation(source.getGrid());
                    data.setInteger("capacity", WirelessChannelManager.capacity(source.getGrid()));
                    data.setInteger("used", allocation == null ? -1 : allocation.used());
                }
            }
            Map<Target, Long> nextIds = new HashMap<>();
            Map<Long, IWirelessLinkView> targets = new LinkedHashMap<>();
            if (focus != null) data.setLong("detailId", identify(focus, nextIds, targets));
            NBTTagList entries = new NBTTagList();
            for (Row row : rows)
                entries.appendTag(writeRow(row, interfaces, dimensions, nextIds, targets, focus != null));
            targetIds = nextIds;
            data.setTag("rows", entries);
            data.setTag("dimensions", dimensions);
            return new Snapshot(data, targets);
        }

        private long identify(IWirelessLinkView link, Map<Target, Long> nextIds, Map<Long, IWirelessLinkView> targets) {
            Target key = new Target(link.node(), link.members(), link instanceof IWirelessLinkView.EntranceView);
            long id = nextIds.computeIfAbsent(key, ignored -> {
                Long previous = targetIds.get(key);
                return previous == null ? ++nextTargetId : previous;
            });
            targets.put(id, link);
            return id;
        }

        private NBTTagCompound writeRow(Row row, WirelessInterfaceView interfaces, NBTTagCompound dimensions,
            Map<Target, Long> nextIds, Map<Long, IWirelessLinkView> targets, boolean detail) {
            var target = row.target();
            NBTTagCompound entry = new NBTTagCompound();
            writeDimensionName(dimensions, target.dimension());
            entry.setInteger("dimension", target.dimension());
            entry.setString("position", coordinates(target));
            entry.setString("key", (row.group() ? "machine:" : "cluster:") + target);
            entry.setString("sortKey", target.toString());
            entry.setString(
                "typeKey",
                row.type() == null ? ""
                    : row.type()
                        .sortKey());
            entry.setBoolean("group", row.group());
            entry.setBoolean(
                "shared",
                row.type() != null && row.type()
                    .shared());
            entry.setInteger(
                "clusters",
                row.links()
                    .size());
            entry.setString(
                "name",
                row.group() ? ""
                    : row.link()
                        .name());
            entry.setString(
                "side",
                target.side()
                    .name());
            entry.setString("state", row.conflicted() ? "conflict" : row.state());
            entry.setBoolean("paused", row.paused());
            entry.setBoolean("conflicted", row.conflicted());
            entry.setInteger("entrances", row.entrances());
            writeStatistics(entry, row.statistics());
            var entrance = !row.group() && row.link() instanceof IWirelessLinkView.EntranceView runtime ? runtime
                : null;
            entry.setBoolean("runtime", entrance != null);
            entry.setInteger(
                "usedChannels",
                entrance == null ? -1
                    : entrance.usedChannels()
                        .orElse(-1));
            ItemStack icon = row.icon();
            writeIcon(entry, "icon", icon);
            if (row.type() != null) writeIcon(
                entry,
                "typeIcon",
                row.type()
                    .icon());
            entry.setString("serverIconName", icon == null ? "" : icon.getDisplayName());
            if (row.group()) {
                NBTTagList children = new NBTTagList();
                for (var link : row.links()) {
                    var child = writeRow(Row.cluster(link), interfaces, dimensions, nextIds, targets, false);
                    // The parent identifies the machine; its children identify the actual ME devices.
                    writeIcon(
                        child,
                        "icon",
                        link.node()
                            .getGridBlock()
                            .getMachineRepresentation());
                    child.removeTag("interfaceNames");
                    child.removeTag("interfaceNameCount");
                    child.setBoolean(
                        "leaf",
                        link.members()
                            .size() == 1);
                    children.appendTag(child);
                }
                entry.setTag("children", children);
            } else {
                var link = row.link();
                entry.setLong("targetId", identify(link, nextIds, targets));
                if (!detail) {
                    var names = interfaces.names(link);
                    NBTTagList labels = new NBTTagList();
                    for (var name : names) {
                        NBTTagCompound label = new NBTTagCompound();
                        label.setString("rawName", name.rawName());
                        label.setString("suffix", name.suffix());
                        label.setString(
                            "serverName",
                            InterfaceNameLocalization.localizeName(name.rawName(), name.suffix(), name.icon()));
                        writeIcon(label, "icon", name.icon());
                        labels.appendTag(label);
                    }
                    entry.setTag("searchLabels", labels);
                    NBTTagList customNames = new NBTTagList();
                    link.names()
                        .stream()
                        .sorted()
                        .forEach(name -> customNames.appendTag(new NBTTagString(name)));
                    entry.setTag("customNames", customNames);
                    // Interface display icons can differ from the underlying device's searchable name.
                    ItemStack device = link.node()
                        .getGridBlock()
                        .getMachineRepresentation();
                    writeIcon(entry, "searchIcon", device);
                    entry.setString("serverDeviceName", device == null ? "" : device.getDisplayName());
                    writeInterfaceNames(entry, names);
                }
            }
            return entry;
        }

        private static void writeStatistics(NBTTagCompound data, WirelessNetworkStatistics statistics) {
            data.setInteger("devices", statistics.devices());
            data.setInteger("online", statistics.online());
            data.setInteger("missing", statistics.missing());
            data.setBoolean("stable", statistics.stable());
        }

        private static void writeIcon(NBTTagCompound data, String key, ItemStack icon) {
            if (icon == null || icon.getItem() == null) return;
            // Display-only identity, never ship inventories or other machine NBT as an icon.
            ItemStack clean = new ItemStack(icon.getItem(), 1, icon.getItemDamage());
            data.setTag(key, clean.writeToNBT(new NBTTagCompound()));
        }

        private static void writeDimensionName(NBTTagCompound dimensions, int dimension) {
            String id = Integer.toString(dimension);
            if (dimensions.hasKey(id)) return;
            var world = DimensionManager.getWorld(dimension);
            dimensions.setString(id, world == null ? "" : Objects.toString(DimensionDef.getDimensionName(world), ""));
        }

        private static void writeInterfaceNames(NBTTagCompound data, List<WirelessInterfaceView.Name> names) {
            if (names.isEmpty()) return;
            NBTTagList entries = new NBTTagList();
            for (var name : names.subList(0, Math.min(4, names.size()))) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setString("rawName", name.rawName());
                entry.setString("suffix", name.suffix());
                writeIcon(entry, "icon", name.icon());
                entries.appendTag(entry);
            }
            data.setTag("interfaceNames", entries);
            data.setInteger("interfaceNameCount", names.size());
            writeIcon(
                data,
                "icon",
                names.get(0)
                    .icon());
        }

        @Desugar
        private record Target(IGridNode node, Set<IGridNode> members, boolean entrance) {}

        @Desugar
        private record RenameState(Set<IGridNode> members, Map<IGridNode, String> names) {}

        public static final class Snapshot {

            private final NBTTagCompound payload;
            private final Map<Long, IWirelessLinkView> targets;

            private Snapshot(NBTTagCompound payload, Map<Long, IWirelessLinkView> targets) {
                this.payload = (NBTTagCompound) payload.copy();
                this.targets = Collections.unmodifiableMap(new HashMap<>(targets));
            }

            public IWirelessLinkView target(long id) {
                return targets.get(id);
            }

            public boolean sameView(Snapshot previous) {
                return previous != null && payload.equals(previous.payload);
            }

            public NBTTagCompound packet(long session, long revision, NBTTagList highlight) {
                var data = (NBTTagCompound) payload.copy();
                data.setLong("session", session);
                data.setLong("revision", revision);
                if (highlight != null) data.setTag("highlight", highlight.copy());
                return data;
            }
        }
    }

    public static final class WirelessCardSyncHandler extends SyncHandler<WirelessCardSyncHandler> {

        private static final int CHUNK_BYTES = 24 * 1024;

        private final WirelessCardContainer container;
        private final WirelessCardActionHandler actions;
        private int ticks;

        public WirelessCardSyncHandler(WirelessCardContainer container) {
            this.container = container;
            actions = new WirelessCardActionHandler(container);
        }

        public void update(boolean force) {
            if (!(container.player instanceof EntityPlayerMP)) return;
            var next = container.view.build(container.binding, container.card);
            boolean changed = force || !next.sameView(container.snapshot);
            container.snapshot = next;
            if (changed) container.revision++;
            if (changed || container.pendingHighlight != null) {
                NBTTagCompound data = next
                    .packet(container.sessionId(), container.revision, container.pendingHighlight);
                container.pendingHighlight = null;
                sendSnapshot(data);
            }
        }

        private void sendSnapshot(NBTTagCompound data) {
            try {
                byte[] compressed = CompressedStreamTools.compress(data);
                long sequence = container.revision;
                for (int offset = 0; offset < compressed.length; offset += CHUNK_BYTES) {
                    int start = offset;
                    byte[] chunk = Arrays
                        .copyOfRange(compressed, start, Math.min(compressed.length, start + CHUNK_BYTES));
                    syncToClient(0, buf -> {
                        buf.writeLong(sequence);
                        buf.writeInt(compressed.length);
                        buf.writeInt(start);
                        buf.writeShort(chunk.length);
                        buf.writeBytes(chunk);
                    });
                }
            } catch (IOException failure) {
                GTNeverReset.LOG.error("cannot encode wireless card view", failure);
            }
        }

        @Override
        public void detectAndSendChanges(boolean init) {
            if ((init || ++ticks % 20 == 0) && container.canInteractWith(container.player)) container.update(init);
        }

        @SideOnly(Side.CLIENT)
        public void request(int action, long target, String text, long nameRevision) {
            if (!isValid() || !container.display.hasKey("session")) return;
            var request = new Action(
                container.display.getLong("session"),
                container.display.getLong("revision"),
                action,
                target,
                text,
                nameRevision);
            syncToServer(1, request::write);
        }

        @Override
        public void readOnServer(int id, PacketBuffer buf) {
            if (id != 1) return;
            final Action request;
            try {
                request = Action.read(buf);
            } catch (IOException | RuntimeException malformed) {
                return;
            }
            if (request.text() == null || request.text()
                .length() > 32) return;
            ServerThreadUtil.addScheduledTask(() -> {
                if (isValid() && container.player.openContainer == container
                    && container.canInteractWith(container.player)
                    && container.sessionId() == request.session()) {
                    actions.execute(
                        request.revision(),
                        request.action(),
                        request.target(),
                        request.text(),
                        request.nameRevision());
                }
            });
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void readOnClient(int id, PacketBuffer buf) {
            if (id != 0) return;
            long sequence = buf.readLong();
            int total = buf.readInt(), offset = buf.readInt(), length = buf.readUnsignedShort();
            if (total <= 0 || total > 64 * 1024 * 1024
                || offset < 0
                || offset > total
                || length == 0
                || length > CHUNK_BYTES
                || length > total - offset
                || length > buf.readableBytes()) return;
            byte[] chunk = new byte[length];
            buf.readBytes(chunk);
            Minecraft minecraft = Minecraft.getMinecraft();
            minecraft.func_152344_a(() -> {
                if (!isValid() || minecraft.thePlayer != container.player
                    || container.player.openContainer != container) return;
                container.clientView()
                    .acceptChunk(sequence, total, offset, chunk);
            });
        }

        @Desugar
        record Action(long session, long revision, int action, long target, String text, long nameRevision) {

            static Action read(PacketBuffer buf) throws IOException {
                return new Action(
                    buf.readLong(),
                    buf.readLong(),
                    buf.readInt(),
                    buf.readLong(),
                    buf.readStringFromBuffer(32),
                    buf.readLong());
            }

            void write(PacketBuffer buf) throws IOException {
                buf.writeLong(session);
                buf.writeLong(revision);
                buf.writeInt(action);
                buf.writeLong(target);
                buf.writeStringToBuffer(text);
                buf.writeLong(nameRevision);
            }
        }
    }
}
