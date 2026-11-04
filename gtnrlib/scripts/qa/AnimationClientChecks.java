package com.xyp.gtnrlib.qa;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import net.minecraftforge.oredict.OreDictionary;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.xyp.gtnrlib.api.animation.AnimationClip.Marker;
import com.xyp.gtnrlib.client.animation.FirstPersonAnimations;
import com.xyp.gtnrlib.client.animation.HmiFirstPerson;
import com.xyp.gtnrlib.client.animation.HmiItemBindings;
import com.xyp.gtnrlib.client.animation.HmiItemBindings.Profile;
import com.xyp.gtnrlib.client.render.animation.AnimationModelChecks;
import com.xyp.gtnrlib.compat.hmi.HmiPoseProgram;
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
    private Runnable wrenchChecks;
    private AnimationMovementChecks movement;
    private final FirstPersonAnimations target = FirstPersonAnimations.INSTANCE;
    private final List<String> results = new ArrayList<>();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtnrlib.animation.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @Mod.EventHandler
    public void prepareWorld(cpw.mods.fml.common.event.FMLServerStartedEvent event) {
        var world = net.minecraft.server.MinecraftServer.getServer()
            .worldServerForDimension(0);
        world.setWorldTime(6000);
        world.getGameRules()
            .setOrCreateGameRule("doDaylightCycle", "false");
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
                    java.util.Objects.requireNonNull(System.getProperty("gtnrlib.animation.qa.world")),
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
            if (wrenchChecks != null) {
                wrenchChecks.run();
                return;
            }
            if (movement != null) {
                if (movement.step()) {
                    movement = null;
                    if (Boolean.getBoolean("gtnrlib.animation.qa.toolMods")) startWrenchChecks();
                    else finish(mc, null);
                }
                return;
            }
            if (Boolean.getBoolean("gtnrlib.animation.qa.defaults")) {
                boolean host = cpw.mods.fml.common.Loader.isModLoaded("gtneverreset");
                boolean gt = cpw.mods.fml.common.Loader.isModLoaded("gregtech");
                require(
                    host == Boolean.getBoolean("gtnrlib.animation.qa.expectHost")
                        && gt == Boolean.getBoolean("gtnrlib.animation.qa.toolMods"),
                    "loaded GTNRLib with GT-Never-Reset=" + host + ", GregTech=" + gt);
                if (!host) require(
                    getClass().getClassLoader()
                        .getResource("com/xyp/gtneverreset/GTNeverReset.class") == null,
                    "GT-Never-Reset classes absent from the client classpath");
                require(
                    mc.getResourcePackRepository()
                        .getRepositoryEntries()
                        .isEmpty() && GTNRLibConfig.animationPack.isEmpty(),
                    "fresh launch with empty selected and imported resource packs");
                require(
                    HmiFirstPerson.INSTANCE.enabled() && HmiFirstPerson.INSTANCE.program()
                        .builtIn(),
                    "built-in player active on the first ordinary world frame");
                AnimationBlockUseChecks.verify(mc);
                checkHmi(mc);
                movement = new AnimationMovementChecks(this, output);
                return;
            }
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
            if (Boolean.getBoolean("gtnrlib.animation.qa.toolMods")) checkModRenderers(mc);
            equip(mc, sword);
            reload(mc, "");
            tickPlayer();
            require(Arrays.equals(baseline, draw(mc, "04-disabled")), "unload restores original pixels");
            equip(mc, null);
            require(Arrays.equals(emptyBaseline, draw(mc, "empty-disabled")), "unload restores vanilla empty arm");
            equip(mc, sword);
            if (!Boolean.getBoolean("gtnrlib.animation.qa.toolMods")) checkRepositoryReload(mc, sword);
            String release = System.getProperty("gtnrlib.animation.qa.hmi");
            if (release != null) {
                checkHmi(mc);
                if (Boolean.getBoolean("gtnrlib.animation.qa.toolMods")) {
                    reload(mc, release);
                    startWrenchChecks();
                    return;
                }
            }
            finish(mc, null);
        } catch (Throwable e) {
            finish(mc, e);
        }
    }

    private void startWrenchChecks() throws Exception {
        wrenchChecks = (Runnable) Class.forName("com.xyp.gtnrlib.qa.AnimationWrenchChecks")
            .getConstructor(AnimationClientChecks.class)
            .newInstance(this);
    }

    private void checkHmi(Minecraft mc) throws Exception {
        String release = System.getProperty("gtnrlib.animation.qa.hmi");
        var hmi = com.xyp.gtnrlib.client.animation.HmiFirstPerson.INSTANCE;
        if (release != null) reload(mc, release);
        require(hmi.enabled(), "first-person pose program enabled");
        int ticks = mc.thePlayer.ticksExisted;
        try {
            var items = new ItemStack[] { null, new ItemStack(Items.diamond_sword), new ItemStack(Items.iron_pickaxe),
                new ItemStack(Items.iron_axe), new ItemStack(Items.iron_shovel), new ItemStack(Items.iron_hoe),
                new ItemStack(Blocks.stone), new ItemStack(Items.iron_ingot), new ItemStack(Items.snowball),
                new ItemStack(Items.apple), new ItemStack(Items.potionitem), new ItemStack(Items.bow),
                new ItemStack(Items.fishing_rod), new ItemStack(Items.filled_map), new ItemStack(Items.water_bucket),
                new ItemStack(Items.bucket), new ItemStack(Items.lava_bucket), new ItemStack(Items.milk_bucket),
                new ItemStack(Items.boat) };
            for (ItemStack stack : items) {
                equip(mc, stack);
                String name = stack == null ? "empty"
                    : stack.getItem()
                        .getUnlocalizedName()
                        .replace('.', '-');
                for (int tick = 0; tick < 48; tick++) {
                    mc.thePlayer.ticksExisted++;
                    mc.entityRenderer.itemRenderer.renderItemInFirstPerson(.5f);
                }
                int[] rest = draw(mc, "hmi-" + name);
                if (AnimationGripChecks.covers(stack)) AnimationGripChecks.measure(this, mc, stack, name, true);
                require(
                    hmi.enabled() && !hmi.right.equals(new org.joml.Matrix4f())
                        && Arrays.stream(rest)
                            .filter(p -> (p & 0xffffff) != 0x202020)
                            .count() > 500,
                    "HMI rendered " + name);
                var before = new org.joml.Matrix4f(hmi.right);
                mc.thePlayer.isSwingInProgress = false;
                mc.thePlayer.swingItem();
                if (stack != null && stack.getItemUseAction() != net.minecraft.item.EnumAction.none)
                    mc.thePlayer.setItemInUse(stack, stack.getMaxItemUseDuration());
                for (int tick = 0; tick < 5; tick++) {
                    mc.thePlayer.ticksExisted++;
                    mc.entityRenderer.itemRenderer.renderItemInFirstPerson(.5f);
                }
                require(
                    hmi.enabled() && !before.equals(hmi.right, .001f)
                        && !Arrays.equals(rest, draw(mc, "hmi-action-" + name)),
                    "HMI action for " + name);
                if (stack != null && stack.getItem() == Items.bow) {
                    var support = new org.joml.Matrix4f(hmi.left);
                    for (int tick = 0; tick < 24; tick++) {
                        mc.thePlayer.ticksExisted++;
                        mc.entityRenderer.itemRenderer.renderItemInFirstPerson(.5f);
                    }
                    require(!support.equals(hmi.left, .01f), "drawn bow moves the support arm");
                    require(
                        hmi.program()
                            .bowStage() == 2,
                        "upstream use duration selects the fully drawn bow sprite");
                    draw(mc, "hmi-bow-fully-drawn");
                }
                if (stack != null && stack.getItem() == Items.milk_bucket) {
                    mc.thePlayer.setItemInUse(stack, stack.getMaxItemUseDuration() - 28);
                    require(
                        !Arrays.equals(rest, draw(mc, "hmi-milk-drinking")),
                        "milk model dispatches elapsed use duration");
                }
                mc.thePlayer.clearItemInUse();
            }
            checkHmiProfiles(mc, release);
            if (Boolean.getBoolean("gtnrlib.animation.qa.toolMods")) {
                checkEtFuturum(mc);
                checkModRenderers(mc);
            }
            if (release != null) {
                reload(mc, "invalid-hmi-fixture.jar");
                require(hmi.enabled(), "failed HMI reload retains previous program");
            } else checkDefaultMotion(mc);
        } finally {
            mc.thePlayer.ticksExisted = ticks;
            mc.thePlayer.clearItemInUse();
            if (release != null) reload(mc, "");
        }
        if (release != null) require(!hmi.enabled(), "HMI unload releases runtime");
    }

    private void checkHmiProfiles(Minecraft mc, String release) throws Exception {
        var hmi = HmiFirstPerson.INSTANCE;
        var stack = new ItemStack(Items.stick);
        stack.stackTagCompound = new net.minecraft.nbt.NBTTagCompound();
        equip(mc, stack);
        HmiItemBindings.register(
            Items.stick,
            s -> new Profile(s.stackTagCompound.getString("hmi"), s.stackTagCompound.getString("variant")));
        try {
            var models = com.xyp.gtnrlib.client.render.animation.DefaultItemModels.entries();
            if (release != null) {
                var reference = new java.util.TreeSet<String>();
                for (String path : HmiPoseProgram.Scripts.read(
                    mc.getResourcePackRepository()
                        .getDirResourcepacks()
                        .toPath()
                        .resolve(release))
                    .assets()
                    .keySet()) {
                    if (path.startsWith("items/") && path.endsWith(".json"))
                        reference.add("minecraft:" + path.substring(6, path.length() - 5));
                }
                require(reference.equals(models), "complete model inventory agrees with pinned reference archive");
            }
            require(models.size() == 35, "35 distinct first-person model profiles");
            var isolated = new HashMap<String, Integer>();
            for (String id : new java.util.TreeSet<>(models)) {
                String name = id.substring(10);
                Item match = (Item) Item.itemRegistry.getObject(id);
                if (match == null && name.equals("oak_boat")) match = Items.boat;
                if (match == null) match = (Item) Item.itemRegistry.getObject("etfuturum:" + name);
                stack.stackTagCompound.setString("hmi", id);
                var held = match == null ? stack : new ItemStack(match);
                equip(mc, held);
                for (int i = 0; i < 30; i++) {
                    mc.thePlayer.ticksExisted++;
                    mc.entityRenderer.itemRenderer.renderItemInFirstPerson(.5f);
                }
                int[] rest = draw(mc, "model-" + name);
                if (release == null) {
                    int[] mesh = draw(mc, "mesh-" + name, .5f, AnimationModelChecks.draw(id, "", -1, 0, hmi.program()));
                    require(
                        Arrays.stream(mesh)
                            .filter(rgb -> (rgb & 0xffffff) != 0x202020)
                            .count() > 30,
                        "isolated geometry visible " + name);
                    isolated.put(name, Arrays.hashCode(mesh));
                    if (Set
                        .of(
                            "cod_bucket",
                            "salmon_bucket",
                            "axolotl_bucket",
                            "pufferfish_bucket",
                            "tropical_fish_bucket",
                            "tadpole_bucket",
                            "end_crystal",
                            "milk_bucket")
                        .contains(name)) {
                        var moving = draw(
                            mc,
                            "mesh-" + name + "-moving",
                            .5f,
                            AnimationModelChecks.draw(id, "", 28, 7, hmi.program()));
                        require(!Arrays.equals(mesh, moving), "isolated moving parts " + name);
                    }
                }
                var right = new org.joml.Matrix4f(hmi.right);
                mc.thePlayer.isSwingInProgress = false;
                mc.thePlayer.swingItem();
                mc.thePlayer.ticksExisted += 3;
                int[] action = draw(mc, "model-" + name + "-action");
                require(
                    hmi.enabled() && id.equals(hmi.itemId())
                        && !right.equals(hmi.right, .0001f)
                        && !Arrays.equals(rest, action),
                    "MODEL " + name
                        + " | "
                        + (match == null ? "profile fixture (item absent)" : Item.itemRegistry.getNameForObject(match))
                        + " | grip + swing");
            }
            if (release == null) {
                for (String model : Set
                    .of("oak", "birch", "spruce", "acacia", "jungle", "dark_oak", "cherry", "mangrove", "pale_oak")) {
                    require(
                        !isolated.get(model + "_boat")
                            .equals(isolated.get(model + "_chest_boat")),
                        "cargo geometry " + model);
                }
                require(
                    !isolated.get("bamboo_raft")
                        .equals(isolated.get("bamboo_chest_raft")),
                    "cargo geometry bamboo");
            }
            equip(mc, stack);
            stack.stackTagCompound.setString("hmi", "minecraft:axolotl_bucket");
            for (int i = 0; i < 45; i++) {
                mc.thePlayer.ticksExisted++;
                mc.entityRenderer.itemRenderer.renderItemInFirstPerson(.5f);
            }
            int[] fallback = draw(mc, "hmi-axolotl-fallback");
            stack.stackTagCompound.setString("variant", "lucy");
            require(
                Arrays.equals(fallback, draw(mc, "hmi-axolotl-lucy")),
                "component fallback matches original lucy model");
            for (String color : List.of("wild", "gold", "cyan", "blue")) {
                stack.stackTagCompound.setString("variant", color);
                require(
                    !Arrays.equals(fallback, draw(mc, "hmi-axolotl-" + color)),
                    "same-stack component selects " + color);
            }
            stack.stackTagCompound.setString("hmi", "minecraft:pink_petals");
            mc.thePlayer.ticksExisted++;
            mc.entityRenderer.itemRenderer.renderItemInFirstPerson(.5f);
            mc.thePlayer.isSwingInProgress = false;
            mc.thePlayer.swingItem();
            mc.thePlayer.ticksExisted++;
            draw(mc, "hmi-petal-swing");
            var program = hmi.program();
            int spawned = program.screenParticleCount();
            require(spawned > 0, "upstream swing emitted screen particles");
            double y = program.screenParticle(0)
                .y();
            mc.entityRenderer.itemRenderer.renderItemInFirstPerson(.5f);
            require(
                spawned == program.screenParticleCount() && y == program.screenParticle(0)
                    .y(),
                "second draw does not emit or advance particles");
            for (int i = 0; i < 3; i++) {
                mc.thePlayer.ticksExisted++;
                mc.entityRenderer.itemRenderer.renderItemInFirstPerson(.5f);
            }
            require(
                y != program.screenParticle(0)
                    .y(),
                "upstream screen particles travel after emission");
            for (int i = 0; i < 48; i++) {
                mc.thePlayer.ticksExisted++;
                mc.entityRenderer.itemRenderer.renderItemInFirstPerson(.5f);
            }
            require(program.screenParticleCount() == 0, "screen particle lifetime and fade completed");
        } finally {
            HmiItemBindings.unregister(Items.stick);
        }
        mc.entityRenderer.itemRenderer.renderItemInFirstPerson(.5f);
        require(
            "minecraft:stick".equals(hmi.itemId()),
            "removing binding restores original stack without re-equipping");
        HmiItemBindings.register(Items.stick, broken -> { throw new IllegalStateException("QA resolver failure"); });
        try {
            mc.entityRenderer.itemRenderer.renderItemInFirstPerson(.5f);
            require(
                hmi.enabled() && "minecraft:stick".equals(hmi.itemId()),
                "failing third-party resolver falls back without disabling HMI");
        } finally {
            HmiItemBindings.unregister(Items.stick);
        }
    }

    private void checkDefaultMotion(Minecraft mc) throws Exception {
        var hmi = HmiFirstPerson.INSTANCE;
        var p = mc.thePlayer;
        int ticks = p.ticksExisted;
        float oldYaw = p.rotationYaw, oldPitch = p.rotationPitch, prevYaw = p.prevRotationYaw,
            prevPitch = p.prevRotationPitch;
        double oldX = p.motionX, oldY = p.motionY, oldZ = p.motionZ;
        boolean ground = p.onGround;
        var originalTarget = mc.objectMouseOver;
        try {
            var sequences = new HashMap<String, Matrix4f[]>();
            for (String scenario : List.of(
                "rest",
                "walk",
                "turn",
                "jump-twice",
                "standing-strike",
                "walking-strike",
                "walking-mine",
                "walking-punch",
                "rest-bow",
                "standing-bow",
                "walking-bow")) {
                var samples = new Matrix4f[40];
                sequences.put(scenario, samples);
                hmi.detach();
                var held = scenario.endsWith("punch") ? null
                    : new ItemStack(
                        scenario.endsWith("bow") ? Items.bow
                            : scenario.endsWith("mine") ? Items.iron_pickaxe : Items.iron_sword);
                mc.objectMouseOver = scenario.endsWith("mine")
                    ? new net.minecraft.util.MovingObjectPosition(
                        0,
                        0,
                        0,
                        1,
                        net.minecraft.util.Vec3.createVectorHelper(0, 0, 0))
                    : null;
                equip(mc, held);
                p.isSwingInProgress = false;
                p.clearItemInUse();
                p.motionX = p.motionY = p.motionZ = 0;
                p.rotationYaw = p.prevRotationYaw = p.rotationPitch = p.prevRotationPitch = 0;
                p.onGround = true;
                for (int i = 0; i < 55; i++) {
                    p.ticksExisted++;
                    hmi.sample(held, .5f, 0);
                }
                var start = new org.joml.Matrix4f(hmi.item);
                for (int i = 0; i < 40; i++) {
                    if (scenario.startsWith("walk")) p.motionZ = .22;
                    if (scenario.equals("turn")) {
                        p.prevRotationYaw = p.rotationYaw;
                        p.prevRotationPitch = p.rotationPitch;
                        if (i < 7) {
                            p.rotationYaw += 6;
                            p.rotationPitch -= 2;
                        }
                    }
                    if (scenario.equals("jump-twice")) {
                        int t = i % 17;
                        if (t == 0) p.jump();
                        p.onGround = t >= 11;
                        if (t > 0) p.motionY = p.onGround ? 0 : (p.motionY - .08) * .98;
                    }
                    if (i == 9
                        && (scenario.endsWith("strike") || scenario.endsWith("punch") || scenario.endsWith("mine")))
                        p.swingItem();
                    if (i == 7 && scenario.endsWith("bow") && !scenario.equals("rest-bow"))
                        p.setItemInUse(held, held.getMaxItemUseDuration());
                    if (i == 32) p.clearItemInUse();
                    p.ticksExisted++;
                    if (!hmi.sample(held, .5f, 0)) throw new AssertionError("failed sample " + scenario + " " + i);
                    samples[i] = new Matrix4f(hmi.item);
                    if (i == 13 && (scenario.equals("walking-strike") || scenario.equals("walking-mine")
                        || scenario.equals("walking-bow")
                        || scenario.equals("jump-twice"))) AnimationGripChecks.measure(this, mc, held, scenario, false);
                    var main = new Matrix4f(hmi.right);
                    var support = new Matrix4f(hmi.left);
                    if (!hmi.sample(held, .5f, 0) || !samples[i].equals(hmi.item)
                        || !main.equals(hmi.right)
                        || !support.equals(hmi.left)) throw new AssertionError("duplicate pass advanced " + scenario);
                    if (scenario.startsWith("walking-") || i == 0
                        || i == 5
                        || i == 10
                        || i == 13
                        || i == 17
                        || i == 27
                        || i == 31
                        || i == 36) draw(mc, "motion-" + scenario + "-" + i);
                }
                require(!start.equals(hmi.item, .0001f), "motion and duplicate-pass regression " + scenario);
            }
            for (String scenario : List.of("walk", "turn", "jump-twice")) {
                for (int i : new int[] { 2, 8, 11, 19, 25, 29 }) require(
                    !sequences.get(scenario)[i].equals(sequences.get("rest")[i], .0001f),
                    scenario + " differs from idle at the same frame " + i);
            }
            for (int i : new int[] { 11, 13 }) {
                require(
                    !sequences.get("walking-strike")[i].equals(sequences.get("standing-strike")[i], .001f)
                        && !sequences.get("walking-strike")[i].equals(sequences.get("walk")[i], .001f),
                    "sword contact preserves both gait and strike at " + i);
            }
            for (int i : new int[] { 16, 24, 30 }) {
                require(
                    !sequences.get("walking-bow")[i].equals(sequences.get("standing-bow")[i], .001f)
                        && !sequences.get("standing-bow")[i].equals(sequences.get("rest-bow")[i], .001f),
                    "drawn bow preserves both gait and draw at " + i);
            }
        } finally {
            p.ticksExisted = ticks;
            mc.objectMouseOver = originalTarget;
            p.rotationYaw = oldYaw;
            p.prevRotationYaw = prevYaw;
            p.rotationPitch = oldPitch;
            p.prevRotationPitch = prevPitch;
            p.motionX = oldX;
            p.motionY = oldY;
            p.motionZ = oldZ;
            p.onGround = ground;
            p.clearItemInUse();
            hmi.detach();
        }
    }

    private void checkEtFuturum(Minecraft mc) throws Exception {
        var hmi = HmiFirstPerson.INSTANCE;
        int rendered = 0;
        for (String path : List.of(
            "spruce_boat",
            "oak_chest_boat",
            "bamboo_chest_raft",
            "lantern",
            "soul_lantern",
            "slime",
            "honey_block",
            "end_crystal")) {
            Item item = (Item) Item.itemRegistry.getObject("etfuturum:" + path);
            if (item == null) continue;
            var stack = new ItemStack(item);
            equip(mc, stack);
            if (MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.EQUIPPED_FIRST_PERSON) != null
                && !com.xyp.gtnrlib.client.render.animation.DefaultItemModels.contains("minecraft:" + path)) {
                require(!hmi.sample(stack, .5f, 0), "delegated Et Futurum renderer " + path);
                continue;
            }
            for (int tick = 0; tick < 35; tick++) {
                mc.thePlayer.ticksExisted++;
                mc.entityRenderer.itemRenderer.renderItemInFirstPerson(.5f);
            }
            draw(mc, "hmi-etfuturum-" + path);
            String expected = path.equals("slime") ? "slime_block" : path;
            require(
                hmi.enabled() && ("minecraft:" + expected).equals(hmi.itemId()),
                "Et Futurum uses original " + expected + " profile");
            if (path.endsWith("lantern")) require(
                hmi.isLantern() && hmi.program()
                    .glow() != null,
                "upstream " + path + " glow");
            if (path.contains("_chest_")) {
                float pitch = mc.thePlayer.rotationPitch;
                try {
                    mc.thePlayer.rotationPitch = pitch + 28;
                    mc.thePlayer.ticksExisted++;
                    mc.entityRenderer.itemRenderer.renderItemInFirstPerson(.5f);
                    var lid = hmi.program()
                        .part(0);
                    require(
                        lid != null && !lid.equals(new org.joml.Matrix4f(), .0001f),
                        "upstream chest boat parts for " + path);
                } finally {
                    mc.thePlayer.rotationPitch = pitch;
                }
            }
            rendered++;
        }
        require(rendered > 0, "Et Futurum built-in mappings exercised");
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

    private void checkModRenderers(Minecraft mc) throws Exception {
        checkIngotGrips(mc);
        Class.forName("com.xyp.gtnrlib.qa.AnimationToolChecks")
            .getMethod("check", AnimationClientChecks.class)
            .invoke(null, this);
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
            if (com.xyp.gtnrlib.client.animation.HmiFirstPerson.INSTANCE.enabled()) require(
                !com.xyp.gtnrlib.client.animation.HmiFirstPerson.INSTANCE.sample(stack, .5f, 0),
                "HMI delegates " + id);
            draw(
                mc,
                renderer.getClass()
                    .getSimpleName());
        }
        require(wanted.isEmpty(), "optional mod renderers found; missing " + wanted);
    }

    private void checkIngotGrips(Minecraft mc) throws Exception {
        var hmi = HmiFirstPerson.INSTANCE;
        var iron = new ItemStack(Items.iron_ingot);
        var gold = new ItemStack(Items.gold_ingot);
        var chrome = gregTechStack("ingotChrome");
        var cobalt = gregTechStack("ingotCobalt");
        var hot = gregTechStack("ingotHotTitanium");
        var plate = gregTechStack("plateChrome");
        require(
            chrome.getItem() == plate.getItem() && chrome.getItem() == cobalt.getItem(),
            "ingots and plate share the real GT meta-item");
        org.joml.Matrix4f arm = null, item = null;
        int[] chromePixels = null;
        for (var stack : List.of(iron, gold, chrome, cobalt, hot)) {
            equip(mc, stack);
            tickPlayer();
            String name = stack.getUnlocalizedName();
            if (hmi.enabled()) {
                // Start each upstream program at the same idle frame so the matrices can be compared directly.
                hmi.detach();
                require(hmi.sample(stack, .5f, 0), "HMI handles ingot " + name);
                int[] pixels = draw(mc, "hmi-ingot-" + name);
                if (arm == null) {
                    arm = new org.joml.Matrix4f(hmi.right);
                    item = new org.joml.Matrix4f(hmi.item);
                } else require(
                    arm.equals(hmi.right, .0001f) && item.equals(hmi.item, .0001f),
                    "matches iron ingot arm and item pose: " + name);
                if (stack == chrome) chromePixels = pixels;
                if (stack == cobalt) {
                    int changed = 0;
                    for (int i = 0; i < pixels.length; i++) if (pixels[i] != chromePixels[i]) changed++;
                    require(changed > 50, "GT ingot material changes visible pixels: " + changed);
                }
            } else {
                require(target.applies(stack), "Punchy flat grip handles ingot " + name);
                draw(mc, "punchy-ingot-" + name);
            }
        }
        equip(mc, plate);
        tickPlayer();
        require(
            !(hmi.enabled() ? hmi.sample(plate, .5f, 0) : target.applies(plate)),
            "the plate metadata keeps its custom renderer");
        if (hmi.enabled()) checkRegisteredIngots(hmi);
    }

    private void checkRegisteredIngots(HmiFirstPerson hmi) {
        record Variant(Item item, int damage) {}
        hmi.detach();
        require(
            hmi.sample(new ItemStack(Items.iron_ingot), .5f, 0) && hmi.sample(new ItemStack(Items.gold_ingot), .5f, 0),
            "reference ingot pose at a fixed frame");
        var arm = new org.joml.Matrix4f(hmi.right);
        var item = new org.joml.Matrix4f(hmi.item);
        var seen = new HashSet<Variant>();
        int custom = 0;
        for (String name : OreDictionary.getOreNames()) {
            if (!name.startsWith("ingot")) continue;
            for (var stack : OreDictionary.getOres(name)) {
                if (stack.getItem() instanceof net.minecraft.item.ItemBlock
                    || !seen.add(new Variant(stack.getItem(), stack.getItemDamage()))) continue;
                var renderer = MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.EQUIPPED_FIRST_PERSON);
                if (!hmi.sample(stack, .5f, 0) || !arm.equals(hmi.right, .0001f)
                    || !item.equals(hmi.item, .0001f)
                    || hmi.forgeRenderer() != renderer)
                    throw new AssertionError("Missing ingot grip for " + name + " " + stack);
                if (renderer != null) custom++;
            }
        }
        require(
            custom > 0,
            "HMI flat grip for all " + seen.size()
                + " registered ingot variants, including "
                + custom
                + " with a Forge renderer");
    }

    private static ItemStack gregTechStack(String ore) {
        for (var stack : OreDictionary.getOres(ore)) {
            if (String.valueOf(Item.itemRegistry.getNameForObject(stack.getItem()))
                .startsWith("gregtech:")
                && MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.EQUIPPED_FIRST_PERSON) != null)
                return stack.copy();
        }
        throw new AssertionError("Missing GT stack with equipped renderer for " + ore);
    }

    void require(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        results.add(label);
    }

    void tickPlayer() {
        for (int i = 0; i < 4; i++) {
            target.tick(new TickEvent.ClientTickEvent(TickEvent.Phase.START));
            target.tick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        }
    }

    private void reload(Minecraft mc, String name) {
        GTNRLibConfig.builtInFirstPerson = false;
        GTNRLibConfig.animationPack = name;
        target.onResourceManagerReload(mc.getResourceManager());
    }

    void equip(Minecraft mc, ItemStack stack) {
        mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem] = stack;
        for (int i = 0; i < 8; i++) mc.entityRenderer.itemRenderer.updateEquippedItem();
    }

    int[] draw(Minecraft mc, String name) throws Exception {
        return draw(mc, name, .5f);
    }

    int[] draw(Minecraft mc, String name, float partial) throws Exception {
        return draw(mc, name, partial, () -> mc.entityRenderer.itemRenderer.renderItemInFirstPerson(partial));
    }

    int[] draw(Minecraft mc, String name, float partial, Runnable render) throws Exception {
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
            // The isolated item silhouettes use neutral illumination for material-pixel comparisons.
            GL11.glDisable(GL11.GL_FOG);
            mc.entityRenderer.disableLightmap(partial);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            render.run();
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

    void finish(Minecraft mc, Throwable error) {
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
