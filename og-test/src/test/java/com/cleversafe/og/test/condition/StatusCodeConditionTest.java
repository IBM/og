/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.test.condition;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.http.Bodies;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.test.LoadTest;
import com.cleversafe.og.api.Operation;
import com.cleversafe.og.util.Pair;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class StatusCodeConditionTest {
  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @DataProvider
  public static Object[][] provideInvalidStatusCodeCondition() {
    final Operation operation = Operation.WRITE;
    final LoadTest test = mock(LoadTest.class);
    final Statistics stats = new Statistics();
    return new Object[][] {{null, 200, 1, test, stats, NullPointerException.class},
        {operation, -1, 1, test, stats, IllegalArgumentException.class},
        {operation, 0, 1, test, stats, IllegalArgumentException.class},
        {operation, 99, 1, test, stats, IllegalArgumentException.class},
        {operation, 600, 1, test, stats, IllegalArgumentException.class},
        {operation, 200, -1, test, stats, IllegalArgumentException.class},
        {operation, 200, 0, test, stats, IllegalArgumentException.class},
        {operation, 200, 1, null, stats, NullPointerException.class},
        {operation, 200, 1, test, null, NullPointerException.class},};
  }

  @Test
  @UseDataProvider("provideInvalidStatusCodeCondition")
  public void invalidStatusCodeCondition(final Operation operation, final int statusCode,
      final long thresholdValue, final LoadTest test, final Statistics stats,
      final Class<Exception> expectedException) {
    this.thrown.expect(expectedException);
    new StatusCodeCondition(operation, statusCode, thresholdValue, test, stats, false);
  }

  @Test
  public void statusCodeCondition() {
    final LoadTest test = mock(LoadTest.class);
    final Statistics stats = new Statistics();

    final Request request = mock(Request.class);
    when(request.getMethod()).thenReturn(Method.PUT);
    when(request.getBody()).thenReturn(Bodies.none());
    when(request.getOperation()).thenReturn(Operation.WRITE);

    final Response response = mock(Response.class);
    when(response.getBody()).thenReturn(Bodies.none());
    when(response.getStatusCode()).thenReturn(200);

    final Pair<Request, Response> operation = Pair.of(request, response);

    final StatusCodeCondition condition =
        new StatusCodeCondition(Operation.WRITE, 200, 2, test, stats, false);

    assertThat(condition.isTriggered(), is(false));
    stats.update(operation);
    assertThat(condition.isTriggered(), is(false));
    stats.update(operation);
    assertThat(condition.isTriggered(), is(true));
  }
}
