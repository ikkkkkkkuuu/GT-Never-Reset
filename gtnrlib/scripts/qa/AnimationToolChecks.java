package com.xyp.gtnrlib.qa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import org.joml.Matrix4f;

import com.xyp.gtnrlib.client.animation.FirstPersonAnimations;
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
        if (hmi.enabled()) checkCutter(qa, mc);
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
            if (r < g + 18 || g < b + 8) continue;
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
