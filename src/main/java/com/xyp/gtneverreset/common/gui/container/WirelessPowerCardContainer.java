package com.xyp.gtneverreset.common.gui.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.screen.ModularContainer;
import com.xyp.gtneverreset.common.item.items.WirelessPowerCard;
import com.xyp.gtneverreset.common.wireless.WirelessCardInventory;

public final class WirelessPowerCardContainer extends ModularContainer {

    private final EntityPlayer player;
    private final IInventory cardInventory;
    private final int cardSlot;
    private final boolean heldOnly;
    private ItemStack card;

    public WirelessPowerCardContainer(EntityPlayer player, int location) {
        this.player = player;
        heldOnly = location == 0;
        var slot = WirelessCardInventory.resolve(player, location);
        cardInventory = slot == null ? null : slot.inventory();
        cardSlot = slot == null ? -1 : slot.index();
        card = slot == null ? null : cardInventory.getStackInSlot(cardSlot);
        // Vanilla looks up the held slot after opening an item GUI to synchronize the right-click result.
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
        ItemStack current = cardInventory == null ? null : cardInventory.getStackInSlot(cardSlot);
        boolean valid = user == player && !user.isDead
            && card != null
            && card.getItem() instanceof WirelessPowerCard
            && current != null
            && current.stackSize == 1
            && (!heldOnly || user.inventory.currentItem == cardSlot)
            && ItemStack.areItemStacksEqual(card, current);
        // Right-click replaces the held stack with an equal copy; edits must reach that live inventory stack.
        if (valid) card = current;
        return valid;
    }

    public ItemStack card() {
        return card;
    }

    @Override
    public ItemStack slotClick(int slot, int button, int mode, EntityPlayer user) {
        return null;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer user, int slot) {
        return null;
    }
}
