package com.xyp.gtnrlib.compat.gtneverreset;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.compat.BaublesCompat;
import com.xyp.gtnrlib.client.animation.HmiItemBindings;
import com.xyp.gtnrlib.client.render.animation.FirstPersonArmRenderer;
import com.xyp.gtnrlib.client.render.animation.ProjectorArmModel;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ProjectorArmAnimation implements IItemRenderer, HmiItemBindings.TwoHandedModel {

    private static Item projector;

    private ProjectorArmAnimation() {}

    public static void register() {
        projector = GameRegistry.findItem("gtneverreset", "multiblock_projector");
        if (projector == null) return;
        var renderer = new ProjectorArmAnimation();
        ProjectorArmModel.watchResources();
        MinecraftForgeClient.registerItemRenderer(projector, renderer);
        HmiItemBindings.registerTool(projector, renderer);
        MinecraftForge.EVENT_BUS.register(renderer);
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

    public static void renderOnFirstPersonArm(EntityPlayer player, boolean left, ItemStack grip) {
        if (left ? wearsProjector(player) : isProjector(grip))
            ProjectorArmModel.renderOnArm(left, player.getCurrentArmor(2) != null);
    }

    @SubscribeEvent
    public void suppressHeldSprite(RenderPlayerEvent.Specials.Pre event) {
        if (isProjector(event.entityPlayer.getHeldItem())) event.renderItem = false;
    }

    @SubscribeEvent
    public void renderPlayerArms(RenderPlayerEvent.Specials.Post event) {
        var player = event.entityPlayer;
        if (player.isInvisible()) return;
        var model = event.renderer.modelBipedMain;
        boolean armored = player.getCurrentArmor(2) != null;
        if (isProjector(player.getHeldItem())) drawOnArm(model.bipedRightArm, false, armored);
        if (wearsProjector(player)) drawOnArm(model.bipedLeftArm, true, armored);
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

    private static void drawOnArm(ModelRenderer arm, boolean left, boolean armored) {
        if (!arm.showModel || arm.isHidden) return;
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(arm.offsetX, arm.offsetY, arm.offsetZ);
            arm.postRender(1f / 16);
            ProjectorArmModel.renderOnArm(left, armored);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static boolean isProjector(ItemStack stack) {
        return projector != null && stack != null && stack.getItem() == projector;
    }

    private static boolean wearsProjector(EntityPlayer player) {
        if (projector == null) return false;
        var baubles = BaublesCompat.getBaubles(player);
        if (baubles != null) {
            for (int slot = 0; slot < baubles.getSizeInventory(); slot++)
                if (isProjector(baubles.getStackInSlot(slot))) return true;
        }
        return false;
    }
}
