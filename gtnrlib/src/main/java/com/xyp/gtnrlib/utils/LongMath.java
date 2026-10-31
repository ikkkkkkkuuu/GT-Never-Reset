package com.xyp.gtnrlib.utils;

/**
 * Long integer rounding and saturating arithmetic unavailable in the libraries shipped with Minecraft 1.7.10.
 */
public final class LongMath {

    /**
     * Rounds towards positive infinity without negating or adding to the dividend.
     * Like Java division, zero divisors throw and {@code Long.MIN_VALUE / -1} returns {@code Long.MIN_VALUE}.
     */
    public static long ceilDiv(long dividend, long divisor) {
        long quotient = dividend / divisor;
        return dividend % divisor != 0 && (dividend ^ divisor) >= 0 ? quotient + 1 : quotient;
    }

    public static long saturatedAdd(long a, long b) {
        long naiveSum = a + b;
        if ((a ^ b) < 0 | (a ^ naiveSum) >= 0) {
            // overflow impossible or did not happen
            return naiveSum;
        }
        // we did over/under flow, if the sign is negative we should return MAX otherwise MIN
        return Long.MAX_VALUE + ((naiveSum >>> (Long.SIZE - 1)) ^ 1);
    }

    public static long saturatedSubtract(long a, long b) {
        long naiveDifference = a - b;
        if ((a ^ b) >= 0 | (a ^ naiveDifference) >= 0) {
            // overflow impossible or did not happen
            return naiveDifference;
        }

        return Long.MAX_VALUE + ((naiveDifference >>> (Long.SIZE - 1)) ^ 1);
    }

    public static long saturatedMultiply(long a, long b) {
        int leadingZeros = Long.numberOfLeadingZeros(a) + Long.numberOfLeadingZeros(~a)
            + Long.numberOfLeadingZeros(b)
            + Long.numberOfLeadingZeros(~b);
        if (leadingZeros > Long.SIZE + 1) {
            return a * b;
        }
        // the return value if we will overflow (which we calculate by overflowing a long :) )
        long limit = Long.MAX_VALUE + ((a ^ b) >>> (Long.SIZE - 1));
        if (leadingZeros < Long.SIZE | (a < 0 & b == Long.MIN_VALUE)) {

            return limit;
        }
        long result = a * b;
        if (a == 0 || result / a == b) {
            return result;
        }
        return limit;
    }
}
