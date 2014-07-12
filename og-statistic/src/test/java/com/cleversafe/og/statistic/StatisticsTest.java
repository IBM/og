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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.operation.Method;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.util.Entities;
import com.cleversafe.og.util.Operation;

public class StatisticsTest
{
   private Statistics stats;
   private Request request;
   private Response response;

   @Before
   public void before()
   {
      this.stats = new Statistics();
      this.request = mock(Request.class);
      when(this.request.getMethod()).thenReturn(Method.PUT);
      when(this.request.getEntity()).thenReturn(Entities.random(1024));
      this.response = mock(Response.class);
      when(this.response.getStatusCode()).thenReturn(201);
      when(this.response.getEntity()).thenReturn(Entities.none());
   }

   @Test(expected = NullPointerException.class)
   public void testUpdateNullRequest()
   {
      this.stats.update(null, this.response);
   }

   @Test(expected = NullPointerException.class)
   public void testUpdateNullResponse()
   {
      this.stats.update(this.request, null);
   }

   @Test
   public void testUpdate()
   {
      this.stats.update(this.request, this.response);
      assertAll(Operation.WRITE, 1, 1024, 0, 201, 1);
   }

   @Test
   public void testUpdateMultiple()
   {
      this.stats.update(this.request, this.response);
      this.stats.update(this.request, this.response);
      assertAll(Operation.WRITE, 2, 2048, 0, 201, 2);
   }

   @Test
   public void testUpdateAbort()
   {
      when(this.response.getMetadata(Metadata.ABORTED)).thenReturn("1");
      this.stats.update(this.request, this.response);
      assertAll(Operation.WRITE, 1, 1024, 1, 201, 0);
   }

   @Test
   public void testUpdateAbortMultiple()
   {
      when(this.response.getMetadata(Metadata.ABORTED)).thenReturn("1");
      this.stats.update(this.request, this.response);
      this.stats.update(this.request, this.response);
      assertAll(Operation.WRITE, 2, 2048, 2, 201, 0);
   }

   @Test
   public void testUpdateReadBytes()
   {
      when(this.request.getMethod()).thenReturn(Method.GET);
      when(this.request.getEntity()).thenReturn(Entities.none());
      when(this.response.getEntity()).thenReturn(Entities.zeroes(1024));
      this.stats.update(this.request, this.response);
      assertAll(Operation.READ, 1, 1024, 0, 201, 1);
   }

   @Test
   public void testDeleteReadBytes()
   {
      when(this.request.getMethod()).thenReturn(Method.DELETE);
      when(this.request.getEntity()).thenReturn(Entities.none());
      this.stats.update(this.request, this.response);
      assertAll(Operation.DELETE, 1, 0, 0, 201, 1);
   }

   @Test
   public void testInitialGetValue()
   {
      for (final Operation o : Operation.values())
      {
         for (final Counter c : Counter.values())
         {
            Assert.assertEquals(0, this.stats.get(o, c));
         }
      }
   }

   @Test(expected = NullPointerException.class)
   public void testGetNullOperation()
   {
      this.stats.get(null, Counter.OPERATIONS);
   }

   @Test(expected = NullPointerException.class)
   public void testGetNullCounter()
   {
      this.stats.get(Operation.WRITE, null);
   }

   @Test
   public void testInitialGetStatusCodeValues()
   {
      for (final Operation o : Operation.values())
      {
         for (int i = 100; i < 600; i++)
         {
            Assert.assertEquals(0, this.stats.getStatusCode(o, i));
         }
      }
   }

   @Test(expected = NullPointerException.class)
   public void testGetStatusCodeNullOperation()
   {
      this.stats.getStatusCode(null, 201);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testGetStatusCodeNegativeSC()
   {
      this.stats.getStatusCode(Operation.WRITE, -1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testGetStatusCodeZeroSC()
   {
      this.stats.getStatusCode(Operation.WRITE, 0);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testGetStatusCodeSmallSC()
   {
      this.stats.getStatusCode(Operation.WRITE, 99);
   }

   @Test
   public void testGetStatusCodeMediumSC()
   {
      this.stats.getStatusCode(Operation.WRITE, 200);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testGetStatusCodeLargeSC()
   {
      this.stats.getStatusCode(Operation.WRITE, 600);
   }

   @Test(expected = UnsupportedOperationException.class)
   public void testStatusCodeIteratorRemove()
   {
      this.stats.update(this.request, this.response);
      final Iterator<Entry<Integer, Long>> it = this.stats.statusCodeIterator(Operation.WRITE);
      it.next();
      it.remove();
   }

   @Test
   public void testConcurrency() throws InterruptedException
   {
      final int threadCount = 10;
      final int operationCount = 1000;
      final List<Thread> threads = new ArrayList<Thread>();
      for (int i = 0; i < threadCount; i++)
      {
         threads.add(new Thread(new Runnable()
         {
            @Override
            public void run()
            {
               final Statistics stats = StatisticsTest.this.stats;
               final Request request = StatisticsTest.this.request;
               final Response response = StatisticsTest.this.response;
               for (int j = 0; j < operationCount / threadCount; j++)
               {
                  stats.update(request, response);
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
         final int opCount,
         final int byteCount,
         final int abortCount,
         final int statusCode,
         final int scCount)
   {
      Assert.assertEquals(opCount, this.stats.get(operation, Counter.OPERATIONS));
      Assert.assertEquals(byteCount, this.stats.get(operation, Counter.BYTES));
      Assert.assertEquals(abortCount, this.stats.get(operation, Counter.ABORTS));
      Assert.assertEquals(scCount, this.stats.getStatusCode(operation, statusCode));
      final Iterator<Entry<Integer, Long>> it = this.stats.statusCodeIterator(operation);
      while (it.hasNext())
      {
         final Entry<Integer, Long> e = it.next();
         if (e.getKey() == statusCode)
            Assert.assertEquals(Long.valueOf(scCount), e.getValue());
      }
   }
}
