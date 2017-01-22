/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.scheduling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

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

  @DataProvider
  public static Object[][] provideRequestsPerSecond() {
    return new Object[][] {{1.0, TimeUnit.SECONDS, 1.0}, {1.5, TimeUnit.SECONDS, 1.5},
        {1.5, TimeUnit.MILLISECONDS, 1500.0}, {1.5, TimeUnit.MINUTES, 0.025},
        {100.0, TimeUnit.DAYS, 0.0011574}};

  }

  @Test
  @UseDataProvider("provideRequestsPerSecond")
  public void requestsPerSecond(final double rate, final TimeUnit unit,
      final double expectedRequestsPerSecond) {
    // constructor values don't matter here, just need an instance to test the public method
    final RequestRateScheduler s =
        new RequestRateScheduler(1.0, TimeUnit.SECONDS, 0.0, TimeUnit.SECONDS);

    assertThat(s.requestsPerSecond(rate, unit),
        closeTo(expectedRequestsPerSecond, Math.pow(0.1, 6)));
  }
}
