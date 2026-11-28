package com.xyp.gtnrlib.client.render.animation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;

import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.client.animation.FirstPersonItemActions.Scene;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Client render hooks selected by the effect identifier on a first-person action binding. */
@SideOnly(Side.CLIENT)
public final class FirstPersonActionEffects {

    private static final Map<ResourceLocation, Effect> EFFECTS = new HashMap<>();

    static {
        register(new ResourceLocation(GTNRLib.MODID, "gtnl_doll"), new GtnlDollInspectionRenderer());
    }

    private FirstPersonActionEffects() {}

    /** Call during client initialization. An identifier can be registered once. */
    public static void register(ResourceLocation id, Effect effect) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(effect, "effect");
        if (EFFECTS.putIfAbsent(id, effect) != null)
            throw new IllegalArgumentException("First-person action effect already registered: " + id);
    }

    /** Remove this provider on the client thread if it still owns the identifier. */
    public static boolean unregister(ResourceLocation id, Effect effect) {
        if (effect == null || EFFECTS.get(id) != effect) return false;
        EFFECTS.remove(id);
        return true;
    }

    static Effect get(ResourceLocation id) {
        return EFFECTS.get(id);
    }

    static void beforeHands(Effect effect, Scene scene, ItemStack stack, Matrix4fc itemPose) {
        invoke(effect, scene, () -> {
            effect.beforeHands(scene, stack, itemPose);
            return true;
        });
    }

    static boolean renderItem(Effect effect, Scene scene, ItemRenderer renderer, ItemStack stack,
        IItemRenderer forgeRenderer) {
        return invoke(effect, scene, () -> effect.renderItem(scene, renderer, stack, forgeRenderer));
    }

    static void overlay(Effect effect, Scene scene, int width, int height) {
        invoke(effect, scene, () -> {
            effect.overlay(scene, width, height);
            return true;
        });
    }

    private static boolean invoke(Effect effect, Scene scene, BooleanSupplier draw) {
        if (get(scene.effect()) != effect) return false;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            return draw.getAsBoolean();
        } catch (RuntimeException failure) {
            unregister(scene.effect(), effect);
            GTNRLib.LOG.error("disabled failing first-person action effect {}", scene.effect(), failure);
            return false;
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    /**
     * Hooks run on the render thread. Treat the stack and pose as read-only for the current call.
     * Model-view, projection and fixed-function attributes are isolated. Balance any matrix and attribute stack
     * operations and restore any other graphics state, including when drawing fails.
     */
    @SideOnly(Side.CLIENT)
    public interface Effect {

        /** Draw in hand view space before the held item and the player's arms. */
        default void beforeHands(Scene scene, ItemStack stack, Matrix4fc itemPose) {}

        /** Draw in item space and return true to replace the standard item draw. The Forge renderer may be null. */
        default boolean renderItem(Scene scene, ItemRenderer renderer, ItemStack stack, IItemRenderer forgeRenderer) {
            return false;
        }

        /** Draw after the hands. Width and height are in framebuffer pixels. */
        default void overlay(Scene scene, int width, int height) {}
    }
}
