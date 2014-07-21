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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.cleversafe.og.test.LoadTest;
import com.google.common.util.concurrent.Uninterruptibles;

public class RuntimeConditionTest
{
   @Test(expected = NullPointerException.class)
   public void testNullLoadTest()
   {
      new RuntimeCondition(null, 1.0, TimeUnit.SECONDS);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeRuntime()
   {
      final LoadTest test = mock(LoadTest.class);
      new RuntimeCondition(test, -1.0, TimeUnit.SECONDS);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testZeroRuntime()
   {
      final LoadTest test = mock(LoadTest.class);
      new RuntimeCondition(test, 0.0, TimeUnit.SECONDS);
   }

   @Test
   public void testPositiveRuntime()
   {
      final LoadTest test = mock(LoadTest.class);
      new RuntimeCondition(test, 1.0, TimeUnit.SECONDS);
   }

   @Test(expected = NullPointerException.class)
   public void testNullTimeUnit()
   {
      final LoadTest test = mock(LoadTest.class);
      new RuntimeCondition(test, 1.0, null);
   }

   @Test
   public void testRuntimeCondition()
   {
      final LoadTest test = mock(LoadTest.class);
      final RuntimeCondition c = new RuntimeCondition(test, 1.0, TimeUnit.SECONDS);
      Assert.assertFalse(c.isTriggered());
      verify(test, never()).stopTest();
      Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
      Assert.assertFalse(c.isTriggered());
      verify(test, never()).stopTest();
      Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
      Assert.assertTrue(c.isTriggered());
      verify(test).stopTest();
   }
}
