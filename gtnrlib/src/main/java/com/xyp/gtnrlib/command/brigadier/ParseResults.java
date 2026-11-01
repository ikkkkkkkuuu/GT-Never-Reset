package com.xyp.gtnrlib.command.brigadier;

import java.util.Collections;
import java.util.Map;

import com.xyp.gtnrlib.command.brigadier.context.CommandContextBuilder;
import com.xyp.gtnrlib.command.brigadier.exceptions.CommandSyntaxException;
import com.xyp.gtnrlib.command.brigadier.tree.CommandNode;

import lombok.Getter;

public class ParseResults<S> {

    @Getter
    private final CommandContextBuilder<S> context;
    @Getter
    private final Map<CommandNode<S>, CommandSyntaxException> exceptions;
    @Getter
    private final ImmutableStringReader reader;

    public ParseResults(CommandContextBuilder<S> context, ImmutableStringReader reader,
        Map<CommandNode<S>, CommandSyntaxException> exceptions) {
        this.context = context;
        this.reader = reader;
        this.exceptions = exceptions;
    }

    public ParseResults(CommandContextBuilder<S> context) {
        this(context, new StringReader(""), Collections.emptyMap());
    }

}
