package com.xyp.gtneverreset.mixins.late.appliedenergistics2;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.client.terminal.TerminalSearchCacheAccess;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.client.me.ItemRepo;

@Mixin(value = ItemRepo.class, remap = false)
public abstract class TerminalSearchCacheMixin implements TerminalSearchCacheAccess {

    @Shadow
    @Final
    private IItemList<IAEStack<?>> list;

    @Shadow
    private String searchString;

    @Shadow
    private Map<IAEStack<?>, Boolean> searchCache;

    @Unique
    private ItemRepo gtnr$previousSearchRepo;

    @Unique
    private boolean gtnr$receivedStorageUpdate;

    @Override
    public Map<IAEStack<?>, Boolean> gtnr$getSearchCache() {
        return searchCache;
    }

    @Override
    public void gtnr$inheritSearchCache(ItemRepo previousRepo) {
        if (!((TerminalSearchCacheAccess) previousRepo).gtnr$getSearchCache()
            .isEmpty()) {
            gtnr$previousSearchRepo = previousRepo;
        }
    }

    @Inject(method = "postUpdate(Lappeng/api/storage/data/IAEStack;)V", at = @At("HEAD"))
    private void gtnr$onStorageUpdate(IAEStack<?> entry, CallbackInfo callback) {
        if (gtnr$previousSearchRepo != null) gtnr$receivedStorageUpdate = true;
    }

    @Inject(method = "updateView", at = @At("HEAD"))
    private void gtnr$reuseSearchResults(CallbackInfo callback) {
        ItemRepo previousRepo = gtnr$previousSearchRepo;
        if (previousRepo == null || !gtnr$receivedStorageUpdate) return;
        gtnr$previousSearchRepo = null;
        if (!searchString.equals(previousRepo.getSearchString())) return;

        // Use the newly synced entries as keys so AE2's weak cache follows this view's lifetime.
        Map<IAEStack<?>, Boolean> previousCache = ((TerminalSearchCacheAccess) previousRepo).gtnr$getSearchCache();
        for (var cached : previousCache.entrySet()) {
            IAEStack<?> previousEntry = cached.getKey();
            IAEStack<?> entry = list.findPrecise(previousEntry);
            if (entry != null && entry.getStackSize() == previousEntry.getStackSize()
                && entry.isCraftable() == previousEntry.isCraftable()
                && entry.getCountRequestable() == previousEntry.getCountRequestable()) {
                searchCache.putIfAbsent(entry, cached.getValue());
            }
        }
    }
}
