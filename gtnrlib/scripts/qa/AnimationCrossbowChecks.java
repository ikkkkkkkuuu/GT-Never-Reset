package com.xyp.gtnrlib.qa;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.ItemStack;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.client.animation.FirstPersonItemActions;
import com.xyp.gtnrlib.client.animation.HmiFirstPerson;
import com.xyp.gtnrlib.compat.tconstruct.TinkersCrossbowAnimation;

import tconstruct.library.tools.ToolCore;
import tconstruct.weaponry.TinkerWeaponry;

public final class AnimationCrossbowChecks {

    private static final String FIRE = "gtnrlib:crossbow_fire";

    static void check(AnimationClientChecks qa, Minecraft mc) throws Exception {
        var p = mc.thePlayer;
        var actions = FirstPersonItemActions.INSTANCE;
        var hmi = HmiFirstPerson.INSTANCE;
        var bows = new ArrayList<ItemStack>();
        ToolCore crossbow = TinkerWeaponry.crossbow;
        crossbow.buildTool(0, null, bows);
        var bow = bows.get(0);
        var stats = bow.getTagCompound()
            .getCompoundTag("InfiTool");
        int tick = p.ticksExisted;
        boolean creative = p.capabilities.isCreativeMode, sneak = p.movementInput.sneak;
        var held = p.getHeldItem();
        int ammoSlot = (p.inventory.currentItem + 1) % 9;
        var previousAmmo = p.inventory.mainInventory[ammoSlot];
        try {
            hmi.detach();
            qa.equip(mc, bow);
            p.movementInput.sneak = false;
            p.capabilities.isCreativeMode = false;
            bow.useItemRightClick(mc.theWorld, p);
            qa.require(!stats.hasKey("Reloading") && !actions.playing(FIRE), "empty crossbow without bolts stays idle");
            var marked = TinkerWeaponry.creativeBolt.copy();
            var colors = marked.getTagCompound()
                .getCompoundTag("InfiTool");
            colors.setInteger("HeadColor", 0xffff00ff);
            colors.setInteger("AccessoryColor", 0xff00ff00);
            p.inventory.mainInventory[ammoSlot] = marked;
            for (int i = 0; i < 32; i++) {
                p.ticksExisted++;
                hmi.sample(bow, 0, 0);
            }
            var idle = qa.draw(mc, "crossbow-idle");
            qa.require(hmi.handheldTool() instanceof TinkersCrossbowAnimation, "equipped native two-handed crossbow");
            bow.useItemRightClick(mc.theWorld, p);
            int duration = stats.getInteger("DrawSpeed");
            qa.require(duration > 2 && stats.getInteger("Reloading") == duration, "started material-timed reload");
            int afterPull = checkStringDraw(qa, mc, bow, duration);
            int presentTick = Math.round(duration * .75f);
            advance(p, bow, presentTick - afterPull);
            var pull = qa.draw(mc, "crossbow-present-bolt");
            qa.require(
                !Arrays.equals(idle, pull) && Arrays.equals(pull, qa.draw(mc, "crossbow-pull-repeat")),
                "winding changes the grip and repeated rendering preserves the same frame");
            var tip = marker(pull, mc.displayWidth, true);
            var feathers = marker(pull, mc.displayWidth, false);
            qa.require(
                tip.distance(feathers) > mc.displayWidth * .09f,
                "visible shaft spans the fist between the actual colored head and fletching");
            int aboveTick = Math.round(duration * .85f);
            advance(p, bow, aboveTick - presentTick);
            var above = qa.draw(mc, "crossbow-above-rail");
            var aboveFeathers = marker(above, mc.displayWidth, false);
            qa.require(
                aboveFeathers.x - feathers.x > mc.displayWidth * .10f,
                "left hand carries the bolt from the left side over the groove");
            p.movementInput.sneak = true;
            bow.useItemRightClick(mc.theWorld, p);
            qa.require(!stats.hasKey("Reloading") && !actions.playing(FIRE), "sneaking cancels winding without recoil");
            p.ticksExisted++;
            var cancelled = qa.draw(mc, "crossbow-load-cancelled");
            qa.require(
                Arrays.stream(cancelled)
                    .noneMatch(pixel -> marked(pixel, true)),
                "cancelling removes the loose bolt");
            p.movementInput.sneak = false;
            bow.useItemRightClick(mc.theWorld, p);
            advance(p, bow, duration - 1);
            var released = qa.draw(mc, "crossbow-hand-released", .99f);
            advance(p, bow, 1);
            qa.require(stats.getBoolean("Loaded") && stats.hasKey("LoadedItem"), "native reload seats a bolt");
            var seated = qa.draw(mc, "crossbow-loaded");
            var seatedFeathers = marker(seated, mc.displayWidth, false);
            qa.require(seatedFeathers.y > aboveFeathers.y + 8, "bolt lowers into the groove and stays after release");
            qa.require(
                seatedFeathers.distance(marker(released, mc.displayWidth, false)) < 6,
                "last loose-bolt frame meets the loaded bolt at the same rail position");
            p.movementInput.sneak = true;
            bow.useItemRightClick(mc.theWorld, p);
            qa.require(!stats.getBoolean("Loaded") && !actions.playing(FIRE), "sneaking unloads a seated bolt");
            p.inventory.mainInventory[ammoSlot] = previousAmmo;
            p.capabilities.isCreativeMode = true;
            p.movementInput.sneak = false;
            bow.useItemRightClick(mc.theWorld, p);
            advance(p, bow, duration);
            qa.draw(mc, "crossbow-aim");
            var before = new Matrix4f(hmi.item);
            bow.useItemRightClick(mc.theWorld, p);
            qa.require(
                actions.playing(FIRE) && !stats.getBoolean("Loaded") && stats.hasKey("Reloading"),
                "firing starts recoil and native automatic reload together");
            advance(p, bow, 1);
            qa.draw(mc, "crossbow-fire", 0);
            qa.require(!before.equals(hmi.item, .03f), "shot moves the bow and both grips");
            for (int i = 2; i <= duration; i++) {
                advance(p, bow, 1);
                qa.draw(mc, "crossbow-cycle-" + i, 0);
            }
            qa.draw(mc, "crossbow-reloaded");
            qa.require(stats.getBoolean("Loaded") && !actions.playing(FIRE), "returns to aim with the next bolt");
        } finally {
            p.capabilities.isCreativeMode = creative;
            p.movementInput.sneak = sneak;
            p.inventory.mainInventory[ammoSlot] = previousAmmo;
            p.ticksExisted = tick;
            qa.equip(mc, held);
            hmi.detach();
        }
    }

    private static int checkStringDraw(AnimationClientChecks qa, Minecraft mc, ItemStack bow, int duration)
        throws Exception {
        var p = mc.thePlayer;
        var stats = bow.getTagCompound()
            .getCompoundTag("InfiTool");
        int originalMaterial = stats.getInteger("Accessory");
        var originalColor = stats.getTag("AccessoryColor");
        // Keep the actual material sprite and mark its pass cyan in the framebuffer.
        stats.setInteger("Accessory", -1);
        stats.setInteger("AccessoryColor", 0xff00ffff);
        try {
            var samples = new StringFrame[4];
            float[] phases = { .14f, .32f, .52f, .62f };
            int elapsed = 0;
            for (int i = 0; i < phases.length; i++) {
                int tick = Math.round(duration * phases[i]);
                advance(p, bow, tick - elapsed);
                elapsed = tick;
                samples[i] = stringFrame(qa, mc, bow, "crossbow-string-" + i);
            }
            var start = samples[0];
            var locked = samples[2];
            float travel = locked.nock.distance(start.nock);
            float midpoint = samples[1].nock.distance(start.nock);
            qa.require(
                travel > mc.displayWidth * .07f && midpoint > .20f * travel && midpoint < .80f * travel,
                "rendered nock travels " + midpoint + " px mid-pull and " + travel + " px to the catch");
            for (var frame : samples) {
                qa.require(
                    start.left.distance(frame.left) < 10 && start.right.distance(frame.right) < 10,
                    "both rendered string tips stay on the limbs");
            }
            var fistPull = new Vector2f(locked.fist).sub(start.fist);
            qa.require(
                fistPull.distance(new Vector2f(locked.nock).sub(start.nock)) < 22
                    && locked.fist.distance(locked.nock) < 50
                    && start.fist.distance(start.nock) < 50,
                "support fist follows the string from " + start + " to " + locked);
            qa.require(
                samples[3].nock.distance(locked.nock) < 5 && samples[3].fist.distance(locked.fist) > 55,
                "string stays on the catch when the left hand reaches for ammunition");
            return elapsed;
        } finally {
            stats.setInteger("Accessory", originalMaterial);
            if (originalColor == null) stats.removeTag("AccessoryColor");
            else stats.setTag("AccessoryColor", originalColor);
        }
    }

    private static StringFrame stringFrame(AnimationClientChecks qa, Minecraft mc, ItemStack bow, String name)
        throws Exception {
        var hmi = HmiFirstPerson.INSTANCE;
        var model = (TinkersCrossbowAnimation) hmi.handheldTool();
        var matrix = BufferUtils.createFloatBuffer(16);
        var fist = new Vector2f();
        int width = mc.displayWidth, height = mc.displayHeight;
        var pixels = qa.draw(mc, name, 0, () -> {
            // Look down on the actual rig. Record the left fist mount without covering the string pixels.
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glOrtho(-1, 1, -.7, .7, -8, 8);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glRotatef(90, 1, 0, 0);
            GL11.glTranslatef(-.1f, -.1f, 1.23f);
            GL11.glDisable(GL11.GL_LIGHTING);
            model.renderGripped(bow, hmi.forgeRenderer(), 0, left -> {
                if (left) {
                    GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrix);
                    fist.set((matrix.get(12) + 1) * width / 2, (.7f - matrix.get(13)) * height / 1.4f);
                }
            });
        });
        int min = width, max = -1;
        for (int i = 0; i < pixels.length; i++) {
            if (stringPixel(pixels[i])) {
                min = Math.min(min, i % width);
                max = Math.max(max, i % width);
            }
        }
        if (max - min < width / 4) throw new AssertionError("missing material string in " + name);
        var left = stringBand(pixels, width, min, min + 6);
        var right = stringBand(pixels, width, max - 6, max);
        // Find the middle of the bent cord, which moves sideways as well as back in this view.
        var chord = new Vector2f(right).sub(left);
        var nock = new Vector2f(left).lerp(right, .5f);
        float furthest = 0;
        for (int x = min + 10; x <= max - 10; x += 6) {
            var band = stringBand(pixels, width, x, x + 5);
            float deviation = Math.abs(chord.x * (band.y - left.y) - chord.y * (band.x - left.x)) / chord.length();
            if (deviation > Math.max(9, furthest)) {
                furthest = deviation;
                nock.set(band);
            }
        }
        return new StringFrame(fist, nock, left, right);
    }

    private static Vector2f stringBand(int[] pixels, int width, int minX, int maxX) {
        var sum = new Vector2f();
        int count = 0;
        for (int row = 0; row < pixels.length; row += width) {
            for (int x = minX; x <= maxX; x++) {
                if (stringPixel(pixels[row + x])) {
                    sum.add(x, row / width);
                    count++;
                }
            }
        }
        if (count == 0) throw new AssertionError("gap in crossbow string at " + minX);
        return sum.div(count);
    }

    private static boolean stringPixel(int rgb) {
        return (rgb >> 16 & 255) < 8 && (rgb >> 8 & 255) > 50 && (rgb & 255) > 50;
    }

    private record StringFrame(Vector2f fist, Vector2f nock, Vector2f left, Vector2f right) {}

    private static Vector2f marker(int[] pixels, int width, boolean head) {
        var center = new Vector2f();
        int count = 0;
        for (int i = 0; i < pixels.length; i++) {
            if (marked(pixels[i], head)) {
                center.add(i % width, i / width);
                count++;
            }
        }
        if (count < 12)
            throw new AssertionError("bolt " + (head ? "point" : "fletching") + " visible pixels: " + count);
        return center.div(count);
    }

    private static boolean marked(int rgb, boolean head) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        return head ? r > 40 && b > 40 && g < 8 : g > 40 && r < 8 && b < 8;
    }

    private static void advance(EntityClientPlayerMP player, ItemStack bow, int ticks) {
        for (int i = 0; i < ticks; i++) {
            player.ticksExisted++;
            bow.getItem()
                .onUpdate(bow, player.worldObj, player, player.inventory.currentItem, true);
        }
    }
}
