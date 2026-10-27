package com.xyp.gtneverreset.mixins.late.bloodmagic;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.api.packaged.BloodAltarAccess;
import com.xyp.gtneverreset.common.block.blocks.tile.TileEntityPackagedProvider;

import WayofTime.alchemicalWizardry.common.tileEntity.TEAltar;

/**
 * Stops a dispatched single-step result from becoming the next slate while its provider is unavailable.
 * The marker survives chunk and world reloads and never replaces Blood Magic's actual crafting cycle.
 */
@Mixin(value = TEAltar.class, remap = false)
public abstract class MixinPackagedBloodAltar implements BloodAltarAccess {

    @Unique
    private ItemStack gtnr$expected;

    @Override
    public void gtnr$holdResult(ItemStack expected) {
        gtnr$expected = ItemStack.copyItemStack(expected);
        ((TEAltar) (Object) this).markDirty();
    }

    @Inject(method = "startCycle", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtnr$holdCompletedResult(CallbackInfo callback) {
        if (gtnr$expected == null) return;
        TEAltar altar = (TEAltar) (Object) this;
        ItemStack actual = altar.getStackInSlot(0);
        if (actual == null) {
            gtnr$holdResult(null);
        } else if (TileEntityPackagedProvider.sameItem(actual, gtnr$expected)) {
            // setActive is Blood Magic's public stop method despite its misleading name.
            altar.setActive();
            callback.cancel();
        }
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"), remap = true, require = 1)
    private void gtnr$read(NBTTagCompound tag, CallbackInfo callback) {
        gtnr$expected = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("GTNGBloodAltarResult"));
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"), remap = true, require = 1)
    private void gtnr$write(NBTTagCompound tag, CallbackInfo callback) {
        if (gtnr$expected != null) {
            tag.setTag("GTNGBloodAltarResult", gtnr$expected.writeToNBT(new NBTTagCompound()));
        } else tag.removeTag("GTNGBloodAltarResult");
    }
}
