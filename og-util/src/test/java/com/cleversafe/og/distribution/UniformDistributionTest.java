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

package com.cleversafe.og.distribution;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.cleversafe.og.distribution.UniformDistribution;

public class UniformDistributionTest
{
   @Test(expected = IllegalArgumentException.class)
   public void testNegativeMean()
   {
      new UniformDistribution(-1.0, 10.0);
   }

   @Test
   public void testZeroMean()
   {
      new UniformDistribution(0.0, 10.0);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeSpread()
   {
      new UniformDistribution(10.0, -1.0);
   }

   @Test
   public void testZeroSpread()
   {
      new UniformDistribution(10.0, 0.0);
   }

   @Test(expected = NullPointerException.class)
   public void testNullRandom()
   {
      new UniformDistribution(10.0, 10.0, null);
   }

   @Test
   public void testBasicUniformDistribution()
   {
      final UniformDistribution ud = new UniformDistribution(10.0, 10.0);
      ud.nextSample();
      ud.nextSample();
      ud.nextSample();
   }

   @Test
   public void testUniformDistributionWithRandom()
   {
      final UniformDistribution ud = new UniformDistribution(10.0, 10.0, new Random());
      ud.nextSample();
      ud.nextSample();
      ud.nextSample();
   }

   @Test
   public void testZeroSpreadValue()
   {
      final UniformDistribution ud = new UniformDistribution(10.0, 0.0);
      for (int i = 0; i < 100; i++)
      {
         Assert.assertEquals(10, ud.nextSample(), 0.00001);
      }
   }
}
