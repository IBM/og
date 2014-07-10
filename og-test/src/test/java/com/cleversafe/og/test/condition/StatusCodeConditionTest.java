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
import org.junit.Test;

import com.cleversafe.og.operation.Method;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.util.Entities;
import com.cleversafe.og.util.Operation;

public class StatusCodeConditionTest
{
   @Test(expected = NullPointerException.class)
   public void testNullOperation()
   {
      new StatusCodeCondition(null, 200, 1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeStatusCode()
   {
      new StatusCodeCondition(Operation.WRITE, -1, 1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testZeroStatusCode()
   {
      new StatusCodeCondition(Operation.WRITE, 0, 1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testSmallStatusCode()
   {
      new StatusCodeCondition(Operation.WRITE, 99, 1);
   }

   @Test
   public void testMediumStatusCode()
   {
      new StatusCodeCondition(Operation.WRITE, 200, 1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testLargeStatusCode()
   {
      new StatusCodeCondition(Operation.WRITE, 600, 1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeThreshold()
   {
      new StatusCodeCondition(Operation.WRITE, 200, -1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testZeroThreshold()
   {
      new StatusCodeCondition(Operation.WRITE, 200, 0);
   }

   @Test
   public void testPositiveThreshold()
   {
      new StatusCodeCondition(Operation.WRITE, 200, 1);
   }

   @Test(expected = NullPointerException.class)
   public void testIsTriggeredNullStatistics()
   {
      new StatusCodeCondition(Operation.WRITE, 200, 1).isTriggered(null);
   }

   @Test
   public void testStatusCodeCondition()
   {
      final Request request = mock(Request.class);
      when(request.getMethod()).thenReturn(Method.PUT);
      when(request.getEntity()).thenReturn(Entities.none());
      final Response response = mock(Response.class);
      when(response.getEntity()).thenReturn(Entities.none());
      when(response.getStatusCode()).thenReturn(200);

      final StatusCodeCondition c = new StatusCodeCondition(Operation.WRITE, 200, 2);
      final Statistics stats = new Statistics();
      Assert.assertFalse(c.isTriggered(stats));
      stats.update(request, response);
      Assert.assertFalse(c.isTriggered(stats));
      stats.update(request, response);
      Assert.assertTrue(c.isTriggered(stats));
   }
}
