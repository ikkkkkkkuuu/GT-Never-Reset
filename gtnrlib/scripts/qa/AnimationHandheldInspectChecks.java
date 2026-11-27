package com.xyp.gtnrlib.qa;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.joml.Vector3f;

import com.xyp.gtnrlib.client.animation.FirstPersonItemActions;
import com.xyp.gtnrlib.client.animation.HmiFirstPerson;
import com.xyp.gtnrlib.client.input.KeyBindingLookup;

final class AnimationHandheldInspectChecks {

    private static final String SWORD = "gtnrlib:sword_inspect", PICKAXE = "gtnrlib:pickaxe_inspect";

    static void verify(AnimationClientChecks qa, Minecraft mc) throws Exception {
        var key = KeyBindingLookup.find("gtnrlib.key.inspect_block", null);
        qa.require(key != null, "inspection key is registered for ordinary tools");
        var player = mc.thePlayer;
        var held = player.getHeldItem();
        int tick = player.ticksExisted;
        var model = ((RenderPlayer) RenderManager.instance.getEntityRenderObject(player)).modelBipedMain;
        try (var arm = new AnimationBlockInspectChecks.ArmProbe(model, false)) {
            qa.equip(mc, new ItemStack(Items.iron_axe));
            AnimationBlockInspectChecks.press(key);
            var actions = FirstPersonItemActions.INSTANCE;
            qa.require(!actions.playing(SWORD) && !actions.playing(PICKAXE), "an axe ignores the two inspection clips");
            checkTool(qa, mc, arm, key, new ItemStack(Items.iron_sword), "sword", SWORD, .55, 1.07, 2.18);
            player.ticksExisted = tick;
            checkTool(qa, mc, arm, key, new ItemStack(Items.iron_pickaxe), "pickaxe", PICKAXE, .62, 1.18, 2.40);
        } finally {
            player.ticksExisted = tick;
            qa.equip(mc, held);
            qa.tickPlayer();
            HmiFirstPerson.INSTANCE.detach();
        }
    }

    private static void checkTool(AnimationClientChecks qa, Minecraft mc, AnimationBlockInspectChecks.ArmProbe arm,
        KeyBinding key, ItemStack tool, String name, String clip, double firstView, double secondView, double duration)
        throws Exception {
        var player = mc.thePlayer;
        var hmi = HmiFirstPerson.INSTANCE;
        var actions = FirstPersonItemActions.INSTANCE;
        qa.equip(mc, tool);
        hmi.detach();
        for (int i = 0; i < 30; i++) {
            player.ticksExisted++;
            hmi.sample(tool, 0, 0);
        }
        int start = player.ticksExisted;
        qa.draw(mc, name + "-inspect-idle", 0);
        var shoulder = arm.shoulder();
        var idleHand = arm.hand();
        var idleItem = hmi.item.getTranslation(new Vector3f());
        var saved = tool.copy();
        boolean swinging = player.isSwingInProgress;
        AnimationBlockInspectChecks.press(key);
        qa.require(actions.playing(clip), "the inspection key starts the " + name + " clip");

        int[] raised = drawPose(qa, mc, start, firstView, name + "-inspect-raised");
        var face = hmi.item.transformDirection(new Vector3f(0, 0, 1))
            .normalize();
        float lift = arm.hand().y - idleHand.y;
        qa.require(lift > .07f, name + " rises into view by " + lift);
        int[] tilted = drawPose(qa, mc, start, secondView, name + "-inspect-tilted");
        float turn = hmi.item.transformDirection(new Vector3f(0, 0, 1))
            .normalize()
            .distance(face);
        qa.require(turn > .15f, name + " presents a different face, direction change " + turn);
        float drift = arm.shoulder()
            .distance(shoulder);
        qa.require(drift < .06f, name + " shoulder stays anchored during the turn, drift " + drift);
        qa.require(
            clearTop(raised, mc.displayWidth) && clearTop(tilted, mc.displayWidth),
            name + " has 8 pixels of clearance above both inspection views");
        AnimationBlockInspectChecks.press(key);

        drawPose(qa, mc, start, duration + .35, name + "-inspect-recovered");
        qa.require(!actions.playing(clip), name + " returns on schedule despite a repeated press");
        qa.require(
            hmi.item.getTranslation(new Vector3f())
                .distance(idleItem) < .10f,
            name + " returns to its normal grip");
        qa.require(
            player.getHeldItem() == tool && ItemStack.areItemStacksEqual(tool, saved)
                && player.isSwingInProgress == swinging
                && !player.isUsingItem(),
            name + " inspection preserves the held tool and gameplay state");
    }

    private static int[] drawPose(AnimationClientChecks qa, Minecraft mc, int start, double seconds, String name)
        throws Exception {
        double elapsed = seconds * 20;
        int ticks = (int) elapsed;
        mc.thePlayer.ticksExisted = start + ticks;
        return qa.draw(mc, name, (float) (elapsed - ticks));
    }

    private static boolean clearTop(int[] image, int width) {
        return Arrays.stream(image, 0, 8 * width)
            .allMatch(pixel -> pixel == image[0]);
    }
}
