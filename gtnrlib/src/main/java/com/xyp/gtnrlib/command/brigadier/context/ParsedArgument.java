package com.xyp.gtnrlib.command.brigadier.context;

import java.util.Objects;

import lombok.Getter;

public class ParsedArgument<S, T> {

    @Getter
    private final StringRange range;
    @Getter
    private final T result;

    public ParsedArgument(int start, int end, T result) {
        this.range = StringRange.between(start, end);
        this.result = result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ParsedArgument)) {
            return false;
        } else {
            ParsedArgument<?, ?> that = (ParsedArgument<?, ?>) o;
            return Objects.equals(this.range, that.range) && Objects.equals(this.result, that.result);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.range, this.result);
    }
}
