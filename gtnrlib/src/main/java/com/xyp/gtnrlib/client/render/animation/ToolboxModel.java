package com.xyp.gtnrlib.client.render.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.client.animation.HmiItemBindings;
import com.xyp.gtnrlib.client.animation.ToolboxMotion;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Red steel case with an actual hinged lid, raised handle and open tray. Coordinates are in blocks. */
@SideOnly(Side.CLIENT)
public final class ToolboxModel {

    private static final int RED = 0x852724, EDGE = 0x321513, STEEL = 0x919191, LINING = 0x302b28;
    private static final float HINGE_Y = .16f, HINGE_Z = -.23f, HANDLE_Y = .47f;
    private static final int[] SIDES = { -1, 1 };
    private static final RenderBlocks RENDER_BLOCKS = new RenderBlocks();
    private static final RenderItem ITEMS = new RenderItem();
    private static final int[][] FACES = { { 0, -1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1 },
        { 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0 }, { 0, 0, -1, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0 },
        { 0, 0, 1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1 }, { -1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0 },
        { 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1 } };

    private ToolboxModel() {}

    public static void render(ToolboxMotion motion, ItemStack appearance, ItemStack[] contents,
        HmiItemBindings.GripHands hands) {
        float support = motion.support(), lid = motion.lid(), reach = motion.reach();
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(.42f + .45f * support, -.08f, -1.45f - .05f * support);
            GL11.glRotatef(16 - 9 * support, 0, 1, 0);
            GL11.glRotatef(42 * support, 1, 0, 0);
            body(appearance.getIconIndex());
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(0, HINGE_Y, HINGE_Z);
                GL11.glRotatef(-105 * lid, 1, 0, 0);
                GL11.glTranslatef(0, -HINGE_Y, -HINGE_Z);
                lid(appearance.getIconIndex());
            } finally {
                GL11.glPopMatrix();
            }
            if (lid > 0) tools(contents);
            if (support > 0) hand(hands, true, -.75f + .39f * support, -.80f + .395f * support, .10f, -38, -65);
            double angle = Math.toRadians(-105 * lid);
            float gripY = HINGE_Y + (float) (Math.cos(angle) * (HANDLE_Y - HINGE_Y) - Math.sin(angle) * -HINGE_Z);
            float gripZ = HINGE_Z + (float) (Math.sin(angle) * (HANDLE_Y - HINGE_Y) + Math.cos(angle) * -HINGE_Z);
            hand(
                hands,
                false,
                (-.04f + motion.searchX()) * reach,
                gripY + (.12f + motion.searchY() - gripY) * reach,
                gripZ + (.04f - gripZ) * reach,
                18 + 14 * reach + motion.searchX() * 35,
                -55 - 45 * reach);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static void body(IIcon icon) {
        metal(LINING, -.40f, -.28f, -.22f, .40f, -.23f, .22f);
        metal(RED, -.42f, -.28f, -.24f, -.37f, .16f, .24f);
        metal(RED, .37f, -.28f, -.24f, .42f, .16f, .24f);
        metal(RED, -.37f, -.28f, -.24f, .37f, .16f, -.19f);
        metal(RED, -.37f, -.28f, .19f, .37f, .16f, .24f);
        // The installed toolbox's pixel art supplies the orange panels and distinctive central clasp.
        front(icon, -.40f, -.27f, .40f, .14f, .241f, 1, 7, 15, 15);
        for (int side : SIDES) {
            float x = side * .42f;
            metal(EDGE, x - .012f, -.27f, -.22f, x + .012f, -.23f, .22f);
            metal(EDGE, x - .012f, .115f, -.24f, x + .012f, .16f, .24f);
            metal(0x9b4227, x - .015f, -.22f, -.16f, x + .015f, .09f, .16f);
            metal(0xbe6030, x - .018f, -.20f, -.12f, x + .018f, .07f, .12f);
            metal(EDGE, side * .38f - .025f, -.28f, .235f, side * .38f + .025f, .16f, .26f);
            metal(STEEL, side * .335f - .012f, .075f, .242f, side * .335f + .012f, .099f, .258f);
            metal(EDGE, side * .26f - .06f, .12f, -.265f, side * .26f + .06f, .19f, -.23f);
        }
        metal(0x424342, -.105f, -.21f, .242f, .105f, .08f, .266f);
        metal(STEEL, -.12f, -.21f, .264f, -.065f, .08f, .285f);
        metal(STEEL, .065f, -.21f, .264f, .12f, .08f, .285f);
        metal(STEEL, -.12f, -.22f, .264f, .12f, -.17f, .285f);
        metal(0x222323, -.045f, -.105f, .267f, .045f, -.035f, .275f);
        // Shallow divided tray above the deep case.
        metal(LINING, -.365f, -.085f, -.185f, .365f, -.055f, .185f);
        metal(0x58534a, -.36f, -.055f, -.185f, .36f, .025f, -.165f);
        metal(0x58534a, -.36f, -.055f, .165f, .36f, .025f, .185f);
        for (int side : SIDES) metal(0x58534a, side * .11f - .01f, -.055f, -.165f, side * .11f + .01f, .025f, .165f);
        metal(0x58534a, -.36f, -.055f, -.01f, .36f, .025f, .01f);
    }

    private static void lid(IIcon icon) {
        metal(EDGE, -.43f, .15f, -.25f, .43f, .19f, .25f);
        metal(RED, -.42f, .19f, -.24f, .42f, .235f, .24f);
        metal(0xa33433, -.38f, .235f, -.205f, .38f, .285f, .205f);
        metal(0x6e2523, -.355f, .285f, -.18f, .355f, .30f, .18f);
        front(icon, -.42f, .19f, .42f, .235f, .241f, 1, 6, 15, 7);
        metal(0x4e2320, -.35f, .135f, -.17f, .35f, .15f, .17f);
        metal(0x742c27, -.29f, .13f, -.12f, .29f, .135f, .12f);
        metal(0x464746, -.22f, .30f, -.065f, -.13f, .325f, .065f);
        metal(0x464746, .13f, .30f, -.065f, .22f, .325f, .065f);
        metal(STEEL, -.20f, .325f, -.035f, -.145f, HANDLE_Y, .035f);
        metal(STEEL, .145f, .325f, -.035f, .20f, HANDLE_Y, .035f);
        metal(STEEL, -.20f, HANDLE_Y - .025f, -.04f, .20f, HANDLE_Y + .025f, .04f);
        metal(0xb3b3b3, -.19f, HANDLE_Y + .025f, -.035f, .19f, HANDLE_Y + .035f, .035f);
    }

    private static void front(IIcon icon, float x, float y, float x2, float y2, float z, int u, int v, int u2, int v2) {
        if (icon == null) return;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_LIGHTING_BIT);
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1, 1, 1, 1);
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(TextureMap.locationItemsTexture);
            var tess = Tessellator.instance;
            tess.startDrawingQuads();
            tess.setColorOpaque_I(0xffffff);
            tess.setNormal(0, 0, 1);
            tess.addVertexWithUV(x, y, z, icon.getInterpolatedU(u), icon.getInterpolatedV(v2));
            tess.addVertexWithUV(x2, y, z, icon.getInterpolatedU(u2), icon.getInterpolatedV(v2));
            tess.addVertexWithUV(x2, y2, z, icon.getInterpolatedU(u2), icon.getInterpolatedV(v));
            tess.addVertexWithUV(x, y2, z, icon.getInterpolatedU(u), icon.getInterpolatedV(v));
            tess.draw();
        } finally {
            GL11.glPopAttrib();
        }
    }

    private static void tools(ItemStack[] contents) {
        var mc = Minecraft.getMinecraft();
        for (int i = 0; i < Math.min(contents.length, 6); i++) {
            ItemStack stack = contents[i];
            if (stack == null) continue;
            GL11.glPushAttrib(
                GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT
                    | GL11.GL_TEXTURE_BIT
                    | GL11.GL_COLOR_BUFFER_BIT
                    | GL11.GL_DEPTH_BUFFER_BIT
                    | GL11.GL_LIGHTING_BIT);
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(-.32f + (i % 3) * .22f, .04f, -.015f + (i / 3) * .16f);
                GL11.glDisable(GL11.GL_CULL_FACE);
                // GT tools have no vanilla icon. Their equipped renderer retains materials without GUI charge bars.
                var nativeRenderer = MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.EQUIPPED);
                if (nativeRenderer != null) {
                    GL11.glRotatef(-90, 1, 0, 0);
                    GL11.glScalef(.192f, .14f, .16f);
                    nativeRenderer.renderItem(ItemRenderType.EQUIPPED, stack, RENDER_BLOCKS, mc.thePlayer);
                } else {
                    GL11.glRotatef(90, 1, 0, 0);
                    GL11.glScalef(.012f, -.00875f, .010f);
                    if (!ForgeHooksClient
                        .renderInventoryItem(RENDER_BLOCKS, mc.getTextureManager(), stack, true, 3, 0, 0))
                        ITEMS.renderItemIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, 0, 0, false);
                }
            } finally {
                GL11.glPopMatrix();
                GL11.glPopAttrib();
            }
        }
    }

    private static void hand(HmiItemBindings.GripHands hands, boolean left, float x, float y, float z, float roll,
        float pitch) {
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(x, y, z);
            GL11.glRotatef(roll, 0, 0, 1);
            GL11.glRotatef(pitch, 1, 0, 0);
            GL11.glColor4f(1, 1, 1, 1);
            hands.draw(left);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static void metal(int color, float x, float y, float z, float x2, float y2, float z2) {
        var tess = Tessellator.instance;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glTranslatef(x, y, z);
            GL11.glScalef(x2 - x, y2 - y, z2 - z);
            GL11.glColor4f(1, 1, 1, 1);
            tess.startDrawingQuads();
            for (var face : FACES) {
                float shade = face[1] > 0 ? 1 : face[1] < 0 ? .5f : face[2] != 0 ? .8f : .65f;
                tess.setColorOpaque(
                    (int) ((color >> 16 & 255) * shade),
                    (int) ((color >> 8 & 255) * shade),
                    (int) ((color & 255) * shade));
                tess.setNormal(face[0], face[1], face[2]);
                for (int corner = 0; corner < 4; corner++) {
                    int offset = 3 + corner * 3;
                    tess.addVertex(face[offset], face[offset + 1], face[offset + 2]);
                }
            }
            tess.draw();
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }
}
