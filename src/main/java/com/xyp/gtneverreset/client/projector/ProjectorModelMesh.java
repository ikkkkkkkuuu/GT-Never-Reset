package com.xyp.gtneverreset.client.projector;

import java.nio.FloatBuffer;
import java.util.EnumMap;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtneverreset.common.projector.ProjectorBatchHatches;
import com.xyp.gtneverreset.common.projector.ProjectorOrientation;
import com.xyp.gtneverreset.common.projector.ProjectorStructure;
import com.xyp.gtnrlib.client.fx.world.FxBlockMesh;
import com.xyp.gtnrlib.client.fx.world.FxBlockView;
import com.xyp.gtnrlib.client.fx.world.FxVoxelOutline;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class ProjectorModelMesh extends FxBlockMesh {

    private final EnumMap<ExtendedFacing, Transform> transforms = new EnumMap<>(ExtendedFacing.class);

    @Desugar
    private record Transform(FloatBuffer matrix, boolean mirrored) {}

    private final FxBlockView machineView = new FxBlockView();
    private ProjectorBatchPreview array;

    boolean prepare(ProjectorStructure shape) {
        int[][] pos = shape.simulationPositions();
        if (!super.prepare(shape, shape.world, pos[0], pos[1], pos[2])) return false;
        array = shape instanceof ProjectorBatchPreview preview ? preview : null;
        return true;
    }

    @Override
    public void release() {
        super.release();
        array = null;
        transforms.clear();
        machineView.source = null;
        machineView.cells.clear();
    }

    @Override
    protected void renderCell(RenderBlocks renderer, Block block, int cell) {
        if (array == null || array.machines[cell] == 0 || array.copyPoses[cell] == array.baseFacing) {
            super.renderCell(renderer, block, cell);
            return;
        }
        // Bake the original machine model under its copy's actual rigid transform.
        // This rotates the front overlay as well as its normal, including glow passes.
        int src = array.originals[cell];
        int[][] pos = array.template.simulationPositions();
        int sx = pos[0][src], sy = pos[1][src], sz = pos[2][src];
        var edit = array.editAt(cell);
        var sourceWorld = edit == null ? array.template.world : edit.world();
        if (edit != null) {
            sx = edit.x();
            sy = ProjectorBatchHatches.SAMPLE_Y;
            sz = 0;
        }
        Transform transform = transforms.computeIfAbsent(array.copyPoses[cell], pose -> {
            ExtendedFacing local = ProjectorOrientation.changeBasis(pose, array.baseFacing, array.canonical);
            FloatBuffer result = BufferUtils.createFloatBuffer(16);
            int[] basis = ProjectorOrientation.matrix(array.canonical, local);
            ProjectorRenderer.fillMatrix(result, basis);
            return new Transform(result, ProjectorOrientation.mirrors(basis));
        });
        Tessellator tess = Tessellator.instance;
        var player = Minecraft.getMinecraft().thePlayer;
        World previousWorld = player == null ? null : player.worldObj;
        IBlockAccess previousAccess = renderer.blockAccess;
        boolean previousAllFaces = renderer.renderAllFaces;
        tess.draw();
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_POLYGON_BIT);
        try {
            GL11.glTranslatef(x(cell) + 0.5f, y(cell) + 0.5f, z(cell) + 0.5f);
            GL11.glMultMatrix(transform.matrix());
            // The enclosing preview already selected a front winding. Cull the opposite
            // side for a mirrored copy without replacing that enclosing orientation.
            if (transform.mirrored()) GL11.glCullFace(GL11.GL_FRONT);
            GL11.glTranslatef(-sx - 0.5f, -sy - 0.5f, -sz - 0.5f);
            machineView.source = sourceWorld;
            machineView.cells.clear();
            machineView.cells.put(FxVoxelOutline.pack(sx, sy, sz), 0);
            renderer.blockAccess = machineView;
            renderer.renderAllFaces = true;
            if (player != null) player.worldObj = sourceWorld;
            tess.startDrawingQuads();
            try {
                renderer.renderBlockByRenderType(block, sx, sy, sz);
            } finally {
                tess.draw();
            }
        } finally {
            if (player != null) player.worldObj = previousWorld;
            renderer.blockAccess = previousAccess;
            renderer.renderAllFaces = previousAllFaces;
            GL11.glPopAttrib();
            GL11.glPopMatrix();
            tess.startDrawingQuads();
        }
    }
}
