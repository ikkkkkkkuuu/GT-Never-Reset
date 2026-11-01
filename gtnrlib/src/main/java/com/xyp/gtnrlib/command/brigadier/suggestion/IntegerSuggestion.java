package com.xyp.gtnrlib.command.brigadier.suggestion;

import java.util.Objects;

import com.xyp.gtnrlib.command.brigadier.Message;
import com.xyp.gtnrlib.command.brigadier.context.StringRange;

import lombok.Getter;

public class IntegerSuggestion extends Suggestion {

    @Getter
    private int value;

    public IntegerSuggestion(StringRange range, int value) {
        this(range, value, null);
    }

    public IntegerSuggestion(StringRange range, int value, Message tooltip) {
        super(range, Integer.toString(value), tooltip);
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof IntegerSuggestion)) {
            return false;
        } else {
            IntegerSuggestion that = (IntegerSuggestion) o;
            return this.value == that.value && super.equals(o);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.value);
    }

    @Override
    public String toString() {
        return "IntegerSuggestion{value=" + this.value
            + ", range="
            + this.getRange()
            + ", text='"
            + this.getText()
            + '\''
            + ", tooltip='"
            + this.getTooltip()
            + '\''
            + '}';
    }

    @Override
    public int compareTo(Suggestion o) {
        return o instanceof IntegerSuggestion ? Integer.compare(this.value, ((IntegerSuggestion) o).value)
            : super.compareTo(o);
    }

    @Override
    public int compareToIgnoreCase(Suggestion b) {
        return this.compareTo(b);
    }
}
