/* Copyright (c) IBM Corporation 2020. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.ibm.og.scheduling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import com.ibm.og.scheduling.customratelimiters.PoissonSmoothRateLimiter;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import java.util.concurrent.TimeUnit;

@RunWith(DataProviderRunner.class)
public class PoissonRequestRateSchedulerIT {

    @DataProvider
    public static Object[][] providePoissonRequestRateScheduler() {

        return new Object[][] {{1000, TimeUnit.SECONDS, 0.0, TimeUnit.SECONDS, 60000, 60000},
                {10, TimeUnit.SECONDS, 0.0, TimeUnit.SECONDS, 100, 10000},
                {10000, TimeUnit.MINUTES, 0.0, TimeUnit.SECONDS, 10000, 60000}};
    }

    @Test
    @UseDataProvider("providePoissonRequestRateScheduler")
    @Ignore
    public void poissonRequestRateScheduler(final double rate, final TimeUnit unit, final double rampup,
                                                    final TimeUnit rampupUnit, final long operations, final long expectedMillis) {

        final double percentError = 0.10;

        final long start = System.nanoTime();
        final Scheduler scheduler = new PoissonRequestRateScheduler(rate, unit, rampup, rampupUnit);

        // Turn on statistics.

        PoissonSmoothRateLimiter.enableTestStatistics();

        for (int i = 0; i < operations; i++) {
            scheduler.schedule();
        }
        final long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        final long error = (long) (expectedMillis * percentError);

        // Turn off statistics and print them.

        PoissonSmoothRateLimiter.disableTestStatistics();
        PoissonSmoothRateLimiter.printTestStatistics();

        assertThat(millis,
                both(greaterThan(expectedMillis - error)).and(lessThan(expectedMillis + error)));
    }
}
