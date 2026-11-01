package com.xyp.gtnrlib.command.brigadier.arguments;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import com.xyp.gtnrlib.command.brigadier.StringReader;
import com.xyp.gtnrlib.command.brigadier.context.CommandContext;
import com.xyp.gtnrlib.command.brigadier.exceptions.CommandSyntaxException;
import com.xyp.gtnrlib.command.brigadier.suggestion.Suggestions;
import com.xyp.gtnrlib.command.brigadier.suggestion.SuggestionsBuilder;

public interface ArgumentType<T> {

    T parse(StringReader var1) throws CommandSyntaxException;

    default <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return Suggestions.empty();
    }

    default Collection<String> getExamples() {
        return Collections.emptyList();
    }
}
