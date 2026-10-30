package com.xyp.gtneverreset.mixins.late.gregtech;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.api.machine.OverloadedMachineConfiguration;
import com.xyp.gtneverreset.utils.machine.OverloadedMachineMolds;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;

@Mixin(value = MTEBasicMachine.class, remap = false)
public abstract class OverloadedMachineConfigurationMixin implements OverloadedMachineConfiguration {

    @Unique
    private ItemStack gtnr$mold;
    @Unique
    private ItemStack gtnr$previousCircuit;
    @Unique
    private ItemStack gtnr$previousMold;
    @Unique
    private boolean gtnr$configured;
    @Unique
    private boolean gtnr$pending;
    @Unique
    private int[] gtnr$provider;

    @Override
    public ItemStack gtnr$getVirtualMold() {
        return gtnr$mold == null ? null : gtnr$mold.copy();
    }

    @Override
    public void gtnr$setVirtualMold(ItemStack mold) {
        gtnr$mold = OverloadedMachineMolds.at(OverloadedMachineMolds.indexOf(mold));
    }

    @Override
    public void gtnr$beginConfiguration(ItemStack previousCircuit) {
        if (!gtnr$configured) {
            gtnr$previousCircuit = previousCircuit == null ? null : previousCircuit.copy();
            gtnr$previousMold = gtnr$getVirtualMold();
        }
        gtnr$configured = true;
    }

    @Override
    public void gtnr$setPending(boolean pending) {
        gtnr$pending = pending;
    }

    @Override
    public boolean gtnr$hasPending() {
        return gtnr$pending;
    }

    @Override
    public void gtnr$setProvider(int x, int y, int z) {
        gtnr$provider = new int[] { x, y, z };
    }

    @Inject(method = "saveNBTData", at = @At("TAIL"))
    private void gtnr$save(NBTTagCompound tag, CallbackInfo ci) {
        if (!gtnr$configured && gtnr$mold == null) {
            tag.removeTag("OverloadedConfiguration");
            return;
        }
        NBTTagCompound state = new NBTTagCompound();
        state.setBoolean("Active", gtnr$configured);
        state.setBoolean("Pending", gtnr$pending);
        if (gtnr$provider != null) state.setIntArray("Provider", gtnr$provider);
        if (gtnr$mold != null) state.setTag("Mold", gtnr$mold.writeToNBT(new NBTTagCompound()));
        if (gtnr$previousMold != null) state.setTag("PreviousMold", gtnr$previousMold.writeToNBT(new NBTTagCompound()));
        if (gtnr$previousCircuit != null)
            state.setTag("PreviousCircuit", gtnr$previousCircuit.writeToNBT(new NBTTagCompound()));
        tag.setTag("OverloadedConfiguration", state);
    }

    @Inject(method = "loadNBTData", at = @At("TAIL"))
    private void gtnr$load(NBTTagCompound tag, CallbackInfo ci) {
        NBTTagCompound state = tag.getCompoundTag("OverloadedConfiguration");
        gtnr$configured = state.getBoolean("Active");
        gtnr$pending = state.getBoolean("Pending");
        gtnr$provider = state.hasKey("Provider") ? state.getIntArray("Provider") : null;
        if (gtnr$provider != null && gtnr$provider.length != 3) gtnr$provider = null;
        gtnr$setVirtualMold(ItemStack.loadItemStackFromNBT(state.getCompoundTag("Mold")));
        gtnr$previousMold = ItemStack.loadItemStackFromNBT(state.getCompoundTag("PreviousMold"));
        gtnr$previousCircuit = ItemStack.loadItemStackFromNBT(state.getCompoundTag("PreviousCircuit"));
    }

    @Inject(method = "onPostTick", at = @At("TAIL"))
    private void gtnr$release(IGregTechTileEntity tile, long tick, CallbackInfo ci) {
        if (!tile.isServerSide() || !gtnr$configured) return;
        if (gtnr$pending && gtnr$provider != null
            && tick % 20 == 0
            && tile.getWorld()
                .blockExists(gtnr$provider[0], gtnr$provider[1], gtnr$provider[2])) {
            var origin = tile.getWorld()
                .getTileEntity(gtnr$provider[0], gtnr$provider[1], gtnr$provider[2]);
            if (!(origin instanceof com.xyp.gtneverreset.common.block.blocks.tile.TileEntityOverloadedProvider provider)
                || !provider.hasPendingDeliveryTo(tile.getXCoord(), tile.getYCoord(), tile.getZCoord())) {
                gtnr$pending = false;
                tile.markDirty();
            }
        }
        if (gtnr$pending) return;
        MTEBasicMachine machine = (MTEBasicMachine) (Object) this;
        if (machine.mMaxProgresstime > 0) return;
        for (int slot = machine.getInputSlot(); slot < machine.getInputSlot() + machine.mInputSlotCount; slot++) {
            ItemStack stack = machine.getStackInSlot(slot);
            if (stack != null && stack.stackSize > 0) return;
        }
        if (machine.getFillableStack() != null && machine.getFillableStack().amount > 0) return;
        tile.setInventorySlotContents(machine.getCircuitSlot(), gtnr$previousCircuit);
        gtnr$previousCircuit = null;
        gtnr$mold = gtnr$previousMold;
        gtnr$previousMold = null;
        gtnr$configured = false;
        gtnr$provider = null;
        tile.markDirty();
    }

    @Inject(method = { "getAllInputs", "appendSelectedCircuit" }, at = @At("RETURN"), cancellable = true)
    private void gtnr$append(CallbackInfoReturnable<ItemStack[]> cir) {
        if (gtnr$mold != null) cir.setReturnValue(OverloadedMachineMolds.append(cir.getReturnValue(), gtnr$mold));
    }
}
