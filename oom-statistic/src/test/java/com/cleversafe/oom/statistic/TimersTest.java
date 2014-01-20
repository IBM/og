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

import com.cleversafe.oom.operation.OperationType;

public class TimersTest
{
   private Timers t;

   @Before
   public void setUp() throws Exception
   {
      this.t = new Timers();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeStartTimer()
   {
      this.t.start(OperationType.ALL, false, -1);
   }

   @Test
   public void testZeroStartTimer()
   {
      this.t.start(OperationType.ALL, false, 0);
   }

   @Test
   public void testPositiveStartTimer()
   {
      this.t.start(OperationType.ALL, false, 1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testStopTimerLessThanStartTimer()
   {
      this.t.start(OperationType.ALL, false, 1);
      this.t.elapsed(OperationType.ALL, false, 0);
   }

   @Test
   public void testStopTimerEqualStartTimer()
   {
      this.t.start(OperationType.ALL, false, 1);
      this.t.elapsed(OperationType.ALL, false, 1);
   }

   @Test
   public void testStopTimerGreaterThanStartTimer()
   {
      this.t.start(OperationType.ALL, false, 1);
      this.t.elapsed(OperationType.ALL, false, 2);
   }

   @Test
   public void testDuration()
   {
      final long start = this.t.start(OperationType.ALL, false, System.nanoTime());
      final long stop = System.nanoTime();
      final long duration = this.t.elapsed(OperationType.ALL, false, stop);
      Assert.assertEquals(stop - start, duration);
   }

   @Test(expected = NullPointerException.class)
   public void testNullCopyStatsTimer()
   {
      new Timers(null);
   }

   @Test
   public void testCopyTimers()
   {
      int i = 1;
      for (final OperationType o : OperationType.values())
      {

         this.t.start(o, false, i);
         this.t.start(o, true, i);
         i++;
      }
      i = 1;
      final Timers copy = new Timers(this.t);
      for (final OperationType o : OperationType.values())
      {
         Assert.assertEquals(1, this.t.elapsed(o, false, i + 1));
         Assert.assertEquals(1, this.t.elapsed(o, true, i + 1));
         Assert.assertEquals(1, copy.elapsed(o, false, i + 1));
         Assert.assertEquals(1, copy.elapsed(o, true, i + 1));
         i++;
      }
   }

   @Test(expected = IllegalArgumentException.class)
   public void testResetTimersNegativeTimestamp()
   {
      this.t.reset(false, -1);
   }

   @Test
   public void testResetTimersZeroTimestamp()
   {
      this.t.reset(false, 0);
   }

   @Test
   public void testResetTimersPositiveTimestamp()
   {
      this.t.reset(false, 1);
   }

   @Test
   public void testResetTimers()
   {
      int i = 1;
      for (final OperationType o : OperationType.values())
      {
         this.t.start(o, false, i);
         this.t.start(o, true, i);
         i++;

      }
      i = 1;
      final long resetTime = 10000;
      this.t.reset(true, resetTime);
      for (final OperationType o : OperationType.values())
      {
         Assert.assertEquals(1, this.t.elapsed(o, false, i + 1));
         Assert.assertEquals(0, this.t.elapsed(o, true, resetTime));
         i++;

      }
   }

   @Test
   public void testResetTimers2()
   {
      int i = 1;
      for (final OperationType o : OperationType.values())
      {
         this.t.start(o, false, i);
         this.t.start(o, true, i);
         i++;

      }
      i = 1;
      final long resetTime = 10000;
      this.t.reset(false, resetTime);
      for (final OperationType o : OperationType.values())
      {
         Assert.assertEquals(0, this.t.elapsed(o, false, resetTime));
         Assert.assertEquals(1, this.t.elapsed(o, true, i + 1));
         i++;

      }
   }
}
