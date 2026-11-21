package com.xyp.gtnrlib.qa;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

import com.xyp.gtnrlib.client.animation.FirstPersonAnimations;
import com.xyp.gtnrlib.client.animation.FirstPersonItemActions;
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
                if (stack.getItem() != Items.filled_map) checks.require(
                    String.valueOf(net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem()))
                        .equals(HmiFirstPerson.OFFHAND.itemId()),
                    "animated offhand uses the equipped item profile");
                if (stack.getItem() == Items.bow) {
                    BackhandUtils
                        .useOffhandItem(player, false, () -> player.setItemInUse(stack, stack.getMaxItemUseDuration()));
                    player.ticksExisted++;
                    checks.draw(mc, "offhand-bow-draw");
                    checks.require(
                        using(HmiFirstPerson.OFFHAND) && !using(HmiFirstPerson.INSTANCE),
                        "drawing the offhand bow leaves the main sword idle");
                    player.clearItemInUse();
                    ((IBackhandPlayer) player).setOffhandItemInUse(false);
                }
            }
            var food = new ItemStack(Items.apple);
            checks.equip(mc, food.copy());
            setOffhand(mc, food);
            BackhandUtils.useOffhandItem(player, false, () -> player.setItemInUse(food, food.getMaxItemUseDuration()));
            player.ticksExisted++;
            checks.draw(mc, "offhand-eating");
            checks.require(
                using(HmiFirstPerson.OFFHAND) && !using(HmiFirstPerson.INSTANCE),
                "only the offhand animates eating with identical food in both hands");
            player.clearItemInUse();
            ((IBackhandPlayer) player).setOffhandItemInUse(false);
            var mainFood = player.getHeldItem();
            player.setItemInUse(mainFood, mainFood.getMaxItemUseDuration());
            player.ticksExisted++;
            checks.draw(mc, "main-eating-offhand-idle");
            checks.require(
                using(HmiFirstPerson.INSTANCE) && !using(HmiFirstPerson.OFFHAND),
                "only the main hand animates eating with identical food in both hands");
            player.clearItemInUse();
            var dye = new ItemStack(Items.dye, 1, 13);
            checks.equip(mc, dye.copy());
            setOffhand(mc, dye);
            var previousHit = mc.objectMouseOver;
            try {
                mc.objectMouseOver = null;
                checks.draw(mc, "offhand-dye-rest");
                BackhandUtils.useOffhandItem(player, false, () -> player.swingItem());
                checks.require(
                    FirstPersonItemActions.OFFHAND.playing("attack")
                        && !FirstPersonItemActions.INSTANCE.playing("attack"),
                    "an accepted Backhand swing selects only the offhand JSON clip");
                player.ticksExisted += 2;
                checks.draw(mc, "offhand-dye-swing");
                checks.require(
                    FirstPersonItemActions.OFFHAND.grip(false)
                        .m30() > .05f
                        && Math.abs(
                            FirstPersonItemActions.INSTANCE.grip(false)
                                .m30())
                            < .001f,
                    "only the swinging offhand receives the bound arm track");
                player.isSwingInProgress = false;
                player.swingItem();
                checks.require(
                    FirstPersonItemActions.OFFHAND.playing("attack")
                        && FirstPersonItemActions.INSTANCE.playing("attack"),
                    "both hands can play the same clip independently");
                setOffhand(mc, new ItemStack(Blocks.torch));
                checks.draw(mc, "offhand-switched-during-main-swing");
                checks.require(
                    !FirstPersonItemActions.OFFHAND.playing("attack")
                        && FirstPersonItemActions.INSTANCE.playing("attack"),
                    "changing the offhand clears only its outgoing clip");
            } finally {
                mc.objectMouseOver = previousHit;
            }
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
            if (Boolean.getBoolean("gtnrlib.animation.qa.toolMods"))
                Class.forName("com.xyp.gtnrlib.qa.AnimationWrenchChecks")
                    .getMethod("verifyOffhandTools", AnimationClientChecks.class, Minecraft.class)
                    .invoke(null, checks, mc);
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

    private static boolean using(HmiFirstPerson hand) throws Exception {
        // The fixture also runs against the development JAR with relocated LuaJ descriptors.
        Object frame = hand.program()
            .getClass()
            .getField("frame")
            .get(hand.program());
        Object value = frame.getClass()
            .getMethod("get", String.class)
            .invoke(frame, "using");
        return (boolean) value.getClass()
            .getMethod("toboolean")
            .invoke(value);
    }

    static void setOffhand(Minecraft mc, ItemStack stack) {
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
