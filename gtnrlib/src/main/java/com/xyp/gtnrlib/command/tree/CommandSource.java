package com.xyp.gtnrlib.command.tree;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import com.xyp.gtnrlib.command.brigadier.context.CommandContext;
import com.xyp.gtnrlib.command.brigadier.suggestion.Suggestions;
import com.xyp.gtnrlib.command.brigadier.suggestion.SuggestionsBuilder;

public class CommandSource implements ISuggestionProvider {

    /** Keeps the adapter constructor compatible; suggestions currently use the server-wide player list. */
    public CommandSource(ICommandSender ignoredSender) {}

    public static CommandSource adapt(ICommandSender sender) {
        return new CommandSource(sender);
    }

    @Override
    public Collection<String> getPlayerNames() {
        return Arrays.asList(
            MinecraftServer.getServer()
                .getAllUsernames());
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestionsFromServer(CommandContext<ISuggestionProvider> context,
        SuggestionsBuilder suggestionsBuilder) {
        return null;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestionsFromServer() {
        return null;
    }
}
