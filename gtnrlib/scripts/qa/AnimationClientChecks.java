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
            || ++frames < 20) return;
        try {
            var sword = new ItemStack(Items.diamond_sword);
            reload(mc, "");
            equip(mc, sword);
            require(!target.enabled(), "library alone is inactive");
            int[] baseline = draw(mc, "01-vanilla");
            require(
                Arrays.stream(baseline)
                    .distinct()
                    .count() > 8,
                "nonempty vanilla silhouette");
            reload(mc, "animation-qa");
            tickPlayer();
            require(target.applies(sword) && target.dualHanded(), "selected pack bound sword");
            int[] active = draw(mc, "02-grip");
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
            if (!Boolean.getBoolean("gtnrlib.animation.qa.host")) checkRepositoryReload(mc, sword);
            finish(mc, null);
        } catch (Throwable e) {
            finish(mc, e);
        }
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
        GL11.glFrustum(-aspect * .05, aspect * .05, -.05, .05, .05, 32);
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
