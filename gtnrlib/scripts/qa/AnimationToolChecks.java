package com.xyp.gtnrlib.qa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import org.joml.Matrix4f;

import com.xyp.gtnrlib.client.animation.FirstPersonAnimations;
import com.xyp.gtnrlib.client.animation.FirstPersonItemActions;
import com.xyp.gtnrlib.client.animation.HmiFirstPerson;
import com.xyp.gtnrlib.client.animation.HmiItemBindings;
import com.xyp.gtnrlib.client.animation.ToolGrip;

import gregtech.api.enums.Materials;
import gregtech.api.items.MetaGeneratedTool;
import gregtech.common.items.IDMetaTool01;
import gregtech.common.items.MetaGeneratedTool01;
import gregtech.common.tools.ToolTurbine;
import ic2.api.item.IC2Items;
import tconstruct.library.TConstructRegistry;
import tconstruct.library.tools.HarvestTool;
import tconstruct.library.tools.ToolCore;
import tconstruct.library.tools.Weapon;
import tconstruct.tools.TinkerTools;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.renderers.item.ItemWandRenderer;
import thaumcraft.client.renderers.models.gear.ModelWand;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandManager;

public final class AnimationToolChecks {

    public static void check(AnimationClientChecks qa) throws Exception {
        var mc = Minecraft.getMinecraft();
        var hmi = HmiFirstPerson.INSTANCE;
        int checked = 0;
        for (var tool : MetaGeneratedTool.sInstances.values()) {
            for (var entry : tool.mToolStats.entrySet()) {
                var stack = new ItemStack(tool, 1, entry.getKey());
                boolean rotor = entry.getValue() instanceof ToolTurbine;
                if ((ToolGrip.resolve(stack) != null) == rotor) throw new AssertionError("GT tool family " + stack);
                if (!rotor) checked++;
            }
        }
        qa.require(checked > 0, "classified " + checked + " GT hand-tool metadata entries; machine rotors excluded");
        int tinkers = 0;
        for (var tool : TConstructRegistry.getToolMapping()) {
            if (!(tool instanceof HarvestTool || tool instanceof Weapon || tool == TinkerTools.chisel)) continue;
            var stack = tinkers(tool);
            var grip = ToolGrip.resolve(stack);
            if (grip == null) throw new AssertionError("Missing Tinkers' tool " + tool.getToolName());
            var broken = stack.copy();
            broken.getTagCompound()
                .getCompoundTag("InfiTool")
                .setBoolean("Broken", true);
            if (ToolGrip.resolve(broken) == null) throw new AssertionError("Missing broken tool " + tool.getToolName());
            tinkers++;
        }
        qa.require(tinkers > 0, "classified " + tinkers + " built and broken Tinkers' melee/harvest tools");
        var drill = gt(IDMetaTool01.DRILL_LV);
        for (var example : List.of(
            new Example("gt-drill", drill, ToolGrip.PICKAXE, Items.iron_pickaxe),
            new Example("gt-chainsaw", gt(IDMetaTool01.CHAINSAW_LV), ToolGrip.AXE, Items.iron_axe),
            new Example("gt-file", gt(IDMetaTool01.FILE), ToolGrip.HANDHELD, null),
            new Example("tic-longsword", tinkers(TinkerTools.longsword), ToolGrip.SWORD, Items.iron_sword),
            new Example("tic-shovel", tinkers(TinkerTools.shovel), ToolGrip.SHOVEL, Items.iron_shovel),
            new Example("ic2-drill", IC2Items.getItem("miningDrill"), ToolGrip.PICKAXE, Items.iron_pickaxe),
            new Example("ic2-chainsaw", IC2Items.getItem("chainsaw"), ToolGrip.AXE, Items.iron_axe),
            new Example("ic2-hoe", IC2Items.getItem("electricHoe"), ToolGrip.HOE, Items.iron_hoe))) {
            var stack = example.stack;
            qa.require(stack != null && ToolGrip.resolve(stack) == example.family, "family " + example.name);
            qa.equip(mc, stack);
            qa.tickPlayer();
            if (!hmi.enabled()) {
                qa.require(FirstPersonAnimations.INSTANCE.applies(stack), "Punchy handles " + example.name);
                qa.draw(mc, "tool-punchy-" + example.name);
                continue;
            }
            var actual = action(qa, stack, example.name);
            qa.require(
                !actual[0].equals(actual[2], .001f) || !actual[1].equals(actual[3], .001f),
                "accepted swing moves " + example.name);
            if (example.reference != null) {
                var reference = action(qa, new ItemStack(example.reference), example.name + "-reference");
                for (int i = 0; i < reference.length; i++)
                    qa.require(reference[i].equals(actual[i], .0001f), "upstream pose " + i + " for " + example.name);
            }
        }
        var movingTool = gt(IDMetaTool01.DRILL_LV);
        qa.require(ToolGrip.resolve(movingTool) == ToolGrip.PICKAXE, "drill before changing the same stack");
        movingTool.setItemDamage(IDMetaTool01.CHAINSAW_LV.ID);
        qa.require(ToolGrip.resolve(movingTool) == ToolGrip.AXE, "same-slot GT metadata changes the grip immediately");
        if (hmi.enabled()) {
            checkWand(qa, mc);
            checkCutter(qa, mc);
        }
    }

    private static void checkWand(AnimationClientChecks qa, Minecraft mc) throws Exception {
        var hmi = HmiFirstPerson.INSTANCE;
        var actions = FirstPersonItemActions.INSTANCE;
        var p = mc.thePlayer;
        int savedTick = p.ticksExisted;
        var hit = mc.objectMouseOver;
        var wand = (ItemWandCasting) ConfigItems.itemWandCasting;
        var stack = new ItemStack(wand);
        try {
            hmi.detach();
            mc.objectMouseOver = null;
            wand.setCap(stack, ConfigItems.WAND_CAP_GOLD);
            wand.setRod(stack, ConfigItems.WAND_ROD_GREATWOOD);
            qa.equip(mc, stack);
            for (int i = 0; i < 32; i++) {
                p.ticksExisted++;
                hmi.sample(stack, .5f, 0);
            }
            qa.require(
                hmi.handheldTool() != null && hmi.forgeRenderer() instanceof ItemWandRenderer,
                "Thaumcraft wand uses its original renderer in the animated hand");
            var model = ItemWandRenderer.class.getDeclaredField("model");
            var capField = ModelWand.class.getDeclaredField("CapBottom");
            model.setAccessible(true);
            capField.setAccessible(true);
            var cap = (ModelRenderer) capField.get(model.get(hmi.forgeRenderer()));
            var idle = AnimationGripChecks.measureWand(qa, mc, stack, "wand-idle", cap);
            wand.setFocus(stack, new ItemStack(ConfigItems.itemFocusFrost));
            p.isSwingInProgress = false;
            WandManager.setCooldown(p, 0);
            stack.useItemRightClick(p.worldObj, p);
            qa.require(actions.playing("attack"), "frost focus swing starts the wand flick");
            p.ticksExisted += 3;
            var flick = AnimationGripChecks.measureWand(qa, mc, stack, "wand-flick", cap);
            qa.require(
                flick.fist()
                    .distance(idle.fist()) > .10
                    && flick.forearm()
                        .angle(idle.forearm()) > .25,
                "frost cast moves and turns the casting arm");
            p.ticksExisted += 6;
            hmi.sample(stack, .5f, 0);
            qa.require(!actions.playing("attack"), "wand flick returns to rest");

            wand.addVis(stack, Aspect.FIRE, wand.getMaxVis(stack), true);
            wand.setFocus(stack, new ItemStack(ConfigItems.itemFocusFire));
            checkChannel(qa, mc, stack, "wand", cap);
            wand.setRod(stack, ConfigItems.STAFF_ROD_SILVERWOOD);
            checkChannel(qa, mc, stack, "staff", cap);

            wand.setFocus(stack, null);
            wand.setRod(stack, ConfigItems.WAND_ROD_SILVERWOOD);
            stack.getTagCompound()
                .setBoolean("sceptre", true);
            AnimationGripChecks.measureWand(qa, mc, stack, "sceptre-idle", cap);
        } finally {
            p.clearItemInUse();
            WandManager.setCooldown(p, 0);
            p.ticksExisted = savedTick;
            p.isSwingInProgress = false;
            mc.objectMouseOver = hit;
            hmi.detach();
        }
    }

    private static void checkChannel(AnimationClientChecks qa, Minecraft mc, ItemStack stack, String name,
        ModelRenderer cap) throws Exception {
        var p = mc.thePlayer;
        var actions = FirstPersonItemActions.INSTANCE;
        var rest = AnimationGripChecks.measureWand(qa, mc, stack, name + "-cast-0", cap);
        WandManager.setCooldown(p, 0);
        stack.useItemRightClick(p.worldObj, p);
        HmiFirstPerson.INSTANCE.sample(stack, .5f, 0);
        qa.require(p.getItemInUse() == stack && actions.playing("use"), name + " starts a fire cast");
        var extended = rest;
        for (int tick = 1; tick <= 36; tick++) {
            if (tick == 29) {
                p.stopUsingItem();
                HmiFirstPerson.INSTANCE.sample(stack, .5f, 0);
            }
            // World.updateEntityWithOptionalForce advances the entity clock before calling onUpdate.
            p.ticksExisted++;
            p.onUpdate();
            var pose = AnimationGripChecks.measureWand(qa, mc, stack, name + "-cast-" + tick, cap);
            qa.require(
                rest.shoulder()
                    .distance(pose.shoulder()) < .09,
                name + " shoulder remains anchored at " + tick);
            qa.require(
                rest.shaftInHand()
                    .angle(pose.shaftInHand()) < .015,
                name + " shaft follows the fist at " + tick);
            if (tick == 7) {
                extended = pose;
                qa.require(
                    rest.fist()
                        .distance(pose.fist()) > .16
                        && rest.forearm()
                            .angle(pose.forearm()) > .4,
                    name + " forearm reaches forward and turns into the cast");
            }
            if (tick == 20) qa.require(
                actions.playing("use") && extended.fist()
                    .distance(pose.fist()) > .06
                    && extended.forearm()
                        .angle(pose.forearm()) > .10,
                name + " wrist and arm keep moving during channel");
            if (tick == 31) qa.require(
                actions.playing("use") && rest.fist()
                    .distance(pose.fist()) > .045,
                name + " arm eases back from the release");
            if (tick == 36) qa.require(
                !actions.playing("use") && rest.fist()
                    .distance(pose.fist()) < .06,
                name + " hand recovers to rest");
        }
    }

    static ItemStack cutter() {
        return MetaGeneratedTool01.INSTANCE
            .getToolWithStats(IDMetaTool01.WIRECUTTER.ID, 1, Materials.Steel, Materials.Wood, null);
    }

    private static void checkCutter(AnimationClientChecks qa, Minecraft mc) throws Exception {
        var hmi = HmiFirstPerson.INSTANCE;
        var p = mc.thePlayer;
        var tool = cutter();
        int tick = p.ticksExisted;
        try {
            hmi.detach();
            qa.equip(mc, tool);
            for (int i = 0; i < 32; i++) {
                p.ticksExisted++;
                hmi.sample(tool, .5f, 0);
            }
            int[] opened = qa.draw(mc, "cutter-open");
            qa.require(hmi.handheldTool() instanceof HmiItemBindings.TwoHandedTool, "manual GT cutter uses both grips");
            AnimationCutterChecks
                .check(qa, tool, (HmiItemBindings.TwoHandedTool) hmi.handheldTool(), hmi.forgeRenderer());
            p.isSwingInProgress = false;
            hmi.duringBlockUseSwing(p::swingItem);
            p.ticksExisted++;
            hmi.sample(tool, .5f, 0);
            qa.require(hmi.strokeSeconds() == 0, "block prediction waits for GT to accept the cut");
            p.isSwingInProgress = false;
            p.swingItem();
            int openSpan = gripSpan(qa, mc, opened), start = p.ticksExisted;
            int[] frame = opened;
            for (int i = 1; i <= 12; i++) {
                p.ticksExisted = start + i / 2;
                float partial = (i % 2) * .5f;
                frame = qa.draw(mc, "cutter-frame-" + i, partial);
                if (i == 5) {
                    int closedSpan = gripSpan(qa, mc, frame);
                    qa.require(
                        closedSpan < .8 * openSpan,
                        "both hands squeeze the handles: contact span " + openSpan + " to " + closedSpan);
                    qa.require(
                        Math.abs(hmi.strokeSeconds() - .125) < .00001 && !Arrays.equals(opened, frame),
                        "accepted swing reaches the closed frame");
                    qa.require(
                        Arrays.equals(frame, qa.draw(mc, "cutter-closed-repeat", partial)),
                        "rendering twice does not advance the cut");
                }
            }
            int restored = gripSpan(qa, mc, frame);
            qa.require(Math.abs(restored - openSpan) < .12 * openSpan, "handles reopen to the resting separation");
            qa.require(hmi.strokeSeconds() == 0, "both grips return to rest after one snip");
            hmi.swing();
            p.ticksExisted += 3;
            hmi.swing();
            p.ticksExisted += 4;
            hmi.sample(tool, .5f, 0);
            qa.require(Math.abs(hmi.strokeSeconds() - .075) < .00001, "early repeat follows the first complete snip");
            qa.equip(mc, gt(IDMetaTool01.WIRECUTTER_LV));
            qa.draw(mc, "cutter-powered-generic");
            qa.require(hmi.handheldTool() == null && hmi.strokeSeconds() == 0, "powered cutter retains generic grip");
        } finally {
            p.ticksExisted = tick;
            p.isSwingInProgress = false;
            hmi.detach();
        }
    }

    private static int gripSpan(AnimationClientChecks qa, Minecraft mc, int[] pixels) {
        // Find both actual skin-to-steel boundaries in the fixture's neutral lighting.
        int width = mc.displayWidth, first = width, last = -1, contacts = 0;
        for (int i = width + 1; i < pixels.length - width - 1; i++) {
            int rgb = pixels[i], r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
            // The selected development skin can be brown or have blue sleeves. Steel and the cleared
            // framebuffer are neutral in both cases.
            if (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) < 9) continue;
            if (!steel(pixels[i - width], pixels[0]) && !steel(pixels[i + width], pixels[0])
                && !steel(pixels[i - 1], pixels[0])
                && !steel(pixels[i + 1], pixels[0])) continue;
            contacts++;
            first = Math.min(first, i % width);
            last = Math.max(last, i % width);
        }
        qa.require(
            contacts > 12 && last - first > mc.displayHeight / 12,
            "visible steel touches separate fists at x=" + first + " and " + last);
        return last - first;
    }

    private static boolean steel(int rgb, int background) {
        return rgb != background && Math.abs((rgb >> 16 & 255) - (rgb & 255)) <= 3;
    }

    private static Matrix4f[] action(AnimationClientChecks qa, ItemStack stack, String name) throws Exception {
        var mc = Minecraft.getMinecraft();
        var hmi = HmiFirstPerson.INSTANCE;
        var p = mc.thePlayer;
        int tick = p.ticksExisted;
        qa.equip(mc, stack);
        hmi.detach();
        try {
            qa.require(
                hmi.sample(stack, .5f, 0) && hmi.forgeRenderer()
                    == MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.EQUIPPED_FIRST_PERSON),
                "kept equipped renderer for " + name);
            qa.draw(mc, "tool-idle-" + name);
            var arm = new Matrix4f(hmi.right);
            var item = new Matrix4f(hmi.item);
            p.isSwingInProgress = false;
            p.swingItem();
            p.ticksExisted += 3;
            qa.draw(mc, "tool-swing-" + name);
            return new Matrix4f[] { arm, item, new Matrix4f(hmi.right), new Matrix4f(hmi.item) };
        } finally {
            p.ticksExisted = tick;
            p.isSwingInProgress = false;
        }
    }

    private static ItemStack gt(IDMetaTool01 id) {
        return MetaGeneratedTool01.INSTANCE.getToolWithStats(
            id.ID,
            1,
            Materials.Steel,
            Materials.Wood,
            id == IDMetaTool01.FILE ? null : new long[] { 100000, 32, 1, -1 });
    }

    private static ItemStack tinkers(ToolCore item) {
        var tools = new ArrayList<ItemStack>();
        item.buildTool(2, null, tools);
        return tools.get(0);
    }

    private record Example(String name, ItemStack stack, ToolGrip family, Item reference) {}
}
