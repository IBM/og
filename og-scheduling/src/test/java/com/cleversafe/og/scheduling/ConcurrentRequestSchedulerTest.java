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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.mock;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.util.Pair;
import com.google.common.util.concurrent.Uninterruptibles;

public class ConcurrentRequestSchedulerTest
{
   @Test(expected = IllegalArgumentException.class)
   public void negativeConcurrentRequests()
   {
      new ConcurrentRequestScheduler(-1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void zeroConcurrentRequests()
   {
      new ConcurrentRequestScheduler(0);
   }

   @Test
   public void oneConcurrentRequest()
   {
      concurrentRequestScheduler(1);
   }

   @Test
   public void multipleConcurrentRequests()
   {
      concurrentRequestScheduler(10);
   }

   private void concurrentRequestScheduler(final int concurrentRequests)
   {
      final ConcurrentRequestScheduler scheduler =
            new ConcurrentRequestScheduler(concurrentRequests);
      int count = 0;
      final int threadWait = 50;
      new Thread(new Runnable()
      {
         @Override
         public void run()
         {
            Uninterruptibles.sleepUninterruptibly(threadWait, TimeUnit.MILLISECONDS);
            scheduler.complete(Pair.of(mock(Request.class), mock(Response.class)));
         }
      }).start();

      final long timestampStart = System.nanoTime();
      while (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart) < threadWait / 2)
      {
         count++;
         scheduler.waitForNext();
      }
      assertThat(count, is(concurrentRequests));
   }

   @Test
   public void interruptedSchedulerThread()
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
      assertThat(duration, lessThan(1000L));
   }
}
