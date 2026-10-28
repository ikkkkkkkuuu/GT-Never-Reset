package com.xyp.gtneverreset.common.block.blocks.tile;

/** Flux Networks point port: delivers GT EU packets using the owner's GTNH wireless balance. */
public class TileEntityFluxPoint extends TileEntityFluxConnector {

    @Override
    public boolean isPlug() {
        return false;
    }
}
