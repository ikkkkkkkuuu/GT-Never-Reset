package com.xyp.gtneverreset.common.wireless;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtneverreset.common.wireless.WirelessChannelManager.Address;
import com.xyp.gtneverreset.common.wireless.WirelessLinkData.Claim;
import com.xyp.gtneverreset.common.wireless.WirelessLinkData.Slot;

/** Persistence and provenance checks that do not need a running Minecraft server. */
public final class WirelessFrequencyContracts {

    private static int checks;

    public static void main(String[] args) throws Exception {
        registrySurvivesRestart();
        multiSourceMigration();
        tileAndLinkRoundTrip();
        mergedNetworkKeepsOriginalFrequency();
        System.out.println("Wireless frequency contracts passed: " + checks);
    }

    private static void registrySurvivesRestart() {
        var registry = new WirelessFrequencyWorldSavedData();
        UUID id = UUID.randomUUID(), owner = UUID.randomUUID();
        Address old = address(1);
        registry.create(id, owner);
        registry.mapLegacy(old, id);
        check(registry.accept(id, 0), "initial controller epoch accepted");
        long epoch = registry.adopt(id);
        check(!registry.accept(id, 0), "old controller rejected after takeover");
        NBTTagCompound tag = new NBTTagCompound();
        registry.writeToNBT(tag);
        var restored = new WirelessFrequencyWorldSavedData();
        restored.readFromNBT(tag);
        check(!restored.accept(id, 0), "old controller still rejected after restart");
        check(restored.accept(id, epoch), "new controller accepted after restart");
        check(
            restored.frequency(id)
                .creator()
                .equals(owner),
            "creator persisted");
        check(
            restored.legacy(old)
                .equals(id),
            "legacy coordinate mapping persisted");
        check(restored.migrated(id), "migration identity persisted");
    }

    private static void multiSourceMigration() throws Exception {
        UUID owner = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
        var registry = new WirelessFrequencyWorldSavedData();
        registry.create(first, null);
        registry.create(second, null);
        registry.mapLegacy(address(1), first);
        registry.mapLegacy(address(2), second);
        var instance = WirelessFrequencyWorldSavedData.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, registry);
        try {
            NBTTagCompound card = new NBTTagCompound();
            new WirelessCardBinding(WirelessFrequency.legacy(address(1)), address(1), null, owner, "Owner").write(card);
            card.setBoolean("GTNLWirelessAutoConnect", true);
            WirelessCardBinding.renameSource(card, 0, "Factory");
            new WirelessCardBinding(WirelessFrequency.legacy(address(2)), address(2), null, owner, "Owner").write(card);
            WirelessCardBinding.renameSource(card, 1, "Storage");
            check(WirelessCardBinding.upgrade(card), "legacy card migrated");
            var sources = WirelessCardBinding.sources(card);
            check(sources.size() == 2, "all saved sources retained");
            check(
                sources.get(0)
                    .frequency()
                    .id()
                    .equals(first),
                "first saved source migrated");
            check(
                sources.get(1)
                    .frequency()
                    .id()
                    .equals(second),
                "second saved source migrated");
            check(
                sources.get(0)
                    .creator() == null,
                "migration does not trust the card owner as creator");
            check(
                WirelessCardBinding.sourceName(card, sources.get(0))
                    .equals("Factory"),
                "first name retained");
            check(
                WirelessCardBinding.sourceName(card, sources.get(1))
                    .equals("Storage"),
                "second name retained");
            check(!WirelessCardBinding.automatic(card), "selected source auto setting retained");
            check(
                WirelessCardBinding.select(card, 0) && WirelessCardBinding.automatic(card),
                "other source auto setting retained");
            check(!WirelessCardBinding.upgrade(card), "migration is idempotent");
            var moved = new WirelessCardBinding(
                sources.get(0)
                    .frequency(),
                address(99),
                owner,
                owner,
                "Owner");
            check(
                sources.get(0)
                    .sameIdentity(moved),
                "location and creator updates preserve queued card identity");
            check(
                !sources.get(1)
                    .sameIdentity(moved),
                "switching frequency invalidates queued card identity");
            moved.write(card);
            check(
                WirelessCardBinding.sources(card)
                    .size() == 2,
                "moving a controller does not duplicate its saved source");
            check(
                WirelessCardBinding.sourceName(card, moved)
                    .equals("Factory"),
                "moving preserves source name");
            check(WirelessCardBinding.automatic(card), "moving preserves auto-connect");
            check(WirelessCardBinding.removeSource(card, 0), "migrated source removable");
            check(
                WirelessCardBinding.read(card)
                    .frequency()
                    .id()
                    .equals(second),
                "remaining source selected");
            NBTTagCompound invalid = (NBTTagCompound) card.copy();
            invalid.getCompoundTag("GTNLWirelessCard")
                .setString("frequency", "invalid");
            check(WirelessCardBinding.read(invalid) == null, "malformed UUID rejected");
        } finally {
            WirelessFrequencies.clear();
        }
    }

    private static void tileAndLinkRoundTrip() {
        WirelessFrequency first = WirelessFrequency.of(UUID.randomUUID());
        WirelessFrequency second = WirelessFrequency.of(UUID.randomUUID());
        Slot slot = new Slot(ForgeDirection.NORTH, false);
        Claim claim = new Claim("test:device", Set.of(first, second), Set.of(first), Set.of(second));
        NBTTagCompound tile = new NBTTagCompound();
        check(WirelessLinkData.write(tile, Map.of(slot, claim)), "links written");
        check(
            WirelessLinkData.read(tile)
                .get(slot)
                .equals(claim),
            "raw IDs, pause and entrance flags retained");
        WirelessFrequencies.write(tile, List.of(new WirelessFrequencies.Entry(first.id(), 7)));
        NBTTagCompound saved = new NBTTagCompound();
        IWirelessTileData.save(tile, saved);
        NBTTagCompound restored = IWirelessTileData.load(saved);
        check(
            WirelessFrequencies.read(restored)
                .get(0)
                .epoch() == 7,
            "controller frequency and epoch survive tile copy");
        check(
            WirelessLinkData.read(restored)
                .get(slot)
                .equals(claim),
            "device claims survive tile copy");
        restored.getCompoundTag(WirelessFrequencies.KEY)
            .setInteger("version", 99);
        check(
            WirelessFrequencies.read(tile)
                .size() == 1,
            "tile copy has independent NBT");
        check(
            WirelessFrequencies.read(restored)
                .isEmpty(),
            "unknown controller schema rejected");

        NBTTagCompound legacy = new NBTTagCompound(), payload = new NBTTagCompound(), node = new NBTTagCompound();
        payload.setInteger("version", 1);
        node.setInteger("side", ForgeDirection.NORTH.ordinal());
        node.setBoolean("external", false);
        node.setString("kind", "test:device");
        NBTTagCompound old = WirelessLinkData.writePosition(new NBTTagCompound(), address(1));
        old.setInteger("side", ForgeDirection.UNKNOWN.ordinal());
        old.setBoolean("paused", true);
        NBTTagList frequencies = new NBTTagList(), nodes = new NBTTagList();
        frequencies.appendTag(old);
        node.setTag("frequencies", frequencies);
        nodes.appendTag(node);
        payload.setTag("nodes", nodes);
        legacy.setTag(WirelessLinkData.KEY, payload);
        check(
            WirelessLinkData.read(legacy)
                .get(slot)
                .paused()
                .contains(WirelessFrequency.legacy(address(1))),
            "v1 link and pause readable");
        payload.setInteger("version", 99);
        check(!WirelessLinkData.write(legacy, Map.of(slot, claim)), "future link schema not overwritten");
    }

    private static void mergedNetworkKeepsOriginalFrequency() {
        Object node = new Object();
        var tracker = new PhysicalClusterTracker<Object, String>(32);
        var topology = new PhysicalClusterTracker.Topology<Object>() {

            public boolean isLive(Object candidate) {
                return candidate == node;
            }

            public Iterable<Object> neighbours(Object candidate) {
                return List.of();
            }

            public boolean blocksWireless(Object candidate) {
                return false;
            }
        };
        tracker.refresh(topology, List.of(node), ignored -> Set.of(), ignored -> "A");
        tracker.link(tracker.clusterOf(node), "B", ignored -> "A");
        tracker.commitClaims(ignored -> "A");
        check(
            tracker.clusterOf(node)
                .frequencies()
                .equals(Set.of("A")),
            "merged networks share routing identity");
        check(
            tracker.claimsOf(node)
                .equals(Set.of("B")),
            "device retains original B frequency while merged");
        tracker.commitClaims(value -> value);
        check(
            tracker.clusterOf(node)
                .frequencies()
                .equals(Set.of("B")),
            "device follows B after networks split");
    }

    private static Address address(int x) {
        return new Address(0, x, 64, 0, ForgeDirection.UNKNOWN);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
