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
// Date: Nov 20, 2013
// ---------------------

package com.cleversafe.oom.test.condition;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.oom.statistic.Stats;

public class DurationStoppingConditionTest
{
   private Stats s;

   @Before
   public void setBefore()
   {
      this.s = new Stats(0, 1);
   }

   @Test(expected = NullPointerException.class)
   public void testNullStats()
   {
      new DurationStoppingCondition(null, 100, TimeUnit.SECONDS);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeDuration()
   {
      new DurationStoppingCondition(this.s, -1, TimeUnit.SECONDS);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeDuration2()
   {
      new DurationStoppingCondition(this.s, -100, TimeUnit.SECONDS);
   }

   @Test(expected = NullPointerException.class)
   public void testNullUnit()
   {
      new DurationStoppingCondition(this.s, 100, null);
   }

   @Test
   public void testZeroDuration()
   {
      new DurationStoppingCondition(this.s, 0, TimeUnit.SECONDS);
   }

   @Test
   public void testPositiveDuration()
   {
      new DurationStoppingCondition(this.s, 1, TimeUnit.SECONDS);
   }

   @Test
   public void testPositiveDuration2()
   {
      new DurationStoppingCondition(this.s, 100, TimeUnit.SECONDS);
   }

   @Test
   public void testDuration() throws InterruptedException
   {
      final StoppingCondition sc =
            new DurationStoppingCondition(this.s, 10, TimeUnit.MILLISECONDS);
      Assert.assertFalse(sc.triggered());
      Thread.sleep(15);
      Assert.assertTrue(sc.triggered());
   }
}
