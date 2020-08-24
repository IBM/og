/* Copyright (c) IBM Corporation 2020. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.ibm.og.scheduling.customratelimiters;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.max;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A ratelimiter that generates events based on a Poisson process.
 *
 * @since 1.10.0
 */
abstract public class PoissonRateLimiter {

    /**
     * Stopwatch to control generation of permits.
     */
    final SleepingStopwatch stopwatch;

    /**
     * Mutex object.
     */
    private volatile Object mutexDoNotUseDirectly;

    /**
     * Mutex operation.
     * @return  Mutex object.
     */
    Object mutex() {
        Object mutex = mutexDoNotUseDirectly;
        if (mutex == null) {
            synchronized (this) {
                mutex = mutexDoNotUseDirectly;
                if (mutex == null) {
                    mutexDoNotUseDirectly = mutex = new Object();
                }
            }
        }
        return mutex;
    }

    /**
     * Default constructor.
     * @param stopwatch Stopwatch to use for controlling generation of permits.
     */
    protected PoissonRateLimiter(SleepingStopwatch stopwatch){
        this.stopwatch = stopwatch;
    }

    /**
     * Create a PoissonRateLimiter for a given mean number of permits per second. The actual number of permits
     * per second will vary based on a Poisson process.
     * @param meanPermitsPerSecond  Mean number of permits per second.
     * @return  New PoissonRateLimiter.
     */
    public static PoissonRateLimiter create(double meanPermitsPerSecond) {
        return create(meanPermitsPerSecond, SleepingStopwatch.createFromSystemTimer());
    }

    /**
     * Create a new PoissonRateLimiter for a given mean number of permits per second and using a
     * PoissonSleepingStopwatch, which controls the generation of permits based on a Poisson process.
     * The number of permits available will vary around the mean number of permits based on the Poisson
     * process.
     * @param meanPermitsPerSecond  Mean number of permits per second.
     * @param stopwatch PoissonSleepingStopwatch to use for generating permits.
     * @return  New PoissonRateLimiter.
     */
    public static PoissonRateLimiter create(double meanPermitsPerSecond, SleepingStopwatch stopwatch) {
        PoissonRateLimiter ratelimiter = new PoissonSmoothRateLimiter.PoissonSmoothBursty(stopwatch, 1);
        ratelimiter.setRate(meanPermitsPerSecond);
        return ratelimiter;
    }

    public static PoissonRateLimiter create(double meanPermitsPerSecond, Duration warmupPeriod) {
        return create(meanPermitsPerSecond, RateLimiterUtils.toNanosSaturated(warmupPeriod), TimeUnit.NANOSECONDS);
    }

    public static PoissonRateLimiter create(double permitsPerSecond, long warmupPeriod, TimeUnit unit) {
        checkArgument(warmupPeriod >= 0, "warmupPeriod must not be negative: %s", warmupPeriod);
        return create(
                permitsPerSecond, warmupPeriod, unit, 3.0, SleepingStopwatch.createFromSystemTimer());
    }

    public static PoissonRateLimiter create(double permitsPerSecond, long warmupPeriod, TimeUnit unit,
                                            double coldFactor, SleepingStopwatch stopwatch) {
        PoissonRateLimiter rateLimiter = new PoissonSmoothRateLimiter.PoissonSmoothWarmingUp
                (stopwatch, warmupPeriod, unit, coldFactor);
        rateLimiter.setRate(permitsPerSecond);
        return rateLimiter;
    }

    public void setRate(double permitsPerSecond) {
        checkArgument(
                permitsPerSecond > 0.0 && !Double.isNaN(permitsPerSecond), "rate must be positive");
        synchronized (mutex()) {
            doSetRate(permitsPerSecond, stopwatch.readMicros());
        }
    }

    abstract void doSetRate(double permitsPerSecond, long nowMicros);

    /**
     * Returns the stable rate (as {@code permits per seconds}) with which this {@code RateLimiter} is
     * configured with. The initial value of this is the same as the {@code permitsPerSecond} argument
     * passed in the factory method that produced this {@code RateLimiter}, and it is only updated
     * after invocations to {@linkplain #setRate}.
     */
    public final double getRate() {
        synchronized (mutex()) {
            return doGetRate();
        }
    }

    abstract double doGetRate();

    public double acquire() {
        return acquire(1);
    }

    private double acquire(int permits) {
        long microsToWait = reserve(permits);
        stopwatch.sleepMicrosUninterruptibly(microsToWait);
        return 1.0 * microsToWait / SECONDS.toMicros(1L);
    }

    private long reserve(int permits) {
        checkPermits(permits);
        synchronized (mutex()) {
            return reserveAndGetWaitLength(permits, stopwatch.readMicros());
        }
    }

    public boolean tryAcquire(Duration timeout) {
        return tryAcquire(1, RateLimiterUtils.toNanosSaturated(timeout), TimeUnit.NANOSECONDS);
    }

    public boolean tryAcquire(long timeout, TimeUnit unit) {
        return tryAcquire(1, timeout, unit);
    }

    public boolean tryAcquire(int permits) {
        return tryAcquire(permits, 0, MICROSECONDS);
    }

    public boolean tryAcquire() {
        return tryAcquire(1, 0, MICROSECONDS);
    }

    public boolean tryAcquire(int permits, Duration timeout) {
        return tryAcquire(permits, RateLimiterUtils.toNanosSaturated(timeout), TimeUnit.NANOSECONDS);
    }

    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
        long timeoutMicros = max(unit.toMicros(timeout), 0);
        checkPermits(permits);
        long microsToWait;
        synchronized (mutex()) {
            long nowMicros = stopwatch.readMicros();
            if (!canAcquire(nowMicros, timeoutMicros)) {
                return false;
            } else {
                microsToWait = reserveAndGetWaitLength(permits, nowMicros);
            }
        }
        stopwatch.sleepMicrosUninterruptibly(microsToWait);
        return true;
    }

    private boolean canAcquire(long nowMicros, long timeoutMicros) {
        return queryEarliestAvailable(nowMicros) - timeoutMicros <= nowMicros;
    }

    final long reserveAndGetWaitLength(int permits, long nowMicros) {
        long momentAvailable = reserveEarliestAvailable(permits, nowMicros);
        return max(momentAvailable - nowMicros, 0);
    }

    abstract long queryEarliestAvailable(long nowMicros);

    /**
     * Reserves the requested number of permits and returns the time that those permits can be used
     * (with one caveat).
     *
     * @return the time that the permits may be used, or, if the permits may be used immediately, an
     *     arbitrary past or present time
     */
    abstract long reserveEarliestAvailable(int permits, long nowMicros);

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "RateLimiter[stableRate=%3.1fqps]", getRate());
    }

    /**
     * Verify that the number of permits is valid. It will throw an exception if the number of permits is <= 0.
     * @param permits   Number of permits to set.
     */
    private static void checkPermits(int permits) {
        checkArgument(permits > 0, "Requested permits (%s) must be positive", permits);
    }


}
