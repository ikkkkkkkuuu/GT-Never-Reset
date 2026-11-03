package com.xyp.gtnrlib.client.render.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.api.animation.AnimationClip.*;
import com.xyp.gtnrlib.client.animation.FirstPersonAnimations;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Runs inside the original branch's pushed model-view matrix, once before the first item pass. */
@SideOnly(Side.CLIENT)
public final class FirstPersonPoseRenderer {

    private static final FirstPersonAnimations ANIMATIONS = FirstPersonAnimations.INSTANCE;
    private static final Bone BODY = new Bone("body"), RIGHT = new Bone("right_arm"), LEFT = new Bone("left_arm"),
        ITEM = new Bone("right_item");
    private static ModelBiped arms;

    public static void held(ItemStack stack, float partial) {
        if (!ANIMATIONS.applies(stack)) return;
        root(partial);
        arm(false, partial);
        if (ANIMATIONS.dualHanded()) arm(true, partial);
        RIGHT.apply(partial);
        ITEM.apply(partial);
    }

    public static void empty(float partial) {
        if (!ANIMATIONS.applies(null)) return;
        root(partial);
        if (ANIMATIONS.dualHanded()) arm(true, partial);
        RIGHT.apply(partial);
    }

    private static void root(float partial) {
        GL11.glRotated(-ANIMATIONS.yaw(partial) * .35, 0, 1, 0);
        GL11.glRotated(-ANIMATIONS.pitch(partial) * .35, 1, 0, 0);
        BODY.apply(partial);
    }

    private static void arm(boolean left, float partial) {
        var mc = Minecraft.getMinecraft();
        if (arms == null) arms = new ModelBiped(0);
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_ENABLE_BIT);
        try {
            mc.getTextureManager()
                .bindTexture(mc.thePlayer.getLocationSkin());
            GL11.glColor4f(1, 1, 1, 1);
            (left ? LEFT : RIGHT).apply(partial);
            GL11.glTranslated(left ? -.2 : .12, .04, .05);
            GL11.glRotatef(left ? 45 : -48, 0, 0, 1);
            GL11.glRotatef(-64, 1, 0, 0);
            GL11.glTranslated(left ? -.3125 : .3125, -.72, 0);
            (left ? arms.bipedLeftArm : arms.bipedRightArm).render(1F / 16);
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    private record Bone(Channel position, Channel rotation, Channel scale) {

        Bone(String id) {
            this(
                new Channel(id, Property.POSITION),
                new Channel(id, Property.ROTATION),
                new Channel(id, Property.SCALE));
        }

        void apply(float partial) {
            Vec p = ANIMATIONS.sample(position, partial), r = ANIMATIONS.sample(rotation, partial),
                s = ANIMATIONS.sample(scale, partial);
            GL11.glTranslated(p.x() / 16, -p.y() / 16, p.z() / 16);
            GL11.glRotated(r.z(), 0, 0, 1);
            GL11.glRotated(-r.y(), 0, 1, 0);
            GL11.glRotated(-r.x(), 1, 0, 0);
            GL11.glScaled(s.x(), s.y(), s.z());
        }
    }
}
