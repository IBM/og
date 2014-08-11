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
import org.junit.Before;
import org.junit.Test;

public class UniformDistributionTest
{
   private static final double ERR = Math.pow(0.1, 6);
   private Random random;

   @Before
   public void before()
   {
      this.random = new Random();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeMean()
   {
      new UniformDistribution(-1.0, 10.0, this.random);
   }

   @Test
   public void testZeroMean()
   {
      new UniformDistribution(0.0, 0.0, this.random);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeSpread()
   {
      new UniformDistribution(10.0, -1.0, this.random);
   }

   @Test
   public void testZeroSpread()
   {
      new UniformDistribution(10.0, 0.0, this.random);
   }

   @Test(expected = NullPointerException.class)
   public void testNullRandom()
   {
      new UniformDistribution(10.0, 10.0, null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeMin()
   {
      new UniformDistribution(5.0, 11.0, this.random);
   }

   @Test
   public void testZeroMin()
   {
      new UniformDistribution(5.0, 10.0, this.random);
   }

   @Test
   public void testBasicUniformDistribution()
   {
      final UniformDistribution ud = new UniformDistribution(10.0, 10.0, this.random);
      Assert.assertEquals(10.0, ud.getAverage(), ERR);
      Assert.assertEquals(10.0, ud.getSpread(), ERR);
      ud.nextSample();
      ud.nextSample();
      ud.nextSample();
   }

   @Test
   public void testZeroSpreadValue()
   {
      final UniformDistribution ud = new UniformDistribution(10.0, 0.0, this.random);
      for (int i = 0; i < 100; i++)
      {
         Assert.assertEquals(10, ud.nextSample(), 0.00001);
      }
   }
}
