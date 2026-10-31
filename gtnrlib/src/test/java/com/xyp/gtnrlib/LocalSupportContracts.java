package com.xyp.gtnrlib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.xyp.gtnrlib.config.json.LowercaseEnumAdapterFactory;
import com.xyp.gtnrlib.config.json.StringMapAdapterFactory;
import com.xyp.gtnrlib.gui.wheel.ArcLayout;
import com.xyp.gtnrlib.gui.wheel.RadialGeometry;
import com.xyp.gtnrlib.gui.wheel.RevealAnimation;
import com.xyp.gtnrlib.gui.wheel.RevealTiming;
import com.xyp.gtnrlib.gui.wheel.SlotLayout;
import com.xyp.gtnrlib.text.TooltipLineWrapper;
import com.xyp.gtnrlib.utils.Colors;
import com.xyp.gtnrlib.utils.ImmutableCollections;
import com.xyp.gtnrlib.utils.LongMath;
import com.xyp.gtnrlib.utils.StructureFileCodec;
import com.xyp.gtnrlib.world.ChunkCoords;
import com.xyp.gtnrlib.world.Facing;
import com.xyp.gtnrlib.world.IBlockStateListener;
import com.xyp.gtnrlib.world.WorldListener;
import com.xyp.gtnrlib.world.WorldTime;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public final class LocalSupportContracts {

    public static void verify() throws Exception {
        arithmetic();
        wheel();
        json();
        structures();
        listeners();
        coordinatesAndTooltips();
        collections();
        System.out.println(
            "Local library contracts passed: arithmetic, wheel, JSON, MBS1, listeners, coordinates and tooltip wrapping");
    }

    private static void arithmetic() {
        long[] edges = { Long.MIN_VALUE, Long.MIN_VALUE + 1, -2, -1, 0, 1, 2, Long.MAX_VALUE - 1, Long.MAX_VALUE };
        for (long a : edges) for (long b : edges) arithmetic(a, b);
        Random random = new Random(663);
        for (int i = 0; i < 10000; i++) arithmetic(random.nextLong(), random.nextLong());
    }

    private static void arithmetic(long a, long b) {
        BigInteger left = BigInteger.valueOf(a), right = BigInteger.valueOf(b);
        check(LongMath.saturatedAdd(a, b) == bounded(left.add(right)), "saturated addition");
        check(LongMath.saturatedSubtract(a, b) == bounded(left.subtract(right)), "saturated subtraction");
        check(LongMath.saturatedMultiply(a, b) == bounded(left.multiply(right)), "saturated multiplication");
        if (b != 0) {
            BigInteger[] division = left.divideAndRemainder(right);
            BigInteger ceiling = division[0];
            if (division[1].signum() != 0 && left.signum() == right.signum()) ceiling = ceiling.add(BigInteger.ONE);
            check(LongMath.ceilDiv(a, b) == ceiling.longValue(), "ceiling division including signed boundaries");
        }
    }

    private static void coordinatesAndTooltips() {
        for (int coordinate : new int[] { Integer.MIN_VALUE, -33, -17, -16, -1, 0, 1, 15, 16, 33, Integer.MAX_VALUE }) {
            int chunk = ChunkCoords.chunkOf(coordinate), offset = ChunkCoords.offsetOf(coordinate);
            check(offset >= 0 && offset < 16, "chunk local coordinate is nonnegative");
            check(ChunkCoords.toWorld(chunk, offset) == coordinate, "chunk coordinate round trip");
        }
        check(WorldTime.day(-1) == -1 && WorldTime.timeOfDay(-1) == 23999, "negative world time");
        check(WorldTime.isDay(0) && !WorldTime.isDay(12000) && WorldTime.isDay(24000), "day/night boundaries");
        check(
            Facing.of(-90)
                .equals("east")
                && Facing.of(450)
                    .equals("west"),
            "wrapped compass directions");
        String[] wrapped = TooltipLineWrapper.wrap(new String[] { "§a§l" + "中".repeat(45) });
        check(wrapped.length == 2 && wrapped[0].equals("§a§l" + "中".repeat(38)), "CJK text uses two tooltip cells");
        check(wrapped[1].equals("  §a§l" + "中".repeat(7)), "continuation preserves formatting and indentation");
        wrapped = TooltipLineWrapper.wrap(new String[] { "1234567😀more" }, 8);
        check(wrapped[0].equals("1234567😀") && wrapped[1].equals("  more"), "supplementary character is not split");
        check(
            Arrays.equals(
                TooltipLineWrapper.wrap(new String[] { "", "§aShort §rplain" }),
                new String[] { "", "§aShort §rplain" }),
            "short tooltip lines unchanged");
        try {
            TooltipLineWrapper.wrap(new String[] { "test" }, 5);
            throw new AssertionError("too narrow tooltip must be rejected");
        } catch (IllegalArgumentException expected) {}
        check(Colors.blendRgb(0x804080C0, 0xFFFFFF, 0) == 0x804080C0, "blend retains starting color");
        check(Colors.blendRgb(0x804080C0, 0xFFFFFF, 1) == 0x80FFFFFF, "blend keeps alpha");
        check(Colors.fadeAlpha(0x814080C0, 0.5F) == 0x404080C0, "alpha fade keeps prior truncation");
        check(Colors.fadeAlpha(0x804080C0, 0) == 0x004080C0, "transparent colors retain RGB");
    }

    private static long bounded(BigInteger value) {
        return value.max(BigInteger.valueOf(Long.MIN_VALUE))
            .min(BigInteger.valueOf(Long.MAX_VALUE))
            .longValue();
    }

    private static void collections() {
        String[] source = { "second", null, "first", "second" };
        var list = ImmutableCollections.listOf(source);
        var set = ImmutableCollections.setOf(source);
        source[0] = "changed";
        check(list.equals(Arrays.asList("second", null, "first", "second")), "list snapshots input array");
        check(Arrays.equals(set.toArray(), new String[] { "second", null, "first" }), "set preserves order and null");
        var mutable = new java.util.ArrayList<>(list);
        var copy = ImmutableCollections.copyList(mutable);
        var setCopy = ImmutableCollections.copySet(mutable);
        mutable.clear();
        check(copy.equals(list) && setCopy.equals(set), "collection copies do not alias their input");
        var map = new java.util.LinkedHashMap<String, String>();
        map.put("second", null);
        map.put(null, "first");
        var mapCopy = ImmutableCollections.copyMap(map);
        map.clear();
        check(
            Arrays.equals(
                mapCopy.keySet()
                    .toArray(),
                new String[] { "second", null }),
            "map preserves order and null");
        var collected = list.stream()
            .collect(ImmutableCollections.toImmutableSet());
        check(Arrays.equals(collected.toArray(), set.toArray()), "collector retains encounter order");
        for (Runnable mutation : new Runnable[] { () -> list.add("x"), () -> set.add("x"), () -> copy.clear(),
            () -> setCopy.clear(), () -> mapCopy.put("x", "x"), () -> collected.clear() }) {
            try {
                mutation.run();
                throw new AssertionError("snapshot must be unmodifiable");
            } catch (UnsupportedOperationException expected) {}
        }
    }

    private static void wheel() {
        for (int count = SlotLayout.MIN_SLOTS; count <= SlotLayout.MAX_SLOTS; count++) {
            for (int rotation = -720; rotation <= 720; rotation += 23) {
                for (int slot = 0; slot < count; slot++) {
                    double angle = RadialGeometry.slotCenterAngle(slot, count, rotation);
                    check(
                        RadialGeometry.slotAtPoint(
                            0,
                            0,
                            RadialGeometry.offsetX(angle, 80),
                            RadialGeometry.offsetY(angle, 80),
                            count,
                            rotation,
                            12) == slot,
                        "wheel center hit test");
                }
            }
            for (RevealAnimation animation : RevealAnimation.values()) {
                for (int slot = 0; slot < count; slot++) {
                    double previous = 0;
                    for (int frame = 0; frame <= 60; frame++) {
                        double progress = RevealTiming.slotProgress(animation, slot, count, frame / 60.0, 1);
                        check(progress >= previous && progress <= 1, "monotonic wheel reveal");
                        previous = progress;
                    }
                    check(previous == 1, "complete wheel reveal");
                }
            }
        }
        check(RadialGeometry.slotAtPoint(0, 0, 0, 0, 8, 0, 12) == RadialGeometry.NO_SLOT, "wheel dead zone");
        check(ArcLayout.place(new double[0], new double[0]).length == 0, "empty inline wheel");
        check(Colors.over("#123456", 0x80112233) == 0x80123456, "color override preserves inherited alpha");
        check(Colors.over("0x40123456", 0x80112233) == 0x40123456, "explicit alpha override");
        check(Colors.parseArgb("invalid", 7) == 7, "invalid color falls back");
    }

    private static void json() {
        Gson gson = new GsonBuilder().registerTypeAdapterFactory(new LowercaseEnumAdapterFactory())
            .registerTypeAdapterFactory(new StringMapAdapterFactory())
            .create();
        check(
            gson.toJson(RevealAnimation.STAGGER_CCW)
                .equals("\"stagger_ccw\""),
            "persisted enum spelling");
        check(gson.fromJson("\"DYNAMIC\"", SlotLayout.Mode.class) == SlotLayout.Mode.Dynamic, "legacy enum spelling");
        var mapType = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> values = gson
            .fromJson("{\"lines\":[\"first\",\"\",\"last\"],\"enabled\":true,\"count\":3}", mapType);
        check(
            values.get("lines")
                .equals("first\n\nlast"),
            "multiline JSON");
        check(
            values.get("enabled")
                .equals("true")
                && values.get("count")
                    .equals("3"),
            "legacy scalar JSON");
        check(values.equals(gson.fromJson(gson.toJson(values, mapType), mapType)), "JSON round trip");
    }

    private static void structures() throws IOException {
        byte[] valid;
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(0x4D425331);
            out.writeInt(3);
            for (String row : new String[] { " A ", "~中 ", "   " }) {
                byte[] encoded = row.getBytes(StandardCharsets.UTF_8);
                out.writeInt(encoded.length);
                out.write(encoded);
            }
            out.writeInt(2);
            for (int index : new int[] { 2, 0, 1, 2, 2, 0 }) out.writeInt(index);
            valid = bytes.toByteArray();
        }
        check(
            Arrays.deepEquals(
                StructureFileCodec.decode(new ByteArrayInputStream(valid)),
                new String[][] { { " A ", "~中 " }, { "   ", " A " } }),
            "MBS1 spaces, UTF-8 and layer order");
        for (int length = 0; length < valid.length; length++) malformed(Arrays.copyOf(valid, length));
        malformed(Arrays.copyOf(valid, valid.length + 1));
        byte[] corrupt = valid.clone();
        corrupt[0] = 0;
        malformed(corrupt);
        corrupt = valid.clone();
        Arrays.fill(corrupt, 4, 8, (byte) 0x7F);
        malformed(corrupt);
        corrupt = valid.clone();
        Arrays.fill(corrupt, corrupt.length - 4, corrupt.length, (byte) 0xFF);
        malformed(corrupt);
    }

    private static void malformed(byte[] bytes) {
        try {
            StructureFileCodec.decode(new ByteArrayInputStream(bytes));
            throw new AssertionError("Malformed MBS1 accepted");
        } catch (IOException expected) {
            // A malformed resource must fail before a controller receives a partial shape.
        }
    }

    private static void listeners() {
        WorldListener access = WorldListener.INSTANCE;
        int[] changes = { 0 };
        IBlockStateListener listener = pos -> changes[0]++;
        access.replacePositions(listener, new LongOpenHashSet(new long[] { CoordinatePacker.pack(1, 64, -1) }));
        access.markBlockForUpdate(1, 64, -1);
        access.markBlockForUpdate(2, 64, -1);
        check(changes[0] == 1, "exact position notification");
        access.replacePositions(listener, new LongOpenHashSet(new long[] { CoordinatePacker.pack(2, 64, -1) }));
        access.markBlockForUpdate(1, 64, -1);
        access.markBlockForUpdate(2, 64, -1);
        check(changes[0] == 2, "old position invalidation");
        access.removeListener(listener);
        access.markBlockForUpdate(2, 64, -1);
        check(changes[0] == 2, "listener removal");
        IBlockStateListener[] self = new IBlockStateListener[1];
        self[0] = pos -> {
            changes[0]++;
            access.removeListener(self[0]);
        };
        access.replacePositions(self[0], new LongOpenHashSet(new long[] { CoordinatePacker.pack(3, 64, -1) }));
        access.markBlockForUpdate(3, 64, -1);
        access.markBlockForUpdate(3, 64, -1);
        check(changes[0] == 3, "callback can unregister itself");
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
