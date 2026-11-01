package com.xyp.gtnrlib.client.command;

import java.util.Map;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

import com.xyp.gtnrlib.command.brigadier.CommandDispatcher;
import com.xyp.gtnrlib.command.brigadier.arguments.StringArgumentType;
import com.xyp.gtnrlib.command.brigadier.builder.LiteralArgumentBuilder;
import com.xyp.gtnrlib.command.brigadier.builder.RequiredArgumentBuilder;
import com.xyp.gtnrlib.command.brigadier.suggestion.Suggestion;
import com.xyp.gtnrlib.command.brigadier.suggestion.Suggestions;
import com.xyp.gtnrlib.command.tree.ISuggestionProvider;
import com.xyp.gtnrlib.command.tree.SuggestionProviders;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Adds Forge client commands to the suggestions sent by the server. */
@SideOnly(Side.CLIENT)
public final class ClientCommandTree {

    private ClientCommandTree() {}

    /**
     * Adds registered client command names and aliases that the local player may use.
     * Calling this again is safe; it also picks up commands registered after login.
     *
     * @param dispatcher the current suggestions tree
     * @param commands   Forge's client command map, including aliases
     * @param sender     the local player whose permissions should be checked
     */
    public static void merge(CommandDispatcher<ISuggestionProvider> dispatcher, Map<String, ICommand> commands,
        ICommandSender sender) {
        for (Map.Entry<String, ICommand> entry : commands.entrySet()) {
            String name = entry.getKey();
            if (name == null || name.isEmpty()
                || dispatcher.getRoot()
                    .getChild(name) != null
                || !entry.getValue()
                    .canCommandSenderUseCommand(sender)) {
                continue;
            }
            dispatcher.register(
                LiteralArgumentBuilder.<ISuggestionProvider>literal(name)
                    .then(
                        RequiredArgumentBuilder
                            .<ISuggestionProvider, String>argument("params", StringArgumentType.greedyString())));
        }
    }

    /** Supplements the login tree after permissions change, preserving detailed command definitions. */
    public static void mergeServer(CommandDispatcher<ISuggestionProvider> dispatcher, Suggestions commands) {
        for (Suggestion suggestion : commands.getList()) {
            String name = suggestion.getText();
            if (name.startsWith("/")) name = name.substring(1);
            if (name.isEmpty() || dispatcher.getRoot()
                .getChild(name) != null) continue;
            dispatcher.register(
                LiteralArgumentBuilder.<ISuggestionProvider>literal(name)
                    .then(
                        RequiredArgumentBuilder
                            .<ISuggestionProvider, String>argument("params", StringArgumentType.greedyString())
                            .suggests(SuggestionProviders.ASK_SERVER)));
        }
    }
}
