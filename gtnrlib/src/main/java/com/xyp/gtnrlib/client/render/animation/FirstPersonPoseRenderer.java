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
    private static final float VANILLA_HAND_YAW = 45;
    private static final float MODEL_UNIT = 1F / 16;
    private static ModelBiped arms;

    public static void held(ItemStack stack, float partial) {
        if (!ANIMATIONS.applies(stack)) return;
        // The vanilla item branch has already scaled to 0.4 and translated the item towards the lower edge.
        // Put the wrist inside the 70-degree hand frustum before posing the item and both arms together.
        GL11.glTranslated(-.43, .95, -.92);
        root(partial);
        heldArm(false, partial);
        if (ANIMATIONS.dualHanded()) heldArm(true, partial);
        RIGHT.apply(partial);
        // Vanilla rotates flat item sprites another 50 degrees around Y when drawing them.
        // From the inward grip their broad face would otherwise be nearly edge-on.
        GL11.glRotatef(32, 0, 1, 0);
        ITEM.apply(partial);
        GRIP.apply(partial);
        tuning(ANIMATIONS.tunedItem());
    }

    public static void empty(float partial) {
        if (!ANIMATIONS.applies(null)) return;
        root(partial);
        GL11.glRotatef(-VANILLA_HAND_YAW, 0, 1, 0);
        if (ANIMATIONS.dualHanded()) {
            GL11.glPushMatrix();
            try {
                GL11.glTranslated(-.88, .43, -.22);
                LEFT.apply(partial);
                tuning(ANIMATIONS.tunedArm(true));
                drawFromWrist(true, .82);
            } finally {
                GL11.glPopMatrix();
            }
        }
        // The original renderFirstPersonArm call still draws the primary empty hand, with the player's skin.
        GL11.glTranslated(-.10, .31, -.18);
        GL11.glRotatef(65 + VANILLA_HAND_YAW, 0, 1, 0);
        RIGHT.apply(partial);
        tuning(ANIMATIONS.tunedArm(false));
    }

    private static void root(float partial) {
        GL11.glRotated(-ANIMATIONS.yaw(partial) * .35, 0, 1, 0);
        GL11.glRotated(-ANIMATIONS.pitch(partial) * .35, 1, 0, 0);
        BODY.apply(partial);
    }

    private static void heldArm(boolean left, float partial) {
        GL11.glPushMatrix();
        try {
            (left ? LEFT : RIGHT).apply(partial);
            tuning(ANIMATIONS.tunedArm(left));
            GL11.glTranslated(left ? -.16 : .04, left ? -.06 : 0, .04);
            GL11.glRotatef(-VANILLA_HAND_YAW, 0, 1, 0);
            drawFromWrist(left, 1.95);
        } finally {
            GL11.glPopMatrix();
        }
    }

    /** ModelBiped defines shoulders as pivots. Place the hand end of its 4 x 12 x 4 mesh at the grip. */
    private static void drawFromWrist(boolean left, double scale) {
        var mc = Minecraft.getMinecraft();
        if (arms == null) arms = new ModelBiped(0);
        GL11.glPushAttrib(GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_ENABLE_BIT);
        try {
            mc.getTextureManager()
                .bindTexture(mc.thePlayer.getLocationSkin());
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glRotatef(left ? -51 : 43, 0, 0, 1);
            GL11.glRotatef(-32, 1, 0, 0);
            GL11.glScaled(scale, scale, scale);
            GL11.glTranslatef((left ? -6 : 6) * MODEL_UNIT, -11 * MODEL_UNIT, 0);
            (left ? arms.bipedLeftArm : arms.bipedRightArm).render(MODEL_UNIT);
        } finally {
            GL11.glPopAttrib();
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
