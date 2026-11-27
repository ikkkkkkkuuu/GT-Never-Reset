package com.xyp.gtnrlib.qa;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;

import org.joml.Vector3f;

import com.xyp.gtnrlib.client.animation.FirstPersonItemActions;
import com.xyp.gtnrlib.client.animation.HmiFirstPerson;
import com.xyp.gtnrlib.client.input.KeyBindingLookup;

import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.wands.ItemWandCasting;

final class AnimationSpecialToolInspectChecks {

    static final String SCREWDRIVER = "gtnrlib:screwdriver_inspect";
    static final String CROSSBOW = "gtnrlib:crossbow_inspect";
    static final String WAND = "gtnrlib:wand_inspect";

    static void verify(AnimationClientChecks qa, Minecraft mc) throws Exception {
        var player = mc.thePlayer;
        var held = player.getHeldItem();
        int tick = player.ticksExisted;
        var model = ((RenderPlayer) RenderManager.instance.getEntityRenderObject(player)).modelBipedMain;
        var key = key(qa);
        try (var right = new AnimationBlockInspectChecks.ArmProbe(model, false);
            var left = new AnimationBlockInspectChecks.ArmProbe(model, true)) {
            checkTool(
                qa,
                mc,
                right,
                left,
                key,
                AnimationToolChecks.screwdriver(),
                "screwdriver",
                SCREWDRIVER,
                .54,
                1.09,
                2.20);
            player.ticksExisted = tick;
            checkTool(
                qa,
                mc,
                right,
                left,
                key,
                AnimationCrossbowChecks.buildCrossbow(),
                "crossbow",
                CROSSBOW,
                .82,
                1.32,
                2.46);
            player.ticksExisted = tick;
            var wand = AnimationToolChecks.greatwoodWand();
            checkTool(qa, mc, right, left, key, wand, "wand", WAND, .61, 1.21, 2.40);
            player.ticksExisted = tick;
            var staff = wand.copy();
            ((ItemWandCasting) staff.getItem()).setRod(staff, ConfigItems.STAFF_ROD_SILVERWOOD);
            checkTool(qa, mc, right, left, key, staff, "staff", WAND, .61, 1.21, 2.40);
        } finally {
            player.ticksExisted = tick;
            qa.equip(mc, held);
            qa.tickPlayer();
            HmiFirstPerson.INSTANCE.detach();
        }
    }

    static KeyBinding key(AnimationClientChecks qa) {
        var key = KeyBindingLookup.find("gtnrlib.key.inspect_block", null);
        qa.require(key != null, "inspection key is available for special tools");
        return key;
    }

    private static void checkTool(AnimationClientChecks qa, Minecraft mc, AnimationBlockInspectChecks.ArmProbe right,
        AnimationBlockInspectChecks.ArmProbe left, KeyBinding key, ItemStack tool, String name, String clip,
        double firstView, double secondView, double duration) throws Exception {
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
        var shoulder = right.shoulder();
        var rightRest = right.palm();
        var leftRest = left.palm();
        var saved = tool.copy();
        boolean swinging = player.isSwingInProgress;
        AnimationBlockInspectChecks.press(key);
        qa.require(actions.playing(clip), "the inspection key starts the " + name + " clip");

        float firstPartial = drawPose(qa, mc, start, firstView, name + "-inspect-raised");
        var first = inspectionAngle(tool, clip, firstPartial);
        qa.require(
            right.palm()
                .distance(rightRest) > .04f,
            name + " moves into inspection view");
        if (CROSSBOW.equals(clip)) {
            qa.require(
                left.palm()
                    .distance(leftRest) > .04f,
                "the crossbow support hand follows the rail");
        }
        float secondPartial = drawPose(qa, mc, start, secondView, name + "-inspect-tilted");
        float turn = inspectionAngle(tool, clip, secondPartial).distance(first);
        qa.require(turn > .15f, name + " takes a different inspection angle, change " + turn);
        if (!CROSSBOW.equals(clip)) {
            float drift = right.shoulder()
                .distance(shoulder);
            qa.require(drift < .08f, name + " shoulder stays anchored during inspection, drift " + drift);
        }
        AnimationBlockInspectChecks.press(key);

        drawPose(qa, mc, start, duration + .35, name + "-inspect-restored");
        qa.require(
            !actions.playing(clip) && right.palm()
                .distance(rightRest) < .10f,
            name + " returns to its normal grip despite a repeated press");
        qa.require(
            player.getHeldItem() == tool && ItemStack.areItemStacksEqual(tool, saved)
                && player.isSwingInProgress == swinging
                && !player.isUsingItem(),
            name + " inspection preserves held tool and gameplay state");
    }

    private static Vector3f inspectionAngle(ItemStack tool, String clip, float partial) {
        if (CROSSBOW.equals(clip)) {
            var rig = FirstPersonItemActions.INSTANCE.sampleRigInspection(tool, partial);
            var rotation = rig.rotation();
            return new Vector3f((float) rotation.x(), (float) rotation.y(), (float) rotation.z())
                .mul((float) Math.PI / 180);
        }
        var face = SCREWDRIVER.equals(clip) ? new Vector3f(1, 0, 0) : new Vector3f(0, 0, 1);
        return HmiFirstPerson.INSTANCE.item.transformDirection(face)
            .normalize();
    }

    private static float drawPose(AnimationClientChecks qa, Minecraft mc, int start, double seconds, String name)
        throws Exception {
        double elapsed = seconds * 20;
        int ticks = (int) elapsed;
        float partial = (float) (elapsed - ticks);
        mc.thePlayer.ticksExisted = start + ticks;
        qa.draw(mc, name, partial);
        return partial;
    }
}
