package com.xyp.gtnrlib.client.render.animation;

import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.compat.hmi.HmiPoseProgram;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** First-person meshes in pixel coordinates, textured from the running game's block atlas. */
@SideOnly(Side.CLIENT)
public final class DefaultItemModels {

    private static final Set<String> MODELS = models();
    private static final int[] SIDES = { -1, 1 };
    private static final RenderBlocks FACES = new RenderBlocks();

    private static Set<String> models() {
        var result = new LinkedHashSet<String>();
        for (String wood : new String[] { "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "cherry",
            "mangrove", "pale_oak" }) {
            result.add("minecraft:" + wood + "_boat");
            result.add("minecraft:" + wood + "_chest_boat");
        }
        for (String id : new String[] { "bamboo_raft", "bamboo_chest_raft", "bucket", "water_bucket", "lava_bucket",
            "milk_bucket", "powder_snow_bucket", "cod_bucket", "salmon_bucket", "pufferfish_bucket",
            "tropical_fish_bucket", "tadpole_bucket", "axolotl_bucket", "leaf_litter", "pink_petals", "wildflowers",
            "end_crystal" }) result.add("minecraft:" + id);
        return Set.copyOf(result);
    }

    public static Set<String> entries() {
        return MODELS;
    }

    public static boolean contains(String id) {
        return MODELS.contains(id);
    }

    public static boolean basket(String id) {
        return "minecraft:leaf_litter".equals(id) || "minecraft:pink_petals".equals(id)
            || "minecraft:wildflowers".equals(id);
    }

    static boolean draw(String id, String variant, int used, int age, HmiPoseProgram poses) {
        if (!contains(id)) return false;
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        if (id.endsWith("_boat") || id.endsWith("_raft")) boat(id, poses);
        else if (id.endsWith("bucket")) bucket(id.substring(10), variant, used, age, poses);
        else if (basket(id)) flowers(id);
        else crystal(age);
        GL11.glColor4f(1, 1, 1, 1);
        return true;
    }

    private static void mount(float x, float y, float z, float pitch, float turn, float size) {
        GL11.glTranslatef(x / 16, y / 16, z / 16);
        GL11.glRotatef(pitch, 1, 0, 0);
        GL11.glRotatef(turn, 0, 1, 0);
        GL11.glScalef(size / 16, size / 16, size / 16);
    }

    private static void boat(String id, HmiPoseProgram poses) {
        mount(-.05f, 4, .5f, 0, 145, .64f);
        String wood = id.substring(10, id.lastIndexOf('_'))
            .replace("_chest", "");
        int plank = switch (wood) {
            case "spruce" -> 1;
            case "birch", "cherry", "pale_oak", "bamboo" -> 2;
            case "jungle", "mangrove" -> 3;
            case "acacia" -> 4;
            case "dark_oak" -> 5;
            default -> 0;
        };
        int tint = switch (wood) {
            case "cherry" -> 0xffbbcd;
            case "mangrove" -> 0xae635a;
            case "pale_oak" -> 0xcdd0c9;
            case "bamboo" -> 0xe5d076;
            default -> 0xffffff;
        };
        if (id.endsWith("_raft")) {
            for (int i = -2; i <= 2; i++)
                box(Blocks.planks, plank, tint, i * 2.2 - 1, -8, -7.5, i * 2.2 + 1, -5.7, 7.5);
            for (int side : SIDES) box(Blocks.planks, 1, 0x99805a, -5.6, -6.2, side * 4 - .5, 5.6, -5.4, side * 4 + .5);
        } else {
            box(Blocks.planks, plank, tint, -4.5, -8, -6, 4.5, -6.5, 6);
            for (int side : SIDES) {
                box(Blocks.planks, plank, tint, side * 5 - .65, -7, -7, side * 5 + .65, -3, 7);
                box(Blocks.planks, plank, tint, -4.6, -7, side * 6.5 - .65, 4.6, -3, side * 6.5 + .65);
            }
            box(Blocks.planks, plank, tint, -4.8, -5, -.8, 4.8, -4.3, .8);
        }
        for (int side : SIDES) {
            GL11.glPushMatrix();
            GL11.glTranslatef(side * 5, -4, 0);
            GL11.glRotatef(side * 18, 0, 1, 0);
            GL11.glRotatef(-23, 1, 0, 0);
            box(Blocks.planks, plank, tint, -.4, -.4, -5, .4, .4, 4);
            box(Blocks.planks, plank, tint, -.85, -.55, 3, .85, .55, 6);
            GL11.glPopMatrix();
        }
        if (id.contains("_chest_")) {
            box(Blocks.planks, 0, 0xb78449, -2.6, -5, -5, 2.6, -1.6, .2);
            GL11.glTranslatef(0, -1.6f, -5);
            HmiPoseRenderer.apply(poses.part(0));
            box(Blocks.planks, 0, 0xbd884a, -2.6, 0, 0, 2.6, 1.2, 5.2);
            box(Blocks.iron_block, 0, 0xd4bc72, -.45, -.65, 5, .45, .55, 5.4);
        }
    }

    private static void bucket(String id, String variant, int used, int age, HmiPoseProgram poses) {
        boolean milk = id.equals("milk_bucket") && used >= 0;
        boolean fish = id.equals("cod_bucket") || id.equals("tropical_fish_bucket") || id.equals("salmon_bucket");
        mount(0, 2.5f, .75f, 90, fish ? -90 : 0, .5f);
        int metal = 0xb8bfc9;
        box(Blocks.iron_block, 0, metal, -3.5, -8, -3.5, 3.5, -6.6, 3.5);
        for (int tier = 0; tier < 2; tier++) {
            double r = 3.9 + tier * .6, y = -7 + tier * 3.4;
            for (int side : SIDES) {
                box(Blocks.iron_block, 0, metal, side * r - .45, y, -r, side * r + .45, y + 3.5, r);
                box(Blocks.iron_block, 0, metal, -r, y, side * r - .45, r, y + 3.5, side * r + .45);
            }
        }
        for (int side : SIDES)
            box(Blocks.iron_block, 0, 0x777d85, side * 4.8 - .35, -1, -.45, side * 4.8 + .35, 2.7, .45);
        box(Blocks.iron_block, 0, 0x777d85, -4.9, 2.3, -.45, 4.9, 3.05, .45);
        if (id.equals("bucket")) return;
        Block content = switch (id) {
            case "lava_bucket" -> Blocks.lava;
            case "milk_bucket", "powder_snow_bucket" -> Blocks.snow;
            default -> Blocks.water;
        };
        GL11.glPushMatrix();
        GL11.glTranslatef(0, milk ? -.65f - 5.5f * Math.min(1, used / 32f) : -.65f, 0);
        HmiPoseRenderer.apply(poses.part(0));
        box(content, 0, content == Blocks.water ? 0x5184db : 0xffffff, -4, -.5, -4, 4, 0, 4);
        if (id.equals("powder_snow_bucket")) box(Blocks.snow, 0, 0xffffff, -3, 0, -3, 2, 2, 2);
        else if (fish || id.equals("axolotl_bucket") || id.equals("pufferfish_bucket") || id.equals("tadpole_bucket"))
            swimmer(id, variant, age);
        GL11.glPopMatrix();
    }

    private static void swimmer(String id, String variant, int age) {
        float wave = (float) Math.sin(age * .23);
        GL11.glTranslatef(0, 1 + wave * .4f, 0);
        GL11.glRotatef(9 * wave, 0, 1, 0);
        boolean axolotl = id.equals("axolotl_bucket"), puffer = id.equals("pufferfish_bucket"),
            tadpole = id.equals("tadpole_bucket");
        int tint = switch (id) {
            case "axolotl_bucket" -> switch (variant) {
                    case "blue" -> 0x8773d6;
                    case "wild" -> 0x896d51;
                    case "cyan" -> 0xbbdee1;
                    case "gold" -> 0xf7d766;
                    default -> 0xf2b6c1;
                };
            case "salmon_bucket" -> 0xdc7362;
            case "tropical_fish_bucket" -> 0xf3a258;
            case "pufferfish_bucket" -> 0xecc565;
            case "tadpole_bucket" -> 0x625447;
            default -> 0xc8b694;
        };
        double h = puffer ? 2.7 : tadpole ? 1 : 1.6, w = puffer ? 2.6 : axolotl ? 1.9 : 1.2;
        box(Blocks.wool, 0, tint, -w, 0, -3, w, h * 2, 2.2);
        for (int side : SIDES) {
            box(Blocks.wool, 15, 0xffffff, side * w - .2, h, -2.7, side * w + .2, h + .65, -2);
            box(Blocks.wool, 0, tint, side * (w + 1) - 1, h, 0, side * (w + 1) + 1, h + .5, 1.6);
            if (axolotl) box(Blocks.wool, 0, tint, side * (w + 1) - .5, h, -2.5, side * (w + 1) + .5, h + 2.2, -1.8);
        }
        if (puffer) {
            for (int x : SIDES) for (int z : SIDES) {
                box(
                    Blocks.wool,
                    0,
                    0xe5ad49,
                    x * 1.6 - .24,
                    h * 2,
                    z * 1.6 - .24,
                    x * 1.6 + .24,
                    h * 2 + .9,
                    z * 1.6 + .24);
            }
        } else if (id.equals("tropical_fish_bucket")) {
            box(Blocks.wool, 0, 0xf6e9ca, -w - .03, .12, -1.5, w + .03, 2 * h - .1, -.8);
            box(Blocks.wool, 0, 0x414457, -.4, 2 * h, -1.8, .4, 2 * h + .9, 1.6);
        } else if (axolotl) {
            for (int side : SIDES) for (int limb : SIDES) {
                box(
                    Blocks.wool,
                    0,
                    tint,
                    side * (w + .2) - .5,
                    -.5,
                    limb * 1.4 - .4,
                    side * (w + .2) + .5,
                    .3,
                    limb * 1.4 + .4);
            }
        } else if (!tadpole) {
            box(
                Blocks.wool,
                0,
                id.equals("salmon_bucket") ? 0x708169 : 0xab9373,
                -.4,
                2 * h,
                -.8,
                .4,
                2 * h + .85,
                1.5);
        }
        GL11.glTranslatef(0, (float) h, 2.2f);
        GL11.glRotatef(24 * wave, 0, 1, 0);
        box(Blocks.wool, 0, tint, -.45, -h, 0, .45, h, tadpole ? 4 : 3.2);
    }

    private static void flowers(String id) {
        mount(0, -1.5f, .25f, 68, 90, .75f);
        box(Blocks.planks, 0, 0xa88059, -5, -8, -3.5, 5, -6.8, 3.5);
        for (int i : SIDES) {
            box(Blocks.planks, 0, 0xc59560, i * 5 - .55, -7, -3.8, i * 5 + .55, -3.8, 3.8);
            box(Blocks.planks, 0, 0xbd8c60, -5.4, -7, i * 3.6 - .45, 5.4, -3.8, i * 3.6 + .45);
        }
        int tint = id.endsWith("litter") ? 0xb56e3b : id.endsWith("petals") ? 0xffb8d9 : 0xffdf68;
        for (int i = 0; i < 8; i++) {
            double x = (i % 4 - 1.5) * 2.25, z = (i / 4 - .5) * 3 + .4 * Math.sin(i);
            double y = -3.7 + (i % 3) * .55;
            GL11.glPushMatrix();
            GL11.glTranslated(x, y, z);
            GL11.glRotatef(i * 41, 0, 1, 0);
            if (id.equals("minecraft:leaf_litter")) {
                GL11.glRotatef(i % 2 == 0 ? 21 : -17, 0, 0, 1);
                box(Blocks.planks, 0, i % 2 == 0 ? tint : 0x947139, -1.2, 0, -.65, 1.2, .22, .65);
                box(Blocks.planks, 0, 0x86562e, .9, 0, -.16, 1.7, .24, .16);
            } else {
                box(Blocks.planks, 0, 0x6c8e4d, -.15, -2.5, -.15, .15, 0, .15);
                for (int side : SIDES) {
                    box(Blocks.wool, 0, tint, side * .65 - .4, 0, -.3, side * .65 + .4, .35, .3);
                    box(Blocks.wool, 0, tint, -.4, 0, side * .6 - .35, .4, .35, side * .6 + .35);
                }
                box(Blocks.wool, 0, id.endsWith("petals") ? 0xffe69d : 0xb8792e, -.35, .2, -.35, .35, .55, .35);
            }
            GL11.glPopMatrix();
        }
    }

    private static void crystal(int age) {
        mount(0, 1.25f, -2, 0, 0, .5f);
        GL11.glTranslatef(0, 4.4f + (float) Math.sin(age * .1) * .5f, 0);
        GL11.glRotatef(age * 2.2f % 360, 0, 1, 0);
        GL11.glRotatef(35, 1, 0, 1);
        box(Blocks.wool, 0, 0xe76deb, -2.1, -2.1, -2.1, 2.1, 2.1, 2.1);
        for (int shell = 0; shell < 2; shell++) {
            GL11.glRotatef(28, 0, 1, 1);
            double r = 2.8 + shell * .9;
            for (int x : SIDES) for (int y : SIDES) {
                box(Blocks.quartz_block, 0, 0xf1b9ef, x * r - .18, y * r - .18, -r, x * r + .18, y * r + .18, r);
                box(Blocks.quartz_block, 0, 0xf1b9ef, x * r - .18, -r, y * r - .18, x * r + .18, r, y * r + .18);
                box(Blocks.quartz_block, 0, 0xf1b9ef, -r, x * r - .18, y * r - .18, r, x * r + .18, y * r + .18);
            }
        }
    }

    static void box(Block block, int meta, int rgb, double x, double y, double z, double x2, double y2, double z2) {
        var tess = Tessellator.instance;
        var icon = block.getIcon(1, meta);
        GL11.glColor3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f);
        FACES.setRenderBounds(0, 0, 0, 1, 1, 1);
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glScaled(x2 - x, y2 - y, z2 - z);
        tess.startDrawingQuads();
        tess.setNormal(0, -1, 0);
        FACES.renderFaceYNeg(block, 0, 0, 0, icon);
        tess.setNormal(0, 1, 0);
        FACES.renderFaceYPos(block, 0, 0, 0, icon);
        tess.setNormal(0, 0, -1);
        FACES.renderFaceZNeg(block, 0, 0, 0, icon);
        tess.setNormal(0, 0, 1);
        FACES.renderFaceZPos(block, 0, 0, 0, icon);
        tess.setNormal(-1, 0, 0);
        FACES.renderFaceXNeg(block, 0, 0, 0, icon);
        tess.setNormal(1, 0, 0);
        FACES.renderFaceXPos(block, 0, 0, 0, icon);
        tess.draw();
        GL11.glPopMatrix();
    }

    private DefaultItemModels() {}
}
