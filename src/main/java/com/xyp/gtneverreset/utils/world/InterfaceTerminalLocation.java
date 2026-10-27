package com.xyp.gtneverreset.utils.world;

import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.util.DimensionalCoord;

public final class InterfaceTerminalLocation extends DimensionalCoord {

    private final ForgeDirection side;

    public InterfaceTerminalLocation(int x, int y, int z, int dimension, ForgeDirection side) {
        super(x, y, z, dimension);
        this.side = side;
    }

    public ForgeDirection getSide() {
        return side;
    }
}
