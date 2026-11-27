package com.xyp.gtnrlib.qa;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.client.animation.FirstPersonItemActions;
import com.xyp.gtnrlib.client.animation.HmiFirstPerson;
import com.xyp.gtnrlib.client.input.KeyBindingLookup;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.gameevent.InputEvent;

final class AnimationBlockInspectChecks {

    private static final String INSPECT = "gtnrlib:block_inspect";

    static void verify(AnimationClientChecks qa, Minecraft mc) throws Exception {
        var key = KeyBindingLookup.find("gtnrlib.key.inspect_block", null);
        qa.require(key != null && key.getKeyCode() == Keyboard.KEY_I, "block inspection registered on I");
        var player = mc.thePlayer;
        var actions = FirstPersonItemActions.INSTANCE;
        var hmi = HmiFirstPerson.INSTANCE;
        int tick = player.ticksExisted;
        int slot = player.inventory.currentItem;
        int nextSlot = (slot + 1) % 9;
        var oldHeld = player.getHeldItem();
        var oldNext = player.inventory.mainInventory[nextSlot];
        var oldScreen = mc.currentScreen;
        int oldView = mc.gameSettings.thirdPersonView;
        var block = new ItemStack(Blocks.log, 8);
        var playerRenderer = (RenderPlayer) RenderManager.instance.getEntityRenderObject(player);
        try (var arm = new ArmProbe(playerRenderer.modelBipedMain, false);
            var leftArm = new ArmProbe(playerRenderer.modelBipedMain, true)) {
            qa.equip(mc, null);
            press(key);
            qa.require(!actions.playing(INSPECT), "empty hand ignores the inspection key");
            qa.equip(mc, new ItemStack(Items.iron_sword));
            press(key);
            qa.require(!actions.playing(INSPECT), "ordinary item ignores the block inspection key");
            qa.equip(mc, block);
            mc.currentScreen = new GuiInventory(player);
            press(key);
            qa.require(!actions.playing(INSPECT), "open inventory ignores the inspection key");
            mc.currentScreen = oldScreen;
            mc.gameSettings.thirdPersonView = 1;
            press(key);
            qa.require(!actions.playing(INSPECT), "third-person camera ignores the inspection key");
            mc.gameSettings.thirdPersonView = 0;

            int[] idle = qa.draw(mc, "block-inspect-idle");
            var idleItem = new Matrix4f(hmi.item);
            var idleArm = new Matrix4f(hmi.right);
            var idleShoulder = arm.shoulder();
            var idleHand = arm.hand();
            var idleLeftHand = leftArm.hand();
            boolean swinging = player.isSwingInProgress;
            press(key);
            qa.require(actions.playing(INSPECT), "registered key starts one inspection of the held oak log");
            qa.require(
                player.getHeldItem() == block && block.stackSize == 8 && player.isSwingInProgress == swinging,
                "inspection leaves the held stack and gameplay swing alone");
            player.ticksExisted = tick + 10;
            int[] raised = qa.draw(mc, "block-inspect-raised");
            var raisedItem = new Matrix4f(hmi.item);
            var raisedArm = new Matrix4f(hmi.right);
            qa.require(
                !idleItem.equals(raisedItem, .05f) && !idleArm.equals(hmi.right, .05f) && !Arrays.equals(idle, raised),
                "inspection lifts the rendered log together with the arm");
            qa.require(
                arm.shoulder()
                    .distance(idleShoulder) < .05f && arm.hand().y - idleHand.y > .10f,
                "the mesh shoulder stays in place as the wrist rises");

            player.ticksExisted = tick + 25;
            int[] turned = qa.draw(mc, "block-inspect-turned");
            qa.require(
                !raisedItem.equals(hmi.item, .05f) && !Arrays.equals(raised, turned),
                "inspection turns the raised log to a different view");
            double armTurn = rotationChangeDegrees(raisedArm, hmi.right);
            double itemTurn = rotationChangeDegrees(raisedItem, hmi.item);
            qa.require(
                armTurn < 15 && itemTurn > 35,
                String
                    .format(Locale.ROOT, "forearm stays steady (%.1f°) as the block turns (%.1f°)", armTurn, itemTurn));
            qa.require(
                arm.shoulder()
                    .distance(idleShoulder) < .05f,
                "the shoulder stays in place through the turn");
            press(key);
            checkToss(qa, mc, tick, leftArm, idleLeftHand);
            qa.require(player.getHeldItem() == block && block.stackSize == 8, "both catches preserve the held stack");
            player.ticksExisted = tick + 90;
            qa.draw(mc, "block-inspect-recovered");
            qa.require(!actions.playing(INSPECT), "inspection finishes on schedule after a repeated press");

            press(key);
            qa.require(actions.playing(INSPECT), "inspection can be triggered again after finishing");
            player.inventory.currentItem = nextSlot;
            qa.equip(mc, block.copy());
            qa.tickPlayer();
            qa.require(!actions.playing(INSPECT), "switching to another slot clears the active inspection");
            press(key);
            qa.require(actions.playing(INSPECT), "inspection starts on the same block in the new slot");
            player.getHeldItem()
                .setItemDamage(1);
            qa.tickPlayer();
            qa.require(!actions.playing(INSPECT), "changing block metadata clears the active inspection");

            qa.equip(mc, new ItemStack(Blocks.brick_block));
            key.setKeyCode(Keyboard.KEY_F14);
            KeyBinding.resetKeyBindingArrayAndHash();
            press(Keyboard.KEY_I);
            qa.require(!actions.playing(INSPECT), "the default key no longer starts inspection after rebinding");
            press(key);
            qa.require(actions.playing(INSPECT), "the rebound key uses the generic inspection on a second block item");
        } finally {
            key.setKeyCode(Keyboard.KEY_I);
            KeyBinding.resetKeyBindingArrayAndHash();
            mc.currentScreen = oldScreen;
            mc.gameSettings.thirdPersonView = oldView;
            player.inventory.mainInventory[nextSlot] = oldNext;
            player.inventory.currentItem = slot;
            qa.equip(mc, oldHeld);
            qa.tickPlayer();
            player.ticksExisted = tick;
            hmi.detach();
        }
    }

    private static void checkToss(AnimationClientChecks qa, Minecraft mc, int start, ArmProbe leftArm,
        Vector3f idleLeftHand) throws Exception {
        var release = drawTossPose(qa, mc, start + 32, "right-release");
        var outward = drawTossPose(qa, mc, start + 36, "flight-to-left");
        var leftCatch = drawTossPose(qa, mc, start + 41, "left-catch");
        qa.require(
            outward.x < release.x && outward.x > leftCatch.x && outward.y > Math.max(release.y, leftCatch.y) + .08f,
            "the first toss arcs from the right hand to the left");
        qa.require(
            leftArm.hand()
                .distance(idleLeftHand) > .10f,
            "the left hand reaches to catch the block");
        var leftRelease = drawTossPose(qa, mc, start + 47, "left-release");
        var returning = drawTossPose(qa, mc, start + 51, "flight-to-right");
        var rightCatch = drawTossPose(qa, mc, start + 56, "right-catch");
        qa.require(
            returning.x > leftRelease.x && returning.x < rightCatch.x
                && returning.y > Math.max(leftRelease.y, rightCatch.y) + .08f,
            "the second toss arcs from the left hand to the right");
    }

    private static Vector3f drawTossPose(AnimationClientChecks qa, Minecraft mc, int tick, String name)
        throws Exception {
        mc.thePlayer.ticksExisted = tick;
        qa.draw(mc, "block-inspect-" + name);
        return HmiFirstPerson.INSTANCE.item.getTranslation(new Vector3f());
    }

    private static double rotationChangeDegrees(Matrix4f start, Matrix4f end) {
        var from = start.getUnnormalizedRotation(new Quaternionf())
            .normalize();
        var to = end.getUnnormalizedRotation(new Quaternionf())
            .normalize();
        return Math.toDegrees(2 * Math.acos(Math.min(1, Math.abs(from.dot(to)))));
    }

    private static void press(KeyBinding key) {
        press(key.getKeyCode());
    }

    private static void press(int key) {
        KeyBinding.onTick(key);
        FMLCommonHandler.instance()
            .bus()
            .post(new InputEvent.KeyInputEvent());
    }

    private static final class ArmProbe extends ModelRenderer implements AutoCloseable {

        private final ModelBiped model;
        private final ModelRenderer arm;
        private final float centerX;
        private final FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        private final Matrix4f matrix = new Matrix4f();

        private ArmProbe(ModelBiped model, boolean left) {
            super(model);
            this.model = model;
            arm = left ? model.bipedLeftArm : model.bipedRightArm;
            centerX = (left ? 1 : -1) / 16f;
            arm.addChild(this);
        }

        @Override
        public void render(float scale) {
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
            matrix.set(buffer);
        }

        private Vector3f shoulder() {
            return matrix.transformPosition(new Vector3f(centerX, -2f / 16, 0));
        }

        private Vector3f hand() {
            return matrix.transformPosition(new Vector3f(centerX, 8f / 16, 0));
        }

        @Override
        public void close() {
            arm.childModels.remove(this);
            model.boxList.remove(this);
        }
    }
}
