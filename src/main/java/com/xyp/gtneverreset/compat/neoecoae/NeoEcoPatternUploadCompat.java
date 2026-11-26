package com.xyp.gtneverreset.compat.neoecoae;

import java.util.Iterator;

import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.client.event.GuiScreenEvent;

import com.xyp.gtneverreset.client.gui.QuickEncodingTerminalGui;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class NeoEcoPatternUploadCompat {

    private static final String UPLOAD_BUTTON_CLASS = "cn.dancingsnow.neoecoae.platform.ui.upload.PatternUploadButton";

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void hideUploadButtons(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof QuickEncodingTerminalGui)) return;

        for (Iterator<?> buttons = event.buttonList.iterator(); buttons.hasNext();) {
            if (buttons.next() instanceof GuiButton button && UPLOAD_BUTTON_CLASS.equals(
                button.getClass()
                    .getName())) {
                // Neo ECO also holds these buttons directly for its tooltip handler.
                button.visible = false;
                buttons.remove();
            }
        }
    }
}
