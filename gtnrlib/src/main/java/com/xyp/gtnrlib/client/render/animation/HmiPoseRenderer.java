package com.xyp.gtnrlib.client.render.animation;

import java.io.IOException;
import java.nio.FloatBuffer;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemCloth;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.xyp.gtnrlib.client.animation.FirstPersonAnimations;
import com.xyp.gtnrlib.client.animation.HmiFirstPerson;
import com.xyp.gtnrlib.client.animation.HmiItemBindings;
import com.xyp.gtnrlib.client.animation.ToolGrip;
import com.xyp.gtnrlib.compat.hmi.HmiPoseProgram;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class HmiPoseRenderer {

    private static final FloatBuffer BUFFER = BufferUtils.createFloatBuffer(16);
    private static final HmiFirstPerson HMI = HmiFirstPerson.INSTANCE;
    private static final ResourceLocation MAP = new ResourceLocation("textures/map/map_background.png");
    // The 5.1.1 hand pass submits held items with THIRD_PERSON_RIGHT_HAND.
    private static final Matrix4fc HANDHELD = display(0, 4, .5f, 0, -90, 55, .85f);
    private static final Matrix4fc ROD = display(0, 4, 2.5f, 0, 90, 55, .85f);
    private static final Matrix4fc BOW = display(-1, -2, 2.5f, -80, 260, -40, .9f);
    private static final Matrix4fc GENERATED = display(0, 3, 1, 0, 0, 0, .55f);
    private static HmiItemModels models;
    private static IIcon bowIcon;
    private static ItemStack mountedStack;

    /** True only inside Forge's first-person draw with the adapter's native model mount active. */
    public static boolean renderingMountedStack(ItemStack stack) {
        return stack != null && mountedStack == stack;
    }

    public static IIcon bowIcon(IIcon original) {
        return bowIcon == null ? original : bowIcon;
    }

    public static void install(HmiPoseProgram.Scripts scripts) throws IOException {
        var next = scripts == null || scripts == HmiPoseProgram.Scripts.BUILT_IN ? null
            : new HmiItemModels(scripts.assets());
        HMI.install(scripts);
        if (models != null) models.close();
        models = next;
    }

    public static boolean render(ItemRenderer renderer, ItemStack stack, float partial, float dip) {
        if (!HMI.enabled() || FirstPersonAnimations.INSTANCE.applies(stack) || !HMI.sample(stack, partial, dip))
            return false;
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT
                | GL11.GL_TEXTURE_BIT
                | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT);
        try {
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            var mc = Minecraft.getMinecraft();
            GL11.glPushMatrix();
            try {
                // Preserve the reference's wide-screen grip framing on narrower legacy windows.
                float aspect = (float) mc.displayWidth / mc.displayHeight;
                GL11.glTranslated(-.40 * Math.max(0, 16f / 9 - aspect), 0, -.10);
                if (HMI.handheldTool() instanceof HmiItemBindings.TwoHandedModel rig) {
                    apply(HMI.item);
                    try {
                        rig.renderGripped(stack, HMI.forgeRenderer(), partial, HmiPoseRenderer::gripHand);
                    } catch (RuntimeException ex) {
                        HMI.disableTool(stack, ex);
                        return false;
                    }
                } else {
                    arm(false, stack != null);
                    arm(true, false);
                    if (stack != null) {
                        apply(HMI.item);
                        if (stack.getItem() instanceof ItemMap map) map(mc, stack, map);
                        else {
                            GL11.glPushMatrix();
                            try {
                                draw(renderer, mc, stack);
                            } finally {
                                GL11.glPopMatrix();
                            }
                            glow(stack);
                        }
                    }
                }
            } finally {
                GL11.glPopMatrix();
            }
            drawScreenParticles();
        } finally {
            GL11.glPopAttrib();
            if (stack != null && stack.getItem() instanceof ItemCloth) GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.disableStandardItemLighting();
        }
        return true;
    }

    private static void arm(boolean left, boolean holding) {
        int side = left ? -1 : 1;
        GL11.glPushMatrix();
        try {
            apply(left ? HMI.left : HMI.right);
            if (holding) {
                GL11.glTranslated(1.5 * side, -.3, -.6);
                GL11.glTranslated(.5 * side, .5, .5);
                GL11.glRotatef(15, 1, 0, 0);
                GL11.glRotatef(35 * side, 0, 1, 0);
                GL11.glRotatef(-65 * side, 0, 0, 1);
                GL11.glTranslated(-.5 * side, -.5, -.5);
                GL11.glScalef(.9f, .9f, .9f);
            }
            GL11.glTranslated(.64 * side, -.6, -.72);
            GL11.glRotatef(45 * side, 0, 1, 0);
            GL11.glTranslated(-side, 3.6, 3.5);
            GL11.glRotatef(120 * side, 0, 0, 1);
            GL11.glRotatef(200, 1, 0, 0);
            GL11.glRotatef(-135 * side, 0, 1, 0);
            GL11.glTranslated(5.6 * side, 0, 0);
            FirstPersonArmRenderer.render(left, side * .1f);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static void gripHand(boolean left) {
        GL11.glPushMatrix();
        try {
            apply(com.xyp.gtnrlib.client.animation.FirstPersonItemActions.INSTANCE.grip(left));
            GL11.glTranslatef((left ? -6 : 6) / 16f, -10f / 16, 0);
            FirstPersonArmRenderer.render(left, 0);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static Matrix4fc display(float x, float y, float z, float pitch, float yaw, float roll, float scale) {
        return new Matrix4f().translation(x / 16, y / 16, z / 16)
            .rotateXYZ((float) Math.toRadians(pitch), (float) Math.toRadians(yaw), (float) Math.toRadians(roll))
            .scale(scale);
    }

    static void apply(Matrix4fc matrix) {
        GL11.glMultMatrix(matrix.get(BUFFER));
    }

    private static void draw(ItemRenderer renderer, Minecraft mc, ItemStack stack) {
        int used = mc.thePlayer.isUsingItem() ? mc.thePlayer.getItemInUseDuration() : -1;
        var tool = HMI.handheldTool();
        var custom = HMI.forgeRenderer();
        if (models == null
            ? DefaultItemModels.draw(HMI.itemId(), HMI.modelVariant(), used, mc.thePlayer.ticksExisted, HMI.program())
            : models.draw(HMI.itemId(), HMI.modelVariant(), used, mc.thePlayer.ticksExisted, HMI.program())) return;
        var mount = tool == null ? null : tool.equippedMount(stack);
        if (mount != null) {
            apply(mount);
            var previous = mountedStack;
            mountedStack = stack;
            try {
                renderer.renderItem(mc.thePlayer, stack, 0, ItemRenderType.EQUIPPED_FIRST_PERSON);
            } finally {
                mountedStack = previous;
            }
            return;
        }
        Item held = stack.getItem();
        boolean cube = held instanceof ItemBlock && RenderBlocks.renderItemIn3d(
            Block.getBlockFromItem(held)
                .getRenderType());
        if (HMI.blockPose()) blockMount(held);
        else apply(switch (HMI.itemId()) {
            case "minecraft:bow" -> BOW;
            case "minecraft:fishing_rod", "minecraft:carrot_on_a_stick" -> ROD;
            default -> HMI.displayGrip() != null || tool != null ? HANDHELD : GENERATED;
        });
        // Forge already centers models that request its equipped-block helper.
        boolean blockHelper = custom != null && tool == null
            && custom
                .shouldUseRenderHelper(ItemRenderType.EQUIPPED_FIRST_PERSON, stack, ItemRendererHelper.EQUIPPED_BLOCK);
        if (!cube && !blockHelper) {
            // Center the legacy reversed-U extruded card in model space and undo renderItem's fixed sprite mount.
            GL11.glRotatef(180, 0, 1, 0);
            GL11.glTranslated(-.5, -.5, .03125);
            if (tool != null && tool.spriteMount() != null) apply(tool.spriteMount());
            else if (!HMI.blockPose()) centerHandle(HMI.displayGrip());
            GL11.glTranslated(.9375, .0625, 0);
            GL11.glRotatef(25, 0, 0, 1);
            GL11.glRotatef(-50, 0, 1, 0);
            GL11.glScalef(2f / 3, 2f / 3, 2f / 3);
            GL11.glTranslated(0, .3, 0);
        }
        if (custom != null) {
            // Forge invokes the owner's renderer once; it draws all material layers itself.
            renderer.renderItem(mc.thePlayer, stack, 0, ItemRenderType.EQUIPPED_FIRST_PERSON);
            return;
        }
        int passes = held.requiresMultipleRenderPasses() ? held.getRenderPasses(stack.getItemDamage()) : 1;
        for (int pass = 0; pass < passes; pass++) {
            int rgb = held.getColorFromItemStack(stack, pass);
            GL11.glColor4f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f, 1);
            if (held == Items.bow) {
                int stage = HMI.program()
                    .bowStage();
                bowIcon = stage < 0 ? held.getIconFromDamage(stack.getItemDamage())
                    : Items.bow.getItemIconForUseDuration(stage);
            }
            try {
                renderer.renderItem(mc.thePlayer, stack, pass, ItemRenderType.EQUIPPED_FIRST_PERSON);
            } finally {
                bowIcon = null;
            }
        }
    }

    private static void centerHandle(ToolGrip grip) {
        if (grip == null || grip == ToolGrip.HANDHELD) return;
        // Distances in the legacy 16px sprite. Offset perpendicular to its diagonal and in depth, so the handle
        // crosses the distal 4x4x4 skin cube without changing the reference shaft angle or sliding along the grip.
        float across = switch (grip) {
            case SWORD -> -.13f;
            case SHOVEL -> -.15f;
            default -> .38f;
        };
        float depth = grip == ToolGrip.SHOVEL ? -1.55f : .74f;
        GL11.glTranslatef(across / 16, across / 16, depth / 16);
    }

    private static void blockMount(Item held) {
        GL11.glTranslated(.22, .25, .2);
        if (Block.getBlockFromItem(held) instanceof net.minecraft.block.BlockTorch || HMI.isLantern()) {
            GL11.glTranslated(-.05, 0, 0);
            GL11.glScalef(1.75f, 1.75f, 1.75f);
        } else GL11.glTranslated(-.25, -.05, 0);
        GL11.glScalef(.3f, .3f, .3f);
        GL11.glTranslated(-.4, .05, -.2);
    }

    private static void glow(ItemStack stack) {
        var glow = HMI.program()
            .glow();
        if (glow == null) return;
        if (HMI.blockPose()) {
            blockMount(stack.getItem());
            GL11.glTranslated(-.5, -.5, -.5);
        }
        GL11.glTranslated(glow.x(), glow.y(), glow.z());
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, BUFFER);
        float radius = .25f * glow.size()
            * (float) Math
                .sqrt(BUFFER.get(0) * BUFFER.get(0) + BUFFER.get(1) * BUFFER.get(1) + BUFFER.get(2) * BUFFER.get(2));
        float x = BUFFER.get(12), y = BUFFER.get(13), z = BUFFER.get(14);
        GL11.glLoadIdentity();
        particleBlend(GL11.GL_ONE);
        GL11.glColor4f(1, 1, 1, glow.alpha());
        if (bindParticle(glow.texture())) quad(x, y, z, radius);
    }

    private static void drawScreenParticles() {
        int count = HMI.program()
            .screenParticleCount();
        if (count == 0) return;
        particleBlend(GL11.GL_ONE_MINUS_SRC_ALPHA);
        for (int i = 0; i < count; i++) {
            var p = HMI.program()
                .screenParticle(i);
            if (!bindParticle(p.texture())) continue;
            GL11.glColor4f(1, 1, 1, p.alpha());
            quad(p.x(), p.y(), p.z(), p.size() * .25f);
        }
    }

    private static boolean bindParticle(String texture) {
        if (models != null) return models.bindParticle(texture);
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(new ResourceLocation(texture));
        return true;
    }

    private static void particleBlend(int destination) {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, destination);
        GL11.glDepthMask(false);
    }

    private static void quad(float x, float y, float z, float radius) {
        var mesh = Tessellator.instance;
        mesh.startDrawingQuads();
        mesh.addVertexWithUV(x - radius, y - radius, z, 0, 1);
        mesh.addVertexWithUV(x + radius, y - radius, z, 1, 1);
        mesh.addVertexWithUV(x + radius, y + radius, z, 1, 0);
        mesh.addVertexWithUV(x - radius, y + radius, z, 0, 0);
        mesh.draw();
    }

    private static void map(Minecraft mc, ItemStack stack, ItemMap map) {
        GL11.glTranslated(-.05, .2, .1);
        GL11.glRotatef(-12, 0, 1, 0);
        GL11.glRotatef(180, 0, 1, 0);
        GL11.glRotatef(180, 0, 0, 1);
        GL11.glScalef(.38f, .38f, .38f);
        GL11.glTranslated(-.5, -.5, 0);
        GL11.glScalef(1f / 128, 1f / 128, 1f / 128);
        mc.getTextureManager()
            .bindTexture(MAP);
        var mesh = Tessellator.instance;
        mesh.startDrawingQuads();
        mesh.addVertexWithUV(-7, 135, 0, 0, 1);
        mesh.addVertexWithUV(135, 135, 0, 1, 1);
        mesh.addVertexWithUV(135, -7, 0, 1, 0);
        mesh.addVertexWithUV(-7, -7, 0, 0, 0);
        mesh.draw();
        var data = map.getMapData(stack, mc.theWorld);
        if (data != null) mc.entityRenderer.getMapItemRenderer()
            .func_148250_a(data, false);
    }

    private HmiPoseRenderer() {}
}
