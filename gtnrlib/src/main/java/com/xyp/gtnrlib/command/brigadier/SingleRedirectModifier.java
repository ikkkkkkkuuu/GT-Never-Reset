package com.xyp.gtnrlib.command.brigadier;

import com.xyp.gtnrlib.command.brigadier.context.CommandContext;
import com.xyp.gtnrlib.command.brigadier.exceptions.CommandSyntaxException;

@FunctionalInterface
public interface SingleRedirectModifier<S> {

    S apply(CommandContext<S> var1) throws CommandSyntaxException;
}
