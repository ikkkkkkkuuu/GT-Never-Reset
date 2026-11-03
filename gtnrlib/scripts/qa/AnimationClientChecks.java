package com.xyp.gtnrlib.qa;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
            var stone = new ItemStack(Blocks.cobblestone);
            equip(mc, stone);
            tickPlayer();
            int[] carry = draw(mc, "block-carry");
            int placeX = x + 2, ground = mc.theWorld.getTopSolidOrLiquidBlock(placeX, z) - 1;
            placeOnTop(mc, placeX, ground, z);
            require(
                mc.theWorld.getBlock(placeX, ground + 1, z) == Blocks.cobblestone && mc.thePlayer.isSwingInProgress
                    && observed.contains("useitem"),
                "normal right click placed cobblestone");
            advanceHands(mc, 2);
            var rest = grayItem(carry, mc.displayWidth);
            var extended = grayItem(draw(mc, "block-extended", 0), mc.displayWidth);
            int[] reach = draw(mc, "block-place");
            int h = mc.displayHeight, center = mc.displayWidth / 2;
            // Measure from the optical center in viewport heights so the check works in 4:3 and widescreen.
            require(
                !target.dualHanded() && rest.x > center + .46 * h
                    && rest.y > .75 * h
                    && extended.x > center + .15 * h
                    && extended.x < center + .32 * h
                    && extended.y > .58 * h
                    && extended.y < .77 * h
                    && extended.width < .78 * rest.width,
                "block travels inward and recedes from " + rest + " to " + extended);
            advanceHands(mc, 2);
            require(
                Arrays.equals(carry, draw(mc, "block-recovered")),
                "block at carry pose by the next vanilla right click");
            int nextGround = mc.theWorld.getTopSolidOrLiquidBlock(placeX, z + 1) - 1;
            placeOnTop(mc, placeX, nextGround, z + 1);
            require(
                mc.theWorld.getBlock(placeX, nextGround + 1, z + 1) == Blocks.cobblestone,
                "second right click placed cobblestone");
            advanceHands(mc, 2);
            require(Arrays.equals(reach, draw(mc, "block-repeat")), "repeated placement follows the same reach");
            advanceHands(mc, 2);
            require(
                Arrays.equals(carry, draw(mc, "block-repeat-recovered")),
                "repeated placement returns without recoil");
            advanceHands(mc, 4);
            mc.playerController.clickBlock(placeX, ground + 1, z, 1);
            mc.playerController.clickBlock(placeX, nextGround + 1, z + 1, 1);
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
        var swing = EntityLivingBase.class.getDeclaredMethod("updateArmSwingProgress");
        swing.setAccessible(true);
        for (int i = 0; i < ticks; i++) {
            mc.thePlayer.prevSwingProgress = mc.thePlayer.swingProgress;
            swing.invoke(mc.thePlayer);
            mc.entityRenderer.itemRenderer.updateEquippedItem();
            target.tick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        }
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
        int[] relaxed = draw(mc, "empty-relaxed");
        target.trigger("attack");
        advanceHands(mc, 2);
        require(!Arrays.equals(relaxed, draw(mc, "empty-punch")), "empty punch extends from its idle pose");
        advanceHands(mc, 6);
        require(Arrays.equals(relaxed, draw(mc, "empty-return")), "empty punch returns to its idle pose");
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
            if (y < 25 || y >= 28) Arrays.fill(texels, y * 64 + 40, y * 64 + 56, y < 25 ? 0xff00ff00 : 0xffff00ff);
        }
        try {
            markers.updateDynamicTexture();
            textures.loadTexture(skin, markers);
            checkArmPose(mc, null, "empty");
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
        checkArmSpan(mc, right, null, false, pose);
        try {
            reload(mc, "animation-qa-dual");
            tickPlayer();
            require(target.dualHanded(), pose + " pack explicitly requested support arm");
            int[] both = draw(mc, pose + "-markers-both");
            checkArmSpan(mc, both, right, true, pose);
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

    private void checkArmSpan(Minecraft mc, int[] pixels, int[] withoutLeft, boolean left, String pose) {
        long sleeveX = 0, wristX = 0;
        int sleeve = 0, wrist = 0;
        for (int i = 0; i < pixels.length; i++) {
            int rgb = pixels[i];
            if (withoutLeft != null && rgb == withoutLeft[i]) continue;
            int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
            if (g > 40 && g > 2 * r && g > 2 * b) {
                sleeveX += i % mc.displayWidth;
                sleeve++;
            } else if (r > 40 && b > 40 && r > 2 * g && b > 2 * g) {
                wristX += i % mc.displayWidth;
                wrist++;
            }
        }
        int area = pixels.length / 600;
        double span = (sleeveX / (double) sleeve - wristX / (double) wrist) * (left ? -1 : 1);
        require(
            sleeve >= area && wrist >= area && span >= mc.displayHeight * .14,
            pose + (left ? " left" : " right")
                + " sleeve and wrist visible, horizontal span "
                + Math.round(span)
                + "px");
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
        for (int i = 0; i < 4; i++) target.tick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
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
