package com.xyp.gtnrlib.qa;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import com.xyp.gtnrlib.client.animation.FirstPersonItemActions;
import com.xyp.gtnrlib.client.animation.HmiFirstPerson;
import com.xyp.gtnrlib.client.input.KeyBindingLookup;

import cpw.mods.fml.common.Loader;

final class AnimationDollInspectChecks {

    private static final String DOLL = "sciencenotleisure:player_doll";
    private static final String CLIP = "gtnrlib:gtnl_doll_inspect";

    static void verify(AnimationClientChecks qa, Minecraft mc) throws Exception {
        var player = mc.thePlayer;
        var actions = FirstPersonItemActions.INSTANCE;
        var hmi = HmiFirstPerson.INSTANCE;
        var key = KeyBindingLookup.find("gtnrlib.key.inspect_block", null);
        qa.require(
            hmi.enabled() && hmi.program()
                .builtIn() && key != null,
            "built-in inspection key is active");
        boolean hasGTNL = Loader.isModLoaded("sciencenotleisure");
        qa.require(
            hasGTNL == Boolean.getBoolean("gtnrlib.animation.qa.doll.expectGTNL"),
            "expected GTNL presence=" + hasGTNL);

        int tick = player.ticksExisted;
        int slot = player.inventory.currentItem;
        int nextSlot = (slot + 1) % 9;
        var held = player.getHeldItem();
        var nextStack = player.inventory.mainInventory[nextSlot];
        try {
            qa.equip(mc, new ItemStack(Items.diamond_sword));
            AnimationBlockInspectChecks.press(key);
            qa.require(!actions.playing(CLIP), "vanilla handheld items do not start the doll inspection");
            qa.equip(mc, new ItemStack(Blocks.glass));
            qa.tickPlayer();
            AnimationBlockInspectChecks.press(key);
            qa.require(
                actions.playing("gtnrlib:block_inspect") && !actions.playing(CLIP),
                "vanilla glass retains ordinary block inspection");
            qa.equip(mc, null);
            qa.tickPlayer();
            AnimationInspectionBindingChecks.verify(qa, mc, key, nextSlot);
            if (hasGTNL) checkDoll(qa, mc, key, nextSlot);
            else qa.require(!Item.itemRegistry.containsKey(DOLL), "library startup does not register a GTNL doll");
        } finally {
            player.inventory.mainInventory[nextSlot] = nextStack;
            player.inventory.currentItem = slot;
            player.ticksExisted = tick;
            qa.equip(mc, held);
            qa.tickPlayer();
            hmi.detach();
        }
    }

    private static void checkDoll(AnimationClientChecks qa, Minecraft mc, KeyBinding key, int nextSlot)
        throws Exception {
        var player = mc.thePlayer;
        var actions = FirstPersonItemActions.INSTANCE;
        var hmi = HmiFirstPerson.INSTANCE;
        var item = (Item) Item.itemRegistry.getObject(DOLL);
        qa.require(item != null, "GTNL registered the actual player doll item");
        var stack = new ItemStack(item, 3, 0);
        var tag = new NBTTagCompound();
        tag.setByte("RenderCapeMode", (byte) 0);
        stack.setTagCompound(tag);
        stack.setStackDisplayName("QA doll");
        var saved = stack.copy();

        var nativeRenderer = MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.EQUIPPED_FIRST_PERSON);
        qa.require(nativeRenderer != null, "GTNL registered a native first-person doll renderer");
        var renderer = new DollRendererProbe(nativeRenderer);
        MinecraftForgeClient.registerItemRenderer(item, renderer);
        var model = ((RenderPlayer) RenderManager.instance.getEntityRenderObject(player)).modelBipedMain;
        try (var right = new AnimationBlockInspectChecks.ArmProbe(model, false);
            var left = new AnimationBlockInspectChecks.ArmProbe(model, true)) {
            qa.equip(mc, stack);
            hmi.detach();
            qa.tickPlayer();
            int start = player.ticksExisted;
            int[] idle = drawPose(qa, mc, start, 0, "idle");
            var rightRest = right.palm();
            var leftRest = left.palm();
            boolean swinging = player.isSwingInProgress;

            AnimationBlockInspectChecks.press(key);
            qa.require(actions.playing(CLIP), "the inspection key selects the doll-specific clip");
            int lastRender = renderer.renders;
            int[] raised = drawPose(qa, mc, start, .8, "raised");
            qa.require(renderer.renders == lastRender + 1, "the raised doll uses GTNL's native renderer exactly once");
            qa.require(
                renderer.rendered != stack && ItemStack.areItemStacksEqual(renderer.rendered, saved),
                "GTNL receives a matching visual copy of the held doll");
            qa.require(
                right.palm()
                    .distance(rightRest) > .08f
                    && left.palm()
                        .distance(leftRest) > .08f,
                "both arms move to lift the doll");
            qa.require(!Arrays.equals(idle, raised), "the raised inspection changes the framebuffer");

            int[] reach = drawPose(qa, mc, start, 2.08, "rift-reach");
            float reachedAt = hmi.item.m30();
            drawPose(qa, mc, start, 2.19, "rival-pull");
            float rivalPull = hmi.item.m30();
            drawPose(qa, mc, start, 2.34, "player-pull");
            float playerPull = hmi.item.m30();
            drawPose(qa, mc, start, 2.46, "rival-resists");
            float rivalResists = hmi.item.m30();
            int[] pull = drawPose(qa, mc, start, 2.70, "pull-back");
            float wonAt = hmi.item.m30();
            AnimationBlockInspectChecks.press(key);
            int[] hit = drawPose(qa, mc, start, 3.15, "impact");
            int[] exit = drawPose(qa, mc, start, 3.58, "withdraw");
            int[] recovered = drawPose(qa, mc, start, 4.65, "recovered");
            qa.require(
                rivalPull - reachedAt > .055f && rivalPull - playerPull > .10f
                    && rivalResists - playerPull > .055f
                    && rivalResists - wonAt > .15f,
                "the doll rocks right, left, right, then left as the player wins, x positions="
                    + Arrays.toString(new float[] { reachedAt, rivalPull, playerPull, rivalResists, wonAt }));
            qa.require(
                violetPixels(reach, mc, true) > violetPixels(raised, mc, true) + 30
                    && violetPixels(recovered, mc, true) <= violetPixels(raised, mc, true),
                "the violet rift opens to the right and disappears after withdrawal");
            checkSideEntry(qa, mc, raised, reach);
            int lensLeft = mc.displayWidth * 11 / 20;
            int lensRight = mc.displayWidth * 17 / 20;
            qa.require(
                violetPixels(hit, mc, lensLeft, lensRight) > 10 + Math
                    .max(violetPixels(pull, mc, lensLeft, lensRight), violetPixels(exit, mc, lensLeft, lensRight)),
                "the impact crack appears around the right-side strike point and then fades");
            qa.require(
                !Arrays.equals(pull, hit) && !Arrays.equals(hit, exit),
                "impact and withdrawal produce distinct frames");
            qa.require(!actions.playing(CLIP), "the original inspection completes despite a repeated key press");
            qa.require(
                player.getHeldItem() == stack && ItemStack.areItemStacksEqual(stack, saved)
                    && player.isSwingInProgress == swinging
                    && !player.isUsingItem(),
                "inspection preserves the real doll, count, NBT and gameplay state");

            AnimationBlockInspectChecks.press(key);
            qa.require(actions.playing(CLIP), "the inspection can replay after recovery");
            player.inventory.currentItem = nextSlot;
            qa.equip(mc, stack.copy());
            qa.tickPlayer();
            qa.require(!actions.playing(CLIP), "changing hotbar slots cancels inspection even with the same doll");
        } finally {
            MinecraftForgeClient.registerItemRenderer(item, nativeRenderer);
        }
    }

    private static void checkSideEntry(AnimationClientChecks qa, Minecraft mc, int[] raised, int[] reach) {
        int width = mc.displayWidth;
        int edge = width * 19 / 20;
        qa.require(
            violetPixels(reach, mc, edge, width) > violetPixels(raised, mc, edge, width) + 10,
            "the rift is clipped by the right edge of the frame");

        // The default GTNL skin has a saturated red sleeve and a warm pink wrist stripe.
        int innerSleeve = sleevePixels(reach, mc, width * 3 / 5, width * 7 / 10);
        int middleSleeve = sleevePixels(reach, mc, width * 3 / 4, width * 17 / 20);
        int outerSleeve = sleevePixels(reach, mc, width * 9 / 10, width);
        qa.require(
            innerSleeve > 50 && middleSleeve > 50 && outerSleeve > 50,
            "the sleeve reaches sideways from the right edge toward the doll, visible pixels="
                + Arrays.toString(new int[] { outerSleeve, middleSleeve, innerSleeve }));
    }

    private static int[] drawPose(AnimationClientChecks qa, Minecraft mc, int start, double seconds, String frame)
        throws Exception {
        double ticks = seconds * 20;
        mc.thePlayer.ticksExisted = start + (int) ticks;
        return qa.draw(mc, "doll-inspect-" + frame, (float) (ticks % 1));
    }

    private static int violetPixels(int[] frame, Minecraft mc, boolean right) {
        int width = mc.displayWidth;
        return violetPixels(frame, mc, right ? width / 2 : 0, right ? width : width / 2);
    }

    private static int violetPixels(int[] frame, Minecraft mc, int fromX, int toX) {
        int width = mc.displayWidth;
        int count = 0;
        for (int y = 0; y < mc.displayHeight / 2; y++) {
            for (int x = fromX; x < toX; x++) {
                int color = frame[y * width + x];
                int red = color >> 16 & 255;
                int green = color >> 8 & 255;
                int blue = color & 255;
                if (red > 90 && blue - red > 12 && red - green > 16) count++;
            }
        }
        return count;
    }

    private static int sleevePixels(int[] frame, Minecraft mc, int fromX, int toX) {
        int width = mc.displayWidth;
        int count = 0;
        for (int y = 0; y < mc.displayHeight * 2 / 3; y++) {
            for (int x = fromX; x < toX; x++) {
                int color = frame[y * width + x];
                int red = color >> 16 & 255;
                int green = color >> 8 & 255;
                int blue = color & 255;
                if (red > 100 && red - green > 26 && red - blue > 16) count++;
            }
        }
        return count;
    }

    private static final class DollRendererProbe implements IItemRenderer {

        private final IItemRenderer delegate;
        private int renders;
        private ItemStack rendered;

        private DollRendererProbe(IItemRenderer delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
            return delegate.handleRenderType(stack, type);
        }

        @Override
        public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
            return delegate.shouldUseRenderHelper(type, stack, helper);
        }

        @Override
        public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
            if (type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
                renders++;
                rendered = stack;
            }
            delegate.renderItem(type, stack, data);
        }
    }
}
