//
// Copyright (C) 2005-2011 Cleversafe, Inc. All rights reserved.
//
// Contact Information:
// Cleversafe, Inc.
// 222 South Riverside Plaza
// Suite 1700
// Chicago, IL 60606, USA
//
// licensing@cleversafe.com
//
// END-OF-HEADER
//
// -----------------------
// @author: rveitch
//
// Date: Feb 11, 2014
// ---------------------

package com.cleversafe.og.scheduling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.cleversafe.og.util.distribution.Distribution;
import com.google.common.util.concurrent.Uninterruptibles;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class RequestRateSchedulerTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private Distribution distribution;

  @Before
  public void before() {
    this.distribution = mock(Distribution.class);
    when(this.distribution.nextSample()).thenReturn(10.0);
  }

  @DataProvider
  public static Object[][] provideInvalidRequestRateScheduler() {
    final Distribution distribution = mock(Distribution.class);
    final TimeUnit unit = TimeUnit.NANOSECONDS;
    return new Object[][] { {null, unit, 0.0, unit, NullPointerException.class},
        {distribution, null, 0.0, unit, NullPointerException.class},
        {distribution, unit, -1.0, unit, IllegalArgumentException.class},
        {distribution, unit, 0.0, null, NullPointerException.class}};
  }

  @Test
  @UseDataProvider("provideInvalidRequestRateScheduler")
  public void invalidRequestRateScheduler(final Distribution distribution, final TimeUnit unit,
      final double rampup, final TimeUnit rampupUnit, final Class<Exception> expectedException) {
    this.thrown.expect(expectedException);
    new RequestRateScheduler(distribution, unit, rampup, rampupUnit);
  }

  @Test
  public void requestRateScheduler() {
    final RequestRateScheduler scheduler =
        new RequestRateScheduler(this.distribution, TimeUnit.SECONDS, 0.0, TimeUnit.NANOSECONDS);
    for (int i = 0; i < 5; i++) {
      final long timestampStart = System.nanoTime();
      scheduler.waitForNext();
      final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart);
      // 10 per second -> 1 per 100 millis
      assertThat(duration, both(greaterThan(50L)).and(lessThan(150L)));
    }
  }

  @Test
  public void interruptedSchedulerThread() {
    final RequestRateScheduler scheduler =
        new RequestRateScheduler(this.distribution, TimeUnit.DAYS, 0.0, TimeUnit.NANOSECONDS);
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
