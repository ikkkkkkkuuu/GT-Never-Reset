package com.xyp.gtnrlib.command.brigadier.context;

import java.util.Objects;

import com.xyp.gtnrlib.command.brigadier.tree.CommandNode;

import lombok.Getter;

public class ParsedCommandNode<S> {

    @Getter
    private final CommandNode<S> node;
    @Getter
    private final StringRange range;

    public ParsedCommandNode(CommandNode<S> node, StringRange range) {
        this.node = node;
        this.range = range;
    }

    @Override
    public String toString() {
        return this.node + "@" + this.range;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            ParsedCommandNode<?> that = (ParsedCommandNode<?>) o;
            return Objects.equals(this.node, that.node) && Objects.equals(this.range, that.range);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.node, this.range);
    }
}
