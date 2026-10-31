package com.xyp.gtneverreset.common.wireless;

import java.util.UUID;

public interface IWirelessPowerReceiver {

    UUID gtnr$powerConnection();

    void gtnr$setPowerConnection(UUID connection);
}
