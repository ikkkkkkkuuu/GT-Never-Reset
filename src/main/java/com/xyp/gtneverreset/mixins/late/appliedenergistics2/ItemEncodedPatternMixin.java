package com.xyp.gtneverreset.mixins.late.appliedenergistics2;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.utils.recipes.wildcard.WildcardPatternGenerator;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEStack;
import appeng.items.misc.ItemEncodedPattern;
import appeng.util.item.AEItemStack;

@Mixin(value = ItemEncodedPattern.class, remap = false)
public abstract class ItemEncodedPatternMixin {

    @Inject(method = "getPatternForItem", at = @At("HEAD"), cancellable = true)
    private void wildcardpattern$useLightweightPatternDetails(ItemStack item, World world,
        CallbackInfoReturnable<ICraftingPatternDetails> cir) {
        if (!WildcardPatternGenerator.isWildcardPattern(item)) {
            return;
        }
        cir.setReturnValue(WildcardPatternGenerator.getDetailsForItem(item, world));
    }

    @Inject(method = "getOutput", at = @At("HEAD"), cancellable = true)
    private void wildcardpattern$useRepresentativeOutput(ItemStack item, CallbackInfoReturnable<ItemStack> cir) {
        if (!WildcardPatternGenerator.isWildcardPattern(item)) {
            return;
        }
        cir.setReturnValue(WildcardPatternGenerator.getOutputForItem(item, null));
    }

    @Inject(method = "getOutputAE", at = @At("HEAD"), cancellable = true)
    private void wildcardpattern$useRepresentativeOutputAe(ItemStack item, CallbackInfoReturnable<IAEStack<?>> cir) {
        if (!WildcardPatternGenerator.isWildcardPattern(item)) {
            return;
        }
        ItemStack output = WildcardPatternGenerator.getOutputForItem(item, null);
        cir.setReturnValue(output == null ? null : AEItemStack.create(output.copy()));
    }

    @Inject(method = "addCheckedInformation", at = @At("HEAD"), cancellable = true)
    private void wildcardpattern$skipVanillaShiftPreview(ItemStack stack, EntityPlayer player, List<String> lines,
        boolean displayMoreInfo, CallbackInfo ci) {
        if (!WildcardPatternGenerator.isWildcardPattern(stack)) {
            return;
        }
        // #tr tooltip.wildcardpattern.usage
        // # Right-click to configure; use NEI + to import a recipe, then save.
        // # zh_CN 右键配置，可点击 NEI 配方的＋自动填写，调整后保存。
        lines.add(StatCollector.translateToLocal("tooltip.wildcardpattern.usage"));
        ci.cancel();
    }
}
