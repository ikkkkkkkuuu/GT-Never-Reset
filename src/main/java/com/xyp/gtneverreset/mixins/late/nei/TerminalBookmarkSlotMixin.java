package com.xyp.gtneverreset.mixins.late.nei;

import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.compat.nei.NeiCraftingFluid;
import com.xyp.gtneverreset.compat.nei.TerminalNeiCrafting;

import appeng.api.storage.data.IAEStack;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.lib.vec.Rectangle4i;
import codechicken.nei.ItemsGrid.MouseContext;
import codechicken.nei.bookmark.BookmarksGridSlot;

@Mixin(value = BookmarksGridSlot.class, remap = false)
public abstract class TerminalBookmarkSlotMixin {

    @Unique
    private IAEStack<?> gtneverreset$patternKey;

    @Unique
    private boolean gtneverreset$hasRecipeMarker;

    @Inject(
        method = "drawRecipeMarker",
        at = { @At(
            value = "INVOKE",
            target = "Lcodechicken/nei/bookmark/BookmarksGridSlot;drawHandlerIcon(Lcodechicken/lib/vec/Rectangle4i;)V"),
            @At(
                value = "INVOKE",
                target = "Lcodechicken/nei/bookmark/BookmarksGridSlot;drawMarker(Lcodechicken/lib/vec/Rectangle4i;Ljava/lang/String;ILcodechicken/nei/NEIClientUtils$Alignment;)V") })
    private void rememberRecipeMarker(Rectangle4i rect, CallbackInfo ci) {
        gtneverreset$hasRecipeMarker = true;
    }

    @Inject(method = "afterDraw", at = @At("RETURN"))
    private void drawCraftablePattern(Rectangle4i rect, MouseContext context, CallbackInfo ci) {
        boolean hasRecipeMarker = gtneverreset$hasRecipeMarker;
        gtneverreset$hasRecipeMarker = false;
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiMEMonitorable terminal)) return;
        if (gtneverreset$patternKey == null) {
            var stack = ((BookmarksGridSlot) (Object) this).getItemStack();
            var fluid = NeiCraftingFluid.getFluid(stack);
            gtneverreset$patternKey = fluid == null ? AEItemStack.create(stack) : AEFluidStack.create(fluid);
            if (gtneverreset$patternKey == null) return;
            // Draw just the native pattern icon, including for outputs with no stock.
            gtneverreset$patternKey.setStackSize(1);
            gtneverreset$patternKey.setCraftable(true);
        }
        if (!TerminalNeiCrafting.isCraftable(terminal, gtneverreset$patternKey)) return;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_TEXTURE_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(rect.x + 1, rect.y + 1, 0);
            GL11.glScalef((rect.w - 2) / 16F, (rect.h - 2) / 16F, 1);
            gtneverreset$patternKey.drawOverlayInGui(mc, hasRecipeMarker ? -7 : 0, 0, false, false, true, true);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }
}
