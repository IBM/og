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
      for (final OperationType o : OperationType.values())
      {
         for (final Counter c : Counter.values())
         {
            Assert.assertEquals(0, this.ctrs.get(o, c, false));
            Assert.assertEquals(0, this.ctrs.get(o, c, true));
         }
      }
   }

   @Test(expected = NullPointerException.class)
   public void testGetNullOperationType()
   {
      this.ctrs.get(null, Counter.BYTES, false);
   }

   @Test(expected = NullPointerException.class)
   public void testGetNullCounterType()
   {
      this.ctrs.get(OperationType.ALL, null, false);
   }

   @Test(expected = NullPointerException.class)
   public void testSetNullOperationType()
   {
      this.ctrs.set(null, Counter.BYTES, false, 1);
   }

   @Test(expected = NullPointerException.class)
   public void testSetNullCounterType()
   {
      this.ctrs.set(OperationType.ALL, null, false, 1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testSetNegativeCounter()
   {
      this.ctrs.set(OperationType.ALL, Counter.BYTES, false, -1);
   }

   @Test
   public void testSetZeroCounter()
   {
      this.ctrs.set(OperationType.ALL, Counter.BYTES, false, 0);
   }

   @Test
   public void testSetPositiveCounter()
   {
      this.ctrs.set(OperationType.ALL, Counter.BYTES, false, 1);
   }

   @Test(expected = NullPointerException.class)
   public void testModifyNullOperationType()
   {
      this.ctrs.modify(null, Counter.BYTES, false, 1);
   }

   @Test(expected = NullPointerException.class)
   public void testModifyNullCounterType()
   {
      this.ctrs.modify(OperationType.ALL, null, false, 1);
   }

   @Test(expected = IllegalStateException.class)
   public void testModifyNegative()
   {
      this.ctrs.modify(OperationType.ALL, Counter.BYTES, false, -100);
   }

   @Test(expected = IllegalStateException.class)
   public void testModifyNegative2()
   {
      this.ctrs.modify(OperationType.ALL, Counter.BYTES, false, -1);
   }

   @Test
   public void testModifyNegative3()
   {
      final long newValue = this.ctrs.modify(OperationType.ALL, Counter.BYTES, false, 1);
      Assert.assertEquals(1, newValue);
      Assert.assertEquals(newValue, this.ctrs.get(OperationType.ALL, Counter.BYTES, false));
      final long newValue2 = this.ctrs.modify(OperationType.ALL, Counter.BYTES, false, -1);
      Assert.assertEquals(0, newValue2);
      Assert.assertEquals(newValue2, this.ctrs.get(OperationType.ALL, Counter.BYTES, false));
   }

   @Test
   public void testModifyZero()
   {
      final long newValue = this.ctrs.modify(OperationType.ALL, Counter.BYTES, false, 0);
      Assert.assertEquals(0, newValue);
      Assert.assertEquals(newValue, this.ctrs.get(OperationType.ALL, Counter.BYTES, false));
   }

   @Test
   public void testModifyPositive()
   {
      final long newValue = this.ctrs.modify(OperationType.ALL, Counter.BYTES, false, 1);
      Assert.assertEquals(1, newValue);
      Assert.assertEquals(newValue, this.ctrs.get(OperationType.ALL, Counter.BYTES, false));
   }

   @Test
   public void testModifyPositive2()
   {
      final long newValue = this.ctrs.modify(OperationType.ALL, Counter.BYTES, false, 100);
      Assert.assertEquals(100, newValue);
      Assert.assertEquals(newValue, this.ctrs.get(OperationType.ALL, Counter.BYTES, false));
   }

   @Test(expected = NullPointerException.class)
   public void testNullCopyCounters()
   {
      new Counters(null);
   }

   @Test
   public void testCopyCounters()
   {
      int i = 1;
      for (final OperationType o : OperationType.values())
      {
         for (final Counter c : Counter.values())
         {
            this.ctrs.set(o, c, false, i);
            this.ctrs.set(o, c, true, i);
            i++;
         }
      }
      i = 1;
      final Counters copy = new Counters(this.ctrs);
      for (final OperationType o : OperationType.values())
      {
         for (final Counter c : Counter.values())
         {
            Assert.assertEquals(i, this.ctrs.get(o, c, false));
            Assert.assertEquals(i, this.ctrs.get(o, c, true));
            Assert.assertEquals(i, copy.get(o, c, false));
            Assert.assertEquals(i, copy.get(o, c, true));
            i++;
         }
      }
   }

   @Test
   public void testClearCounters()
   {
      int i = 1;
      for (final OperationType o : OperationType.values())
      {
         for (final Counter c : Counter.values())
         {
            this.ctrs.set(o, c, false, i);
            this.ctrs.set(o, c, true, i);
            i++;
         }
      }
      i = 1;
      this.ctrs.clear(true);
      for (final OperationType o : OperationType.values())
      {
         for (final Counter c : Counter.values())
         {
            Assert.assertEquals(i, this.ctrs.get(o, c, false));
            Assert.assertEquals(0, this.ctrs.get(o, c, true));
            i++;
         }
      }
   }

   @Test
   public void testClearCounters2()
   {
      int i = 1;
      for (final OperationType o : OperationType.values())
      {
         for (final Counter c : Counter.values())
         {
            this.ctrs.set(o, c, false, i);
            this.ctrs.set(o, c, true, i);
            i++;
         }
      }
      i = 1;
      this.ctrs.clear(false);
      for (final OperationType o : OperationType.values())
      {
         for (final Counter c : Counter.values())
         {
            Assert.assertEquals(0, this.ctrs.get(o, c, false));
            Assert.assertEquals(i, this.ctrs.get(o, c, true));
            i++;
         }
      }
   }
}
