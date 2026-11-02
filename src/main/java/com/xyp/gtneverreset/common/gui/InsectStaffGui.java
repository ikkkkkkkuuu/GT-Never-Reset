package com.xyp.gtneverreset.common.gui;

import java.util.List;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.utils.DataUtils;
import com.brandon3055.draconicevolution.common.utils.IConfigurableItem.ProfileHelper;
import com.brandon3055.draconicevolution.common.utils.ItemConfigField;
import com.brandon3055.draconicevolution.common.utils.ItemNBTHelper;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.item.InvWrapper;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtneverreset.common.item.items.InsectStaff;

public final class InsectStaffGui {

    private static final int INPUT_PAGE_WIDTH = 232;
    private static final ResourceLocation TOOL_TEXTURE = new ResourceLocation(
        "draconicevolution",
        "textures/gui/ToolConfig.png");
    private static final ResourceLocation FIELD_TEXTURE = new ResourceLocation(
        "draconicevolution",
        "textures/gui/Widgets.png");
    private static final ResourceLocation VANILLA_BUTTON_TEXTURE = new ResourceLocation("textures/gui/widgets.png");
    private static final UITexture BACKGROUND = UITexture.builder()
        .location(TOOL_TEXTURE)
        .imageSize(256, 256)
        .subAreaXYWH(0, 0, 198, 89)
        .adaptable(4)
        .build();
    private static final UITexture SLOT = texture(
        new ResourceLocation("brandonscore", "textures/gui/Widgets.png"),
        119,
        1,
        18,
        18);
    private static final UITexture ITEM_FRAME = texture(
        new ResourceLocation("brandonscore", "textures/gui/Widgets.png"),
        118,
        0,
        20,
        20);
    private static final UITexture ENCHANTMENT = UITexture.builder()
        .location(TOOL_TEXTURE)
        .imageSize(256, 256)
        .subAreaXYWH(0, 89, 16, 14)
        .nonOpaque()
        .build();

    private InsectStaffGui() {}

    public static ModularPanel build(PlayerInventoryGuiData data, PanelSyncManager sync, UISettings settings) {
        Session session = new Session(data);
        settings.canInteractWith(player -> session.valid());
        settings.getRecipeViewerSettings()
            .disable();
        sync.bindPlayerInventory(
            data.getPlayer(),
            (inventory, index) -> new ModularSlot(inventory, index)
                .accessibility(index != data.getSlotIndex(), index != data.getSlotIndex()));
        List<ItemConfigField> fields = session.staff.getFields(session.stack, data.getSlotIndex());
        int listHeight = Math.max(89, 10 + fields.size() * 12);
        var panel = ModularPanel.defaultPanel("insect_staff", INPUT_PAGE_WIDTH, listHeight)
            .background(IDrawable.EMPTY)
            .disableHoverBackground();
        var pages = new PagedWidget<>().size(INPUT_PAGE_WIDTH, listHeight)
            .name("configuration_pages");
        var list = new ParentWidget<>().size(198, listHeight)
            .align(Alignment.Center)
            .background(BACKGROUND)
            .name("configuration_list");
        pages.addPage(list);
        panel.child(pages);
        list.child(
            ITEM_FRAME.asWidget()
                .pos(3, 5)
                .size(20));
        list.child(
            new ItemDisplayWidget().item(session.stack)
                .pos(4, 6)
                .size(18)
                .background(IDrawable.EMPTY)
                .disableHoverBackground());
        list.child(
            button(3, 26, 20, 12, IKey.str("<="), true).name("close_configuration")
                .addTooltipLine(IKey.lang("gui.back"))
                .onMousePressed(mouse -> {
                    if (mouse != 0) return false;
                    panel.closeIfOpen();
                    return true;
                }));
        var filters = sync
            .syncedPanel("obliteration", true, (manager, handler) -> filterPanel(session, manager).onCloseAction(() -> {
                pages.setEnabled(true);
                settings.getRecipeViewerSettings()
                    .disable();
            }));
        list.child(
            button(3, 39, 20, 12, IKey.str("Inv"), true).name("obliteration_filter")
                .addTooltipLine(IKey.lang("gui.de.itemInventory.txt"))
                .onMousePressed(mouse -> {
                    if (mouse != 0) return false;
                    pages.setEnabled(false);
                    settings.getRecipeViewerSettings()
                        .enable();
                    filters.openPanel();
                    return true;
                }));
        for (int i = 0; i < fields.size(); i++) addField(list, pages, sync, session, fields.get(i), 5 + i * 12);
        return panel;
    }

    private static void addField(ParentWidget<?> list, PagedWidget<?> pages, PanelSyncManager sync, Session session,
        ItemConfigField field, int y) {
        DoubleSyncValue value = new DoubleSyncValue(
            () -> session.field(field.name)
                .castToDouble(),
            v -> session.set(field.name, v)) {

            @Override
            public String getStringValue() {
                if (field.datatype == References.FLOAT_ID) return Float.toString(getFloatValue());
                return field.datatype == References.INT_ID ? Integer.toString((int) getDoubleValue())
                    : super.getStringValue();
            }
        }.allowC2S();
        sync.syncValue(field.name, value);
        int editorPage = pages.getPages()
            .size();
        var entry = new ButtonWidget<>().pos(24, y)
            .size(150, 12)
            .name(field.name)
            .background(fieldButtonTexture(false))
            .hoverBackground(fieldButtonTexture(true))
            .child(
                IKey.lang("button.de." + field.name + ".name")
                    .asWidget()
                    .color(0xffffffff)
                    .shadow(true)
                    .alignment(Alignment.TopLeft)
                    .pos(2, 2)
                    .size(146, 8))
            .tooltipDynamic(tooltip -> tooltip.addLine(IKey.str(formatted(field, value))))
            .onMousePressed(mouse -> {
                if (mouse != 0) return false;
                if (field.datatype == References.BOOLEAN_ID) value.setDoubleValue(value.getDoubleValue() == 0 ? 1 : 0);
                else pages.setPage(editorPage);
                return true;
            });
        entry.tooltip()
            .setAutoUpdate(true);
        list.child(entry);
        if (field.datatype == References.BOOLEAN_ID) return;

        boolean speedMultiplier = "MULTIPLIER".equals(field.modifier);
        boolean directInput = speedMultiplier || field.name.equals(InsectStaffConfig.RANGE)
            || field.name.equals(InsectStaffConfig.AMOUNT);
        int editorWidth = directInput ? INPUT_PAGE_WIDTH : 198;
        var editor = new ParentWidget<>().size(editorWidth, 89)
            .align(Alignment.Center)
            .background(BACKGROUND)
            .name(field.name + "_editor");
        pages.addPage(editor);
        editor.child(
            button(3, 3, 20, 12, IKey.str("<="), true).name(field.name + "_back")
                .addTooltipLine(IKey.lang("gui.back"))
                .onMousePressed(mouse -> {
                    if (mouse != 0) return false;
                    pages.setPage(0);
                    return true;
                }));
        editor.child(
            IKey.lang("button.de." + field.name + ".name")
                .asWidget()
                .color(0xff000000)
                .shadow(false)
                .alignment(Alignment.TopCenter)
                .pos(4, 22)
                .size(editorWidth - 9, 8));
        if (directInput) {
            // #tr gui.gtneverreset.insect_staff.enter_value
            // # Click to enter a number; press Enter to apply
            // # zh_CN 点击数字输入，按回车确认
            TextFieldWidget input = new TextFieldWidget().pos(84, 35)
                .size(64, 18)
                .padding(4, 0)
                .background(GuiTextures.DISPLAY_SMALL)
                .disableHoverBackground()
                .setTextColor(0xffffffff)
                .setTextAlignment(Alignment.Center)
                .syncHandler(field.name)
                .name(field.name + "_input")
                .addTooltipLine(IKey.lang("gui.gtneverreset.insect_staff.enter_value"));
            if (speedMultiplier) {
                input.numbersDouble(field::castMinToDouble, field::castMaxToDouble);
                // #tr gui.gtneverreset.insect_staff.speed_multiplier
                // # Multiplies your current speed while holding the staff; 1 disables the boost
                // # zh_CN 设为 1 时保持原速
                input.addTooltipLine(IKey.lang("gui.gtneverreset.insect_staff.speed_multiplier"));
            } else {
                input.numbersLong((long) field.castMinToDouble(), (long) field.castMaxToDouble())
                    .formatAsInteger(false);
            }
            editor.child(input);
        } else {
            editor.child(
                IKey.dynamic(() -> formatted(field, value))
                    .asWidget()
                    .color(0xffffffff)
                    .shadow(true)
                    .alignment(Alignment.TopCenter)
                    .pos(80, 40)
                    .size(37, 8)
                    .name(field.name + "_value"));
        }
        if (field.modifier == null || field.modifier.equals("AOE") || speedMultiplier) {
            IKey maximum = IKey.dynamic(
                () -> StatCollector.translateToLocal("gui.de.max.txt") + " "
                    + field.getMaxFormattedValue()
                    + (speedMultiplier ? "×" : ""));
            if (directInput) {
                // #tr gui.gtneverreset.insect_staff.set_maximum
                // # Click to set the maximum
                // # zh_CN 点击设为最大值
                editor.child(
                    new ButtonWidget<>().pos(4, 58)
                        .size(editorWidth - 8, 12)
                        .name(field.name + "_maximum")
                        .background(IDrawable.EMPTY)
                        .disableHoverBackground()
                        .child(
                            maximum.asWidget()
                                .color(0xffffffff)
                                .shadow(true)
                                .alignment(Alignment.TopCenter)
                                .pos(0, 2)
                                .size(editorWidth - 9, 8))
                        .addTooltipLine(IKey.lang("gui.gtneverreset.insect_staff.set_maximum"))
                        .onMousePressed(mouse -> {
                            if (mouse != 0) return false;
                            value.setDoubleValue(field.castMaxToDouble());
                            return true;
                        }));
            } else {
                editor.child(
                    maximum.asWidget()
                        .color(0xffffffff)
                        .shadow(true)
                        .alignment(Alignment.TopCenter)
                        .pos(4, 54)
                        .size(189, 8));
            }
        }
        int[] increments = { -100, -10, -1, 1, 10, 100 };
        String[] labels = { "---", "--", "-", "+", "++", "+++" };
        for (int i = 0; i < increments.length; i++) {
            int amount = increments[i];
            int x = i < 3 ? 4 + i * 26 : editorWidth - 80 + (i - 3) * 26;
            editor.child(
                button(x, 37, 24, 14, IKey.str(labels[i]), false).name(field.name + "_" + amount)
                    .onMousePressed(mouse -> {
                        if (mouse != 0) return false;
                        double next = value.getDoubleValue() + ((Number) field.incroment).doubleValue() * amount;
                        if (field.datatype == References.FLOAT_ID) next = Math.round(next * 100) / 100D;
                        value
                            .setDoubleValue(Math.max(field.castMinToDouble(), Math.min(field.castMaxToDouble(), next)));
                        return true;
                    }));
        }
    }

    private static String formatted(ItemConfigField field, DoubleSyncValue value) {
        if ("MULTIPLIER".equals(field.modifier)) return Float.toString((float) value.getDoubleValue()) + "×";
        ItemConfigField display = new ItemConfigField(field.datatype, 0, field.name).setModifier(field.modifier);
        if (field.datatype == References.BOOLEAN_ID) display.value = value.getDoubleValue() != 0;
        else if (field.datatype == References.FLOAT_ID) display.value = (float) value.getDoubleValue();
        else display.value = (int) value.getDoubleValue();
        return display.getFormattedValue();
    }

    private static UITexture texture(ResourceLocation location, int x, int y, int width, int height) {
        return new UITexture(location, x / 256F, y / 256F, (x + width) / 256F, (y + height) / 256F, null);
    }

    private static IDrawable fieldButtonTexture(boolean hover) {
        int v = hover ? 20 : 0;
        UITexture left = texture(FIELD_TEXTURE, 18, v, 100, 12);
        UITexture right = texture(FIELD_TEXTURE, 19, v, 99, 12);
        UITexture leftBottom = texture(FIELD_TEXTURE, 18, v + 19, 100, 1);
        UITexture rightBottom = texture(FIELD_TEXTURE, 19, v + 19, 99, 1);
        return (context, x, y, width, height, theme) -> {
            left.draw(context, x, y, 100, 12, theme);
            leftBottom.draw(context, x, y + 11, 100, 1, theme);
            right.draw(context, x + 50, y, 99, 12, theme);
            rightBottom.draw(context, x + 50, y + 11, 99, 1, theme);
        };
    }

    private static IDrawable buttonTexture(int width, int height, boolean hover) {
        int v = hover ? 86 : 66;
        int half = width / 2;
        UITexture left = texture(VANILLA_BUTTON_TEXTURE, 0, v, half, height);
        UITexture right = texture(VANILLA_BUTTON_TEXTURE, 200 - half, v, half, height);
        UITexture leftBottom = texture(VANILLA_BUTTON_TEXTURE, 0, v + 23 - height, half, height - 3);
        UITexture rightBottom = texture(VANILLA_BUTTON_TEXTURE, 200 - half, v + 23 - height, half, height - 3);
        return (context, x, y, w, h, theme) -> {
            left.draw(context, x, y, half, height, theme);
            right.draw(context, x + half, y, half, height, theme);
            leftBottom.draw(context, x, y + 3, half, height - 3, theme);
            rightBottom.draw(context, x + half, y + 3, half, height - 3, theme);
        };
    }

    private static ButtonWidget<?> button(int x, int y, int width, int height, IKey label, boolean shadow) {
        ButtonWidget<?> button = new ButtonWidget<>().pos(x, y)
            .size(width, height)
            .background(buttonTexture(width, height, false))
            .hoverBackground(buttonTexture(width, height, true));
        button.child(
            label.asWidget()
                .color(() -> shadow ? button.isHovering() ? 0xffffffA0 : 0xffe0e0e0 : 0xff000000)
                .shadow(shadow)
                .alignment(Alignment.TopCenter)
                .pos(0, (height - 8) / 2)
                .size(width - 1, 8));
        return button;
    }

    private static ModularPanel filterPanel(Session session, PanelSyncManager sync) {
        ModularPanel panel = ModularPanel.defaultPanel("insect_staff_filter", 198, 130)
            .background(BACKGROUND);
        panel.child(
            IKey.lang("info.de.toolInventoryOblit.txt")
                .asWidget()
                .color(0xff555555)
                .shadow(false)
                .alignment(Alignment.TopLeft)
                .size(164, 8)
                .pos(5, 5));
        panel.child(
            button(172, 3, 18, 12, IKey.str("<="), true).name("filter_back")
                .addTooltipLine(IKey.lang("gui.back"))
                .onMousePressed(mouse -> {
                    if (mouse != 0) return false;
                    panel.closeIfOpen();
                    return true;
                }));
        var inventory = new ToolInventory(session);
        var wrapper = new InvWrapper(inventory);
        sync.registerSlotGroup("tool_inventory", 14, false);
        for (int i = 0; i < 9; i++) {
            panel.child(
                new ItemSlot()
                    .slot(
                        new ModularSlot(wrapper, i)
                            .filter(stack -> stack != null && stack.getItem() instanceof ItemBlock)
                            .changeListener(
                                (stack, amountOnly, client, init) -> { if (!client && !init) inventory.markDirty(); })
                            .slotGroup("tool_inventory"))
                    .name("filter_" + i)
                    .background(SLOT)
                    .pos(7 + i * 18, 18));
        }
        for (int i = 0; i < 5; i++) {
            panel.child(
                new ItemSlot().slot(
                    new ModularSlot(wrapper, 9 + i).filter(session::validEnchantment)
                        .changeListener(
                            (stack, amountOnly, client, init) -> { if (!client && !init) inventory.markDirty(); })
                        .slotGroup("tool_inventory"))
                    .name("enchantment_" + i)
                    .background(
                        SLOT,
                        ENCHANTMENT.asIcon()
                            .size(16, 14))
                    .pos(172, 18 + i * 21));
        }
        panel.child(
            SlotGroupWidget
                .playerInventory(
                    (index, slot) -> slot.background(index == session.data.getSlotIndex() ? IDrawable.EMPTY : SLOT))
                .pos(7, 44));
        return panel;
    }

    private static final class ToolInventory extends InventoryBasic {

        private final Session session;
        private boolean loading = true;

        private ToolInventory(Session session) {
            super("insect_staff", false, 14);
            this.session = session;
            var tags = ItemNBTHelper.getCompound(session.stack);
            for (int i = 0; i < 9; i++)
                setInventorySlotContents(i, ItemStack.loadItemStackFromNBT(tags.getCompoundTag("Item" + i)));
            var enchantments = tags.getTagList("ench", 10);
            for (int i = 0; i < Math.min(5, enchantments.tagCount()); i++) {
                ItemStack book = new ItemStack(Items.enchanted_book);
                var list = new NBTTagList();
                list.appendTag(
                    enchantments.getCompoundTagAt(i)
                        .copy());
                book.setTagCompound(new NBTTagCompound());
                book.getTagCompound()
                    .setTag("StoredEnchantments", list);
                setInventorySlotContents(9 + i, book);
            }
            loading = false;
        }

        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public void markDirty() {
            if (loading || !session.canWrite()) return;
            var tags = ItemNBTHelper.getCompound(session.stack);
            var enchantments = new NBTTagList();
            for (int i = 0; i < 14; i++) {
                ItemStack stored = getStackInSlot(i);
                if (i < 9) {
                    var tag = new NBTTagCompound();
                    if (stored != null && stored.stackSize > 0) stored.writeToNBT(tag);
                    tags.setTag("Item" + i, tag);
                } else if (stored != null && stored.stackSize > 0 && stored.hasTagCompound()) {
                    enchantments.appendTag(
                        stored.getTagCompound()
                            .getTagList("StoredEnchantments", 10)
                            .getCompoundTagAt(0)
                            .copy());
                }
            }
            if (enchantments.tagCount() == 0) tags.removeTag("ench");
            else tags.setTag("ench", enchantments);
            session.data.getPlayer().inventory.markDirty();
        }
    }

    private static final class Session {

        private final PlayerInventoryGuiData data;
        private final ItemStack stack;
        private final InsectStaff staff;

        private Session(PlayerInventoryGuiData data) {
            this.data = data;
            stack = data.getUsedItemStack();
            staff = (InsectStaff) stack.getItem();
        }

        private boolean validEnchantment(ItemStack book) {
            if (book == null || book.getItem() != Items.enchanted_book || !book.hasTagCompound()) return false;
            var list = book.getTagCompound()
                .getTagList("StoredEnchantments", 10);
            if (list.tagCount() != 1) return false;
            int id = list.getCompoundTagAt(0)
                .getInteger("id");
            if (id < 0 || id >= Enchantment.enchantmentsList.length) return false;
            var enchantment = Enchantment.enchantmentsList[id];
            return enchantment != null
                && (staff.isEnchantValid(enchantment) || enchantment.type == EnumEnchantmentType.all)
                && !EnchantmentHelper.getEnchantments(stack)
                    .containsKey(id);
        }

        private boolean valid() {
            return data.getUsedItemStack() == stack && !data.getPlayer().isDead;
        }

        private boolean canWrite() {
            return !data.getPlayer().worldObj.isRemote && valid();
        }

        private ItemConfigField field(String name) {
            return staff.getFields(stack, data.getSlotIndex())
                .stream()
                .filter(field -> field.name.equals(name))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
        }

        private void set(String name, double value) {
            if (!canWrite() || !Double.isFinite(value)) return;
            ItemConfigField field = field(name);
            double minimum = field.datatype == References.BOOLEAN_ID ? 0 : field.castMinToDouble();
            double maximum = field.datatype == References.BOOLEAN_ID ? 1 : field.castMaxToDouble();
            if (value < minimum || value > maximum) return;
            Object stored;
            if (field.datatype == References.BOOLEAN_ID) stored = value != 0;
            else if (field.datatype == References.FLOAT_ID) stored = (float) value;
            else stored = (int) value;
            DataUtils.writeObjectToCompound(ProfileHelper.getProfileCompound(stack), stored, field.datatype, name);
            data.getPlayer().inventory.markDirty();
        }
    }
}
