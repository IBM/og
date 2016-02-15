/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.cleversafe.og.api.Client;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Operation;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.http.HttpRequest;
import com.cleversafe.og.http.HttpResponse;
import com.cleversafe.og.scheduling.ConcurrentRequestScheduler;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.statistic.Counter;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.test.condition.CounterCondition;
import com.cleversafe.og.test.condition.TestCondition;
import com.cleversafe.og.util.Context;
import com.cleversafe.og.util.Pair;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.SettableFuture;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class LoadTestTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private Request request;
  private Response response;
  private RequestManager requestManager;
  private Client client;
  private Scheduler scheduler;
  private LoadTestSubscriberExceptionHandler handler;
  private EventBus eventBus;
  private Statistics stats;
  private LoadTest test;

  @Before
  public void before() throws URISyntaxException {
    this.request = new HttpRequest.Builder(Method.PUT, new URI("http://127.0.0.1"), Operation.WRITE)
        .withContext(Context.X_OG_REQUEST_ID, "1").build();
    this.response = new HttpResponse.Builder().withStatusCode(200)
        .withContext(Context.X_OG_REQUEST_ID, "1").build();

    this.requestManager = mock(RequestManager.class);
    when(this.requestManager.get()).thenReturn(this.request);

    this.client = mock(Client.class);
    final SettableFuture<Response> future = SettableFuture.create();
    future.set(this.response);
    when(this.client.execute(this.request)).thenReturn(future);

    this.scheduler = new ConcurrentRequestScheduler(1, 0.0, TimeUnit.SECONDS);
    this.handler = new LoadTestSubscriberExceptionHandler();
    this.eventBus = new EventBus(this.handler);
    this.stats = new Statistics();
    this.test = new LoadTest(this.requestManager, this.client, this.scheduler, this.eventBus, true);
    this.handler.setLoadTest(this.test);

    final TestCondition condition =
        new CounterCondition(Operation.WRITE, Counter.OPERATIONS, 5, this.test, this.stats);

    this.eventBus.register(this.scheduler);
    this.eventBus.register(this.stats);
    this.eventBus.register(condition);
  }

  @DataProvider
  public static Object[][] provideInvalidLoadTest() {
    final RequestManager requestSupplier = mock(RequestManager.class);
    final Client client = mock(Client.class);
    final Scheduler scheduler = mock(Scheduler.class);
    final EventBus eventBus = mock(EventBus.class);
    return new Object[][] {{null, client, scheduler, eventBus},
        {requestSupplier, null, scheduler, eventBus}, {requestSupplier, client, null, eventBus},
        {requestSupplier, client, scheduler, null}};
  }

  @Test
  @UseDataProvider("provideInvalidLoadTest")
  public void invalidLoadTest(final RequestManager requestManager, final Client client,
      final Scheduler scheduler, final EventBus eventBus) {
    this.thrown.expect(NullPointerException.class);
    new LoadTest(requestManager, client, scheduler, eventBus, true);
  }

  @Test
  public void requestSupplierException() {
    when(this.requestManager.get()).thenThrow(new IllegalStateException());
    assertThat(this.test.call().success, is(false));
    verify(this.client, times(1)).shutdown(true);
  }

  @Test
  public void eventBusSubscriberException() {
    this.eventBus.register(new Object() {
      @Subscribe
      public void consume(final Pair<Request, Response> operation) {
        throw new RuntimeException();
      }
    });
    assertThat(this.test.call().success, is(false));
  }

  @Test
  public void loadTest() {
    assertThat(this.test.call().success, is(true));
    assertThat(this.stats.get(Operation.WRITE, Counter.OPERATIONS), greaterThanOrEqualTo(5L));
    verify(this.client, atLeast(5)).execute(this.request);
    verify(this.client, times(1)).shutdown(true);
  }
}
