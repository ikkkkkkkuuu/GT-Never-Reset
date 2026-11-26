package com.xyp.gtnrlib.compat.gtneverreset;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.cleanroommc.modularui.screen.ModularScreen;
import com.gtnewhorizon.gtnhlib.compat.BaublesCompat;
import com.xyp.gtnrlib.client.render.animation.FirstPersonArmRenderer;
import com.xyp.gtnrlib.client.render.animation.ProjectorHologramRenderer;
import com.xyp.gtnrlib.config.GTNRLibConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ProjectorArmAnimation {

    private static final int LIFT_LEAD_TICKS = 8;
    private static final int OPEN_RESPONSE_TICKS = 20;
    private static final Pose LOCAL = new Pose();
    private static final Map<Integer, Pose> REMOTE = new HashMap<>();
    private static Item projector;
    private static Runnable openRequest;
    private static int openDelay, responseTicks;
    private static World world;
    private static float renderPartial;

    private ProjectorArmAnimation() {}

    public static void register() {
        projector = GameRegistry.findItem("gtneverreset", "multiblock_projector");
        if (projector == null) return;
        var renderer = new ProjectorArmAnimation();
        MinecraftForge.EVENT_BUS.register(renderer);
        FMLCommonHandler.instance()
            .bus()
            .register(renderer);
    }

    public static void requestOpen(ItemStack stack, Runnable sendRequest) {
        var mc = Minecraft.getMinecraft();
        if (!GTNRLibConfig.enableFirstPersonAnimations || mc.thePlayer == null
            || mc.thePlayer.isInvisible()
            || mc.currentScreen != null
            || !isProjector(stack)) {
            sendRequest.run();
            return;
        }
        resetWorld(mc.theWorld);
        if (openRequest != null || responseTicks > 0) return;
        openDelay = LIFT_LEAD_TICKS;
        openRequest = sendRequest;
    }

    public static void receivePose(Minecraft minecraft, int entity, int dimension, boolean open) {
        var connection = minecraft.getNetHandler();
        var receivingWorld = minecraft.theWorld;
        if (projector == null || connection == null || receivingWorld == null) return;
        minecraft.func_152344_a(() -> {
            if (minecraft.getNetHandler() != connection || minecraft.theWorld != receivingWorld
                || minecraft.thePlayer == null
                || minecraft.thePlayer.dimension != dimension
                || minecraft.thePlayer.getEntityId() == entity) return;
            resetWorld(receivingWorld);
            if (open) REMOTE.computeIfAbsent(entity, ignored -> new Pose()).open = true;
            else if (REMOTE.containsKey(entity)) REMOTE.get(entity).open = false;
        });
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getMinecraft();
        resetWorld(mc.theWorld);
        boolean visible = mc.thePlayer != null && projectionOpen();
        advanceOpenRequest(mc, visible);
        LOCAL.open = visible || openRequest != null || responseTicks > 0;
        LOCAL.advance();
        REMOTE.values()
            .removeIf(pose -> {
                pose.advance();
                return !pose.open && pose.progress == 0 && pose.previous == 0;
            });
    }

    private static void advanceOpenRequest(Minecraft mc, boolean visible) {
        if (visible || mc.thePlayer == null || mc.currentScreen != null) {
            openRequest = null;
            responseTicks = 0;
        } else if (openRequest != null && --openDelay == 0) {
            Runnable send = openRequest;
            openRequest = null;
            responseTicks = OPEN_RESPONSE_TICKS;
            send.run();
        } else if (responseTicks > 0) {
            responseTicks--;
        }
    }

    @SubscribeEvent
    public void frame(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            renderPartial = event.renderTickTime;
            ProjectorHologramRenderer.beginFrame();
        }
    }

    public static boolean renderRaisedFirstPerson(float partial, float dip) {
        var mc = Minecraft.getMinecraft();
        var player = mc.thePlayer;
        if (!GTNRLibConfig.enableFirstPersonAnimations || player == null
            || player.isInvisible()
            || mc.gameSettings.thirdPersonView != 0) return false;
        float progress = LOCAL.sample(partial);
        if (progress <= .001f) return false;
        GL11.glPushAttrib(
            GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT
                | GL11.GL_ENABLE_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glTranslatef(0, -.35f - .27f * dip, .20f);
            mountFirstPersonArm(progress);
            GL11.glTranslatef(6f / 16, -10f / 16, 0);
            FirstPersonArmRenderer.renderRaisedProjectorArm(projectionOpen() ? progress : 0);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            RenderHelper.disableStandardItemLighting();
        }
        return true;
    }

    public static void poseThirdPersonArm(EntityPlayer player, ModelRenderer arm) {
        var mc = Minecraft.getMinecraft();
        if (player == mc.thePlayer && mc.gameSettings.thirdPersonView == 0) return;
        float amount = progress(player);
        if (amount <= .001f) return;
        arm.rotateAngleX += (-1.29f - arm.rotateAngleX) * amount;
        arm.rotateAngleY += (-.98f - arm.rotateAngleY) * amount;
        arm.rotateAngleZ += (-.18f - arm.rotateAngleZ) * amount;
    }

    @SubscribeEvent
    public void suppressHeldSprite(RenderPlayerEvent.Specials.Pre event) {
        if (progress(event.entityPlayer) > .15f) event.renderItem = false;
    }

    @SubscribeEvent
    public void renderProjection(RenderPlayerEvent.Specials.Post event) {
        var player = event.entityPlayer;
        if (player.isInvisible()) return;
        float amount = progress(player);
        if (amount <= .01f) return;
        var arm = event.renderer.modelBipedMain.bipedRightArm;
        boolean local = Minecraft.getMinecraft().thePlayer == player;
        if (local && !projectionOpen() || !arm.showModel || arm.isHidden) return;
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(arm.offsetX, arm.offsetY, arm.offsetZ);
            arm.postRender(1f / 16);
            ProjectorHologramRenderer.renderFromWrist(amount, local);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static float progress(EntityPlayer player) {
        if (world != player.worldObj || projector == null) return 0;
        if (Minecraft.getMinecraft().thePlayer == player) return LOCAL.sample(renderPartial);
        Pose pose = REMOTE.get(player.getEntityId());
        return pose == null ? 0 : pose.sample(renderPartial);
    }

    private static boolean projectionOpen() {
        return ModularScreen.isActive("gtneverreset", "multiblock_projector");
    }

    private static void resetWorld(World next) {
        if (world == next) return;
        world = next;
        openRequest = null;
        responseTicks = 0;
        LOCAL.open = false;
        LOCAL.previous = LOCAL.progress = 0;
        REMOTE.clear();
    }

    private static void mountFirstPersonArm(float raise) {
        GL11.glTranslatef(.53f - .45f * raise, -.22f + .34f * raise, -1.43f + .14f * raise);
        orientForearm(raise);
    }

    private static void orientForearm(float raise) {
        GL11.glRotatef(33 + 54 * raise, 0, 0, 1);
        GL11.glRotatef(-29 + 10 * raise, 1, 0, 0);
        GL11.glRotatef(166 - 12 * raise, 0, 1, 0);
    }

    private static boolean isProjector(ItemStack stack) {
        return projector != null && stack != null && stack.getItem() == projector;
    }

    public static boolean wearsProjector(EntityPlayer player) {
        if (projector == null) return false;
        var baubles = BaublesCompat.getBaubles(player);
        if (baubles != null) {
            for (int slot = 0; slot < baubles.getSizeInventory(); slot++) {
                ItemStack stack = baubles.getStackInSlot(slot);
                if (isProjector(stack)) return true;
            }
        }
        return false;
    }

    private static final class Pose {

        boolean open;
        float previous, progress;

        void advance() {
            previous = progress;
            progress = open ? Math.min(1, progress + .14f) : Math.max(0, progress - .16f);
        }

        float sample(float partial) {
            float value = previous + (progress - previous) * partial;
            return value * value * (3 - 2 * value);
        }
    }
}
