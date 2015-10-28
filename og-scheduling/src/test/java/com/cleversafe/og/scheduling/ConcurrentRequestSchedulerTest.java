/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.scheduling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.util.Pair;
import com.google.common.util.concurrent.Uninterruptibles;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class ConcurrentRequestSchedulerTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @DataProvider
  public static Object[][] provideInvalidConcurrentRequestScheduler() {
    final TimeUnit unit = TimeUnit.SECONDS;
    return new Object[][] {{-1, 0.0, unit, IllegalArgumentException.class},
        {0, 0.0, unit, IllegalArgumentException.class},
        {1, -1.0, unit, IllegalArgumentException.class},
        {1, 0.0, null, NullPointerException.class}};
  }

  @Test
  @UseDataProvider("provideInvalidConcurrentRequestScheduler")
  public void invalidConcurrentRequestScheduler(final int concurrentRequests, final double rampup,
      final TimeUnit rampupUnit, final Class<Exception> expectedException) {
    this.thrown.expect(expectedException);
    new ConcurrentRequestScheduler(concurrentRequests, rampup, rampupUnit);
  }

  @Test
  public void oneConcurrentRequest() {
    concurrentRequestScheduler(1);
  }

  @Test
  public void multipleConcurrentRequests() {
    concurrentRequestScheduler(10);
  }

  private void concurrentRequestScheduler(final int concurrentRequests) {
    final ConcurrentRequestScheduler scheduler =
        new ConcurrentRequestScheduler(concurrentRequests, 0.0, TimeUnit.SECONDS);
    final AtomicBoolean running = new AtomicBoolean(true);
    int count = 0;
    new Thread(new Runnable() {
      @Override
      public void run() {
        Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
        running.set(false);
        scheduler.complete(Pair.of(mock(Request.class), mock(Response.class)));
      }
    }).start();

    while (running.get()) {
      scheduler.waitForNext();
      if (running.get()) {
        count++;
      }
    }
    assertThat(count, is(concurrentRequests));
  }
}
