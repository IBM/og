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
// Date: Oct 23, 2013
// ---------------------

package com.cleversafe.og.util.distribution;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;

import java.util.Random;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public abstract class AbstractDistributionTest
{
   @Rule
   public ExpectedException thrown = ExpectedException.none();
   private static final double ERR = Math.pow(0.1, 6);

   protected abstract Distribution createDistribution(double average, double spread, Random random);

   @DataProvider
   public static Object[][] provideInvalidDistribution()
   {
      final Random random = new Random();
      return new Object[][]
      {
            {-1.0, 0.0, random, IllegalArgumentException.class},
            {0.0, -1.0, random, IllegalArgumentException.class},
            {0.0, 0.0, null, NullPointerException.class},
            {0.0, 10.0, random, IllegalArgumentException.class},
      };
   }

   @Test
   @UseDataProvider("provideInvalidDistribution")
   public void invalidDistribution(
         final double average,
         final double spread,
         final Random random,
         final Class<Exception> expectedException)
   {
      this.thrown.expect(expectedException);
      createDistribution(average, spread, random);
   }

   @DataProvider
   public static Object[][] provideDistribution()
   {
      return new Object[][]
      {
            {0.0, 0.0},
            {10.0, 0.0},
            {10.0, 2.0},
      };
   }

   @Test
   @UseDataProvider("provideDistribution")
   public void distribution(final double average, final double spread)
   {
      final Distribution d = createDistribution(average, spread, new Random());
      assertThat(d.getAverage(), is(average));
      assertThat(d.getSpread(), is(spread));
   }

   @Test
   public void sample()
   {
      final Distribution d = createDistribution(10.0, 0.0, new Random());
      for (int i = 0; i < 100; i++)
      {
         assertThat(d.nextSample(), closeTo(10.0, ERR));
      }
   }
}
