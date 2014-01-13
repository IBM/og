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
import com.cleversafe.oom.statistic.Counter;
import com.cleversafe.oom.statistic.Stats;

public class CounterStoppingConditionTest
{
   private OperationType w;
   private Stats s;

   @Before
   public void setBefore()
   {
      this.w = OperationType.WRITE;
      this.s = new Stats(0, 1);
   }

   @Test(expected = NullPointerException.class)
   public void testNullStats()
   {
      new CounterStoppingCondition(null, this.w, Counter.COUNT, false, RelationalOperator.EQ, 1);
   }

   @Test(expected = NullPointerException.class)
   public void testNullOperationType()
   {
      new CounterStoppingCondition(this.s, null, Counter.COUNT, false, RelationalOperator.EQ,
            1);
   }

   @Test(expected = NullPointerException.class)
   public void testNullCounter()
   {
      new CounterStoppingCondition(this.s, OperationType.ALL, null, false, RelationalOperator.EQ, 1);
   }

   @Test(expected = NullPointerException.class)
   public void testNullRelationalOperator()
   {
      new CounterStoppingCondition(this.s, OperationType.ALL, Counter.COUNT, false, null, 1);
   }

   @Test
   public void testEQ()
   {
      final CounterStoppingCondition c =
            new CounterStoppingCondition(this.s, this.w, Counter.COUNT, false,
                  RelationalOperator.EQ, 1);
      Assert.assertFalse(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertTrue(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertFalse(c.triggered());
   }

   @Test
   public void testNE()
   {
      final CounterStoppingCondition c =
            new CounterStoppingCondition(this.s, this.w, Counter.COUNT, false,
                  RelationalOperator.NE, 1);
      Assert.assertTrue(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertFalse(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertTrue(c.triggered());
   }

   @Test
   public void testGT()
   {
      final CounterStoppingCondition c =
            new CounterStoppingCondition(this.s, this.w, Counter.COUNT, false,
                  RelationalOperator.GT, 1);
      Assert.assertFalse(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertFalse(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertTrue(c.triggered());
   }

   @Test
   public void testLT()
   {
      final CounterStoppingCondition c =
            new CounterStoppingCondition(this.s, this.w, Counter.COUNT, false,
                  RelationalOperator.LT, 1);
      Assert.assertTrue(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertFalse(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertFalse(c.triggered());
   }

   @Test
   public void testGE()
   {
      final CounterStoppingCondition c =
            new CounterStoppingCondition(this.s, this.w, Counter.COUNT, false,
                  RelationalOperator.GE, 1);
      Assert.assertFalse(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertTrue(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertTrue(c.triggered());
   }

   @Test
   public void testLE()
   {
      final CounterStoppingCondition c =
            new CounterStoppingCondition(this.s, this.w, Counter.COUNT, false,
                  RelationalOperator.LE, 1);
      Assert.assertTrue(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertTrue(c.triggered());
      this.s.beginOperation(this.w);
      Assert.assertFalse(c.triggered());
   }
}
