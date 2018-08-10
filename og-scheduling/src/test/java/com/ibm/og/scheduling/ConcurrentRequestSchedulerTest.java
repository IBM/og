/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.scheduling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Rule;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.ibm.og.api.Request;
import com.ibm.og.api.Response;
import com.ibm.og.util.Pair;
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
  @Ignore public void invalidConcurrentRequestScheduler(final int concurrentRequests, final double rampup,
      final TimeUnit rampupUnit, final Class<Exception> expectedException) {
    this.thrown.expect(expectedException);
    new ConcurrentRequestScheduler(concurrentRequests, rampup, rampupUnit);
  }

  @Test
  @Ignore public void oneConcurrentRequest() {
    concurrentRequestScheduler(1);
  }

  @Test
  @Ignore public void multipleConcurrentRequests() {
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
        scheduler.complete();
      }
    }).start();

    while (running.get()) {
      scheduler.schedule();
      if (running.get()) {
        count++;
      }
    }
    assertThat(count, is(concurrentRequests));
  }
}
