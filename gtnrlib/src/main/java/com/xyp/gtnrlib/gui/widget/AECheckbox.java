package com.xyp.gtnrlib.gui.widget;

import java.util.function.Supplier;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.value.IBoolValue;
import com.cleanroommc.modularui.api.value.IIntValue;
import com.cleanroommc.modularui.widget.sizer.Unit.Measure;
import com.xyp.gtnrlib.gui.theme.AE2Theme;
import com.xyp.gtnrlib.gui.theme.ae2.AE2CheckboxPainter;
import com.xyp.gtnrlib.gui.theme.ae2.AE2Look;

/**
 * AE2's check box with its label ({@code AECheckbox}), or one radio button of a group, drawn by the theme of its panel
 * ({@link AE2CheckboxPainter#checkbox}, {@link AE2CheckboxPainter#radio}). A left click anywhere on it flips the check
 * box, or selects the radio button. Radio buttons that share one integer value form a group: each stands for one value,
 * and selecting it sets the value, so only one is selected at a time.
 * <p>
 * Values are MUI2 values, so a sync value registered with the panel's sync manager makes it work in GUIs the server
 * opens. Unless the caller sets a size, it is as high as the box and as wide as the box and one line of label in the
 * look of its panel, measured while the panel is laid out and again whenever the label's text changes. It is a
 * {@link ThemedButton}, so {@code available} disables it.
 */
public class AECheckbox extends ThemedButton<AECheckbox> {

    private final Supplier<String> label;
    private String measured;

    /**
     * Creates a check box.
     *
     * @param label supplier of the label, read every frame
     * @param value value the box shows and flips
     */
    public AECheckbox(Supplier<String> label, IBoolValue<?> value) {
        this(label, AE2Theme.CHECKBOX_HEIGHT);
        width(() -> AE2CheckboxPainter.checkboxWidth(AE2Look.of(this), label.get()), Measure.PIXEL);
        overlay(AE2CheckboxPainter.checkbox(this, value::getBoolValue, label, this::isAvailable));
        action(() -> value.setBoolValue(!value.getBoolValue()));
    }

    private AECheckbox(Supplier<String> label, int height) {
        this.label = label;
        background(IDrawable.EMPTY).hoverBackground(IDrawable.EMPTY)
            .height(height);
    }

    /**
     * Creates a radio button of the group that shares a value.
     *
     * @param label  supplier of the label, read every frame
     * @param value  value shared by the group's radio buttons
     * @param option value this radio button stands for
     * @return the radio button
     */
    public static AECheckbox radio(Supplier<String> label, IIntValue<?> value, int option) {
        AECheckbox radio = new AECheckbox(label, AE2Theme.RADIO_SIZE);
        radio.width(() -> AE2CheckboxPainter.radioWidth(label.get()), Measure.PIXEL)
            .overlay(AE2CheckboxPainter.radio(radio, () -> value.getIntValue() == option, label, radio::isAvailable));
        radio.action(() -> value.setIntValue(option));
        return radio;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        String text = label.get();
        if (text.equals(measured)) return;
        measured = text;
        scheduleResize();
    }
}
