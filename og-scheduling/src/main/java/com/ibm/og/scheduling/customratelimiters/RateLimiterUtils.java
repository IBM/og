/* Copyright (c) IBM Corporation 2020. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.ibm.og.scheduling.customratelimiters;

import java.time.Duration;

/**
 * Utulities pulled out of Guava to allow a broader scope.
 *
 * @since 1.10.0
 */
public class RateLimiterUtils {

    /**
     * Convert a duration to nanoseconds. This makes an internal Guava method accessible.
     * See com.google.common.util.concurrent.Internal.
     * @param duration  Duration to convert.
     * @return  Duration converted to nanoseconds.
     */
    static long toNanosSaturated(Duration duration) {
        // Using a try/catch seems lazy, but the catch block will rarely get invoked (except for
        // durations longer than approximately +/- 292 years).
        try {
            return duration.toNanos();
        } catch (ArithmeticException tooBig) {
            return duration.isNegative() ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
    }

    /**
     * Returns the sum of two long values unless it causes an overflow or underflow, in which case
     * return either the Long.MAX_VALUE or Long.MIN_VALUE.
     * See com.google.common.math.LongMath
     * @param a First value to add.
     * @param b Second value to add.
     * @return  Sum of the two longs, or Long.MAX_VALUE for overflow, or Long.MIN_VALUE for underflow.
     */
    public static long saturatedAdd(long a, long b) {
        long naiveSum = a + b;
        if ((a ^ b) < 0 | (a ^ naiveSum) >= 0) {
            // If a and b have different signs or a has the same sign as the result then there was no
            // overflow, return.
            return naiveSum;
        }
        // we did over/under flow, if the sign is negative we should return MAX otherwise MIN
        return Long.MAX_VALUE + ((naiveSum >>> (Long.SIZE - 1)) ^ 1);
    }
}
