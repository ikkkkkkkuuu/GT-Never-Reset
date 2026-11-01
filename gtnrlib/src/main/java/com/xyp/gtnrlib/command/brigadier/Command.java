package com.xyp.gtnrlib.command.brigadier;

import com.xyp.gtnrlib.command.brigadier.context.CommandContext;
import com.xyp.gtnrlib.command.brigadier.exceptions.CommandSyntaxException;

@FunctionalInterface
public interface Command<S> {

    int SINGLE_SUCCESS = 1;

    int run(CommandContext<S> var1) throws CommandSyntaxException;
}
