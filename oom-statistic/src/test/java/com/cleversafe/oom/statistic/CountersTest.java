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

public class CountersTest
{
   private Counters ctrs;

   @Before
   public void setBefore() throws Exception
   {
      this.ctrs = new Counters();
   }

   @Test
   public void testInitialCounters()
   {
      for (final Counter c : Counter.values())
      {
         Assert.assertEquals(0, this.ctrs.getCounter(c));
      }
   }

   @Test(expected = NullPointerException.class)
   public void testGetNullCounterType()
   {
      this.ctrs.getCounter((Counter) null);
   }

   @Test(expected = NullPointerException.class)
   public void testSetNullCounterType()
   {
      this.ctrs.setCounter((Counter) null, 1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testSetNegativeCounter()
   {
      this.ctrs.setCounter(Counter.BYTES, -1);
   }

   @Test
   public void testSetZeroCounter()
   {
      this.ctrs.setCounter(Counter.BYTES, 0);
   }

   @Test
   public void testSetPositiveCounter()
   {
      this.ctrs.setCounter(Counter.BYTES, 1);
   }

   @Test
   public void testSetGetCounter()
   {
      this.ctrs.setCounter(Counter.BYTES, 1024);
      Assert.assertEquals(1024, this.ctrs.getCounter(Counter.BYTES));
   }

   @Test(expected = NullPointerException.class)
   public void testNullCopyCounters()
   {
      new Counters(null);
   }

   @Test
   public void testCopyCounters()
   {
      int i = 0;
      for (final Counter c : Counter.values())
      {
         this.ctrs.setCounter(c, i);
         i++;
      }
      i = 0;
      final Counters copy = new Counters(this.ctrs);
      for (final Counter c : Counter.values())
      {
         Assert.assertEquals(i, this.ctrs.getCounter(c));
         Assert.assertEquals(i, copy.getCounter(c));
         i++;
      }
   }
}
