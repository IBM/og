/* Copyright (c) IBM Corporation 2020. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.ibm.og.scheduling.customratelimiters;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;

import static java.util.concurrent.TimeUnit.MICROSECONDS;

/**
 * This class implements a stopwatch that sleeps an amount of time based on a Poisson process around
 * a mean time. It is a generalized copy of the Guava RateLimiter.SleepingStopwatch class, pulling it out
 * of the RateLimiter class for general use.
 * @since   1.10.0
 */
abstract class SleepingStopwatch {
    /**
     * Constructor for use by subclasses.
     */
    protected SleepingStopwatch() {
    }

    /*
     * We always hold the mutex when calling this. TODO(cpovirk): Is that important? Perhaps we need
     * to guarantee that each call to reserveEarliestAvailable, etc. sees a value >= the previous?
     * Also, is it OK that we don't hold the mutex when sleeping?
     */
    abstract long readMicros();

    /**
     * Sleep for a given amount of time, based on a PoissonProcess.
     *
     * @param micros Mean microseconds to sleep.
     */
    abstract void sleepMicrosUninterruptibly(long micros);

    /**
     * Create a new SleepingStopwatch from the system timer.
     *
     * @return Newly created SleepingStopwatch.
     */
    public static SleepingStopwatch createFromSystemTimer() {
        return new SleepingStopwatch() {
            final Stopwatch stopwatch = Stopwatch.createStarted();

            @Override
            protected long readMicros() {
                return stopwatch.elapsed(MICROSECONDS);
            }

            @Override
            protected void sleepMicrosUninterruptibly(long micros) {
                if (micros > 0) {
                    Uninterruptibles.sleepUninterruptibly(micros, MICROSECONDS);
                }
            }
        };
    }
}