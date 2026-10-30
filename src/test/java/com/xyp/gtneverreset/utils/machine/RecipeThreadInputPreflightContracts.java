package com.xyp.gtneverreset.utils.machine;

import appeng.api.config.PowerMultiplier;

public final class RecipeThreadInputPreflightContracts {

    public static void main(String[] args) {
        double previous = PowerMultiplier.CONFIG.multiplier;
        int checked = 0;
        int reproduced = 0;
        try {
            for (double multiplier : new double[] { 0.01, 0.1, 0.3, 0.7, 1, 1.1, 2 }) {
                PowerMultiplier.CONFIG.multiplier = multiplier;
                for (long requested = 64; requested < 100000; requested += 64) {
                    // EnergyGridCache's infinite-store branch returns the multiplied request unchanged.
                    double returned = PowerMultiplier.CONFIG.divide(PowerMultiplier.CONFIG.multiply(requested));
                    if (returned < requested) reproduced++;
                    require(RecipeThreadInputPreflight.hasSufficientEnergy(returned, requested), "infinite grid");
                    require(!RecipeThreadInputPreflight.hasSufficientEnergy(requested - 1, requested), "missing unit");
                    require(!RecipeThreadInputPreflight.hasSufficientEnergy(requested - 0.01, requested), "shortfall");
                    checked++;
                }
            }
            require(reproduced > 0, "original strict comparison must fail for some quantities");
            require(!RecipeThreadInputPreflight.hasSufficientEnergy(Double.NaN, 64), "invalid energy");
            require(!RecipeThreadInputPreflight.hasSufficientEnergy(0, 64), "empty grid");
            require(!RecipeThreadInputPreflight.hasSufficientEnergy(-1, 64), "negative energy");
            long large = 900L * Integer.MAX_VALUE;
            require(
                RecipeThreadInputPreflight.hasSufficientEnergy(Math.nextDown((double) large), large),
                "large rounding");
            require(!RecipeThreadInputPreflight.hasSufficientEnergy(large - 1, large), "large shortfall");
            System.out.println(
                "ME preflight energy contracts passed: " + checked
                    + " quantities; "
                    + reproduced
                    + " original false failures reproduced");
        } finally {
            PowerMultiplier.CONFIG.multiplier = previous;
        }
    }

    private static void require(boolean condition, String scenario) {
        if (!condition) throw new AssertionError(scenario);
    }
}
