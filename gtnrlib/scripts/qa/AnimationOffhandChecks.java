package com.xyp.gtnrlib.qa;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

import com.xyp.gtnrlib.client.animation.FirstPersonAnimations;
import com.xyp.gtnrlib.client.animation.HmiFirstPerson;
import com.xyp.gtnrlib.config.GTNRLibConfig;

import xonin.backhand.api.core.BackhandUtils;
import xonin.backhand.api.core.IBackhandPlayer;
import xonin.backhand.client.utils.BackhandRenderHelper;
import xonin.backhand.utils.BackhandConfigClient;

final class AnimationOffhandChecks {

    static void verify(AnimationClientChecks checks, Minecraft mc) throws Exception {
        var player = mc.thePlayer;
        var main = player.getHeldItem();
        var offhand = BackhandUtils.getOffhandItem(player);
        boolean empty = BackhandConfigClient.RenderEmptyOffhandAtRest;
        var probe = new Probe();
        MinecraftForgeClient.registerItemRenderer(Items.emerald, probe);
        try {
            BackhandConfigClient.RenderEmptyOffhandAtRest = false;
            setOffhand(mc, new ItemStack(Items.emerald));
            for (boolean enabled : new boolean[] { true, false }) {
                GTNRLibConfig.enableFirstPersonAnimations = enabled;
                for (var held : new ItemStack[] { null, new ItemStack(Items.diamond_sword),
                    new ItemStack(Blocks.stone) }) {
                    checks.equip(mc, held);
                    probe.calls = 0;
                    int slot = player.inventory.currentItem;
                    checks.draw(
                        mc,
                        "offhand-probe-" + enabled
                            + "-"
                            + (held == null ? "empty"
                                : held.getItem()
                                    .getUnlocalizedName()));
                    checks.require(
                        probe.calls == 1 && probe.offhand,
                        "exactly one native offhand pass, animations=" + enabled);
                    checks.require(player.inventory.currentItem == slot, "offhand render restores selected slot");
                    if (enabled) checks.require(
                        HmiFirstPerson.INSTANCE.itemId()
                            .equals(
                                held == null ? "minecraft:air"
                                    : held.getItem() == Items.diamond_sword ? "minecraft:diamond_sword"
                                        : "minecraft:stone"),
                        "offhand does not overwrite main animation state");
                }
            }
            GTNRLibConfig.enableFirstPersonAnimations = true;
            checks.equip(mc, new ItemStack(Items.filled_map));
            probe.calls = 0;
            checks.draw(mc, "offhand-main-map");
            checks.require(probe.calls == 0, "main-hand map keeps Backhand two-handed exclusion");
            checks.equip(mc, new ItemStack(Items.diamond_sword));
            for (var stack : new ItemStack[] { new ItemStack(Blocks.torch), new ItemStack(Blocks.stone),
                new ItemStack(Items.apple), new ItemStack(Items.bow), new ItemStack(Items.filled_map) }) {
                setOffhand(mc, stack);
                checks.draw(
                    mc,
                    "offhand-" + stack.getItem()
                        .getUnlocalizedName());
                checks.require(HmiFirstPerson.INSTANCE.enabled(), "offhand item renders without disabling animation");
            }
            var food = new ItemStack(Items.apple);
            checks.equip(mc, food.copy());
            setOffhand(mc, food);
            BackhandUtils.useOffhandItem(player, false, () -> player.setItemInUse(food, food.getMaxItemUseDuration()));
            player.ticksExisted++;
            checks.draw(mc, "offhand-eating");
            // The dev JAR relocates LuaJ; inspect the frame without binding the fixture to its unshaded descriptor.
            Object frame = HmiFirstPerson.INSTANCE.program()
                .getClass()
                .getField("frame")
                .get(HmiFirstPerson.INSTANCE.program());
            Object using = frame.getClass()
                .getMethod("get", String.class)
                .invoke(frame, "using");
            checks.require(
                !(boolean) using.getClass()
                    .getMethod("toboolean")
                    .invoke(using),
                "offhand eating does not animate identical main-hand food");
            player.clearItemInUse();
            ((IBackhandPlayer) player).setOffhandItemInUse(false);
            setOffhand(mc, null);
            checks.draw(mc, "offhand-removed");
            checks.require(
                !FirstPersonAnimations.offhandOwnsLeftArm(.5f),
                "empty resting offhand releases animated left arm");
            BackhandConfigClient.RenderEmptyOffhandAtRest = true;
            checks.draw(mc, "offhand-empty-visible");
            checks
                .require(FirstPersonAnimations.offhandOwnsLeftArm(.5f), "Backhand empty-hand preference owns left arm");
            BackhandConfigClient.RenderEmptyOffhandAtRest = false;
            AnimationBlockUseChecks.verify(mc);
            checks.require(true, "offhand placement preserves main-hand action isolation");
        } finally {
            MinecraftForgeClient.registerItemRenderer(Items.emerald, null);
            player.clearItemInUse();
            ((IBackhandPlayer) player).setOffhandItemInUse(false);
            BackhandConfigClient.RenderEmptyOffhandAtRest = empty;
            GTNRLibConfig.enableFirstPersonAnimations = true;
            checks.equip(mc, main);
            setOffhand(mc, offhand);
        }
    }

    private static void setOffhand(Minecraft mc, ItemStack stack) {
        BackhandUtils.setPlayerOffhandItem(mc.thePlayer, stack);
        for (int i = 0; i < 8; i++) BackhandUtils
            .useOffhandItem(mc.thePlayer, false, () -> BackhandRenderHelper.itemRenderer.updateEquippedItem());
    }

    private static final class Probe implements IItemRenderer {

        private int calls;
        private boolean offhand;

        @Override
        public boolean handleRenderType(ItemStack item, ItemRenderType type) {
            return type == ItemRenderType.EQUIPPED_FIRST_PERSON;
        }

        @Override
        public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
            return false;
        }

        @Override
        public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
            calls++;
            offhand = BackhandUtils.isUsingOffhand(Minecraft.getMinecraft().thePlayer);
        }
    }
}
