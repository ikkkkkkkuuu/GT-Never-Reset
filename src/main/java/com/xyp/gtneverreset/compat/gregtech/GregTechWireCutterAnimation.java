package com.xyp.gtneverreset.compat.gregtech;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.DoubleBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.packet.WireCutterSnipPacket;
import com.xyp.gtnrlib.animation.resource.NativeAnimationResource;
import com.xyp.gtnrlib.api.animation.AnimationClip;
import com.xyp.gtnrlib.api.animation.AnimationClip.Channel;
import com.xyp.gtnrlib.api.animation.AnimationClip.Property;
import com.xyp.gtnrlib.client.animation.HmiItemBindings;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.items.MetaGeneratedTool01;
import gregtech.common.render.MetaGeneratedToolRenderer;

@SideOnly(Side.CLIENT)
public final class GregTechWireCutterAnimation
    implements HmiItemBindings.TwoHandedTool, IResourceManagerReloadListener {

    private static final ResourceLocation RESOURCE = new ResourceLocation(
        GTNeverReset.MODID,
        "animations/wire_cutter.json");
    private static final Channel REACH = new Channel("hinge", Property.POSITION),
        JAW = new Channel("jaw", Property.ROTATION);
    // Coordinates in GT's 16px equipped sprite, measured from the lower edge.
    private static final float PIXEL = 1f / 16, HINGE_Y = 10.5f * PIXEL;
    private static final DoubleBuffer LEFT = plane(-1, 0), RIGHT = plane(1, 0), ABOVE = plane(0, 1),
        BELOW = plane(0, -1);
    private AnimationClip clip;

    private GregTechWireCutterAnimation() {}

    public static void register() {
        var cutter = new GregTechWireCutterAnimation();
        HmiItemBindings.registerTool(MetaGeneratedTool01.INSTANCE, cutter);
        ((IReloadableResourceManager) Minecraft.getMinecraft()
            .getResourceManager()).registerReloadListener(cutter);
    }

    @Override
    public void onResourceManagerReload(IResourceManager resources) {
        try (var reader = new InputStreamReader(
            resources.getResource(RESOURCE)
                .getInputStream(),
            StandardCharsets.UTF_8)) {
            var clips = new HashMap<String, AnimationClip>();
            NativeAnimationResource.load(reader, clips::put);
            var next = Objects.requireNonNull(clips.get("gtneverreset:wire_cutter"));
            if (next.end() != AnimationClip.End.ONCE || !next.tracks()
                .keySet()
                .containsAll(List.of(REACH, JAW)))
                throw new IllegalArgumentException("Expected a one-shot hinge position and jaw rotation");
            clip = next;
        } catch (IOException | RuntimeException ex) {
            GTNeverReset.LOG.warn("Could not reload {}", RESOURCE, ex);
        }
    }

    @Override
    public boolean matches(ItemStack stack, IItemRenderer renderer) {
        return clip != null && WireCutterSnipPacket.isManualCutter(stack)
            && renderer.getClass() == MetaGeneratedToolRenderer.class;
    }

    @Override
    public boolean handlesBlockUseSwing() {
        return true;
    }

    @Override
    public double strokeDuration() {
        return clip.duration();
    }

    @Override
    public void render(ItemStack stack, IItemRenderer renderer, double seconds, HmiItemBindings.GripHands hands) {
        var reach = clip.sample(REACH, seconds);
        float angle = (float) clip.sample(JAW, seconds)
            .z();
        GL11.glTranslated(.03 + reach.x(), .055 + reach.y(), -1.18 + reach.z());
        GL11.glRotatef(-38, 1, 0, 0);
        half(stack, renderer, hands, true, angle);
        half(stack, renderer, hands, false, angle);
    }

    private static void half(ItemStack stack, IItemRenderer renderer, HmiItemBindings.GripHands hands, boolean left,
        float angle) {
        int side = left ? -1 : 1;
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(0, 0, side * PIXEL / 8);
            GL11.glRotatef(-side * angle, 0, 0, 1);
            GL11.glPushAttrib(GL11.GL_TRANSFORM_BIT | GL11.GL_ENABLE_BIT);
            try {
                GL11.glEnable(GL11.GL_CLIP_PLANE0);
                GL11.glEnable(GL11.GL_CLIP_PLANE1);
                // The handle drives the jaw across the hinge. Both keep GT's material and overlay render passes.
                section(stack, renderer, left ? LEFT : RIGHT, BELOW);
                section(stack, renderer, left ? RIGHT : LEFT, ABOVE);
            } finally {
                GL11.glPopAttrib();
            }
            GL11.glTranslatef(side * 5.25f * PIXEL, -5.25f * PIXEL, .015f);
            GL11.glRotatef(side * 38, 0, 0, 1);
            GL11.glRotatef(-22, 1, 0, 0);
            hands.draw(left);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static void section(ItemStack stack, IItemRenderer renderer, DoubleBuffer horizontal,
        DoubleBuffer vertical) {
        GL11.glClipPlane(GL11.GL_CLIP_PLANE0, horizontal);
        GL11.glClipPlane(GL11.GL_CLIP_PLANE1, vertical);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(-.5f, -HINGE_Y, .5f * PIXEL);
            renderer.renderItem(ItemRenderType.EQUIPPED_FIRST_PERSON, stack);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static DoubleBuffer plane(int x, int y) {
        var buffer = BufferUtils.createDoubleBuffer(4);
        buffer.put(0, x);
        buffer.put(1, y);
        // Leave a small lap on each side of the pivot so the two steel cheeks stay joined as they turn.
        buffer.put(3, y == 0 ? 0 : .75 * PIXEL);
        return buffer;
    }
}
