package com.xyp.gtnrlib.gui.widget;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerGhostIngredientSlot;
import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerIngredientProvider;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.RecipeViewerSettingsImpl;
import com.cleanroommc.modularui.theme.SlotTheme;
import com.cleanroommc.modularui.widget.Widget;
import com.gtnewhorizon.gtnhlib.util.data.ItemId;

/**
 * An item slot for client-only screens: drag an item from NEI onto it and it shows and keeps that item. With
 * {@link #clearable(boolean)} turned on, a right-click empties it. It keeps the item and its meta without NBT, which is
 * what an icon or a filter needs, and never touches the player's inventory. Under the pointer it lights up like MUI2's
 * item slots.
 */
public class GhostItemSlot extends Widget<GhostItemSlot>
    implements RecipeViewerGhostIngredientSlot<ItemStack>, RecipeViewerIngredientProvider, Interactable {

    public static final int SIZE = 18;

    private ItemId item, drawnItem;
    private Predicate<ItemStack> filter = stack -> true;
    private Consumer<ItemId> onChange = item -> {};
    private Runnable onClear = () -> {};
    private boolean clearable;
    private IDrawable drawn = IDrawable.EMPTY;

    public GhostItemSlot() {
        size(SIZE, SIZE);
        widgetTheme(IThemeApi.ITEM_SLOT);
        overlay((context, x, y, width, height, theme) -> {
            shown().draw(context, x + 1, y + 1, width - 2, height - 2, theme);
            if (isHovering() && theme instanceof SlotTheme slot)
                GuiDraw.drawRect(x + 1, y + 1, width - 2, height - 2, slot.getSlotHoverColor());
        });
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        if (mouseButton != 1 || !clearable) return Result.IGNORE;
        if (item != null) {
            item = null;
            onClear.run();
            Interactable.playButtonClickSound();
        }
        return Result.SUCCESS;
    }

    /**
     * Sets whether a right-click empties the slot; it does not by default. Emptying runs {@link #onClear(Runnable)},
     * not {@link #onChange(Consumer)}, so a screen that keeps the chosen item through {@code onChange} also needs an
     * {@code onClear}, or reads {@link #item()} when it saves.
     *
     * @param clearable whether a right-click empties the slot
     * @return this slot
     */
    public GhostItemSlot clearable(boolean clearable) {
        this.clearable = clearable;
        return this;
    }

    /**
     * Sets what runs after a right-click emptied the slot.
     *
     * @param onClear action run once per emptying
     * @return this slot
     */
    public GhostItemSlot onClear(Runnable onClear) {
        this.onClear = onClear;
        return this;
    }

    /**
     * Opens a client-only screen with NEI shown, so items can be dragged from it. ModularUI hides NEI on client-only
     * screens and only draws it at all over a container screen, so this turns NEI on and gives the screen an empty
     * client-side container.
     */
    public static void open(ModularScreen screen) {
        RecipeViewerSettingsImpl recipeViewer = new RecipeViewerSettingsImpl();
        recipeViewer.enable();
        ClientGUI.open(screen, recipeViewer, ModularContainer::new);
    }

    public GhostItemSlot item(ItemId item) {
        this.item = item;
        return this;
    }

    /**
     * The item in the slot, or null when it is empty.
     */
    public ItemId item() {
        return item;
    }

    public GhostItemSlot clear() {
        return item(null);
    }

    /**
     * Only items passing the filter are taken from NEI; a refused drop leaves the slot and NEI's drag unchanged.
     */
    public GhostItemSlot filter(Predicate<ItemStack> filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Called after an item dragged from NEI was taken, with the new item. Emptying the slot calls
     * {@link #onClear(Runnable)} instead.
     */
    public GhostItemSlot onChange(Consumer<ItemId> onChange) {
        this.onChange = onChange;
        return this;
    }

    @Override
    public boolean handleDragAndDrop(ItemStack stack, int button) {
        if (stack == null || stack.getItem() == null || !filter.test(stack)) return false;
        item = ItemId.createWithoutNBT(stack);
        onChange.accept(item);
        return true;
    }

    @Override
    public ItemStack getStackForRecipeViewer() {
        return item == null ? null : item.toStack();
    }

    private IDrawable shown() {
        if (!Objects.equals(item, drawnItem)) {
            drawnItem = item;
            drawn = item == null ? IDrawable.EMPTY : new ItemDrawable(item.toStack());
        }
        return drawn;
    }
}
