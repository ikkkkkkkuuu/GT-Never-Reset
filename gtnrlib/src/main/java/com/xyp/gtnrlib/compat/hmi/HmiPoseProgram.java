package com.xyp.gtnrlib.compat.hmi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import org.joml.Matrix4f;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JseMathLib;

/** Executes the release's three pose stages in their shared namespace, independently of the game and GL. */
public final class HmiPoseProgram {

    private static final String DIGEST = "dc90bce5ff8d4a9932ec2226d8d5a2644a190fbe6681b7a26bb1438679f3f9f8";
    private static final String ROOT = "resourcepacks/pack_test/assets/minecraft/holdmyitems/";
    private static final Pattern DEFAULT = Pattern.compile("(?m)^\\s*global\\.(\\w+)\\s*=\\s*(-?[\\d.]+|true|false);");
    private final Globals compiler = new Globals();
    private final LuaTable env = new LuaTable();
    public final LuaTable frame = new LuaTable();
    private final LuaTable context = new LuaTable();
    private int bowStage = -1;
    private final LuaValue shared, relative, item, model;
    private final Map<Integer, Matrix4f> parts = new HashMap<>();
    private final Matrix4f working = new Matrix4f(), scene = new Matrix4f(), shear = new Matrix4f(),
        partOp = new Matrix4f();
    public final Matrix4f armMatrix = new Matrix4f(), itemMatrix = new Matrix4f();
    public static final String EMPTY = "minecraft:air";

    public record Scripts(String hand, String arm, String item, String model, Map<String, byte[]> assets) {

        public static Scripts read(Path path) throws Exception {
            byte[] bytes;
            try (var in = Files.newInputStream(path)) {
                bytes = in.readNBytes(860882);
            }
            if (bytes.length != 860881 || !MessageDigest.isEqual(
                MessageDigest.getInstance("SHA-256")
                    .digest(bytes),
                com.google.common.io.BaseEncoding.base16()
                    .lowerCase()
                    .decode(DIGEST)))
                throw new IOException("Expected Hold My Items 5.1.1 for 1.21.11 (Modrinth KZm1qTcU)");
            Map<String, String> scripts = new HashMap<>();
            Map<String, byte[]> assets = new HashMap<>();
            try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
                for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    String name = entry.getName();
                    if (name.startsWith(ROOT) && name.endsWith(".lua"))
                        scripts.put(name, new String(zip.readAllBytes(), StandardCharsets.UTF_8));
                    String prefix = "resourcepacks/pack_test/assets/minecraft/";
                    if (name.startsWith(prefix) && !entry.isDirectory() && !name.startsWith(ROOT))
                        assets.put(name.substring(prefix.length()), zip.readAllBytes());
                }
            }
            return new Scripts(
                scripts.get(ROOT + "hand_pose.lua"),
                scripts.get(ROOT + "hand_relative_pose.lua"),
                scripts.get(ROOT + "item_pose.lua"),
                scripts.get(ROOT + "item_model.lua"),
                Map.copyOf(assets));
        }
    }

    public HmiPoseProgram(Scripts scripts) throws IOException {
        // Only the named primitives are reachable by the release programs. No package, file, class or host access.
        compiler.load(new org.luaj.vm2.lib.jse.JseBaseLib());
        compiler.load(new org.luaj.vm2.lib.PackageLib());
        compiler.load(new TableLib());
        compiler.load(new StringLib());
        compiler.load(new JseMathLib());
        LuaC.install(compiler);
        for (String name : new String[] { "ipairs", "pairs", "type", "tonumber", "tostring", "math", "string",
            "table" }) env.set(name, compiler.get(name));
        env.set("context", context);
        env.set("data", context);
        env.set("frame", frame);
        context.set("player", frame);
        context.set("matrices", new LuaTable());
        context.set("particles", new LuaTable());
        LuaTable math = new LuaTable();
        LuaTable animator = new LuaTable();
        for (String op : new String[] { "translate", "scale", "shear", "moveX", "moveY", "moveZ", "rotateX", "rotateY",
            "rotateZ" }) {
            math.set(op, new VarArgFunction() {

                @Override
                public Varargs invoke(Varargs args) {
                    transform(working, op, args, 3);
                    return NONE;
                }
            });
            animator.set(op, new VarArgFunction() {

                @Override
                public Varargs invoke(Varargs args) {
                    int end = args.checkint(3);
                    transform(partOp.identity(), op, args, 4);
                    for (int index = args.checkint(2); index <= end; index++)
                        parts.computeIfAbsent(index, k -> new Matrix4f())
                            .mulLocal(partOp);
                    return NONE;
                }
            });
        }
        env.set("M", math);
        env.set("animator", animator);
        env.set("maps", env);
        try (var stream = HmiPoseProgram.class.getResourceAsStream("bridge.lua")) {
            if (stream == null) throw new IOException("Missing HMI bridge");
            compiler.load(stream, "bridge", "t", env)
                .call();
        }
        env.set("maps", LuaValue.NIL);
        shared = compile("hand_pose", scripts.hand);
        relative = compile("hand_relative_pose", scripts.arm);
        item = compile("item_pose", scripts.item);
        model = compile("item_model", scripts.model);
    }

    private LuaValue compile(String name, String source) {
        var matcher = DEFAULT.matcher(source);
        while (matcher.find()) {
            String key = matcher.group(1), value = matcher.group(2);
            if (env.get(key)
                .isnil())
                env.set(
                    key,
                    value.equals("true") || value.equals("false") ? LuaValue.valueOf(Boolean.parseBoolean(value))
                        : LuaValue.valueOf(Double.parseDouble(value)));
        }
        String code = matcher.replaceAll("")
            .replaceAll("\\$\\{(xOffset|yOffset|zOffset|inspectKeybind)\\}", "0");
        return compiler.load(code, name, env);
    }

    public int swingDuration(String id) {
        return Math.max(
            1,
            env.get("itemSwingSpeed")
                .get(id)
                .optint(10));
    }

    public boolean renderAsBlock(String id) {
        return env.get("renderAsBlock")
            .get(id)
            .optboolean(true);
    }

    public int bowStage() {
        return bowStage;
    }

    public Matrix4f part(int index) {
        return parts.get(index);
    }

    public void pose(boolean main, String id, float progress, float dip, boolean changed, boolean alternate,
        boolean breaking, float seconds) {
        if (main) frame.set("glow", LuaValue.NIL);
        context.set("bl", LuaValue.valueOf(main));
        context.set("mainHand", LuaValue.valueOf(main));
        context.set("hand", main ? "main" : "off");
        context.set("item", id);
        context.set("mainHandSwingProgress", progress);
        context.set("offHandSwingProgress", 0);
        context.set("mainHandSwitchEvent", LuaValue.valueOf(changed));
        context.set("offHandSwitchEvent", LuaValue.FALSE);
        context.set("swingMHand", LuaValue.valueOf(alternate));
        context.set("swingOHand", LuaValue.FALSE);
        context.set("blockBreaking", LuaValue.valueOf(breaking));
        context.set("deltaTime", seconds);
        float swing = main ? progress : 0;
        working.identity();
        stage(shared, swing, dip);
        if (!EMPTY.equals(id)) working.translate(0, -.35f, .2f);
        scene.set(working);
        stage(relative, swing, dip);
        armMatrix.set(working);
        working.set(scene);
        working.translate(main ? .5f : -.5f, -.15f, -.85f)
            .translate(.5f, .5f, .5f)
            .rotateX(rad(15))
            .translate(-.5f, -.5f, -.5f)
            .scale(.9f);
        stage(
            item,
            main && frame.get("block")
                .toboolean() && renderAsBlock(id) ? 0 : swing,
            dip);
        itemMatrix.set(working);
        if (main && !EMPTY.equals(id)) {
            parts.values()
                .forEach(Matrix4f::identity);
            context.set("swingProgress", swing);
            context.set("equipProgress", dip);
            model.call();
            if (id.equals("minecraft:bow")) {
                double pull = env.get("useDuration")
                    .get(id)
                    .optdouble(0);
                bowStage = !env.get("usingItem")
                    .get(id)
                    .toboolean() ? -1 : pull >= 18 ? 2 : pull >= 13 ? 1 : 0;
            }
        }
        if (!armMatrix.isFinite() || !itemMatrix.isFinite())
            throw new IllegalStateException("Non-finite HMI pose for " + id);
    }

    private void stage(LuaValue script, float progress, float dip) {
        context.set("swingProgress", progress);
        context.set("equipProgress", dip);
        script.call();
    }

    private static float rad(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    private void transform(Matrix4f target, String op, Varargs args, int start) {
        float x = (float) args.checkdouble(start);
        if (op.startsWith("rotate")) {
            float px = (float) args.optdouble(start + 1, 0), py = (float) args.optdouble(start + 2, 0),
                pz = (float) args.optdouble(start + 3, 0);
            if (x == 0) return;
            target.translate(px, py, pz);
            switch (op) {
                case "rotateX" -> target.rotateX(rad(x));
                case "rotateY" -> target.rotateY(rad(x));
                case "rotateZ" -> target.rotateZ(rad(x));
            }
            target.translate(-px, -py, -pz);
        } else {
            float y = (float) args.optdouble(start + 1, 0), z = (float) args.optdouble(start + 2, 0);
            switch (op) {
                case "moveX" -> target.translate(x, 0, 0);
                case "moveY" -> target.translate(0, x, 0);
                case "moveZ" -> target.translate(0, 0, x);
                case "translate" -> target.translate(x, y, z);
                case "scale" -> target.scale(x, y, z);
                case "shear" -> target.mul(shear.set(1, x, x, 0, y, 1, y, 0, z, z, 1, 0, 0, 0, 0, 1));
            }
        }
    }
}
