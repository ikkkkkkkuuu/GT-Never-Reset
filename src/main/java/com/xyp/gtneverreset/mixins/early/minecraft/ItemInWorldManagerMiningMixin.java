package com.xyp.gtneverreset.mixins.early.minecraft;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.management.ItemInWorldManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.xyp.gtneverreset.common.item.items.ItemUniversalWand;
import com.xyp.gtneverreset.common.wand.UniversalWandMiningHandler;

@Mixin(ItemInWorldManager.class)
public abstract class ItemInWorldManagerMiningMixin {

    @Shadow
    public EntityPlayerMP thisPlayerMP;

    @Inject(method = "getBlockReachDistance", at = @At("RETURN"), cancellable = true, remap = false)
    private void gtneverreset$extendBlockReach(CallbackInfoReturnable<Double> cir) {
        if (ItemUniversalWand.hasReachBonus(thisPlayerMP))
            cir.setReturnValue(cir.getReturnValue() + ItemUniversalWand.REACH_BONUS);
    }

    @WrapMethod(method = "tryHarvestBlock")
    private boolean gtneverreset$useConfiguredMiningExhaustion(int x, int y, int z, Operation<Boolean> original) {
        EntityPlayerMP player = thisPlayerMP;
        ItemStack stack = player.getHeldItem();
        if (stack == null || !(stack.getItem() instanceof ItemUniversalWand)) return original.call(x, y, z);
        if (ItemUniversalWand.isProtectionMode(stack)) return false;
        FoodStatsMiningAccessor food = (FoodStatsMiningAccessor) player.getFoodStats();
        float exhaustionBefore = food.gtneverreset$getMiningExhaustion();
        boolean harvested = false;
        try (UniversalWandMiningHandler.HarvestScope scope = UniversalWandMiningHandler.beginHarvest(player, x, y, z)) {
            try {
                harvested = original.call(x, y, z);
                scope.complete(harvested);
                return harvested;
            } finally {
                // Account for this block before the scope mines the rest of its vein.
                food.gtneverreset$setMiningExhaustion(exhaustionBefore);
                if (harvested) ItemUniversalWand.applyMiningExhaustion(player);
            }
        }
    }

}
