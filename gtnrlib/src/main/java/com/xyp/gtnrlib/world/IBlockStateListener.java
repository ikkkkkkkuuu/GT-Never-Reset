package com.xyp.gtnrlib.world;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;

public interface IBlockStateListener {

    void onBlockChanged(BlockPos pos);
}
