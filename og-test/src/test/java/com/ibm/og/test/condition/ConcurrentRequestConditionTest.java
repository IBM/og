/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.test.condition;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.ibm.og.api.Method;
import com.ibm.og.api.Request;
import com.ibm.og.api.Response;
import com.ibm.og.http.Bodies;
import com.ibm.og.statistic.Statistics;
import com.ibm.og.test.LoadTest;
import com.ibm.og.api.Operation;
import com.ibm.og.util.Pair;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class ConcurrentRequestConditionTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private LoadTest test;
  private Statistics stats;
  private Request request;
  private Response response;
  private Pair<Request, Response> operation;

  @Before
  public void before() {
    this.test = mock(LoadTest.class);
    this.stats = new Statistics();

    this.request = mock(Request.class);
    when(this.request.getMethod()).thenReturn(Method.PUT);
    when(this.request.getBody()).thenReturn(Bodies.none());
    when(this.request.getOperation()).thenReturn(Operation.WRITE);

    this.response = mock(Response.class);
    when(this.response.getBody()).thenReturn(Bodies.none());
  }

  @DataProvider
  public static Object[][] provideInvalidConcurrentRequestsCondition() {
    final LoadTest test = mock(LoadTest.class);
    final Statistics stats = new Statistics();
    final Operation operation = Operation.WRITE;
    return new Object[][] {{null, 1, test, stats, NullPointerException.class},
        {operation, -1, test, stats, IllegalArgumentException.class},
        {operation, 0, test, stats, IllegalArgumentException.class},
        {operation, 1, null, stats, NullPointerException.class},
        {operation, 1, test, null, NullPointerException.class},};
  }

  @Test
  @UseDataProvider("provideInvalidConcurrentRequestsCondition")
  public void invalidConcurrentRequestsCondition(final Operation operation,
      final long thresholdValue, final LoadTest test, final Statistics stats,
      final Class<Exception> expectedException) {
    this.thrown.expect(expectedException);
    new ConcurrentRequestCondition(operation, thresholdValue, test, stats, false);
  }

  @Test
  public void concurrentRequestsCondition() {
    final ConcurrentRequestCondition condition =
        new ConcurrentRequestCondition(Operation.WRITE, 2, this.test, this.stats, false);

    assertThat(condition.isTriggered(), is(false));
    this.stats.update(this.request);
    assertThat(condition.isTriggered(), is(false));
    this.stats.update(this.request);
    assertThat(condition.isTriggered(), is(true));
  }

  @Test
  public void parentUpdateOverriden() {
    final ConcurrentRequestCondition condition =
        new ConcurrentRequestCondition(Operation.WRITE, 1, this.test, this.stats, false);

    this.stats.update(this.request);
    this.stats.update(this.request);

    condition.update(this.operation);
    verify(this.test, times(0)).stopTest();

    condition.update(this.request);
    verify(this.test, times(1)).stopTest();
  }
}
