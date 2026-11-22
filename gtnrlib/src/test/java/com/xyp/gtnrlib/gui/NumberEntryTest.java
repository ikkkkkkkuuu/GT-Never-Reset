package com.xyp.gtnrlib.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.value.LongValue;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnrlib.gui.widget.NumberEntryWidget;

import it.unimi.dsi.fastutil.longs.LongArrayList;

public class NumberEntryTest {

    @Test
    public void typedExpressions() {
        NumberEntryWidget entry = new NumberEntryWidget().bounds(0, 1_000_000);
        type(entry, "2 * 64");
        assertEquals(OptionalLong.of(128), entry.value(), "a typed expression is worked out");
        type(entry, "1.5k");
        assertEquals(OptionalLong.of(1500), entry.value(), "an SI suffix scales the number");
        type(entry, "2.5");
        assertFalse(entry.isInputValid(), "a fraction is not a whole number");
        assertEquals(OptionalLong.empty(), entry.value(), "invalid input has no value");
        type(entry, "abc");
        assertFalse(entry.isInputValid(), "text that is not a number is invalid");
        type(entry, " ");
        assertFalse(entry.isInputValid(), "blank input is invalid");
    }

    @Test
    public void boundsHold() {
        NumberEntryWidget entry = new NumberEntryWidget().bounds(1, 64);
        type(entry, "64");
        assertEquals(OptionalLong.of(64), entry.value(), "the maximum itself is accepted");
        type(entry, "65");
        assertFalse(entry.isInputValid(), "a value above the maximum is invalid");
        type(entry, "0");
        assertFalse(entry.isInputValid(), "a value below the minimum is invalid");
        entry.value(100);
        assertFalse(entry.isInputValid(), "a value set from code outside the bounds stays visible as invalid");
        entry.bounds(10, 5);
        type(entry, "10");
        assertTrue(entry.isInputValid(), "a maximum below the minimum is raised to the minimum");
        type(entry, "11");
        assertFalse(entry.isInputValid(), "the raised maximum still holds");
    }

    @Test
    public void hugeNumbers() {
        NumberEntryWidget entry = new NumberEntryWidget();
        type(entry, Long.toString(Long.MAX_VALUE));
        assertEquals(OptionalLong.of(Long.MAX_VALUE), entry.value(), "the largest long is accepted by default");
        type(entry, "9223372036854775808");
        assertFalse(entry.isInputValid(), "one past the largest long is invalid rather than wrapping around");
        type(entry, "-1");
        assertFalse(entry.isInputValid(), "the default minimum is zero");
        entry.bounds(Long.MIN_VALUE, Long.MAX_VALUE);
        type(entry, Long.toString(Long.MIN_VALUE));
        assertEquals(OptionalLong.of(Long.MIN_VALUE), entry.value(), "the smallest long is accepted when allowed");
    }

    @Test
    public void listenerTiming() {
        LongArrayList heard = new LongArrayList();
        NumberEntryWidget entry = new NumberEntryWidget().bounds(0, 100)
            .onChange(heard::add);
        entry.value(5);
        entry.bounds(0, 50);
        assertTrue(heard.isEmpty(), "values and bounds set from code are not reported");
        type(entry, "7");
        assertEquals(LongArrayList.of(7), heard, "typing a new valid value reports it");
        type(entry, "3 + 4");
        assertEquals(LongArrayList.of(7), heard, "an expression with the same result is not reported again");
        type(entry, "x");
        assertEquals(LongArrayList.of(7), heard, "invalid input is not reported");
        type(entry, "7");
        assertEquals(LongArrayList.of(7, 7), heard, "input that becomes valid again is reported, same value or not");
        type(entry, "80");
        assertEquals(LongArrayList.of(7, 7), heard, "a value outside the bounds is not reported");
    }

    @Test
    public void boundValue() {
        LongValue value = new LongValue(5);
        NumberEntryWidget entry = new NumberEntryWidget().bounds(0, 100)
            .value(value);
        assertEquals(OptionalLong.of(5), entry.value(), "a bound entry starts with the value it is bound to");
        type(entry, "12");
        assertEquals(12, value.getLongValue(), "a valid typed value is written into the bound value");
        type(entry, "500");
        assertEquals(12, value.getLongValue(), "a value outside the bounds is not written");
    }

    private static void type(NumberEntryWidget entry, String text) {
        field(entry).getStringValue()
            .setStringValue(text);
    }

    private static TextFieldWidget field(NumberEntryWidget entry) {
        for (IWidget child : entry.getChildren()) if (child instanceof TextFieldWidget field) return field;
        throw new AssertionError("the entry has a text field");
    }
}
