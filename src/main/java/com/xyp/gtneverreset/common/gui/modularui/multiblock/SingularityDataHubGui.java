package com.xyp.gtneverreset.common.gui.modularui.multiblock;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.FluidDrawable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.GenericSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.sizer.Unit;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtneverreset.common.machine.hatch.VaultPortHatch;
import com.xyp.gtneverreset.common.machine.multiblock.SingularityDataHub;
import com.xyp.gtneverreset.common.me.vault.VaultNetworkInventory;
import com.xyp.gtneverreset.config.ScreenThemeConfig;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;
import com.xyp.gtnrlib.gui.widget.ThemedButton;

import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.modularui2.factory.GTBaseGuiBuilder;

public class SingularityDataHubGui extends GTNRMultiBlockBaseGui<SingularityDataHub> {

    private static final String TEXT = "gtnr.gui.singularity_data_hub.";
    private static final String ITEMS = "vaultNetworkItems";
    private static final String FLUIDS = "vaultNetworkFluids";
    private static final String RESIZE = "vaultNetworkResize";
    private static final String SCROLL = "vaultNetworkScroll";
    private static final String SORT = "vaultNetworkSort";
    private static final String SEARCH = "vaultNetworkSearch";
    private static final int SLOT_SIZE = AE2Theme.SLOT_SIZE;
    private static final int GRID_PADDING = 4;
    private static final int PANEL_WIDTH = 440;
    private static final int PANEL_HEIGHT = 320;
    private static final int PANEL_PADDING = 8;
    private static final int OVERVIEW_HEIGHT = 64;
    private static final int REFRESH_TICKS = 20;
    private static final int SEARCH_LENGTH = 80;

    public SingularityDataHubGui(SingularityDataHub multiblock) {
        super(multiblock);
    }

    @Override
    protected int getBasePanelWidth() {
        return PANEL_WIDTH;
    }

    @Override
    protected int getBasePanelHeight() {
        return PANEL_HEIGHT;
    }

    @Override
    protected ModularPanel getBasePanel(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        ModularPanel panel = new GTBaseGuiBuilder(multiblock, guiData, syncManager, uiSettings)
            .setWidth(getBasePanelWidth())
            .setHeight(getBasePanelHeight())
            .doesAddTitle(false)
            .doesAddGregTechLogo(false)
            .doesBindPlayerInventory(false)
            .build();
        if (guiData.isClient()) {
            uiSettings.useTheme(ScreenThemeConfig.vault.themeId);
            panel.width(
                () -> Math.min(
                    PANEL_WIDTH,
                    panel.getScreen()
                        .getScreenArea()
                        .w() - 2 * PANEL_PADDING),
                Unit.Measure.PIXEL);
            panel.height(
                () -> Math.min(
                    PANEL_HEIGHT,
                    panel.getScreen()
                        .getScreenArea()
                        .h() - 2 * PANEL_PADDING),
                Unit.Measure.PIXEL);
        }
        return panel;
    }

    @Override
    public Flow createMainColumn(ModularPanel panel, PanelSyncManager syncManager) {
        ParentWidget<?> content = new ParentWidget<>().fullWidth()
            .expanded();
        NetworkBrowser browser = new NetworkBrowser(syncManager);
        content.child(createOverview(syncManager, browser));
        content.child(
            createNetworkBrowser(syncManager, browser).left(0)
                .right(0)
                .top(OVERVIEW_HEIGHT)
                .bottom(0));
        return Flow.column()
            .full()
            .padding(PANEL_PADDING)
            .child(content);
    }

    private ParentWidget<?> createOverview(PanelSyncManager syncManager, NetworkBrowser browser) {
        LongSyncValue usedBytes = new LongSyncValue(multiblock::getUsedStorageBytes);
        LongSyncValue itemTypes = new LongSyncValue(multiblock::itemsCount);
        LongSyncValue fluidTypes = new LongSyncValue(multiblock::fluidsCount);
        BooleanSyncValue autoVoid = new BooleanSyncValue(() -> multiblock.doVoidExcess);
        BooleanSyncValue hasPort = new BooleanSyncValue(() -> multiblock.portHatch != null);
        IntSyncValue priority = new IntSyncValue(
            multiblock::getStoragePriority,
            value -> { if (!syncManager.isClient()) multiblock.setStoragePriority(syncManager.getPlayer(), value); })
                .allowC2S();
        syncManager.syncValue("singularityDataHubStorageBytes", usedBytes);
        syncManager.syncValue("singularityDataHubItemTypes", itemTypes);
        syncManager.syncValue("singularityDataHubFluidTypes", fluidTypes);
        syncManager.syncValue("singularityDataHubAutoVoid", autoVoid);
        syncManager.syncValue("singularityDataHubPortHatch", hasPort);
        syncManager.syncValue("singularityDataHubStoragePriority", priority);

        ParentWidget<?> overview = new ParentWidget<>().fullWidth()
            .height(OVERVIEW_HEIGHT);
        overview.child(
            label(IKey.lang("gtnr.machine.singularity_data_hub.name")).pos(0, 0)
                .width(156));
        // #tr gtnr.gui.singularity_data_hub.network_search
        // # Search
        // # zh_CN 搜索
        overview.child(
            label(IKey.lang(TEXT + "network_search")).pos(122, 2)
                .width(38));
        overview.child(
            new TextFieldWidget().left(164)
                .right(0)
                .top(0)
                .height(16)
                .setMaxLength(SEARCH_LENGTH)
                .autoUpdateOnChange(true)
                .value(new StringValue.Dynamic(() -> browser.clientQuery, browser::setClientQuery)));
        // #tr gtnr.gui.singularity_data_hub.bytes
        // # Bytes
        // # zh_CN 字节
        overview.child(
            metric(
                "bytes",
                () -> compact(usedBytes.getLongValue()) + " / " + compact(SingularityDataHub.MAX_STORAGE_BYTES),
                18).left(0)
                    .widthRel(0.48f));
        Widget<?> loadBar = new Widget<>().pos(0, 31)
            .widthRel(0.46f)
            .height(10);
        loadBar.background((context, x, y, width, height, theme) -> {
            AE2Look look = AE2Look.of(loadBar);
            look.drawWell(x, y, width, height);
            int fill = (int) (load(usedBytes.getLongValue()) * (width - 4));
            if (fill > 0) look.drawSelectedRow(x + 2, y + 2, fill, height - 4);
        });
        loadBar.tooltip(
            t -> t.setAutoUpdate(true)
                .tooltipBuilder(lines -> {
                    // #tr gtnr.gui.singularity_data_hub.current_load
                    // # Current Load
                    // # zh_CN 当前负载
                    lines.addLine(IKey.str(tr("current_load") + " " + percent(usedBytes.getLongValue())));
                    lines.addLine(
                        IKey.str(
                            String.format(
                                Locale.ROOT,
                                "%,d / %,d",
                                usedBytes.getLongValue(),
                                SingularityDataHub.MAX_STORAGE_BYTES)));
                }));
        overview.child(loadBar);
        // #tr gtnr.gui.singularity_data_hub.item_types
        // # Item Types
        // # zh_CN 物品种类
        overview.child(
            metric("item_types", () -> compact(itemTypes.getLongValue()), 18).leftRelAnchor(0.5f, 0)
                .right(0));
        // #tr gtnr.gui.singularity_data_hub.fluid_types
        // # Fluid Types
        // # zh_CN 流体种类
        overview.child(
            metric("fluid_types", () -> compact(fluidTypes.getLongValue()), 31).leftRelAnchor(0.5f, 0)
                .right(0));
        // #tr gtnr.gui.singularity_data_hub.auto_void
        // # Auto-voiding
        // # zh_CN 自动销毁溢出
        // #tr gtnr.gui.singularity_data_hub.enabled
        // # Enabled
        // # zh_CN 已启用
        // #tr gtnr.gui.singularity_data_hub.disabled
        // # Disabled
        // # zh_CN 已禁用
        overview.child(
            metric("auto_void", () -> tr(autoVoid.getBoolValue() ? "enabled" : "disabled"), 44).leftRelAnchor(0.5f, 0)
                .right(0));
        // #tr gtnr.gui.singularity_data_hub.need_vault_port_hatch
        // # Vault Port Hatch is required
        // # zh_CN 需要保险库接口
        overview.child(
            label(IKey.dynamic(() -> hasPort.getBoolValue() ? "" : tr("need_vault_port_hatch"))).left(0)
                .widthRel(0.48f)
                .top(44));
        // #tr gtnr.gui.singularity_data_hub.storage_priority
        // # ME Priority
        // # zh_CN ME 优先级
        overview.child(
            label(IKey.lang(TEXT + "storage_priority")).pos(0, 44)
                .width(62)
                .setEnabledIf(widget -> hasPort.getBoolValue()));
        overview.child(
            new TextFieldWidget().name("vault_storage_priority")
                .pos(65, 42)
                .size(75, 16)
                .numbersInt(Integer.MIN_VALUE, Integer.MAX_VALUE)
                .value(priority)
                .setEnabledIf(widget -> hasPort.getBoolValue()));
        overview.child(
            Flow.row()
                .pos(72, 0)
                .size(44, 18)
                .child(createPowerSwitchButton())
                .child(createStructureUpdateButton(syncManager)));
        return overview;
    }

    private ParentWidget<?> createNetworkBrowser(PanelSyncManager syncManager, NetworkBrowser browser) {
        ParentWidget<?> network = new ParentWidget<>();
        InventoryGrid grid = new InventoryGrid(browser, syncManager).left(0)
            .right(0)
            .top(23)
            .bottom(0);
        syncManager.onClientTick(grid::syncSize);
        syncManager.onClientTick(browser::sendSearch);
        network.child(grid);
        // #tr gtnr.gui.singularity_data_hub.network
        // # ME Network
        // # zh_CN ME 网络
        network.child(
            label(IKey.lang(TEXT + "network")).left(0)
                .top(4)
                .width(78));
        network.child(
            new ThemedButton<>().size(84, 20)
                .right(152)
                .top(0)
                .label(browser::sortLabel, () -> true)
                .action(() -> syncManager.callSyncedAction(SORT)));
        // #tr gtnr.gui.singularity_data_hub.network_items
        // # Items
        // # zh_CN 物品
        network.child(
            channelButton("network_items", () -> !browser.fluid(), syncManager, ITEMS).right(76)
                .top(0));
        // #tr gtnr.gui.singularity_data_hub.network_fluids
        // # Fluids
        // # zh_CN 流体
        network.child(
            channelButton("network_fluids", browser::fluid, syncManager, FLUIDS).right(0)
                .top(0));
        return network;
    }

    private static ThemedButton<?> channelButton(String key, BooleanSupplier selected, PanelSyncManager sync,
        String action) {
        return new ThemedButton<>().size(72, 20)
            .selected(selected)
            .label(() -> tr(key), () -> true)
            .action(() -> sync.callSyncedAction(action));
    }

    private static TextWidget<?> metric(String key, Supplier<String> value, int y) {
        return label(IKey.dynamic(() -> tr(key) + ": " + value.get())).top(y);
    }

    private static TextWidget<?> label(IKey text) {
        return text.asWidget()
            .height(12)
            .textAlign(Alignment.CenterLeft);
    }

    private static String tr(String key) {
        return StatCollector.translateToLocal(TEXT + key);
    }

    private static double load(long bytes) {
        return bytes / (double) SingularityDataHub.MAX_STORAGE_BYTES;
    }

    private static String percent(long bytes) {
        double percentage = load(bytes) * 100;
        return percentage > 0 && percentage < 0.1 ? "<0.1%" : String.format(Locale.ROOT, "%.1f%%", percentage);
    }

    private static String compact(long amount) {
        if (amount < 1000) return Long.toString(amount);
        String[] units = { "", "K", "M", "G", "T", "P", "E" };
        double scaled = amount;
        int index = 0;
        while (scaled >= 1000 && index < units.length - 1) {
            scaled /= 1000;
            index++;
        }
        return String.format(Locale.ROOT, "%.1f%s", scaled, units[index]);
    }

    private final class NetworkBrowser {

        private final PanelSyncManager sync;
        private final GenericSyncValue<NBTTagCompound, ?> viewValue;
        private final VaultNetworkInventory inventory = new VaultNetworkInventory();

        private EntityPlayer viewer;
        private NBTTagCompound serverView = new NBTTagCompound();
        private long nextRefresh;
        private boolean showFluids;
        private boolean sortByAmount;
        private int columns;
        private int rows;
        private int firstRow;
        private boolean dirty = true;

        private NBTTagCompound displayedView;
        private NetworkEntry[] visibleEntries = new NetworkEntry[0];
        private String clientQuery = "";
        private String sentQuery = "";
        private int searchDelay;

        private NetworkBrowser(PanelSyncManager sync) {
            this.sync = sync;
            sync.addOpenListener(player -> { if (!sync.isClient()) viewer = player; });
            sync.addCloseListener(player -> inventory.stopObserving());
            viewValue = GenericSyncValue.builder(NBTTagCompound.class)
                .getter(this::snapshot)
                .serializer(NetworkBrowser::writeView)
                .deserializer(NetworkBrowser::readView)
                .equalsDefault()
                .copy(tag -> (NBTTagCompound) tag.copy())
                .build();
            sync.syncValue("vaultNetworkView", viewValue);
            sync.registerSyncedAction(ITEMS, false, true, buffer -> select(false));
            sync.registerSyncedAction(FLUIDS, false, true, buffer -> select(true));
            sync.registerSyncedAction(
                RESIZE,
                false,
                true,
                buffer -> resize(buffer.readUnsignedByte(), buffer.readUnsignedByte()));
            sync.registerSyncedAction(SCROLL, false, true, buffer -> scroll(buffer.readByte()));
            sync.registerSyncedAction(SORT, false, true, buffer -> sort());
            sync.registerSyncedAction(SEARCH, false, true, this::receiveSearch);
        }

        // A full grid may exceed the signed-short length used by PacketBuffer's NBT methods.
        private static void writeView(PacketBuffer buffer, NBTTagCompound view) throws IOException {
            byte[] compressed = CompressedStreamTools.compress(view);
            buffer.writeVarIntToBuffer(compressed.length);
            buffer.writeBytes(compressed);
        }

        private static NBTTagCompound readView(PacketBuffer buffer) throws IOException {
            int length = buffer.readVarIntFromBuffer();
            if (length <= 0 || length > buffer.readableBytes()) throw new IOException("Invalid vault inventory data");
            byte[] compressed = new byte[length];
            buffer.readBytes(compressed);
            return CompressedStreamTools.func_152457_a(compressed, new NBTSizeTracker(8L * 1024 * 1024));
        }

        private void select(boolean fluid) {
            if (showFluids == fluid) return;
            showFluids = fluid;
            firstRow = 0;
            dirty = true;
        }

        private void sort() {
            sortByAmount = !sortByAmount;
            firstRow = 0;
            inventory.invalidateOrder();
            dirty = true;
        }

        private void setClientQuery(String text) {
            clientQuery = text;
            searchDelay = 5;
        }

        private void sendSearch() {
            if (searchDelay == 0 || --searchDelay != 0 || sentQuery.equals(clientQuery)) return;
            sentQuery = clientQuery;
            sync.callSyncedAction(SEARCH, buffer -> {
                NetworkUtils.writeStringSafe(buffer, sentQuery, SEARCH_LENGTH * 4);
                NetworkUtils.writeStringSafe(
                    buffer,
                    FMLCommonHandler.instance()
                        .getCurrentLanguage(),
                    16);
            });
        }

        private void receiveSearch(PacketBuffer buffer) {
            try {
                search(buffer.readStringFromBuffer(SEARCH_LENGTH), buffer.readStringFromBuffer(16));
            } catch (IOException ignored) {}
        }

        private void search(String text, String locale) {
            if (!inventory.setSearch(text, locale)) return;
            firstRow = 0;
            dirty = true;
        }

        private void resize(int newColumns, int newRows) {
            if (newColumns < 1 || newColumns > PANEL_WIDTH / SLOT_SIZE
                || newRows < 1
                || newRows > PANEL_HEIGHT / SLOT_SIZE) return;
            if (newColumns == columns && newRows == rows) return;
            firstRow = (int) ((long) firstRow * columns / newColumns);
            columns = newColumns;
            rows = newRows;
            dirty = true;
        }

        private void scroll(int direction) {
            if (direction != -1 && direction != 1) return;
            if (dirty || (direction < 0 ? firstRow == 0 : !serverView.getBoolean("more"))) return;
            firstRow += direction;
            dirty = true;
        }

        private NBTTagCompound snapshot() {
            if (viewer == null || columns == 0) return serverView;
            long tick = viewer.worldObj.getTotalWorldTime();
            boolean poll = tick >= nextRefresh;
            if (!dirty && !poll) return serverView;
            if (poll) nextRefresh = tick + REFRESH_TICKS;

            VaultPortHatch port = multiblock.portHatch;
            IStorageGrid storage = port == null || port.controller != multiblock ? null
                : port.getNetworkStorageFor(viewer);
            IMEMonitor<?> monitor = storage == null ? null
                : showFluids ? storage.getFluidInventory() : storage.getItemInventory();
            boolean inventoryChanged = inventory.refresh(monitor, poll);
            if (!dirty && !inventoryChanged) return serverView;
            dirty = false;

            NBTTagCompound result = new NBTTagCompound();
            result.setBoolean("fluids", showFluids);
            result.setBoolean("amountOrder", sortByAmount);
            result.setBoolean("online", monitor != null);
            result.setBoolean("filtering", inventory.isFiltering());
            collectVisible(result, inventory.entries(showFluids, sortByAmount));
            result.setInteger("columns", columns);
            result.setInteger("rows", rows);
            result.setLong("firstRow", firstRow);
            serverView = result;
            return serverView;
        }

        private void collectVisible(NBTTagCompound result, List<IAEStack<?>> filtered) {
            NBTTagList entries = new NBTTagList();
            int lastRow = (int) Math.max(0, ((long) filtered.size() + columns - 1) / columns - rows);
            firstRow = Math.min(firstRow, lastRow);
            int start = firstRow * columns;
            int capacity = columns * rows;
            int visible = Math.min(capacity, filtered.size() - start);
            for (int index = 0; index < visible; index++) {
                IAEStack<?> stack = filtered.get(start + index);
                long amount = stack.getStackSize();
                NBTTagCompound entry = new NBTTagCompound();
                NBTTagCompound icon = new NBTTagCompound();
                if (stack instanceof IAEFluidStack fluid) {
                    FluidStack display = fluid.getFluidStack();
                    display.amount = 1;
                    display.writeToNBT(icon);
                } else if (stack instanceof IAEItemStack item) {
                    ItemStack display = item.getItemStack();
                    display.stackSize = 1;
                    display.writeToNBT(icon);
                }
                entry.setTag("icon", icon);
                entry.setLong("amount", amount);
                entries.appendTag(entry);
            }
            result.setBoolean("more", firstRow < lastRow);
            result.setTag("entries", entries);
        }

        private boolean fluid() {
            return viewValue.getValue()
                .getBoolean("fluids");
        }

        private String sortLabel() {
            // #tr gtnr.gui.singularity_data_hub.sort_amount
            // # By quantity
            // # zh_CN 按数量
            if (viewValue.getValue()
                .getBoolean("amountOrder")) return tr("sort_amount");
            // #tr gtnr.gui.singularity_data_hub.sort_fluid
            // # By fluid ID
            // # zh_CN 按流体 ID
            // #tr gtnr.gui.singularity_data_hub.sort_inventory
            // # By category
            // # zh_CN 按分类
            return tr(fluid() ? "sort_fluid" : "sort_inventory");
        }

        private String emptyMessage() {
            // #tr gtnr.gui.singularity_data_hub.network_unavailable
            // # ME inventory unavailable
            // # zh_CN 无法读取 ME 网络库存
            if (!viewValue.getValue()
                .getBoolean("online")) return tr("network_unavailable");
            // #tr gtnr.gui.singularity_data_hub.network_no_matches
            // # No matching entries
            // # zh_CN 没有匹配的库存
            if (entries().length == 0 && viewValue.getValue()
                .getBoolean("filtering")) return tr("network_no_matches");
            // #tr gtnr.gui.singularity_data_hub.network_empty
            // # No stored entries
            // # zh_CN 暂无库存
            return entries().length == 0 ? tr("network_empty") : "";
        }

        private NetworkEntry[] entries() {
            NBTTagCompound view = viewValue.getValue();
            if (displayedView != view) {
                displayedView = view;
                NBTTagList entries = view.getTagList("entries", Constants.NBT.TAG_COMPOUND);
                visibleEntries = new NetworkEntry[entries.tagCount()];
                for (int i = 0; i < visibleEntries.length; i++) {
                    visibleEntries[i] = NetworkEntry.read(entries.getCompoundTagAt(i), fluid());
                }
            }
            return visibleEntries;
        }
    }

    private record NetworkEntry(ItemStack item, FluidStack fluid, IDrawable icon, IKey count, String exactAmount) {

        private static NetworkEntry read(NBTTagCompound entry, boolean fluidChannel) {
            NBTTagCompound icon = entry.getCompoundTag("icon");
            long amount = entry.getLong("amount");
            IKey count = IKey.str(compact(amount))
                .color(0xFFFFFFFF)
                .shadow(true)
                .scale(0.55f)
                .alignment(Alignment.BottomRight);
            String exact = String.format(Locale.ROOT, "%,d", amount);
            if (fluidChannel) {
                FluidStack fluid = FluidStack.loadFluidStackFromNBT(icon);
                return fluid == null ? null
                    : new NetworkEntry(null, fluid, new FluidDrawable(fluid), count, exact + " mB");
            }
            ItemStack item = ItemStack.loadItemStackFromNBT(icon);
            return item == null ? null : new NetworkEntry(item, null, new ItemDrawable(item), count, exact);
        }
    }

    private static final class InventoryGrid extends Widget<InventoryGrid> implements Interactable {

        private final NetworkBrowser browser;
        private final PanelSyncManager sync;
        private int lastColumns;
        private int lastRows;

        private InventoryGrid(NetworkBrowser browser, PanelSyncManager sync) {
            this.browser = browser;
            this.sync = sync;
            name("vault_network_grid");
            disableHoverBackground();
            tooltip(
                t -> t.setAutoUpdate(true)
                    .tooltipBuilder(lines -> {
                        int index = hoveredIndex();
                        NetworkEntry[] entries = browser.entries();
                        if (index < 0 || index >= entries.length || entries[index] == null) return;
                        NetworkEntry entry = entries[index];
                        if (entry.item() != null) lines.addFromItem(entry.item());
                        else lines.addFromFluid(entry.fluid());
                        lines.addLine(IKey.str(entry.exactAmount()));
                    }));
        }

        private int columns() {
            return Math.max(0, (getArea().w() - 2 * GRID_PADDING) / SLOT_SIZE);
        }

        private int rows() {
            return Math.max(0, (getArea().h() - 2 * GRID_PADDING) / SLOT_SIZE);
        }

        private void syncSize() {
            int columns = columns();
            int rows = rows();
            if (columns == 0 || rows == 0 || (columns == lastColumns && rows == lastRows)) return;
            lastColumns = columns;
            lastRows = rows;
            sync.callSyncedAction(RESIZE, buffer -> {
                buffer.writeByte(columns);
                buffer.writeByte(rows);
            });
        }

        @Override
        public boolean onMouseScroll(UpOrDown direction, int amount) {
            sync.callSyncedAction(SCROLL, buffer -> buffer.writeByte(direction == UpOrDown.UP ? -1 : 1));
            return true;
        }

        private int hoveredIndex() {
            int columns = columns();
            int rows = rows();
            int x = getContext().getAbsMouseX() - getArea().x - gridLeft(columns);
            int y = getContext().getAbsMouseY() - getArea().y - GRID_PADDING;
            if (x < 0 || y < 0 || x >= columns * SLOT_SIZE || y >= rows * SLOT_SIZE) return -1;
            return y / SLOT_SIZE * columns + x / SLOT_SIZE;
        }

        private int gridLeft(int columns) {
            return (getArea().w() - columns * SLOT_SIZE) / 2;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
            int columns = columns();
            int rows = rows();
            int width = getArea().w();
            int height = getArea().h();
            AE2Look look = AE2Look.of(this);
            look.drawWell(0, 0, width, height);
            boolean fluid = browser.fluid();
            AE2Theme.Variant variant = look.variant();
            IDrawable slot = variant == null ? (fluid ? GuiTextures.SLOT_FLUID : GuiTextures.SLOT_ITEM)
                : (fluid ? variant.slotFluid : variant.slotItem);
            NetworkEntry[] entries = browser.entries();
            if (entries.length == 0) {
                IKey.str(browser.emptyMessage())
                    .color(look.listText())
                    .shadow(look.listShadow())
                    .alignment(Alignment.Center)
                    .draw(context, GRID_PADDING, 0, width - 2 * GRID_PADDING, height, theme.getTheme());
                return;
            }
            int hovered = isHovering() ? hoveredIndex() : -1;
            int left = gridLeft(columns);
            for (int index = 0; index < columns * rows; index++) {
                int x = left + index % columns * SLOT_SIZE;
                int y = GRID_PADDING + index / columns * SLOT_SIZE;
                slot.draw(context, x, y, SLOT_SIZE, SLOT_SIZE, theme.getTheme());
                NetworkEntry entry = index < entries.length ? entries[index] : null;
                if (entry == null) continue;
                entry.icon()
                    .draw(context, x + 1, y + 1, 16, 16, theme.getTheme());
                if (index == hovered) GuiDraw.drawRect(x + 1, y + 1, 16, 16, 0x55FFFFFF);
                entry.count()
                    .draw(context, x + 1, y + 8, 16, 9, theme.getTheme());
            }
        }
    }
}
