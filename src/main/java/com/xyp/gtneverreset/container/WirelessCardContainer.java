// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtneverreset.container;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.cleanroommc.modularui.screen.ModularContainer;
import com.xyp.gtneverreset.client.gui.WirelessCardGui.WirelessCardClientView;
import com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardSyncHandler;
import com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardViewState;
import com.xyp.gtneverreset.common.gui.WirelessFrequencyCardGui.WirelessCardViewState.Snapshot;
import com.xyp.gtneverreset.common.item.items.WirelessFrequencyCard;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessCardBinding;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessCardInventory;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class WirelessCardContainer extends ModularContainer {

    public final WirelessCardSyncHandler sync = new WirelessCardSyncHandler(this).allowC2S();

    private static final AtomicLong SESSIONS = new AtomicLong();
    public final EntityPlayer player;
    public ItemStack card;
    private final int cardSlot;
    private final IInventory cardInventory;
    private final boolean heldOnly;
    public WirelessCardBinding binding;
    private final long session = SESSIONS.incrementAndGet();
    public long revision;
    public WirelessCardViewState view = new WirelessCardViewState();
    public Snapshot snapshot;
    public NBTTagList pendingHighlight;
    public volatile NBTTagCompound display = new NBTTagCompound();
    @SideOnly(Side.CLIENT)
    private WirelessCardClientView clientView;

    @SideOnly(Side.CLIENT)
    public WirelessCardClientView clientView() {
        if (clientView == null) clientView = new WirelessCardClientView(this);
        return clientView;
    }

    public WirelessCardContainer(EntityPlayer player, int location) {
        this.player = player;
        heldOnly = location == 0;
        var slot = WirelessCardInventory.resolve(player, location);
        cardInventory = slot == null ? null : slot.inventory();
        cardSlot = slot == null ? -1 : slot.index();
        card = slot == null ? null : cardInventory.getStackInSlot(cardSlot);
        binding = card == null ? null : WirelessCardBinding.read(card.getTagCompound());
        // NetHandlerPlayServer queries this slot after onItemRightClick returns, even for an item GUI.
        if (slot != null) addSlotToContainer(new Slot(cardInventory, cardSlot, -10000, -10000) {

            @Override
            public boolean canTakeStack(EntityPlayer user) {
                return false;
            }

            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }
        });
    }

    @Override
    public boolean canInteractWith(EntityPlayer user) {
        ItemStack held = cardInventory == null ? null : cardInventory.getStackInSlot(cardSlot);
        boolean valid = user == player && !user.isDead
            && card != null
            && card.getItem() instanceof WirelessFrequencyCard
            && matchesHeldCard(cardSlot, heldOnly ? user.inventory.currentItem : cardSlot, card, held)
            && (user.worldObj.isRemote || Objects.equals(binding, WirelessCardBinding.read(held.getTagCompound())))
            && (binding == null || binding.belongsTo(user.getUniqueID()));
        // Vanilla replaces the held stack with an equal copy at the end of every right-click packet.
        // Always mutate the live inventory stack, not the object captured before that copy.
        if (valid) card = held;
        return valid;
    }

    public void selectSource(int index) {
        if (!WirelessCardBinding.select(card.getTagCompound(), index)) return;
        binding = WirelessCardBinding.read(card.getTagCompound());
        view = new WirelessCardViewState();
        pendingHighlight = null;
        WirelessCardInventory.sync(player, card);
    }

    public void deleteSource(int index) {
        if (!WirelessCardBinding.removeSource(card.getTagCompound(), index)) return;
        WirelessCardBinding next = WirelessCardBinding.read(card.getTagCompound());
        if (!Objects.equals(binding, next)) {
            binding = next;
            view = new WirelessCardViewState();
            pendingHighlight = null;
        }
        WirelessCardInventory.sync(player, card);
    }

    private static boolean matchesHeldCard(int expectedSlot, int selectedSlot, ItemStack expected, ItemStack current) {
        return expectedSlot == selectedSlot && expected != null
            && current != null
            && current.stackSize == 1
            && ItemStack.areItemStacksEqual(expected, current);
    }

    @Override
    public ItemStack slotClick(int slot, int button, int mode, EntityPlayer user) {
        return null;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer user, int slot) {
        return null;
    }

    public void update(boolean force) {
        sync.update(force);
    }

    public long sessionId() {
        return session;
    }
}
