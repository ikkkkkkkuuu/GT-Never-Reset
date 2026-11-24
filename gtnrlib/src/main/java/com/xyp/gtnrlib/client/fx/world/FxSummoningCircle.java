package com.xyp.gtnrlib.client.fx.world;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class FxSummoningCircle {

    private static final double TURN = 2 * Math.PI;
    private static final double SECTOR = TURN / 6;
    private static final double BORDER = 0.80;
    private static final double SEAL_RADIUS = 0.46;
    private static final double SEAL_SIZE = 0.095;
    private static final double CENTER_SIZE = 0.177;
    private static final int RING_SEGMENTS = 128;
    private static final int GOLD = 0xC09552;
    // Each pair connects two points on a 3 by 3 grid, indexed row by row.
    private static final int[][] RUNES = { { 6, 1, 1, 8, 3, 5 }, { 0, 8, 2, 6, 3, 1 }, { 0, 4, 4, 2, 4, 8, 3, 7 },
        { 0, 6, 1, 5, 5, 7 }, { 1, 7, 3, 4, 4, 8 }, { 0, 2, 0, 8, 3, 5 }, { 0, 4, 4, 8, 2, 4, 4, 6 },
        { 1, 3, 3, 7, 7, 5, 5, 2 } };

    private FxSummoningCircle() {}

    static void draw() {
        Tessellator tess = Tessellator.instance;
        GL11.glLineWidth(5);
        tess.startDrawing(GL11.GL_LINES);
        tess.setColorRGBA_I(GOLD, 32);
        ring(tess, 0, 0, 1);
        tess.draw();

        GL11.glLineWidth(3);
        tess.startDrawing(GL11.GL_LINES);
        tess.setColorRGBA_I(GOLD, 247);
        ring(tess, 0, 0, 1);
        ring(tess, 0, 0, 0.958);
        ring(tess, 0, 0, BORDER);
        tess.setColorRGBA_I(GOLD, 221);
        ring(tess, 0, 0, CENTER_SIZE + 0.046);
        tess.draw();

        GL11.glLineWidth(2);
        tess.startDrawing(GL11.GL_LINES);
        tess.setColorRGBA_I(GOLD, 224);
        ring(tess, 0, 0, CENTER_SIZE);
        ring(tess, 0, 0, 0.056);
        for (int i = 0; i < 6; i++) {
            double angle = i * SECTOR - Math.PI / 2;
            tess.setColorRGBA_I(GOLD, 167);
            polarLine(tess, BORDER, angle, BORDER, angle + SECTOR);
            tess.setColorRGBA_I(GOLD, 186);
            polarLine(tess, BORDER, angle, BORDER, angle + 2 * SECTOR);
            polarLine(tess, 0.059, angle, CENTER_SIZE, angle);
            tess.setColorRGBA_I(GOLD, 227);
            drawInscription(tess, i, angle);
            if ((i & 1) == 0) drawSeal(tess, i, angle + SECTOR * 0.5);
        }
        tess.draw();
    }

    private static void drawInscription(Tessellator tess, int sector, double angle) {
        glyph(tess, 0.878, angle, 0.035, (sector + 2) % RUNES.length);
        for (int mark = 1; mark <= 7; mark++) {
            double at = angle + mark * SECTOR / 8;
            glyph(tess, 0.878, at, 0.026, (sector * 5 + mark * 3) % RUNES.length);
        }
    }

    private static void drawSeal(Tessellator tess, int sector, double angle) {
        double cosine = Math.cos(angle), sine = Math.sin(angle);
        double x = SEAL_RADIUS * cosine, z = SEAL_RADIUS * sine;
        tess.setColorRGBA_I(GOLD, 219);
        ring(tess, x, z, SEAL_SIZE);
        ring(tess, x, z, SEAL_SIZE * 0.8);
        tess.setColorRGBA_I(GOLD, 239);
        glyph(tess, SEAL_RADIUS, angle, 0.05, (sector + 1) % RUNES.length);
        tess.setColorRGBA_I(GOLD, 170);
        polarLine(tess, CENTER_SIZE + 0.046, angle, SEAL_RADIUS - SEAL_SIZE, angle);
        double outer = 0.70;
        arc(tess, outer * cosine, outer * sine, 0.215, angle + Math.PI - 0.75, 1.5, 24);
        arc(tess, outer * cosine, outer * sine, 0.246, angle + Math.PI - 0.75, 1.5, 24);
        polarLine(tess, SEAL_RADIUS + SEAL_SIZE, angle, BORDER, angle);
    }

    private static void glyph(Tessellator tess, double radius, double angle, double size, int index) {
        double cosine = Math.cos(angle), sine = Math.sin(angle);
        for (int point : RUNES[index]) {
            double tangential = (point % 3 - 1) * size * 0.64;
            double radial = radius + (1 - point / 3) * size;
            tess.addVertex(radial * cosine - tangential * sine, 0, radial * sine + tangential * cosine);
        }
    }

    private static void ring(Tessellator tess, double x, double z, double radius) {
        arc(tess, x, z, radius, 0, TURN, Math.max(24, (int) (radius * RING_SEGMENTS)));
    }

    private static void arc(Tessellator tess, double x, double z, double radius, double start, double sweep,
        int steps) {
        double lastX = x + radius * Math.cos(start);
        double lastZ = z + radius * Math.sin(start);
        for (int step = 1; step <= steps; step++) {
            double angle = start + sweep * step / steps;
            double nextX = x + radius * Math.cos(angle);
            double nextZ = z + radius * Math.sin(angle);
            tess.addVertex(lastX, 0, lastZ);
            tess.addVertex(nextX, 0, nextZ);
            lastX = nextX;
            lastZ = nextZ;
        }
    }

    private static void polarLine(Tessellator tess, double r0, double a0, double r1, double a1) {
        tess.addVertex(r0 * Math.cos(a0), 0, r0 * Math.sin(a0));
        tess.addVertex(r1 * Math.cos(a1), 0, r1 * Math.sin(a1));
    }
}
