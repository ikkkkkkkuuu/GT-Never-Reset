package com.xyp.gtneverreset.utils.machine;

import java.util.UUID;

public interface IWirelessPowerReceiver {

    UUID gtnr$powerConnection();

    void gtnr$setPowerConnection(UUID connection);
}
