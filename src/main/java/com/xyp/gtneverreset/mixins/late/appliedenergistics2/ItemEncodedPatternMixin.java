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

import com.xyp.gtneverreset.common.me.CircuitPatternCodec;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;
import com.xyp.gtneverreset.common.me.wildcard.WildcardPatternGenerator;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEStack;
import appeng.items.misc.ItemEncodedPattern;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mixin(value = ItemEncodedPattern.class, remap = false)
public abstract class ItemEncodedPatternMixin {

    @SideOnly(Side.CLIENT)
    @Inject(method = "addCheckedInformation", at = @At("TAIL"))
    private void gtnr$describeVirtualTools(ItemStack stack, EntityPlayer player, List<String> lines,
        boolean displayMoreInfo, CallbackInfo ci) {
        String recipeMap = PatternRecipeMap.name(stack);
        if (recipeMap != null) {
            // #tr gtnr.tooltip.gtneverreset.pattern.recipe_map
            // # Recipe type: %s
            // # zh_CN 配方池：%s
            lines.add(
                StatCollector.translateToLocalFormatted(
                    "gtnr.tooltip.gtneverreset.pattern.recipe_map",
                    StatCollector.translateToLocal(recipeMap)));
        }
        int circuit = CircuitPatternCodec.storedCircuit(stack);
        if (circuit >= 0) {
            // #tr gtnr.tooltip.gtneverreset.pattern.circuit
            // # Stored circuit configuration: %s (not requested from ME)
            // # zh_CN 内置编程电路：%s（不向 ME 索取）
            lines.add(StatCollector.translateToLocalFormatted("gtnr.tooltip.gtneverreset.pattern.circuit", circuit));
        }
        ItemStack[] molds = CircuitPatternCodec.storedMolds(stack);
        if (molds == null) return;
        for (ItemStack mold : molds) {
            // #tr gtnr.tooltip.gtneverreset.pattern.mold
            // # Stored reusable tool: %s (not requested from ME)
            // # zh_CN 内置不消耗工具：%s（不向 ME 索取）
            lines.add(
                StatCollector
                    .translateToLocalFormatted("gtnr.tooltip.gtneverreset.pattern.mold", mold.getDisplayName()));
        }
    }

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
        // #tr gtnr.tooltip.wildcardpattern.usage
        // # Right-click to configure; use NEI + to import a recipe, then save.
        // # zh_CN 右键配置，可点击 NEI 配方的＋自动填写，调整后保存。
        lines.add(StatCollector.translateToLocal("gtnr.tooltip.wildcardpattern.usage"));
        ci.cancel();
    }
}
