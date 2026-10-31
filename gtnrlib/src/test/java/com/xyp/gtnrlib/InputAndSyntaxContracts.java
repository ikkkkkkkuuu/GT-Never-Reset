package com.xyp.gtnrlib;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.xyp.gtnrlib.client.input.KeyModifiers;
import com.xyp.gtnrlib.text.syntax.LuaSyntax;

final class InputAndSyntaxContracts {

    static void verify() {
        Set<String> globals = new HashSet<>(Set.of("api"));
        LuaSyntax syntax = new LuaSyntax(globals, Set.of("value", "call"));
        globals.clear();
        check(syntax.isGlobal("api"), "vocabulary snapshot");
        check(!syntax.isGlobal(null) && !syntax.isMember(null), "null vocabulary lookup");
        LuaSyntax independent = new LuaSyntax(Set.of("other"), Set.of("different"));
        check(!independent.isGlobal("api") && !syntax.isGlobal("other"), "independent vocabularies");
        kind(syntax, "local", LuaSyntax.Kind.Declaration);
        kind(syntax, "while", LuaSyntax.Kind.Keyword);
        kind(syntax, "true", LuaSyntax.Kind.Constant);
        kind(syntax, "self", LuaSyntax.Kind.Self);
        kind(syntax, "api", LuaSyntax.Kind.Api);
        kind(independent, "api", LuaSyntax.Kind.Plain);
        kind(syntax, "0xFF", LuaSyntax.Kind.Number);
        kind(syntax, "'escaped\\'quote'", LuaSyntax.Kind.String);
        kind(syntax, "-- comment", LuaSyntax.Kind.Comment);
        var fields = syntax.tokenize("api.value api.unknown api.call()")
            .get(0);
        check(fields.get(2).kind == LuaSyntax.Kind.Field, "known member");
        check(
            fields.stream()
                .anyMatch(t -> t.kind == LuaSyntax.Kind.Function),
            "known callable member");
        List<String> lines = List.of("--[=[comment", "still comment]=] local x", "[==[string", "still string]==] true");
        var tokens = syntax.tokenize(lines);
        check(
            tokens.get(1)
                .get(0).kind == LuaSyntax.Kind.Comment,
            "multiline comment continuation");
        check(
            tokens.get(1)
                .stream()
                .anyMatch(t -> t.kind == LuaSyntax.Kind.Declaration),
            "comment closes");
        check(
            tokens.get(3)
                .get(0).kind == LuaSyntax.Kind.String,
            "multiline string continuation");
        check(
            tokens.get(3)
                .stream()
                .anyMatch(t -> t.kind == LuaSyntax.Kind.Constant),
            "string closes");
        for (int i = 0; i < lines.size(); i++) {
            int position = 0;
            for (var token : tokens.get(i)) {
                check(
                    token.start == position && token.end <= lines.get(i)
                        .length(),
                    "complete ordered spans");
                position = token.end;
            }
            check(
                position == lines.get(i)
                    .length(),
                "line covered");
        }
        check(
            syntax.tokenize((String) null)
                .size() == 1,
            "null source is empty line");
        check(
            syntax.tokenize((List<String>) null)
                .isEmpty(),
            "null lines");
        for (int mask = 0; mask < 8; mask++) {
            KeyModifiers modifiers = new KeyModifiers((mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0);
            check(modifiers.any() == (mask != 0), "modifier combination " + mask);
        }
        check(
            KeyModifiers.NONE.toString()
                .equals("none"),
            "empty modifiers");
        check(
            new KeyModifiers(true, true, true).toString()
                .equals("shift+ctrl+alt"),
            "modifier labels");
        System.out.println("Independent Lua vocabulary, lexical spans and key modifier contracts passed");
    }

    private static void kind(LuaSyntax syntax, String source, LuaSyntax.Kind kind) {
        var tokens = syntax.tokenize(source)
            .get(0);
        check(tokens.size() == 1 && tokens.get(0).kind == kind, source + " classification");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
