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

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.cleversafe.og.util.distribution.NormalDistribution;

public class NormalDistributionTest
{
   private static final double err = Math.pow(0.1, 6);

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeMean()
   {
      new NormalDistribution(-1.0, 10.0);
   }

   @Test
   public void testZeroMean()
   {
      new NormalDistribution(0.0, 10.0);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeSpread()
   {
      new NormalDistribution(10.0, -1.0);
   }

   @Test
   public void testZeroSpread()
   {
      new NormalDistribution(10.0, 0.0);
   }

   @Test(expected = NullPointerException.class)
   public void testNullRandom()
   {
      new NormalDistribution(10.0, 10.0, null);
   }

   @Test
   public void testBasicNormalDistribution()
   {
      final NormalDistribution nd = new NormalDistribution(10.0, 10.0);
      Assert.assertEquals(10.0, nd.getMean(), err);
      Assert.assertEquals(10.0, nd.getSpread(), err);
      nd.nextSample();
      nd.nextSample();
      nd.nextSample();
   }

   @Test
   public void testNormalDistributionWithRandom()
   {
      final NormalDistribution nd = new NormalDistribution(10.0, 10.0, new Random());
      nd.nextSample();
      nd.nextSample();
      nd.nextSample();
   }

   @Test
   public void testZeroSpreadValue()
   {
      final NormalDistribution nd = new NormalDistribution(10.0, 0.0);
      for (int i = 0; i < 100; i++)
      {
         Assert.assertEquals(10, nd.nextSample(), 0.00001);
      }
   }
}