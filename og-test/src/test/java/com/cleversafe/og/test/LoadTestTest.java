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

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.client.Client;
import com.cleversafe.og.http.HttpRequest;
import com.cleversafe.og.http.HttpResponse;
import com.cleversafe.og.operation.Method;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.operation.manager.OperationManager;
import com.cleversafe.og.operation.manager.OperationManagerException;
import com.cleversafe.og.scheduling.ConcurrentRequestScheduler;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.statistic.Counter;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.test.condition.CounterCondition;
import com.cleversafe.og.test.condition.TestCondition;
import com.cleversafe.og.util.Operation;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.SettableFuture;

public class LoadTestTest
{
   private EventBus eventBus;
   private Request request;
   Response response;
   private OperationManager operationManager;
   private Client client;
   private Scheduler scheduler;
   private Statistics stats;
   private List<TestCondition> testConditions;
   private LoadTest test;

   @Before
   public void before() throws OperationManagerException, URISyntaxException
   {
      this.request = new HttpRequest.Builder(Method.PUT, new URI("http://127.0.0.1")).build();
      this.response = new HttpResponse.Builder().withStatusCode(200).build();

      this.operationManager = mock(OperationManager.class);
      when(this.operationManager.next()).thenReturn(this.request);

      this.client = mock(Client.class);
      final SettableFuture<Response> future = SettableFuture.create();
      future.set(this.response);
      when(this.client.execute(this.request)).thenReturn(future);

      final SettableFuture<Boolean> shutdownFuture = SettableFuture.create();
      shutdownFuture.set(true);
      when(this.client.shutdown(true)).thenReturn(shutdownFuture);

      this.scheduler = new ConcurrentRequestScheduler(1);

      this.stats = new Statistics();

      this.testConditions = new ArrayList<TestCondition>();
      this.testConditions.add(new CounterCondition(Operation.WRITE, Counter.OPERATIONS, 5));

      this.eventBus = new EventBus();
      this.eventBus.register(this.scheduler);
      this.eventBus.register(this.stats);

      this.test =
            new LoadTest(this.eventBus, this.operationManager, this.client, this.scheduler,
                  this.stats, this.testConditions);
   }

   @Test(expected = NullPointerException.class)
   public void testNullEventBus()
   {
      new LoadTest(null, this.operationManager, this.client, this.scheduler, this.stats,
            this.testConditions);
   }

   @Test(expected = NullPointerException.class)
   public void testNullOperationManager()
   {
      new LoadTest(this.eventBus, null, this.client, this.scheduler, this.stats,
            this.testConditions);
   }

   @Test(expected = NullPointerException.class)
   public void testNullClient()
   {
      new LoadTest(this.eventBus, this.operationManager, null, this.scheduler, this.stats,
            this.testConditions);
   }

   @Test(expected = NullPointerException.class)
   public void testNullScheduler()
   {
      new LoadTest(this.eventBus, this.operationManager, this.client, null, this.stats,
            this.testConditions);
   }

   @Test(expected = NullPointerException.class)
   public void testNullStatistics()
   {
      new LoadTest(this.eventBus, this.operationManager, this.client, this.scheduler, null,
            this.testConditions);
   }

   @Test(expected = NullPointerException.class)
   public void testNullTestConditions()
   {
      new LoadTest(this.eventBus, this.operationManager, this.client, this.scheduler, this.stats,
            null);
   }

   @Test
   public void testOperationManagerException() throws OperationManagerException
   {
      when(this.operationManager.next()).thenThrow(new OperationManagerException());
      final boolean success = this.test.runTest();
      Assert.assertFalse(success);
      verify(this.client, atLeast(1)).shutdown(true);
   }

   @Test
   public void testLoadTest()
   {
      final boolean success = this.test.runTest();
      Assert.assertTrue(success);
      Assert.assertTrue(this.stats.get(Operation.WRITE, Counter.OPERATIONS) >= 5);
      verify(this.client, atLeast(5)).execute(this.request);
      verify(this.client, atLeast(1)).shutdown(true);
   }
}
