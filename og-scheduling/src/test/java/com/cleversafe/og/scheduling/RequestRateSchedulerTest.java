/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.scheduling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.google.common.util.concurrent.Uninterruptibles;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class RequestRateSchedulerTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @DataProvider
  public static Object[][] provideInvalidRequestRateScheduler() {
    final double rate = 10.0;
    final TimeUnit unit = TimeUnit.NANOSECONDS;
    return new Object[][] {{0.0, unit, 0.0, unit, IllegalArgumentException.class},
        {rate, null, 0.0, unit, NullPointerException.class},
        {rate, unit, -1.0, unit, IllegalArgumentException.class},
        {rate, unit, 0.0, null, NullPointerException.class}};
  }

  @Test
  @UseDataProvider("provideInvalidRequestRateScheduler")
  public void invalidRequestRateScheduler(final double rate, final TimeUnit unit,
      final double rampup, final TimeUnit rampupUnit, final Class<Exception> expectedException) {
    this.thrown.expect(expectedException);
    new RequestRateScheduler(rate, unit, rampup, rampupUnit);
  }

  @Test
  public void interruptedSchedulerThread() {
    final RequestRateScheduler scheduler =
        new RequestRateScheduler(10.0, TimeUnit.DAYS, 0.0, TimeUnit.NANOSECONDS);
    final Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        scheduler.waitForNext();
      }
    });
    t.start();
    t.interrupt();
    final long timestampStart = System.nanoTime();
    Uninterruptibles.joinUninterruptibly(t);
    final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart);
    assertThat(duration, lessThan(1000L));
  }
}
