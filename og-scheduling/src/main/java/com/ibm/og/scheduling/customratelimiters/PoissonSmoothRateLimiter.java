/* Copyright (c) IBM Corporation 2020. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.ibm.og.scheduling.customratelimiters;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.min;
import java.util.concurrent.TimeUnit;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.Random;

/**
 * A smooth-bursty ratelimiter which generates events based on a Poisson process.
 *
 * @since 1.10.0
 */
public abstract class PoissonSmoothRateLimiter extends PoissonRateLimiter {

    /**
     * The currently stored permits.
     */
    double storedPermits;

    /**
     * The maximum number of stored permits.
     */
    double maxPermits;

    /**
     * The interval between two unit requests, at our stable rate. E.g., a stable rate of 5 permits
     * per second has a stable interval of 200ms.
     */
    double meanIntervalMicros;

    /**
     * The time when the next request (no matter its size) will be granted. After granting a request,
     * this is pushed further in the future. Large requests push this further than small requests.
     */
    private long nextFreeTicketMicros = 0L; // could be either in the past or future

    /**
     * Random number generator used for generating the Poisson distribution.
     */
    private Random poissonRandomGenerator = new Random();

    /**
     * Default constructor. Creates a new PoissonSmoothRateLimiter using its own PoissonSleepingStopwatch.
     *
     * @param stopwatch PoissonSleepingStopwatch to use.
     */
    public PoissonSmoothRateLimiter(SleepingStopwatch stopwatch) {
        super(stopwatch);
        poissonRandomGenerator.setSeed(System.currentTimeMillis());
    }

    @Override
    public final void setRate(double permitsPerSecond) {
        checkArgument(
                permitsPerSecond > 0.0 && !Double.isNaN(permitsPerSecond), "rate must be positive");
        synchronized (mutex()) {
            doSetRate(permitsPerSecond, stopwatch.readMicros());
        }
    }

    static final class PoissonSmoothBursty extends PoissonSmoothRateLimiter {

        final double maxBurstSeconds;

        PoissonSmoothBursty(SleepingStopwatch stopwatch, double maxBurstSeconds) {
            super(stopwatch);
            this.maxBurstSeconds = maxBurstSeconds;
        }

        @Override
        void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
            double oldMaxPermits = this.maxPermits;
            maxPermits = maxBurstSeconds * permitsPerSecond;
            if (oldMaxPermits == Double.POSITIVE_INFINITY) {
                // if we don't special-case this, we would get storedPermits == NaN, below
                storedPermits = maxPermits;
            } else {
                storedPermits =
                        (oldMaxPermits == 0.0)
                                ? 0.0 // initial state
                                : storedPermits * maxPermits / oldMaxPermits;
            }
        }


        @Override
        long storedPermitsToWaitTime(double storedPermits, double permitsToTake) {
            return 0L;
        }

        @Override
        double coolDownIntervalMicros() {
            return meanIntervalMicros;
        }
    }

    static final class PoissonSmoothWarmingUp extends PoissonSmoothRateLimiter {
        private final long warmupPeriodMicros;
        /**
         * The slope of the line from the stable interval (when permits == 0), to the cold interval
         * (when permits == maxPermits)
         */
        private double slope;

        private double thresholdPermits;
        private double coldFactor;

        PoissonSmoothWarmingUp(
                SleepingStopwatch stopwatch, long warmupPeriod, TimeUnit timeUnit, double coldFactor) {
            super(stopwatch);
            this.warmupPeriodMicros = timeUnit.toMicros(warmupPeriod);
            this.coldFactor = coldFactor;
        }

        @Override
        void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
            double oldMaxPermits = maxPermits;
            double coldIntervalMicros = stableIntervalMicros * coldFactor;
            thresholdPermits = 0.5 * warmupPeriodMicros / stableIntervalMicros;
            maxPermits =
                    thresholdPermits + 2.0 * warmupPeriodMicros / (stableIntervalMicros + coldIntervalMicros);
            slope = (coldIntervalMicros - stableIntervalMicros) / (maxPermits - thresholdPermits);
            if (oldMaxPermits == Double.POSITIVE_INFINITY) {
                // if we don't special-case this, we would get storedPermits == NaN, below
                storedPermits = 0.0;
            } else {
                storedPermits =
                        (oldMaxPermits == 0.0)
                                ? maxPermits // initial state is cold
                                : storedPermits * maxPermits / oldMaxPermits;
            }
        }

        @Override
        long storedPermitsToWaitTime(double storedPermits, double permitsToTake) {
            double availablePermitsAboveThreshold = storedPermits - thresholdPermits;
            long micros = 0;
            // measuring the integral on the right part of the function (the climbing line)
            if (availablePermitsAboveThreshold > 0.0) {
                double permitsAboveThresholdToTake = min(availablePermitsAboveThreshold, permitsToTake);
                // TODO(cpovirk): Figure out a good name for this variable.
                double length =
                        permitsToTime(availablePermitsAboveThreshold)
                                + permitsToTime(availablePermitsAboveThreshold - permitsAboveThresholdToTake);
                micros = (long) (permitsAboveThresholdToTake * length / 2.0);
                permitsToTake -= permitsAboveThresholdToTake;
            }
            // measuring the integral on the left part of the function (the horizontal line)
            micros += (long) (meanIntervalMicros * permitsToTake);
            return micros;
        }

        private double permitsToTime(double permits) {
            return meanIntervalMicros + permits * slope;
        }

        @Override
        double coolDownIntervalMicros() {
            return warmupPeriodMicros / maxPermits;
        }
    }

    @Override
    final void doSetRate(double permitsPerSecond, long nowMicros) {
        resync(nowMicros);
        double stableIntervalMicros = SECONDS.toMicros(1L) / permitsPerSecond;
        this.meanIntervalMicros = stableIntervalMicros;
        doSetRate(permitsPerSecond, stableIntervalMicros);
    }

    abstract void doSetRate(double permitsPerSecond, double stableIntervalMicros);

    @Override
    final double doGetRate() {
        return SECONDS.toMicros(1L) / meanIntervalMicros;
    }

    @Override
    final long queryEarliestAvailable(long nowMicros) {
        return nextFreeTicketMicros;
    }

    @Override
    final long reserveEarliestAvailable(int requiredPermits, long nowMicros) {
        resync(nowMicros);
        long returnValue = nextFreeTicketMicros;
        double storedPermitsToSpend = min(requiredPermits, this.storedPermits);
        double freshPermits = requiredPermits - storedPermitsToSpend;
        long tempStoredPermitsToWaitTime = storedPermitsToWaitTime(this.storedPermits, storedPermitsToSpend);
        long waitMicros = tempStoredPermitsToWaitTime + (long) (freshPermits * poissonIntervalMicros1(meanIntervalMicros));

// Test Code

        if (enableTestCode)
        {
            if ((waitMicros > 0) && (waitMicros < minWaitMicros)) minWaitMicros = waitMicros;
            if (waitMicros > maxWaitMicros) maxWaitMicros = waitMicros;
            if (waitMicros > 0) {
                statsCount++;
                sumWaitMicros += waitMicros;
                meanMicros = sumWaitMicros / statsCount;
//                System.out.println("waitMicros = " + waitMicros + ", meanWaitMicros = " + meanMicros +
//                        ", minWaitMicros = " + minWaitMicros + ", maxWaitMicros = " + maxWaitMicros);
            }
        }

        this.nextFreeTicketMicros =RateLimiterUtils.saturatedAdd(nextFreeTicketMicros,waitMicros);
        this.storedPermits -=storedPermitsToSpend;
        return returnValue;
}

// Test Code.

    private static boolean enableTestCode = false;
    private static final int statsBucketMultiplier = 3;
    private static long sumWaitMicros;
    private static long meanMicros;
    private static int  statsCount;

    private static long minWaitMicros;
    private static long maxWaitMicros;
    private static long [] bucketCounts;

    /**
     * Enable statistics.
     */
    public static void enableTestStatistics()
    {
        enableTestCode = true;
        sumWaitMicros = 0;
        meanMicros = 0;
        statsCount = 0;
        minWaitMicros = Long.MAX_VALUE;
        maxWaitMicros = 0;
        bucketCounts = new long[statsBucketMultiplier * poissonBuckets + 1];
    }

    /**
     * Disable statistics.
     */
    public static void disableTestStatistics()
    {
        enableTestCode = false;
    }

    /**
     * Print statistics.
     */
    public static void printTestStatistics()
    {
        System.out.println("************************************************");
        System.out.println("PoissonSmoothRateLimiter Statistics:");
        System.out.println("     statsCount = " + statsCount);
        System.out.println("     meanMicros = " + meanMicros);
        System.out.println("     minWaitMicros = " + minWaitMicros);
        System.out.println("     maxWaitMicros = " + maxWaitMicros);
        for(int i = 0; i < statsBucketMultiplier * poissonBuckets + 1; i++)
            System.out.println("     bucketCounts[" + i + "] = " + bucketCounts[i]);
        System.out.println("************************************************");
    }

    /**
     * Number of "buckets" into which the meanInterval is to be divided for determining the Poisson
     * random number.
     */
    private static final int poissonBuckets = 10;

    /**
     * Steps for calculating the Poisson random number.
     */
    private static final int poissonStep = 1;

    /**
     * Find the wait time, based on an approximation of the Poisson distribution. The algorithm is from:
     * https://en.wikipedia.org/wiki/Poisson_distribution#Generating_Poisson-distributed_random_variables,
     * using the algoritm of Junhao, based on Knuth. The meanInterval is divided into {@code poissonBuckets}
     * "buckets". The buckets are used for calculating the random number. This is done since the algorithm
     * tended to create a small spread with the meanInterval in microseconds and 500 for the step. The
     * current values (10 for buckets and 1 for step) seem to produce a better spread of values.
     * @param meanInterval  Mean wait time.
     * @return  Wait time to be used, with a mean about meanInterval.
     */
    private final double poissonIntervalMicros1(final double meanInterval) {

        int lambdaLeft = poissonBuckets;
        double k = 0;
        double p = 1;

        boolean done = false;

        while (!done)
        {
            k = k + 1.0;
            double u = poissonRandomGenerator.nextDouble();
            p = p * u;
            while((p < 1.0) && (lambdaLeft > 0.0))
            {
                if(lambdaLeft > poissonStep)
                {
                    p = p * Math.exp(poissonStep);
                    lambdaLeft = lambdaLeft - poissonStep;
                }
                else
                {
                    p = p * Math.exp(lambdaLeft);
                    lambdaLeft = 0;
                }
            }
            done = (p <= 1.0);
        }

        // If test statistics are enabled, update the number of times the bucket is used. If the
        // bucket is greater than or equal to the last stats bucket count, use the last stat.

        if(enableTestCode)
        {
            if ((int)k - 1 < statsBucketMultiplier * poissonBuckets + 1)
                bucketCounts[(int)k - 1]++;
            else
                bucketCounts[statsBucketMultiplier * poissonBuckets]++;
        }

        // Convert the result (k-1) back to microseconds, and add a random offset within the bucket.

        return (k - 1) * (meanInterval / poissonBuckets);
    }

    /**
     * Translates a specified portion of our currently stored permits which we want to spend/acquire,
     * into a throttling time. Conceptually, this evaluates the integral of the underlying function we
     * use, for the range of [(storedPermits - permitsToTake), storedPermits].
     *
     * <p>This always holds: {@code 0 <= permitsToTake <= storedPermits}
     */
    abstract long storedPermitsToWaitTime(double storedPermits, double permitsToTake);

    /**
     * Returns the number of microseconds during cool down that we have to wait to get a new permit.
     */
    abstract double coolDownIntervalMicros();

    /** Updates {@code storedPermits} and {@code nextFreeTicketMicros} based on the current time. **/

    /**
     * Updates {@code storedPermits} and {@code nextFreeTicketMicros} based on the current time.
     * @param nowMicros Current time.
     */
    void resync(long nowMicros) {
        // if nextFreeTicket is in the past, resync to now
        if (nowMicros > nextFreeTicketMicros) {
            double newPermits = (nowMicros - nextFreeTicketMicros) / coolDownIntervalMicros();
            storedPermits = min(maxPermits, storedPermits + newPermits);
            nextFreeTicketMicros = nowMicros;
        }
    }

}
