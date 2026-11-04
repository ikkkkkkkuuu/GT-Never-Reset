package com.xyp.gtnrlib.client.animation;

import java.io.IOException;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBed;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBoat;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemSign;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import org.joml.Matrix4f;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.lwjgl.input.Keyboard;

import com.xyp.gtnrlib.GTNRLib;
import com.xyp.gtnrlib.compat.hmi.HmiPoseProgram;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Maps the local player's rendered stack and accepted swings to HMI's two visual hand passes. */
@SideOnly(Side.CLIENT)
public final class HmiFirstPerson {

    public static final HmiFirstPerson INSTANCE = new HmiFirstPerson();
    public final Matrix4f right = new Matrix4f(), left = new Matrix4f(), item = new Matrix4f();
    private HmiPoseProgram.Scripts scripts;
    private HmiPoseProgram program;
    private boolean resetProgram;
    private Object player;
    private Item lastItem;
    private HmiItemBindings.Profile bound;
    private HmiItemBindings.HandheldTool tool;
    private IItemRenderer forgeRenderer;
    private final Matrix4f wristMotion = new Matrix4f(), sceneMotion = new Matrix4f();
    private int lastDamage, swings;
    private String id;
    private boolean breaking, blockUseSwing;
    private double swingStart = Double.NEGATIVE_INFINITY, lastFrame = Double.NaN;

    private HmiFirstPerson() {}

    public void install(HmiPoseProgram.Scripts next) throws IOException {
        var ready = next == null ? null : new HmiPoseProgram(next);
        resetProgram = false;
        scripts = next;
        program = ready;
        reset(null);
    }

    private void reset(Object nextPlayer) {
        player = nextPlayer;
        id = null;
        bound = null;
        tool = null;
        forgeRenderer = null;
        lastItem = null;
        swings = 0;
        lastFrame = Double.NaN;
        swingStart = Double.NEGATIVE_INFINITY;
        breaking = false;
    }

    public boolean enabled() {
        return program != null;
    }

    public void detach() {
        if (player == null) return;
        reset(null);
        resetProgram = true;
    }

    public HmiPoseProgram program() {
        return program;
    }

    public String itemId() {
        return id;
    }

    public String modelVariant() {
        return bound == null ? "" : bound.variant();
    }

    public HmiItemBindings.HandheldTool handheldTool() {
        return tool;
    }

    public IItemRenderer forgeRenderer() {
        return forgeRenderer;
    }

    public boolean isLantern() {
        return "minecraft:lantern".equals(id) || "minecraft:soul_lantern".equals(id);
    }

    public void duringBlockUseSwing(Runnable vanillaSwing) {
        boolean previous = blockUseSwing;
        blockUseSwing = true;
        try {
            vanillaSwing.run();
        } finally {
            blockUseSwing = previous;
        }
    }

    public void swing() {
        if (program == null) return;
        var mc = Minecraft.getMinecraft();
        var p = mc.thePlayer;
        if (p == null || p != player || mc.gameSettings.thirdPersonView != 0) return;
        if (blockUseSwing && tool != null
            && tool.handlesBlockUseSwing()
            && p.getHeldItem() != null
            && p.getHeldItem()
                .getItem() == lastItem
            && p.getHeldItem()
                .getItemDamage() == lastDamage)
            return;
        swings++;
        swingStart = p.ticksExisted;
        breaking = mc.gameSettings.keyBindAttack.getIsKeyPressed() && mc.objectMouseOver != null
            && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK;
    }

    public boolean sample(ItemStack stack, float partial, float dip) {
        if (program == null) return false;
        var mc = Minecraft.getMinecraft();
        var p = mc.thePlayer;
        if (p == null || mc.gameSettings.thirdPersonView != 0 || p.isInvisible()) return false;
        HmiItemBindings.HandheldTool selected = null;
        IItemRenderer renderer = null;
        if (stack != null) {
            if (stack.getItem() instanceof ItemMap
                && MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.FIRST_PERSON_MAP) != null) return false;
            renderer = MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.EQUIPPED_FIRST_PERSON);
            if (renderer != null) {
                selected = HmiItemBindings.tool(stack, renderer);
                if (selected == null && !IngotGrip.matches(stack)) {
                    tool = null;
                    return false;
                }
            }
        }
        try {
            if (player != p) {
                if (player != null || resetProgram) program = new HmiPoseProgram(scripts);
                resetProgram = false;
                reset(p);
            }
            Item held = stack == null ? null : stack.getItem();
            boolean changed = id == null || held != lastItem || selected != tool;
            tool = selected;
            forgeRenderer = renderer;
            int damage = stack == null ? 0 : stack.getItemDamage();
            var profile = HmiItemBindings.resolve(stack);
            boolean reclassify = changed || damage != lastDamage
                || !Objects.equals(bound == null ? null : bound.itemId(), profile == null ? null : profile.itemId());
            bound = profile;
            if (reclassify) {
                String previous = id;
                classify(held, stack, damage);
                changed |= !Objects.equals(previous, id);
                if (damage != lastDamage && bound == null && tool == null && IngotGrip.matches(stack)) changed = true;
            }
            double now = (double) p.ticksExisted + partial;
            if (!reclassify && lastFrame == now) return true;
            float seconds = Double.isNaN(lastFrame) ? 1f / 60
                : (float) Math.max(0, Math.min(.05, (now - lastFrame) / 20));
            lastFrame = now;
            lastItem = held;
            lastDamage = damage;
            var frame = program.frame;
            frame.set("age", p.ticksExisted);
            frame.set(
                "using",
                LuaValue.valueOf(
                    p.isUsingItem() && stack != null
                        && p.getItemInUse() != null
                        && p.getItemInUse()
                            .getItem() == held));
            frame.set("ground", LuaValue.valueOf(p.onGround));
            frame.set("climb", LuaValue.valueOf(p.isOnLadder()));
            frame.set("water", LuaValue.valueOf(p.isInWater()));
            frame.set("sneak", LuaValue.valueOf(p.isSneaking()));
            frame.set("inspect", LuaValue.valueOf(mc.currentScreen == null && Keyboard.isKeyDown(Keyboard.KEY_J)));
            frame.set("swings", swings);
            frame.set("speed", Math.sqrt(p.motionX * p.motionX + p.motionY * p.motionY + p.motionZ * p.motionZ));
            frame.set("vertical", p.motionY);
            frame.set("pitch", p.prevRotationPitch + (p.rotationPitch - p.prevRotationPitch) * partial);
            frame.set(
                "yaw",
                p.prevRotationYaw
                    + net.minecraft.util.MathHelper.wrapAngleTo180_float(p.rotationYaw - p.prevRotationYaw) * partial);
            double elapsed = now - swingStart;
            int duration = program.swingDuration(id);
            float progress = elapsed >= 0 && elapsed < duration ? (float) (elapsed / duration) : 0;
            boolean hit = elapsed < duration && breaking;
            program.pose(true, id, progress, dip, changed, (swings & 1) != 0, hit, seconds);
            right.set(program.armMatrix);
            item.set(program.itemMatrix);
            if (tool != null) {
                try {
                    if (tool.pose(stack, partial, wristMotion.identity())) {
                        if (!wristMotion.isFinite()) throw new IllegalArgumentException("Non-finite wrist transform");
                        // Conjugate the local wrist motion into the independent arm's coordinate system.
                        item.invert(sceneMotion)
                            .mulLocal(wristMotion)
                            .mulLocal(item);
                        right.mulLocal(sceneMotion);
                        item.mul(wristMotion);
                    }
                } catch (RuntimeException ex) {
                    HmiItemBindings.unregisterTool(held, tool);
                    tool = null;
                    GTNRLib.LOG.error("Disabled failing HMI tool pose for {}", stack, ex);
                    return false;
                }
            }
            program.pose(false, HmiPoseProgram.EMPTY, progress, 0, changed, (swings & 1) != 0, hit, seconds);
            left.set(program.armMatrix);
            return true;
        } catch (Exception ex) {
            GTNRLib.LOG.error("HMI pose failed for {}. Reload resources to retry.", id, ex);
            program = null;
            return false;
        }
    }

    public boolean blockPose() {
        return program.frame.get("block")
            .toboolean() && program.renderAsBlock(id);
    }

    private void classify(Item held, ItemStack stack, int damage) {
        id = held == null ? HmiPoseProgram.EMPTY : String.valueOf(Item.itemRegistry.getNameForObject(held));
        if (bound != null) id = bound.itemId();
        else if (id.startsWith("etfuturum:")) {
            String name = id.substring("etfuturum:".length());
            if (scripts.assets()
                .containsKey("items/" + name + ".json")) id = "minecraft:" + name;
            else id = switch (name) {
                case "slime" -> "minecraft:slime_block";
                case "honey_block", "lantern", "soul_lantern", "soul_torch" -> "minecraft:" + name;
                default -> id;
            };
        } else if (held instanceof ItemPotion && ItemPotion.isSplash(damage)) id = "minecraft:splash_potion";
        else if (held == Items.boat || held instanceof ItemBoat) id = "minecraft:oak_boat";
        else if (id.equals("minecraft:wooden_door")) id = "minecraft:oak_door";
        else if (id.equals("minecraft:sign")) id = "minecraft:oak_sign";
        // Legacy equipment can borrow a vanilla item's grip and tool tags while keeping its own use behaviour.
        Item shape = bound == null ? held : (Item) Item.itemRegistry.getObject(id);
        if (shape == null) shape = held;
        var tags = new LuaTable();
        if (id.startsWith("minecraft:")) {
            boolean chestBoat = id.endsWith("_chest_boat") || id.equals("minecraft:bamboo_chest_raft");
            tag(tags, "chest_boats", chestBoat);
            tag(tags, "boats", chestBoat || id.endsWith("_boat") || id.equals("minecraft:bamboo_raft"));
        }
        tag(tags, "swords", shape instanceof ItemSword);
        tag(tags, "pickaxes", shape instanceof ItemPickaxe);
        tag(tags, "axes", shape instanceof ItemAxe);
        tag(tags, "shovels", shape instanceof ItemSpade);
        tag(tags, "hoes", shape instanceof ItemHoe);
        tag(tags, "skulls", shape instanceof ItemSkull);
        tag(tags, "doors", shape instanceof ItemDoor);
        tag(tags, "signs", shape instanceof ItemSign);
        tag(tags, "beds", shape instanceof ItemBed);
        tag(tags, "music_discs", shape instanceof ItemRecord);
        tag(tags, "nuggets", id.endsWith("_nugget"));
        var frame = program.frame;
        frame.set("mainItem", id);
        frame.set("tags", tags);
        frame.set(
            "action",
            held == null ? "none"
                : stack.getItemUseAction()
                    .name());
        frame.set("block", LuaValue.valueOf(shape instanceof ItemBlock));
        frame.set("lantern", LuaValue.valueOf(isLantern()));
        frame.set(
            "throwable",
            LuaValue.valueOf(
                shape instanceof ItemSnowball || shape instanceof ItemEgg
                    || shape instanceof ItemEnderPearl
                    || id.equals("minecraft:splash_potion")));
        boolean handheld = shape instanceof ItemTool || shape instanceof ItemSword
            || shape instanceof ItemHoe
            || shape instanceof ItemFishingRod
            || shape instanceof ItemBow
            || shape != null && shape.isFull3D();
        frame.set(
            "translate",
            LuaValue.valueOf(tool == null && (!handheld || bound == null && IngotGrip.matches(stack))));
    }

    private static void tag(LuaTable tags, String name, boolean present) {
        if (present) tags.set(name, LuaValue.TRUE);
    }
}
