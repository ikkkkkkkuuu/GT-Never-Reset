package com.xyp.gtnrlib.qa;

import java.util.ArrayList;
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
