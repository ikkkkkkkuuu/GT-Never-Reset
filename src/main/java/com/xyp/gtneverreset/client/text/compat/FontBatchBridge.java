package com.xyp.gtneverreset.client.text.compat;

/** Optional font-batch boundary without linking callers to Angelica classes. */
@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public interface FontBatchBridge {

    int gtnr$suspendBatch();

    void gtnr$resumeBatch(int depth);

    void gtnr$flushBatch();
}
