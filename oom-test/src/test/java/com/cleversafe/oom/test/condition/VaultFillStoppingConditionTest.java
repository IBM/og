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
// Date: Nov 18, 2013
// ---------------------

package com.cleversafe.oom.test.condition;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.oom.operation.OperationType;
import com.cleversafe.oom.statistic.Statistics;
import com.cleversafe.oom.statistic.StatisticsImpl;

public class VaultFillStoppingConditionTest
{
   private OperationType w;
   private Statistics s;

   @Before
   public void setBefore()
   {
      this.w = OperationType.WRITE;
      this.s = new StatisticsImpl(0, 1);
   }

   @Test(expected = NullPointerException.class)
   public void testNullStats()
   {
      new VaultFillStoppingCondition(null, 1, 1, RelationalOperator.EQ, 10);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeInitialObjectCount()
   {
      new VaultFillStoppingCondition(this.s, -1, 1, RelationalOperator.EQ, 10);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeInitialObjectCount2()
   {
      new VaultFillStoppingCondition(this.s, -100, 1, RelationalOperator.EQ, 10);
   }

   @Test
   public void testZeroInitialObjectCount()
   {
      new VaultFillStoppingCondition(this.s, 0, 1, RelationalOperator.EQ, 10);
   }

   @Test
   public void testPositiveInitialObjectCount()
   {
      new VaultFillStoppingCondition(this.s, 1, 1, RelationalOperator.EQ, 10);
   }

   @Test
   public void testPositiveInitialObjectCount2()
   {
      new VaultFillStoppingCondition(this.s, 100, 1, RelationalOperator.EQ, 10);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeGlobalAverageObjectSize()
   {
      new VaultFillStoppingCondition(this.s, 1, -1, RelationalOperator.EQ, 10);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeGlobalAverageObjectSize2()
   {
      new VaultFillStoppingCondition(this.s, 1, -100, RelationalOperator.EQ, 10);
   }

   @Test
   public void testZeroGlobalAverageObjectSize()
   {
      new VaultFillStoppingCondition(this.s, 1, 0, RelationalOperator.EQ, 10);
   }

   @Test
   public void testPositiveGlobalAverageObjectSize()
   {
      new VaultFillStoppingCondition(this.s, 1, 1, RelationalOperator.EQ, 10);
   }

   @Test
   public void testPositiveGlobalAverageObjectSize2()
   {
      new VaultFillStoppingCondition(this.s, 1, 100, RelationalOperator.EQ, 10);
   }

   @Test(expected = NullPointerException.class)
   public void testNullRelationalOperator()
   {
      new VaultFillStoppingCondition(this.s, 1, 1, null, 10);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeVaultFill()
   {
      new VaultFillStoppingCondition(this.s, 1, 1, RelationalOperator.EQ, -1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeVaultFill2()
   {
      new VaultFillStoppingCondition(this.s, 1, 1, RelationalOperator.EQ, -100);
   }

   @Test
   public void testZeroVaultFill()
   {
      new VaultFillStoppingCondition(this.s, 1, 1, RelationalOperator.EQ, 0);
   }

   @Test
   public void testPositiveVaultFill()
   {
      new VaultFillStoppingCondition(this.s, 1, 1, RelationalOperator.EQ, 1);
   }

   @Test
   public void testPositiveVaultFill2()
   {
      new VaultFillStoppingCondition(this.s, 1, 1, RelationalOperator.EQ, 100);
   }

   @Test
   public void testEQ()
   {
      final VaultFillStoppingCondition c =
            new VaultFillStoppingCondition(this.s, 1, 1024, RelationalOperator.EQ, 2048);
      Assert.assertFalse(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertTrue(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertFalse(c.triggered());
   }

   @Test
   public void testNE()
   {
      final VaultFillStoppingCondition c =
            new VaultFillStoppingCondition(this.s, 1, 1024, RelationalOperator.NE, 2048);
      Assert.assertTrue(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertFalse(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertTrue(c.triggered());
   }

   @Test
   public void testGT()
   {
      final VaultFillStoppingCondition c =
            new VaultFillStoppingCondition(this.s, 1, 1024, RelationalOperator.GT, 2048);
      Assert.assertFalse(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertFalse(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertTrue(c.triggered());
   }

   @Test
   public void testLT()
   {
      final VaultFillStoppingCondition c =
            new VaultFillStoppingCondition(this.s, 1, 1024, RelationalOperator.LT, 2048);
      Assert.assertTrue(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertFalse(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertFalse(c.triggered());
   }

   @Test
   public void testGE()
   {
      final VaultFillStoppingCondition c =
            new VaultFillStoppingCondition(this.s, 1, 1024, RelationalOperator.GE, 2048);
      Assert.assertFalse(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertTrue(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertTrue(c.triggered());
   }

   @Test
   public void testLE()
   {
      final VaultFillStoppingCondition c =
            new VaultFillStoppingCondition(this.s, 1, 1024, RelationalOperator.LE, 2048);
      Assert.assertTrue(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertTrue(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertFalse(c.triggered());
   }
}
