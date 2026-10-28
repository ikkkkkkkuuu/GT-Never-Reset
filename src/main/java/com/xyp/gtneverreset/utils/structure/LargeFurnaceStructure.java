package com.xyp.gtneverreset.utils.structure;

public final class LargeFurnaceStructure {

    public static final int WIDTH = 11, HEIGHT = 13, LENGTH = 12;
    public static final int OFFSET_X = 5, OFFSET_Y = 11, OFFSET_Z = 0;

    public static String[][] shape() {
        return MultiblockStructureLoader.load("large_furnace");
    }
}
