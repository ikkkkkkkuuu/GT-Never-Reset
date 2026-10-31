package com.xyp.gtneverreset.mixins.late.appliedenergistics2.quantum;

import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.api.mixinHelper.QuantumComputerCpuStatus;
import com.xyp.gtneverreset.common.machine.multiblock.QuantumComputer;
import com.xyp.gtneverreset.utils.ECraftingCPUCluster;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.container.implementations.CraftingCPUStatus;

@Mixin(value = CraftingCPUStatus.class, remap = false)
public class MixinCraftingCPUStatus implements QuantumComputerCpuStatus {

    @Unique
    private byte gtnr$cpuType = QuantumComputerCpuStatus.NORMAL_CPU;

    @Inject(method = "<init>(Lappeng/api/networking/crafting/ICraftingCPU;I)V", at = @At("RETURN"), require = 1)
    private void injectInit(final ICraftingCPU cluster, final int serial, final CallbackInfo ci) {
        if (!(cluster instanceof ECraftingCPUCluster eCluster)) {
            return;
        }

        final QuantumComputer owner = eCluster.getVirtualCPUOwner();
        if (owner == null) {
            return;
        }

        this.gtnr$cpuType = owner.isVirtualCPU(cluster) ? QuantumComputerCpuStatus.VIRTUAL_CPU
            : QuantumComputerCpuStatus.SPLIT_CPU;
    }

    @Inject(method = "<init>(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("RETURN"), require = 1)
    private void injectInit(final NBTTagCompound i, final CallbackInfo ci) {
        final byte cpuType = i.getByte("gtngQuantumComputerCPU");
        this.gtnr$cpuType = cpuType == QuantumComputerCpuStatus.VIRTUAL_CPU
            || cpuType == QuantumComputerCpuStatus.SPLIT_CPU ? cpuType : QuantumComputerCpuStatus.NORMAL_CPU;
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"), require = 1)
    private void injectWriteToNBT(final NBTTagCompound i, final CallbackInfo ci) {
        if (this.gtnr$cpuType == QuantumComputerCpuStatus.NORMAL_CPU) {
            return;
        }

        i.setByte("gtngQuantumComputerCPU", this.gtnr$cpuType);
    }

    @Override
    public byte gtnr$getCPUType() {
        return this.gtnr$cpuType;
    }
}
