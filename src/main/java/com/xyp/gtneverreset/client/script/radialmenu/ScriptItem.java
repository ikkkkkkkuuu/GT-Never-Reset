package com.xyp.gtneverreset.client.script.radialmenu;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ScriptItem {

    public final String id;

    public final int meta;

    public final int count;

    public final String label;

    public final int slot;

    public ScriptItem(String id, int meta, int count, String label, int slot) {
        this.id = id == null ? "" : id;
        this.meta = meta;
        this.count = count;
        this.label = label == null ? this.id : label;
        this.slot = slot;
    }
}
