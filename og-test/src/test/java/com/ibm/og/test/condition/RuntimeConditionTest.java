/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.test.condition;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import java.util.concurrent.TimeUnit;

import com.ibm.og.test.LoadTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.google.common.util.concurrent.Uninterruptibles;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class RuntimeConditionTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @DataProvider
  public static Object[][] provideInvalidRuntimeCondition() {
    final LoadTest test = mock(LoadTest.class);
    final TimeUnit unit = TimeUnit.SECONDS;
    return new Object[][] {{null, 1.0, unit, NullPointerException.class},
        {test, -1.0, unit, IllegalArgumentException.class},
        {test, 0.0, unit, IllegalArgumentException.class},
        {test, 1.0, null, NullPointerException.class}};
  }

  @Test
  @UseDataProvider("provideInvalidRuntimeCondition")
  public void invalidRuntimeCondition(final LoadTest test, final double runtime,
      final TimeUnit unit, final Class<Exception> expectedException) {
    this.thrown.expect(expectedException);
    new RuntimeCondition(test, runtime, unit, false);
  }

  @Test
  public void runtimeCondition() {
    final RuntimeCondition condition =
        new RuntimeCondition(mock(LoadTest.class), 50, TimeUnit.MILLISECONDS, false);

    assertThat(condition.isTriggered(), is(false));
    Uninterruptibles.sleepUninterruptibly(25, TimeUnit.MILLISECONDS);
    assertThat(condition.isTriggered(), is(false));
    Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
    assertThat(condition.isTriggered(), is(true));
  }
}
