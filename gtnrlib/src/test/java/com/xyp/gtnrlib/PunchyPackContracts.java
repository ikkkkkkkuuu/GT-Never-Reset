package com.xyp.gtnrlib;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.xyp.gtnrlib.api.animation.AnimationClip.Channel;
import com.xyp.gtnrlib.api.animation.AnimationClip.Property;
import com.xyp.gtnrlib.compat.punchy.PunchyPack;

final class PunchyPackContracts {

    static void verify() throws Exception {
        Path root = Files.createTempDirectory("gtnrlib-packs");
        try {
            Path base = root.resolve("base.zip"), high = root.resolve("override");
            String clips = "assets/minecraft/punchy/animations/a.json";
            String rules = "assets/minecraft/punchy/compat/a.json";
            String clip = "{\"animation_length\":1,\"timeline\":{\"0\":\"use_keyframes_v2;dual_handed;anim_speed_1;\"},\"bones\":{\"right_arm\":{\"rotation\":[0,\"math.sin(q.anim_time*360)\",4]}}}";
            try (var zip = new ZipOutputStream(Files.newOutputStream(base))) {
                write(zip, clips, "{\"animations\":{\"keep\":" + clip + ",\"swing\":" + clip + "}}");
                write(
                    zip,
                    rules,
                    "{\"item\":{\"SWORD\":{\"customAnimation\":[{\"type\":\"attack\",\"name\":\"swing\"},{\"type\":\"idle\",\"name\":\"keep\"}]}}}");
            }
            Files.createDirectories(
                high.resolve(clips)
                    .getParent());
            Files.createDirectories(
                high.resolve(rules)
                    .getParent());
            Files.writeString(high.resolve(clips), "{\"animations\":{\"swing\":" + clip.replace(",4]", ",8]") + "}}");
            Files.writeString(
                high.resolve(rules),
                "{\"item\":{\"SWORD\":{\"customAnimation\":[{\"type\":\"attack\",\"var_1\":\"swing\",\"var_2\":\"keep\"}]}}}");
            List<String> errors = new ArrayList<>();
            var pack = PunchyPack.load(List.of(base, high), errors::add);
            var swing = pack.select("mod:item", "SWORD", "attack", 0);
            var axis = swing.clip()
                .sample(new Channel("right_arm", Property.ROTATION), .25);
            if (!errors.isEmpty() || axis.y() != 1
                || axis.z() != 8
                || !swing.dualHanded()
                || swing.speed() != 1
                || !swing.v2()
                || pack.select("mod:item", "SWORD", "idle", 0) != pack.clip("keep")
                || pack.select("mod:item", "SWORD", "attack", 1) != pack.clip("keep"))
                throw new AssertionError("pack layering " + errors);
            Files.writeString(high.resolve(rules), "{\"item\":{},\"item\":{}}");
            var retry = PunchyPack.load(List.of(base, high), errors::add);
            if (errors.size() != 1 || retry.select("mod:item", "SWORD", "attack", 0) != retry.clip("swing"))
                throw new AssertionError("bad rule isolation");
        } finally {
            try (var files = Files.walk(root)) {
                for (Path p : files.sorted(Comparator.reverseOrder())
                    .toList()) Files.delete(p);
            }
        }
    }

    private static void write(ZipOutputStream zip, String path, String body) throws Exception {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(body.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
