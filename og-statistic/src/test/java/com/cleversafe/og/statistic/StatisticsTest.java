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
// Date: Jun 30, 2014
// ---------------------

package com.cleversafe.og.statistic;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.http.Bodies;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.util.Operation;
import com.cleversafe.og.util.Pair;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class StatisticsTest
{
   @Rule
   public ExpectedException thrown = ExpectedException.none();
   private Statistics stats;
   private Request request;
   private Response response;
   private Pair<Request, Response> operation;

   @Before
   public void before()
   {
      this.stats = new Statistics();
      this.request = mock(Request.class);
      when(this.request.getMethod()).thenReturn(Method.PUT);
      when(this.request.getBody()).thenReturn(Bodies.random(1024));
      this.response = mock(Response.class);
      when(this.response.getStatusCode()).thenReturn(201);
      when(this.response.getBody()).thenReturn(Bodies.none());
      this.operation = Pair.of(this.request, this.response);
   }

   @Test(expected = NullPointerException.class)
   public void updateNullOperation()
   {
      this.stats.update(null);
   }

   @Test
   public void update()
   {
      this.stats.update(this.operation);
      assertAll(Operation.WRITE, 1, 1024, 0, 201, 1);
   }

   @Test
   public void updateMultiple()
   {
      this.stats.update(this.operation);
      this.stats.update(this.operation);
      assertAll(Operation.WRITE, 2, 2048, 0, 201, 2);
   }

   @Test
   public void updateAbort()
   {
      when(this.response.headers()).thenReturn(ImmutableMap.of(Headers.X_OG_ABORTED, ""));
      this.stats.update(this.operation);
      assertAll(Operation.WRITE, 1, 0, 1, 201, 0);
   }

   @Test
   public void updateAbortMultiple()
   {
      when(this.response.headers()).thenReturn(ImmutableMap.of(Headers.X_OG_ABORTED, ""));
      this.stats.update(this.operation);
      this.stats.update(this.operation);
      assertAll(Operation.WRITE, 2, 0, 2, 201, 0);
   }

   @Test
   public void updateReadBytes()
   {
      when(this.request.getMethod()).thenReturn(Method.GET);
      when(this.request.getBody()).thenReturn(Bodies.none());
      when(this.response.getBody()).thenReturn(Bodies.zeroes(1024));
      this.stats.update(this.operation);
      assertAll(Operation.READ, 1, 1024, 0, 201, 1);
   }

   @Test
   public void updateDeleteBytes()
   {
      when(this.request.getMethod()).thenReturn(Method.DELETE);
      when(this.request.getBody()).thenReturn(Bodies.none());
      this.stats.update(this.operation);
      assertAll(Operation.DELETE, 1, 0, 0, 201, 1);
   }

   @Test(expected = NullPointerException.class)
   public void getNullOperation()
   {
      this.stats.get(null, Counter.OPERATIONS);
   }

   @Test(expected = NullPointerException.class)
   public void getNullCounter()
   {
      this.stats.get(Operation.WRITE, null);
   }

   @DataProvider
   public static Object[][] provideInvalidStatusCode()
   {
      final Operation operation = Operation.WRITE;
      return new Object[][]{
            {null, 201, NullPointerException.class},
            {operation, -1, IllegalArgumentException.class},
            {operation, 0, IllegalArgumentException.class},
            {operation, 99, IllegalArgumentException.class},
            {operation, 600, IllegalArgumentException.class},
      };
   }

   @Test
   @UseDataProvider("provideInvalidStatusCode")
   public void invalidStatusCode(
         final Operation operation,
         final int statusCode,
         final Class<Exception> expectedException)
   {
      this.thrown.expect(expectedException);
      this.stats.getStatusCode(operation, statusCode);
   }

   @Test
   public void concurrency() throws InterruptedException
   {
      final int threadCount = 10;
      final int operationCount = 1000;
      final List<Thread> threads = Lists.newArrayList();
      for (int i = 0; i < threadCount; i++)
      {
         threads.add(new Thread(new Runnable()
         {
            @Override
            public void run()
            {
               final Statistics stats = StatisticsTest.this.stats;
               final Pair<Request, Response> operation = StatisticsTest.this.operation;
               for (int j = 0; j < operationCount / threadCount; j++)
               {
                  stats.update(operation);
               }
            }
         }));
      }

      for (final Thread t : threads)
      {
         t.start();
      }

      for (final Thread t : threads)
      {
         t.join();
      }
      assertAll(Operation.WRITE, 1000, 1024 * operationCount, 0, 201, 1000);
   }

   private void assertAll(
         final Operation operation,
         final long opCount,
         final long byteCount,
         final long abortCount,
         final int statusCode,
         final long statusCodeCount)
   {
      assertThat(this.stats.get(operation, Counter.OPERATIONS), is(opCount));
      assertThat(this.stats.get(operation, Counter.BYTES), is(byteCount));
      assertThat(this.stats.get(operation, Counter.ABORTS), is(abortCount));
      assertThat(this.stats.getStatusCode(operation, statusCode), is(statusCodeCount));
      final Iterator<Entry<Integer, Long>> it = this.stats.statusCodeIterator(operation);
      while (it.hasNext())
      {
         final Entry<Integer, Long> e = it.next();
         if (e.getKey() == statusCode)
            assertThat(e.getValue(), is(statusCodeCount));
      }
   }
}
