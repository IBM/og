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

public class LogNormalDistributionTest
{
   private Random random;

   @Before
   public void before()
   {
      this.random = new Random();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeMean()
   {
      new LogNormalDistribution(-1.0, 10.0, this.random);
   }

   @Test
   public void testZeroMean()
   {
      new LogNormalDistribution(0.0, 10.0, this.random);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeSpread()
   {
      new LogNormalDistribution(10.0, -1.0, this.random);
   }

   @Test
   public void testZeroSpread()
   {
      new LogNormalDistribution(10.0, 0.0, this.random);
   }

   @Test(expected = NullPointerException.class)
   public void testNullRandom()
   {
      new LogNormalDistribution(10.0, 10.0, null);
   }

   @Test
   public void testBasicLogNormalDistribution()
   {
      final LogNormalDistribution lnd = new LogNormalDistribution(10.0, 10.0, this.random);
      lnd.nextSample();
      lnd.nextSample();
      lnd.nextSample();
   }

   @Test
   public void testLogNormalDistributionWithRandom()
   {
      final LogNormalDistribution lnd = new LogNormalDistribution(10.0, 10.0, this.random);
      lnd.nextSample();
      lnd.nextSample();
      lnd.nextSample();
   }

   @Test
   public void testZeroSpreadValue()
   {
      final LogNormalDistribution lnd = new LogNormalDistribution(10.0, 0.0, this.random);
      for (int i = 0; i < 100; i++)
      {
         Assert.assertEquals(10.0, lnd.nextSample(), 0.00001);
      }
   }
}
