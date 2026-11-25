package com.xyp.gtneverreset.mixins.late.appliedenergistics2;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.inventory.Container;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtneverreset.api.mixinHelper.CraftingCompletionRepoAccess;
import com.xyp.gtneverreset.api.mixinHelper.CraftingCompletionTerminalAccess;
import com.xyp.gtneverreset.client.gui.QuickEncodingTerminalGui;
import com.xyp.gtneverreset.client.terminal.CraftingCompletionClient;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.slots.VirtualMEMonitorableSlot;
import appeng.client.gui.slots.VirtualMEPinSlot;
import appeng.client.gui.slots.VirtualMESlot;
import appeng.client.me.ItemRepo;

@Mixin(value = GuiMEMonitorable.class, remap = false)
public abstract class CraftingCompletionTerminalMixin extends AEBaseGui implements CraftingCompletionTerminalAccess {

    @Shadow
    @Final
    private ItemRepo repo;

    @Shadow
    private int perRow;

    @Shadow
    protected VirtualMEPinSlot[] pinSlots;

    @Shadow
    protected VirtualMEMonitorableSlot[] monitorableSlots;

    @Unique
    private final Map<VirtualMESlot, Integer> gtnr$slotShifts = new IdentityHashMap<>();

    @Unique
    private int gtnr$normalRepoColumns = -1;

    protected CraftingCompletionTerminalMixin(Container container) {
        super(container);
    }

    @Shadow
    private void setScrollBar() {
        throw new AssertionError();
    }

    @Override
    public void gtnr$refreshCompletionLayout(boolean scrollToTop) {
        if (monitorableSlots == null || (Object) this instanceof QuickEncodingTerminalGui) return;
        gtnr$updateRepoColumns();
        setScrollBar();
        if (scrollToTop) getScrollBar().setCurrentScroll(0);
        gtnr$positionSlots();
    }

    @Inject(method = "initGui()V", at = @At("HEAD"), remap = true)
    private void gtnr$restorePositions(CallbackInfo callback) {
        for (Map.Entry<VirtualMESlot, Integer> shifted : gtnr$slotShifts.entrySet()) {
            VirtualMESlot slot = shifted.getKey();
            slot.setY(slot.getY() - shifted.getValue());
        }
        gtnr$slotShifts.clear();
    }

    @Inject(method = "initGui()V", at = @At("RETURN"), remap = true)
    private void gtnr$refreshAfterInit(CallbackInfo callback) {
        if ((Object) this instanceof QuickEncodingTerminalGui) return;
        if (((CraftingCompletionRepoAccess) repo).gtnr$completionSlots() > 0 || gtnr$normalRepoColumns != -1) {
            gtnr$refreshCompletionLayout(false);
        }
    }

    @Inject(method = "drawScreen(IIF)V", at = @At("HEAD"), remap = true)
    private void gtnr$beforeDraw(int mouseX, int mouseY, float partialTicks, CallbackInfo callback) {
        gtnr$positionSlots();
    }

    @Inject(method = "drawScreen(IIF)V", at = @At("RETURN"), remap = true)
    private void gtnr$afterDraw(int mouseX, int mouseY, float partialTicks, CallbackInfo callback) {
        if (monitorableSlots == null || monitorableSlots.length == 0) return;
        int prefix = ((CraftingCompletionRepoAccess) repo).gtnr$completionSlots();
        if (prefix > getScrollBar().getCurrentScroll() * repo.getRowSize()) {
            CraftingCompletionClient.rowsDisplayed(this);
        }
    }

    @Inject(method = "setScrollBar", at = @At("RETURN"))
    private void gtnr$afterScrollRangeChange(CallbackInfo callback) {
        gtnr$positionSlots();
    }

    @Inject(method = "mouseWheelEvent", at = @At("RETURN"))
    private void gtnr$afterScroll(int mouseX, int mouseY, int wheel, CallbackInfoReturnable<Boolean> callback) {
        gtnr$positionSlots();
    }

    @Unique
    private void gtnr$updateRepoColumns() {
        // Native FULL mode changes the slot grid's width but leaves the repository at its default stride.
        if (((CraftingCompletionRepoAccess) repo).gtnr$completionSlots() > 0) {
            if (perRow <= 0 || repo.getRowSize() == perRow) return;
            if (gtnr$normalRepoColumns == -1) gtnr$normalRepoColumns = repo.getRowSize();
            repo.setRowSize(perRow);
        } else if (gtnr$normalRepoColumns != -1) {
            repo.setRowSize(gtnr$normalRepoColumns);
            gtnr$normalRepoColumns = -1;
        }
    }

    @Unique
    private void gtnr$positionSlots() {
        if ((Object) this instanceof QuickEncodingTerminalGui || pinSlots == null
            || pinSlots.length == 0
            || monitorableSlots == null
            || monitorableSlots.length == 0) return;
        int columns = repo.getRowSize();
        if (columns <= 0) return;
        int prefix = ((CraftingCompletionRepoAccess) repo).gtnr$completionSlots();
        if (prefix == 0 && gtnr$slotShifts.isEmpty()) return;

        int scroll = getScrollBar().getCurrentScroll();
        int visibleRows = Math.min(monitorableSlots.length / columns, Math.max(0, prefix / columns - scroll));
        int pinHeight = Math.max(0, gtnr$baseY(monitorableSlots[0]) - gtnr$baseY(pinSlots[0]));
        for (VirtualMEMonitorableSlot slot : monitorableSlots) {
            boolean completed = slot.getSlotIndex() + scroll * columns < prefix;
            gtnr$shift(slot, completed ? -pinHeight : 0);
        }
        for (VirtualMEPinSlot slot : pinSlots) gtnr$shift(slot, visibleRows * 18);
    }

    @Unique
    private int gtnr$baseY(VirtualMESlot slot) {
        return slot.getY() - gtnr$slotShifts.getOrDefault(slot, 0);
    }

    @Unique
    private void gtnr$shift(VirtualMESlot slot, int offset) {
        int previous = gtnr$slotShifts.getOrDefault(slot, 0);
        if (previous == offset) return;
        slot.setY(slot.getY() + offset - previous);
        if (offset == 0) gtnr$slotShifts.remove(slot);
        else gtnr$slotShifts.put(slot, offset);
    }
}
