package com.xyp.gtnrlib.command.brigadier.suggestion;

import java.util.concurrent.CompletableFuture;

import com.xyp.gtnrlib.command.brigadier.context.CommandContext;
import com.xyp.gtnrlib.command.brigadier.exceptions.CommandSyntaxException;

@FunctionalInterface
public interface SuggestionProvider<S> {

    CompletableFuture<Suggestions> getSuggestions(CommandContext<S> var1, SuggestionsBuilder var2)
        throws CommandSyntaxException;
}
