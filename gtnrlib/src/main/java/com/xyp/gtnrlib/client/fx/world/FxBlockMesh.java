package com.xyp.gtnrlib.client.fx.world;

import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.GTNRLib;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * Chosen blocks of a source world, drawn at their own positions from display lists. The blocks are split into
 * 16-block sections; a section is compiled once and again only when the blocks it shows change, and compiling is
 * spread over frames by a time budget. Call {@link #release()} when done, display lists are not freed otherwise.
 */
@SideOnly(Side.CLIENT)
public class FxBlockMesh {

    public static final int MAX_BLOCKS = 250_000;

    public static class Section {

        /** Lowest corner of the section in source-world coordinates; the section spans 16 blocks on each axis. */
        public final int x, y, z;
        public final IntArrayList cells = new IntArrayList();
        public int list = -1;
        public boolean dirty = true;
        public long signature;

        public Section(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public final FxBlockView view = new FxBlockView();
    private final Long2ObjectOpenHashMap<Section> sections = new Long2ObjectOpenHashMap<>();
    private Object key;
    private int[] xs = new int[0], ys = new int[0], zs = new int[0];

    public int x(int cell) {
        return xs[cell];
    }

    public int y(int cell) {
        return ys[cell];
    }

    public int z(int cell) {
        return zs[cell];
    }

    public int cellAt(int x, int y, int z) {
        return view.cellAt(x, y, z);
    }

    public void release() {
        for (Section section : sections.values()) {
            if (section.list >= 0) GL11.glDeleteLists(section.list, 1);
        }
        sections.clear();
        key = null;
        view.source = null;
        view.shown = null;
        view.cells.clear();
    }

    /**
     * Uses the blocks at the given positions of the source world. Nothing is rebuilt while the key stays the same;
     * more than {@link #MAX_BLOCKS} blocks leave the mesh empty.
     *
     * @return true when the mesh was rebuilt, so per-block filters must be applied again
     */
    public boolean prepare(Object key, World source, int[] xs, int[] ys, int[] zs) {
        if (key == this.key) return false;
        release();
        this.key = key;
        if (xs.length > MAX_BLOCKS) return true;
        this.xs = xs;
        this.ys = ys;
        this.zs = zs;
        view.source = source;
        for (int cell = 0; cell < xs.length; cell++) {
            view.cells.put(FxVoxelOutline.pack(xs[cell], ys[cell], zs[cell]), cell);
            int sectionX = xs[cell] >> 4, sectionY = ys[cell] >> 4, sectionZ = zs[cell] >> 4;
            sections.computeIfAbsent(
                FxVoxelOutline.pack(sectionX, sectionY, sectionZ),
                k -> new Section(sectionX << 4, sectionY << 4, sectionZ << 4)).cells.add(cell);
        }
        return true;
    }

    /**
     * Shows only blocks whose entry is true, or all blocks for null. Only sections whose shown blocks changed are
     * compiled again.
     */
    public void show(boolean[] shown) {
        if (shown == null && view.shown == null) return;
        view.shown = shown;
        for (Section section : sections.values()) {
            long signature = 0;
            for (int i = 0; i < section.cells.size(); i++) {
                int cell = section.cells.getInt(i);
                if (shown == null || shown[cell]) signature = signature * 31 + cell + 1;
            }
            if (signature != section.signature) {
                section.signature = signature;
                section.dirty = true;
            }
        }
    }

    /**
     * Compiles changed sections until the budget runs out.
     *
     * @return true when every section is compiled
     */
    public boolean compile(long budgetNanos) {
        long deadline = System.nanoTime() + budgetNanos;
        boolean complete = true;
        for (Section section : sections.values()) {
            if (!section.dirty) continue;
            if (System.nanoTime() > deadline) {
                complete = false;
                continue;
            }
            compile(section);
        }
        return complete;
    }

    /**
     * Draws the compiled sections. The caller binds the block texture and sets the GL state.
     */
    public void draw() {
        for (Section section : sections.values()) {
            if (section.list >= 0) GL11.glCallList(section.list);
        }
    }

    /**
     * Draws the compiled sections that pass the test, such as a view-frustum check of each section's box.
     */
    public void draw(Predicate<Section> visible) {
        for (Section section : sections.values()) {
            if (section.list >= 0 && visible.test(section)) GL11.glCallList(section.list);
        }
    }

    private void compile(Section section) {
        section.dirty = false;
        if (section.list < 0) section.list = GL11.glGenLists(1);
        Minecraft mc = Minecraft.getMinecraft();
        int ambientOcclusion = mc.gameSettings.ambientOcclusion;
        mc.gameSettings.ambientOcclusion = 0;
        RenderBlocks renderer = new RenderBlocks(view);
        Tessellator tessellator = Tessellator.instance;
        EntityPlayer player = mc.thePlayer;
        World playerWorld = player == null ? null : player.worldObj;
        GL11.glNewList(section.list, GL11.GL_COMPILE);
        tessellator.setTranslation(0, 0, 0);
        tessellator.startDrawingQuads();
        try {
            // Some renderers (GT multiblock fronts) look tile entities up in the player's world at the drawn position.
            if (player != null) player.worldObj = view.source;
            for (int pass = 0; pass < 2; pass++) {
                ForgeHooksClient.setRenderPass(pass);
                for (int i = 0; i < section.cells.size(); i++) {
                    int cell = section.cells.getInt(i);
                    if (view.shown != null && !view.shown[cell]) continue;
                    Block block = view.getBlock(xs[cell], ys[cell], zs[cell]);
                    if (!block.canRenderInPass(pass)) continue;
                    try {
                        renderer.renderBlockByRenderType(block, xs[cell], ys[cell], zs[cell]);
                    } catch (RuntimeException e) {
                        GTNRLib.LOG.debug("Could not draw {} in a block mesh", block, e);
                    }
                }
            }
        } finally {
            ForgeHooksClient.setRenderPass(-1);
            tessellator.draw();
            GL11.glEndList();
            mc.gameSettings.ambientOcclusion = ambientOcclusion;
            if (player != null) player.worldObj = playerWorld;
        }
    }
}
