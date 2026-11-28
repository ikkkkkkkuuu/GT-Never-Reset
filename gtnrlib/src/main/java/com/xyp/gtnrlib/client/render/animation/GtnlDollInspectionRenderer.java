package com.xyp.gtnrlib.client.render.animation;

import java.nio.FloatBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.client.animation.FirstPersonItemActions.Scene;
import com.xyp.gtnrlib.mod.ModList.ModIds;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class GtnlDollInspectionRenderer implements FirstPersonActionEffects.Effect {

    private static final ResourceLocation GTNL_SKIN = new ResourceLocation(ModIds.GT_NOT_LEISURE, "models/skin.png");
    private static final double RIFT_OPEN = 1.46, ARM_REACH = 1.80, ARM_GRAB = 2.08, ARM_RELEASE = 2.60;
    private static final double ARM_WINDUP = 2.97, LENS_HIT = 3.12, WITHDRAW = 3.82, RIFT_CLOSE = 4.06;
    private static final float RIFT_SCREEN_X = 1.015f, RIFT_SCREEN_Y = .09f, RIFT_DEPTH = -1.02f;
    private static final float LENS_SCREEN_X = .40f, LENS_SCREEN_Y = .11f, LENS_DEPTH = -.83f;
    private static final float MODEL_UNIT = 1f / 16;
    private static final int RIM_POINTS = 13, SHARDS = 11, LENS_CRACKS = 8;
    private static final ArmModel GTNL_ARM = new ArmModel(3, 64);
    private static final ArmModel PLAYER_ARM = new ArmModel(4, 64), LEGACY_ARM = new ArmModel(4, 32);
    private static final FloatBuffer MATRIX_VALUES = BufferUtils.createFloatBuffer(16);
    private static final Matrix4f HAND_TO_SCREEN = new Matrix4f(), SCREEN_TO_HAND = new Matrix4f();
    private static final Matrix4f ARM_MOUNT = new Matrix4f();
    private static final Quaternionf ARM_ROTATION = new Quaternionf();
    private static final Vector3f REACH = new Vector3f(), DOLL_GRIP = new Vector3f();
    private static final Vector3f RIFT_ORIGIN = new Vector3f(), LENS_CONTACT = new Vector3f(),
        SCREEN_RAY = new Vector3f();

    @Override
    public boolean renderItem(Scene scene, ItemRenderer renderer, ItemStack stack, IItemRenderer custom) {
        if (custom == null) return false;
        // Center GTNL's seated OBJ above the palms and cancel its native first-person display transform.
        GL11.glScalef(.49f, .49f, .49f);
        GL11.glRotatef(-12, 1, 0, 0);
        GL11.glTranslatef(0, -.56f, .125f);
        GL11.glRotatef(90, 0, 1, 0);
        GL11.glTranslatef(-.3f, .1f, -.3f);
        GL11.glScalef(2f / 3, 2f / 3, 2f / 3);
        var type = ItemRenderType.EQUIPPED_FIRST_PERSON;
        if (custom.shouldUseRenderHelper(type, stack, ItemRendererHelper.EQUIPPED_BLOCK)) {
            GL11.glTranslatef(.5f, .5f, .5f);
        } else HmiPoseRenderer.undoForgeItemMount();
        renderer.renderItem(Minecraft.getMinecraft().thePlayer, stack.copy(), 0, type);
        return true;
    }

    @Override
    public void beforeHands(Scene scene, ItemStack doll, Matrix4fc itemPose) {
        double seconds = effectTime(scene);
        if (seconds < RIFT_OPEN || seconds >= RIFT_CLOSE) return;
        updateRiftPlacement();
        float opening = ease(seconds, RIFT_OPEN, 1.81) * (1 - ease(seconds, 3.62, RIFT_CLOSE));
        prepareEffects();
        GL11.glDepthMask(false);
        float width = .14f * opening, height = .37f * opening;
        drawPortal(seconds, opening, width, height);
        drawShards(seconds, opening, width, height);
        if (seconds >= ARM_REACH && seconds < WITHDRAW) drawArm(seconds, doll, itemPose);
    }

    @Override
    public void overlay(Scene scene, int width, int height) {
        double seconds = effectTime(scene);
        if (seconds < LENS_HIT || seconds >= 3.59 || width <= 0 || height <= 0) return;
        float age = (float) (seconds - LENS_HIT);
        float alpha = 1 - ease(seconds, 3.20, 3.59);
        float aspect = (float) width / height;
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(-aspect, aspect, -1, 1, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        prepareEffects();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glTranslatef(LENS_SCREEN_X * aspect, LENS_SCREEN_Y, 0);
        GL11.glRotatef(-9 * (1 - ease(seconds, LENS_HIT, 3.33)), 0, 0, 1);
        float pulse = 1 - ease(seconds, LENS_HIT, 3.23);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4f(.83f, .65f, 1, .42f * pulse);
        GL11.glVertex2f(0, 0);
        GL11.glColor4f(.43f, .16f, .72f, 0);
        for (int i = 0; i <= LENS_CRACKS; i++) {
            double angle = i * Math.PI * 2 / LENS_CRACKS;
            GL11.glVertex2d(Math.cos(angle) * .85, Math.sin(angle) * .85);
        }
        GL11.glEnd();
        float spread = Math.min(1, .35f + 12 * age);
        lensCracks(spread, .40f * alpha, 3, .38f, .14f, .68f);
        lensCracks(spread, .86f * alpha, 1, .95f, .82f, 1);
    }

    private static double effectTime(Scene scene) {
        double seconds = scene.seconds();
        double open = scene.markerTime("doll_rift_open", RIFT_OPEN);
        double hit = scene.markerTime("doll_lens_hit", LENS_HIT);
        if (open >= hit || open == RIFT_OPEN && hit == LENS_HIT) return seconds;
        if (seconds <= open) return RIFT_OPEN + (seconds - open);
        if (seconds >= hit) return LENS_HIT + (seconds - hit);
        // Fit the intervening reach and struggle between the markers, then leave the withdrawal at its authored pace.
        return RIFT_OPEN + (LENS_HIT - RIFT_OPEN) * (seconds - open) / (hit - open);
    }

    private static void updateRiftPlacement() {
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, MATRIX_VALUES);
        HAND_TO_SCREEN.set(MATRIX_VALUES);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX_VALUES);
        HAND_TO_SCREEN.mul(SCREEN_TO_HAND.set(MATRIX_VALUES))
            .invert(SCREEN_TO_HAND);
        placeAtDepth(RIFT_SCREEN_X, RIFT_SCREEN_Y, RIFT_DEPTH, RIFT_ORIGIN);
        placeAtDepth(LENS_SCREEN_X, LENS_SCREEN_Y, LENS_DEPTH, LENS_CONTACT);
    }

    private static void placeAtDepth(float x, float y, float depth, Vector3f position) {
        SCREEN_TO_HAND.transformProject(x, y, -1, SCREEN_RAY);
        SCREEN_TO_HAND.transformProject(x, y, 0, position);
        position.sub(SCREEN_RAY)
            .mul((depth - SCREEN_RAY.z) / position.z)
            .add(SCREEN_RAY);
    }

    private static void drawPortal(double seconds, float opening, float width, float height) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4f(.017f, .006f, .034f, .98f * opening);
        GL11.glVertex3f(RIFT_ORIGIN.x, RIFT_ORIGIN.y, RIFT_ORIGIN.z);
        GL11.glColor4f(.11f, .025f, .21f, .96f * opening);
        for (int i = 0; i <= RIM_POINTS; i++) rimPoint(i % RIM_POINTS, width, height, 1);
        GL11.glEnd();

        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i <= RIM_POINTS; i++) {
            int point = i % RIM_POINTS;
            float flicker = (float) (.77 + .23 * Math.sin(seconds * 19 + point * 4));
            GL11.glColor4f(.75f, .28f, 1, opening * flicker);
            rimPoint(point, width, height, 1);
            GL11.glColor4f(.44f, .08f, .82f, 0);
            rimPoint(point, width, height, 1.3f);
        }
        GL11.glEnd();

        GL11.glLineWidth(1.5f);
        GL11.glColor4f(.94f, .70f, 1, opening);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < RIM_POINTS; i++) rimPoint(i, width, height, 1);
        GL11.glEnd();
    }

    private static void rimPoint(int index, float width, float height, float expansion) {
        double angle = index * Math.PI * 2 / RIM_POINTS;
        double radius = expansion * (.81 + .26 * Math.sin(index * 4.8));
        GL11.glVertex3d(
            RIFT_ORIGIN.x + radius * (Math.cos(angle) * width - Math.sin(angle) * height * .27),
            RIFT_ORIGIN.y + Math.sin(angle) * height * radius,
            RIFT_ORIGIN.z);
    }

    private static void drawShards(double seconds, float opening, float width, float height) {
        float spread = ease(seconds, RIFT_OPEN, 2.30);
        GL11.glBegin(GL11.GL_TRIANGLES);
        for (int i = 0; i < SHARDS; i++) {
            double angle = i * 2.399963 + .23 * seconds;
            double distance = 1.17 + .58 * spread + .22 * Math.sin(i * 13);
            double x = RIFT_ORIGIN.x + Math.cos(angle) * width * distance;
            double y = RIFT_ORIGIN.y + Math.sin(angle) * height * distance;
            float size = (.025f + .009f * (i % 4)) * opening;
            GL11.glColor4f(.78f, .48f, 1, .77f * opening);
            GL11.glVertex3d(x + Math.cos(angle) * size, y + Math.sin(angle) * size, RIFT_ORIGIN.z + .06);
            GL11.glColor4f(.29f, .06f, .55f, .36f * opening);
            GL11.glVertex3d(x - Math.sin(angle) * size, y + Math.cos(angle) * size, RIFT_ORIGIN.z + .06);
            GL11.glVertex3d(x + Math.sin(angle) * size * .5, y - Math.cos(angle) * size * .5, RIFT_ORIGIN.z + .06);
        }
        GL11.glEnd();
    }

    private static void drawArm(double seconds, ItemStack doll, Matrix4fc itemPose) {
        float grab = ease(seconds, ARM_REACH, ARM_GRAB) * (1 - ease(seconds, ARM_RELEASE, 2.78));
        float windup = ease(seconds, 2.71, ARM_WINDUP) * (1 - ease(seconds, ARM_WINDUP, LENS_HIT));
        float punch = ease(seconds, ARM_WINDUP, LENS_HIT) * (1 - ease(seconds, LENS_HIT, 3.54));
        float extension = ease(seconds, ARM_REACH, 1.98) * (1 - ease(seconds, 3.35, WITHDRAW));
        itemPose.transformPosition(.18f, .11f, .13f, DOLL_GRIP);
        float gripDepth = Math.max(RIFT_DEPTH - .12f, Math.min(RIFT_DEPTH + .12f, DOLL_GRIP.z));
        // Match the doll's projected grip while keeping the reach mostly along the side of the screen.
        HAND_TO_SCREEN.transformProject(DOLL_GRIP);
        placeAtDepth(DOLL_GRIP.x, DOLL_GRIP.y, gripDepth, DOLL_GRIP);
        REACH.set(RIFT_ORIGIN.x - .22f + .09f * windup, RIFT_ORIGIN.y + .17f * windup, RIFT_DEPTH + .02f)
            .lerp(DOLL_GRIP, grab)
            .lerp(LENS_CONTACT, punch)
            .sub(RIFT_ORIGIN)
            .mul(extension);
        float length = REACH.length();
        if (length < .025f) return;

        var mc = Minecraft.getMinecraft();
        var tag = doll.getTagCompound();
        String ownerName = tag == null ? "" : tag.getString("SkullOwner");
        var owner = ownerName.isEmpty() ? null : mc.theWorld.getPlayerEntityByName(ownerName);
        boolean liveSkin = owner instanceof AbstractClientPlayer;
        var skin = liveSkin ? ((AbstractClientPlayer) owner).getLocationSkin() : GTNL_SKIN;
        GL11.glPushMatrix();
        try {
            ARM_ROTATION.rotationTo(0, 1, 0, REACH.x, REACH.y, REACH.z)
                .rotateY(.40f + .33f * punch);
            HmiPoseRenderer.apply(
                ARM_MOUNT.translation(RIFT_ORIGIN)
                    .rotate(ARM_ROTATION)
                    .scale(1.45f, length / (12 * MODEL_UNIT), 1.28f));
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, .1f);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glColor4f(.9f, .80f, 1, 1);
            mc.getTextureManager()
                .bindTexture(skin);
            ArmModel model = GTNL_ARM;
            if (liveSkin) {
                int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
                int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
                model = height < width ? LEGACY_ARM : PLAYER_ARM;
            }
            model.render();
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static void lensCracks(float spread, float alpha, float thickness, float r, float g, float b) {
        GL11.glLineWidth(thickness);
        GL11.glColor4f(r, g, b, alpha);
        GL11.glBegin(GL11.GL_LINES);
        for (int i = 0; i < LENS_CRACKS; i++) {
            double angle = i * Math.PI * 2 / LENS_CRACKS + .19 * Math.sin(i * 4.6);
            double length = spread * (.30 + .30 * (.5 + .5 * Math.cos(i * 2.1)));
            double x = Math.cos(angle), y = Math.sin(angle);
            double nearX = (x * .39 - y * .15) * length, nearY = (y * .39 + x * .15) * length;
            double farX = x * length, farY = y * length;
            GL11.glVertex2d(0, 0);
            GL11.glVertex2d(nearX, nearY);
            GL11.glVertex2d(nearX, nearY);
            GL11.glVertex2d(farX, farY);
            GL11.glVertex2d(nearX, nearY);
            GL11.glVertex2d((x * .66 - y * .32) * length, (y * .66 + x * .32) * length);
        }
        GL11.glEnd();
    }

    private static void prepareEffects() {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);
    }

    private static float ease(double seconds, double start, double end) {
        float t = (float) Math.max(0, Math.min(1, (seconds - start) / (end - start)));
        return t * t * (3 - 2 * t);
    }

    private static final class ArmModel {

        private final ModelRenderer arm, sleeve;

        private ArmModel(int width, int textureHeight) {
            var model = new ModelBase() {};
            arm = new ModelRenderer(model, 40, 16).setTextureSize(64, textureHeight);
            arm.addBox(-width / 2f, 0, -2, width, 12, 4);
            sleeve = textureHeight == 64 ? new ModelRenderer(model, 40, 32).setTextureSize(64, 64) : null;
            if (sleeve != null) sleeve.addBox(-width / 2f, 0, -2, width, 12, 4, .25f);
        }

        private void render() {
            arm.render(MODEL_UNIT);
            if (sleeve != null) sleeve.render(MODEL_UNIT);
        }
    }
}
