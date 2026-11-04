package com.xyp.gtneverreset.compat.gregtech;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import com.xyp.gtneverreset.GTNeverReset;
import com.xyp.gtneverreset.common.packet.WrenchTurnPacket;
import com.xyp.gtnrlib.animation.resource.NativeAnimationResource;
import com.xyp.gtnrlib.api.animation.AnimationClip;
import com.xyp.gtnrlib.api.animation.AnimationClip.Channel;
import com.xyp.gtnrlib.api.animation.AnimationClip.Property;
import com.xyp.gtnrlib.api.animation.AnimationPlayer;
import com.xyp.gtnrlib.client.animation.HmiFirstPerson;
import com.xyp.gtnrlib.client.animation.HmiItemBindings;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.items.MetaGeneratedTool01;
import gregtech.common.render.MetaGeneratedToolRenderer;

@SideOnly(Side.CLIENT)
public final class GregTechWrenchAnimation implements HmiItemBindings.HandheldTool, IResourceManagerReloadListener {

    public static final GregTechWrenchAnimation INSTANCE = new GregTechWrenchAnimation();
    private static final ResourceLocation RESOURCE = new ResourceLocation(
        GTNeverReset.MODID,
        "animations/wrench_turn.json");
    private static final Channel POSITION = new Channel("wrist", Property.POSITION),
        ROTATION = new Channel("wrist", Property.ROTATION);
    // GT's 16px wrench has a vertical, centered handle. Place its lower grip on the upstream handheld grip.
    private static final Matrix4fc GRIP = new Matrix4f().translation(12f / 16, 4f / 16, 0)
        .rotateZ((float) Math.toRadians(32))
        .translate(-8f / 16, -4f / 16, 0);
    private final AnimationPlayer player = new AnimationPlayer(marker -> {});
    private AnimationClip clip;
    private AnimationPlayer.Playback turn;
    private EntityPlayer owner;
    private int slot;
    private double sampledTick;

    private GregTechWrenchAnimation() {}

    public static void register() {
        HmiItemBindings.registerTool(MetaGeneratedTool01.INSTANCE, INSTANCE);
        ((IReloadableResourceManager) Minecraft.getMinecraft()
            .getResourceManager()).registerReloadListener(INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    @Override
    public void onResourceManagerReload(IResourceManager resources) {
        try (var reader = new InputStreamReader(
            resources.getResource(RESOURCE)
                .getInputStream(),
            StandardCharsets.UTF_8)) {
            var clips = new HashMap<String, AnimationClip>();
            NativeAnimationResource.load(reader, clips::put);
            var next = Objects.requireNonNull(clips.get("gtneverreset:wrench_turn"));
            if (next.end() != AnimationClip.End.ONCE || !next.tracks()
                .containsKey(ROTATION)) throw new IllegalArgumentException("Expected a one-shot wrist rotation");
            clip = next;
            clear();
        } catch (IOException | RuntimeException ex) {
            GTNeverReset.LOG.warn("Could not reload {}", RESOURCE, ex);
        }
    }

    @Override
    public boolean matches(ItemStack stack, IItemRenderer renderer) {
        return renderer.getClass() == MetaGeneratedToolRenderer.class && WrenchTurnPacket.isManualWrench(stack);
    }

    @Override
    public Matrix4fc spriteMount() {
        return GRIP;
    }

    @Override
    public boolean handlesBlockUseSwing() {
        return true;
    }

    public void accepted(int dimension, int usedSlot) {
        var mc = Minecraft.getMinecraft();
        var p = mc.thePlayer;
        if (clip == null || !HmiFirstPerson.INSTANCE.enabled()
            || p == null
            || p.dimension != dimension
            || p.inventory.currentItem != usedSlot
            || !WrenchTurnPacket.isManualWrench(p.getHeldItem())
            || mc.gameSettings.thirdPersonView != 0) return;
        if (owner != p || slot != usedSlot) clear();
        double now = turn == null ? p.ticksExisted : Math.max(sampledTick, p.ticksExisted);
        if (turn != null) advance(now);
        if (turn != null) turn.stop(.08);
        boolean chaining = turn != null;
        owner = p;
        slot = usedSlot;
        sampledTick = now;
        turn = player.play(clip, chaining ? .08 : 0);
    }

    @Override
    public boolean pose(ItemStack stack, float partial, Matrix4f motion) {
        if (turn == null || owner != Minecraft.getMinecraft().thePlayer || owner.inventory.currentItem != slot)
            return false;
        advance((double) owner.ticksExisted + partial);
        if (turn == null) return false;
        var offset = player.sample(POSITION, 0);
        var rotation = player.sample(ROTATION, 0);
        motion.translate((float) offset.x(), (float) offset.y(), (float) offset.z())
            .rotateXYZ(
                (float) Math.toRadians(rotation.x()),
                (float) Math.toRadians(rotation.y()),
                (float) Math.toRadians(rotation.z()));
        return true;
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (turn == null || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getMinecraft();
        if (owner != mc.thePlayer || owner.inventory.currentItem != slot
            || !WrenchTurnPacket.isManualWrench(owner.getHeldItem())
            || !HmiFirstPerson.INSTANCE.enabled()
            || mc.gameSettings.thirdPersonView != 0) clear();
        else advance(owner.ticksExisted);
    }

    private void advance(double now) {
        if (now <= sampledTick) return;
        player.advance((now - sampledTick) / 20);
        sampledTick = now;
        if (!turn.active()) clear();
    }

    private void clear() {
        player.clear();
        turn = null;
        owner = null;
    }
}
