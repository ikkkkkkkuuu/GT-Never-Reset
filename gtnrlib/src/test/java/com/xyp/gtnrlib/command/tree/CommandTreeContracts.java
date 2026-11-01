package com.xyp.gtnrlib.command.tree;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

import com.xyp.gtnrlib.client.command.ClientCommandTree;
import com.xyp.gtnrlib.command.brigadier.CommandDispatcher;
import com.xyp.gtnrlib.command.brigadier.arguments.IntegerArgumentType;
import com.xyp.gtnrlib.command.brigadier.builder.LiteralArgumentBuilder;
import com.xyp.gtnrlib.command.brigadier.builder.RequiredArgumentBuilder;
import com.xyp.gtnrlib.command.brigadier.tree.ArgumentCommandNode;
import com.xyp.gtnrlib.command.brigadier.tree.RootCommandNode;
import com.xyp.gtnrlib.command.commodore.CommodoreFileReader;
import com.xyp.gtnrlib.command.tree.serialization.ArgumentTypes;
import com.xyp.gtnrlib.network.CommandTreePacket;

import io.netty.buffer.Unpooled;

public final class CommandTreeContracts {

    public static void verify() throws Exception {
        ArgumentTypes.init();
        CommandDispatcher<ISuggestionProvider> dispatcher = new CommandDispatcher<>();
        var node = dispatcher.register(
            LiteralArgumentBuilder.<ISuggestionProvider>literal("count")
                .then(
                    RequiredArgumentBuilder
                        .<ISuggestionProvider, Integer>argument("amount", IntegerArgumentType.integer(1, 64))));
        dispatcher.register(
            LiteralArgumentBuilder.<ISuggestionProvider>literal("c")
                .redirect(node));
        RootCommandNode<ISuggestionProvider> decoded = CommandTreeCodec
            .read(CommandTreeCodec.create(dispatcher.getRoot()));
        check(
            decoded.getChild("c")
                .getRedirect() == decoded.getChild("count"),
            "alias target survives serialization");
        var argument = (ArgumentCommandNode<ISuggestionProvider, ?>) decoded.getChild("count")
            .getChild("amount");
        var range = (IntegerArgumentType) argument.getType();
        check(range.getMinimum() == 1 && range.getMaximum() == 64, "numeric constraints survive serialization");
        var parsed = new CommandDispatcher<>(decoded).parse("count 65", null);
        check(
            !parsed.getExceptions()
                .isEmpty(),
            "invalid arguments remain rejected");

        Map<String, ICommand> commands = new LinkedHashMap<>();
        var allowed = new TestCommand("visible", true);
        commands.put("visible", allowed);
        commands.put("alias", allowed);
        commands.put("hidden", new TestCommand("hidden", false));
        RootCommandNode<ISuggestionProvider> visible = CommandTreeService.createTree(commands, null);
        check(visible.getChild("hidden") == null, "unauthorized command stays out of the transmitted tree");
        check(
            visible.getChild("alias")
                .getRedirect() == visible.getChild("visible"),
            "server aliases preserve identity");
        var visibleCopy = CommandTreeCodec.read(CommandTreeCodec.create(visible));
        check(
            visibleCopy.getChild("alias")
                .getRedirect() == visibleCopy.getChild("visible"),
            "server alias round trip");
        var client = new CommandDispatcher<ISuggestionProvider>(visibleCopy);
        commands.put("local", new TestCommand("local", true));
        ClientCommandTree.merge(client, commands, null);
        check(
            client.getRoot()
                .getChild("local") != null
                && client.getRoot()
                    .getChild("hidden") == null,
            "client commands respect permission checks");

        try (var paths = Files.walk(Path.of("src/main/resources/assets/gtnrlib/commandtree/commands"))) {
            for (Path path : paths.filter(
                p -> p.toString()
                    .endsWith(".commodore"))
                .toList()) {
                var definition = CommodoreFileReader.INSTANCE.<ISuggestionProvider>parse(path);
                var root = new RootCommandNode<ISuggestionProvider>();
                root.addChild(definition);
                check(
                    CommandTreeCodec.read(CommandTreeCodec.create(root))
                        .getChild(definition.getName()) != null,
                    "bundled definition round trip: " + path);
            }
        }

        for (int length : new int[] { -1, CommandTreePacket.MAX_TREE_BYTES + 1, 10 }) {
            var buffer = Unpooled.buffer();
            try {
                buffer.writeInt(length);
                try {
                    new CommandTreePacket().fromBytes(buffer);
                    throw new AssertionError("invalid payload accepted: " + length);
                } catch (IllegalArgumentException expected) {}
            } finally {
                buffer.release();
            }
        }
        System.out.println("Command tree parser, permissions, aliases and packet contracts passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class TestCommand extends CommandBase {

        private final String name;
        private final boolean permitted;

        private TestCommand(String name, boolean permitted) {
            this.name = name;
            this.permitted = permitted;
        }

        @Override
        public String getCommandName() {
            return name;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return name;
        }

        @Override
        public List<String> getCommandAliases() {
            return List.of("alias");
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return permitted;
        }

        @Override
        public void processCommand(ICommandSender sender, String[] arguments) {}
    }
}
