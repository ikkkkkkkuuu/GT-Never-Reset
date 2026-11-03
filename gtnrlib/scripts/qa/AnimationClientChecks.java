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
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.api.animation.AnimationClip;
import com.xyp.gtnrlib.client.animation.FirstPersonAnimations;
import com.xyp.gtnrlib.compat.punchy.PunchyPack;
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
            require(target.applies(sword) && target.dualHanded(), "selected pack bound sword");
            int[] active = draw(mc, "02-grip");
            equip(mc, null);
            tickPlayer();
            draw(mc, "empty-grip");
            checkArmVisibility(mc, sword);
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
            tickPlayer();
            require(observed.contains("attack"), "accepted left click dispatched attack");
            require(!Arrays.equals(active, draw(mc, "03-swing")), "attack changes grip");
            int x = (int) mc.thePlayer.posX, y = (int) mc.thePlayer.posY + 3, z = (int) mc.thePlayer.posZ;
            mc.objectMouseOver = new net.minecraft.util.MovingObjectPosition(
                x,
                y,
                z,
                1,
                net.minecraft.util.Vec3.createVectorHelper(x, y, z));
            var attackKey = mc.gameSettings.keyBindAttack;
            net.minecraft.client.settings.KeyBinding.setKeyBindState(attackKey.getKeyCode(), true);
            try {
                tickPlayer();
                require(observed.contains("mining"), "held attack on block dispatched mining");
            } finally {
                net.minecraft.client.settings.KeyBinding.setKeyBindState(attackKey.getKeyCode(), false);
                mc.objectMouseOver = previousHit;
            }
            var stone = new ItemStack(net.minecraft.init.Blocks.stone);
            equip(mc, stone);
            tickPlayer();
            require(
                ((ItemBlock) stone.getItem())
                    .placeBlockAt(stone, mc.thePlayer, mc.theWorld, x, y, z, 1, .5f, .5f, .5f, 0)
                    && observed.contains("useitem"),
                "successful local placement dispatched useItem");
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

    private void checkArmVisibility(Minecraft mc, ItemStack sword) throws Exception {
        var dir = mc.getResourcePackRepository()
            .getDirResourcepacks()
            .toPath()
            .resolve("animation-qa");
        AnimationClip grip = PunchyPack.load(List.of(dir), message -> {})
            .clip("gtnrlib_grip")
            .clip();
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
            checkArmPose(mc, null, grip, "empty");
            checkArmPose(mc, sword, grip, "sword");
        } finally {
            textures.loadTexture(skin, original);
            markers.deleteGlTexture();
        }
    }

    private void checkArmPose(Minecraft mc, ItemStack held, AnimationClip grip, String pose) throws Exception {
        equip(mc, held);
        tickPlayer();
        int[] both = draw(mc, pose + "-markers-both");
        target.bind("fixture:single", (stack, action) -> "idle".equals(action) ? grip : null);
        try {
            tickPlayer();
            require(!target.dualHanded(), pose + " single-arm override active");
            int[] right = draw(mc, pose + "-markers-right");
            checkArmSpan(mc, right, null, false, pose);
            checkArmSpan(mc, both, right, true, pose);
        } finally {
            target.unbind("fixture:single");
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
            mc.entityRenderer.itemRenderer.renderItemInFirstPerson(.5f);
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
