package com.xyp.gtnrlib.command.tree;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import com.xyp.gtnrlib.command.brigadier.context.CommandContext;
import com.xyp.gtnrlib.command.brigadier.suggestion.Suggestions;
import com.xyp.gtnrlib.command.brigadier.suggestion.SuggestionsBuilder;

public interface ISuggestionProvider {

    Collection<String> getPlayerNames();

    CompletableFuture<Suggestions> getSuggestionsFromServer(CommandContext<ISuggestionProvider> var1,
        SuggestionsBuilder var2);

    CompletableFuture<Suggestions> getSuggestionsFromServer();

    static CompletableFuture<Suggestions> suggest(Iterable<String> iterable, SuggestionsBuilder suggestionsBuilder) {
        String string = suggestionsBuilder.getRemaining()
            .toLowerCase(Locale.ROOT);

        for (String string2 : iterable) {
            if (string2.toLowerCase(Locale.ROOT)
                .startsWith(string)) {
                suggestionsBuilder.suggest(string2);
            }
        }

        return suggestionsBuilder.buildFuture();
    }
}
