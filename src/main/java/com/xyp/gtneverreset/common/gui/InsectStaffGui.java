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
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.item.InvWrapper;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtneverreset.common.item.items.InsectStaff;

public final class InsectStaffGui {

    private static final UITexture BACKGROUND = UITexture.builder()
        .location(new ResourceLocation("draconicevolution", "textures/gui/ToolConfig.png"))
        .imageSize(256, 256)
        .subAreaXYWH(0, 0, 198, 89)
        .adaptable(4)
        .build();
    private static final UITexture BUTTON = buttonTexture(0);
    private static final UITexture HOVER = buttonTexture(20);

    private InsectStaffGui() {}

    private static UITexture buttonTexture(int y) {
        return UITexture.builder()
            .location(new ResourceLocation("draconicevolution", "textures/gui/Widgets.png"))
            .imageSize(256, 256)
            .subAreaXYWH(18, y, 100, 20)
            .adaptable(1)
            .build();
    }

    public static ModularPanel build(PlayerInventoryGuiData data, PanelSyncManager sync, UISettings settings) {
        Session session = new Session(data);
        settings.canInteractWith(player -> session.valid());
        sync.bindPlayerInventory(
            data.getPlayer(),
            (inventory, index) -> new ModularSlot(inventory, index)
                .accessibility(index != data.getSlotIndex(), index != data.getSlotIndex()));
        var panel = ModularPanel.defaultPanel("insect_staff", 350, 201)
            .background(BACKGROUND)
            .disableHoverBackground();
        panel.child(button(325, 5, 18, IKey.str("x")).onMousePressed(mouse -> {
            panel.closeIfOpen();
            return true;
        }));
        panel.child(
            IKey.lang("item.gtneverreset.insect_staff.name")
                .color(0xff303030)
                .shadow(false)
                .asWidget()
                .pos(7, 7));
        List<ItemConfigField> fields = session.staff.getFields(session.stack, data.getSlotIndex());
        for (int i = 0; i < fields.size(); i++) addField(panel, sync, session, fields.get(i), 21 + i * 13);
        var filters = sync.syncedPanel("obliteration", true, (manager, handler) -> filterPanel(session, manager));
        panel.child(
            button(7, 181, 161, IKey.lang("info.de.toolInventoryOblit.txt")).name("obliteration_filter")
                .onMousePressed(mouse -> {
                    if (mouse != 0) return false;
                    filters.openPanel();
                    return true;
                }));
        return panel;
    }

    private static void addField(ModularPanel panel, PanelSyncManager sync, Session session, ItemConfigField field,
        int y) {
        DoubleSyncValue value = new DoubleSyncValue(
            () -> session.field(field.name)
                .castToDouble(),
            v -> session.set(field.name, v)) {

            @Override
            public String getStringValue() {
                return field.datatype == References.INT_ID ? Integer.toString((int) getDoubleValue())
                    : super.getStringValue();
            }
        }.allowC2S();
        sync.syncValue(field.name, value);
        panel.child(
            IKey.lang("button.de." + field.name + ".name")
                .color(0xff303030)
                .shadow(false)
                .scale(0.85F)
                .asWidget()
                .pos(7, y + 2)
                .size(130, 10));
        if (field.datatype == References.BOOLEAN_ID) {
            panel.child(
                button(
                    140,
                    y,
                    203,
                    IKey.dynamic(
                        () -> StatCollector
                            .translateToLocal(value.getDoubleValue() == 0 ? "gui.de.off.txt" : "gui.de.on.txt")))
                                .name(field.name)
                                .onMousePressed(mouse -> {
                                    if (mouse != 0) return false;
                                    value.setDoubleValue(value.getDoubleValue() == 0 ? 1 : 0);
                                    return true;
                                }));
            return;
        }
        int[] increments = { -100, -10, -1, 1, 10, 100 };
        String[] labels = { "---", "--", "-", "+", "++", "+++" };
        for (int i = 0; i < increments.length; i++) {
            int amount = increments[i];
            int x = i < 3 ? 140 + i * 21 : 282 + (i - 3) * 21;
            panel.child(
                button(x, y, 19, IKey.str(labels[i])).name(field.name + "_" + amount)
                    .onMousePressed(mouse -> {
                        if (mouse != 0) return false;
                        double next = value.getDoubleValue() + ((Number) field.incroment).doubleValue() * amount;
                        value
                            .setDoubleValue(Math.max(field.castMinToDouble(), Math.min(field.castMaxToDouble(), next)));
                        return true;
                    }));
        }
        if (field.name.equals(InsectStaffConfig.AMOUNT) || field.name.equals(InsectStaffConfig.RANGE)) {
            panel.child(
                new TextFieldWidget().pos(204, y)
                    .size(76, 12)
                    .numbersLong((long) field.castMinToDouble(), (long) field.castMaxToDouble())
                    .syncHandler(field.name)
                    .name(field.name + "_input"));
            return;
        }
        panel.child(IKey.dynamic(() -> {
            ItemConfigField display = new ItemConfigField(field.datatype, 0, field.name).setModifier(field.modifier);
            if (field.datatype == References.FLOAT_ID) display.value = (float) value.getDoubleValue();
            else display.value = (int) value.getDoubleValue();
            return display.getFormattedValue();
        })
            .color(0xff303030)
            .shadow(false)
            .asWidget()
            .pos(204, y + 2)
            .size(76, 10));
    }

    private static ButtonWidget<?> button(int x, int y, int width, IKey label) {
        return new ButtonWidget<>().pos(x, y)
            .size(width, 12)
            .background(BUTTON)
            .hoverBackground(HOVER)
            .overlay(
                label.color(0xffffffff)
                    .shadow(false));
    }

    private static ModularPanel filterPanel(Session session, PanelSyncManager sync) {
        ModularPanel panel = ModularPanel.defaultPanel("insect_staff_filter", 200, 135)
            .background(BACKGROUND);
        panel.child(
            IKey.lang("info.de.toolInventoryOblit.txt")
                .color(0xff303030)
                .shadow(false)
                .scale(0.8F)
                .asWidget()
                .pos(7, 7));
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
                    .pos(7 + i * 18, 21));
        }
        for (int i = 0; i < 5; i++) {
            panel.child(
                new ItemSlot().slot(
                    new ModularSlot(wrapper, 9 + i).filter(session::validEnchantment)
                        .changeListener(
                            (stack, amountOnly, client, init) -> { if (!client && !init) inventory.markDirty(); })
                        .slotGroup("tool_inventory"))
                    .name("enchantment_" + i)
                    .pos(176, 21 + i * 21));
        }
        panel.child(
            SlotGroupWidget.playerInventory(true)
                .pos(7, 47));
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
