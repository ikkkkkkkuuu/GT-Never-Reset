package com.xyp.gtneverreset.client.wand;

import net.minecraftforge.client.event.RenderPlayerEvent;

import com.xyp.gtneverreset.common.wand.UniversalWandProtectionHandler;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public final class UniversalWandStealthRenderer {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayer(RenderPlayerEvent.Pre event) {
        if (UniversalWandProtectionHandler.isHidden(event.entityPlayer)) event.setCanceled(true);
    }

}
