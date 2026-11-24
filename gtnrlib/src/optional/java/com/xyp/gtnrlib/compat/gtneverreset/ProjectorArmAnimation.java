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
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.cleanroommc.modularui.screen.ModularScreen;
import com.gtnewhorizon.gtnhlib.compat.BaublesCompat;
import com.xyp.gtnrlib.client.animation.HmiItemBindings;
import com.xyp.gtnrlib.client.render.animation.FirstPersonArmRenderer;
import com.xyp.gtnrlib.client.render.animation.ProjectorArmModel;
import com.xyp.gtnrlib.client.render.animation.ProjectorHologramRenderer;
import com.xyp.gtnrlib.config.GTNRLibConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ProjectorArmAnimation implements IItemRenderer, HmiItemBindings.TwoHandedModel {

    private static final Pose LOCAL = new Pose();
    private static final Map<Integer, Pose> REMOTE = new HashMap<>();
    private static Item projector;
    private static ItemStack activeStack;
    private static World world;
    private static float renderPartial;

    private ProjectorArmAnimation() {}

    public static void register() {
        projector = GameRegistry.findItem("gtneverreset", "multiblock_projector");
        if (projector == null) return;
        var renderer = new ProjectorArmAnimation();
        ProjectorArmModel.watchResources();
        MinecraftForgeClient.registerItemRenderer(projector, renderer);
        HmiItemBindings.registerTool(projector, renderer);
        MinecraftForge.EVENT_BUS.register(renderer);
        FMLCommonHandler.instance()
            .bus()
            .register(renderer);
    }

    @Override
    public boolean matches(ItemStack stack, IItemRenderer renderer) {
        return renderer == this;
    }

    @Override
    public void renderGripped(ItemStack stack, IItemRenderer renderer, float partial, HmiItemBindings.GripHands hands) {
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(.32f, .02f, -1.38f);
            orientForearm();
            hands.draw(false);
        } finally {
            GL11.glPopMatrix();
        }
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
        var player = mc.thePlayer;
        LOCAL.open = player != null && projectionOpen();
        if (LOCAL.open) {
            ItemStack held = player.getHeldItem();
            activeStack = isProjector(held) ? held : wornProjector(player);
            if (activeStack == null) {
                for (ItemStack stack : player.inventory.mainInventory) if (isProjector(stack)) {
                    activeStack = stack;
                    break;
                }
            }
        }
        LOCAL.advance();
        if (!LOCAL.open && LOCAL.progress == 0 && LOCAL.previous == 0) activeStack = null;
        REMOTE.values()
            .removeIf(pose -> {
                pose.advance();
                return !pose.open && pose.progress == 0 && pose.previous == 0;
            });
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
        if (!GTNRLibConfig.enableFirstPersonAnimations || activeStack == null
            || mc.thePlayer == null
            || mc.thePlayer.isInvisible()
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
            GL11.glTranslatef(.30f + .35f * progress, -.34f - .125f * progress - .15f * dip, -1.39f);
            float distanceScale = 1 - .15f * progress;
            GL11.glScalef(distanceScale, distanceScale, distanceScale);
            GL11.glRotatef(54 + 29 * progress, 0, 0, 1);
            GL11.glRotatef(-30 + 10 * progress, 1, 0, 0);
            GL11.glRotatef(166 - 12 * progress, 0, 1, 0);
            GL11.glTranslatef(6f / 16, -10f / 16, 0);
            FirstPersonArmRenderer.render(false, 0, activeStack);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            RenderHelper.disableStandardItemLighting();
        }
        return true;
    }

    public static void renderOnFirstPersonArm(EntityPlayer player, boolean left, ItemStack grip) {
        if (!left && (isProjector(grip) || isProjector(player.getHeldItem()) || wearsProjector(player))) {
            float strength = projectionOpen() ? LOCAL.sample(renderPartial) : 0;
            ProjectorArmModel.renderOnArm(player.getCurrentArmor(2) != null, strength, true);
        }
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
        if (isProjector(event.entityPlayer.getHeldItem()) || progress(event.entityPlayer) > .15f)
            event.renderItem = false;
    }

    @SubscribeEvent
    public void renderPlayerArms(RenderPlayerEvent.Specials.Post event) {
        var player = event.entityPlayer;
        if (player.isInvisible()) return;
        float amount = progress(player);
        if (amount <= .001f && !isProjector(player.getHeldItem()) && !wearsProjector(player)) return;
        var arm = event.renderer.modelBipedMain.bipedRightArm;
        var local = Minecraft.getMinecraft().thePlayer == player;
        drawOnArm(arm, player.getCurrentArmor(2) != null, local && !projectionOpen() ? 0 : amount, local);
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
        activeStack = null;
        LOCAL.open = false;
        LOCAL.previous = LOCAL.progress = 0;
        REMOTE.clear();
    }

    @Override
    public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
        return type == ItemRenderType.INVENTORY || type == ItemRenderType.ENTITY
            || type == ItemRenderType.EQUIPPED_FIRST_PERSON;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
        return switch (type) {
            case ENTITY -> helper == ItemRendererHelper.ENTITY_ROTATION || helper == ItemRendererHelper.ENTITY_BOBBING;
            case EQUIPPED_FIRST_PERSON -> helper == ItemRendererHelper.EQUIPPED_BLOCK;
            default -> false;
        };
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        GL11.glPushMatrix();
        try {
            switch (type) {
                case INVENTORY -> {
                    GL11.glTranslatef(8, 8, 0);
                    GL11.glScalef(28, 28, 28);
                    GL11.glRotatef(18, 1, 0, 0);
                    GL11.glRotatef(153, 0, 1, 0);
                    GL11.glRotatef(-25, 0, 0, 1);
                    ProjectorArmModel.render();
                }
                case ENTITY -> {
                    GL11.glTranslatef(0, .22f, 0);
                    GL11.glScalef(1.8f, 1.8f, 1.8f);
                    GL11.glRotatef(16, 1, 0, 0);
                    ProjectorArmModel.render();
                }
                case EQUIPPED_FIRST_PERSON -> {
                    // Undo Forge's block centering, then draw the arm in the vanilla equipped frame.
                    GL11.glTranslatef(.5f, .5f, .5f);
                    GL11.glScalef(2.15f, 2.15f, 2.15f);
                    GL11.glRotatef(-45, 0, 1, 0);
                    GL11.glTranslatef(-.25f, .23f, -.48f);
                    orientForearm();
                    GL11.glTranslatef(6f / 16, -10f / 16, 0);
                    FirstPersonArmRenderer.render(false, 0, stack);
                }
                default -> {}
            }
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static void orientForearm() {
        GL11.glRotatef(54, 0, 0, 1);
        GL11.glRotatef(-30, 1, 0, 0);
        GL11.glRotatef(166, 0, 1, 0);
    }

    private static void drawOnArm(ModelRenderer arm, boolean armored, float amount, boolean localView) {
        if (!arm.showModel || arm.isHidden) return;
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(arm.offsetX, arm.offsetY, arm.offsetZ);
            arm.postRender(1f / 16);
            ProjectorArmModel.renderOnArm(armored, amount, localView);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static boolean isProjector(ItemStack stack) {
        return projector != null && stack != null && stack.getItem() == projector;
    }

    public static boolean wearsProjector(EntityPlayer player) {
        return wornProjector(player) != null;
    }

    private static ItemStack wornProjector(EntityPlayer player) {
        if (projector == null) return null;
        var baubles = BaublesCompat.getBaubles(player);
        if (baubles != null) {
            for (int slot = 0; slot < baubles.getSizeInventory(); slot++) {
                ItemStack stack = baubles.getStackInSlot(slot);
                if (isProjector(stack)) return stack;
            }
        }
        return null;
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
