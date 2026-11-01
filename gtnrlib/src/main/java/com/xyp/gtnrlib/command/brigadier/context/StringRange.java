package com.xyp.gtnrlib.command.brigadier.context;

import java.util.Objects;

import com.xyp.gtnrlib.command.brigadier.ImmutableStringReader;

import lombok.Getter;

public class StringRange {

    @Getter
    private final int start;
    @Getter
    private final int end;

    public StringRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public static StringRange at(int pos) {
        return new StringRange(pos, pos);
    }

    public static StringRange between(int start, int end) {
        return new StringRange(start, end);
    }

    public static StringRange encompassing(StringRange a, StringRange b) {
        return new StringRange(Math.min(a.getStart(), b.getStart()), Math.max(a.getEnd(), b.getEnd()));
    }

    public String get(ImmutableStringReader reader) {
        return reader.getString()
            .substring(this.start, this.end);
    }

    public String get(String string) {
        return string.substring(this.start, this.end);
    }

    public boolean isEmpty() {
        return this.start == this.end;
    }

    public int getLength() {
        return this.end - this.start;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof StringRange)) {
            return false;
        } else {
            StringRange that = (StringRange) o;
            return this.start == that.start && this.end == that.end;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.start, this.end);
    }

    @Override
    public String toString() {
        return "StringRange{start=" + this.start + ", end=" + this.end + '}';
    }
}
