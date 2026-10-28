package com.xyp.gtneverreset.common.block.blocks.tile;

/** Flux Networks plug port: accepts GT EU packets and credits the owner's GTNH wireless account. */
public class TileEntityFluxPlug extends TileEntityFluxConnector {

    @Override
    public boolean isPlug() {
        return true;
    }
}
