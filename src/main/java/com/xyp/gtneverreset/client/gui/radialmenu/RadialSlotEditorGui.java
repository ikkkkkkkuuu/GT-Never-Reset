package com.xyp.gtneverreset.client.gui.radialmenu;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;

import com.xyp.gtneverreset.client.radialmenu.ActionField;
import com.xyp.gtneverreset.client.radialmenu.ActionType;
import com.xyp.gtneverreset.client.radialmenu.ActionTypes;
import com.xyp.gtneverreset.client.radialmenu.IconSpec;
import com.xyp.gtneverreset.client.radialmenu.MenuNode;
import com.xyp.gtneverreset.client.radialmenu.action.ToolBeltMenu;
import com.xyp.gtneverreset.client.radialmenu.profile.ProfileManager;
import com.xyp.gtneverreset.client.radialmenu.render.IconRenderer;
import com.xyp.gtnrlib.gui.widget.RadialUi;
import com.xyp.gtnrlib.gui.widget.RadialUiCheckbox;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RadialSlotEditorGui extends RadialActionEditorGui implements RadialIconPickerGui.Callback {

    private static final int ID_ICON = 1;
    private static final int ID_KEEP_OPEN = 2;
    private static final int ID_ENTRIES = 3;

    private final MenuNode parent;
    private final boolean readOnly;
    private final boolean toolBelt;

    /** Position in the parent's child list, not the sector it is drawn in. */
    private final int childIndex;

    /** Working copy; the parent is only touched on save. */
    private final MenuNode draft;

    private GuiTextField titleField;

    /** Survives control rebuilds, which recreate the field. */
    private String titleText;

    private int actionSectionTop;

    private int iconPreviewLeft;
    private int iconPreviewTop;

    public RadialSlotEditorGui(MenuNode parent, int childIndex) {
        this.parent = parent;
        this.childIndex = childIndex;

        MenuNode existing = parent.childAtIndex(childIndex);
        this.toolBelt = ToolBeltMenu.isMenu(existing);
        this.readOnly = parent.isBuiltIn() || existing != null && existing.isBuiltIn() && !toolBelt;
        this.draft = existing != null ? existing.copy()
            : MenuNode.leaf(
                // #tr radialmenu.editor.newEntry
                // # New entry
                // # zh_CN 新建条目
                I18n.format("radialmenu.editor.newEntry"),
                IconSpec.item("minecraft:stone", 0),
                ActionTypes.get(ActionTypes.KEYBIND)
                    .newSpec());

        this.titleText = draft.title == null ? "" : draft.title;

        if (draft.isCategory()) {
            // Submenu settings live on the node, so mirror them into a spec the generic field code can drive.
            setSpec(RadialSubmenuFields.toSpec(draft), ActionTypes.SUBMENU);
        } else if (draft.action != null && ActionTypes.isRegistered(draft.action.type)) {
            setSpec(draft.action, draft.action.type);
        } else {
            // A hand-edited file can name a type no build of the mod has. Showing an empty screen would be worse
            // than showing the default one, and the original is only overwritten if the player saves.
            setSpec(
                ActionTypes.get(ActionTypes.KEYBIND)
                    .newSpec(),
                ActionTypes.KEYBIND);
        }
    }

    @Override
    protected String titleKey() {
        // #tr radialmenu.editor.slot
        // # Slot %s
        // # zh_CN 槽位 %s
        return "radialmenu.editor.slot";
    }

    @Override
    protected Object[] titleArgs() {
        // Numbered by the sector, which is what the player clicked; on a dynamic wheel that is not the list position.
        return new Object[] { Integer.valueOf(parent.slotForChildIndex(childIndex) + 1) };
    }

    @Override
    protected int panelWidth() {
        return 330;
    }

    @Override
    protected boolean framedViewport() {
        return true;
    }

    @Override
    protected List<ActionType> offeredTypes() {
        return toolBelt ? Collections.singletonList(ActionTypes.get(ActionTypes.SUBMENU)) : ActionTypes.all();
    }

    @Override
    protected List<ActionField> offeredFields(ActionType type) {
        return toolBelt ? RadialSubmenuFields.fields(draft) : super.offeredFields(type);
    }

    @Override
    protected String fieldTooltip(ActionField field) {
        return toolBelt && ActionTypes.PARAM_SLOT_COUNT.equals(field.key) ? RadialEditorRestrictions.resizeTip()
            : super.fieldTooltip(field);
    }

    @Override
    protected void buildControls() {
        if (readOnly) {
            setContentHeight(RadialUi.STEP);
            addBottomBar("gui.done", null, null);
            return;
        }
        int left = contentLeft();
        int controlLeft = left + RadialUi.LABEL_WIDTH + RadialUi.GAP;
        int controlWidth = contentRight() - controlLeft;
        int y = scrolledTop() + 14;

        // Built at its real size: GuiTextField works out its horizontal scroll from the width it has when the
        // text is set, so a placeholder-sized field resized afterwards renders blank until it is clicked.
        titleField = new GuiTextField(this.fontRendererObj, controlLeft + 1, y + 3, controlWidth - 2, 14);
        titleField.setMaxStringLength(64);
        titleField.setText(titleText);
        titleField.setCursorPositionZero();
        y += RadialUi.STEP;

        iconPreviewLeft = controlLeft;
        iconPreviewTop = y;
        this.buttonList.add(
            new GuiButton(
                ID_ICON,
                controlLeft + RadialUi.ROW + RadialUi.GAP,
                y,
                controlWidth - RadialUi.ROW - RadialUi.GAP,
                RadialUi.ROW,
                iconLabel()));
        // #tr radialmenu.editor.icon.tip
        // # Choose an item icon, a built-in sprite or a custom PNG
        // # zh_CN 选择物品图标、内置精灵图或自定义 PNG
        tooltip(ID_ICON, describe("radialmenu.editor.icon.tip"));
        y += RadialUi.STEP;

        this.buttonList.add(
            new RadialUiCheckbox(
                ID_KEEP_OPEN,
                controlLeft,
                y,
                controlWidth,
                // #tr radialmenu.editor.keepOpen
                // # Keep menu open
                // # zh_CN 保持菜单打开
                I18n.format("radialmenu.editor.keepOpen"),
                draft.keepOpen));
        // #tr radialmenu.editor.keepOpen.tip
        // # Keep the wheel open after running this action
        // # zh_CN 执行此动作后保持轮盘打开
        tooltip(ID_KEEP_OPEN, describe("radialmenu.editor.keepOpen.tip"));
        y += RadialUi.STEP + RadialUi.GAP;

        actionSectionTop = y;
        y += 14;

        y = buildActionSection(y, left, controlLeft, controlWidth);

        if (!toolBelt && draft.isCategory() && ActionTypes.SUBMENU.equals(selectedType)) {
            y += RadialUi.GAP;
            this.buttonList.add(
                new GuiButton(
                    ID_ENTRIES,
                    controlLeft,
                    y,
                    controlWidth,
                    RadialUi.ROW,
                    // #tr radialmenu.editor.entries
                    // # Entries (%d)...
                    // # zh_CN 条目 (%d)...
                    I18n.format("radialmenu.editor.entries", Integer.valueOf(draft.filledCount()))));
            // #tr radialmenu.editor.entries.tip
            // # Save this slot, then arrange, copy, remove or move its submenu entries
            // # zh_CN 先保存此槽位，再调整、复制、删除或移动子菜单条目
            tooltip(ID_ENTRIES, describe("radialmenu.editor.entries.tip"));
            y += RadialUi.STEP;
        }

        setContentHeight(y - scrolledTop() + RadialUi.PAD);
        // #tr radialmenu.editor.delete
        // # Delete
        // # zh_CN 删除
        addBottomBar("radialmenu.editor.save", toolBelt ? null : "radialmenu.editor.delete", "gui.cancel");
        if (toolBelt) {
            tooltip(ID_PRIMARY, RadialEditorRestrictions.resizeTip());
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (readOnly) {
            onCancel();
            return;
        }
        switch (button.id) {
            case ID_PRIMARY:
                save();
                return;
            case ID_SECONDARY:
                onCancel();
                return;
            case ID_DANGER:
                if (!toolBelt) apply(null);
                return;
            case ID_ICON:
                captureInputs();
                RadialGuiStack.push(new RadialIconPickerGui(this, draft.icon == null ? null : draft.icon.color));
                return;
            case ID_ENTRIES:
                openEntries();
                return;
            case ID_KEEP_OPEN:
                ((RadialUiCheckbox) button).toggle();
                draft.keepOpen = ((RadialUiCheckbox) button).checked;
                return;
            default:
                handleActionButton(button);
        }
    }

    /** Captures the title as well, so opening a picker or scrolling cannot discard it. */
    @Override
    protected void captureInputs() {
        if (titleField != null) {
            titleText = titleField.getText();
        }
        draft.title = titleText.trim();
        super.captureInputs();
    }

    private void save() {
        if (!dropsEntries()) {
            write();
            RadialGuiStack.closeAll();
            return;
        }
        captureInputs();
        RadialGuiStack.push(
            new RadialConfirmGui(
                // #tr radialmenu.editor.confirmDiscard
                // # Discard this submenu?
                // # zh_CN 丢弃此子菜单？
                "radialmenu.editor.confirmDiscard",
                // #tr radialmenu.editor.confirmDiscard.subject
                // # %d entries inside it will be lost
                // # zh_CN 其中的 %d 个条目将会丢失
                I18n.format("radialmenu.editor.confirmDiscard.subject", Integer.valueOf(draft.filledCount())),
                // #tr radialmenu.editor.discard
                // # Discard
                // # zh_CN 丢弃
                "radialmenu.editor.discard",
                new RadialConfirmGui.Result() {

                    @Override
                    public void onConfirmed() {
                        write();
                        RadialGuiStack.closeAll();
                    }
                }));
    }

    /** Whether saving would turn a submenu that still holds entries into something that cannot hold any. */
    private boolean dropsEntries() {
        return draft.isCategory() && draft.filledCount() > 0 && !ActionTypes.SUBMENU.equals(selectedType);
    }

    private MenuNode write() {
        captureInputs();

        if (toolBelt) selectedType = ActionTypes.SUBMENU;

        draft.action = spec;
        draft.action.type = selectedType;

        if (ActionTypes.SUBMENU.equals(selectedType)) {
            RadialSubmenuFields.applyToNode(draft);
        } else {
            draft.children = null;
            draft.layout = null;
            draft.style = null;
        }

        draft.normalize();
        store(draft);
        return draft;
    }

    private void openEntries() {
        if (!ActionTypes.SUBMENU.equals(selectedType)) {
            return;
        }
        MenuNode node = write();
        if (node == null || !node.isCategory()) {
            return;
        }
        RadialGuiStack.push(new RadialMenuSettingsGui(node));
    }

    private void apply(MenuNode node) {
        store(node);
        // Editing is finished, so the wheel closes with the editor instead of reappearing beneath it.
        RadialGuiStack.closeAll();
    }

    /** Puts a node in this slot and saves the profile. Null is how an entry is deleted. */
    private void store(MenuNode node) {
        if (readOnly || toolBelt && node == null) {
            return;
        }
        int requestedSlots = toolBelt ? node.layoutOrDefault().slots : 0;
        parent.setChildAt(childIndex, node);
        if (toolBelt) ToolBeltMenu.requestResize(node, requestedSlots);
        ProfileManager.active()
            .normalize();
        ProfileManager.saveActive();
        IconRenderer.clearCache();
    }

    @Override
    public void onIconPicked(IconSpec icon) {
        draft.icon = icon;
        requestRebuild();
    }

    @Override
    protected void onCancel() {
        RadialGuiStack.closeAll();
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        if (readOnly) {
            this.fontRendererObj
                .drawString(RadialEditorRestrictions.builtInTip(), contentLeft(), contentTop(), RadialUi.TEXT);
            return;
        }
        int left = contentLeft();
        int y = scrolledTop();

        // #tr radialmenu.editor.sectionLook
        // # Appearance
        // # zh_CN 外观
        RadialUi.sectionHeader(I18n.format("radialmenu.editor.sectionLook"), left, y, contentRight());
        y += 14;

        if (isVisibleRow(y)) {
            // #tr radialmenu.editor.title
            // # Title
            // # zh_CN 标题
            RadialUi.rowLabel(I18n.format("radialmenu.editor.title"), left, y);
            titleField.drawTextBox();
        }
        y += RadialUi.STEP;

        if (isVisibleRow(y)) {
            // #tr radialmenu.editor.icon
            // # Icon
            // # zh_CN 图标
            RadialUi.rowLabel(I18n.format("radialmenu.editor.icon"), left, y);
        }

        if (isVisibleRow(actionSectionTop)) {
            // #tr radialmenu.editor.sectionAction
            // # Action
            // # zh_CN 操作
            RadialUi
                .sectionHeader(I18n.format("radialmenu.editor.sectionAction"), left, actionSectionTop, contentRight());
        }

        drawActionSection();
        drawIconPreview();
    }

    private String iconLabel() {
        return draft.icon == null || draft.icon.id == null ? I18n.format("radialmenu.editor.pickIcon")
            : RadialUi.fit(draft.icon.id, contentWidth() - RadialUi.LABEL_WIDTH - RadialUi.ROW - 16);
    }

    /** The preview tile, drawn with the content so it never has to fight a button for the GL state. */
    private void drawIconPreview() {
        if (!isVisibleRow(iconPreviewTop)) {
            return;
        }
        RadialUi.frame(
            iconPreviewLeft,
            iconPreviewTop,
            iconPreviewLeft + RadialUi.ROW,
            iconPreviewTop + RadialUi.ROW,
            0xFF101010,
            RadialUi.PANEL_BORDER);
        if (draft.icon != null) {
            IconRenderer.draw(draft.icon, iconPreviewLeft + 2, iconPreviewTop + 2);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        // A field clipped at the panel edge still answers to clicks on the part that was cut away.
        if (readOnly || !isShowing() || !isInsideViewport(mouseY)) {
            return;
        }
        titleField.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean handleKey(char typedChar, int keyCode) {
        if (readOnly) {
            return false;
        }
        if (keyCode == Keyboard.KEY_TAB) {
            return true;
        }
        if (titleField.textboxKeyTyped(typedChar, keyCode)) {
            return true;
        }
        return super.handleKey(typedChar, keyCode);
    }
}
