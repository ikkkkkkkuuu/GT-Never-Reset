package com.xyp.gtnrlib.qa;

import java.io.File;
import java.nio.file.Files;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;

import org.joml.Matrix4f;

import com.xyp.gtnrlib.client.animation.FirstPersonItemActions;
import com.xyp.gtnrlib.client.animation.HmiFirstPerson;

/** Observes two actual takeoffs, descents and collisions with the flat world's floor. One sample per game tick. */
final class AnimationMovementChecks {

    private final AnimationClientChecks qa;
    private final File output;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final int started = mc.thePlayer.ticksExisted;
    private final Matrix4f rise = new Matrix4f();
    private int lastTick = -1, stage, cycle, walkTick, videoFrame;
    private double startY;
    private boolean falling, switched;

    AnimationMovementChecks(AnimationClientChecks qa, File output) {
        this.qa = qa;
        this.output = output;
    }

    boolean step() throws Exception {
        var p = mc.thePlayer;
        int tick = p.ticksExisted;
        if (tick == lastTick) return false;
        lastTick = tick;
        if (tick - started > 220) throw new AssertionError("physical movement timed out at stage " + stage);
        var hmi = HmiFirstPerson.INSTANCE;
        if (stage == 1 || stage == 2) shot(String.format(Locale.ROOT, "sequence-%03d", videoFrame++));
        switch (stage) {
            case 0 -> {
                if (!p.onGround) return false;
                mc.gameSettings.chatVisibility = EntityPlayer.EnumChatVisibility.HIDDEN;
                mc.gameSettings.heldItemTooltips = false;
                p.rotationPitch = p.prevRotationPitch = 12;
                qa.equip(mc, new ItemStack(Items.iron_sword));
                startY = p.posY;
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
                walkTick = tick;
                stage++;
            }
            case 1 -> {
                if (tick < walkTick + 8) {
                    p.setAngles(24, 0);
                    return false;
                }
                qa.require(
                    Math.hypot(p.motionX, p.motionZ) > .04,
                    "walk driven by the local player's normal movement tick");
                shot("walk");
                p.jump();
                stage++;
            }
            case 2 -> {
                if (!p.onGround && p.motionY > 0 && p.posY > startY + .1 && !switched) {
                    if (cycle == 0) p.swingItem();
                    else p.setItemInUse(
                        p.getHeldItem(),
                        p.getHeldItem()
                            .getMaxItemUseDuration());
                    rise.set(hmi.item);
                    shot("rise-" + cycle);
                    switched = true;
                }
                if (switched && !falling && !p.onGround && p.motionY < -.05) {
                    qa.require(!rise.equals(hmi.item, .001f), "inertial pose changes from ascent to descent " + cycle);
                    shot("fall-" + cycle);
                    falling = true;
                }
                if (!falling || !p.onGround) return false;
                qa.require(Math.abs(p.posY - startY) < .05, "ground collision ends jump " + cycle);
                shot("land-" + cycle);
                if (++cycle == 1) {
                    qa.equip(mc, new ItemStack(Items.bow));
                    p.jump();
                    switched = falling = false;
                } else {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
                    p.clearItemInUse();
                    stage++;
                }
            }
            case 3 -> {
                Files.writeString(new File(output, "movement-frames.txt").toPath(), Integer.toString(videoFrame));
                checkJsonAction();
                return true;
            }
            default -> throw new AssertionError(stage);
        }
        return false;
    }

    private void checkJsonAction() throws Exception {
        var p = mc.thePlayer;
        var hmi = HmiFirstPerson.INSTANCE;
        var actions = FirstPersonItemActions.INSTANCE;
        var dye = new ItemStack(Items.dye, 1, 13);
        qa.equip(mc, dye);
        hmi.sample(dye, .5f, 0);
        qa.require(!actions.trigger("use"), "unbound behavior falls through on the JSON-bound item");
        var target = mc.objectMouseOver;
        int tick = p.ticksExisted;
        try {
            mc.objectMouseOver = null;
            p.isSwingInProgress = false;
            p.swingItem();
            qa.require(actions.playing("attack"), "accepted swing selected a metadata-bound JSON wrist clip");
            var start = new Matrix4f(hmi.item);
            p.ticksExisted += 3;
            qa.draw(mc, "json-wrist-attack");
            qa.require(!start.equals(hmi.item, .01f), "generic JSON action moves the rendered item");
            dye.setItemDamage(12);
            hmi.sample(dye, .75f, 0);
            qa.require(
                !actions.playing("attack") && !actions.trigger("attack"),
                "other metadata keeps its default swing");
            dye.setItemDamage(13);
            qa.require(actions.trigger("attack"), "same-slot binding can be retriggered");
            p.ticksExisted += 20;
            hmi.sample(dye, .5f, 0);
            qa.require(!actions.playing("attack"), "completed JSON action returns to the live base pose");
            var apple = new ItemStack(Items.golden_apple, 1, 1);
            qa.equip(mc, apple);
            p.setItemInUse(apple, apple.getMaxItemUseDuration());
            hmi.sample(apple, .5f, 0);
            qa.require(actions.playing("use"), "using the bound item starts its hold clip");
            p.ticksExisted += 12;
            hmi.sample(apple, .5f, 0);
            qa.require(
                actions.playing("use") && !actions.grip(false)
                    .equals(new Matrix4f(), .001f)
                    && !actions.grip(true)
                        .equals(new Matrix4f(), .001f),
                "both JSON arm tracks remain applied after the hold key");
            p.clearItemInUse();
            hmi.sample(apple, .75f, 0);
            p.ticksExisted += 3;
            hmi.sample(apple, .5f, 0);
            qa.require(!actions.playing("use"), "releasing an item fades out the hold clip");
        } finally {
            p.clearItemInUse();
            mc.objectMouseOver = target;
            p.ticksExisted = tick;
            hmi.detach();
        }
    }

    private void shot(String name) {
        ScreenShotHelper.saveScreenshot(
            output,
            "world-default-" + name + ".png",
            mc.displayWidth,
            mc.displayHeight,
            mc.getFramebuffer());
    }
}
