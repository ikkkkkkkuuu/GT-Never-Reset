package com.xyp.gtnrlib.compat.gregtech;

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

import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.animation.resource.NativeAnimationResource;
import com.xyp.gtnrlib.api.animation.AnimationClip;
import com.xyp.gtnrlib.api.animation.AnimationClip.Channel;
import com.xyp.gtnrlib.api.animation.AnimationClip.Property;
import com.xyp.gtnrlib.client.animation.HmiItemBindings;
import com.xyp.gtnrlib.network.GregTechToolUsePacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.items.IDMetaTool01;
import gregtech.common.items.MetaGeneratedTool01;
import gregtech.common.render.MetaGeneratedToolRenderer;

@SideOnly(Side.CLIENT)
public final class GregTechWireCutterAnimation
    implements HmiItemBindings.TwoHandedTool, IResourceManagerReloadListener {

    private static final ResourceLocation RESOURCE = new ResourceLocation(
        "gtneverreset",
        "animations/wire_cutter.json");
    private static final Channel REACH = new Channel("hinge", Property.POSITION),
        JAW = new Channel("jaw", Property.ROTATION);
    // Coordinates in GT's 16px equipped sprite, measured from the lower edge.
    private static final float PIXEL = 1f / 16, HINGE_Y = 9.5f * PIXEL;
    private static final float GRIP_X = 6.5f * PIXEL, GRIP_Y = 3.5f * PIXEL;
    private static final float REST_OPEN_DEGREES = 17;
    private static final DoubleBuffer LEFT = plane(-1, 0, 0), RIGHT = plane(1, 0, 0);
    // Row 9 is the pin. The jaws start at row 10 and the handles end at row 8.
    private static final DoubleBuffer JAWS = plane(0, 1, -.5f * PIXEL), HANDLES = plane(0, -1, -.5f * PIXEL),
        PIN_BOTTOM = plane(0, 1, .5f * PIXEL), PIN_TOP = plane(0, -1, .5f * PIXEL);
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
            GTNRLib.LOG.warn("Could not reload {}", RESOURCE, ex);
        }
    }

    @Override
    public boolean matches(ItemStack stack, IItemRenderer renderer) {
        return clip != null && GregTechToolUsePacket.matches(stack, IDMetaTool01.WIRECUTTER)
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
        // GT's stock icon already shows the blades touching. The JSON supplies the squeeze from the open rest pose.
        float opening = Math.max(
            0,
            REST_OPEN_DEGREES - (float) clip.sample(JAW, seconds)
                .z());
        GL11.glTranslated(.03 + reach.x(), .055 + reach.y(), -1.18 + reach.z());
        GL11.glRotatef(-38, 1, 0, 0);
        half(stack, renderer, hands, true, opening);
        half(stack, renderer, hands, false, opening);
        // At contact the stock mesh is already the whole tool, including its extruded edges.
        if (opening == 0) sprite(stack, renderer);
        else section(stack, renderer, PIN_BOTTOM, PIN_TOP, -4);
    }

    private static void half(ItemStack stack, IItemRenderer renderer, HmiItemBindings.GripHands hands, boolean left,
        float opening) {
        int side = left ? -1 : 1;
        GL11.glPushMatrix();
        try {
            GL11.glRotatef(side * opening, 0, 0, 1);
            if (opening > 0) {
                int depthBias = left ? 4 : 0;
                section(stack, renderer, left ? LEFT : RIGHT, HANDLES, depthBias);
                section(stack, renderer, left ? RIGHT : LEFT, JAWS, depthBias);
            }
            GL11.glTranslatef(side * GRIP_X, GRIP_Y - HINGE_Y, .015f);
            GL11.glRotatef(side * 38, 0, 0, 1);
            GL11.glRotatef(-22, 1, 0, 0);
            hands.draw(left);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static void section(ItemStack stack, IItemRenderer renderer, DoubleBuffer first, DoubleBuffer second,
        int depthBias) {
        GL11.glPushAttrib(GL11.GL_TRANSFORM_BIT | GL11.GL_ENABLE_BIT | GL11.GL_POLYGON_BIT);
        try {
            GL11.glClipPlane(GL11.GL_CLIP_PLANE0, first);
            GL11.glClipPlane(GL11.GL_CLIP_PLANE1, second);
            GL11.glEnable(GL11.GL_CLIP_PLANE0);
            GL11.glEnable(GL11.GL_CLIP_PLANE1);
            // Order the cheeks at the pivot without moving either textured face out of the blade plane.
            if (depthBias != 0) {
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                GL11.glPolygonOffset(0, depthBias);
            }
            sprite(stack, renderer);
        } finally {
            GL11.glPopAttrib();
        }
    }

    private static void sprite(ItemStack stack, IItemRenderer renderer) {
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(-.5f, -HINGE_Y, .5f * PIXEL);
            renderer.renderItem(ItemRenderType.EQUIPPED_FIRST_PERSON, stack);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static DoubleBuffer plane(int x, int y, float offset) {
        var buffer = BufferUtils.createDoubleBuffer(4);
        buffer.put(0, x);
        buffer.put(1, y);
        buffer.put(3, offset);
        return buffer;
    }
}
