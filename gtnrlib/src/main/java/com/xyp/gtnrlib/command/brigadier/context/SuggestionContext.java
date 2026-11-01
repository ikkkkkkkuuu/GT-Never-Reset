package com.xyp.gtnrlib.command.brigadier.context;

import com.xyp.gtnrlib.command.brigadier.tree.CommandNode;

public class SuggestionContext<S> {

    public final CommandNode<S> parent;
    public final int startPos;

    public SuggestionContext(CommandNode<S> parent, int startPos) {
        this.parent = parent;
        this.startPos = startPos;
    }
}
