package com.xyp.gtneverreset.mixins.late.appliedenergistics2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.api.mixinHelper.CraftingCompletionRepoAccess;
import com.xyp.gtneverreset.api.mixinHelper.CraftingCompletionTerminalAccess;
import com.xyp.gtneverreset.client.gui.QuickEncodingTerminalGui;
import com.xyp.gtneverreset.client.terminal.CraftingCompletionClient;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.widgets.IScrollSource;
import appeng.client.gui.widgets.ISortSource;
import appeng.client.me.ItemRepo;
import appeng.util.item.AEItemStack;

@Mixin(value = ItemRepo.class, remap = false)
public abstract class CraftingCompletionItemRepoMixin implements CraftingCompletionRepoAccess {

    @Shadow
    @Final
    private ArrayList<IAEStack<?>> view;

    @Shadow
    @Final
    private IItemList<IAEStack<?>> list;

    @Shadow
    @Final
    private ISortSource sortSrc;

    @Shadow
    private int rowSize;

    @Shadow
    private String searchString;

    @Unique
    private final List<IAEStack<?>> gtnr$normalView = new ArrayList<>();

    @Unique
    private int gtnr$completionSlots;

    @Unique
    private int gtnr$prefixColumns;

    @Unique
    private int gtnr$prefixBeforeUpdate;

    @Shadow
    private void addEntriesToView(Iterable<IAEStack<?>> entries, IItemList<IAEStack<?>> visiblePins) {
        throw new AssertionError();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void gtnr$bindScrollSource(IScrollSource scroll, ISortSource owner, CallbackInfo callback) {
        CraftingCompletionClient.bindRepo(owner, this);
    }

    @Inject(method = "updateView", at = @At("HEAD"))
    private void gtnr$beforeUpdateView(CallbackInfo callback) {
        gtnr$prefixBeforeUpdate = gtnr$completionSlots;
        // AE2's Shift-pause can iterate the previous view and expects only real entries.
        gtnr$restoreView();
    }

    @Inject(method = "updateView", at = @At("RETURN"))
    private void gtnr$afterUpdateView(CallbackInfo callback) {
        gtnr$refreshCompletionRows();
        if (gtnr$prefixBeforeUpdate != gtnr$completionSlots && !(sortSrc instanceof QuickEncodingTerminalGui)
            && sortSrc instanceof CraftingCompletionTerminalAccess terminal) {
            terminal.gtnr$refreshCompletionLayout(false);
        }
    }

    @Inject(method = "setRowSize", at = @At("RETURN"))
    private void gtnr$reflowCompletionRows(int columns, CallbackInfo callback) {
        if (gtnr$completionSlots > 0 && columns != gtnr$prefixColumns) gtnr$refreshCompletionRows();
    }

    @Override
    public int gtnr$completionSlots() {
        return gtnr$completionSlots;
    }

    @Override
    public void gtnr$refreshCompletionRows() {
        gtnr$restoreView();
        List<ItemStack> items = CraftingCompletionClient.items(sortSrc);
        if (items.isEmpty() || rowSize <= 0) return;

        List<IAEStack<?>> candidates = new ArrayList<>();
        for (ItemStack item : items) {
            IAEStack<?> stored = list.findPrecise(AEItemStack.create(item));
            if (stored != null && (stored.getStackSize() > 0 || stored.isCraftable())) candidates.add(stored);
        }
        if (candidates.isEmpty()) return;

        List<IAEStack<?>> recent;
        int originalSize = view.size();
        String normalSearch = searchString;
        try {
            // The temporary row is independent of text search, but respects the terminal's filters.
            searchString = "";
            addEntriesToView(candidates, null);
            recent = new ArrayList<>(view.subList(originalSize, view.size()));
        } finally {
            view.subList(originalSize, view.size())
                .clear();
            searchString = normalSearch;
        }
        if (recent.isEmpty()) return;

        gtnr$normalView.addAll(view);
        Set<IAEStack<?>> moved = new HashSet<>(recent);
        view.removeIf(moved::contains);
        gtnr$prefixColumns = rowSize;
        gtnr$completionSlots = (recent.size() + rowSize - 1) / rowSize * rowSize;
        while (recent.size() < gtnr$completionSlots) recent.add(null);
        view.addAll(0, recent);
    }

    @Unique
    private void gtnr$restoreView() {
        if (gtnr$completionSlots == 0) return;
        view.clear();
        view.addAll(gtnr$normalView);
        gtnr$normalView.clear();
        gtnr$completionSlots = 0;
    }
}
