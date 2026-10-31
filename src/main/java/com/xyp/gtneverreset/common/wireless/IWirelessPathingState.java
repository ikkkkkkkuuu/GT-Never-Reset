package com.xyp.gtneverreset.common.wireless;

public interface IWirelessPathingState {

    boolean gtnr$isRepathPending();

    /** Counts grid membership and wired connections; excludes frequency-card connections. */
    long gtnr$topologyRevision();

    void gtnr$topologyChanged();
}
