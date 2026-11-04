package com.xyp.gtnrlib.client.render.animation;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.compat.hmi.HmiPoseProgram;

/** Renders just the shipping mesh into the fixture framebuffer, without arms or a fallback sprite. */
public final class AnimationModelChecks {

    public static Runnable draw(String id, String variant, int used, int age, HmiPoseProgram pose) {
        return () -> {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glTranslatef(.08f, .1f, -2.6f);
            GL11.glRotatef(52, 1, 0, 0);
            GL11.glScalef(1.6f, 1.6f, 1.6f);
            if (!DefaultItemModels.draw(id, variant, used, age, pose)) throw new AssertionError("Missing mesh " + id);
        };
    }

    private AnimationModelChecks() {}
}
