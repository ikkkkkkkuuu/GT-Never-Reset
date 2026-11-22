package com.xyp.gtneverreset.common.packet;

public final class GenerateProjectorBatchPacket extends GenerateProjectorPacket {

    public GenerateProjectorBatchPacket() {
        this(0);
    }

    public GenerateProjectorBatchPacket(int location) {
        super(location, true);
    }
}
