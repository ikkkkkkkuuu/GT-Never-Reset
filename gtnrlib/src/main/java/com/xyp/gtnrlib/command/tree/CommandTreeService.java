package com.xyp.gtnrlib.command.tree;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.google.common.collect.Maps;
import com.xyp.gtnrlib.command.brigadier.CommandDispatcher;
import com.xyp.gtnrlib.command.brigadier.arguments.StringArgumentType;
import com.xyp.gtnrlib.command.brigadier.builder.LiteralArgumentBuilder;
import com.xyp.gtnrlib.command.brigadier.builder.RequiredArgumentBuilder;
import com.xyp.gtnrlib.command.brigadier.tree.CommandNode;
import com.xyp.gtnrlib.command.brigadier.tree.LiteralCommandNode;
import com.xyp.gtnrlib.command.brigadier.tree.RootCommandNode;
import com.xyp.gtnrlib.command.tree.serialization.ArgumentTypes;
import com.xyp.gtnrlib.network.GTNRLibNetwork;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

/** Sends each player the commands they are permitted to use. */
public final class CommandTreeService {

    private CommandTreeService() {}

    public static void init() {
        ArgumentTypes.init();
        CommandTreeDefinitions.init();
        FMLCommonHandler.instance()
            .bus()
            .register(new CommandTreeService());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            sendCommands(player);
        }
    }

    @SuppressWarnings("unchecked")
    private void sendCommands(EntityPlayerMP player) {
        Map<String, ICommand> commands = (Map<String, ICommand>) MinecraftServer.getServer()
            .getCommandManager()
            .getCommands();
        GTNRLibNetwork.sendCommands(CommandTreeCodec.create(createTree(commands, player)), player);
    }

    static RootCommandNode<ISuggestionProvider> createTree(Map<String, ICommand> commandMap, ICommandSender sender) {
        CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();

        // Deduplicate: commandMap contains both primary names and aliases pointing to the same ICommand
        Set<ICommand> seen = new HashSet<ICommand>();
        for (ICommand command : commandMap.values()) {
            if (seen.add(command) && command.canCommandSenderUseCommand(sender)) {
                registerCommand(dispatcher, command);
            }
        }

        // Convert to client-side tree and send
        CommandSource source = CommandSource.adapt(sender);
        Map<CommandNode<CommandSource>, CommandNode<ISuggestionProvider>> nodeMapping = Maps.newHashMap();
        RootCommandNode<ISuggestionProvider> clientRoot = new RootCommandNode<ISuggestionProvider>();
        nodeMapping.put(dispatcher.getRoot(), clientRoot);
        new CommandTreeConverter(nodeMapping, source).convertChildren(dispatcher.getRoot(), clientRoot);

        return clientRoot;
    }

    @SuppressWarnings("unchecked")
    private static void registerCommand(CommandDispatcher<CommandSource> dispatcher, ICommand command) {
        LiteralCommandNode<CommandSource> node;
        if (CommandTreeDefinitions.hasCompatFor(command.getClass())) {
            node = CommandTreeDefinitions.getCompatCommand(command.getClass());
        } else {
            node = LiteralArgumentBuilder.<CommandSource>literal(command.getCommandName())
                .then(
                    RequiredArgumentBuilder.<CommandSource, String>argument("params", StringArgumentType.greedyString())
                        .suggests((context, suggestions) -> suggestions.buildFuture()))
                .build();
        }

        dispatcher.getRoot()
            .addChild(node);
        List<?> aliases = command.getCommandAliases();
        if (aliases != null) {
            for (Object aliasObj : aliases) {
                String alias = (String) aliasObj;
                if (!alias.equals(command.getCommandName())) {
                    dispatcher.register(
                        LiteralArgumentBuilder.<CommandSource>literal(alias)
                            .redirect(node));
                }
            }
        }
    }
}
