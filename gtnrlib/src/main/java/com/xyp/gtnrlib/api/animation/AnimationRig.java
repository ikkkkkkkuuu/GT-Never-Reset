package com.xyp.gtnrlib.api.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.joml.Matrix4d;

import com.xyp.gtnrlib.api.animation.AnimationClip.Channel;
import com.xyp.gtnrlib.api.animation.AnimationClip.Property;
import com.xyp.gtnrlib.api.animation.AnimationClip.Vec;

/** Ordered skeleton. Rest translation is relative to the parent; rotations are Z-Y-X in degrees. */
public final class AnimationRig {

    public record Bone(String name, String parent, Vec offset, Vec rotation, Vec scale) {

        public Bone {
            Objects.requireNonNull(name);
            Objects.requireNonNull(offset);
            Objects.requireNonNull(rotation);
            Objects.requireNonNull(scale);
        }
    }

    private record Joint(Bone bone, int parent, Channel position, Channel rotation, Channel scale) {}

    private final List<Joint> joints;

    public AnimationRig(List<Bone> bones) {
        Map<String, Integer> index = new HashMap<>();
        List<Joint> built = new ArrayList<>();
        for (Bone bone : bones) {
            Integer parent = bone.parent == null ? -1 : index.getOrDefault(bone.parent, -2);
            if (parent == -2 || bone.name.isEmpty() || index.putIfAbsent(bone.name, built.size()) != null)
                throw new IllegalArgumentException("invalid bone or parent: " + bone.name);
            built.add(
                new Joint(
                    bone,
                    parent,
                    new Channel(bone.name, Property.POSITION),
                    new Channel(bone.name, Property.ROTATION),
                    new Channel(bone.name, Property.SCALE)));
        }
        joints = List.copyOf(built);
    }

    /** Writes one column-major matrix per joint to caller-owned storage. No GL context needed. */
    public void sample(AnimationPlayer player, double ahead, double[] matrices) {
        if (matrices.length / 16 < joints.size()) throw new IllegalArgumentException("pose buffer too short");
        Matrix4d matrix = new Matrix4d();
        for (int i = 0; i < joints.size(); i++) {
            Joint joint = joints.get(i);
            Bone bone = joint.bone;
            if (joint.parent < 0) matrix.identity();
            else matrix.set(matrices, 16 * joint.parent);
            Vec p = player.sample(joint.position, ahead), r = player.sample(joint.rotation, ahead),
                s = player.sample(joint.scale, ahead);
            matrix.translate(bone.offset.x() + p.x(), bone.offset.y() + p.y(), bone.offset.z() + p.z());
            rotate(matrix, bone.rotation);
            rotate(matrix, r);
            matrix.scale(bone.scale.x() * s.x(), bone.scale.y() * s.y(), bone.scale.z() * s.z())
                .get(matrices, 16 * i);
        }
    }

    private static void rotate(Matrix4d m, Vec r) {
        m.rotateZYX(Math.toRadians(r.z()), Math.toRadians(r.y()), Math.toRadians(r.x()));
    }
}
