package com.xyp.gtnrlib.qa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.joml.Matrix4fc;

import com.xyp.gtnrlib.client.animation.FirstPersonAnimations;
import com.xyp.gtnrlib.client.animation.FirstPersonItemActions;
import com.xyp.gtnrlib.client.animation.FirstPersonItemActions.Scene;
import com.xyp.gtnrlib.client.render.animation.FirstPersonActionEffects;

final class AnimationInspectionBindingChecks {

    private static final String EXACT = "animationclientqa:inspect_exact";
    private static final String WILDCARD = "animationclientqa:inspect_any";
    private static final String BLOCK = "gtnrlib:block_inspect";
    private static final String EARLY_SOUND = "minecraft:dig.glass";
    private static final String TERMINAL_SOUND = "minecraft:random.anvil_land";
    private static final ResourceLocation EFFECT = new ResourceLocation("animationclientqa", "inspection_probe");

    static void verify(AnimationClientChecks qa, Minecraft mc, KeyBinding key, int nextSlot) throws Exception {
        var player = mc.thePlayer;
        var actions = FirstPersonItemActions.INSTANCE;
        var probe = new EffectProbe(false);
        FirstPersonActionEffects.register(EFFECT, probe);
        FirstPersonAnimations.INSTANCE.onResourceManagerReload(mc.getResourceManager());
        int slot = player.inventory.currentItem;
        var held = player.getHeldItem();
        var nextStack = player.inventory.mainInventory[nextSlot];
        var screen = mc.currentScreen;
        try (var sounds = new SoundProbe()) {
            var exact = new ItemStack(Blocks.wool, 3, 3);
            qa.equip(mc, exact);
            AnimationBlockInspectChecks.press(key);
            qa.require(
                actions.playing(EXACT) && !actions.playing(WILDCARD) && !actions.playing(BLOCK),
                "a data-defined exact inspection wins over its item-wide rule and ordinary block inspection");
            player.ticksExisted += 3;
            qa.tickPlayer();
            qa.draw(mc, "data-inspection-two-hands");
            qa.require(
                probe.beforeHands == 1 && probe.overlays == 1
                    && EFFECT.equals(probe.scene.effect())
                    && Math.abs(probe.scene.seconds() - .175) < .001
                    && probe.stack == exact
                    && actions.inspectionUsesLeftArm(),
                "the two-handed JSON inspection calls its effect at the render sample time");
            qa.draw(mc, "data-inspection-repeat-render");
            qa.require(
                sounds.ids.equals(List.of(EARLY_SOUND)),
                "redrawing does not replay the JSON sound marker, requested=" + sounds.ids);

            mc.currentScreen = new GuiInventory(player);
            player.ticksExisted += 16;
            qa.tickPlayer();
            qa.draw(mc, "data-inspection-cancelled");
            qa.require(
                !actions.playing(EXACT) && probe.beforeHands == 2
                    && probe.overlays == 2
                    && sounds.ids.equals(List.of(EARLY_SOUND)),
                "opening an inventory clears the effect and suppresses unplayed cues, requested=" + sounds.ids);
            mc.currentScreen = screen;

            AnimationBlockInspectChecks.press(key);
            player.ticksExisted += 16;
            qa.tickPlayer();
            qa.draw(mc, "data-inspection-completed");
            qa.require(
                !actions.playing(EXACT) && probe.beforeHands == 2
                    && probe.overlays == 2
                    && sounds.ids.equals(List.of(EARLY_SOUND, EARLY_SOUND, TERMINAL_SOUND)),
                "replay dispatches both cues including the terminal marker when crossing the clip end, requested="
                    + sounds.ids);

            var fallback = new ItemStack(Blocks.wool, 1, 7);
            qa.equip(mc, fallback);
            AnimationBlockInspectChecks.press(key);
            qa.require(
                actions.playing(WILDCARD) && !actions.playing(BLOCK),
                "an invalid exact binding leaves the valid item-wide inspection available");
            qa.draw(mc, "data-inspection-one-hand");
            qa.require(
                probe.beforeHands == 3 && probe.overlays == 3 && !actions.inspectionUsesLeftArm(),
                "the item-wide inspection uses its own hand mode and shares the registered effect");
            player.inventory.currentItem = nextSlot;
            qa.equip(mc, fallback.copy());
            qa.tickPlayer();
            qa.draw(mc, "data-inspection-slot-changed");
            qa.require(
                !actions.playing(WILDCARD) && probe.beforeHands == 3 && probe.overlays == 3,
                "switching to an identical stack in another slot clears the generic inspection effect");

            FirstPersonActionEffects.unregister(EFFECT, probe);
            player.inventory.currentItem = slot;
            qa.equip(mc, fallback);
            checkEffectFailure(qa, mc, key);
        } finally {
            FirstPersonActionEffects.unregister(EFFECT, probe);
            mc.currentScreen = screen;
            player.inventory.mainInventory[nextSlot] = nextStack;
            player.inventory.currentItem = slot;
            qa.equip(mc, held);
            qa.tickPlayer();
        }
    }

    private static void checkEffectFailure(AnimationClientChecks qa, Minecraft mc, KeyBinding key) throws Exception {
        var failing = new EffectProbe(true);
        FirstPersonActionEffects.register(EFFECT, failing);
        try {
            AnimationBlockInspectChecks.press(key);
            mc.thePlayer.ticksExisted++;
            qa.tickPlayer();
            int[] first = qa.draw(mc, "data-inspection-failed-hook");
            int[] next = qa.draw(mc, "data-inspection-failed-hook-isolated");
            qa.require(
                failing.beforeHands == 1 && failing.overlays == 0
                    && FirstPersonItemActions.INSTANCE.playing(WILDCARD)
                    && Arrays.stream(first)
                        .distinct()
                        .count() > 8
                    && Arrays.stream(next)
                        .distinct()
                        .count() > 8,
                "a throwing effect is disabled and normal held-item drawing continues");
        } finally {
            FirstPersonActionEffects.unregister(EFFECT, failing);
        }
    }

    private static final class EffectProbe implements FirstPersonActionEffects.Effect {

        private final boolean fail;
        private int beforeHands, overlays;
        private Scene scene;
        private ItemStack stack;

        private EffectProbe(boolean fail) {
            this.fail = fail;
        }

        @Override
        public void beforeHands(Scene scene, ItemStack stack, Matrix4fc itemPose) {
            beforeHands++;
            if (fail) throw new IllegalStateException("fixture effect failure");
            this.scene = scene;
            this.stack = stack;
        }

        @Override
        public void overlay(Scene scene, int width, int height) {
            overlays++;
        }
    }

    private static final class SoundProbe extends AbstractAppender implements AutoCloseable {

        private final Logger logger = (Logger) LogManager.getLogger(SoundManager.class);
        private final Level previousLevel = logger.getLevel();
        private final List<String> ids = new ArrayList<>();

        private SoundProbe() {
            super("inspection-qa-sounds", null, null);
            start();
            logger.addAppender(this);
            logger.setLevel(Level.DEBUG);
        }

        @Override
        public void append(LogEvent event) {
            var message = event.getMessage();
            // Forge's sound event only posts after the sound manager's master-volume check.
            if (!"Skipped playing soundEvent: {}, master volume was zero".equals(message.getFormat())) return;
            String id = String.valueOf(message.getParameters()[0]);
            if (EARLY_SOUND.equals(id) || TERMINAL_SOUND.equals(id)) ids.add(id);
        }

        @Override
        public void close() {
            logger.removeAppender(this);
            logger.setLevel(previousLevel);
            stop();
        }
    }
}
