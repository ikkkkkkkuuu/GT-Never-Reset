package com.xyp.gtnrlib.command.brigadier;

import java.util.Collection;

import com.xyp.gtnrlib.command.brigadier.context.CommandContext;
import com.xyp.gtnrlib.command.brigadier.exceptions.CommandSyntaxException;

@FunctionalInterface
public interface RedirectModifier<S> {

    Collection<S> apply(CommandContext<S> var1) throws CommandSyntaxException;
}
