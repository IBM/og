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
// Date: Jul 9, 2014
// ---------------------

package com.cleversafe.og.test.condition;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.operation.Method;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.statistic.Counter;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.test.LoadTest;
import com.cleversafe.og.util.Entities;
import com.cleversafe.og.util.Operation;
import com.cleversafe.og.util.Pair;

public class CounterConditionTest
{
   private LoadTest test;
   private Statistics stats;

   @Before
   public void before()
   {
      this.test = mock(LoadTest.class);
      this.stats = new Statistics();
   }

   @Test(expected = NullPointerException.class)
   public void testNullOperation()
   {
      new CounterCondition(null, Counter.BYTES, 1, this.test, this.stats);
   }

   @Test(expected = NullPointerException.class)
   public void testNullCounter()
   {
      new CounterCondition(Operation.WRITE, null, 1, this.test, this.stats);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeThreshold()
   {
      new CounterCondition(Operation.WRITE, Counter.BYTES, -1, this.test, this.stats);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testZeroThreshold()
   {
      new CounterCondition(Operation.WRITE, Counter.BYTES, 0, this.test, this.stats);
   }

   @Test
   public void testPositiveThreshold()
   {
      new CounterCondition(Operation.WRITE, Counter.BYTES, 1, this.test, this.stats);
   }

   @Test(expected = NullPointerException.class)
   public void testNullLoadTest()
   {
      new CounterCondition(Operation.WRITE, Counter.BYTES, 1, null, this.stats);
   }

   @Test(expected = NullPointerException.class)
   public void testNullStatistics()
   {
      new CounterCondition(Operation.WRITE, Counter.BYTES, 1, this.test, null);
   }

   @Test
   public void testCounterCondition()
   {
      final Request request = mock(Request.class);
      when(request.getMethod()).thenReturn(Method.PUT);
      when(request.getEntity()).thenReturn(Entities.none());
      final Response response = mock(Response.class);
      when(response.getEntity()).thenReturn(Entities.none());
      final Pair<Request, Response> operation = new Pair<Request, Response>(request, response);

      final CounterCondition c =
            new CounterCondition(Operation.WRITE, Counter.OPERATIONS, 2, this.test, this.stats);
      Assert.assertFalse(c.isTriggered());
      this.stats.update(operation);
      Assert.assertFalse(c.isTriggered());
      this.stats.update(operation);
      Assert.assertTrue(c.isTriggered());
   }
}
