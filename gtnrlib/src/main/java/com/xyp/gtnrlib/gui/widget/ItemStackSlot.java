package com.xyp.gtnrlib.gui.widget;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.MCHelper;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerGhostIngredientSlot;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;

/**
 * A supplied item preview, optionally accepting cursor/NEI copies through a client callback.
 * Copies retain NBT and use count one. Inventory changes and server synchronization belong to the caller.
 */
public class ItemStackSlot extends Widget<ItemStackSlot>
    implements Interactable, RecipeViewerGhostIngredientSlot<ItemStack> {

    private final Supplier<ItemStack> getter;
    private Consumer<ItemStack> onDrop;
    private boolean itemTooltip;

    public ItemStackSlot(Supplier<ItemStack> getter, IDrawable background) {
        this.getter = getter;
        size(18);
        background(background);
    }

    /** Without a callback the slot remains display-only. Every mouse button follows the same copy/clear policy. */
    public ItemStackSlot onDrop(Consumer<ItemStack> callback) {
        onDrop = callback;
        return this;
    }

    public ItemStackSlot itemTooltip() {
        itemTooltip = true;
        tooltip().setAutoUpdate(true)
            .tooltipBuilder(tooltip -> {
                ItemStack stack = displayStack();
                if (stack != null && stack.getItem() != null) tooltip.addFromItem(stack);
            });
        return this;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        ItemStack stack = displayStack();
        if (stack != null && stack.getItem() != null) {
            new ItemDrawable(stack).draw(context, 1, 1, 16, 16, widgetTheme.getTheme());
        }
    }

    @Override
    public void drawForeground(ModularGuiContext context) {
        if (!itemTooltip) {
            super.drawForeground(context);
            return;
        }
        ItemStack stack = displayStack();
        RichTooltip tooltip = getTooltip();
        if (stack != null && stack.getItem() != null
            && tooltip != null
            && isHoveringFor(tooltip.getShowUpTimer())
            && !context.hasDraggable()) tooltip.draw(context, stack);
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        if (onDrop == null) return Result.IGNORE;
        EntityPlayer player = MCHelper.getPlayer();
        acceptCopy(player == null ? null : player.inventory.getItemStack());
        Interactable.playButtonClickSound();
        return Result.SUCCESS;
    }

    @Override
    public boolean handleDragAndDrop(@NotNull ItemStack draggedStack, int button) {
        if (onDrop == null || draggedStack.getItem() == null) return false;
        acceptCopy(draggedStack);
        return true;
    }

    private ItemStack displayStack() {
        return getter == null ? null : getter.get();
    }

    private void acceptCopy(ItemStack stack) {
        ItemStack copy = null;
        if (stack != null && stack.getItem() != null) {
            copy = stack.copy();
            copy.stackSize = 1;
        }
        onDrop.accept(copy);
    }
}
