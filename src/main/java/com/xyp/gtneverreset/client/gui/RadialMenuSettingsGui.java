package com.xyp.gtneverreset.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;

import com.xyp.gtneverreset.client.action.radialmenu.ToolBeltMenu;
import com.xyp.gtneverreset.client.profile.radialmenu.ProfileManager;
import com.xyp.gtneverreset.client.render.radialmenu.IconRenderer;
import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtneverreset.utils.gui.radialmenu.ActionField;
import com.xyp.gtneverreset.utils.gui.radialmenu.ActionSpec;
import com.xyp.gtneverreset.utils.gui.radialmenu.ActionTypes;
import com.xyp.gtneverreset.utils.gui.radialmenu.IconSpec;
import com.xyp.gtneverreset.utils.gui.radialmenu.MenuNode;
import com.xyp.gtneverreset.utils.gui.radialmenu.SlotLayout;
import com.xyp.gtneverreset.utils.gui.radialmenu.StyleResolver;

import cpw.mods.fml.client.config.GuiSlider;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RadialMenuSettingsGui extends RadialUiScreen implements RadialIconPickerGui.Callback {

    private static final int ID_ICON = 1;
    private static final int ID_FIELD_BASE = 40;
    private static final int ID_CLEAR_BASE = 100;
    private static final int ID_UP_BASE = 200;
    private static final int ID_DOWN_BASE = 300;
    private static final int ID_COPY_BASE = 400;
    private static final int ID_DELETE_BASE = 500;
    private static final int ID_MOVE_BASE = 600;

    private static final int ARROW = 18;

    private static final int GROUP_GAP = 6;

    private final MenuNode menu;
    private final boolean toolBelt;
    private final boolean readOnly;

    private final ActionSpec draft;

    private GuiTextField titleField;

    /** Survives control rebuilds, which recreate the field. */
    private String titleText;
    private IconSpec icon;
    private int iconRowTop;

    private final List<ActionField> fields = new ArrayList<>();

    private final List<Integer> fieldRowTops = new ArrayList<>();
    private final List<GuiTextField> inputs = new ArrayList<>();

    /** Parallel to the fields, like the text boxes: a slider holds its value in the widget rather than in the spec. */
    private final List<GuiSlider> sliders = new ArrayList<>();

    /** The menu's entries, reordered here and written back only on save. */
    private final List<MenuNode> workingChildren;

    /** Where each drawn row sits, so the icon and title can be drawn with the content rather than over a button. */
    private final List<int[]> rowPositions = new ArrayList<>();
    private final List<MenuNode> rowNodes = new ArrayList<>();

    private final List<Integer> rowIndices = new ArrayList<>();

    private final RadialUiDragReorder drag = new RadialUiDragReorder();

    /** Entries on their way to another menu, applied when this screen is saved. */
    private final List<MenuNode[]> pendingMoves = new ArrayList<>();

    private int generalSectionTop;
    private int entriesSectionTop;

    public RadialMenuSettingsGui(MenuNode menu) {
        this.menu = menu;
        this.toolBelt = ToolBeltMenu.isMenu(menu);
        this.readOnly = menu.isBuiltIn() && !toolBelt;
        this.draft = RadialSubmenuFields.toSpec(menu);
        this.titleText = menu.title == null ? "" : menu.title;
        this.icon = menu.icon == null ? null : menu.icon.copy();
        this.workingChildren = new ArrayList<>(menu.childrenOrEmpty());
    }

    @Override
    protected String titleKey() {
        // #tr radialmenu.menu.title
        // # Menu settings
        // # zh_CN 菜单设置
        return "radialmenu.menu.title";
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
    protected void buildControls() {
        if (readOnly) {
            setContentHeight(RadialUi.STEP);
            addBottomBar("gui.done", null, null);
            return;
        }
        fields.clear();
        inputs.clear();
        sliders.clear();
        fieldRowTops.clear();

        int left = contentLeft();
        int controlLeft = left + RadialUi.LABEL_WIDTH + RadialUi.GAP;
        int controlWidth = contentRight() - controlLeft;

        generalSectionTop = scrolledTop();
        int y = generalSectionTop + 14;

        // Built at its real size: GuiTextField works out its horizontal scroll from the width it has when the
        // text is set, so a placeholder-sized field resized afterwards renders blank until it is clicked.
        titleField = new GuiTextField(this.fontRendererObj, controlLeft + 1, y + 3, controlWidth - 2, 14);
        titleField.setMaxStringLength(48);
        titleField.setText(titleText);
        titleField.setCursorPositionZero();
        y += RadialUi.STEP;

        if (toolBelt) {
            iconRowTop = y;
            String iconLabel = icon == null || icon.id == null ? I18n.format("radialmenu.editor.pickIcon")
                : RadialUi.fit(icon.id, controlWidth - RadialUi.ROW - 16);
            this.buttonList.add(
                new GuiButton(
                    ID_ICON,
                    controlLeft + RadialUi.ROW + RadialUi.GAP,
                    y,
                    controlWidth - RadialUi.ROW - RadialUi.GAP,
                    RadialUi.ROW,
                    iconLabel));
            tooltip(ID_ICON, describe("radialmenu.editor.icon.tip"));
            y += RadialUi.STEP;
        }

        List<ActionField> available = RadialSubmenuFields.fields(menu);
        for (int fieldIndex = 0; fieldIndex < available.size(); fieldIndex++) {
            ActionField field = available.get(fieldIndex);
            if (!RadialFieldControls.isRelevant(field, draft)) {
                continue;
            }
            ActionField nextField = fieldIndex + 1 < available.size() ? available.get(fieldIndex + 1) : null;
            int index = fields.size();
            fields.add(field);
            tooltip(
                ID_FIELD_BASE + index,
                toolBelt && ActionTypes.PARAM_SLOT_COUNT.equals(field.key) ? RadialEditorRestrictions.resizeTip()
                    : describe(field.tooltipKey()));

            String current = draft.getString(field.key, field.defaultValue);
            sliders.add(null);

            if (RadialFieldControls.standsApart(field)) {
                y += RadialUi.GAP;
            }
            fieldRowTops.add(Integer.valueOf(y));

            if (RadialFieldControls.isButton(field)) {
                int mainWidth = RadialFieldControls.hasClear(field)
                    ? controlWidth - RadialUi.GAP - RadialFieldControls.CLEAR_WIDTH
                    : controlWidth;

                boolean editable = field.kind != ActionField.Kind.Color || RadialFieldControls.colorEnabled(field.key);

                GuiButton main = field.kind == ActionField.Kind.Color
                    ? colorButton(ID_FIELD_BASE + index, field, controlLeft, y, mainWidth)
                    : new GuiButton(
                        ID_FIELD_BASE + index,
                        controlLeft,
                        y,
                        mainWidth,
                        RadialUi.ROW,
                        RadialFieldControls.buttonLabel(field, current));
                main.enabled = editable;
                this.buttonList.add(main);
                inputs.add(null);

                if (RadialFieldControls.hasClear(field)) {
                    GuiButton clear = new RadialUiIconButton(
                        ID_CLEAR_BASE + index,
                        controlLeft + mainWidth + RadialUi.GAP,
                        y,
                        RadialFieldControls.CLEAR_WIDTH,
                        RadialUi.ROW,
                        RadialUiIconButton.Icon.Cross);
                    clear.enabled = current != null && !current.trim()
                        .isEmpty();
                    this.buttonList.add(clear);
                    tooltip(ID_CLEAR_BASE + index, describe("radialmenu.profileColors.clear.tip"));
                }
                if (!editable) {
                    tooltip(ID_FIELD_BASE + index, RadialFieldControls.colorOffTip(field.key));
                }
            } else if (field.hasRange()) {
                GuiSlider slider = RadialFieldControls
                    .slider(ID_FIELD_BASE + index, controlLeft, y, controlWidth, RadialUi.ROW, field, current);
                this.buttonList.add(slider);
                sliders.set(index, slider);
                inputs.add(null);
            } else {
                GuiTextField input = new GuiTextField(
                    this.fontRendererObj,
                    controlLeft + 1,
                    y + 3,
                    controlWidth - 2,
                    14);
                input.setMaxStringLength(8);
                input.setText(current == null ? "" : current);
                inputs.add(input);
            }
            y += RadialFieldControls.stepAfter(field, nextField);
            if (RadialFieldControls.standsApart(field)) {
                y += RadialUi.GAP;
            }
        }

        y += RadialUi.GAP;
        entriesSectionTop = y;
        y += 14;

        rowPositions.clear();
        rowNodes.clear();
        rowIndices.clear();

        for (int i = 0; i < workingChildren.size(); i++) {
            MenuNode child = workingChildren.get(i);
            if (child == null) {
                continue;
            }

            GuiButton move = new RadialUiIconButton(
                ID_MOVE_BASE + i,
                rowButtonsLeft(),
                y,
                ARROW,
                RadialUi.ROW,
                RadialUiIconButton.Icon.Right);
            GuiButton copy = new RadialUiIconButton(
                ID_COPY_BASE + i,
                rowButtonsLeft() + ARROW + 2,
                y,
                ARROW,
                RadialUi.ROW,
                RadialUiIconButton.Icon.Copy);
            GuiButton delete = new RadialUiIconButton(
                ID_DELETE_BASE + i,
                rowButtonsLeft() + (ARROW + 2) * 2,
                y,
                ARROW,
                RadialUi.ROW,
                RadialUiIconButton.Icon.Cross);
            GuiButton up = new RadialUiIconButton(
                ID_UP_BASE + i,
                contentRight() - ARROW * 2 - 2,
                y,
                ARROW,
                RadialUi.ROW,
                RadialUiIconButton.Icon.Up);
            GuiButton down = new RadialUiIconButton(
                ID_DOWN_BASE + i,
                contentRight() - ARROW,
                y,
                ARROW,
                RadialUi.ROW,
                RadialUiIconButton.Icon.Down);
            copy.enabled = !toolBelt && !child.isBuiltIn();
            delete.enabled = !toolBelt && !child.isBuiltIn();
            move.enabled = !toolBelt && !child.isBuiltIn();
            up.enabled = canMove(i, -1);
            down.enabled = canMove(i, 1);
            this.buttonList.add(move);
            this.buttonList.add(copy);
            this.buttonList.add(delete);
            this.buttonList.add(up);
            this.buttonList.add(down);
            // #tr radialmenu.menu.move.tip
            // # Move this entry into another submenu
            // # zh_CN 将此条目移动到另一个子菜单
            tooltip(ID_MOVE_BASE + i, describe("radialmenu.menu.move.tip"));
            // #tr radialmenu.menu.copy.tip
            // # Duplicate this entry
            // # zh_CN 复制此条目
            tooltip(ID_COPY_BASE + i, describe("radialmenu.menu.copy.tip"));
            // #tr radialmenu.menu.remove.tip
            // # Remove this entry. Changes take effect when saved
            // # zh_CN 移除此条目，保存后生效
            tooltip(ID_DELETE_BASE + i, describe("radialmenu.menu.remove.tip"));
            if (toolBelt || child.isBuiltIn()) {
                tooltip(ID_COPY_BASE + i, RadialEditorRestrictions.builtInTip());
                tooltip(ID_DELETE_BASE + i, RadialEditorRestrictions.builtInTip());
                tooltip(ID_MOVE_BASE + i, RadialEditorRestrictions.builtInTip());
                if (toolBelt) {
                    tooltip(ID_UP_BASE + i, RadialEditorRestrictions.builtInTip());
                    tooltip(ID_DOWN_BASE + i, RadialEditorRestrictions.builtInTip());
                }
            }

            rowPositions.add(new int[] { left, y });
            rowNodes.add(child);
            rowIndices.add(Integer.valueOf(i));
            y += RadialUi.STEP;
        }

        // The queued moves are drawn under the list, so they are part of what scrolls - left out of the height
        // they would sit outside the clip and be cut off by the frame.
        y += pendingMoves.size() * 10;

        setContentHeight(y - scrolledTop() + RadialUi.PAD);
        addBottomBar("radialmenu.editor.save", null, "gui.cancel");
        if (toolBelt) tooltip(ID_PRIMARY, RadialEditorRestrictions.resizeTip());
    }

    private int rowButtonsLeft() {
        return contentRight() - ARROW * 5 - 4 - GROUP_GAP;
    }

    /** Whether a move would change anything, so a button that cannot act is visibly disabled. */
    private boolean canMove(int index, int direction) {
        if (toolBelt) return false;
        List<MenuNode> probe = new ArrayList<>(workingChildren);
        return MenuNode.moveInList(probe, index, direction, isFixedLayout());
    }

    /** The layout as chosen on screen, which may not yet be the one saved on the node. */
    private boolean isFixedLayout() {
        return draft.getEnum(ActionTypes.PARAM_SLOT_MODE, SlotLayout.Mode.class, SlotLayout.Mode.Fixed)
            == SlotLayout.Mode.Fixed;
    }

    private static String accentStart() {
        int highlight = StyleResolver.resolve(
            MainConfig.client.radialMenu.colors.defaultColors(),
            ProfileManager.active().style,
            null).highlight;
        return String.format("#%06X", Integer.valueOf(highlight & 0x00FFFFFF));
    }

    private RadialUiColorButton colorButton(int id, final ActionField field, int x, int y, int width) {
        return new RadialUiColorButton(id, x, y, width, new RadialUiColorButton.Value() {

            @Override
            public String get() {
                return draft.getString(field.key, "");
            }
        }, RadialFieldControls.inheritedColor(field.key), I18n.format("radialmenu.editor.inherit"));
    }

    private static String describe(String key) {
        String translated = I18n.format(key);
        return translated.equals(key) ? null : translated;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (readOnly) {
            onCancel();
            return;
        }
        if (button.id == ID_PRIMARY) {
            save();
            return;
        }
        if (button.id == ID_SECONDARY) {
            onCancel();
            return;
        }
        if (button.id == ID_ICON && toolBelt) {
            capture();
            RadialGuiStack.push(new RadialIconPickerGui(this, icon == null ? null : icon.color));
            return;
        }

        int clearIndex = button.id - ID_CLEAR_BASE;
        if (clearIndex >= 0 && clearIndex < fields.size()) {
            capture();
            draft.set(fields.get(clearIndex).key, "");
            requestRebuild();
            return;
        }

        int upIndex = button.id - ID_UP_BASE;
        if (!toolBelt && upIndex >= 0 && upIndex < workingChildren.size()) {
            capture();
            MenuNode.moveInList(workingChildren, upIndex, -1, isFixedLayout());
            requestRebuild();
            return;
        }
        int downIndex = button.id - ID_DOWN_BASE;
        if (!toolBelt && downIndex >= 0 && downIndex < workingChildren.size()) {
            capture();
            MenuNode.moveInList(workingChildren, downIndex, 1, isFixedLayout());
            requestRebuild();
            return;
        }

        int copyIndex = button.id - ID_COPY_BASE;
        if (!toolBelt && copyIndex >= 0 && copyIndex < workingChildren.size()) {
            capture();
            duplicate(copyIndex);
            requestRebuild();
            return;
        }
        int deleteIndex = button.id - ID_DELETE_BASE;
        if (!toolBelt && deleteIndex >= 0 && deleteIndex < workingChildren.size()) {
            capture();
            remove(deleteIndex);
            requestRebuild();
            return;
        }
        int moveIndex = button.id - ID_MOVE_BASE;
        if (!toolBelt && moveIndex >= 0 && moveIndex < workingChildren.size()) {
            capture();
            askWhereToMove(moveIndex);
            return;
        }

        int index = button.id - ID_FIELD_BASE;
        if (index < 0 || index >= fields.size()) {
            return;
        }
        final ActionField field = fields.get(index);
        String current = draft.getString(field.key, field.defaultValue);

        if (field.kind == ActionField.Kind.Color) {
            capture();
            RadialGuiStack.push(new RadialColorPickerGui(current, true, new RadialColorPickerGui.Result() {

                @Override
                public void onColorPicked(String hex) {
                    draft.set(field.key, hex);
                    requestRebuild();
                }
            }));
            return;
        }

        if (field.kind == ActionField.Kind.Accent) {
            capture();
            RadialGuiStack.push(new RadialColorPickerGui(accentStart(), false, new RadialColorPickerGui.Result() {

                @Override
                public void onColorPicked(String hex) {
                    RadialFieldControls.applyAccent(draft, field, hex);
                    requestRebuild();
                }
            }));
            return;
        }

        if (field.kind == ActionField.Kind.Enum) {
            // Rebuilding throws away anything typed but not yet read back, so it is read back first.
            capture();
            int at = field.options.indexOf(current);
            String next = field.options.get((at + 1) % field.options.size());
            draft.set(field.key, next);
            requestRebuild();
        }
    }

    private void duplicate(int index) {
        MenuNode original = workingChildren.get(index);
        if (original == null || original.isBuiltIn()) {
            return;
        }
        MenuNode copy = original.copy();

        if (!isFixedLayout()) {
            workingChildren.add(index + 1, copy);
            return;
        }
        for (int i = index + 1; i < workingChildren.size(); i++) {
            if (workingChildren.get(i) == null) {
                workingChildren.set(i, copy);
                return;
            }
        }
        workingChildren.add(copy);
    }

    private void remove(int index) {
        MenuNode removed = workingChildren.get(index);
        if (removed != null && removed.isBuiltIn()) {
            return;
        }
        workingChildren.set(index, null);
        // An entry queued to move into a submenu that is itself being deleted would arrive nowhere.
        dropMovesInto(removed);
    }

    private void dropMovesInto(MenuNode gone) {
        if (gone == null || !gone.isCategory()) {
            return;
        }
        for (int i = pendingMoves.size() - 1; i >= 0; i--) {
            if (pendingMoves.get(i)[1] == gone) {
                pendingMoves.remove(i);
            }
        }
    }

    private void askWhereToMove(final int index) {
        final MenuNode moved = workingChildren.get(index);
        if (moved == null || moved.isBuiltIn()) {
            return;
        }
        RadialGuiStack.push(new RadialMenuPickerGui(menu, moved, new RadialMenuPickerGui.Result() {

            @Override
            public void onMenuPicked(MenuNode target) {
                if (target == null || target == menu || target.isBuiltIn()) {
                    return;
                }
                workingChildren.set(index, null);
                pendingMoves.add(new MenuNode[] { moved, target });
                requestRebuild();
            }
        }));
    }

    private void capture() {
        if (titleField != null) {
            titleText = titleField.getText();
        }
        for (int i = 0; i < fields.size() && i < inputs.size(); i++) {
            GuiTextField input = inputs.get(i);
            if (input != null) {
                draft.set(fields.get(i).key, input.getText());
            }
            GuiSlider slider = sliders.get(i);
            if (slider != null) {
                draft.set(fields.get(i).key, Integer.toString(slider.getValueInt()));
            }
        }
    }

    private void save() {
        if (readOnly) {
            return;
        }
        capture();

        String title = titleText.trim();
        menu.title = title.isEmpty() ? null : title;

        if (toolBelt) {
            menu.icon = icon;
        } else {
            menu.children = new ArrayList<>(workingChildren);
        }

        for (MenuNode[] move : pendingMoves) {
            MenuNode node = move[0];
            MenuNode target = move[1];
            target.setChildAt(target.appendIndex(), node);
            target.ensureSlotCapacity();
        }

        // applyToNode reads the spec off the node, the same way the slot editor hands it over, and clears it
        // afterwards - a category carries no action at any depth.
        menu.action = draft;
        RadialSubmenuFields.applyToNode(menu);
        if (toolBelt) {
            int requestedSlots = menu.layout.slots;
            ToolBeltMenu.requestResize(menu, requestedSlots);
        }

        ProfileManager.active()
            .normalize();
        ProfileManager.saveActive();
        IconRenderer.clearCache();
        RadialGuiStack.closeAll();
    }

    @Override
    public void onIconPicked(IconSpec icon) {
        this.icon = icon;
        requestRebuild();
    }

    @Override
    protected void onCancel() {
        RadialGuiStack.closeAll();
    }

    @Override
    protected void beforeScroll() {
        capture();
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        if (readOnly) {
            this.fontRendererObj
                .drawString(RadialEditorRestrictions.builtInTip(), contentLeft(), contentTop(), RadialUi.TEXT);
            return;
        }
        int left = contentLeft();

        if (isVisibleRow(generalSectionTop)) {
            // #tr radialmenu.menu.sectionGeneral
            // # Appearance
            // # zh_CN 外观
            RadialUi
                .sectionHeader(I18n.format("radialmenu.menu.sectionGeneral"), left, generalSectionTop, contentRight());
        }

        int y = generalSectionTop + 14;

        if (isVisibleRow(y)) {
            // #tr radialmenu.menu.name
            // # Name
            // # zh_CN 名称
            RadialUi.rowLabel(I18n.format("radialmenu.menu.name"), left, y);
            titleField.drawTextBox();
        }
        y += RadialUi.STEP;

        if (toolBelt && isVisibleRow(iconRowTop)) {
            RadialUi.rowLabel(I18n.format("radialmenu.editor.icon"), left, iconRowTop);
            int iconLeft = left + RadialUi.LABEL_WIDTH + RadialUi.GAP;
            RadialUi.frame(
                iconLeft,
                iconRowTop,
                iconLeft + RadialUi.ROW,
                iconRowTop + RadialUi.ROW,
                0xFF101010,
                RadialUi.PANEL_BORDER);
            if (icon != null) IconRenderer.draw(icon, iconLeft + 2, iconRowTop + 2);
        }

        for (int i = 0; i < fields.size() && i < fieldRowTops.size(); i++) {
            int rowTop = fieldRowTops.get(i)
                .intValue();
            if (isVisibleRow(rowTop)) {
                RadialUi
                    .rowLabel(RadialUi.fit(I18n.format(fields.get(i).labelKey), RadialUi.LABEL_WIDTH), left, rowTop);
            }
        }

        for (GuiTextField input : inputs) {
            if (input != null && isVisibleRow(input.yPosition - 3)) {
                input.drawTextBox();
            }
        }

        if (isVisibleRow(entriesSectionTop)) {
            // #tr radialmenu.menu.entries
            // # Entries
            // # zh_CN 条目
            RadialUi.sectionHeader(I18n.format("radialmenu.menu.entries"), left, entriesSectionTop, contentRight());
        }

        if (rowNodes.isEmpty() && isVisibleRow(entriesSectionTop + 14)) {
            this.fontRendererObj
                // #tr radialmenu.menu.noEntries
                // # This menu has no entries yet
                // # zh_CN 此菜单还没有任何条目
                .drawString(
                    I18n.format("radialmenu.menu.noEntries"),
                    left,
                    entriesSectionTop + 18,
                    RadialUi.TEXT_MUTED);
        }

        for (int i = 0; i < rowNodes.size(); i++) {
            int[] position = rowPositions.get(i);
            if (!isVisibleRow(position[1])) {
                continue;
            }
            MenuNode child = rowNodes.get(i);
            int iconLeft = position[0] + RadialUiDragReorder.GRIP + 2;

            if (drag.isDragging() && drag.grabbedRow() == i) {
                // The row being carried, marked where it came from: without it, a drag of one row in a list of
                // identical-looking rows gives no clue what is moving.
                Gui.drawRect(
                    position[0],
                    position[1],
                    contentRight(),
                    position[1] + RadialUi.ROW,
                    RadialUi.ROW_SELECTED);
            }
            if (!toolBelt) {
                RadialUiDragReorder
                    .drawGrip(position[0], position[1], RadialUi.ROW, drag.isDragging() && drag.grabbedRow() == i);
            }

            IconRenderer.draw(child.icon, iconLeft, position[1] + 2);

            String title = child.title == null || child.title.isEmpty() ? I18n.format("radialmenu.menu.untitled")
                : child.title;
            this.fontRendererObj.drawString(
                RadialUi.fit(title, rowButtonsLeft() - iconLeft - 24),
                iconLeft + 20,
                position[1] + (RadialUi.ROW - 8) / 2,
                RadialUi.TEXT);
        }

        drawPendingMoves();

        drag.drawInsertion(rows(), contentLeft(), contentRight());
        scrollTowardsDrag();
    }

    /** Says where entries have gone, for as long as they have not gone there yet. */
    private void drawPendingMoves() {
        if (pendingMoves.isEmpty()) {
            return;
        }
        int y = rowPositions.isEmpty() ? entriesSectionTop + 18
            : rowPositions.get(rowPositions.size() - 1)[1] + RadialUi.STEP;
        for (MenuNode[] move : pendingMoves) {
            if (!isVisibleRow(y)) {
                y += 10;
                continue;
            }
            this.fontRendererObj.drawString(
                // #tr radialmenu.menu.movePending
                // # %s moves to %s when you save
                // # zh_CN %s 将在保存时移动到 %s
                RadialUi.fit(I18n.format("radialmenu.menu.movePending", name(move[0]), name(move[1])), contentWidth()),
                contentLeft(),
                y + 2,
                RadialUi.TEXT_MUTED);
            y += 10;
        }
    }

    private String name(MenuNode node) {
        if (node == ProfileManager.active().root) {
            return ProfileManager.activeName();
        }
        return node.title == null || node.title.isEmpty() ? I18n.format("radialmenu.menu.untitled") : node.title;
    }

    private void scrollTowardsDrag() {
        if (!drag.isDragging()) {
            return;
        }
        int overshoot = drag.overshoot(rows());
        if (overshoot == 0) {
            return;
        }
        int step = Math.max(-6, Math.min(6, overshoot));
        int next = Math.max(0, Math.min(maxScroll(), scrollOffset + step));
        if (next != scrollOffset) {
            beforeScroll();
            scrollOffset = next;
            requestRebuild();
        }
    }

    private RadialUiDragReorder.Rows rows() {
        return new RadialUiDragReorder.Rows() {

            @Override
            public int count() {
                return rowPositions.size();
            }

            @Override
            public int top(int row) {
                return rowPositions.get(row)[1];
            }

            @Override
            public int height() {
                return RadialUi.ROW;
            }
        };
    }

    private void moveRow(int from, int to) {
        if (toolBelt || from < 0 || to < 0 || from == to || from >= rowIndices.size() || to >= rowIndices.size()) {
            return;
        }

        int direction = to > from ? 1 : -1;
        int index = rowIndices.get(from)
            .intValue();
        int steps = 0;
        int cap = workingChildren.size() * 2;

        while (rowOf(index) != to && steps++ < cap) {
            if (!MenuNode.moveInList(workingChildren, index, direction, isFixedLayout())) {
                break;
            }
            index = workingChildren.indexOf(rowNodes.get(from));
        }
    }

    /** Which drawn row a list position is, counting only the entries that are drawn. */
    private int rowOf(int index) {
        int row = 0;
        for (int i = 0; i < index && i < workingChildren.size(); i++) {
            if (workingChildren.get(i) != null) {
                row++;
            }
        }
        return row;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        // Before super, and the whole point of the grip: a press that lands on one must not reach a button at all.
        if (!toolBelt && button == 0 && isInsideViewport(mouseY) && drag.press(mouseX, mouseY, contentLeft(), rows())) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
        // A field clipped at the panel edge still answers to clicks on the part that was cut away.
        if (readOnly || !isShowing() || !isInsideViewport(mouseY)) {
            return;
        }
        titleField.mouseClicked(mouseX, mouseY, button);
        for (GuiTextField input : inputs) {
            if (input != null) {
                input.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceClick) {
        drag.moveTo(mouseY);
        super.mouseClickMove(mouseX, mouseY, mouseButton, timeSinceClick);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int which) {
        if (which == 0) {
            int from = drag.grabbedRow();
            int to = drag.release(rows());
            if (to >= 0 && to != from) {
                capture();
                moveRow(from, to);
                requestRebuild();
            }
        }
        super.mouseMovedOrUp(mouseX, mouseY, which);
    }

    @Override
    protected boolean handleKey(char typedChar, int keyCode) {
        if (readOnly) {
            return false;
        }
        if (keyCode == Keyboard.KEY_ESCAPE && drag.isDragging()) {
            drag.cancel();
            return true;
        }
        if (titleField.textboxKeyTyped(typedChar, keyCode)) {
            return true;
        }
        for (GuiTextField input : inputs) {
            if (input != null && input.textboxKeyTyped(typedChar, keyCode)) {
                return true;
            }
        }
        return false;
    }
}
