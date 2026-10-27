// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtneverreset.mixins.late.appliedenergistics2.quantum;

import net.minecraft.entity.player.EntityPlayerMP;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.quantum.QuantumCraftConfirmPacket;
import com.xyp.gtneverreset.utils.crafting.quantum.ae2.IQuantumCraftConfirm;
import com.xyp.gtneverreset.utils.crafting.quantum.ae2.QuantumCraftingJob;
import com.xyp.gtneverreset.utils.crafting.quantum.ae2.QuantumCraftingTree;
import com.xyp.gtneverreset.utils.machine.QuantumComputerCpuStatus;

import appeng.api.networking.crafting.ICraftingJob;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.container.implementations.CraftingCPUStatus;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketCraftingTreeData;

@Mixin(value = ContainerCraftConfirm.class, remap = false)
public abstract class MixinContainerCraftConfirm implements IQuantumCraftConfirm {

    @Shadow
    protected ICraftingJob result;

    @Unique
    private boolean gtnr$quantumJob;

    @Inject(
        method = "detectAndSendChanges",
        at = @At(
            value = "FIELD",
            target = "Lappeng/container/implementations/ContainerCraftConfirm;result:Lappeng/api/networking/crafting/ICraftingJob;",
            opcode = Opcodes.PUTFIELD,
            shift = At.Shift.AFTER,
            remap = false),
        require = 1,
        remap = true)
    private void gtnr$syncQuantumJob(CallbackInfo ci) {
        final boolean quantumJob = this.result instanceof QuantumCraftingJob;
        if (!(((AEBaseContainer) (Object) this).getPlayerInv().player instanceof EntityPlayerMP player)) return;

        if (quantumJob != this.gtnr$quantumJob) {
            this.gtnr$quantumJob = quantumJob;
            NetWorkHandler.sendTo(new QuantumCraftConfirmPacket(quantumJob), player);
        }
        // AE2 only sends the crafting tree of its own V2 jobs
        if (this.result instanceof QuantumCraftingJob<?>job) {
            for (PacketCraftingTreeData packet : QuantumCraftingTree.createChunks(player.worldObj, job)) {
                NetworkHandler.instance.sendTo(packet, player);
            }
        }
    }

    @Inject(method = "cpuMatches", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtnr$onlyQuantumCPUs(CraftingCPUStatus c, CallbackInfoReturnable<Boolean> cir) {
        if (this.gtnr$quantumJob
            && ((QuantumComputerCpuStatus) c).gtnr$getCPUType() == QuantumComputerCpuStatus.NORMAL_CPU) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public boolean gtnr$isQuantumJob() {
        return this.gtnr$quantumJob;
    }

    @Override
    public void gtnr$setQuantumJob(boolean quantumJob) {
        this.gtnr$quantumJob = quantumJob;
    }
}
