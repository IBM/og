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
// Date: Jul 11, 2014
// ---------------------

package com.cleversafe.og.scheduling;

import static org.mockito.Mockito.mock;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.util.Pair;
import com.google.common.util.concurrent.Uninterruptibles;

public class ConcurrentRequestSchedulerTest
{
   @Test(expected = IllegalArgumentException.class)
   public void testNegativeConcurrentRequests()
   {
      new ConcurrentRequestScheduler(-1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testZeroConcurrentRequests()
   {
      new ConcurrentRequestScheduler(0);
   }

   @Test
   public void testPositiveConcurrentRequests()
   {
      new ConcurrentRequestScheduler(1);
   }

   @Test
   public void testOneConcurrentRequest()
   {
      testConcurrentRequestScheduler(1);
   }

   @Test
   public void testManyConcurrentRequests()
   {
      testConcurrentRequestScheduler(10);
   }

   private void testConcurrentRequestScheduler(final int concurrentRequests)
   {
      final ConcurrentRequestScheduler s = new ConcurrentRequestScheduler(concurrentRequests);
      int count = 0;
      final int threadWait = 100;
      getRequestThread(s, threadWait).start();

      final long timestampStart = System.nanoTime();
      while (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart) < threadWait / 2)
      {
         count++;
         s.waitForNext();
      }
      Assert.assertEquals(concurrentRequests, count);
   }

   private Thread getRequestThread(final ConcurrentRequestScheduler s, final int threadWait)
   {
      return new Thread(new Runnable()
      {
         @Override
         public void run()
         {
            Uninterruptibles.sleepUninterruptibly(threadWait, TimeUnit.MILLISECONDS);
            final Request request = mock(Request.class);
            final Response response = mock(Response.class);
            s.complete(new Pair<Request, Response>(request, response));
         }
      });
   }

   @Test
   public void testInterruptedSchedulerThread()
   {
      final ConcurrentRequestScheduler s = new ConcurrentRequestScheduler(1);
      final Thread t = new Thread(new Runnable()
      {
         @Override
         public void run()
         {
            s.waitForNext();
         }
      });
      t.start();
      t.interrupt();
      final long timestampStart = System.nanoTime();
      Uninterruptibles.joinUninterruptibly(t);
      final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart);
      Assert.assertTrue(duration < 1000);
   }
}
