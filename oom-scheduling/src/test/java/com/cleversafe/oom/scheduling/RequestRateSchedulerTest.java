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

package com.cleversafe.oom.scheduling;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.oom.distribution.Distribution;
import com.cleversafe.oom.operation.Operation;

public class RequestRateSchedulerTest
{
   private Distribution mockDistribution;
   private Scheduler<Operation> scheduler;

   @Before
   public void setBefore()
   {
      this.mockDistribution = mock(Distribution.class);
      when(this.mockDistribution.nextSample()).thenReturn(1.0);
      this.scheduler =
            new RequestRateScheduler<Operation>(this.mockDistribution, TimeUnit.MILLISECONDS);
   }

   @Test(expected = NullPointerException.class)
   public void testNullDistribution()
   {
      new RequestRateScheduler<Operation>(null, TimeUnit.MILLISECONDS);
   }

   @Test(expected = NullPointerException.class)
   public void testNullTimeUnit()
   {
      new RequestRateScheduler<Operation>(this.mockDistribution, null);
   }

   @Test
   public void testRequestRateScheduler()
   {
      new RequestRateScheduler<Operation>(this.mockDistribution, TimeUnit.MILLISECONDS);
   }

   @Test
   public void testWaitForNext()
   {
      final List<Long> durations = new ArrayList<Long>();
      for (int i = 0; i < 1000; i++)
      {
         final long beginTime = System.nanoTime();
         this.scheduler.waitForNext();
         final long endTime = System.nanoTime();
         durations.add(endTime - beginTime);
      }

      final long max = Collections.max(durations);
      final long min = Collections.min(durations);
      final long delta = max - min;

      // TODO establish performance criteria
      final long validMax = TimeUnit.MICROSECONDS.toNanos(5000);
      final long validDelta = TimeUnit.MICROSECONDS.toNanos(1500);
      Assert.assertTrue("max exceeded, " + max, max < validMax);
      Assert.assertTrue("validDelta exceeded, " + delta, delta < validDelta);
   }

   @Test
   public void testDelayedWaitForNext() throws InterruptedException
   {
      this.scheduler.waitForNext();
      Thread.sleep(5);

      // waitForNext should return immediately if elapsed time between
      // successive calls is longer than the sleepDuration of the scheduler
      final long beginTime = System.nanoTime();
      this.scheduler.waitForNext();
      final long endTime = System.nanoTime();
      final long delta = endTime - beginTime;

      // TODO establish performance criteria
      final long validDelta = TimeUnit.MICROSECONDS.toNanos(200);
      Assert.assertTrue("validDelta2 exceeded, " + delta, delta < validDelta);
   }
}
