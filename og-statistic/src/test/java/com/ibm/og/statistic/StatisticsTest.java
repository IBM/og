/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.statistic;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map.Entry;

import com.ibm.og.api.RequestTimestamps;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.ibm.og.api.Method;
import com.ibm.og.api.Request;
import com.ibm.og.api.Response;
import com.ibm.og.http.Bodies;
import com.ibm.og.api.Operation;
import com.ibm.og.util.Pair;
import com.ibm.og.util.TestState;
import com.google.common.collect.Lists;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class StatisticsTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private Statistics stats;
  private Request request;
  private Response response;
  private Pair<Request, Response> operation;

  @Before
  public void before() {
    this.stats = new Statistics();
    RequestTimestamps timestamps = new RequestTimestamps();
    timestamps.startMillis = System.currentTimeMillis();
    timestamps.finishMillis = timestamps.startMillis + 17;
    this.request = mock(Request.class);
    when(this.request.getMethod()).thenReturn(Method.PUT);
    when(this.request.getOperation()).thenReturn(Operation.WRITE);
    when(this.request.getBody()).thenReturn(Bodies.random(1024));
    this.response = mock(Response.class);
    when(this.response.getStatusCode()).thenReturn(201);
    when(this.response.getBody()).thenReturn(Bodies.none());
    when(this.response.getRequestTimestamps()).thenReturn(timestamps);
    this.operation = Pair.of(this.request, this.response);
  }

  @Test(expected = NullPointerException.class)
  public void updateRequestNullOperation() {
    this.stats.update((Request) null);
  }

  @Test(expected = NullPointerException.class)
  public void updateStateNullState() {
    this.stats.update((TestState) null);
  }

  @Test
  public void updateState() {
    this.stats.update(this.operation);
    this.stats.update(TestState.STOPPING);
    // valid responses after shutdown should still be counted
    this.stats.update(this.operation);

    assertThat(this.stats.get(Operation.ALL, Counter.OPERATIONS), is(2L));
  }

  @Test
  public void updateState2() {
    this.stats.update(this.operation);
    this.stats.update(TestState.STOPPING);
    // simulate aborted request posting to event bus after client shutdown
    when(this.response.getStatusCode()).thenReturn(599);
    this.stats.update(this.operation);

    // aborted requests after shutdown should not count towards total
    assertThat(this.stats.get(Operation.ALL, Counter.OPERATIONS), is(1L));
  }

  @Test
  public void updateRequest() {
    this.stats.update(this.request);
    assertThat(this.stats.get(Operation.WRITE, Counter.ACTIVE_OPERATIONS), is(1L));
    assertThat(this.stats.get(Operation.ALL, Counter.ACTIVE_OPERATIONS), is(1L));
    this.stats.update(this.operation);
    assertThat(this.stats.get(Operation.WRITE, Counter.ACTIVE_OPERATIONS), is(0L));
    assertThat(this.stats.get(Operation.ALL, Counter.ACTIVE_OPERATIONS), is(0L));
  }

  @Test(expected = NullPointerException.class)
  public void updateNullOperation() {
    this.stats.update((Pair<Request, Response>) null);
  }

  @Test
  public void update() {
    this.stats.update(this.operation);
    assertAll(Operation.WRITE, 1, 1024, 201, 1);
  }

  @Test
  public void updateMultiple() {
    this.stats.update(this.operation);
    this.stats.update(this.operation);
    assertAll(Operation.WRITE, 2, 2048, 201, 2);
  }

  @Test
  public void updateUnsuccessfulStatusCode() {
    this.stats.update(this.operation);
    when(this.response.getStatusCode()).thenReturn(500);
    this.stats.update(this.operation);
    assertAll(Operation.WRITE, 2, 1024, 201, 1);
  }

  @Test
  public void updateReadBytes() {
    when(this.request.getMethod()).thenReturn(Method.GET);
    when(this.request.getOperation()).thenReturn(Operation.READ);
    when(this.request.getBody()).thenReturn(Bodies.none());
    when(this.response.getBody()).thenReturn(Bodies.zeroes(1024));
    this.stats.update(this.operation);
    assertAll(Operation.READ, 1, 1024, 201, 1);
  }

  @Test
  public void updateReadUnsuccessfulStatusCode() {
    when(this.request.getMethod()).thenReturn(Method.GET);
    when(this.request.getOperation()).thenReturn(Operation.READ);
    when(this.request.getBody()).thenReturn(Bodies.none());
    when(this.response.getBody()).thenReturn(Bodies.random(1024));

    this.stats.update(this.operation);
    when(this.response.getStatusCode()).thenReturn(500);
    this.stats.update(this.operation);
    assertAll(Operation.READ, 2, 1024, 201, 1);
  }

  @Test
  public void updateDeleteBytes() {
    when(this.request.getMethod()).thenReturn(Method.DELETE);
    when(this.request.getBody()).thenReturn(Bodies.none());
    when(this.request.getOperation()).thenReturn(Operation.DELETE);
    this.stats.update(this.operation);
    assertAll(Operation.DELETE, 1, 0, 201, 1);
  }

  @Test(expected = NullPointerException.class)
  public void getNullOperation() {
    this.stats.get(null, Counter.OPERATIONS);
  }

  @Test(expected = NullPointerException.class)
  public void getNullCounter() {
    this.stats.get(Operation.WRITE, null);
  }

  @DataProvider
  public static Object[][] provideInvalidStatusCode() {
    final Operation operation = Operation.WRITE;
    return new Object[][] {{null, 201, NullPointerException.class},
        {operation, -1, IllegalArgumentException.class},
        {operation, 0, IllegalArgumentException.class},
        {operation, 99, IllegalArgumentException.class},
        {operation, 600, IllegalArgumentException.class},};
  }

  @Test
  @UseDataProvider("provideInvalidStatusCode")
  public void invalidStatusCode(final Operation operation, final int statusCode,
      final Class<Exception> expectedException) {
    this.thrown.expect(expectedException);
    this.stats.getStatusCode(operation, statusCode);
  }

  @Test
  public void concurrency() throws InterruptedException {
    final int threadCount = 10;
    final int operationCount = 1000;
    final List<Thread> threads = Lists.newArrayList();
    for (int i = 0; i < threadCount; i++) {
      threads.add(new Thread(new Runnable() {
        @Override
        public void run() {
          final Statistics stats = StatisticsTest.this.stats;
          final Pair<Request, Response> operation = StatisticsTest.this.operation;
          for (int j = 0; j < operationCount / threadCount; j++) {
            stats.update(operation);
          }
        }
      }));
    }

    for (final Thread t : threads) {
      t.start();
    }

    for (final Thread t : threads) {
      t.join();
    }
    assertAll(Operation.WRITE, 1000, 1024 * operationCount, 201, 1000);
  }

  private void assertAll(final Operation operation, final long opCount, final long byteCount,
      final int statusCode, final long statusCodeCount) {
    assertThat(this.stats.get(operation, Counter.OPERATIONS), is(opCount));
    assertThat(this.stats.get(operation, Counter.BYTES), is(byteCount));
    assertThat(this.stats.getStatusCode(operation, statusCode), is(statusCodeCount));

    for (final Entry<Integer, Long> e : this.stats.statusCodes(operation).entrySet()) {
      if (e.getKey() == statusCode) {
        assertThat(e.getValue(), is(statusCodeCount));
      }
    }
  }
}
