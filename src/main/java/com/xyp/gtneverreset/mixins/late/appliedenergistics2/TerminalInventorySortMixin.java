package com.xyp.gtneverreset.mixins.late.appliedenergistics2;

import java.util.Comparator;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.xyp.gtneverreset.compat.ae2.TerminalInventorySort;

import appeng.api.config.SortDir;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.widgets.ISortSource;
import appeng.client.me.ItemRepo;
import appeng.util.InvTweakSortingModule;
import appeng.util.ItemSorters;

@Mixin(value = ItemRepo.class, remap = false)
public abstract class TerminalInventorySortMixin {

    @Shadow
    @Final
    private ISortSource sortSrc;

    @ModifyArg(
        method = "updateView",
        at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;sort(Ljava/util/Comparator;)V"),
        index = 0,
        require = 1)
    private Comparator<IAEStack<?>> gtnr$cacheInventorySortKeys(Comparator<IAEStack<?>> original) {
        return original == ItemSorters.CONFIG_BASED_SORT_BY_INV_TWEAKS && InvTweakSortingModule.isLoaded()
            ? new TerminalInventorySort(((SortDir) sortSrc.getSortDir()).sortHint)
            : original;
    }
}
