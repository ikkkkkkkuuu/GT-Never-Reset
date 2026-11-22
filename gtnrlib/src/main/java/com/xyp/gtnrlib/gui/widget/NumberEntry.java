package com.xyp.gtnrlib.gui.widget;

import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.utils.Alignment;
import com.xyp.gtnrlib.gui.theme.AE2Theme.Variant;

/** @deprecated Use {@link NumberEntryWidget}. */
@Deprecated
public class NumberEntry extends NumberEntryWidget {

    public NumberEntry(@Nullable Variant variant) {
        this();
    }

    public boolean isValueValid() {
        return isInputValid();
    }

    public NumberEntry() {
        super();
    }

    @Override
    public NumberEntry resetValue(long reset) {
        super.resetValue(reset);
        return this;
    }

    @Override
    public NumberEntry available(BooleanSupplier available) {
        super.available(available);
        return this;
    }

    @Override
    public NumberEntry bounds(long min, long max) {
        super.bounds(min, max);
        return this;
    }

    @Override
    public NumberEntry value(long value) {
        super.value(value);
        return this;
    }

    @Override
    public NumberEntry onChange(LongConsumer listener) {
        super.onChange(listener);
        return this;
    }

    @Override
    public NumberEntry follow(LongSupplier source) {
        super.follow(source);
        return this;
    }

    @Override
    public NumberEntry textAlignment(Alignment alignment) {
        super.textAlignment(alignment);
        return this;
    }

    @Override
    public NumberEntry wheel(boolean wheel) {
        super.wheel(wheel);
        return this;
    }
}
