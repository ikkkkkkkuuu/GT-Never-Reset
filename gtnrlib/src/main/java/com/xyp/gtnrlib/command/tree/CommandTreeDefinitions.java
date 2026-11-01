package com.xyp.gtnrlib.command.tree;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.command.brigadier.tree.LiteralCommandNode;
import com.xyp.gtnrlib.command.commodore.CommodoreFileReader;
import com.xyp.gtnrlib.mod.ModList;

public final class CommandTreeDefinitions {

    private static final Map<String, LiteralCommandNode<CommandSource>> COMMANDS = new HashMap<>();

    private CommandTreeDefinitions() {}

    public static void init() {
        register(
            "minecraft",
            "net.minecraft.command.CommandDefaultGameMode",
            "net.minecraft.command.CommandDifficulty",
            "net.minecraft.command.CommandGameMode",
            "net.minecraft.command.CommandGameRule",
            "net.minecraft.command.CommandTime",
            "net.minecraft.command.CommandWeather",
            "net.minecraft.command.server.CommandWhitelist");
        if (ModList.Baubles.isModLoaded()) register("baubles", "baubles.common.event.CommandBaubles");
        if (ModList.Thaumcraft.isModLoaded()) register("thaumcraft", "thaumcraft.common.lib.CommandThaumcraft");
    }

    public static boolean hasCompatFor(Class<?> type) {
        return COMMANDS.containsKey(type.getName());
    }

    public static LiteralCommandNode<CommandSource> getCompatCommand(Class<?> type) {
        return COMMANDS.get(type.getName());
    }

    private static void register(String directory, String... classes) {
        for (String name : classes) {
            String path = "/assets/" + GTNRLib.MODID + "/commandtree/commands/" + directory + "/" + name + ".commodore";
            try (InputStream stream = CommandTreeDefinitions.class.getResourceAsStream(path)) {
                if (stream == null) throw new IOException("Missing command definition: " + path);
                COMMANDS.put(name, CommodoreFileReader.INSTANCE.parse(stream));
            } catch (IOException exception) {
                GTNRLib.LOG.warn("could not load command definition {}", name, exception);
            }
        }
    }
}
