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

import org.junit.Test;

public class NormalDistributionTest extends AbstractDistributionTest
{
   @Override
   protected Distribution createDistribution(
         final double average,
         final double spread,
         final Random random)
   {
      return new NormalDistribution(average, spread, random);
   }

   @Test
   public void zeroMin()
   {
      new NormalDistribution(6.0, 2.0, new Random());
   }
}
