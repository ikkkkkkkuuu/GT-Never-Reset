package com.xyp.gtneverreset.client.radialmenu.script;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.xyp.gtnrlib.text.syntax.LuaSyntax;

public final class ScriptSyntax {

    private static final Set<String> GLOBALS = new HashSet<>(
        Arrays.asList(
            "chat",
            "menu",
            "player",
            "chunk",
            "world",
            "inventory",
            "store",
            "prompt",
            "action",
            "notify",
            "log",
            "sleep",
            "print",
            "string",
            "table",
            "math"));

    private static final Set<String> MEMBERS = new HashSet<>(
        Arrays.asList(
            "send",
            "await",
            "awaitAll",
            "line",
            "open",
            "close",
            "update",
            "run",
            "maxEntries",
            "name",
            "dim",
            "x",
            "y",
            "z",
            "chunkX",
            "chunkZ",
            "xInChunk",
            "zInChunk",
            "yaw",
            "pitch",
            "facing",
            "of",
            "offset",
            "lookingAt",
            "time",
            "day",
            "isDay",
            "items",
            "count",
            "has",
            "get",
            "set",
            "health",
            "food",
            "air",
            "held",
            "meta",
            "slot",
            "kind",
            "toWorld",
            "size",
            "key",
            "label",
            "icon",
            "color",
            "onPick"));

    public static final LuaSyntax LUA = new LuaSyntax(GLOBALS, MEMBERS);
}
