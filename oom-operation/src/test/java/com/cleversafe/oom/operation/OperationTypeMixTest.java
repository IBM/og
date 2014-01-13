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
// Date: Oct 25, 2013
// ---------------------

package com.cleversafe.oom.operation;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OperationTypeMixTest
{
   private Map<OperationType, Integer> countMap;

   @Before
   public void setBefore()
   {
      this.countMap = new HashMap<OperationType, Integer>();
      this.countMap.put(OperationType.READ, 0);
      this.countMap.put(OperationType.WRITE, 0);
      this.countMap.put(OperationType.DELETE, 0);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeReadPercentage()
   {
      new OperationTypeMix(-1, 50, 50, 0, 100);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeWritePercentage()
   {
      new OperationTypeMix(50, -1, 50, 0, 100);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeDeletePercentage()
   {
      new OperationTypeMix(50, 50, -1, 0, 100);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testLargeReadPercentage()
   {
      new OperationTypeMix(101, 50, 50, 0, 100);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testLargeWritePercentage()
   {
      new OperationTypeMix(50, 101, 50, 0, 100);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testLargeDeletePercentage()
   {
      new OperationTypeMix(50, 50, 101, 0, 100);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testLessThan100Mix()
   {
      new OperationTypeMix(33, 33, 33, 0, 100);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testGreaterThan100Mix()
   {
      new OperationTypeMix(33, 33, 35, 0, 100);
   }

   @Test
   public void testEqual100Mix()
   {
      new OperationTypeMix(33, 33, 34, 0, 100);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeFloor()
   {
      new OperationTypeMix(100, 0, 0, -100, 100);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeFloor2()
   {
      new OperationTypeMix(100, 0, 0, -1, 100);
   }

   @Test
   public void testZeroFloor()
   {
      new OperationTypeMix(100, 0, 0, 0, 100);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeCeiling()
   {
      new OperationTypeMix(100, 0, 0, 100, -100);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeCeiling2()
   {
      new OperationTypeMix(100, 0, 0, 100, -1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testCeilingLessThanFloor()
   {
      new OperationTypeMix(100, 0, 0, 100, 99);
   }

   @Test
   public void testCeilingEqualFloor()
   {
      new OperationTypeMix(100, 0, 0, 100, 100);
   }

   @Test
   public void testCeilingGreaterThanFloor()
   {
      new OperationTypeMix(100, 0, 0, 100, 101);
   }

   @Test(expected = NullPointerException.class)
   public void testNullRandom()
   {
      new OperationTypeMix(33, 33, 34, 0, 100, null);
   }

   @Test
   public void testValidRandom()
   {
      new OperationTypeMix(33, 33, 34, 0, 100, new Random());
   }

   @Test
   public void test100PercentRead()
   {
      final OperationTypeMix mix = new OperationTypeMix(100, 0, 0, 0, 100);
      for (int i = 0; i < 100; i++)
      {
         Assert.assertEquals(OperationType.READ, mix.getNextOperationType(50));
      }
   }

   @Test
   public void test100PercentWrite()
   {
      final OperationTypeMix mix = new OperationTypeMix(0, 100, 0, 0, 100);
      for (int i = 0; i < 100; i++)
      {
         Assert.assertEquals(OperationType.WRITE, mix.getNextOperationType(50));
      }
   }

   @Test
   public void test100PercentDelete()
   {
      final OperationTypeMix mix = new OperationTypeMix(0, 0, 100, 0, 100);
      for (int i = 0; i < 100; i++)
      {
         Assert.assertEquals(OperationType.DELETE, mix.getNextOperationType(50));
      }
   }

   @Test
   public void testReadWriteMix()
   {
      final OperationTypeMix mix = new OperationTypeMix(50, 50, 0, 0, 100);

      for (int i = 0; i < 100; i++)
      {
         final OperationType type = mix.getNextOperationType(50);
         final int count = this.countMap.get(type);
         this.countMap.put(type, count + 1);
      }
      Assert.assertTrue(this.countMap.get(OperationType.READ) > 0);
      Assert.assertTrue(this.countMap.get(OperationType.WRITE) > 0);
      Assert.assertTrue(this.countMap.get(OperationType.DELETE) == 0);
   }

   @Test
   public void testReadDeleteMix()
   {
      final OperationTypeMix mix = new OperationTypeMix(50, 0, 50, 0, 100);

      for (int i = 0; i < 100; i++)
      {
         final OperationType type = mix.getNextOperationType(50);
         final int count = this.countMap.get(type);
         this.countMap.put(type, count + 1);
      }
      Assert.assertTrue(this.countMap.get(OperationType.READ) > 0);
      Assert.assertTrue(this.countMap.get(OperationType.WRITE) == 0);
      Assert.assertTrue(this.countMap.get(OperationType.DELETE) > 0);
   }

   @Test
   public void testWriteDeleteMix()
   {
      final OperationTypeMix mix = new OperationTypeMix(0, 50, 50, 0, 100);

      for (int i = 0; i < 100; i++)
      {
         final OperationType type = mix.getNextOperationType(50);
         final int count = this.countMap.get(type);
         this.countMap.put(type, count + 1);
      }
      Assert.assertTrue(this.countMap.get(OperationType.READ) == 0);
      Assert.assertTrue(this.countMap.get(OperationType.WRITE) > 0);
      Assert.assertTrue(this.countMap.get(OperationType.DELETE) > 0);
   }

   @Test
   public void testReadWriteDeleteMix()
   {
      final OperationTypeMix mix = new OperationTypeMix(33, 33, 34, 0, 100);

      for (int i = 0; i < 100; i++)
      {
         final OperationType type = mix.getNextOperationType(50);
         final int count = this.countMap.get(type);
         this.countMap.put(type, count + 1);
      }
      Assert.assertTrue(this.countMap.get(OperationType.READ) > 0);
      Assert.assertTrue(this.countMap.get(OperationType.WRITE) > 0);
      Assert.assertTrue(this.countMap.get(OperationType.DELETE) > 0);
   }

   @Test
   public void testFloorCeiling()
   {
      final OperationTypeMix mix = new OperationTypeMix(0, 100, 0, 100, 200);
      Assert.assertEquals(OperationType.WRITE, mix.getNextOperationType(150));
      Assert.assertEquals(OperationType.DELETE, mix.getNextOperationType(201));
      Assert.assertEquals(OperationType.DELETE, mix.getNextOperationType(151));
      Assert.assertEquals(OperationType.WRITE, mix.getNextOperationType(150));
   }

   @Test
   public void testFloorCeiling2()
   {
      final OperationTypeMix mix = new OperationTypeMix(0, 0, 100, 100, 200);
      Assert.assertEquals(OperationType.DELETE, mix.getNextOperationType(150));
      Assert.assertEquals(OperationType.WRITE, mix.getNextOperationType(99));
      Assert.assertEquals(OperationType.WRITE, mix.getNextOperationType(149));
      Assert.assertEquals(OperationType.DELETE, mix.getNextOperationType(150));
   }
}
