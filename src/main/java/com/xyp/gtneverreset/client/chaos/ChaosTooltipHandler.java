package com.xyp.gtneverreset.client.chaos;

import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import com.xyp.gtneverreset.common.crafting.chaos.ChaosLocalization;
import com.xyp.gtneverreset.common.machine.multiblock.BottleInChaos;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ChaosTooltipHandler {

    @SubscribeEvent
    public void onTitle(ItemTooltipEvent event) {
        ItemStack stack = event.itemStack;
        if (BottleInChaos.isPrototype(stack) && !stack.hasDisplayName()) {
            if (!event.toolTip.isEmpty()) event.toolTip.set(0, ChaosLocalization.tr(BottleInChaos.PROTOTYPE_NAME_KEY));
        }
    }
}
