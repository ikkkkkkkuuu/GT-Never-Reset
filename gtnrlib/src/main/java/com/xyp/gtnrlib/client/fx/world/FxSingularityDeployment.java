package com.xyp.gtnrlib.client.fx.world;

import static com.xyp.gtnrlib.animation.StructureDeploymentStyle.SINGULARITY_AFTERSHOCK_TICKS;
import static com.xyp.gtnrlib.animation.StructureDeploymentStyle.SINGULARITY_CHARGE_TICKS;
import static com.xyp.gtnrlib.animation.StructureDeploymentStyle.SINGULARITY_COLLAPSE_TICK;
import static com.xyp.gtnrlib.animation.StructureDeploymentStyle.SINGULARITY_FORM_TICK;
import static com.xyp.gtnrlib.animation.StructureDeploymentStyle.SINGULARITY_GROWTH_TICKS;
import static com.xyp.gtnrlib.animation.StructureDeploymentStyle.SINGULARITY_IMPACT_TICK;
import static com.xyp.gtnrlib.animation.StructureDeploymentStyle.SINGULARITY_LAUNCH_TICKS;
import static com.xyp.gtnrlib.utils.MathUtils.clampUnit;

import java.util.Random;

import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.animation.StructureDeployment;
import com.xyp.gtnrlib.utils.MathUtils;
import com.xyp.gtnrlib.utils.Vec3d;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class FxSingularityDeployment {

    private static final double TAU = Math.PI * 2;
    private static final int SPHERE_SLICES = 48, SPHERE_STACKS = 24, RING_SEGMENTS = 88;
    private static final int BRANCH_NODE = 9, FORK_NODE = 17, TWIG_NODE = 24, MAIN_SEGMENTS = 32;
    private static final int TRAIL_SEGMENTS = 24, TRAIL_MOTES = 20;
    private static final double DISCHARGE_DURATION = 11;
    private static final int[] DISCHARGE_PERIODS = { 17, 19, 23, 21, 27, 18, 25, 20, 29, 22, 24, 26 };
    private static final Vec3d[] SPHERE = sphereVertices();
    private static final Vec3d UP = new Vec3d(0, 1, 0);

    private final World world;
    private final StructureDeployment bounds;
    private final Vec3d center, launchOrigin;
    private final double radius, height, width, depth;
    private final long seed;
    private final Discharge[] discharges = new Discharge[DISCHARGE_PERIODS.length];
    private final Vec3d[] lightningPoints = new Vec3d[MAIN_SEGMENTS + 1];
    private final Vec3d[] lightningSides = new Vec3d[MAIN_SEGMENTS + 1];
    private final float[] lightningOpacity = new float[MAIN_SEGMENTS + 1];
    private long impactTick = -1;
    private boolean launched, started, struck;

    FxSingularityDeployment(World world, StructureDeployment bounds, StructureDeployment.Launch launch) {
        this.world = world;
        this.bounds = bounds;
        width = Math.max(1, bounds.maxX() - bounds.minX());
        height = Math.max(1, bounds.maxY() - bounds.minY());
        depth = Math.max(1, bounds.maxZ() - bounds.minZ());
        radius = Math.min(22, 1.65 + .29 * Math.sqrt(width * depth) + .13 * height);
        center = new Vec3d(bounds.centerX(), bounds.minY() + Math.max(radius + .65, height * .54), bounds.centerZ());
        launchOrigin = launch == null ? null : new Vec3d(launch.x(), launch.y(), launch.z());
        seed = MathUtils.hash(bounds.startTick(), bounds.minX(), bounds.minY(), bounds.minZ());
    }

    void impact(long tick) {
        if (impactTick < 0) impactTick = tick;
    }

    boolean hasImpact() {
        return impactTick >= 0;
    }

    double impactAge(double age) {
        return hasImpact() ? age - (impactTick - bounds.startTick()) : -1;
    }

    boolean finished(long age) {
        return hasImpact() ? impactAge(age) >= SINGULARITY_AFTERSHOCK_TICKS
            : age >= SINGULARITY_IMPACT_TICK + SINGULARITY_AFTERSHOCK_TICKS;
    }

    void tick(long age) {
        if (!launched && launchOrigin != null
            && age >= -SINGULARITY_LAUNCH_TICKS
            && age < -SINGULARITY_LAUNCH_TICKS + 4) {
            launched = true;
            world.playSound(launchOrigin.x(), launchOrigin.y(), launchOrigin.z(), "random.bow", .70f, .56f, false);
        }
        if (!started && age >= 0 && age < SINGULARITY_GROWTH_TICKS) {
            started = true;
            world.playSound(center.x(), center.y(), center.z(), "portal.trigger", .63f, .76f, false);
        }
        if (!struck && hasImpact() && impactAge(age) >= 0) {
            struck = true;
            if (impactAge(age) < 5) {
                world.playSound(center.x(), center.y(), center.z(), "random.explode", 1f, .78f, false);
                world.playSound(center.x(), bounds.minY(), center.z(), "random.orb", .48f, .61f, false);
            }
        }
    }

    void draw(double age, double cameraX, double cameraY, double cameraZ) {
        Vec3d camera = FxCamera.position();
        if (age < 0) {
            if (launchOrigin != null && age >= -SINGULARITY_LAUNCH_TICKS)
                drawProjectile(age, camera, cameraX, cameraY, cameraZ);
            return;
        }
        double impactAge = impactAge(age);
        if (impactAge >= SINGULARITY_AFTERSHOCK_TICKS
            || !FxCamera.inView(center, Math.max(Math.hypot(width, depth) * .75, height * .65) + radius * 3.85)) return;

        prepareEffects();
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(center.x() - cameraX, center.y() - cameraY, center.z() - cameraZ);
            Vec3d viewer = camera.subtract(center);
            if (impactAge >= 0) {
                drawRelease(impactAge, viewer);
            } else if (age >= SINGULARITY_FORM_TICK) {
                double formAge = age - SINGULARITY_FORM_TICK;
                double charge = clampUnit(formAge / (SINGULARITY_IMPACT_TICK - SINGULARITY_FORM_TICK));
                drawSparks(formAge);
                drawDischarges(age, 0, viewer, .58 + .19 * charge, true);
            } else {
                double contraction = collapse(age);
                double aperture = aperture(age, contraction);
                if (aperture > .055) drawHorizon(age, aperture, viewer);
                if (age <= SINGULARITY_GROWTH_TICKS && aperture <= .055) {
                    double charge = smooth(age / SINGULARITY_CHARGE_TICKS);
                    drawPinprick(viewer, radius * (.08 + .14 * charge), .96f);
                    drawDischarges(age, 0, viewer, .75 + .25 * charge, false);
                }
                if (contraction > 0) drawCollapseStar(viewer, aperture, contraction);
            }
        } finally {
            GL11.glPopMatrix();
        }
    }

    void drawForegroundStar(double age, double cameraX, double cameraY, double cameraZ) {
        if (age < SINGULARITY_FORM_TICK || !FxCamera.inView(center, radius * 1.55)) return;
        double elapsed = impactAge(age);
        if (elapsed >= 9) return;
        boolean released = elapsed >= 0;
        double strength;
        double size;
        if (released) {
            double flash = 1 - elapsed / 9;
            strength = flash * flash;
            size = radius * (.285 + .18 * (1 - flash));
        } else {
            double charge = clampUnit(
                (age - SINGULARITY_FORM_TICK) / (SINGULARITY_IMPACT_TICK - SINGULARITY_FORM_TICK));
            size = radius * (.191 + .091 * charge + .012 * Math.sin(age * .72));
            strength = .87;
        }
        Vec3d viewer = FxCamera.position()
            .subtract(center);
        double range = viewer.length();
        if (range < .10) return;
        Vec3d direction = viewer.scale(1 / range);
        double shift = foregroundOffset(direction, range);
        Vec3d location = center.add(direction.scale(shift));
        // A change in ray depth must not change the on-screen position or apparent size.
        size *= 1 - shift / range;
        prepareEffects();
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(location.x() - cameraX, location.y() - cameraY, location.z() - cameraZ);
            drawPinprick(viewer, size, (float) strength);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private double foregroundOffset(Vec3d towardViewer, double viewRange) {
        if (center.y() >= bounds.maxY()) return 0;
        double exit = Math.min(
            axisExit(center.x(), towardViewer.x(), bounds.minX(), bounds.maxX()),
            Math.min(
                axisExit(center.y(), towardViewer.y(), bounds.minY(), bounds.maxY()),
                axisExit(center.z(), towardViewer.z(), bounds.minZ(), bounds.maxZ())));
        return exit < viewRange ? Math.min(exit + .12, viewRange * .94) : 0;
    }

    private static double axisExit(double position, double direction, int low, int high) {
        if (Math.abs(direction) < 1e-6) return Double.POSITIVE_INFINITY;
        return ((direction > 0 ? high : low) - position) / direction;
    }

    private void drawProjectile(double age, Vec3d camera, double cameraX, double cameraY, double cameraZ) {
        double time = 1 + age / SINGULARITY_LAUNCH_TICKS;
        Vec3d travel = center.subtract(launchOrigin);
        double distance = travel.length();
        if (distance < .01) return;
        Vec3d direction = travel.scale(1 / distance);
        double moved = distance * time * (1.075 - .075 * time);
        Vec3d head = launchOrigin.add(direction.scale(moved));
        double tailLength = Math.min(moved, Math.min(19, distance * .42));
        double size = (.25 + Math.min(.21, radius * .026)) * (.21 + .79 * smooth(time * 5));
        Vec3d middle = head.add(direction.scale(-tailLength / 2));
        if (!FxCamera.inView(middle, tailLength / 2 + size * 3.5)) return;

        Vec3d towardViewer = camera.subtract(head);
        double range = towardViewer.length();
        Vec3d view = range < .01 ? FxCamera.forward()
            .scale(-1) : towardViewer.scale(1 / range);
        Vec3d side = direction.cross(view)
            .normalize();
        if (side.length() < .2) side = Vec3d.rightOf(direction);
        Vec3d up = side.cross(direction)
            .normalize();
        prepareEffects();
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(head.x() - cameraX, head.y() - cameraY, head.z() - cameraZ);
            if (range > size * 1.10) solidSphere(size, .036f, .012f, .072f);
            additive();
            if (tailLength > .04) {
                projectileTrail(direction, side, up, tailLength, size, time);
                projectileMotes(direction, side, up, tailLength, size, time);
            }
            Vec3d right = Vec3d.rightOf(view);
            Vec3d vertical = right.cross(view)
                .normalize();
            double depth = Math.min(size * .80, size * size / Math.max(size, range));
            Vec3d edge = view.scale(depth + size * .03);
            double apparent = Math.sqrt(size * size - depth * depth);
            horizonRing(age, edge, right, vertical, apparent, 1.06, 2.75, .55f, .14f, 1, .82f, 0);
            horizonRing(age, edge, right, vertical, apparent, 1.02, 1.35, .82f, .38f, 1, .94f, .16f);
            horizonRing(age, edge, right, vertical, apparent, 1.01, 1.13, .91f, .74f, 1, 1, .32f);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static void projectileTrail(Vec3d axis, Vec3d side, Vec3d up, double length, double size, double time) {
        for (int strand = 0; strand < 3; strand++) {
            for (int edge = -1; edge <= 1; edge += 2) {
                GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
                for (int sample = 0; sample <= TRAIL_SEGMENTS; sample++) {
                    double t = sample / (double) TRAIL_SEGMENTS;
                    double angle = strand * TAU / 3 + t * 9 - time * 17;
                    double curl = size * Math.sin(t * Math.PI) * (strand == 0 ? .25 : .94);
                    Vec3d point = axis.scale(-length * t)
                        .add(side.scale(Math.cos(angle) * curl))
                        .add(up.scale(Math.sin(angle) * curl));
                    double thickness = size * (1 - t) * (strand == 0 ? 1.06 : .29);
                    float strength = (float) ((1 - t) * (strand == 0 ? .62 : .86));
                    GL11.glColor4f(strand == 0 ? .58f : .83f, strand == 0 ? .15f : .50f, 1, strength);
                    vertex(point, 1);
                    GL11.glColor4f(.47f, .10f, 1, 0);
                    vertex(point.add(side.scale(edge * thickness)), 1);
                }
                GL11.glEnd();
            }
        }
    }

    private void projectileMotes(Vec3d axis, Vec3d side, Vec3d up, double length, double size, double time) {
        GL11.glPointSize(3);
        GL11.glBegin(GL11.GL_POINTS);
        for (int i = 0; i < TRAIL_MOTES; i++) {
            double t = MathUtils.hashUnit(seed, i, 19, 0) + time * 2.2;
            t -= Math.floor(t);
            double a = TAU * MathUtils.hashUnit(seed, i, 21, 0) - t * 4;
            double drift = size * Math.sin(t * Math.PI) * (1.12 + MathUtils.hashUnit(seed, i, 22, 0));
            GL11.glColor4f(.74f, .47f, 1, (float) (.75 * (1 - t)));
            vertex(
                axis.scale(-length * t)
                    .add(side.scale(Math.cos(a) * drift))
                    .add(up.scale(Math.sin(a) * drift)),
                1);
        }
        GL11.glEnd();
    }

    private static void prepareEffects() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glShadeModel(GL11.GL_SMOOTH);
    }

    private static double collapse(double age) {
        return clampUnit((age - SINGULARITY_COLLAPSE_TICK) / (SINGULARITY_FORM_TICK - SINGULARITY_COLLAPSE_TICK));
    }

    private static double aperture(double age, double collapse) {
        double growth = smooth(
            (age - SINGULARITY_CHARGE_TICKS) / (SINGULARITY_GROWTH_TICKS - SINGULARITY_CHARGE_TICKS));
        return growth * Math.pow(1 - collapse * collapse, 2);
    }

    private void drawCollapseStar(Vec3d viewer, double aperture, double contraction) {
        double size = radius * (.067 + .136 * smooth(contraction));
        float intensity = (float) (.89 * smooth(contraction * 3.8));
        double distance = viewer.length();
        double surface = radius * aperture;
        if (aperture <= .055 || distance < surface * 1.08) {
            drawPinprick(viewer, size, intensity);
            return;
        }
        // Offset along the sightline just enough to put the star on the visible face. Its centre stays fixed on screen.
        Vec3d foreground = viewer.scale(surface * 1.013 / distance);
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(foreground.x(), foreground.y(), foreground.z());
            drawPinprick(viewer, size, intensity);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private void drawHorizon(double age, double aperture, Vec3d viewer) {
        double sphereRadius = radius * aperture;
        double distance = viewer.length();
        if (distance < sphereRadius * 1.08) {
            drawDischarges(age, 0, viewer, .85, false);
            return;
        }
        solidSphere(sphereRadius, .012f, .018f, .033f);

        additive();
        Vec3d axis = viewer.scale(1 / distance);
        Vec3d right = Vec3d.rightOf(axis);
        Vec3d up = right.cross(axis)
            .normalize();
        double depth = sphereRadius * sphereRadius / distance;
        double apparentRadius = Math.sqrt(sphereRadius * sphereRadius - depth * depth);
        Vec3d limb = axis.scale(depth + sphereRadius * .020);
        horizonRing(age, limb, right, up, apparentRadius, .94, 1.15, .10f, .42f, 1, .42f, .11f);
        horizonRing(age, limb, right, up, apparentRadius, .985, 1.074, .27f, .66f, 1, .88f, .12f);
        horizonRing(age, limb, right, up, apparentRadius, 1, 1.032, .71f, .86f, 1, .96f, .12f);
        drawCorona(age, limb, right, up, apparentRadius);
        drawDischarges(age, aperture, viewer, .76 + .24 * aperture, false);
    }

    private static void solidSphere(double size, float red, float green, float blue) {
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        GL11.glColor4f(red, green, blue, 1);
        for (int row = 0; row < SPHERE_STACKS; row++) {
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            for (int column = 0; column <= SPHERE_SLICES; column++) {
                int index = row * (SPHERE_SLICES + 1) + column;
                vertex(SPHERE[index + SPHERE_SLICES + 1], size);
                vertex(SPHERE[index], size);
            }
            GL11.glEnd();
        }
    }

    private static void horizonRing(double age, Vec3d center, Vec3d right, Vec3d up, double radius, double inner,
        double outer, float red, float green, float blue, float intensity, float outerLight) {
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= RING_SEGMENTS; i++) {
            double angle = TAU * i / RING_SEGMENTS;
            double distortion = .009 * Math.sin(5 * angle + age * .28) + .004 * Math.sin(13 * angle - age * .53);
            double light = .73 + .16 * Math.sin(3 * angle - age * .17) + .11 * Math.sin(9 * angle + age * .51);
            float strength = (float) (intensity * light);
            double x = Math.cos(angle), y = Math.sin(angle);
            GL11.glColor4f(red, green, blue, strength);
            planeVertex(center, right, up, x, y, radius * (inner + distortion));
            GL11.glColor4f(red, green, blue, strength * outerLight);
            planeVertex(center, right, up, x, y, radius * (outer + distortion));
        }
        GL11.glEnd();
    }

    private static void drawCorona(double age, Vec3d center, Vec3d right, Vec3d up, double radius) {
        for (int layer = 0; layer < 3; layer++) {
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            for (int i = 0; i <= RING_SEGMENTS; i++) {
                double angle = TAU * i / RING_SEGMENTS;
                double turbulence = .52 + .22 * Math.sin(3 * angle - age * .12 + layer)
                    + .17 * Math.sin(7 * angle + age * .20 - layer * 2)
                    + .09 * Math.sin(16 * angle - age * .43);
                double inner = 1.004 + .012 * Math.sin(11 * angle + age * .29);
                double extent = .105 + (.13 + .068 * layer) * turbulence;
                float intensity = (float) ((.27 - .061 * layer) * (.56 + .44 * turbulence));
                GL11.glColor4f(.16f, .53f, 1, intensity);
                planeVertex(center, right, up, Math.cos(angle), Math.sin(angle), radius * inner);
                double curl = angle + (.035 + layer * .02) * turbulence;
                GL11.glColor4f(.14f, .40f, 1, 0);
                planeVertex(center, right, up, Math.cos(curl), Math.sin(curl), radius * (inner + extent));
            }
            GL11.glEnd();
        }
    }

    private void drawDischarges(double age, double aperture, Vec3d viewer, double intensity, boolean forming) {
        additive();
        Vec3d view = viewer.length() < .01 ? FxCamera.forward()
            .scale(-1) : viewer.normalize();
        Vec3d eye = aperture > .055 ? viewer.scale(1 / radius) : null;
        for (int lane = 0; lane < discharges.length; lane++) {
            int period = DISCHARGE_PERIODS[lane];
            double time = age + lane * 7 + (seed & 3);
            int cycle = (int) (time / period);
            double phase = time - cycle * period;
            if (phase >= DISCHARGE_DURATION) continue;
            Discharge bolt = discharges[lane];
            if (bolt == null || bolt.cycle != cycle || bolt.forming != forming) {
                bolt = discharge(lane, cycle, forming);
                discharges[lane] = bolt;
            }
            double pulse = Math.sin(Math.PI * phase / DISCHARGE_DURATION);
            float light = (float) (Math.sqrt(pulse) * intensity * (.83 + .17 * Math.sin(age * 3.1 + lane)));
            if (light <= .012f) continue;
            Vec3d shift = bolt.root.scale(aperture - 1);
            lightningPath(bolt.main, shift, view, eye, aperture, 0, .013, .91f, .70f, light);
            lightningPath(bolt.branch, shift, view, eye, aperture, BRANCH_NODE, .007, .76f, .43f, light * .78f);
            lightningPath(bolt.fork, shift, view, eye, aperture, FORK_NODE, .006, .70f, .36f, light * .68f);
            lightningPath(bolt.twig, shift, view, eye, aperture, TWIG_NODE, .004, .67f, .33f, light * .56f);
        }
    }

    private void lightningPath(Vec3d[] path, Vec3d shift, Vec3d view, Vec3d eye, double aperture, int anchor,
        double width, float red, float green, float light) {
        // Three strips share the deformed points and perspective cut. The violet glow falls off on both sides.
        int count = path.length;
        for (int i = 0; i < count; i++) {
            double t = (double) i / (count - 1);
            double attachment = (1 - t) * (1 - (double) anchor / MAIN_SEGMENTS);
            Vec3d here = path[i].add(shift.scale(attachment * attachment));
            lightningPoints[i] = here;
            Vec3d tangent = path[Math.min(i + 1, count - 1)].subtract(path[Math.max(0, i - 1)]);
            lightningSides[i] = tangent.cross(view)
                .normalize()
                .scale(width * (1 - .72 * t));
            lightningOpacity[i] = (float) (light * (1 - .69 * t)) * outsideHorizon(here, eye, aperture);
        }
        for (int pass = -1; pass <= 1; pass++) {
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            for (int i = 0; i < count; i++) {
                Vec3d here = lightningPoints[i], side = lightningSides[i];
                if (pass == 0) {
                    GL11.glColor4f(red, green, 1, lightningOpacity[i]);
                    vertex(here.subtract(side), radius);
                    vertex(here.add(side), radius);
                } else {
                    GL11.glColor4f(red * .65f, green * .34f, 1, lightningOpacity[i] * .52f);
                    vertex(here, radius);
                    GL11.glColor4f(.43f, .11f, 1, 0);
                    vertex(here.add(side.scale(pass * 4.25)), radius);
                }
            }
            GL11.glEnd();
        }
    }

    private static float outsideHorizon(Vec3d point, Vec3d eye, double aperture) {
        if (eye == null) return 1;
        // Distance from the singularity centre to the view ray through this point, in full-core radii.
        Vec3d ray = point.subtract(eye);
        double near = -ray.dot(eye);
        if (near <= 0) return 1;
        double base = eye.dot(eye) - near * near / Math.max(1e-7, ray.dot(ray));
        double normalized = Math.sqrt(Math.max(0, base)) / aperture;
        return (float) smooth((normalized - 1.003) * 12);
    }

    private Discharge discharge(int lane, int cycle, boolean forming) {
        Random random = new Random(seed + lane * 8191L + cycle * 65537L);
        double angle = TAU * (lane + .86 * random.nextDouble()) / discharges.length;
        Vec3d radial = new Vec3d(Math.cos(angle), 0, Math.sin(angle));
        Vec3d tangent = radial.cross(UP);
        Vec3d root = forming ? Vec3d.ZERO
            : radial.add(0, -.39 + .78 * random.nextDouble(), 0)
                .normalize()
                .scale(1.017);
        Vec3d end;
        boolean groundStrike = lane % 3 != 2;
        if (forming) {
            // Reach the outside of the materializing structure while retaining the same fixed source.
            double y = bounds.minY() + height * (.1 + random.nextDouble() * .77) - center.y();
            double spread = .45 + random.nextDouble() * .18;
            end = new Vec3d(radial.x() * width * spread, y, radial.z() * depth * spread).scale(1 / radius);
        } else {
            double reach = 1.44 + .93 * random.nextDouble();
            double x = center.x() + radius * reach * radial.x(), z = center.z() + radius * reach * radial.z();
            double endY = groundStrike ? ground(x, z) : center.y() + radius * (.34 + random.nextDouble() * 1.01);
            end = radial.scale(reach)
                .add(0, (endY - center.y()) / radius, 0);
        }
        Vec3d[] main = crack(root, end, MAIN_SEGMENTS + 1, forming ? .34 : .57, random);
        double forkRise = groundStrike ? -.23 : .27;
        Vec3d branchEnd = main[BRANCH_NODE].add(tangent.scale(.36 + random.nextDouble() * .35))
            .add(radial.scale(.39))
            .add(0, forkRise, 0);
        Vec3d forkEnd = main[FORK_NODE].subtract(tangent.scale(.26 + random.nextDouble() * .25))
            .add(radial.scale(.31))
            .add(0, forkRise * .67, 0);
        Vec3d twigEnd = main[TWIG_NODE].add(tangent.scale(.21 + random.nextDouble() * .17))
            .add(radial.scale(.22))
            .add(0, forkRise * .37, 0);
        return new Discharge(
            cycle,
            forming,
            root,
            main,
            crack(main[BRANCH_NODE], branchEnd, 17, .18, random),
            crack(main[FORK_NODE], forkEnd, 9, .13, random),
            crack(main[TWIG_NODE], twigEnd, 9, .09, random));
    }

    private double ground(double x, double z) {
        int blockX = (int) Math.floor(x), blockZ = (int) Math.floor(z);
        if (!world.blockExists(blockX, bounds.minY(), blockZ)) return bounds.minY() + .10;
        int ground = world.getHeightValue(blockX, blockZ);
        return Math.max(bounds.minY() - 1.5 * radius, Math.min(center.y() - radius * .5, ground + .12));
    }

    private static Vec3d[] crack(Vec3d start, Vec3d end, int points, double variance, Random random) {
        Vec3d[] line = new Vec3d[points];
        line[0] = start;
        line[points - 1] = end;
        double bend = (random.nextDouble() - .5) * variance * 1.22;
        Vec3d bow = Vec3d.rightOf(end.subtract(start))
            .add(0, .22, 0)
            .scale(bend);
        for (int step = (points - 1) / 2; step > 0; step /= 2) {
            for (int i = step; i < points; i += 2 * step) {
                Vec3d a = line[i - step], b = line[i + step];
                Vec3d middle = a.add(b)
                    .scale(.5);
                Vec3d normal = Vec3d.rightOf(b.subtract(a));
                Vec3d binormal = normal.cross(
                    b.subtract(a)
                        .normalize());
                line[i] = middle.add(normal.scale((random.nextDouble() - .5) * variance))
                    .add(binormal.scale((random.nextDouble() - .5) * variance));
            }
            variance *= .54;
        }
        for (int i = 1; i < points - 1; i++) {
            line[i] = line[i].add(bow.scale(Math.sin(Math.PI * i / (points - 1))));
        }
        return line;
    }

    private void drawSparks(double age) {
        additive();
        double charge = clampUnit(age / (SINGULARITY_IMPACT_TICK - SINGULARITY_FORM_TICK));
        double spanX = Math.max(1, (bounds.maxX() - bounds.minX()) * .56);
        double spanZ = Math.max(1, (bounds.maxZ() - bounds.minZ()) * .56);
        int sparks = Math.min(105, 46 + (int) (bounds.radius() * 3 + height));
        for (int layer = 0; layer < 2; layer++) {
            GL11.glPointSize(layer == 0 ? 4 : 2);
            GL11.glBegin(GL11.GL_POINTS);
            for (int i = 0; i < sparks; i++) {
                double phase = age * (.09 + .03 * MathUtils.hashUnit(seed, i, 0, 0))
                    + MathUtils.hashUnit(seed, i, 1, 0);
                phase -= Math.floor(phase);
                double angle = TAU * MathUtils.hashUnit(seed, i, 2, 0) + phase * .17;
                double drift = .80 + .64 * phase;
                double y = bounds.minY() - center.y() + height * MathUtils.hashUnit(seed, i, 3, 0) + phase * 1.5;
                float shine = (float) (Math.sin(phase * Math.PI) * (.52 + .36 * charge));
                if (layer == 0) GL11.glColor4f(.15f, .51f, 1, shine * .36f);
                else GL11.glColor4f(.72f, .89f, 1, shine);
                GL11.glVertex3d(spanX * drift * Math.cos(angle), y, spanZ * drift * Math.sin(angle));
            }
            GL11.glEnd();
        }
    }

    private void drawRelease(double age, Vec3d viewer) {
        additive();
        if (age < 19) drawImpulseShell(age, viewer);
        double progress = clampUnit(age / SINGULARITY_AFTERSHOCK_TICKS);
        double fade = (1 - progress) * (1 - progress);
        double expansion = .10 + .90 * (1 - Math.pow(1 - progress, 2));
        double radiusX = Math.max(radius * 3.8, width * .65 + radius);
        double radiusZ = Math.max(radius * 3.8, depth * .65 + radius);
        double y = bounds.minY() - center.y() + .25 + .62 * progress;
        ripple(age, radiusX * expansion, radiusZ * expansion, y, .35, (float) (fade * .44), .16f, .48f);
        ripple(age, radiusX * expansion, radiusZ * expansion, y + .11, .13, (float) (fade * .63), .31f, .65f);
        if (age >= 4) {
            double echo = expansion * .68;
            ripple(age + 6, radiusX * echo, radiusZ * echo, y + .22, .28, (float) (fade * .24), .20f, .53f);
        }
    }

    private void drawImpulseShell(double age, Vec3d viewer) {
        double progress = age / 19;
        double envelope = Math.sin(progress * Math.PI) * (1 - progress);
        double size = radius * (.42 + 2.72 * progress);
        Vec3d axis = viewer.normalize();
        for (int row = 0; row < SPHERE_STACKS; row += 2) {
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            for (int column = 0; column <= SPHERE_SLICES; column += 2) {
                int index = row * (SPHERE_SLICES + 1) + column;
                shellVertex(SPHERE[index + 2 * (SPHERE_SLICES + 1)], axis, size, age, envelope);
                shellVertex(SPHERE[index], axis, size, age, envelope);
            }
            GL11.glEnd();
        }
    }

    private static void shellVertex(Vec3d direction, Vec3d view, double size, double age, double opacity) {
        double tangent = 1 - Math.abs(direction.dot(view));
        double grain = .72 + .28 * Math.sin(direction.x() * 13 + direction.y() * 16 + direction.z() * 10 - age * .41);
        double rim = tangent * tangent * tangent;
        float alpha = (float) (opacity * grain * (.022 + .42 * rim));
        GL11.glColor4f(.21f, .57f, 1, alpha);
        vertex(direction, size * (1 + .018 * grain));
    }

    private static void ripple(double age, double radiusX, double radiusZ, double y, double width, float opacity,
        float red, float green) {
        for (int side = -1; side <= 1; side += 2) {
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            for (int i = 0; i <= RING_SEGMENTS; i++) {
                double angle = TAU * i / RING_SEGMENTS;
                double distortion = 1 + .019 * Math.sin(angle * 7 - age * .31);
                double flutter = .83 + .17 * Math.sin(angle * 9 + age * .26);
                double x = radiusX * Math.cos(angle) * distortion, z = radiusZ * Math.sin(angle) * distortion;
                float alpha = (float) (opacity * flutter);
                GL11.glColor4f(red, green, 1, alpha);
                GL11.glVertex3d(x, y, z);
                double reach = 1 + side * width * flutter * (side < 0 ? 1 : .54);
                GL11.glColor4f(red, green, 1, 0);
                GL11.glVertex3d(x * reach, y + .20 + width * .75, z * reach);
            }
            GL11.glEnd();
        }
    }

    private static void drawPinprick(Vec3d viewer, double size, float intensity) {
        additive();
        Vec3d axis = viewer.normalize();
        Vec3d right = Vec3d.rightOf(axis);
        Vec3d up = right.cross(axis)
            .normalize();
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4f(.86f, .95f, 1, intensity);
        GL11.glVertex3d(0, 0, 0);
        for (int i = 0; i <= 32; i++) {
            double angle = TAU * i / 32;
            double flare = i % 16 == 8 ? 3.20 : i % 16 == 0 ? 2.35 : 1;
            GL11.glColor4f(.18f, .54f, 1, 0);
            planeVertex(Vec3d.ZERO, right, up, Math.cos(angle), Math.sin(angle), size * flare);
        }
        GL11.glEnd();
    }

    private static void additive() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);
    }

    private static void vertex(Vec3d p, double size) {
        GL11.glVertex3d(p.x() * size, p.y() * size, p.z() * size);
    }

    private static void planeVertex(Vec3d center, Vec3d right, Vec3d up, double x, double y, double radius) {
        GL11.glVertex3d(
            center.x() + radius * (x * right.x() + y * up.x()),
            center.y() + radius * (x * right.y() + y * up.y()),
            center.z() + radius * (x * right.z() + y * up.z()));
    }

    private static Vec3d[] sphereVertices() {
        Vec3d[] vertices = new Vec3d[(SPHERE_SLICES + 1) * (SPHERE_STACKS + 1)];
        for (int row = 0; row <= SPHERE_STACKS; row++) {
            double angle = Math.PI * row / SPHERE_STACKS;
            double ring = Math.sin(angle), y = Math.cos(angle);
            for (int column = 0; column <= SPHERE_SLICES; column++) {
                double azimuth = TAU * column / SPHERE_SLICES;
                vertices[row * (SPHERE_SLICES + 1)
                    + column] = new Vec3d(ring * Math.cos(azimuth), y, ring * Math.sin(azimuth));
            }
        }
        return vertices;
    }

    private static double smooth(double t) {
        t = clampUnit(t);
        return t * t * (3 - 2 * t);
    }

    private record Discharge(int cycle, boolean forming, Vec3d root, Vec3d[] main, Vec3d[] branch, Vec3d[] fork,
        Vec3d[] twig) {}
}
