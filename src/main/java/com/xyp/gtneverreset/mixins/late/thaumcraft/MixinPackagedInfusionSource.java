package com.xyp.gtneverreset.mixins.late.thaumcraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.api.mixinHelper.InfusionSourceAccess;
import com.xyp.gtneverreset.compat.thaumcraft.DirectEssentiaSupply;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.tiles.TileInfusionMatrix;

/** A new manual craft must not inherit a completed Provider job's persistent source marker. */
@Mixin(value = TileInfusionMatrix.class, remap = false)
public abstract class MixinPackagedInfusionSource implements InfusionSourceAccess {

    @Unique
    private NBTTagCompound gtnr$essentiaSource;

    @Redirect(
        method = "craftCycle",
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/api/aspects/AspectList;reduce(Lthaumcraft/api/aspects/Aspect;I)Z"),
        require = 1)
    private boolean gtnr$batchSource(AspectList remaining, Aspect aspect, int amount) {
        return DirectEssentiaSupply.reduceBatch((TileInfusionMatrix) (Object) this, remaining, aspect, amount);
    }

    @Override
    public NBTTagCompound gtnr$getEssentiaSource() {
        return gtnr$essentiaSource;
    }

    @Override
    public void gtnr$setEssentiaSource(NBTTagCompound source) {
        gtnr$essentiaSource = source;
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"), require = 1, remap = true)
    private void gtnr$readSource(NBTTagCompound tag, CallbackInfo callback) {
        gtnr$essentiaSource = tag.hasKey(DirectEssentiaSupply.TAG) ? tag.getCompoundTag(DirectEssentiaSupply.TAG)
            : null;
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"), require = 1, remap = true)
    private void gtnr$writeSource(NBTTagCompound tag, CallbackInfo callback) {
        if (gtnr$essentiaSource != null) tag.setTag(DirectEssentiaSupply.TAG, gtnr$essentiaSource);
        else tag.removeTag(DirectEssentiaSupply.TAG);
    }

    @Inject(method = "craftingStart", at = @At("HEAD"), require = 1)
    private void gtnr$resetSource(EntityPlayer player, CallbackInfo callback) {
        TileInfusionMatrix matrix = (TileInfusionMatrix) (Object) this;
        gtnr$essentiaSource = null;
        matrix.markDirty();
    }
}
