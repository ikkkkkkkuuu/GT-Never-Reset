package com.xyp.gtnrlib.animation;

import org.joml.Quaterniond;
import org.joml.Vector3d;

import com.xyp.gtnrlib.api.animation.AnimationClip.Vec;

/** Spherical interpolation of Euler angles in degrees. Clip curves use Z-Y-X order. */
public final class RotationCurves {

    public static Vec slerp(Vec a, Vec b, double t) {
        return angles(rotation(a).slerp(rotation(b), t));
    }

    /** Interpolate orientations composed in first-person item matrix order. */
    public static Vec slerpXYZ(Vec a, Vec b, double t) {
        return angles(rotation(a, true).slerp(rotation(b, true), t), true);
    }

    /** Extend the shortest relative rotation by a multiple of its measured time interval. */
    public static Vec extrapolate(Vec start, Vec ahead, double intervals, boolean xyz) {
        var origin = rotation(start, xyz);
        var delta = origin.difference(rotation(ahead, xyz), new Quaterniond());
        if (delta.w < 0) delta.mul(-1);
        double length = Math.sqrt(delta.x * delta.x + delta.y * delta.y + delta.z * delta.z);
        if (length < 1e-12) return start;
        double angle = 2 * Math.atan2(length, delta.w) * intervals;
        origin.rotateAxis(angle, delta.x / length, delta.y / length, delta.z / length);
        return angles(origin, xyz);
    }

    public static Vec squad(Vec previous, Vec a, Vec b, Vec next, double t) {
        var l = rotation(a);
        var r = rotation(b);
        var s = control(rotation(previous), l, r);
        var u = control(l, r, rotation(next));
        return angles(
            l.slerp(r, t)
                .slerp(s.slerp(u, t), 2 * t * (1 - t)));
    }

    private static Quaterniond control(Quaterniond a, Quaterniond b, Quaterniond c) {
        var inv = new Quaterniond(b).conjugate();
        var log = log(inv.mul(a, new Quaterniond())).add(log(inv.mul(c, new Quaterniond())))
            .mul(-.25);
        double length = log.length();
        if (length < 1e-12) return new Quaterniond(b);
        double factor = Math.sin(length) / length;
        return new Quaterniond(b)
            .mul(new Quaterniond(log.x * factor, log.y * factor, log.z * factor, Math.cos(length)));
    }

    private static Vector3d log(Quaterniond q) {
        double sign = q.w < 0 ? -1 : 1;
        double n = Math.sqrt(q.x * q.x + q.y * q.y + q.z * q.z);
        double factor = n < 1e-12 ? sign : sign * Math.atan2(n, sign * q.w) / n;
        return new Vector3d(q.x * factor, q.y * factor, q.z * factor);
    }

    private static Quaterniond rotation(Vec v) {
        return new Quaterniond().rotationZYX(Math.toRadians(v.z()), Math.toRadians(v.y()), Math.toRadians(v.x()));
    }

    private static Quaterniond rotation(Vec v, boolean xyz) {
        return xyz ? new Quaterniond().rotationXYZ(Math.toRadians(v.x()), Math.toRadians(v.y()), Math.toRadians(v.z()))
            : rotation(v);
    }

    private static Vec angles(Quaterniond q) {
        return angles(q, false);
    }

    private static Vec angles(Quaterniond q, boolean xyz) {
        var e = xyz ? q.getEulerAnglesXYZ(new Vector3d()) : q.getEulerAnglesZYX(new Vector3d());
        return new Vec(Math.toDegrees(e.x), Math.toDegrees(e.y), Math.toDegrees(e.z));
    }
}
