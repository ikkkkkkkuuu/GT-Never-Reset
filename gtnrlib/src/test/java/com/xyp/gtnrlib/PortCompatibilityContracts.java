package com.xyp.gtnrlib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.xyp.gtnrlib.client.text.EffectTextRenderer;
import com.xyp.gtnrlib.client.text.TextEffectRegistry;
import com.xyp.gtnrlib.client.text.effect.BuiltinTextEffects;
import com.xyp.gtnrlib.command.CommandItemInfo;
import com.xyp.gtnrlib.command.CommandSuicide;
import com.xyp.gtnrlib.text.EffectTextParser;
import com.xyp.gtnrlib.text.TextEffectStyle;
import com.xyp.gtnrlib.text.TextEffects;
import com.xyp.gtnrlib.utils.StructureFileCodec;

final class PortCompatibilityContracts {

    static void verify() throws Exception {
        BuiltinTextEffects.register();
        check(
            TextEffectRegistry.identifiers()
                .size() == 24,
            "Exactly 24 builtins, aliases are not duplicate effects");
        for (String id : TextEffectRegistry.identifiers()) {
            String name = id.substring(id.indexOf(':') + 1);
            check(
                Files.isRegularFile(Path.of("src/main/resources/assets/gtnrlib/shaders/text/" + name + ".frag.glsl")),
                "Missing shader " + id);
            TextEffectStyle style = new TextEffectStyle(id, List.of(0x123456, 0xabcdef), 1.25F);
            var parsed = EffectTextParser.parse(TextEffects.apply("中文 Ω", style));
            check(
                parsed.plainText()
                    .equals("中文 Ω")
                    && parsed.runs()
                        .get(0)
                        .style()
                        .equals(style),
                "Parameterized style round trip " + id);
        }
        check(
            TextEffectRegistry.get("gtnrlib:exotic_rainbow") == TextEffectRegistry.get("gtneverreset:exotic_rainbow"),
            "Legacy rainbow alias resolves to the same renderer");
        for (String foreign : List.of(
            "\u2063[gtnl:gtnllib:exotic_rainbow;1.0;]\u2063Upstream\u2063[/gtnl]\u2063",
            "&{gtnllib:pulse_upwards}Upstream",
            "§{sciencenotleisure:exotic_rainbow}Legacy")) {
            check(!EffectTextRenderer.handles(foreign), "Do not claim foreign renderer text");
            check(
                TextEffects.plainText(foreign)
                    .equals(foreign),
                "Do not strip foreign formatting");
        }
        check(EffectTextRenderer.handles(TextEffects.apply("legacy")), "Legacy scoped format is still rendered");
        check(EffectTextRenderer.handles("&{gtnrlib:exotic_rainbow}new"), "Qualified rainbow alias is rendered");
        check(
            new CommandItemInfo().getCommandName()
                .equals("gtnr_iteminfo"),
            "Isolated item command");
        check(
            new CommandSuicide().getCommandName()
                .equals("gtnr_suicide"),
            "Isolated suicide command");
        String[][] shape = { { " A ", "   " }, { "中  ", " A " } };
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        StructureFileCodec.writeBinary(bytes, shape);
        check(
            Arrays.deepEquals(shape, StructureFileCodec.decode(new ByteArrayInputStream(bytes.toByteArray()))),
            "Writer interoperates with bounded local decoder without losing spaces or layers");
        System.out.println("Upstream port compatibility contracts passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
