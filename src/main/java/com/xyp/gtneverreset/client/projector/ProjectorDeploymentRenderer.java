package com.xyp.gtneverreset.client.projector;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.common.packet.ProjectorDeploymentPacket.Phase;
import com.xyp.gtneverreset.common.projector.ProjectorBatch;
import com.xyp.gtneverreset.common.projector.ProjectorPlan;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtnrlib.animation.StructureDeployment;
import com.xyp.gtnrlib.client.fx.world.FxBlockMesh;
import com.xyp.gtnrlib.client.fx.world.FxCamera;
import com.xyp.gtnrlib.client.fx.world.FxStructureDeployment;
import com.xyp.gtnrlib.utils.Vec3d;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ProjectorDeploymentRenderer {

    private static final long COMPILE_NANOS = 3_000_000L;
    private static final long LAYOUT_NANOS = 2_000_000L;
    private static final Map<Long, Presentation> ACTIVE = new LinkedHashMap<>();

    private ProjectorDeploymentRenderer() {}

    public static void receive(Minecraft minecraft, long id, int dimension, Phase phase, StructureDeployment animation,
        NBTTagCompound data, boolean batch, int finishedBands) {
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
                update(id, phase, finishedBands);
            }
        });
    }

    private static void start(long id, World world, StructureDeployment animation, ProjectorPlan plan, boolean batch) {
        ACTIVE.putIfAbsent(id, new Presentation(world, animation, plan, batch));
    }

    private static void update(long id, Phase phase, int finishedBands) {
        Presentation presentation = ACTIVE.get(id);
        if (presentation == null) return;
        switch (phase) {
            case IMPACT -> presentation.effects.impact();
            case COMPLETE -> presentation.effects.complete();
            case PROGRESS -> presentation.effects.progress(finishedBands);
            case CANCEL -> {
                if (!presentation.effects.cancel()) {
                    ACTIVE.remove(id);
                    presentation.release();
                } else {
                    presentation.unavailable = true;
                    presentation.model = null;
                    presentation.mesh.release();
                }
            }
            case START -> {}
        }
    }

    static void clear() {
        for (Presentation presentation : ACTIVE.values()) presentation.release();
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
            if (world != presentation.world || presentation.effects.expired()) {
                presentation.release();
                iterator.remove();
                continue;
            }
            presentation.prepare();
            presentation.effects.tick(Minecraft.getMinecraft());
        }
    }

    static void render(Minecraft minecraft, float partial, double cameraX, double cameraY, double cameraZ) {
        long compileDeadline = System.nanoTime() + COMPILE_NANOS;
        for (Presentation presentation : ACTIVE.values()) {
            if (presentation.world != minecraft.theWorld) continue;
            boolean hasModel = presentation.model != null;
            presentation.effects.render(
                minecraft,
                partial,
                cameraX,
                cameraY,
                cameraZ,
                hasModel ? presentation.mesh : null,
                compileDeadline - System.nanoTime(),
                hasModel ? (x, y, z) -> drawModels(presentation, x, y, z, cameraX, cameraY, cameraZ) : null);
        }
    }

    private static void drawModels(Presentation presentation, double offsetX, double offsetY, double offsetZ,
        double cameraX, double cameraY, double cameraZ) {
        ProjectorPlan plan = presentation.plan;
        if (presentation.repeatedModel) {
            ProjectorBatch.Layout layout = presentation.layout;
            for (int copy = 0; copy < plan.batch.machines(); copy++) {
                var at = layout.placement(copy);
                double x = plan.x + at.x() + offsetX, y = plan.y + at.y() + offsetY, z = plan.z + at.z() + offsetZ;
                if (FxCamera.inView(new Vec3d(x, y, z), presentation.modelRadius))
                    drawModel(presentation, at.facing(), x, y, z, cameraX, cameraY, cameraZ);
            }
        } else {
            drawModel(
                presentation,
                plan.facing,
                plan.x + offsetX,
                plan.y + offsetY,
                plan.z + offsetZ,
                cameraX,
                cameraY,
                cameraZ);
        }
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

    private static final class Presentation {

        private final World world;
        private final ProjectorPlan plan;
        private final boolean batch;
        private final FxStructureDeployment effects;
        private final ProjectorModelMesh mesh = new ProjectorModelMesh();
        private ProjectorStructure template, model;
        private ProjectorBatch.Layout layout;
        private boolean repeatedModel, unavailable;
        private double modelRadius;

        private Presentation(World world, StructureDeployment animation, ProjectorPlan plan, boolean batch) {
            this.world = world;
            this.plan = plan;
            this.batch = batch;
            effects = new FxStructureDeployment(world, plan.buildStyle, animation);
        }

        private void release() {
            effects.close();
            mesh.release();
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

    }
}
