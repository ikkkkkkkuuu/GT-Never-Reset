package com.xyp.gtnrlib.qa;

import java.nio.FloatBuffer;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.IItemRenderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.client.animation.HmiFirstPerson;
import com.xyp.gtnrlib.client.animation.ToolGrip;
import com.xyp.gtnrlib.client.render.animation.HmiPoseRenderer;

/** Probes the final skin and sprite matrices in the client, including the model pivot and Forge's equipped helper. */
final class AnimationGripChecks implements IItemRenderer {

    private final FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
    private Matrix4f arm, sprite;

    static boolean covers(ItemStack stack) {
        if (stack == null) return false;
        var item = stack.getItem();
        var family = ToolGrip.resolve(stack);
        return family != null && family != ToolGrip.HANDHELD || item == Items.bow
            || item == Items.fishing_rod
            || item == Items.iron_ingot;
    }

    static void measure(AnimationClientChecks qa, Minecraft mc, ItemStack held, String name, boolean rest)
        throws Exception {
        var probe = new AnimationGripChecks();
        var field = HmiPoseRenderer.class.getDeclaredField("arms");
        field.setAccessible(true);
        ModelBiped model = (ModelBiped) field.get(null);
        var hand = new ModelRenderer(model) {

            @Override
            public void render(float scale) {
                probe.arm = probe.modelView();
            }
        };
        var renderer = new ItemRenderer(mc) {

            @Override
            public void renderItem(EntityLivingBase owner, ItemStack stack, int pass, ItemRenderType type) {
                ForgeHooksClient.renderEquippedItem(type, probe, null, owner, stack);
                super.renderItem(owner, stack, pass, type);
            }
        };
        model.bipedRightArm.addChild(hand);
        try {
            qa.draw(
                mc,
                "grip-" + name,
                .5f,
                () -> {
                    if (!HmiPoseRenderer.render(renderer, held, .5f, 0))
                        throw new AssertionError("Missing grip " + name);
                });
        } finally {
            model.bipedRightArm.childModels.remove(hand);
            model.boxList.remove(hand);
        }
        // 1.21.11 thirdperson_righthand axes, read independently from the four source models. Legacy U and Z reverse.
        boolean shaft = ToolGrip.resolve(held) != null;
        Vector3f up, normal;
        if (shaft) {
            // A +45 degree blade in the modern image, followed by Z=55 and Y=-90.
            up = new Vector3f(0, .9848078f, -.1736482f);
            normal = new Vector3f(-1, 0, 0);
        } else if (held.getItem() == Items.bow) {
            up = new Vector3f(-.1116189f, .7564274f, -.6444834f);
            normal = new Vector3f(-.9848078f, -.1710101f, -.0301537f);
        } else if (held.getItem() == Items.fishing_rod) {
            up = new Vector3f(0, .5735764f, .819152f);
            normal = new Vector3f(1, 0, 0);
        } else {
            up = new Vector3f(0, 1, 0);
            normal = new Vector3f(0, 0, 1);
        }
        double axisError = probe.angle(new Vector3f(shaft ? -1 : 0, 1, 0), up);
        double faceError = probe.angle(new Vector3f(0, 0, -1), normal);
        qa.require(
            Math.max(axisError, faceError) < .05,
            String.format(Locale.ROOT, "%s model vs HMI axis %.4f / face %.4f degrees", name, axisError, faceError));
        if (!shaft || !rest) return;
        // Distal 4x4x4 skin cube: [-3,1] x [6,10] x [-2,2] before scaling by 1/16.
        var fist = probe.arm.transformPosition(new Vector3f(-1f / 16, 8f / 16, 0));
        var pixel = probe.sprite.invert(new Matrix4f())
            .transformPosition(fist)
            .mul(16);
        // Sprite U is reversed. Read the diagonal through the actual 16px sword hilt / wooden tool handles.
        float line = ToolGrip.resolve(held) == ToolGrip.SWORD ? 16 : 15;
        double miss = Math.hypot((pixel.x + pixel.y - line) / Math.sqrt(2), pixel.z + .5);
        double along = (16 - pixel.x + pixel.y) / 2;
        qa.require(
            along > 1 && along < 5.5 && miss < .12,
            String.format(
                Locale.ROOT,
                "%s fist in sprite (%.3f, %.3f, %.3f), shaft offset %.3f texels",
                name,
                pixel.x,
                pixel.y,
                pixel.z,
                miss));
    }

    private double angle(Vector3f meshAxis, Vector3f referenceAxis) {
        sprite.transformDirection(meshAxis)
            .normalize();
        HmiFirstPerson.INSTANCE.item.transformDirection(referenceAxis)
            .normalize();
        return Math.toDegrees(
            Math.atan2(
                new Vector3f(meshAxis).cross(referenceAxis)
                    .length(),
                meshAxis.dot(referenceAxis)));
    }

    private Matrix4f modelView() {
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
        return new Matrix4f().set(buffer);
    }

    @Override
    public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
        return type == ItemRenderType.EQUIPPED_FIRST_PERSON;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
        return false;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        sprite = modelView();
    }
}
