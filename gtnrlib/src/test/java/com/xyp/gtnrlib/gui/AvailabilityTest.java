package com.xyp.gtnrlib.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.cleanroommc.modularui.api.widget.Interactable.Result;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.IntValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.xyp.gtnrlib.gui.widget.AE2Button;
import com.xyp.gtnrlib.gui.widget.AECheckbox;
import com.xyp.gtnrlib.gui.widget.Availability;
import com.xyp.gtnrlib.gui.widget.CycleDropdown;
import com.xyp.gtnrlib.gui.widget.ListRow;
import com.xyp.gtnrlib.gui.widget.SettingToggleButton;
import com.xyp.gtnrlib.gui.widget.SlideSwitch;
import com.xyp.gtnrlib.gui.widget.StepSlider;
import com.xyp.gtnrlib.gui.widget.ValueStepper;
import com.xyp.gtnrlib.gui.widget.VerticalButtonBar;

public class AvailabilityTest {

    @Test
    public void swallowsPresses() {
        Availability availability = new Availability();
        int[] handled = { 0 };
        Result open = availability.press(() -> {
            handled[0]++;
            return Result.SUCCESS;
        });
        assertEquals(Result.SUCCESS, open, "an available widget answers the press itself");
        availability.set(() -> false);
        Result shut = availability.press(() -> {
            handled[0]++;
            return Result.SUCCESS;
        });
        assertEquals(Result.STOP, shut, "an unavailable widget stops the press");
        assertEquals(1, handled[0], "an unavailable widget does not run its own handling");
    }

    @Test
    public void checkboxClicks() {
        BoolValue on = new BoolValue(false);
        AECheckbox checkbox = new AECheckbox(() -> "Auto", on).playClickSound(false);
        checkbox.onMousePressed(0);
        assertTrue(on.getBoolValue(), "a click checks the box");
        IntValue mode = new IntValue(0);
        AECheckbox first = AECheckbox.radio(() -> "First", mode, 0)
            .playClickSound(false);
        AECheckbox second = AECheckbox.radio(() -> "Second", mode, 1)
            .playClickSound(false);
        second.onMousePressed(0);
        assertEquals(1, mode.getIntValue(), "a radio button selects the option it stands for");
        first.onMousePressed(0);
        first.onMousePressed(0);
        assertEquals(0, mode.getIntValue(), "clicking a selected radio button keeps it selected");
        checkbox.available(() -> false)
            .onMousePressed(0);
        assertTrue(on.getBoolValue(), "an unavailable check box keeps its value");
    }

    @Test
    public void toolbarSettings() {
        IntValue value = new IntValue(0);
        SettingToggleButton setting = new SettingToggleButton().value(value)
            .stateCount(2)
            .playClickSound(false);
        VerticalButtonBar bar = new VerticalButtonBar().add(setting)
            .available(() -> false);
        assertTrue(
            bar.getChildren()
                .contains(setting),
            "the toolbar takes a setting button as one of its buttons");
        setting.onMousePressed(0);
        assertEquals(0, value.getIntValue(), "the toolbar's available state reaches a setting button added to it");
        assertThrows(
            IllegalStateException.class,
            () -> new VerticalButtonBar().add(new ParentWidget<>())
                .available(() -> false),
            "a widget without an available state of its own cannot take one from the toolbar");
    }

    @Test
    public void widgetsSwallow() {
        int[] clicks = { 0 };
        Result button = new AE2Button(() -> "Apply", () -> clicks[0]++).available(() -> false)
            .onMousePressed(0);
        assertEquals(Result.STOP, button, "an unavailable button stops the press");
        Result row = new ListRow(() -> "Row").checkbox(() -> true, () -> clicks[0]++)
            .available(() -> false)
            .onMousePressed(0);
        assertEquals(Result.STOP, row, "an unavailable clickable row stops the press");
        Result stepper = new ValueStepper(() -> "LV", step -> clicks[0]++).available(() -> false)
            .onMousePressed(0);
        assertEquals(Result.STOP, stepper, "an unavailable stepper stops the press");
        Result slider = new StepSlider().value(() -> 0, value -> clicks[0]++)
            .available(() -> false)
            .onMousePressed(0);
        assertEquals(Result.STOP, slider, "an unavailable slider stops the press");
        Result slideSwitch = new SlideSwitch().onMousePressed(mouse -> clicks[0]++ >= 0)
            .available(() -> false)
            .onMousePressed(0);
        assertEquals(Result.STOP, slideSwitch, "an unavailable switch stops the press");
        Result dropdown = new CycleDropdown<>("availability_menu", String.class).available(() -> false)
            .onMousePressed(0);
        assertEquals(Result.STOP, dropdown, "an unavailable dropdown stops the press");
        assertEquals(0, clicks[0], "no unavailable widget ran its action");
        assertEquals(
            Result.IGNORE,
            new ListRow(() -> "Plain row").available(() -> false)
                .onMousePressed(0),
            "a row without a click action lets the press through, available or not");
        assertEquals(
            Result.IGNORE,
            new ValueStepper(() -> "LV", step -> {}).onMousePressed(0),
            "an available stepper leaves the press to its arrow buttons");
    }
}
