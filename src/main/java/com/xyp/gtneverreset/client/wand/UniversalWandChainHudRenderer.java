package com.xyp.gtneverreset.client.wand;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import com.xyp.gtneverreset.common.item.items.ItemUniversalWand;
import com.xyp.gtneverreset.common.wand.UniversalWandMiningHandler;
import com.xyp.gtneverreset.common.wand.UniversalWandSearch;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public final class UniversalWandChainHudRenderer {

    private int count;
    private long nextUpdate;
    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;
    private int lastZ = Integer.MIN_VALUE;
    private int lastRadius;
    private int lastMax;
    private boolean lastEnhanced;
    private boolean lastSmallChain;
    private Object lastWorld;
    private Block lastBlock;
    private int lastMetadata;
    private UniversalWandSearch search;
    private boolean refreshing;
    private int lastTick = -1;

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Text event) {
        Minecraft mc = Minecraft.getMinecraft();
        ItemStack stack = mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
        if (stack == null || !(stack.getItem() instanceof ItemUniversalWand)
            || !UniversalWandKeyHandler.isChainKeyHeld()
            || mc.currentScreen != null
            || !mc.inGameHasFocus) {
            search = null;
            lastWorld = null;
            return;
        }
        boolean enhanced = ItemUniversalWand.isEnhancedChain(stack);
        long now = System.nanoTime();
        int targetX = mc.objectMouseOver == null ? Integer.MIN_VALUE : mc.objectMouseOver.blockX;
        int targetY = mc.objectMouseOver == null ? Integer.MIN_VALUE : mc.objectMouseOver.blockY;
        int targetZ = mc.objectMouseOver == null ? Integer.MIN_VALUE : mc.objectMouseOver.blockZ;
        int radius = ItemUniversalWand.getEnhancedChainRadius(stack);
        int max = ItemUniversalWand.getMaxMiningCount(stack);
        boolean smallChain = ItemUniversalWand.isGtSmallChainEnabled(stack);
        boolean blockHit = mc.objectMouseOver != null
            && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
        Block block = blockHit ? mc.theWorld.getBlock(targetX, targetY, targetZ) : null;
        int metadata = blockHit ? mc.theWorld.getBlockMetadata(targetX, targetY, targetZ) : 0;
        boolean changed = targetX != lastX || targetY != lastY
            || targetZ != lastZ
            || enhanced != lastEnhanced
            || radius != lastRadius
            || max != lastMax
            || smallChain != lastSmallChain
            || block != lastBlock
            || metadata != lastMetadata
            || mc.theWorld != lastWorld;
        if (changed || (now >= nextUpdate && (search == null || search.isFinished()))) {
            refreshing = !changed && search != null;
            search = UniversalWandMiningHandler.createPreviewSearch(mc.theWorld, mc.objectMouseOver, stack);
            if (!refreshing || search == null) count = search == null ? 0 : 1;
            lastX = targetX;
            lastY = targetY;
            lastZ = targetZ;
            lastEnhanced = enhanced;
            lastRadius = radius;
            lastMax = max;
            lastSmallChain = smallChain;
            lastWorld = mc.theWorld;
            lastBlock = block;
            lastMetadata = metadata;
            nextUpdate = now + 500000000L;
        }
        if (search != null && lastTick != mc.thePlayer.ticksExisted) {
            search.advance(4096, (x, y, z) -> {});
            if (!refreshing || search.isFinished()) count = 1 + search.getFound();
            lastTick = mc.thePlayer.ticksExisted;
        }
        String title = "Ultimine: "
            // #tr gtneverreset.hud.wand.normal
            // # Normal chain
            // # zh_CN 普通连锁
            // #tr gtneverreset.hud.wand.enhanced
            // # Enhanced chain
            // # zh_CN 增强连锁
            + StatCollector
                .translateToLocal(enhanced ? "gtneverreset.hud.wand.enhanced" : "gtneverreset.hud.wand.normal");
        String countText = count >= UniversalWandMiningHandler.PREVIEW_MAX_BLOCKS ? "65536+++" : String.valueOf(count);
        // #tr gtneverreset.hud.wand.count
        // # Mining count:
        // # zh_CN 挖掘数量:
        String amount = StatCollector.translateToLocal("gtneverreset.hud.wand.count") + " " + countText;
        int color = enhanced ? 0x43DDE9 : 0x59BB62;
        int width = Math.max(mc.fontRenderer.getStringWidth(title), mc.fontRenderer.getStringWidth(amount)) + 16;
        Gui.drawRect(3, 0, width + 3, 39, 0x99302A28);
        Gui.drawRect(3, 0, 5, 39, 0xFF000000 | color);
        Gui.drawRect(5, 21, width + 3, 22, 0x66555555);
        mc.fontRenderer.drawStringWithShadow(title, 11, 6, color);
        mc.fontRenderer.drawStringWithShadow(amount, 11, 27, 0xFFFFFF);
    }
}
