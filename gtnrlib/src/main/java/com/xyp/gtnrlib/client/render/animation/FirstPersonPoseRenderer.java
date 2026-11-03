package com.xyp.gtnrlib.client.render.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.api.animation.AnimationClip.Channel;
import com.xyp.gtnrlib.api.animation.AnimationClip.Property;
import com.xyp.gtnrlib.api.animation.AnimationClip.Vec;
import com.xyp.gtnrlib.client.animation.FirstPersonAnimations;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Runs inside the original branch's pushed model-view matrix, once before the first item pass. */
@SideOnly(Side.CLIENT)
public final class FirstPersonPoseRenderer {

    private static final FirstPersonAnimations ANIMATIONS = FirstPersonAnimations.INSTANCE;
    private static final Bone BODY = new Bone("body"), RIGHT = new Bone("right_arm"), LEFT = new Bone("left_arm"),
        ITEM = new Bone("item_right"), GRIP = new Bone("itemgrip_right");
    private static ModelBiped arms;

    public static void held(ItemStack stack, float partial) {
        if (!ANIMATIONS.applies(stack)) return;
        // Move the grip inward so there is room for the support hand inside vanilla's item matrix.
        GL11.glTranslated(-.65, .32, -.2);
        root(partial);
        arm(false, partial);
        if (ANIMATIONS.dualHanded()) arm(true, partial);
        RIGHT.apply(partial);
        ITEM.apply(partial);
        GRIP.apply(partial);
        tuning(ANIMATIONS.tunedItem());
    }

    public static void empty(float partial) {
        if (!ANIMATIONS.applies(null)) return;
        root(partial);
        if (ANIMATIONS.dualHanded()) arm(true, partial);
        RIGHT.apply(partial);
        tuning(ANIMATIONS.tunedArm(false));
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
            tuning(ANIMATIONS.tunedArm(left));
            GL11.glTranslated(left ? -.28 : .08, left ? .18 : .04, .05);
            GL11.glScaled(1.35, 1.35, 1.35);
            GL11.glRotatef(left ? 45 : -48, 0, 0, 1);
            GL11.glRotatef(-64, 1, 0, 0);
            GL11.glTranslated(left ? -.3125 : .3125, -.72, 0);
            (left ? arms.bipedLeftArm : arms.bipedRightArm).render(1F / 16);
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    private static void tuning(com.xyp.gtnrlib.compat.punchy.PunchyPack.Pose pose) {
        if (pose == null) return;
        Vec off = pose.offset(), p = pose.pivot(), r = pose.rotation();
        GL11.glTranslated(off.x() + p.x(), off.y() + p.y(), off.z() + p.z());
        GL11.glRotated(r.z(), 0, 0, 1);
        GL11.glRotated(r.y(), 0, 1, 0);
        GL11.glRotated(r.x(), 1, 0, 0);
        GL11.glScaled(pose.scale(), pose.scale(), pose.scale());
        GL11.glTranslated(-p.x(), -p.y(), -p.z());
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
