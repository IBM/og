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
// Date: Feb 11, 2014
// ---------------------

package com.cleversafe.og.scheduling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.util.distribution.Distribution;
import com.google.common.util.concurrent.Uninterruptibles;

public class RequestRateSchedulerTest
{
   private Distribution mockDistribution;

   @Before
   public void before()
   {
      this.mockDistribution = mock(Distribution.class);
      when(this.mockDistribution.nextSample()).thenReturn(10.0);
   }

   @Test(expected = NullPointerException.class)
   public void nullDistribution()
   {
      new RequestRateScheduler(null, TimeUnit.MILLISECONDS);
   }

   @Test(expected = NullPointerException.class)
   public void nullTimeUnit()
   {
      new RequestRateScheduler(this.mockDistribution, null);
   }

   @Test
   public void requestRateScheduler()
   {
      final RequestRateScheduler scheduler =
            new RequestRateScheduler(this.mockDistribution, TimeUnit.SECONDS);
      for (int i = 0; i < 5; i++)
      {
         final long timestampStart = System.nanoTime();
         scheduler.waitForNext();
         final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart);
         // 10 per second -> 1 per 100 millis
         assertThat(duration, both(greaterThan(50L)).and(lessThan(150L)));
      }
   }

   @Test
   public void interruptedSchedulerThread()
   {
      final RequestRateScheduler scheduler =
            new RequestRateScheduler(this.mockDistribution, TimeUnit.DAYS);
      final Thread t = new Thread(new Runnable()
      {
         @Override
         public void run()
         {
            scheduler.waitForNext();
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
