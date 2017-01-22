/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.test.condition;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ibm.og.http.Bodies;
import com.ibm.og.statistic.Counter;
import com.ibm.og.statistic.Statistics;
import com.ibm.og.test.LoadTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.ibm.og.api.Method;
import com.ibm.og.api.Request;
import com.ibm.og.api.Response;
import com.ibm.og.api.Operation;
import com.ibm.og.util.Pair;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class CounterConditionTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @DataProvider
  public static Object[][] provideInvalidCounterCondition() {
    final LoadTest test = mock(LoadTest.class);
    final Statistics stats = new Statistics();
    final Operation operation = Operation.WRITE;
    final Counter counter = Counter.BYTES;
    return new Object[][] {{null, counter, 1, test, stats, NullPointerException.class},
        {operation, null, 1, test, stats, NullPointerException.class},
        {operation, counter, -1, test, stats, IllegalArgumentException.class},
        {operation, counter, 0, test, stats, IllegalArgumentException.class},
        {operation, counter, 1, null, stats, NullPointerException.class},
        {operation, counter, 1, test, null, NullPointerException.class},};
  }

  @Test
  @UseDataProvider("provideInvalidCounterCondition")
  public void invalidCounterCondition(final Operation operation, final Counter counter,
      final long thresholdValue, final LoadTest test, final Statistics stats,
      final Class<Exception> expectedException) {
    this.thrown.expect(expectedException);
    new CounterCondition(operation, counter, thresholdValue, test, stats, false);
  }

  @Test
  public void counterCondition() {
    final LoadTest test = mock(LoadTest.class);
    final Statistics stats = new Statistics();

    final Request request = mock(Request.class);
    when(request.getMethod()).thenReturn(Method.PUT);
    when(request.getBody()).thenReturn(Bodies.none());
    when(request.getOperation()).thenReturn(Operation.WRITE);

    final Response response = mock(Response.class);
    when(response.getBody()).thenReturn(Bodies.none());

    final Pair<Request, Response> operation = Pair.of(request, response);

    final CounterCondition condition =
        new CounterCondition(Operation.WRITE, Counter.OPERATIONS, 2, test, stats, false);

    assertThat(condition.isTriggered(), is(false));
    stats.update(operation);
    assertThat(condition.isTriggered(), is(false));
    stats.update(operation);
    assertThat(condition.isTriggered(), is(true));
  }
}
