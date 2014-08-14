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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.cleversafe.og.test.LoadTest;
import com.google.common.util.concurrent.Uninterruptibles;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class RuntimeConditionTest
{
   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @DataProvider
   public static Object[][] provideInvalidRuntimeCondition()
   {
      final LoadTest test = mock(LoadTest.class);
      final TimeUnit unit = TimeUnit.SECONDS;
      return new Object[][]{
            {null, 1.0, unit, NullPointerException.class},
            {test, -1.0, unit, IllegalArgumentException.class},
            {test, 0.0, unit, IllegalArgumentException.class},
            {test, 1.0, null, NullPointerException.class}
      };
   }

   @Test
   @UseDataProvider("provideInvalidRuntimeCondition")
   public void invalidRuntimeCondition(
         final LoadTest test,
         final double runtime,
         final TimeUnit unit,
         final Class<Exception> expectedException)
   {
      this.thrown.expect(expectedException);
      new RuntimeCondition(test, runtime, unit);
   }

   @Test
   public void runtimeCondition()
   {
      final RuntimeCondition condition =
            new RuntimeCondition(mock(LoadTest.class), 50, TimeUnit.MILLISECONDS);

      assertThat(condition.isTriggered(), is(false));
      Uninterruptibles.sleepUninterruptibly(25, TimeUnit.MILLISECONDS);
      assertThat(condition.isTriggered(), is(false));
      Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
      assertThat(condition.isTriggered(), is(true));
   }
}
