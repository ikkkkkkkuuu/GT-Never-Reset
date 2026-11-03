package com.xyp.gtnrlib.qa;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.api.animation.AnimationClip.Marker;
import com.xyp.gtnrlib.client.animation.FirstPersonAnimations;
import com.xyp.gtnrlib.config.GTNRLibConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Separate non-shipping mod. Exercises the patched vanilla renderer against deterministic projections. */
@Mod(modid = "animationclientqa", name = "GTNRLib Animation QA", version = "1", dependencies = "after:gtnrlib")
public final class AnimationClientChecks {

    private static final int WORLD_WARMUP_FRAMES = 20;
    private final File output = new File(System.getProperty("gtnrlib.animation.qa.output", "."));
    private boolean started, finished;
    private int ticks, frames;
    private final FirstPersonAnimations target = FirstPersonAnimations.INSTANCE;
    private final List<String> results = new ArrayList<>();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnrlib.animation.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
            if (++ticks > 3600) throw new IllegalStateException("fixture timed out");
            if (!started && mc.currentScreen != null && mc.theWorld == null) {
                started = true;
                mc.launchIntegratedServer(
                    "animation-qa",
                    "Animation QA",
                    new WorldSettings(4711, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
        } catch (Throwable e) {
            finish(mc, e);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (finished || !started
            || mc.thePlayer == null
            || mc.currentScreen != null
            || event.phase != TickEvent.Phase.END
            || ++frames < WORLD_WARMUP_FRAMES) return;
        try {
            var sword = new ItemStack(Items.diamond_sword);
            if (frames == WORLD_WARMUP_FRAMES) {
                mc.gameSettings.heldItemTooltips = false;
                reload(mc, "animation-qa");
                equip(mc, sword);
                tickPlayer();
                return;
            }
            // Capture two ordinary frames before the isolated draws, with the game's lighting, projection and HUD.
            boolean holdingSword = frames == WORLD_WARMUP_FRAMES + 1;
            ScreenShotHelper.saveScreenshot(
                output,
                holdingSword ? "world-grip.png" : "world-empty.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            if (holdingSword) {
                equip(mc, null);
                tickPlayer();
                return;
            }
            reload(mc, "");
            equip(mc, sword);
            require(!target.enabled(), "library alone is inactive");
            int[] baseline = draw(mc, "01-vanilla");
            equip(mc, null);
            int[] emptyBaseline = draw(mc, "empty-vanilla");
            equip(mc, sword);
            require(
                Arrays.stream(baseline)
                    .distinct()
                    .count() > 8,
                "nonempty vanilla silhouette");
            reload(mc, "animation-qa");
            tickPlayer();
            require(target.applies(sword) && !target.dualHanded(), "example sword uses primary hand");
            int[] active = draw(mc, "02-grip");
            equip(mc, null);
            tickPlayer();
            draw(mc, "empty-grip");
            checkArmVisibility(mc, sword);
            checkHandMotion(mc, sword);
            require(!Arrays.equals(baseline, active), "render injection changed first-person output");
            var observed = new ArrayList<String>();
            target.bind("fixture:observer", (held, action) -> {
                observed.add(action);
                return null;
            });
            var previousHit = mc.objectMouseOver;
            mc.objectMouseOver = null;
            var click = Minecraft.class.getDeclaredMethod("func_147116_af");
            click.setAccessible(true);
            click.invoke(mc);
            advanceHands(mc, 2);
            int[] beforeRepeat = draw(mc, "sword-before-early-repeat");
            int requests = java.util.Collections.frequency(observed, "attack");
            click.invoke(mc);
            require(
                requests == java.util.Collections.frequency(observed, "attack")
                    && Arrays.equals(beforeRepeat, draw(mc, "sword-after-early-repeat")),
                "ignored early click does not restart sword pose");
            advanceHands(mc, 2);
            require(
                observed.contains("attack") && !target.dualHanded(),
                "accepted left click dispatched single-hand attack");
            require(!Arrays.equals(active, draw(mc, "03-swing")), "attack changes grip");
            for (int step = 0; step < 4; step++) {
                int[] before = draw(mc, "sword-return-" + step + "-before", 1);
                advanceHands(mc, 1);
                require(
                    Arrays.equals(before, draw(mc, "sword-return-" + step + "-after", 0)),
                    "sword recovery is continuous across tick " + step);
            }
            int x = (int) mc.thePlayer.posX, y = (int) mc.thePlayer.posY + 3, z = (int) mc.thePlayer.posZ;
            mc.objectMouseOver = new net.minecraft.util.MovingObjectPosition(
                x,
                y,
                z,
                1,
                net.minecraft.util.Vec3.createVectorHelper(x, y, z));
            equip(mc, new ItemStack(Items.iron_axe));
            tickPlayer();
            advanceHands(mc, 8);
            int[] axeIdle = draw(mc, "axe-idle");
            var attackKey = mc.gameSettings.keyBindAttack;
            net.minecraft.client.settings.KeyBinding.setKeyBindState(attackKey.getKeyCode(), true);
            try {
                mc.thePlayer.swingItem();
                advanceHands(mc, 5);
                require(
                    observed.contains("mining") && !target.dualHanded()
                        && target.replacesSwing(mc.thePlayer.getHeldItem())
                        && mc.thePlayer.getSwingProgress(.5f) > 0,
                    "axe clip replaces only the visual swing");
                int idleX = grayItem(axeIdle, mc.displayWidth).x;
                int strikeX = grayItem(draw(mc, "axe-chop"), mc.displayWidth).x;
                require(
                    strikeX > 0 && idleX - strikeX > mc.displayHeight * .12,
                    "axe head swings inward: " + idleX + " -> " + strikeX);
            } finally {
                net.minecraft.client.settings.KeyBinding.setKeyBindState(attackKey.getKeyCode(), false);
                mc.objectMouseOver = previousHit;
            }
            advanceHands(mc, 8);
            require(Arrays.equals(axeIdle, draw(mc, "axe-recovered")), "axe returns to its grip after mining stops");
            // Release on the first tick of a new native swing. The pack's short fade finishes before that swing does.
            // Give each run a fresh target. Repeated creative clicks otherwise excavate the flat test world.
            int tapX = x - 2, tapY = y;
            mc.theWorld.setBlock(tapX, tapY, z, Blocks.log);
            mc.objectMouseOver = new net.minecraft.util.MovingObjectPosition(
                tapX,
                tapY,
                z,
                1,
                net.minecraft.util.Vec3.createVectorHelper(tapX + .5, tapY + 1, z + .5));
            try {
                click.invoke(mc);
            } finally {
                mc.theWorld.setBlockToAir(tapX, tapY, z);
                mc.objectMouseOver = previousHit;
            }
            advanceHands(mc, 4);
            require(
                mc.thePlayer.getSwingProgress(.5f) > 0 && Arrays.equals(axeIdle, draw(mc, "axe-tap-released")),
                "quick mining release stays at rest while vanilla swing finishes");
            advanceHands(mc, 4);
            require(!target.replacesSwing(mc.thePlayer.getHeldItem()), "native swing ownership released at rest");
            checkPlacement(mc, x + 2, z, observed);
            target.unbind("fixture:observer");
            equip(mc, new ItemStack(Items.filled_map));
            tickPlayer();
            require(!target.applies(mc.thePlayer.getHeldItem()), "map uses original path");
            if (Boolean.getBoolean("gtnrlib.animation.qa.host")) checkHost(mc);
            equip(mc, sword);
            reload(mc, "");
            tickPlayer();
            require(Arrays.equals(baseline, draw(mc, "04-disabled")), "unload restores original pixels");
            equip(mc, null);
            require(Arrays.equals(emptyBaseline, draw(mc, "empty-disabled")), "unload restores vanilla empty arm");
            equip(mc, sword);
            if (!Boolean.getBoolean("gtnrlib.animation.qa.host")) checkRepositoryReload(mc, sword);
            finish(mc, null);
        } catch (Throwable e) {
            finish(mc, e);
        }
    }

    private void checkPlacement(Minecraft mc, int x, int z, List<String> observed) throws Exception {
        equip(mc, new ItemStack(Blocks.cobblestone));
        tickPlayer();
        int[] carry = draw(mc, "block-carry");
        int[] start = draw(mc, "block-at-input", 0);
        int ground = mc.theWorld.getTopSolidOrLiquidBlock(x, z) - 1;
        placeDuringTick(mc, x, ground, z);
        require(
            mc.theWorld.getBlock(x, ground + 1, z) == Blocks.cobblestone && mc.thePlayer.isSwingInProgress
                && observed.contains("useitem"),
            "normal right click placed cobblestone");
        require(Arrays.equals(start, draw(mc, "block-first-render", 0)), "placement renders its first interval");
        int width = mc.displayWidth, h = mc.displayHeight;
        var rest = grayItem(carry, width);
        var launch = grayItem(draw(mc, "block-launch", .75f), width);
        advanceHands(mc, 2);
        var extended = grayItem(draw(mc, "block-extended"), width);
        require(
            !target.dualHanded() && rest.x > width / 2 + .43 * h
                && extended.x > width / 2 + .10 * h
                && extended.x < width / 2 + .30 * h
                && extended.y > .58 * h
                && extended.y < .77 * h
                && extended.width < .78 * rest.width
                && launch.x > extended.x
                && launch.y > extended.y + .12 * h
                && launch.width < .92 * rest.width,
            "depth leads lift: " + rest + " -> " + launch + " -> " + extended);
        advanceHands(mc, 1);
        int[] beforeInput = draw(mc, "block-before-repeat", 1);
        var before = grayItem(beforeInput, width);
        int wrist = wristX(beforeInput, width);
        require(
            before.x > extended.x + .04 * h && before.x < rest.x - .10 * h,
            "first return still in flight at four ticks: " + before);
        int nextGround = mc.theWorld.getTopSolidOrLiquidBlock(x, z + 1) - 1;
        placeDuringTick(mc, x, nextGround, z + 1);
        require(
            mc.theWorld.getBlock(x, nextGround + 1, z + 1) == Blocks.cobblestone,
            "second right click placed cobblestone");
        int[] atInput = draw(mc, "block-repeat-start", 0);
        var after = grayItem(atInput, width);
        require(
            wrist >= 0 && Math.abs(wristX(atInput, width) - wrist) <= 1
                && Math.abs(after.x - before.x) <= 1
                && Math.abs(after.y - before.y) <= 1
                && Math.abs(after.width - before.width) <= 1,
            "repeat starts at the outgoing grip: " + before + " -> " + after);
        int outermost = after.x, outermostWrist = wrist;
        for (int tick = 0; tick < 2; tick++) {
            for (int quarter = 1; quarter <= 4; quarter++) {
                int[] pose = draw(mc, "block-handoff-" + tick + "-" + quarter, quarter * .25f);
                outermost = Math.max(outermost, grayItem(pose, width).x);
                outermostWrist = Math.max(outermostWrist, wristX(pose, width));
            }
            advanceHands(mc, 1);
        }
        require(
            outermost - after.x < h * .045 && outermostWrist - wrist < h * .055,
            "maximum outward travel at repeat: block " + (outermost - after.x)
                + "px, wrist "
                + (outermostWrist - wrist)
                + "px");
        var repeated = grayItem(draw(mc, "block-repeat"), width);
        require(
            Math.abs(repeated.x - extended.x) <= 1 && Math.abs(repeated.width - extended.width) <= 1,
            "second placement reaches the same contact pose");
        int lastX = repeated.x;
        for (int tick = 3; tick < 8; tick++) {
            advanceHands(mc, 1);
            for (int half = 0; half < 2; half++) {
                var point = grayItem(draw(mc, "block-return-" + tick + "-" + half, half * .5f), width);
                require(
                    point.x >= lastX - 2 && point.x <= rest.x + 2,
                    "shared grip returns without reversing at " + (tick + .5 * half) + " ticks: " + point);
                lastX = point.x;
            }
        }
        advanceHands(mc, 2);
        require(Arrays.equals(carry, draw(mc, "block-recovered")), "release settles to the original carry pose");
        mc.playerController.clickBlock(x, ground + 1, z, 1);
        mc.playerController.clickBlock(x, nextGround + 1, z + 1, 1);
    }

    private void placeDuringTick(Minecraft mc, int x, int y, int z) throws Exception {
        var markers = new ArrayList<String>();
        Consumer<Marker> listener = m -> markers.add(m.name());
        target.addListener(listener);
        try {
            target.tick(new TickEvent.ClientTickEvent(TickEvent.Phase.START));
            placeOnTop(mc, x, y, z);
            finishHandTick(mc);
            require(
                java.util.Collections.frequency(markers, "gtnrlib:chain_placement") == 1,
                "new placement fires its zero-time marker in the input tick");
        } finally {
            target.removeListener(listener);
        }
    }

    private static void placeOnTop(Minecraft mc, int x, int y, int z) throws Exception {
        var previous = mc.objectMouseOver;
        var click = Minecraft.class.getDeclaredMethod("func_147121_ag");
        click.setAccessible(true);
        try {
            mc.objectMouseOver = new net.minecraft.util.MovingObjectPosition(
                x,
                y,
                z,
                1,
                net.minecraft.util.Vec3.createVectorHelper(x + .5, y + 1, z + .5));
            click.invoke(mc);
        } finally {
            mc.objectMouseOver = previous;
        }
    }

    private void advanceHands(Minecraft mc, int ticks) throws Exception {
        for (int i = 0; i < ticks; i++) {
            target.tick(new TickEvent.ClientTickEvent(TickEvent.Phase.START));
            finishHandTick(mc);
        }
    }

    private void finishHandTick(Minecraft mc) throws Exception {
        var swing = EntityLivingBase.class.getDeclaredMethod("updateArmSwingProgress");
        swing.setAccessible(true);
        mc.thePlayer.prevSwingProgress = mc.thePlayer.swingProgress;
        swing.invoke(mc.thePlayer);
        mc.entityRenderer.itemRenderer.updateEquippedItem();
        target.tick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
    }

    // In the empty-hand and cobblestone draws the skin supplies all colored texels. Track its upper edge.
    private static int wristX(int[] pixels, int width) {
        long sum = 0;
        int count = 0, bottom = pixels.length;
        for (int i = 0; i < bottom; i++) {
            int rgb = pixels[i], r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
            if (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) <= 6) continue;
            if (count == 0) bottom = Math.min(pixels.length, (i / width + 12) * width);
            sum += i % width;
            count++;
        }
        return count == 0 ? -1 : (int) (sum / count);
    }

    private record Footprint(int x, int y, int width) {}

    private static Footprint grayItem(int[] pixels, int width) {
        // Cobblestone, iron ingots and axe heads have gray texels against the dark (32, 32, 32) background.
        long sumX = 0, sumY = 0;
        int count = 0, minX = width, maxX = -1;
        for (int i = 0; i < pixels.length; i++) {
            int rgb = pixels[i], r = rgb >> 16 & 255;
            if (r > 48 && r == (rgb >> 8 & 255) && r == (rgb & 255)) {
                int x = i % width;
                sumX += x;
                sumY += i / width;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                count++;
            }
        }
        return count == 0 ? new Footprint(-1, -1, 0)
            : new Footprint((int) (sumX / count), (int) (sumY / count), maxX - minX + 1);
    }

    private void checkHandMotion(Minecraft mc, ItemStack sword) throws Exception {
        require(target.handOffset(.5f) == null, "sword keeps its existing motion profile");
        equip(mc, new ItemStack(Items.fishing_rod));
        tickPlayer();
        require(target.handOffset(.5f) == null, "fishing rod stays outside the flat item profile");
        var ingot = new ItemStack(Items.iron_ingot);
        equip(mc, null);
        tickPlayer();
        checkEmptyHand(mc);
        var local = mc.thePlayer;
        local.inventory.mainInventory[local.inventory.currentItem] = ingot;
        advanceHands(mc, 1);
        require(target.applies(null) && !target.applies(ingot), "lowering keeps the outgoing empty hand");
        target.trigger("attack");
        require(target.applies(null), "a click during the switch retains the outgoing pose");
        advanceHands(mc, 3);
        require(
            target.replacesSwing(ingot) && !target.applies(null) && target.applies(ingot),
            "raising plays the incoming item action");
        advanceHands(mc, 8);
        int[] held = draw(mc, "ingot-carry");
        require(grayItem(held, mc.displayWidth).x > mc.displayWidth / 2, "ingot sprite visible at the grip");
        target.trigger("attack");
        advanceHands(mc, 2);
        require(!Arrays.equals(held, draw(mc, "ingot-swing")), "ordinary item swings with the hand");
        advanceHands(mc, 6);
        require(Arrays.equals(held, draw(mc, "ingot-return")), "ordinary item swing recovers to its grip");

        double prevX = local.prevPosX, prevY = local.prevPosY, prevZ = local.prevPosZ;
        float oldYaw = local.rotationYaw, oldPrevYaw = local.prevRotationYaw;
        try {
            local.prevPosX = local.posX;
            local.prevPosY = local.posY;
            local.prevPosZ = local.posZ;
            local.rotationYaw = local.prevRotationYaw = 160;
            advanceHands(mc, 20);
            var rest = target.handOffset(.5f);
            local.prevPosY -= .35;
            for (int i = 0; i < 7; i++) {
                local.rotationYaw = local.prevRotationYaw = net.minecraft.util.MathHelper
                    .wrapAngleTo180_float(local.rotationYaw + 18);
                double angle = Math.toRadians(local.rotationYaw);
                local.prevPosX = local.posX + .20 * Math.sin(angle);
                local.prevPosZ = local.posZ - .20 * Math.cos(angle);
                advanceHands(mc, 1);
            }
            var lag = target.handOffset(.5f);
            require(
                lag.x() < rest.x() - .02 && lag.y() < rest.y() - .01 && lag.z() > rest.z() + .02,
                "turn, ascent and forward motion produce camera-space lag");
            int[] moving = draw(mc, "ingot-inertia");
            require(
                !Arrays.equals(held, moving) && Arrays.equals(moving, draw(mc, "ingot-extra-render")),
                "extra render pass does not advance inertia");
            var boundary = target.handOffset(1);
            local.prevPosX = local.posX;
            local.prevPosY = local.posY;
            local.prevPosZ = local.posZ;
            advanceHands(mc, 1);
            // advanceHands steps the hand subsystem only. Match the outer player clock for this boundary.
            local.ticksExisted++;
            try {
                require(boundary.equals(target.handOffset(0)), "motion interpolates across tick boundaries");
            } finally {
                local.ticksExisted--;
            }
            advanceHands(mc, 24);
            var stopped = target.handOffset(.5f);
            require(
                Math.abs(stopped.x() - rest.x()) + Math.abs(stopped.y() - rest.y()) + Math.abs(stopped.z() - rest.z())
                    < .0001,
                "stopping settles to breathing at rest");
            local.prevPosX = local.posX - 8;
            advanceHands(mc, 1);
            require(
                Math.abs(
                    target.handOffset(1)
                        .x())
                    < .0001,
                "teleport clears simulated lag");
        } finally {
            local.prevPosX = prevX;
            local.prevPosY = prevY;
            local.prevPosZ = prevZ;
            local.rotationYaw = oldYaw;
            local.prevRotationYaw = oldPrevYaw;
        }
        equip(mc, sword);
        tickPlayer();
    }

    private void checkEmptyHand(Minecraft mc) throws Exception {
        int width = mc.displayWidth, h = mc.displayHeight;
        int[] rest = draw(mc, "empty-relaxed");
        int restX = wristX(rest, width);
        var local = mc.thePlayer;
        local.ticksExisted += 30;
        try {
            int[] breath = draw(mc, "empty-breathing");
            require(
                !Arrays.equals(rest, breath) && Math.abs(restX - wristX(breath, width)) < .06 * h
                    && Arrays.equals(breath, draw(mc, "empty-breathing-extra-pass")),
                "idle breathing is small and frame-stable");
        } finally {
            local.ticksExisted -= 30;
        }
        var previousHit = mc.objectMouseOver;
        var click = Minecraft.class.getDeclaredMethod("func_147116_af");
        click.setAccessible(true);
        int x = (int) local.posX, y = (int) local.posY + 4, z = (int) local.posZ;
        var center = net.minecraft.util.Vec3.createVectorHelper(x + .5, y + .5, z + .5);
        var miss = new net.minecraft.util.MovingObjectPosition(x, y, z, -1, center, false);
        var markers = new ArrayList<String>();
        Consumer<Marker> listener = marker -> {
            if ("gtnrlib:empty_punch".equals(marker.name())) markers.add(marker.name());
        };
        target.addListener(listener);
        mc.theWorld.setBlock(x, y, z, Blocks.log);
        try {
            mc.objectMouseOver = new net.minecraft.util.MovingObjectPosition(x, y, z, 1, center);
            leftClickDuringTick(mc, click);
            require(markers.size() == 1 && local.isSwingInProgress, "tap on a real block starts one empty punch");
            mc.objectMouseOver = miss;
            leftClickDuringTick(mc, click);
            int[] contact = draw(mc, "empty-punch");
            int inside = wristX(contact, width);
            advanceHands(mc, 2);
            int outside = wristX(draw(mc, "empty-follow-through"), width);
            require(
                markers.size() == 1 && inside >= 0 && restX - inside > .22 * h && outside > restX + .04 * h,
                "released punch travels inward and returns on the outside: " + restX
                    + " -> "
                    + inside
                    + " -> "
                    + outside);
            advanceHands(mc, 4);
            require(Arrays.equals(rest, draw(mc, "empty-return")), "released block tap settles at the empty grip");

            leftClickDuringTick(mc, click);
            advanceHands(mc, 3);
            int outgoing = wristX(draw(mc, "empty-before-repeat", 1), width);
            leftClickDuringTick(mc, click);
            int incoming = wristX(draw(mc, "empty-repeat-start", 0), width);
            require(
                markers.size() == 3 && outgoing >= 0 && Math.abs(incoming - outgoing) <= 2,
                "accepted air clicks join at the outgoing wrist: " + outgoing + " -> " + incoming);
            advanceHands(mc, 1);
            require(Arrays.equals(contact, draw(mc, "empty-repeat-contact")), "repeat reaches the same inward strike");
            advanceHands(mc, 7);
            require(
                Arrays.equals(rest, draw(mc, "empty-repeat-return")),
                "repeated air punches settle at the empty grip");
        } finally {
            target.removeListener(listener);
            mc.theWorld.setBlockToAir(x, y, z);
            mc.objectMouseOver = previousHit;
        }

        double prevZ = local.prevPosZ;
        boolean grounded = local.onGround, bobbing = mc.gameSettings.viewBobbing;
        var atRest = target.emptyHandRotation(1);
        try {
            local.onGround = mc.gameSettings.viewBobbing = true;
            local.prevPosZ = local.posZ - .18;
            advanceHands(mc, 6);
            var walking = target.emptyHandRotation(1);
            require(
                !walking.equals(atRest) && walking.equals(target.emptyHandRotation(1)),
                "distance advances empty-hand gait once per tick");
            mc.gameSettings.viewBobbing = false;
            advanceHands(mc, 10);
            require(atRest.equals(target.emptyHandRotation(1)), "empty-hand gait settles with view bobbing disabled");
        } finally {
            local.prevPosZ = prevZ;
            local.onGround = grounded;
            mc.gameSettings.viewBobbing = bobbing;
            advanceHands(mc, 24);
        }
    }

    private void leftClickDuringTick(Minecraft mc, java.lang.reflect.Method click) throws Exception {
        target.tick(new TickEvent.ClientTickEvent(TickEvent.Phase.START));
        click.invoke(mc);
        finishHandTick(mc);
    }

    private void checkArmVisibility(Minecraft mc, ItemStack sword) throws Exception {
        var textures = mc.getTextureManager();
        var skin = mc.thePlayer.getLocationSkin();
        textures.bindTexture(skin);
        var original = textures.getTexture(skin);
        var markers = new DynamicTexture(64, 32);
        int[] texels = markers.getTextureData();
        Arrays.fill(texels, 0xff888888);
        // Only the side faces carry markers. A wrist end-cap alone cannot pass the check.
        for (int y = 20; y < 32; y++) {
            int band = y < 25 ? 0xff00ff00 : y < 28 ? 0xffff0000 : 0xffff00ff;
            Arrays.fill(texels, y * 64 + 40, y * 64 + 56, band);
        }
        try {
            markers.updateDynamicTexture();
            textures.loadTexture(skin, markers);
            checkArmPose(mc, null, "empty");
            for (Item item : List
                .of(Items.iron_ingot, Items.iron_axe, Items.iron_pickaxe, Items.iron_shovel, Items.iron_hoe)) {
                equip(mc, new ItemStack(item));
                tickPlayer();
                String id = item.getUnlocalizedName();
                checkArmMarkers(mc, draw(mc, id + "-carry-markers"), null, id);
                if (item == Items.iron_pickaxe) {
                    float forward = mc.thePlayer.moveForward;
                    try {
                        mc.thePlayer.moveForward = 1;
                        tickPlayer();
                        checkArmMarkers(mc, draw(mc, "pickaxe-walk-markers"), null, "walking pickaxe");
                    } finally {
                        mc.thePlayer.moveForward = forward;
                        tickPlayer();
                    }
                }
            }
            checkArmPose(mc, sword, "sword");
        } finally {
            textures.loadTexture(skin, original);
            markers.deleteGlTexture();
        }
    }

    private void checkArmPose(Minecraft mc, ItemStack held, String pose) throws Exception {
        equip(mc, held);
        tickPlayer();
        require(!target.dualHanded(), pose + " example uses primary hand");
        int[] right = draw(mc, pose + "-markers-right");
        checkArmMarkers(mc, right, null, pose);
        try {
            reload(mc, "animation-qa-dual");
            tickPlayer();
            require(target.dualHanded(), pose + " pack explicitly requested support arm");
            int[] both = draw(mc, pose + "-markers-both");
            checkArmMarkers(mc, both, right, pose);
            target.trigger("attack");
            mc.thePlayer.swingItem();
            advanceHands(mc, 3);
            require(
                !target.replacesSwing(held) && !Arrays.equals(both, draw(mc, pose + "-vanilla-swing")),
                pose + " unmarked clip keeps vanilla swing");
            advanceHands(mc, 6);
        } finally {
            reload(mc, "animation-qa");
            tickPlayer();
        }
    }

    private void checkArmMarkers(Minecraft mc, int[] pixels, int[] withoutLeft, String pose) {
        boolean left = withoutLeft != null;
        long sleeveX = 0, wristX = 0, sleeveY = 0, wristY = 0;
        int sleeve = 0, wrist = 0, forearm = 0, shaftContact = 0, top = mc.displayHeight;
        boolean lowGrip = !left && !pose.equals("empty");
        for (int i = 0; i < pixels.length; i++) {
            int rgb = pixels[i];
            if (withoutLeft != null && rgb == withoutLeft[i]) continue;
            int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
            if (g > 40 && g > 2 * r && g > 2 * b) {
                sleeveX += i % mc.displayWidth;
                sleeveY += i / mc.displayWidth;
                sleeve++;
            } else if (r > 40 && r > 2 * g && r > 2 * b) {
                forearm++;
            } else if (r > 40 && b > 40 && r > 2 * g && b > 2 * g) {
                top = Math.min(top, i / mc.displayWidth);
                wristX += i % mc.displayWidth;
                wristY += i / mc.displayWidth;
                wrist++;
                // The vanilla iron handles and the diamond sword's hilt use brown texels.
                if (i >= mc.displayWidth && isHandlePixel(pixels[i - mc.displayWidth])
                    || i % mc.displayWidth < mc.displayWidth - 1 && isHandlePixel(pixels[i + 1])) shaftContact++;
            }
        }
        int area = pixels.length / 600;
        if (lowGrip) {
            int x = (int) (wristX / Math.max(1, wrist)), y = (int) (wristY / Math.max(1, wrist));
            int h = mc.displayHeight;
            require(
                wrist >= area && forearm >= area / 2
                    && top >= .78 * h
                    && x > mc.displayWidth / 2 + .37 * h
                    && y > .85 * h
                    && y < .97 * h,
                pose + " keeps the wrist low with a visible forearm: "
                    + x
                    + ", "
                    + y
                    + "; top "
                    + top
                    + ", forearm "
                    + forearm);
            if (!pose.equals(Items.iron_ingot.getUnlocalizedName())) require(
                shaftContact >= mc.displayHeight / 48,
                pose + " handle meets the fist along " + shaftContact + " pixels");
            return;
        }
        double span = (sleeveX / (double) sleeve - wristX / (double) wrist) * (left ? -1 : 1);
        double drop = sleeveY / (double) sleeve - wristY / (double) wrist;
        boolean separated = left ? span >= mc.displayHeight * .14
            : span > mc.displayHeight * .05 && drop > mc.displayHeight * .08
                && Math.hypot(span, drop) >= mc.displayHeight * .14;
        require(
            sleeve >= area && wrist >= area && separated,
            pose + (left ? " left" : " right")
                + " sleeve and wrist visible, separation "
                + Math.round(span)
                + " x "
                + Math.round(drop)
                + "px");
    }

    private static boolean isHandlePixel(int rgb) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        return r > 10 && r > 1.2 * g && g > b + 6 && b > 0;
    }

    private void checkRepositoryReload(Minecraft mc, ItemStack sword) {
        var repo = mc.getResourcePackRepository();
        var before = repo.getRepositoryEntries();
        var selected = new ArrayList<>(before);
        repo.updateRepositoryEntriesAll();
        for (var pack : repo.getRepositoryEntriesAll()) if (pack.getResourcePackName()
            .equals("animation-qa")) selected.add(pack);
        try {
            repo.func_148527_a(selected);
            mc.refreshResources();
            tickPlayer();
            require(target.applies(sword), "game resource-manager reload activated the selected folder");
            float yaw = mc.thePlayer.rotationYaw;
            try {
                mc.thePlayer.rotationYaw = yaw + 10;
                tickPlayer();
                require(Math.abs(target.yaw(.5f)) > 1, "movement inertia follows view rotation");
            } finally {
                mc.thePlayer.rotationYaw = yaw;
            }
        } finally {
            repo.func_148527_a(before);
            mc.refreshResources();
        }
        require(!target.enabled(), "deselecting the folder disabled the adapter");
    }

    private void checkHost(Minecraft mc) throws Exception {
        Set<String> wanted = new HashSet<>(
            List.of("MetaGeneratedToolRenderer", "ItemWandRenderer", "ToolBiometricCardRender"));
        for (Object value : Item.itemRegistry) {
            Item item = (Item) value;
            String id = String.valueOf(Item.itemRegistry.getNameForObject(item))
                .toLowerCase(Locale.ROOT);
            if (!id.contains("metatool") && !id.contains("wandcasting") && !id.contains("biometric")) continue;
            ItemStack stack = new ItemStack(item);
            var renderer = MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.EQUIPPED_FIRST_PERSON);
            if (renderer == null || !wanted.remove(
                renderer.getClass()
                    .getSimpleName()))
                continue;
            equip(mc, stack);
            tickPlayer();
            require(!target.applies(stack), "delegated " + id);
            draw(
                mc,
                renderer.getClass()
                    .getSimpleName());
        }
        require(wanted.isEmpty(), "host renderers found; missing " + wanted);
    }

    private void require(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        results.add(label);
    }

    private void tickPlayer() {
        for (int i = 0; i < 4; i++) {
            target.tick(new TickEvent.ClientTickEvent(TickEvent.Phase.START));
            target.tick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        }
    }

    private void reload(Minecraft mc, String name) {
        GTNRLibConfig.animationPack = name;
        target.onResourceManagerReload(mc.getResourceManager());
    }

    private void equip(Minecraft mc, ItemStack stack) {
        mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem] = stack;
        for (int i = 0; i < 8; i++) mc.entityRenderer.itemRenderer.updateEquippedItem();
    }

    private int[] draw(Minecraft mc, String name) throws Exception {
        return draw(mc, name, .5f);
    }

    private int[] draw(Minecraft mc, String name, float partial) throws Exception {
        int w = mc.displayWidth, h = mc.displayHeight;
        Framebuffer buffer = mc.getFramebuffer();
        buffer.bindFramebuffer(true);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        double aspect = w / (double) h;
        // EntityRenderer.renderHand uses 70 degrees on land, independently of the world FOV.
        double halfHeight = .05 * Math.tan(Math.toRadians(35));
        GL11.glFrustum(-aspect * halfHeight, aspect * halfHeight, -halfHeight, halfHeight, .05, 32);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        try {
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
            GL11.glClearColor(.125f, .125f, .125f, 1);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            mc.entityRenderer.itemRenderer.renderItemInFirstPerson(partial);
            ScreenShotHelper.saveScreenshot(output, name + ".png", w, h, buffer);
            var image = ImageIO.read(new File(output, "screenshots/" + name + ".png"));
            return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private void finish(Minecraft mc, Throwable error) {
        finished = true;
        if (error != null) error.printStackTrace();
        try {
            Files.writeString(
                new File(output, "result.txt").toPath(),
                (error == null ? "PASS" : "FAIL " + error) + "\n" + String.join("\n", results));
        } catch (Exception e) {
            e.printStackTrace();
        }
        mc.shutdown();
    }
}
