package com.xyp.gtnrlib.compat.hmi;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import org.joml.Matrix4f;
import org.luaj.vm2.LuaTable;

/** Measured local poses, composed with continuous gait and spring state for one first-person target. */
final class DefaultHandPoses {

    private static final float HZ = 60;
    private static final int ARM = 0, ITEM = 1, SUPPORT = 2, MOUNT = 3, LOOK = 4, BREATH = 5, GAIT = 6, ITEM_LAG = 10,
        SWING_LAG = 11, SWING_GAIN = 12;
    private static final int JOINTS = 13;
    private static final float DEG = (float) (Math.PI / 180);
    private static Map<String, Profile> profiles;
    private final Matrix4f[] pose = matrices();
    private Curve useCurve, releaseA, releaseB;
    private int useRate = 60;
    private float releaseBlend;
    private final Matrix4f motion = new Matrix4f(), sample = new Matrix4f(), other = new Matrix4f(),
        delta = new Matrix4f();
    private final Spring height = new Spring(.27f, .1f, .82f), pitch = new Spring(.15f, .04f, .85f),
        yaw = new Spring(.15f, .04f, .85f);
    private Profile profile;
    private String id, swingKind;
    private float strokeAge;
    private int lastSwingCount;
    private Curve swing, highRateSwing;
    private float swingBlend, bowPull, bowHide;
    private boolean bowProfile;
    private float stride, gait, breath, sneak, lastPitch = Float.NaN, lastYaw;
    private float swingTime, equipTime = 2, useTime, releaseTime, inspectTime, climbTime;
    private boolean using, recovering, wasSwinging, airborneSword;

    DefaultHandPoses() throws IOException {
        if (profiles == null) profiles = load();
    }

    private static Map<String, Profile> load() throws IOException {
        var data = DefaultHandPoses.class.getResourceAsStream("default-poses.gz");
        if (data == null) throw new IOException("Missing built-in first-person poses");
        try (var in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(data)))) {
            if (in.readInt() != 0x47545036) throw new IOException("Unknown hand pose format");
            var result = new HashMap<String, Profile>();
            var shared = new java.util.ArrayList<Track>();
            int count = in.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                String name = in.readUTF();
                int duration = in.readUnsignedByte();
                boolean block = in.readBoolean();
                var clips = new HashMap<String, Curve>();
                var rates = new HashMap<String, NavigableMap<Integer, Curve>>();
                for (String key = in.readUTF(); !key.isEmpty(); key = in.readUTF()) {
                    var tracks = new Track[JOINTS];
                    for (int j = 0; j < JOINTS; j++) tracks[j] = Track.read(in, shared);
                    int split = key.indexOf('@');
                    String base = split < 0 ? key : key.substring(0, split);
                    int hz = split < 0 ? 60 : Integer.parseInt(key.substring(split + 1));
                    var curve = new Curve(tracks, hz);
                    clips.put(key, curve);
                    rates.computeIfAbsent(base, unused -> new TreeMap<>())
                        .put(hz, curve);
                }
                var inverse = matrices();
                for (int j = 0; j < inverse.length; j++) clips.get("rest")
                    .at(0, j, inverse[j])
                    .invert();
                var releases = new HashMap<Integer, NavigableMap<Integer, Curve>>();
                clips.forEach((key, value) -> {
                    if (key.startsWith("release")) releases.computeIfAbsent(value.hz, rate -> new TreeMap<>())
                        .put(
                            Integer.parseInt(
                                key.substring(7)
                                    .split("@")[0]),
                            value);
                });
                result.put(name, new Profile(duration, block, Map.copyOf(clips), inverse, releases, rates));
            }
            return Map.copyOf(result);
        }
    }

    private static Matrix4f[] matrices() {
        var result = new Matrix4f[JOINTS];
        java.util.Arrays.setAll(result, index -> new Matrix4f());
        return result;
    }

    void select(String item, LuaTable frame, boolean changed) {
        if (!changed && item.equals(id)) return;
        String key = item.startsWith("minecraft:") ? item.substring(10) : "";
        var tags = frame.get("tags");
        if (!profiles.containsKey(key)) {
            if (tags.get("saplings")
                .toboolean()) key = "oak_sapling";
            else if (key.endsWith("_stained_glass_pane")) key = "white_stained_glass_pane";
            else if (tags.get("chest_boats")
                .toboolean()) key = "oak_chest_boat";
            else if (tags.get("boats")
                .toboolean()) key = "oak_boat";
            else if (tags.get("swords")
                .toboolean()) key = "iron_sword";
            else if (tags.get("pickaxes")
                .toboolean()) key = "iron_pickaxe";
            else if (tags.get("axes")
                .toboolean()) key = "iron_axe";
            else if (tags.get("shovels")
                .toboolean()) key = "iron_shovel";
            else if (tags.get("hoes")
                .toboolean()) key = "iron_hoe";
            else if (tags.get("music_discs")
                .toboolean()) key = "music_disc_13";
            else if (tags.get("signs")
                .toboolean()) key = "oak_sign";
            else if (tags.get("doors")
                .toboolean()) key = "oak_door";
            else if (tags.get("beds")
                .toboolean()) key = "bed";
            else if (tags.get("skulls")
                .toboolean()) key = "skull";
            else if (tags.get("nuggets")
                .toboolean()) key = "gold_nugget";
            else if (frame.get("lantern")
                .toboolean()) key = "lantern";
            else if (frame.get("throwable")
                .toboolean()) key = "snowball";
            else if (frame.get("block")
                .toboolean()) key = "stone";
            else key = switch (frame.get("action")
                .tojstring()) {
                case "eat" -> "apple";
                case "drink" -> "potion";
                case "bow" -> "bow";
                case "block" -> "iron_sword";
                default -> frame.get("translate")
                    .toboolean() ? "stick" : "generic_handheld";
            };
        }
        if (!"bow".equals(key)) {
            bowPull = 0;
            bowHide = 1;
        }
        bowProfile = "bow".equals(key);
        profile = profiles.get(key);
        id = item;
    }

    int duration() {
        return profile.duration;
    }

    boolean block() {
        return profile.block;
    }

    int bowStage() {
        return !using ? -1 : useTime >= .9f ? 2 : useTime >= .65f ? 1 : 0;
    }

    void pose(boolean main, String item, float progress, float dip, boolean changed, boolean alternate, boolean mining,
        float dt, LuaTable frame, Matrix4f arm, Matrix4f held, Matrix4f shared) {
        if (!main) {
            arm.set(pose[SUPPORT]);
            return;
        }
        select(item, frame, changed);
        // Keep slow render frames on game time while integrating the damped springs in stable steps.
        int steps = Math.max(1, (int) Math.ceil(dt / .05f));
        float step = dt / steps;
        for (int i = 0; i < steps; i++) {
            float subProgress = Math.max(0, progress - step * (steps - i - 1) * 20 / duration());
            step(item, frame, subProgress, dip, changed && i == 0, alternate, mining, step);
        }
        arm.set(pose[ARM]);
        held.set(pose[ITEM]);
        shared.set(pose[MOUNT]);
    }

    private void step(String item, LuaTable frame, float progress, float dip, boolean changed, boolean alternate,
        boolean mining, float dt) {
        if (progress == 0 || changed) {
            airborneSword = !frame.get("ground")
                .toboolean() && frame.get("tags")
                    .get("swords")
                    .toboolean();
        }
        advanceSwing(
            progress,
            changed,
            alternate,
            mining || airborneSword,
            frame.get("swings")
                .toint(),
            dt);
        advanceUse(
            frame.get("using")
                .toboolean(),
            dt);
        inspectTime = ramp(
            inspectTime,
            frame.get("inspect")
                .toboolean(),
            dt,
            1.9f);
        climbTime = ramp(
            climbTime,
            frame.get("climb")
                .toboolean(),
            dt,
            1.9f);
        sampleActionLayers(item, dip);
        applyMovement(item, frame, dt);
    }

    private void advanceSwing(float progress, boolean changed, boolean alternate, boolean mining, int count, float dt) {
        if (changed) {
            equipTime = 0;
            recovering = using = wasSwinging = false;
            swing = null;
            useTime = 0;
        } else equipTime += dt;
        boolean accepted = count != lastSwingCount;
        lastSwingCount = count;
        strokeAge += dt;
        if (progress > 0 || accepted && swing != null) {
            String name = mining ? (alternate ? "mine_alternate" : "mine") : alternate ? "alternate" : "attack";
            swingTime = progress * duration() / 20;
            if (accepted || !wasSwinging || !name.equals(swingKind)) {
                String clip = name;
                if (swing != null && !mining && !name.equals(swingKind) && !swingKind.startsWith("mine")) {
                    int previousTicks = Math.round((strokeAge - swingTime) * 20);
                    String chain = "chain" + previousTicks + "/" + name;
                    if (profile.clips.containsKey(chain)) clip = chain;
                }
                strokeAge = swingTime;
                swingKind = name;
                selectSwingRate(clip, dt);
            }
        } else if (swing != null) {
            swingTime += dt;
            if (wasSwinging) swingTime = Math.max(duration() / 20f, swingTime);
            if (swingTime >= duration() / 20f + .5f) swing = null;
        }
        wasSwinging = progress > 0 || accepted && swing != null;
    }

    private void selectSwingRate(String clip, float dt) {
        var rates = profile.rates.get(clip);
        int hz = dt > 0 ? Math.round(1 / dt) : 60;
        var low = rates.floorEntry(hz);
        var high = rates.ceilingEntry(hz);
        swing = low == null ? rates.firstEntry()
            .getValue() : low.getValue();
        highRateSwing = high == null ? swing : high.getValue();
        swingBlend = swing == highRateSwing ? 0
            : Math.max(0, Math.min(1, (1f / swing.hz - dt) / (1f / swing.hz - 1f / highRateSwing.hz)));
        if (swingBlend >= .9999f) swing = highRateSwing;
        if (swingBlend <= .0001f || swing == highRateSwing) highRateSwing = null;
    }

    private void advanceUse(boolean requested, float dt) {
        boolean use = requested && profile.clips.containsKey("use");
        if (using && !use) {
            float heldFrames = useTime * HZ;
            var a = profile.releases.get(useRate)
                .floorEntry((int) heldFrames);
            var b = profile.releases.get(useRate)
                .ceilingEntry((int) Math.ceil(heldFrames));
            if (b == null) b = a;
            releaseA = a.getValue();
            releaseB = b.getValue();
            releaseBlend = a.getKey()
                .equals(b.getKey()) ? 0 : (heldFrames - a.getKey()) / (b.getKey() - a.getKey());
            releaseTime = dt - 1f / useRate;
            recovering = true;
        }
        if (use) {
            if (!using) {
                useCurve = profile.useCurve(dt);
                useRate = useCurve.hz;
            }
            useTime = using ? useTime + dt : dt;
            recovering = false;
        } else if (recovering && !using && (releaseTime += dt) >= 1) recovering = false;
        using = use;
    }

    private void sampleActionLayers(String item, float dip) {
        for (int j = 0; j < JOINTS; j++) {
            profile.clips.get("rest")
                .at(0, j, pose[j]);
            if (swing != null) {
                swing.at(swingTime, j, sample);
                if (highRateSwing != null) sample.lerp(highRateSwing.at(swingTime, j, other), swingBlend);
                append(sample, j);
            }
            sampleUse(item, j);
            if (equipTime < 1) layer("equip", equipTime, j);
            if (dip > 0) layer("lower", Math.min(1, dip), j);
            if (inspectTime > 0) layer("inspect", inspectTime, j);
            if (climbTime > 0) layer("climb", climbTime, j);
        }
    }

    private void sampleUse(String item, int j) {
        if (using) {
            // At full draw the reference bow vibrates with a 31.794 rad/s phase.
            float hold = useTime - 1f / useRate;
            if (item.equals("minecraft:bow") && hold > 2.5f)
                hold = 2.5f + (hold - 2.5f) % (float) (2 * Math.PI / 31.794);
            useCurve.at(hold, j, sample);
            if (hold < 0) {
                profile.clips.get("rest")
                    .at(0, j, other);
                sample.lerp(other, Math.min(1, -hold * useRate));
            }
            append(sample, j);
        } else if (recovering) {
            releaseA.at(releaseTime, j, sample);
            releaseB.at(releaseTime, j, other);
            sample.lerp(other, releaseBlend);
            if (releaseTime < 0) {
                useCurve.at(useTime - 1f / useRate, j, other);
                sample.lerp(other, Math.min(1, -releaseTime * useRate));
            }
            append(sample, j);
        }
    }

    private void applyMovement(String item, LuaTable frame, float dt) {
        float view = (float) frame.get("pitch")
            .todouble(),
            heading = (float) frame.get("yaw")
                .todouble();
        float speed = (float) frame.get("speed")
            .todouble(),
            vertical = (float) frame.get("vertical")
                .todouble();
        boolean walking = frame.get("ground")
            .toboolean() && speed > .08
            && Math.abs(vertical) < .08;
        float turn = Float.isNaN(lastPitch) ? 0 : (heading - lastYaw) % 360;
        if (turn > 180) turn -= 360;
        if (turn < -180) turn += 360;
        float look = Float.isNaN(lastPitch) ? 0 : view - lastPitch;
        lastPitch = view;
        lastYaw = heading;
        // Both hand passes advance the reference oscillator. Keep its 30 Hz unit and the order of the two passes.
        float step = dt * 30;
        for (int hand = 0; hand < 2; hand++) {
            int side = hand == 0 ? 1 : -1;
            gait = ramp(gait, walking, .1f * step, 1);
            if (walking) stride += speed * step;
            breath += .04f * step;
            sneak = ramp(
                sneak,
                frame.get("sneak")
                    .toboolean(),
                .1f * step,
                1);
            height.step(-vertical + .14f * sin(sneak * (float) Math.PI), step);
            float lift = height.position * (item.equals(HmiPoseProgram.EMPTY) || frame.get("block")
                .toboolean() || hand == 1 ? .7f : 1);
            int lookMount = LOOK + hand * 3, breathMount = BREATH + hand * 3, gaitMount = GAIT + hand * 3;
            motion.identity();
            delta.identity()
                .translate(0, -.08f * sneak, 0);
            pivotX(delta, (4 * sin(sneak * (float) Math.PI) - .03f * view + .1f * pitch.position) * DEG);
            composeAt(lookMount);
            float anchorX = .3f * side;
            delta.identity()
                .translate(0, .01f * sin(breath), 0)
                .translate(anchorX, -.4f, 0)
                .rotateX(1.1f * side * cos(breath) * DEG)
                .rotateY(.5f * sin(breath) * DEG)
                .rotateZ(2 * sin(breath * .3f) * DEG)
                .rotateX(-1.1f * side * DEG)
                .translate(-anchorX, .4f, 0);
            composeAt(breathMount);
            delta.identity();
            if (bowProfile && hand == 1) {
                float hide = bowVisibility(
                    frame.get("climb")
                        .toboolean(),
                    dt);
                delta.translate(0, -hide, .5f * hide);
            }
            if (hand == 1 || item.equals(HmiPoseProgram.EMPTY)) delta.translate(0, 0, .06f * lift);
            pivotX(delta, 2 * lift * DEG);
            delta.translate(0, .06f * lift, 0)
                .translate(0, -.4f, 0)
                .rotateX(1.5f * sin(stride - hand * .75f) * gait * DEG)
                .rotateY(-.5f * cos(stride * 1.5f) * gait * side * DEG)
                .rotateZ(cos(stride * 1.5f) * gait * side * DEG)
                .translate(0, .4f, 0);
            composeAt(gaitMount);
            if (hand == 0) {
                delta.rotationX(pitch.position * .35f * pose[SWING_GAIN].m30() * DEG);
                composeAt(SWING_LAG);
                pose[ARM].mulLocal(motion);
                pose[MOUNT].mulLocal(motion);
            } else pose[SUPPORT].mulLocal(motion);
            pitch.step(-22 * speed * gait + height.position * 3 + (hand == 0 ? look : 0), step);
            yaw.step(sin(stride) * 3 * gait + (hand == 0 ? turn : 0), step);
            if (hand == 0) applyItemLag(item, frame, view);
        }
    }

    private float bowVisibility(boolean climbing, float dt) {
        bowPull = ramp(bowPull, using, (using ? 2.25f : 2.1f) * dt, 1);
        boolean revealing = using || climbing;
        if (revealing) bowHide = ramp(bowHide, false, 2.4f * dt, 1);
        if (bowPull > 0) {
            float x = bowPull < .5f ? 2 * bowPull : 2 * bowPull - 2;
            float back = 1.70158f * 1.525f;
            float eased = bowPull < .5f ? x * x * ((back + 1) * x - back) / 2
                : (x * x * ((back + 1) * x + back) + 2) / 2;
            bowHide = bowPull == 1 ? -.1f : 1 - eased;
        }
        float visible = ramp(bowHide, !revealing, 2.4f * dt, 1);
        if (bowPull == 0) bowHide = visible;
        if (visible == 0 || visible == 1) return visible;
        return visible < .5f ? (float) Math.pow(2, 20 * visible - 11) : 1 - (float) Math.pow(2, 9 - 20 * visible);
    }

    private void applyItemLag(String item, LuaTable frame, float view) {
        delta.identity();
        if (frame.get("lantern")
            .toboolean()) {
            delta.translate(0, .4f, 0)
                .rotateX((Math.max(-20, view * .4f) + pitch.position) * DEG)
                .rotateZ(-yaw.position * DEG)
                .translate(0, -.4f, 0);
        } else if (item.equals("minecraft:end_crystal")) {
            float pulse = 1 + .01f * sin(breath * 15);
            delta.scale(pulse, pulse, 1 + .01f * sin(breath * 8))
                .translate(-yaw.position / 150f, .25f + .03f * sin(breath * 2) + pitch.position / 150f, 0)
                .rotateZ(5 * sin(breath) * DEG)
                .translate(0, -.25f, 0);
        } else if (item.endsWith("pink_petals") || item.endsWith("wildflowers") || item.endsWith("leaf_litter")) {
            delta.translate(0, -.13f, 0)
                .rotateX((Math.max(-20, view * .4f) + pitch.position + yaw.position * .5f) * DEG)
                .translate(0, .13f, 0);
        } else if (!frame.get("block")
            .toboolean()) {
                boolean axe = frame.get("tags")
                    .get("axes")
                    .toboolean();
                if (!item.equals(HmiPoseProgram.EMPTY) && frame.get("action")
                    .tojstring()
                    .equals("none")) {
                    float y = axe ? -.3f : -.4f;
                    delta.translate(.3f, y, 0)
                        .rotateX(((axe ? .15f : .05f) * pitch.position) * DEG)
                        .translate(-.3f, -y, 0);
                }
                delta.translate(0, -.2f, 0)
                    .rotateX(((axe ? .2f : .1f) * pitch.position - (axe ? .05f : .025f) * view) * DEG)
                    .translate(0, .2f, 0);
            }
        composeAt(ITEM_LAG);
        pose[ITEM].mulLocal(motion);
    }

    void modelPart(String item, Matrix4f out) {
        out.identity();
        if (item.endsWith("_chest_boat") || item.endsWith("_chest_raft")) {
            out.translate(0, Math.max(0, Math.min(1, height.position * .1f)) * 16, 0)
                .rotateX(Math.max(0, pitch.position * 1.35f) * DEG)
                .rotateZ(-yaw.position * .12f * DEG);
        } else if (item.equals("minecraft:water_bucket") || item.equals("minecraft:lava_bucket")
            || item.equals("minecraft:milk_bucket")) {
                out.rotateX(Math.max(-5, Math.min(5, -pitch.position * .6f)) * DEG)
                    .rotateZ(Math.max(-5, Math.min(5, yaw.position * .6f)) * DEG);
            } else if (item.endsWith("_bucket")) {
                out.translate(0, .25f * height.position, 0)
                    .rotateX(Math.max(-20, Math.min(20, height.position * 15 + pitch.position * .25f)) * DEG)
                    .rotateZ(-yaw.position * .23f * DEG);
            }
    }

    private void composeAt(int joint) {
        // Change basis into the measured mount. Actions can rotate that mount while the player is moving.
        var mount = pose[joint];
        motion.mul(mount)
            .mul(delta)
            .mul(mount.invert(other));
    }

    private void layer(String name, float t, int j) {
        profile.clips.get(name)
            .at(t, j, sample);
        append(sample, j);
    }

    private void append(Matrix4f value, int j) {
        pose[j].mul(profile.inverse[j])
            .mul(value);
    }

    private static void pivotX(Matrix4f m, float radians) {
        m.translate(0, -.4f, 0)
            .rotateX(radians)
            .translate(0, .4f, 0);
    }

    private static float ramp(float value, boolean up, float step, float max) {
        return Math.max(0, Math.min(max, value + (up ? step : -step)));
    }

    private static float sin(float x) {
        return (float) Math.sin(x);
    }

    private static float cos(float x) {
        return (float) Math.cos(x);
    }

    private record Profile(int duration, boolean block, Map<String, Curve> clips, Matrix4f[] inverse,
        Map<Integer, NavigableMap<Integer, Curve>> releases, Map<String, NavigableMap<Integer, Curve>> rates) {

        Curve useCurve(float dt) {
            var available = rates.get("use");
            int hz = dt > 0 ? Math.round(1 / dt) : 60;
            var low = available.floorEntry(hz);
            var high = available.ceilingEntry(hz);
            if (low == null) return high.getValue();
            if (high == null) return low.getValue();
            return Math.abs(dt - 1f / low.getKey()) < Math.abs(dt - 1f / high.getKey()) ? low.getValue()
                : high.getValue();
        }
    }

    private record Curve(Track[] joints, int hz) {

        Matrix4f at(float seconds, int joint, Matrix4f result) {
            return joints[joint].at(Math.max(0, seconds) * hz, result);
        }
    }

    private record Track(int[] times, float[] values) {

        static Track read(DataInputStream in, java.util.List<Track> shared) throws IOException {
            int count = in.readUnsignedShort();
            if (count == 0) return shared.get(in.readInt());
            var times = new int[count];
            var values = new float[count * 12];
            for (int i = 0; i < count; i++) {
                times[i] = in.readUnsignedShort();
                for (int j = 0; j < 12; j++) values[i * 12 + j] = in.readFloat();
            }
            var track = new Track(times, values);
            shared.add(track);
            return track;
        }

        Matrix4f at(float frame, Matrix4f result) {
            int last = times.length - 1;
            int end = java.util.Arrays.binarySearch(times, (int) Math.ceil(frame));
            if (end < 0) end = Math.min(-end - 1, last);
            int start = Math.max(0, end - 1);
            float t = start == end ? 0 : Math.min(1, (frame - times[start]) / (times[end] - times[start]));
            for (int i = 0; i < 12; i++) {
                float a = values[start * 12 + i];
                result.set(i / 3, i % 3, a + t * (values[end * 12 + i] - a));
            }
            return result;
        }
    }

    private static final class Spring {

        private final float drive, stiffness, damping;
        private float position, velocity;

        Spring(float drive, float stiffness, float damping) {
            this.drive = drive;
            this.stiffness = stiffness;
            this.damping = damping;
        }

        void step(float target, float time) {
            velocity = (velocity + (drive * target - stiffness * position) * time) * (float) Math.pow(damping, time);
            position += velocity * time;
        }
    }
}
