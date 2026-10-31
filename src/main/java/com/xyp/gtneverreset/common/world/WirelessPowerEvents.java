package com.xyp.gtneverreset.common.world;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.BlockEvent;

import com.xyp.gtneverreset.common.item.items.WirelessPowerCard;
import com.xyp.gtneverreset.common.wireless.IWirelessPowerReceiver;
import com.xyp.gtneverreset.common.wireless.WirelessCardInventory;
import com.xyp.gtneverreset.common.wireless.WirelessPowerSavedData;
import com.xyp.gtneverreset.common.wireless.WirelessPowerTransfer;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.metatileentity.BaseMetaTileEntity;

public final class WirelessPowerEvents {

    public static final WirelessPowerEvents INSTANCE = new WirelessPowerEvents();
    private final List<Pending> pending = new ArrayList<>();
    private long ticks;

    private WirelessPowerEvents() {}

    public void clear() {
        pending.clear();
        ticks = 0;
        WirelessPowerSavedData.clear();
    }

    @SubscribeEvent
    public void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        WirelessPowerSavedData data = WirelessPowerSavedData.get();
        if (data != null) data.remember(event.player.getUniqueID(), event.player.getCommandSenderName());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void placed(BlockEvent.PlaceEvent event) {
        if (event.world.isRemote || event.isCanceled() || event.player == null || event.player instanceof FakePlayer)
            return;
        ItemStack card = automaticCard(event.player);
        if (card == null) return;
        UUID source = WirelessPowerCard.source(card, event.player, true);
        if (event instanceof BlockEvent.MultiPlaceEvent multi) {
            for (BlockSnapshot snapshot : multi.getReplacedBlockSnapshots()) enqueue(event, snapshot, card, source);
        } else enqueue(event, event.blockSnapshot, card, source);
    }

    private void enqueue(BlockEvent.PlaceEvent event, BlockSnapshot snapshot, ItemStack card, UUID source) {
        if (snapshot.getReplacedBlock() == snapshot.getCurrentBlock()
            && snapshot.meta == snapshot.world.getBlockMetadata(snapshot.x, snapshot.y, snapshot.z)) return;
        TileEntity tile = event.world.getTileEntity(snapshot.x, snapshot.y, snapshot.z);
        if (tile instanceof BaseMetaTileEntity base) pending.add(new Pending(event, base, card, source, ticks + 4));
    }

    public static ItemStack automaticCard(EntityPlayer player) {
        ItemStack held = player.getCurrentEquippedItem();
        if (WirelessPowerCard.automatic(held)) return held;
        ItemStack selected = null;
        UUID source = null;
        for (ItemStack card : WirelessCardInventory.carried(player)) {
            if (!WirelessPowerCard.automatic(card)) continue;
            UUID candidate = WirelessPowerCard.source(card, player, false);
            if (selected != null && !candidate.equals(source)) return null;
            selected = card;
            source = candidate;
        }
        return selected;
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ticks++;
        Iterator<Pending> iterator = pending.iterator();
        while (iterator.hasNext()) {
            Pending entry = iterator.next();
            if (ticks < entry.ready) continue;
            iterator.remove();
            EntityPlayer player = entry.event.player;
            BaseMetaTileEntity tile = entry.tile;
            if (entry.event.isCanceled() || player.isDead
                || tile.isInvalid()
                || player.worldObj != tile.getWorld()
                || !tile.getWorld()
                    .blockExists(tile.xCoord, tile.yCoord, tile.zCoord)
                || tile.getWorld()
                    .getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) != tile
                || ((IWirelessPowerReceiver) tile).gtnr$powerConnection() != null
                || !WirelessCardInventory.carried(player)
                    .contains(entry.card)
                || !WirelessPowerCard.automatic(entry.card)
                || !entry.source.equals(WirelessPowerCard.source(entry.card, player, false))) continue;
            WirelessPowerTransfer.connect(tile, player, entry.source);
        }
    }

    private static final class Pending {

        private final BlockEvent.PlaceEvent event;
        private final BaseMetaTileEntity tile;
        private final ItemStack card;
        private final UUID source;
        private final long ready;

        private Pending(BlockEvent.PlaceEvent event, BaseMetaTileEntity tile, ItemStack card, UUID source, long ready) {
            this.event = event;
            this.tile = tile;
            this.card = card;
            this.source = source;
            this.ready = ready;
        }
    }
}
