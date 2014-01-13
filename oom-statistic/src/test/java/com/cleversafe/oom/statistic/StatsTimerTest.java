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
// Date: Nov 6, 2013
// ---------------------

package com.cleversafe.oom.statistic;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class StatsTimerTest
{
   private StatsTimer t;

   @Before
   public void setUp() throws Exception
   {
      this.t = new StatsTimer();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeStartTimer()
   {
      this.t.startTimer(-1);
   }

   @Test
   public void testZeroStartTimer()
   {
      this.t.startTimer(0);
   }

   @Test
   public void testPositiveStartTimer()
   {
      this.t.startTimer(1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testStopTimerLessThanStartTimer()
   {
      this.t.startTimer(1);
      this.t.stopTimer(0);
   }

   @Test
   public void testStopTimerEqualStartTimer()
   {
      this.t.startTimer(1);
      this.t.stopTimer(1);
   }

   @Test
   public void testStopTimerGreaterThanStartTimer()
   {
      this.t.startTimer(1);
      this.t.stopTimer(2);
   }

   @Test
   public void testDuration()
   {
      final long start = this.t.startTimer(System.nanoTime());
      final long stop = System.nanoTime();
      final long duration = this.t.stopTimer(stop);
      Assert.assertEquals(stop - start, duration);
   }

   @Test(expected = NullPointerException.class)
   public void testNullCopyStatsTimer()
   {
      new StatsTimer(null);
   }

   @Test
   public void testCopyStatsTimerSameDuration()
   {
      final long start = this.t.startTimer(System.nanoTime());
      final StatsTimer t2 = new StatsTimer(this.t);
      final long timestamp = System.nanoTime();
      final long stop1 = this.t.stopTimer(timestamp);
      final long stop2 = t2.stopTimer(timestamp);
      Assert.assertEquals(stop2 - start, stop1 - start);
   }
}
