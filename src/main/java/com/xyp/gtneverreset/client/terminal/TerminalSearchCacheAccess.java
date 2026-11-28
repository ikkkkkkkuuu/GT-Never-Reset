package com.xyp.gtneverreset.client.terminal;

import java.util.Map;

import appeng.api.storage.data.IAEStack;
import appeng.client.me.ItemRepo;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface TerminalSearchCacheAccess {

    Map<IAEStack<?>, Boolean> gtnr$getSearchCache();

    void gtnr$inheritSearchCache(ItemRepo previousRepo);
}
