package com.xyp.gtnrlib.client.render.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.client.animation.HmiItemBindings;
import com.xyp.gtnrlib.client.animation.ToolboxMotion;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Red steel case with an actual hinged lid, raised handle and open tray. Coordinates are in blocks. */
@SideOnly(Side.CLIENT)
public final class ToolboxModel {

    private static final int RED = 0xa63827, EDGE = 0x65251e, STEEL = 0xc3b9a1, LINING = 0x3a302a;
    private static final float HINGE_Y = .10f, HINGE_Z = -.22f, HANDLE_Y = .34f;

    private ToolboxModel() {}

    public static void render(ToolboxMotion motion, ItemStack[] contents, HmiItemBindings.GripHands hands) {
        float support = motion.support(), lid = motion.lid(), reach = motion.reach();
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(.42f - .26f * support, -.42f + .10f * support, -1.10f - .12f * support);
            GL11.glRotatef(16 - 9 * support, 0, 1, 0);
            GL11.glRotatef(8 * support, 1, 0, 0);
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
            metal(LINING, -.40f, -.24f, -.22f, .40f, -.18f, .22f);
            metal(RED, -.42f, -.24f, -.24f, -.37f, .10f, .24f);
            metal(RED, .37f, -.24f, -.24f, .42f, .10f, .24f);
            metal(RED, -.37f, -.24f, -.24f, .37f, .10f, -.19f);
            metal(RED, -.37f, -.24f, .19f, .37f, .10f, .24f);
            metal(EDGE, -.425f, -.22f, .241f, .425f, -.18f, .25f);
            metal(STEEL, -.30f, -.04f, .241f, -.23f, .09f, .26f);
            metal(STEEL, .23f, -.04f, .241f, .30f, .09f, .26f);
            for (int i = -1; i <= 1; i++) metal(LINING, i * .18f - .012f, -.18f, -.19f, i * .18f + .012f, -.06f, .19f);
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(0, HINGE_Y, HINGE_Z);
                GL11.glRotatef(-105 * lid, 1, 0, 0);
                GL11.glTranslatef(0, -HINGE_Y, -HINGE_Z);
                metal(RED, -.425f, .10f, -.245f, .425f, .18f, .245f);
                metal(EDGE, -.425f, .09f, .24f, .425f, .12f, .255f);
                metal(STEEL, -.155f, .18f, -.035f, -.105f, HANDLE_Y, .035f);
                metal(STEEL, .105f, .18f, -.035f, .155f, HANDLE_Y, .035f);
                metal(LINING, -.155f, HANDLE_Y - .025f, -.04f, .155f, HANDLE_Y + .025f, .04f);
            } finally {
                GL11.glPopMatrix();
            }
            if (lid > 0) tools(contents);
            if (support > 0) hand(hands, true, -.75f + .39f * support, -.65f + .41f * support, .18f, -38, -65);
            double angle = Math.toRadians(-105 * lid);
            float gripY = HINGE_Y + (float) (Math.cos(angle) * (HANDLE_Y - HINGE_Y) - Math.sin(angle) * -HINGE_Z);
            float gripZ = HINGE_Z + (float) (Math.sin(angle) * (HANDLE_Y - HINGE_Y) + Math.cos(angle) * -HINGE_Z);
            hand(
                hands,
                false,
                (.10f + motion.searchX()) * reach,
                gripY + (.02f + motion.searchY() - gripY) * reach,
                gripZ + (.04f - gripZ) * reach,
                18 + 14 * reach,
                -55 - 45 * reach);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static void tools(ItemStack[] contents) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(TextureMap.locationItemsTexture);
        for (int i = 0; i < Math.min(contents.length, 6); i++) {
            ItemStack stack = contents[i];
            if (stack == null) continue;
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(-.30f + (i % 3) * .20f, -.10f, -.14f + (i / 3) * .17f);
                GL11.glRotatef(90, 1, 0, 0);
                GL11.glScalef(.17f, .25f, .17f);
                int passes = stack.getItem()
                    .requiresMultipleRenderPasses()
                        ? stack.getItem()
                            .getRenderPasses(stack.getItemDamage())
                        : 1;
                for (int pass = 0; pass < passes; pass++) {
                    var icon = stack.getItem()
                        .getIcon(stack, pass);
                    if (icon == null) continue;
                    int color = stack.getItem()
                        .getColorFromItemStack(stack, pass);
                    GL11.glColor4ub((byte) (color >> 16), (byte) (color >> 8), (byte) color, (byte) 255);
                    ItemRenderer.renderItemIn2D(
                        Tessellator.instance,
                        icon.getMaxU(),
                        icon.getMinV(),
                        icon.getMinU(),
                        icon.getMaxV(),
                        icon.getIconWidth(),
                        icon.getIconHeight(),
                        .035f);
                    GL11.glTranslatef(0, 0, -.002f);
                }
            } finally {
                GL11.glPopMatrix();
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
        DefaultItemModels.box(Blocks.iron_block, 0, color, x, y, z, x2, y2, z2);
    }
}
