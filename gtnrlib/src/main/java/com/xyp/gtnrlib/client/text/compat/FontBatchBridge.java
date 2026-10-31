package com.xyp.gtnrlib.client.text.compat;

/** Optional font-batch boundary without linking callers to Angelica classes. */
public interface FontBatchBridge {

    int gtnr$suspendBatch();

    void gtnr$resumeBatch(int depth);

    void gtnr$flushBatch();
}
