package com.xyp.gtnrlib.animation;

public enum StructureDeploymentStyle {

    SUMMON,
    REVEAL,
    FLIGHT,
    NONE;

    private static final int MAX_BANDS = 6;
    public static final int REVEAL_FLASH_TICKS = 5;
    public static final int REVEAL_OUTLINE_TICKS = 11;
    public static final int REVEAL_SOLID_TICKS = 19;
    public static final int REVEAL_BAND_TICKS = 3;
    public static final int FLIGHT_TRAVEL_TICKS = 32;
    public static final int FLIGHT_BAND_TICKS = 8;

    public boolean segmented() {
        return this == REVEAL || this == FLIGHT;
    }

    public int bandCount(int length) {
        return segmented() ? Math.max(1, Math.min(MAX_BANDS, (length + 2) / 3)) : 1;
    }

    public int bandReadyTick(int band) {
        return switch (this) {
            case REVEAL -> REVEAL_SOLID_TICKS + band * REVEAL_BAND_TICKS;
            case FLIGHT -> FLIGHT_TRAVEL_TICKS + band * FLIGHT_BAND_TICKS;
            default -> 0;
        };
    }

    public static int bandAt(int coordinate, int min, int max, int bands) {
        return Math.max(0, Math.min(bands - 1, (coordinate - min) * bands / Math.max(1, max - min)));
    }

    public static int bandBoundary(int band, int min, int max, int bands) {
        return min + (band * (max - min) + bands - 1) / bands;
    }
}
