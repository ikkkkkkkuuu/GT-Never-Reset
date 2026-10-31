package com.xyp.gtneverreset.client.gui.radialmenu;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;

import com.xyp.gtneverreset.client.radialmenu.ActionSpec;
import com.xyp.gtneverreset.client.radialmenu.ActionType;
import com.xyp.gtneverreset.client.radialmenu.ActionTypes;
import com.xyp.gtnrlib.gui.widget.RadialUi;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RadialStepEditorGui extends RadialActionEditorGui {

    private final int ordinal;

    private final Result result;

    public RadialStepEditorGui(ActionSpec step, int ordinal, Result result) {
        this.ordinal = ordinal;
        this.result = result;

        ActionSpec draft = step == null || step.type == null ? ActionTypes.get(ActionTypes.KEYBIND)
            .newSpec() : step.copy();
        setSpec(draft, draft.type);
    }

    @Override
    protected String titleKey() {
        // #tr radialmenu.steps.title
        // # Step %s
        // # zh_CN 步骤 %s
        return "radialmenu.steps.title";
    }

    @Override
    protected Object[] titleArgs() {
        return new Object[] { Integer.valueOf(ordinal) };
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
        List<ActionType> types = new ArrayList<>();
        for (ActionType type : ActionTypes.all()) {
            if (!ActionTypes.SUBMENU.equals(type.id)) {
                types.add(type);
            }
        }
        return types;
    }

    @Override
    protected void buildControls() {
        int left = contentLeft();
        int controlLeft = left + RadialUi.LABEL_WIDTH + RadialUi.GAP;
        int controlWidth = contentRight() - controlLeft;

        int y = buildActionSection(scrolledTop(), left, controlLeft, controlWidth);

        setContentHeight(y - scrolledTop() + RadialUi.PAD);
        addBottomBar("gui.done", null, "gui.cancel");
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case ID_PRIMARY:
                captureInputs();
                spec.type = selectedType;
                spec.normalize();
                result.onStepEdited(spec);
                RadialGuiStack.pop();
                return;
            case ID_SECONDARY:
                onCancel();
                return;
            default:
                handleActionButton(button);
        }
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        drawActionSection();
    }

    public interface Result {

        void onStepEdited(ActionSpec step);
    }
}
