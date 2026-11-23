package com.xyp.gtneverreset.client.projector;

import java.nio.DoubleBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.common.packet.ProjectorDeploymentPacket.Phase;
import com.xyp.gtneverreset.common.projector.ProjectorBatch;
import com.xyp.gtneverreset.common.projector.ProjectorDeployment;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtnrlib.client.fx.world.FxBlockMesh;
import com.xyp.gtnrlib.client.fx.world.FxCamera;
import com.xyp.gtnrlib.utils.Vec3d;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ProjectorDeploymentRenderer {

    private static final int FINISH_GRACE_TICKS = 8;
    private static final long COMPILE_NANOS = 3_000_000L;
    private static final long LAYOUT_NANOS = 2_000_000L;
    private static final double CIRCLE_DEGREES_PER_TICK = 2.4;
    private static final Map<Long, Presentation> ACTIVE = new LinkedHashMap<>();
    private static final DoubleBuffer PORTAL_PLANE = BufferUtils.createDoubleBuffer(4);

    private ProjectorDeploymentRenderer() {}

    public static void receive(Minecraft minecraft, long id, int dimension, Phase phase, ProjectorDeployment animation,
        NBTTagCompound data, boolean batch) {
        var connection = minecraft.getNetHandler();
        var world = minecraft.theWorld;
        if (connection == null || world == null) return;
        minecraft.func_152344_a(() -> {
            if (minecraft.getNetHandler() != connection || minecraft.theWorld != world
                || minecraft.thePlayer == null
                || minecraft.thePlayer.dimension != dimension) return;
            if (phase == Phase.START) {
                ProjectorPlan plan = new ProjectorPlan();
                plan.readFromNBT(data);
                start(id, world, animation, plan, batch);
            } else {
                update(id, phase);
            }
        });
    }

    private static void start(long id, World world, ProjectorDeployment animation, ProjectorPlan plan, boolean batch) {
        ACTIVE.putIfAbsent(id, new Presentation(world, animation, plan, batch));
    }

    private static void update(long id, Phase phase) {
        Presentation presentation = ACTIVE.get(id);
        if (presentation == null) return;
        long now = presentation.world.getTotalWorldTime();
        switch (phase) {
            case IMPACT -> {
                if (presentation.impact == null) presentation.impact = new ProjectorDeploymentImpact(
                    presentation.world,
                    presentation.animation,
                    now);
            }
            case COMPLETE -> presentation.finishedTick = now;
            case CANCEL -> {
                if (presentation.impact == null) ACTIVE.remove(id).mesh.release();
                else {
                    presentation.finishedTick = now;
                    presentation.unavailable = true;
                    presentation.model = null;
                    presentation.mesh.release();
                }
            }
            case START -> {}
        }
    }

    public static void shakeCamera(float partialTicks) {
        if (ACTIVE.isEmpty()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        Entity camera = minecraft.renderViewEntity;
        if (camera == null) return;
        for (Presentation presentation : ACTIVE.values()) {
            if (presentation.world == minecraft.theWorld && presentation.impact != null)
                presentation.impact.shake(camera, partialTicks);
        }
    }

    static void clear() {
        for (Presentation presentation : ACTIVE.values()) presentation.mesh.release();
        ACTIVE.clear();
    }

    static boolean isActive() {
        return !ACTIVE.isEmpty();
    }

    static boolean covers(ProjectorPlan selected) {
        for (Presentation presentation : ACTIVE.values()) {
            ProjectorPlan plan = presentation.plan;
            if (selected.machine == plan.machine && selected.facing == plan.facing
                && ProjectorClient.x == plan.x
                && ProjectorClient.y == plan.y
                && ProjectorClient.z == plan.z) return true;
        }
        return false;
    }

    static void tick(World world) {
        var iterator = ACTIVE.values()
            .iterator();
        while (iterator.hasNext()) {
            Presentation presentation = iterator.next();
            long now = world.getTotalWorldTime();
            long age = now - presentation.animation.startTick();
            if (world != presentation.world || presentation.finishedTick >= 0
                && now - presentation.finishedTick >= FINISH_GRACE_TICKS
                && age >= ProjectorDeployment.IMPACT_TICK + ProjectorDeployment.CLOSE_TICKS
                && (presentation.impact == null || presentation.impact.age() >= ProjectorDeploymentImpact.TAIL_TICKS)) {
                presentation.mesh.release();
                iterator.remove();
                continue;
            }
            presentation.prepare();
            if (age >= 0 && age < ProjectorDeployment.OPEN_TICKS) presentation.open();
            if (presentation.impact != null) presentation.impact.tick(Minecraft.getMinecraft());
        }
    }

    static void render(Minecraft minecraft, float partial, double cameraX, double cameraY, double cameraZ) {
        for (Presentation presentation : ACTIVE.values()) {
            if (presentation.world != minecraft.theWorld) continue;
            double age = presentation.world.getTotalWorldTime() - presentation.animation.startTick() + partial;
            if (presentation.impact != null) age = Math.max(age, ProjectorDeployment.IMPACT_TICK);
            GL11.glPushAttrib(
                GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT
                    | GL11.GL_COLOR_BUFFER_BIT
                    | GL11.GL_CURRENT_BIT
                    | GL11.GL_POLYGON_BIT
                    | GL11.GL_LINE_BIT
                    | GL11.GL_TRANSFORM_BIT
                    | GL11.GL_TEXTURE_BIT);
            try {
                if (presentation.model != null) presentation.mesh.compile(COMPILE_NANOS);
                if (age < 0) continue;
                if (age >= ProjectorDeployment.OPEN_TICKS && presentation.model != null)
                    drawStructure(minecraft, presentation, age, cameraX, cameraY, cameraZ);
                float scale = presentation.animation.circleScale(age);
                if (scale > 0) drawCircle(presentation.animation, age, scale, cameraX, cameraY, cameraZ);
            } finally {
                GL11.glPopAttrib();
            }
        }
    }

    private static void drawStructure(Minecraft minecraft, Presentation presentation, double age, double cameraX,
        double cameraY, double cameraZ) {
        minecraft.getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LESS);
        GL11.glDepthMask(true);
        GL11.glColor4f(1, 1, 1, 1);

        ProjectorDeployment animation = presentation.animation;
        double offset = animation.verticalOffset(age);
        if (age < ProjectorDeployment.IMPACT_TICK) {
            PORTAL_PLANE.clear();
            PORTAL_PLANE.put(0)
                .put(-1)
                .put(0)
                .put(animation.openingY() - cameraY)
                .flip();
            GL11.glClipPlane(GL11.GL_CLIP_PLANE0, PORTAL_PLANE);
            GL11.glEnable(GL11.GL_CLIP_PLANE0);
        } else {
            // Real blocks already rendered by the terrain pass take precedence at the landing position.
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(1, 1);
        }
        ProjectorPlan plan = presentation.plan;
        if (presentation.repeatedModel) {
            ProjectorBatch.Layout layout = presentation.layout;
            for (int copy = 0; copy < plan.batch.machines(); copy++) {
                var at = layout.placement(copy);
                double x = plan.x + at.x(), y = plan.y + at.y() + offset, z = plan.z + at.z();
                if (FxCamera.inView(new Vec3d(x, y, z), presentation.modelRadius))
                    drawModel(presentation, at.facing(), x, y, z, cameraX, cameraY, cameraZ);
            }
        } else {
            drawModel(presentation, plan.facing, plan.x, plan.y + offset, plan.z, cameraX, cameraY, cameraZ);
        }
        GL11.glDisable(GL11.GL_CLIP_PLANE0);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
    }

    private static void drawModel(Presentation presentation, ExtendedFacing facing, double x, double y, double z,
        double cameraX, double cameraY, double cameraZ) {
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(x - cameraX, y - cameraY, z - cameraZ);
            boolean mirrored = ProjectorRenderer.applyFacing(presentation.model, facing);
            GL11.glFrontFace(mirrored ? GL11.GL_CW : GL11.GL_CCW);
            presentation.mesh.draw(ProjectorRenderer.sectionsInView(presentation.model, facing, x, y, z));
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static void drawCircle(ProjectorDeployment animation, double age, float scale, double cameraX,
        double cameraY, double cameraZ) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(
                animation.centerX() - cameraX,
                animation.openingY() - cameraY,
                animation.centerZ() - cameraZ);
            GL11.glScaled(animation.radius() * scale, 1, animation.radius() * scale);
            GL11.glRotated(age * CIRCLE_DEGREES_PER_TICK % 360, 0, 1, 0);
            ProjectorSummoningCircle.draw();
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static final class Presentation {

        private final World world;
        private final ProjectorDeployment animation;
        private final ProjectorPlan plan;
        private final boolean batch;
        private final ProjectorModelMesh mesh = new ProjectorModelMesh();
        private ProjectorStructure template, model;
        private ProjectorBatch.Layout layout;
        private long finishedTick = -1;
        private boolean repeatedModel, opened, unavailable;
        private ProjectorDeploymentImpact impact;
        private double modelRadius;

        private Presentation(World world, ProjectorDeployment animation, ProjectorPlan plan, boolean batch) {
            this.world = world;
            this.animation = animation;
            this.plan = plan;
            this.batch = batch;
        }

        private void prepare() {
            if (model != null || unavailable) return;
            if (template == null) {
                template = ProjectorStructures.get(plan.settings());
                if (template == null) return;
                if (template.isEmpty() || !template.failure.isEmpty() || !template.limit.isEmpty()) {
                    unavailable = true;
                    return;
                }
            }
            if (!batch) {
                model = template;
            } else {
                if (layout == null) layout = new ProjectorBatch.Layout(plan.batch, template, plan.facing);
                if (!layout.advanceSearch(System.nanoTime() + LAYOUT_NANOS)) return;
                if (!layout.solve()) {
                    unavailable = true;
                    return;
                }
                repeatedModel = (long) plan.batch.machines() * (template.size + template.emptyCells.length)
                    > FxBlockMesh.MAX_BLOCKS;
                model = repeatedModel ? template : ProjectorBatchPreview.expand(template, plan, plan.facing, layout);
                if (repeatedModel) {
                    double x = Math.max(Math.abs(template.minA), Math.abs(template.maxA)) + 1;
                    double y = Math.max(Math.abs(template.minB), Math.abs(template.maxB)) + 1;
                    double z = Math.max(Math.abs(template.minC), Math.abs(template.maxC)) + 1;
                    modelRadius = Math.sqrt(x * x + y * y + z * z);
                }
            }
            mesh.prepare(model);
        }

        private void open() {
            if (opened) return;
            opened = true;
            world.playSound(
                animation.centerX(),
                animation.openingY(),
                animation.centerZ(),
                "random.orb",
                0.65f,
                0.45f,
                false);
        }
    }
}
