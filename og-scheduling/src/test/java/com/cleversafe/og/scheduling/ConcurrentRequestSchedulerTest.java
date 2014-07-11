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

import com.cleversafe.og.operation.Response;

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
            try
            {
               TimeUnit.MILLISECONDS.sleep(threadWait);
            }
            catch (final InterruptedException e)
            {
            }
            final Response response = mock(Response.class);
            s.complete(response);
         }
      });
   }
}
