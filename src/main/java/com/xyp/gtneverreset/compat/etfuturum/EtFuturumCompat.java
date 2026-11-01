package com.xyp.gtneverreset.compat.etfuturum;

import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.GTNeverReset;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Mods;
import gregtech.api.enums.OreDictNames;
import gregtech.api.util.GTOreDictUnificator;

public final class EtFuturumCompat {

    private EtFuturumCompat() {}

    public static void registerBlastFurnaceOre() {
        ItemStack blastFurnace = GameRegistry.findItemStack(Mods.EtFuturumRequiem.ID, "blast_furnace", 1);
        if (blastFurnace == null) {
            GTNeverReset.LOG.warn("skipping craftingBlastFurnace registration: etfuturum:blast_furnace is unavailable");
            return;
        }

        // GT's preInit lookup can run before EFR registers its blocks. This also skips existing entries.
        if (GTOreDictUnificator.registerOre(OreDictNames.craftingBlastFurnace, blastFurnace)) {
            GTNeverReset.LOG.info("registered missing craftingBlastFurnace entry for etfuturum:blast_furnace");
        }
    }
}
