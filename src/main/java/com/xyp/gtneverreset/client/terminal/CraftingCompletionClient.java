package com.xyp.gtneverreset.client.terminal;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.xyp.gtneverreset.api.mixinHelper.CraftingCompletionRepoAccess;
import com.xyp.gtneverreset.api.mixinHelper.CraftingCompletionTerminalAccess;

import appeng.api.storage.data.IAEItemStack;
import appeng.client.gui.implementations.GuiCraftAmount;
import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.client.gui.implementations.GuiCraftingCPU;
import appeng.client.gui.implementations.GuiCraftingStatus;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class CraftingCompletionClient {

    private static final long DISPLAY_TIME = TimeUnit.SECONDS.toNanos(60);
    private static final long PENDING_TIMEOUT = TimeUnit.SECONDS.toNanos(3);
    private static final int MAX_ITEMS = 256;
    private static final int MAX_PENDING_PACKETS = 64;
    private static final Map<GuiMEMonitorable, WeakReference<CraftingCompletionRepoAccess>> REPOSITORIES = new WeakHashMap<>();
    private static final Map<IAEItemStack, ItemStack> HISTORY = new LinkedHashMap<>();
    private static final Deque<Pending> PENDING = new ArrayDeque<>();
    private static List<ItemStack> newestFirst = List.of();
    private static World world;
    private static GuiScreen previousScreen;
    private static GuiMEMonitorable boundTerminal;
    private static long gridId;
    private static long lastSequence;
    private static long firstDisplayAt;
    private static boolean displayed;

    private CraftingCompletionClient() {}

    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(new CraftingCompletionClient());
    }

    public static void bindRepo(Object owner, CraftingCompletionRepoAccess repo) {
        if (owner instanceof GuiMEMonitorable terminal) REPOSITORIES.put(terminal, new WeakReference<>(repo));
    }

    public static List<ItemStack> items(Object screen) {
        Minecraft minecraft = Minecraft.getMinecraft();
        return screen == boundTerminal && screen == minecraft.currentScreen && minecraft.theWorld == world ? newestFirst
            : List.of();
    }

    public static void rowsDisplayed(Object screen) {
        if (displayed || items(screen).isEmpty()) return;
        displayed = true;
        firstDisplayAt = System.nanoTime();
    }

    public static void receive(Minecraft minecraft, int dimension, int windowId, long gridId, long sequence,
        List<ItemStack> items) {
        updateScreen(minecraft);
        if (sequence <= lastSequence) return;
        long now = System.nanoTime();
        PENDING.addLast(new Pending(dimension, windowId, gridId, sequence, items, now));
        while (PENDING.size() > MAX_PENDING_PACKETS) PENDING.removeFirst();
        applyPending(minecraft, now);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        updateScreen(minecraft);
        if (displayed || !PENDING.isEmpty()) applyPending(minecraft, System.nanoTime());
    }

    private static void updateScreen(Minecraft minecraft) {
        GuiScreen current = minecraft.currentScreen;
        if (world != minecraft.theWorld) {
            GuiScreen old = previousScreen;
            world = minecraft.theWorld;
            previousScreen = current;
            clearRows();
            PENDING.clear();
            boundTerminal = null;
            gridId = 0;
            lastSequence = 0;
            refresh(old, false);
            return;
        }
        if (previousScreen == current) return;
        GuiScreen old = previousScreen;
        previousScreen = current;
        boolean openingCrafting = isCraftingScreen(current)
            && (old instanceof GuiMEMonitorable || isCraftingScreen(old));
        if (openingCrafting || current instanceof GuiMEMonitorable && isCraftingScreen(old)) return;

        clearRows();
        boundTerminal = null;
        gridId = 0;
        if (!(current instanceof GuiMEMonitorable)) PENDING.clear();
        refresh(old, false);
    }

    private static boolean isCraftingScreen(GuiScreen screen) {
        return screen instanceof GuiCraftAmount || screen instanceof GuiCraftConfirm
            || screen instanceof GuiCraftingCPU
            || screen instanceof GuiCraftingStatus;
    }

    private static void applyPending(Minecraft minecraft, long now) {
        boolean changed = displayed && now - firstDisplayAt >= DISPLAY_TIME;
        if (changed) clearRows();
        boolean scrollToTop = false;
        Iterator<Pending> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            Pending packet = iterator.next();
            if (packet.sequence <= lastSequence || now - packet.receivedAt >= PENDING_TIMEOUT) {
                iterator.remove();
                continue;
            }
            if (minecraft.thePlayer == null || minecraft.thePlayer.dimension != packet.dimension
                || !(minecraft.currentScreen instanceof GuiMEMonitorable terminal)
                || terminal.inventorySlots.windowId != packet.windowId) continue;

            if (gridId != packet.gridId) {
                changed |= !HISTORY.isEmpty();
                clearRows();
            }
            changed |= boundTerminal != terminal && !HISTORY.isEmpty();
            boundTerminal = terminal;
            gridId = packet.gridId;
            lastSequence = packet.sequence;
            boolean added = remember(packet.items);
            changed |= added;
            scrollToTop |= added;
            iterator.remove();
        }
        if (changed) refresh(minecraft.currentScreen, scrollToTop);
    }

    private static boolean remember(List<ItemStack> items) {
        if (items.isEmpty()) return false;
        boolean added = false;
        for (ItemStack item : items) {
            if (item == null || item.getItem() == null) continue;
            ItemStack identity = item.copy();
            identity.stackSize = 1;
            IAEItemStack key = AEItemStack.create(identity);
            if (key == null) continue;
            HISTORY.remove(key);
            HISTORY.put(key, identity);
            added = true;
        }
        if (!added) return false;
        while (HISTORY.size() > MAX_ITEMS) HISTORY.remove(
            HISTORY.keySet()
                .iterator()
                .next());
        List<ItemStack> ordered = new ArrayList<>(HISTORY.values());
        Collections.reverse(ordered);
        newestFirst = List.copyOf(ordered);
        return true;
    }

    private static void clearRows() {
        HISTORY.clear();
        newestFirst = List.of();
        displayed = false;
    }

    private static void refresh(GuiScreen screen, boolean scrollToTop) {
        WeakReference<CraftingCompletionRepoAccess> reference = REPOSITORIES.get(screen);
        CraftingCompletionRepoAccess repo = reference == null ? null : reference.get();
        if (repo != null) repo.gtnr$refreshCompletionRows();
        if (screen instanceof CraftingCompletionTerminalAccess terminal) {
            terminal.gtnr$refreshCompletionLayout(scrollToTop);
        }
    }

    private record Pending(int dimension, int windowId, long gridId, long sequence, List<ItemStack> items,
        long receivedAt) {}
}
