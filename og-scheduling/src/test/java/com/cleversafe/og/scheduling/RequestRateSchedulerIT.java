/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.scheduling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class RequestRateSchedulerIT {

  @DataProvider
  public static Object[][] provideRequestRateScheduler() {
    // high iops
    // low iops
    // alternate unit
    return new Object[][] {{1000, TimeUnit.SECONDS, 0.0, TimeUnit.SECONDS, 60000, 60000},
        {10, TimeUnit.SECONDS, 0.0, TimeUnit.SECONDS, 100, 10000},
        {10000, TimeUnit.MINUTES, 0.0, TimeUnit.SECONDS, 10000, 60000}};
  }

  @Test
  @UseDataProvider("provideRequestRateScheduler")
  public void requestRateScheduler(final double rate, final TimeUnit unit, final double rampup,
      final TimeUnit rampupUnit, final long operations, final long expectedMillis) {

    final double percentError = 0.05;

    final long start = System.nanoTime();
    final Scheduler scheduler = new RequestRateScheduler(rate, unit, rampup, rampupUnit);
    for (int i = 0; i < operations; i++) {
      scheduler.schedule();
    }
    final long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    final long error = (long) (expectedMillis * percentError);

    assertThat(millis,
        both(greaterThan(expectedMillis - error)).and(lessThan(expectedMillis + error)));
  }
}
