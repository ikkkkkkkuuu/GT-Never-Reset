// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #655 at 81cd514129dbb2ce06fdea2d8d3cbdbeb14b05d8.

package com.xyp.gtneverreset.utils.machine.wireless;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;

import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;

import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBuild;
import com.recursive_pineapple.matter_manipulator.common.building.PendingMove;
import com.xyp.gtneverreset.common.item.WirelessFrequencyCard;
import com.xyp.gtneverreset.mixins.late.matterManipulator.MixinPendingBlockWireless;
import com.xyp.gtneverreset.mixins.late.matterManipulator.MixinPendingBuildWireless;
import com.xyp.gtneverreset.mixins.late.matterManipulator.MixinPendingMoveWireless;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessCardInventory.TrackedCard;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessChannelManager.Address;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessConstructionSites.Site;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessLinkData.Claim;

import appeng.api.parts.IPart;
import appeng.tile.networking.TileCableBus;
import cpw.mods.fml.common.registry.RegistryDelegate;

public final class WirelessCardCompatibilityChecks {

    private static final UUID OWNER = UUID.fromString("c209c099-1b44-43fd-9dc2-5a6fc903415b");
    private static final Address SOURCE_A = position(1000);
    private static final Address SOURCE_B = position(2000);
    private static final WirelessCardBinding BINDING = new WirelessCardBinding(SOURCE_A, OWNER, "owner");
    private static int assertions;

    private WirelessCardCompatibilityChecks() {}

    public static void main(String[] args) throws ReflectiveOperationException {
        nativeInventoryMoves();
        trackedIdentity();
        invalidReplacements();
        savedSourcesAndAuthorization();
        copiedFrequencies();
        constructionSites();
        restoreQueueQuotas();
        try (PasteFixture fixture = new PasteFixture()) {
            currentPastedTargets(fixture);
            partialPaste(fixture);
        }
        injectionTargets(MixinPendingBlockWireless.class, PendingBlock.class);
        injectionTargets(MixinPendingBuildWireless.class, PendingBuild.class);
        injectionTargets(MixinPendingMoveWireless.class, PendingMove.class);
        System.out.println("Wireless card compatibility: " + assertions + " assertions passed");
    }

    private static void nativeInventoryMoves() throws ReflectiveOperationException {
        EntityPlayer player = allocate(EntityPlayerMP.class);
        player.inventory = new InventoryPlayer(player);
        TestContainer container = new TestContainer(player);
        for (int mode : new int[] { 0, 1, 2 }) {
            player.inventory.clearInventory(null, -1);
            ItemStack original = card(new Item());
            TrackedCard tracked = new TrackedCard(original, BINDING);
            player.inventory.mainInventory[10] = original;
            container.slotClick(10, 0, mode, player);
            if (mode == 0) container.slotClick(4, 0, 0, player);
            ItemStack moved = player.inventory.mainInventory[mode == 0 ? 4 : mode == 1 ? 35 : 0];
            check(moved != original && moved.stackSize == 1, "vanilla move creates a live copy: " + mode);
            check(
                tracked.present(WirelessCardInventory.collect(player.inventory.mainInventory, null)),
                "cursor, shift and number-key moves keep the pending card: " + mode);
            WirelessCardBinding.toggleAutomatic(moved);
            check(
                !tracked.present(WirelessCardInventory.collect(player.inventory.mainInventory, null)),
                "turning off the adopted card cancels the pending task: " + mode);
        }
    }

    private static void trackedIdentity() {
        ItemStack original = card(new Item());
        UUID identity = WirelessCardBinding.identity(original.getTagCompound());
        check(identity != null && !WirelessCardBinding.identify(original), "identity is assigned only once");
        TrackedCard tracked = new TrackedCard(original, BINDING);
        ItemStack live = original.copy();
        live.setStackDisplayName("renamed");
        check(tracked.present(List.of(live)), "renaming a live copy keeps its identity");
        InventoryBasic worn = new InventoryBasic("test", false, 4);
        worn.setInventorySlotContents(1, live.copy());
        check(
            tracked.present(WirelessCardInventory.collect(new ItemStack[36], worn)),
            "moving the card into Baubles keeps its identity");
        check(!tracked.present(List.of()), "removed cards and cursor-only cards cannot authorize a task");
        check(
            !identity.equals(WirelessCardBinding.identity(card(new Item()).getTagCompound())),
            "independent cards receive distinct identities");

        ItemStack duplicate = original.copy();
        WirelessCardBinding.toggleAutomatic(original);
        check(
            !new TrackedCard(original, BINDING).present(List.of(duplicate, original)),
            "an enabled duplicate cannot replace a disabled original that is still carried");
    }

    private static void invalidReplacements() {
        ItemStack original = card(new Item());
        check(
            !new TrackedCard(original, BINDING).present(List.of(original.copy(), original.copy())),
            "duplicate identities cannot replace an absent original");
        for (int variant = 0; variant < 8; variant++) {
            ItemStack replacement = original.copy();
            switch (variant) {
                case 0 -> replacement = card(original.getItem());
                case 1 -> {
                    replacement = new ItemStack(new Item());
                    replacement.setTagCompound(
                        (NBTTagCompound) original.getTagCompound()
                            .copy());
                }
                case 2 -> replacement.setItemDamage(1);
                case 3 -> new WirelessCardBinding(SOURCE_A, UUID.randomUUID(), "other")
                    .write(replacement.getTagCompound());
                case 4 -> new WirelessCardBinding(SOURCE_B, OWNER, "owner").write(replacement.getTagCompound());
                case 5 -> WirelessCardBinding.toggleAutomatic(replacement);
                case 6 -> replacement.stackSize = 0;
                case 7 -> WirelessCardBinding.clear(replacement.getTagCompound());
            }
            check(
                !new TrackedCard(original, BINDING).present(List.of(replacement)),
                "reject changed identity, item, damage, owner, source, toggle, size or cleared binding: " + variant);
        }
    }

    private static void savedSourcesAndAuthorization() throws ReflectiveOperationException {
        WirelessFrequencyCard item = allocate(WirelessFrequencyCard.class);
        field(Item.class, "delegate").set(item, new RegistryDelegate.Delegate<>(item, Item.class));
        ItemStack card = card(item);
        NBTTagCompound tag = card.getTagCompound();
        UUID identity = WirelessCardBinding.identity(tag);
        WirelessCardBinding second = new WirelessCardBinding(SOURCE_B, OWNER, "owner");
        check(WirelessCardBinding.renameSource(tag, 0, "First source"), "the first source can be named");
        second.write(tag);
        check(
            WirelessCardBinding.sources(tag)
                .equals(List.of(BINDING, second)),
            "both sources remain saved");
        check(!WirelessCardBinding.automatic(tag), "a new source keeps its own disabled automatic preference");
        check(WirelessCardBinding.renameSource(tag, 1, "Second source"), "the second source can be named");
        check(
            WirelessCardInventory.hasBoundCard(OWNER, SOURCE_B, List.of(card)),
            "current frequency authorizes restoration");
        check(
            !WirelessCardInventory.hasBoundCard(OWNER, SOURCE_A, List.of(card)),
            "a saved historical frequency does not authorize restoration");
        check(
            !WirelessCardInventory.hasBoundCard(UUID.randomUUID(), SOURCE_B, List.of(card)),
            "carrying another player's bound card does not authorize restoration");
        check(WirelessCardBinding.select(tag, 0), "the first saved source can be selected");
        check(BINDING.equals(WirelessCardBinding.read(tag)), "selection restores the current source");
        check(WirelessCardBinding.automatic(tag), "selection restores that source's automatic preference");
        check("First source".equals(WirelessCardBinding.sourceName(tag, BINDING)), "selection preserves source names");
        check("Second source".equals(WirelessCardBinding.sourceName(tag, second)), "other source names remain saved");
        WirelessCardBinding.toggleAutomatic(card);
        check(
            WirelessCardInventory.hasBoundCard(OWNER, SOURCE_A, List.of(card)),
            "restoration does not require automatic mode to be enabled");
        check(WirelessCardBinding.select(tag, 1), "the second saved source remains selectable");
        check(!WirelessCardBinding.automatic(tag), "the second source keeps its own automatic preference");
        check(WirelessCardBinding.select(tag, 0), "switching back is possible");
        check(!WirelessCardBinding.automatic(tag), "a changed per-source preference survives switching");
        check(identity.equals(WirelessCardBinding.identity(tag)), "switching sources does not replace card identity");
        check(WirelessCardBinding.removeSource(tag, 0), "removing the current saved source succeeds");
        check(second.equals(WirelessCardBinding.read(tag)), "removing the current source selects the remaining source");
        check(
            WirelessCardBinding.sources(tag)
                .equals(List.of(second)),
            "only the removed source is deleted");
        check(identity.equals(WirelessCardBinding.identity(tag)), "removing sources preserves card identity");
        WirelessCardBinding.clear(tag);
        check(
            WirelessCardBinding.sources(tag)
                .isEmpty(),
            "clearing the last source removes the binding");
        check(identity.equals(WirelessCardBinding.identity(tag)), "clearing the binding preserves card identity");

        ItemStack ordinary = card(new Item());
        check(
            !WirelessCardInventory.hasBoundCard(OWNER, SOURCE_A, List.of(ordinary)),
            "copying card NBT onto an ordinary item cannot authorize restoration");
        BINDING.write(tag);
        card.stackSize = 0;
        check(
            !WirelessCardInventory.hasBoundCard(OWNER, SOURCE_A, List.of(card)),
            "empty stacks cannot authorize restoration");
    }

    private static void copiedFrequencies() {
        Map<ForgeDirection, Address> captured = WirelessCopiedLinks.frequencies(
            Map.of(
                new WirelessLinkData.Slot(ForgeDirection.NORTH, false),
                new Claim("part", Set.of(SOURCE_A), Set.of(), Set.of()),
                new WirelessLinkData.Slot(ForgeDirection.SOUTH, false),
                new Claim("part", Set.of(SOURCE_B), Set.of(), Set.of()),
                new WirelessLinkData.Slot(ForgeDirection.UP, true),
                new Claim("external", Set.of(SOURCE_B), Set.of(), Set.of())));
        check(
            Map.of(ForgeDirection.NORTH, SOURCE_A, ForgeDirection.SOUTH, SOURCE_B)
                .equals(captured),
            "copying preserves per-part frequencies and excludes external-facing nodes");
        check(
            WirelessCopiedLinks.frequencies(
                Map.of(
                    new WirelessLinkData.Slot(ForgeDirection.NORTH, false),
                    new Claim("part", Set.of(SOURCE_A, SOURCE_B), Set.of(), Set.of())))
                == null,
            "conflicted parts cannot supply a copied frequency");
        Map<ForgeDirection, Address> rotated = WirelessCopiedLinks.rotate(
            Map.of(ForgeDirection.NORTH, SOURCE_A, ForgeDirection.UNKNOWN, SOURCE_B),
            side -> ForgeDirection.EAST);
        check(
            Map.of(ForgeDirection.EAST, SOURCE_A, ForgeDirection.UNKNOWN, SOURCE_B)
                .equals(rotated),
            "rotation moves sided frequencies and preserves centre-cable frequency");
    }

    private static void constructionSites() {
        WirelessConstructionSites.clear();
        Site site = new Site(0, 0, 60, 0, 9, 69, 9);
        Address inside = new Address(0, 4, 64, 4, ForgeDirection.UNKNOWN);
        Object first = new Object();
        Object second = new Object();
        try {
            WirelessConstructionSites.update(first, true, site);
            for (int tick = 0; tick < WirelessConstructionSites.QUIET_TICKS; tick++) WirelessConstructionSites.tick();
            check(WirelessConstructionSites.contains(inside), "construction survives gaps between placement steps");
            WirelessConstructionSites.tick();
            check(!WirelessConstructionSites.contains(inside), "abandoned construction expires");
            WirelessConstructionSites.update(first, true, site);
            WirelessConstructionSites.update(second, true, site);
            WirelessConstructionSites.update(first, false);
            check(
                WirelessConstructionSites.contains(inside),
                "one finished task does not release another task's region");
            WirelessConstructionSites.update(second, false);
            check(
                !WirelessConstructionSites.contains(inside),
                "the final finished task immediately releases its region");
            WirelessConstructionSites.update(first, true, site, site.offset(100, 0, 0));
            check(
                WirelessConstructionSites.contains(new Address(0, 104, 64, 4, ForgeDirection.UNKNOWN)),
                "moves protect both source and target regions");
            check(
                !WirelessConstructionSites.contains(new Address(1, 4, 64, 4, ForgeDirection.UNKNOWN)),
                "construction in another dimension does not block the position");
        } finally {
            WirelessConstructionSites.clear();
        }
    }

    private static void restoreQueueQuotas() {
        WirelessCopiedLinks.clear();
        try {
            boolean accepted = true;
            for (int index = 0; index < WirelessCopiedLinks.MAX_PENDING_PER_PLAYER; index++) {
                accepted &= WirelessCopiedLinks.enqueue(OWNER, position(index), null, null, SOURCE_A);
            }
            check(accepted, "one player may fill their restoration quota");
            check(
                !WirelessCopiedLinks.enqueue(OWNER, position(-1), null, null, SOURCE_A),
                "one player cannot exceed the restoration quota");
            check(
                WirelessCopiedLinks.enqueue(UUID.randomUUID(), position(-2), null, null, SOURCE_B),
                "another player retains queue capacity");
            int before = WirelessCopiedLinks.waiting();
            WirelessCopiedLinks.tick();
            check(
                WirelessCopiedLinks.waiting() == before - WirelessCopiedLinks.SCANS_PER_TICK,
                "one tick only scans the bounded number of queued entries");
            check(
                WirelessCopiedLinks.enqueue(OWNER, position(-1), null, null, SOURCE_A),
                "finished entries return the player's restoration quota");
        } finally {
            WirelessCopiedLinks.clear();
        }
    }

    private static void currentPastedTargets(PasteFixture fixture) throws ReflectiveOperationException {
        TileEntity first = fixture.put(new TileEntity(), 1);
        TestHost second = fixture.host(2);
        IPart original = part(new ItemStack(new Item()));
        second.parts.put(ForgeDirection.UNKNOWN, original);
        WirelessCopiedLinks.enqueue(OWNER, position(1), first, null, SOURCE_A);
        WirelessCopiedLinks.enqueue(OWNER, position(2), second, original, SOURCE_B);
        List<Address> cluster = List.of(position(1), position(2));
        check(!WirelessCopiedLinks.clusterAgrees(SOURCE_A, cluster), "two current pasted frequencies conflict");
        second.invalidate();
        check(WirelessCopiedLinks.clusterAgrees(SOURCE_A, cluster), "invalidated tiles cannot vote in a conflict");
        second.validate();
        check(!WirelessCopiedLinks.clusterAgrees(SOURCE_A, cluster), "the same valid tile still votes");
        fixture.put(second, 3);
        fixture.put(new TileEntity(), 2);
        check(WirelessCopiedLinks.clusterAgrees(SOURCE_A, cluster), "moved tiles cannot vote at the old location");
        fixture.put(second, 2);
        fixture.put(new TileEntity(), 2);
        check(WirelessCopiedLinks.clusterAgrees(SOURCE_A, cluster), "replaced but valid tiles cannot vote");
        fixture.put(second, 2);
        second.parts.put(ForgeDirection.UNKNOWN, part(new ItemStack(new Item())));
        check(WirelessCopiedLinks.clusterAgrees(SOURCE_A, cluster), "replaced parts cannot vote from the same host");
        second.parts.put(ForgeDirection.UNKNOWN, original);
        fixture.world.loaded = false;
        check(WirelessCopiedLinks.clusterAgrees(SOURCE_A, cluster), "unloaded targets are not resolved or counted");
        fixture.world.loaded = true;
        second.setWorldObj(allocate(TestWorld.class));
        check(WirelessCopiedLinks.clusterAgrees(SOURCE_A, cluster), "a tile in another world cannot vote");
        second.setWorldObj(fixture.world);
        check(!WirelessCopiedLinks.clusterAgrees(SOURCE_A, cluster), "restoring the genuine target restores its vote");
        WirelessCopiedLinks.clear();
    }

    private static void partialPaste(PasteFixture fixture) throws ReflectiveOperationException {
        Item item = new Item();
        ItemStack expectedNorth = new ItemStack(item, 1, 7);
        TestHost source = fixture.host(10);
        source.parts.put(ForgeDirection.NORTH, part(expectedNorth));
        source.parts.put(ForgeDirection.SOUTH, part(new ItemStack(item, 1, 4)));
        WirelessLinkData.write(
            source.data,
            Map.of(
                new WirelessLinkData.Slot(ForgeDirection.NORTH, false),
                new Claim("part", Set.of(SOURCE_A), Set.of(), Set.of()),
                new WirelessLinkData.Slot(ForgeDirection.SOUTH, false),
                new Claim("part", Set.of(SOURCE_B), Set.of(), Set.of())));
        var captured = WirelessCopiedLinks.capture(source);
        TestHost target = fixture.host(20);
        target.parts.put(ForgeDirection.NORTH, part(new ItemStack(item, 1, 6)));
        target.parts.put(ForgeDirection.SOUTH, part(new ItemStack(item, 1, 4)));
        WirelessCopiedLinks.pasted(fixture.player, target, captured);
        check(WirelessCopiedLinks.waiting() == 1, "a residual part with the wrong metadata is not claimed");
        List<Address> sides = List
            .of(new Address(0, 20, 64, 0, ForgeDirection.NORTH), new Address(0, 20, 64, 0, ForgeDirection.SOUTH));
        check(
            WirelessCopiedLinks.clusterAgrees(SOURCE_B, sides),
            "a failed replacement cannot introduce its intended frequency into conflict checks");
        WirelessCopiedLinks.clear();
        target.parts.put(ForgeDirection.NORTH, part(new ItemStack(new Item(), 1, 7)));
        WirelessCopiedLinks.pasted(fixture.player, target, captured);
        check(WirelessCopiedLinks.waiting() == 1, "an unrelated item with matching metadata is not claimed");
        WirelessCopiedLinks.clear();
        expectedNorth.setItemDamage(8);
        target.parts.put(ForgeDirection.NORTH, part(new ItemStack(item, 1, 7)));
        WirelessCopiedLinks.pasted(fixture.player, target, captured);
        check(
            WirelessCopiedLinks.waiting() == 2,
            "captured item identities do not change when the source stack changes");
        check(!WirelessCopiedLinks.clusterAgrees(SOURCE_A, sides), "genuine matching parts keep their own frequencies");
        WirelessCopiedLinks.clear();
    }

    private static void injectionTargets(Class<?> mixin, Class<?> target) throws ReflectiveOperationException {
        for (Method handler : mixin.getDeclaredMethods()) {
            Inject inject = handler.getAnnotation(Inject.class);
            if (inject == null) continue;
            for (String selector : inject.method()) {
                int open = selector.indexOf('(');
                String name = open < 0 ? selector : selector.substring(0, open);
                int matches = 0;
                if (name.equals("<init>")) {
                    for (var constructor : target.getDeclaredConstructors()) {
                        if (open < 0 || selector.substring(open)
                            .equals(Type.getConstructorDescriptor(constructor))) {
                            matches++;
                        }
                    }
                } else {
                    for (Method method : target.getDeclaredMethods()) {
                        if (method.isBridge() || !method.getName()
                            .equals(name)) continue;
                        if (open < 0 || selector.substring(open)
                            .equals(Type.getMethodDescriptor(method))) matches++;
                    }
                }
                check(
                    name.equals("<init>") && open < 0 ? matches > 0 : matches == 1,
                    mixin.getSimpleName() + " selects a compatible " + target.getSimpleName() + "." + selector);
            }
        }
        for (Field shadow : mixin.getDeclaredFields()) {
            if (shadow.getAnnotation(Shadow.class) == null) continue;
            Field actual = target.getDeclaredField(shadow.getName());
            check(
                actual.getType() == shadow.getType(),
                mixin.getSimpleName() + " keeps the MM field signature: " + shadow.getName());
        }
    }

    private static IPart part(ItemStack stack) {
        return (IPart) Proxy.newProxyInstance(
            IPart.class.getClassLoader(),
            new Class<?>[] { IPart.class },
            (proxy, method, args) -> method.getName()
                .equals("getItemStack") ? stack : null);
    }

    private static ItemStack card(Item item) {
        ItemStack card = new ItemStack(item);
        card.setTagCompound(new NBTTagCompound());
        BINDING.write(card.getTagCompound());
        WirelessCardBinding.toggleAutomatic(card);
        WirelessCardBinding.identify(card);
        return card;
    }

    private static Address position(int x) {
        return new Address(0, x, 64, 0, ForgeDirection.UNKNOWN);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static Field field(Class<?> type, String name) throws ReflectiveOperationException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @SuppressWarnings("unchecked")
    private static <T> T allocate(Class<T> type) throws ReflectiveOperationException {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        return (T) unsafe.getMethod("allocateInstance", Class.class)
            .invoke(field(unsafe, "theUnsafe").get(null), type);
    }

    private static final class PasteFixture implements AutoCloseable {

        private final TestWorld world;
        private final TestPlayer player;
        private final Map<Integer, WorldServer> worlds;
        private final WorldServer previous;

        @SuppressWarnings("unchecked")
        private PasteFixture() throws ReflectiveOperationException {
            world = allocate(TestWorld.class);
            world.tiles = new HashMap<>();
            world.loaded = true;
            field(World.class, "provider").set(world, new WorldProviderSurface());
            player = allocate(TestPlayer.class);
            player.worldObj = world;
            worlds = (Map<Integer, WorldServer>) field(DimensionManager.class, "worlds").get(null);
            previous = worlds.put(0, world);
            WirelessCopiedLinks.clear();
        }

        private <T extends TileEntity> T put(T tile, int x) {
            world.tiles.values()
                .removeIf(existing -> existing == tile);
            tile.setWorldObj(world);
            tile.xCoord = x;
            tile.yCoord = 64;
            tile.zCoord = 0;
            world.tiles.put(position(x), tile);
            return tile;
        }

        private TestHost host(int x) throws ReflectiveOperationException {
            TestHost host = allocate(TestHost.class);
            host.parts = new EnumMap<>(ForgeDirection.class);
            host.data = new NBTTagCompound();
            return put(host, x);
        }

        @Override
        public void close() {
            WirelessCopiedLinks.clear();
            WirelessChannelManager.invalidateControllerSources();
            if (previous == null) worlds.remove(0);
            else worlds.put(0, previous);
        }
    }

    private static final class TestWorld extends WorldServer {

        private Map<Address, TileEntity> tiles;
        private boolean loaded;

        private TestWorld() {
            super(null, null, null, 0, null, null);
        }

        @Override
        public boolean blockExists(int x, int y, int z) {
            return loaded;
        }

        @Override
        public TileEntity getTileEntity(int x, int y, int z) {
            if (!loaded) throw new AssertionError("restoration accessed an unloaded block");
            return tiles.get(new Address(0, x, y, z, ForgeDirection.UNKNOWN));
        }
    }

    private static final class TestPlayer extends EntityPlayerMP {

        private TestPlayer() {
            super(null, null, null, null);
        }

        @Override
        public UUID getUniqueID() {
            return OWNER;
        }
    }

    private static final class TestHost extends TileCableBus implements IWirelessTileData {

        private Map<ForgeDirection, IPart> parts;
        private NBTTagCompound data;

        @Override
        public IPart getPart(ForgeDirection side) {
            return parts.get(side);
        }

        @Override
        public NBTTagCompound gtnr$wirelessData() {
            return data;
        }

        @Override
        public boolean gtnr$hasWirelessData() {
            return data.hasKey(KEY, 10);
        }

        @Override
        public void invalidate() {
            tileEntityInvalid = true;
        }

        @Override
        public void validate() {
            tileEntityInvalid = false;
        }
    }

    private static final class TestContainer extends Container {

        private TestContainer(EntityPlayer player) {
            for (int slot = 0; slot < player.inventory.mainInventory.length; slot++) {
                addSlotToContainer(new Slot(player.inventory, slot, 0, 0));
            }
        }

        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return true;
        }

        @Override
        public ItemStack transferStackInSlot(EntityPlayer player, int index) {
            Slot slot = (Slot) inventorySlots.get(index);
            ItemStack stack = slot.getStack();
            if (stack == null || index < 9) return null;
            ItemStack previous = stack.copy();
            if (!mergeItemStack(stack, 27, 36, true)) return null;
            if (stack.stackSize == 0) slot.putStack(null);
            return previous;
        }
    }
}
