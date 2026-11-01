package com.xyp.gtnrlib.command.brigadier;

import java.util.Collection;

import com.xyp.gtnrlib.command.brigadier.tree.CommandNode;

@FunctionalInterface
public interface AmbiguityConsumer<S> {

    void ambiguous(CommandNode<S> var1, CommandNode<S> var2, CommandNode<S> var3, Collection<String> var4);
}
