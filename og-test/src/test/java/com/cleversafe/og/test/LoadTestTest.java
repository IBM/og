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
// Date: Jul 10, 2014
// ---------------------

package com.cleversafe.og.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.cleversafe.og.api.Client;
import com.cleversafe.og.api.Metadata;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.OperationManager;
import com.cleversafe.og.api.OperationManagerException;
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
import com.cleversafe.og.util.Operation;
import com.cleversafe.og.util.Pair;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.SettableFuture;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class LoadTestTest
{
   @Rule
   public ExpectedException thrown = ExpectedException.none();
   private Request request;
   private Response response;
   private OperationManager operationManager;
   private Client client;
   private Scheduler scheduler;
   private LoadTestSubscriberExceptionHandler handler;
   private EventBus eventBus;
   private Statistics stats;
   private LoadTest test;

   @Before
   public void before() throws OperationManagerException, URISyntaxException
   {
      this.request = new HttpRequest.Builder(Method.PUT, new URI("http://127.0.0.1"))
            .withMetadata(Metadata.REQUEST_ID, "1")
            .build();
      this.response = new HttpResponse.Builder().withStatusCode(200)
            .withMetadata(Metadata.REQUEST_ID, "1")
            .build();

      this.operationManager = mock(OperationManager.class);
      when(this.operationManager.next()).thenReturn(this.request);

      this.client = mock(Client.class);
      final SettableFuture<Response> future = SettableFuture.create();
      future.set(this.response);
      when(this.client.execute(this.request)).thenReturn(future);

      this.scheduler = new ConcurrentRequestScheduler(1);
      this.handler = new LoadTestSubscriberExceptionHandler();
      this.eventBus = new EventBus(this.handler);
      this.stats = new Statistics();
      this.test =
            new LoadTest(this.operationManager, this.client, this.scheduler, this.eventBus,
                  this.handler);

      final TestCondition condition =
            new CounterCondition(Operation.WRITE, Counter.OPERATIONS, 5, this.test, this.stats);

      this.eventBus.register(this.scheduler);
      this.eventBus.register(this.stats);
      this.eventBus.register(condition);
   }

   @DataProvider
   public static Object[][] provideInvalidLoadTest()
   {
      final OperationManager operationManager = mock(OperationManager.class);
      final Client client = mock(Client.class);
      final Scheduler scheduler = mock(Scheduler.class);
      final EventBus eventBus = mock(EventBus.class);
      final LoadTestSubscriberExceptionHandler handler =
            mock(LoadTestSubscriberExceptionHandler.class);
      return new Object[][]{
            {null, client, scheduler, eventBus, handler},
            {operationManager, null, scheduler, eventBus, handler},
            {operationManager, client, null, eventBus, handler},
            {operationManager, client, scheduler, null, handler},
            {operationManager, client, scheduler, eventBus, null}
      };
   }

   @Test
   @UseDataProvider("provideInvalidLoadTest")
   public void invalidLoadTest(
         final OperationManager operationManager,
         final Client client,
         final Scheduler scheduler,
         final EventBus eventBus,
         final LoadTestSubscriberExceptionHandler handler)
   {
      this.thrown.expect(NullPointerException.class);
      new LoadTest(operationManager, client, scheduler, eventBus, handler);
   }

   @Test
   public void operationManagerException() throws OperationManagerException
   {
      when(this.operationManager.next()).thenThrow(new OperationManagerException());
      assertThat(this.test.call(), is(false));
      verify(this.client, never()).shutdown(true);
   }

   @Test
   public void eventBusSubscriberException()
   {
      this.eventBus.register(new Object()
      {
         @Subscribe
         public void consume(final Pair<Request, Response> operation)
         {
            throw new RuntimeException();
         }
      });
      assertThat(this.test.call(), is(false));
   }

   @Test
   public void loadTest()
   {
      assertThat(this.test.call(), is(true));
      assertThat(this.stats.get(Operation.WRITE, Counter.OPERATIONS), greaterThanOrEqualTo(5L));
      verify(this.client, atLeast(5)).execute(this.request);
      verify(this.client, never()).shutdown(true);
   }
}
