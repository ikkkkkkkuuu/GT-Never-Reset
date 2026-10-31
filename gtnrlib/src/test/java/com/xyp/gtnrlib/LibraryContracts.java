package com.xyp.gtnrlib;

import java.nio.file.Files;
import java.nio.file.Path;

import com.xyp.gtnrlib.text.EffectTextParser;
import com.xyp.gtnrlib.text.TextEffectFormat;
import com.xyp.gtnrlib.text.TextEffects;

public final class LibraryContracts {

    public static void main(String[] args) throws Exception {
        absent("com.xyp.gtneverreset.GTNeverReset");
        absent("gregtech.api.GregTechAPI");
        LocalSupportContracts.verify();
        SharedImplementationContracts.verify();
        SharedGuiContracts.verify();
        InputAndSyntaxContracts.verify();
        GTNRLibContractTest.main(new String[0]);
        PortCompatibilityContracts.verify();
        for (String text : new String[] { "", "plain", "中文", "line\nnext", "§aGreen §lbold", "emoji \uD83D\uDE00" }) {
            check(
                TextEffects.plainText(TextEffects.apply(text))
                    .equals(text),
                "text round trip: " + text);
        }
        String literal = "before\u2063after";
        check(
            TextEffects.plainText(TextEffects.apply(TextEffects.escapeLiteral(literal)))
                .equals(literal),
            "escaped marker survives inside a styled span");
        check(
            TextEffects.EXOTIC_RAINBOW.rendererId()
                .equals("gtneverreset:exotic_rainbow"),
            "legacy style id");
        check(
            TextEffects
                .plainText(
                    EffectTextParser.OPEN + "outer" + TextEffects.apply("inner") + "tail" + EffectTextParser.CLOSE)
                .equals("outerinnertail"),
            "nested spans");
        check(TextEffectFormat.nativeLength("§x§1§2§3§4§5§6", 0) == 14, "hex formatting width");
        check(TextEffectFormat.hexColor("§x§1§2§3§4§5§6", 0) == 0x123456, "hex color");
        check(TextEffectFormat.hexColor("§x§1§2", 0) == -1, "truncated hex is rejected");
        try (var sources = Files.walk(Path.of("src/main/java"))) {
            for (Path source : sources.filter(
                p -> p.toString()
                    .endsWith(".java"))
                .toList()) {
                String content = Files.readString(source);
                check(!content.contains("com.xyp.gtneverreset"), "host dependency: " + source);
                check(!content.contains("import gregtech."), "GregTech dependency: " + source);
            }
        }
        System.out.println("GTNRLib text and boundary contracts passed");
    }

    private static void absent(String name) throws Exception {
        try {
            Class.forName(name, false, LibraryContracts.class.getClassLoader());
            throw new AssertionError("Unexpected runtime dependency: " + name);
        } catch (ClassNotFoundException expected) {
            // The library must work independently of its host.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
