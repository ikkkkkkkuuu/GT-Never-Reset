import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;

import org.joml.Matrix4f;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

import com.xyp.gtnrlib.compat.hmi.HmiPoseProgram;

/** Development-only motion capture. Measures arm, item and two-handed mounts at several display rates. */
public class CapturePoses {

    private static final String[] MOTIONS = { "rest", "attack", "alternate", "mine", "mine_alternate", "equip", "lower",
        "use", "inspect", "climb" };
    private final int hz;
    private final float dt;
    private final HmiPoseProgram program;
    private final LuaTable env;
    private final String id;
    private final Matrix4f[] joints = new Matrix4f[13];
    private boolean support;
    private int frame;

    CapturePoses(HmiPoseProgram.Scripts scripts, String profile, int hz) throws Exception {
        this.hz = hz;
        dt = 1f / hz;
        program = new HmiPoseProgram(scripts);
        var globals = HmiPoseProgram.class.getDeclaredField("env");
        globals.setAccessible(true);
        env = (LuaTable) globals.get(program);
        var working = HmiPoseProgram.class.getDeclaredField("working");
        working.setAccessible(true);
        var matrix = (Matrix4f) working.get(program);
        Arrays.setAll(joints, i -> new Matrix4f());
        env.set("observeMount", new OneArgFunction() {

            @Override
            public LuaValue call(LuaValue index) {
                int i = index.toint();
                if (i != 10 || !support) joints[i + (support && i < 10 ? 3 : 0)].set(matrix);
                return NIL;
            }
        });
        env.set("observeSwing", new OneArgFunction() {

            @Override
            public LuaValue call(LuaValue amount) {
                if (!support) {
                    joints[11].set(matrix);
                    joints[12].translation(amount.tofloat(), 0, 0);
                }
                return NIL;
            }
        });
        var parts = profile.split("/");
        id = "minecraft:" + parts[0];
        var data = program.frame;
        data.set("mainItem", id);
        data.set("action", parts[1]);
        var tags = new LuaTable();
        for (String flag : parts[2].split(",")) tags.set(flag, LuaValue.TRUE);
        data.set("tags", tags);
        for (String key : List.of("pitch", "yaw", "speed", "vertical", "age", "swings")) data.set(key, 0);
        for (String key : List.of("water", "sneak", "climb", "inspect", "using")) data.set(key, LuaValue.FALSE);
        for (String key : List.of("block", "lantern", "throwable", "translate")) data.set(key, tags.get(key));
        data.set("ground", LuaValue.TRUE);
    }

    void sample(float progress, float dip, boolean equip, boolean alternate, boolean mine) {
        program.frame.set("age", frame++ * 20 / hz);
        // Separate the free-running respiratory phase from the event and the held-item mounts.
        env.set("a", -.04 * dt * 30);
        support = false;
        joints[10].identity();
        joints[11].identity();
        joints[12].identity();
        program.pose(true, id, progress, dip, equip, alternate, mine, dt);
        joints[0].set(program.armMatrix);
        joints[1].set(program.itemMatrix);
        joints[3].set(program.sharedPose());
        env.set("a", -.04 * dt * 30);
        support = true;
        program.pose(false, HmiPoseProgram.EMPTY, progress, 0, equip, alternate, mine, dt);
        joints[2].set(program.armMatrix);
    }

    private static final int JOINTS = 13, AXES = 12;
    private static final Map<ByteBuffer, Integer> UNIQUE_TRACKS = new HashMap<>();

    void frames(DataOutputStream out, String mode) throws IOException {
        frames(out, mode, 0);
    }

    void frames(DataOutputStream out, String mode, int previousTicks) throws IOException {
        boolean swing = List.of("attack", "alternate", "mine", "mine_alternate")
            .contains(mode);
        boolean releasing = mode.startsWith("release");
        program.frame.set("using", LuaValue.FALSE);
        for (int i = 0; i < 2 * hz; i++) sample(0, 0, false, false, false);
        if (releasing) {
            int drawFrames = Integer.parseInt(mode.substring("release".length()));
            program.frame.set("using", LuaValue.TRUE);
            for (int i = 0; i < Math.round(drawFrames * hz / 60f); i++) sample(0, 0, false, false, false);
        }
        float duration = program.swingDuration(id) * hz / 20f;
        if (previousTicks > 0) {
            boolean previousAlternate = mode.equals("attack");
            program.frame.set("swings", previousAlternate ? 1 : 2);
            for (int i = 0; i < Math.round(previousTicks * hz / 20f); i++) {
                sample(i < duration ? (float) i / duration : 0, 0, false, previousAlternate, false);
            }
        }
        int count = switch (mode) {
            case "rest" -> 1;
            case "use" -> 3 * hz + 1;
            case "climb", "inspect" -> 2 * hz + 1;
            default -> swing ? (int) Math.ceil(duration) + hz / 2 + 1 : hz + 1;
        };
        String key = previousTicks > 0 ? "chain" + previousTicks + "/" + mode : mode;
        out.writeUTF(hz == 60 ? key : key + "@" + hz);
        var tracks = new float[JOINTS][count][AXES];
        program.frame.set("using", LuaValue.valueOf(mode.equals("use")));
        program.frame.set("inspect", LuaValue.valueOf(mode.equals("inspect")));
        program.frame.set("climb", LuaValue.valueOf(mode.equals("climb")));
        if (swing) program.frame.set("swings", mode.endsWith("alternate") ? 2 : 1);
        for (int i = 0; i < count; i++) {
            sample(
                swing && i < duration ? (float) i / duration : 0,
                mode.equals("lower") ? i / (float) hz : 0,
                i == 0 && mode.equals("equip"),
                mode.endsWith("alternate"),
                mode.startsWith("mine") && i < duration);
            for (int joint = 0; joint < JOINTS; joint++) {
                for (int j = 0; j < AXES; j++) tracks[joint][i][j] = joints[joint].get(j / 3, j % 3);
            }
        }
        for (var track : tracks) writeTrack(out, track);
    }

    // Subdivide only where needed to reproduce the observed affine transforms to 0.00005.
    private static void writeTrack(DataOutputStream out, float[][] frames) throws IOException {
        var keep = new TreeSet<Integer>();
        keep.add(0);
        boolean constant = Arrays.stream(frames)
            .allMatch(v -> Arrays.equals(v, frames[0]));
        if (!constant) {
            keep.add(frames.length - 1);
            subdivide(frames, 0, frames.length - 1, keep);
        }
        var bytes = new ByteArrayOutputStream();
        var track = new DataOutputStream(bytes);
        track.writeShort(keep.size());
        for (int index : keep) {
            track.writeShort(index);
            for (float v : frames[index]) track.writeFloat(v);
        }
        byte[] data = bytes.toByteArray();
        var existing = UNIQUE_TRACKS.putIfAbsent(ByteBuffer.wrap(data), UNIQUE_TRACKS.size());
        if (existing == null) out.write(data);
        else {
            out.writeShort(0);
            out.writeInt(existing);
        }
    }

    private static void subdivide(float[][] v, int a, int b, Set<Integer> keep) {
        float worst = .00005f;
        int split = -1;
        for (int i = a + 1; i < b; i++) {
            float t = (float) (i - a) / (b - a);
            for (int j = 0; j < AXES; j++) {
                float error = Math.abs(v[i][j] - (v[a][j] + t * (v[b][j] - v[a][j])));
                if (error > worst) {
                    worst = error;
                    split = i;
                }
            }
        }
        if (split >= 0) {
            keep.add(split);
            subdivide(v, a, split, keep);
            subdivide(v, split, b, keep);
        }
    }

    private static String observed(String code, Map<Integer, Integer> mounts) {
        var lines = code.lines()
            .toArray(String[]::new);
        mounts.forEach(
            (line,
                mount) -> lines[line - 1] = (mount < 0 ? "observeSwing(swing)" : "observeMount(" + mount + ")") + "\n"
                    + lines[line - 1]);
        return String.join("\n", lines);
    }

    public static void main(String[] args) throws Exception {
        UNIQUE_TRACKS.clear();
        var release = HmiPoseProgram.Scripts.read(Path.of(args[0]));
        // The bow's support hand crosses the view edge very quickly. Measure its grip separately from
        // visibility, which the player integrates from the actual time spent drawing and releasing.
        String[] hand = release.hand()
            .lines()
            .toArray(String[]::new);
        hand[682] = "if frame.mainItem ~= 'minecraft:bow' then " + hand[682] + " end";
        // Matrix probes immediately before the look, breathing, gait and held-item response on the pinned release.
        var scripts = new HmiPoseProgram.Scripts(
            observed(String.join("\n", hand), Map.of(593, 4, 677, 5, 693, 6, 1017, -1)),
            release.arm(),
            observed(release.item(), Map.of(342, 10, 346, 10, 358, 10, 361, 10, 366, 10, 373, 10, 380, 10)),
            release.model(),
            release.assets());
        var profiles = new ArrayList<>(
            List.of(
                "air/none/-",
                "stick/none/translate",
                "generic_handheld/none/-",
                "iron_sword/block/swords",
                "iron_pickaxe/none/pickaxes",
                "iron_axe/none/axes",
                "iron_hoe/none/hoes",
                "iron_shovel/none/shovels",
                "stone/none/block,translate",
                "torch/none/block,translate",
                "bow/bow/-",
                "fishing_rod/none/-",
                "carrot_on_a_stick/none/-",
                "apple/eat/translate",
                "potion/drink/translate",
                "splash_potion/drink/throwable,translate",
                "snowball/none/throwable,translate",
                "filled_map/none/translate",
                "iron_ingot/none/translate",
                "shears/none/translate",
                "flint_and_steel/none/translate",
                "oak_sign/none/signs,translate",
                "oak_door/none/doors,translate",
                "bed/none/beds,translate",
                "skull/none/skulls,translate",
                "music_disc_13/none/music_discs,translate",
                "gold_nugget/none/nuggets,translate",
                "lantern/none/lantern,block,translate",
                "slime_block/none/block,translate",
                "honey_block/none/block,translate",
                "oak_boat/none/boats,translate",
                "oak_chest_boat/none/boats,chest_boats,translate",
                "bamboo_raft/none/boats,translate",
                "bamboo_chest_raft/none/boats,chest_boats,translate",
                "bucket/none/translate",
                "water_bucket/none/translate",
                "lava_bucket/none/translate",
                "milk_bucket/drink/translate",
                "axolotl_bucket/none/translate",
                "cod_bucket/none/translate",
                "powder_snow_bucket/none/translate",
                "pufferfish_bucket/none/translate",
                "salmon_bucket/none/translate",
                "tadpole_bucket/none/translate",
                "tropical_fish_bucket/none/translate",
                "pink_petals/none/block,translate",
                "leaf_litter/none/block,translate",
                "wildflowers/none/block,translate",
                "end_crystal/none/translate"));
        for (String name : List.of(
            "bone",
            "redstone",
            "ender_eye",
            "string",
            "magma_cream",
            "slime_ball",
            "glass_bottle",
            "item_frame",
            "painting",
            "brewing_stand",
            "cauldron",
            "repeater",
            "comparator")) profiles.add(name + "/none/translate");
        profiles.add("ender_pearl/none/throwable,translate");
        profiles.add("potato/eat/translate");
        profiles.add("carrot/eat/translate");
        for (String name : List.of(
            "ladder",
            "iron_bars",
            "glass_pane",
            "tripwire_hook",
            "vine",
            "redstone_torch",
            "rail",
            "activator_rail",
            "powered_rail",
            "detector_rail",
            "hopper",
            "short_grass",
            "tall_grass")) profiles.add(name + "/none/block,translate");
        profiles.add("oak_sapling/none/saplings,block,translate");
        profiles.add("white_stained_glass_pane/none/block,translate");
        try (var out = new DataOutputStream(
            new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(Path.of(args[1])))))) {
            out.writeInt(0x47545036);
            out.writeShort(profiles.size());
            for (String profile : profiles) {
                String[] parts = profile.split("/");
                out.writeUTF(parts[0]);
                var base = new CapturePoses(scripts, profile, 60);
                base.sample(0, 0, false, false, false);
                out.writeByte(base.program.swingDuration(base.id));
                out.writeBoolean(base.program.renderAsBlock(base.id));
                boolean primary = Set
                    .of(
                        "air",
                        "iron_sword",
                        "iron_pickaxe",
                        "iron_axe",
                        "iron_hoe",
                        "iron_shovel",
                        "stone",
                        "stick",
                        "bow")
                    .contains(parts[0]);
                boolean common = Set.of("air", "iron_sword", "iron_pickaxe", "bow")
                    .contains(parts[0]);
                int[] rates = common ? new int[] { 60, 30, 45, 75, 90, 120, 144, 240 }
                    : primary ? new int[] { 60, 30, 120 } : new int[] { 60 };
                for (int rate : rates) {
                    for (String mode : MOTIONS) {
                        boolean strike = mode.equals("attack") || mode.equals("alternate") || mode.startsWith("mine");
                        if (rate != 60 && !(strike || mode.equals("use") && parts[0].equals("bow"))) continue;
                        if (mode.equals("use") && parts[1].equals("none")) continue;
                        new CapturePoses(scripts, profile, rate).frames(out, mode);
                    }
                    if (parts[0].equals("iron_sword")) {
                        for (int ticks = 2; ticks <= 18; ticks++)
                            for (String direction : List.of("attack", "alternate"))
                                new CapturePoses(scripts, profile, rate).frames(out, direction, ticks);
                    }
                    if (!parts[1].equals("none") && (rate == 60 || parts[0].equals("bow"))) {
                        var holds = new TreeSet<>(List.of(0, 3, 6, 9, 12, 15, 18, 21, 24, 30, 42, 60, 90, 120));
                        if (parts[0].equals("bow")) for (int h = 0; h <= 36; h++) holds.add(h);
                        for (int held : holds) new CapturePoses(scripts, profile, rate).frames(out, "release" + held);
                    }
                }
                out.writeUTF("");
            }
        }
    }
}
