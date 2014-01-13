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
// Date: Jan 3, 2014
// ---------------------

package com.cleversafe.oom.distribution;

import java.util.Random;

import org.apache.commons.lang3.Validate;

/**
 * A <code>Distribution</code> implementation that returns values conforming to a poisson
 * distribution.
 */
public class PoissonDistribution implements Distribution
{
   private final double mean;
   private final Random random;

   /**
    * Constructs a <code>PoissonDistribution</code> instance
    * 
    * @param mean
    *           the mean value of this distribution
    * @throws IllegalArgumentException
    *            if mean is negative
    */
   public PoissonDistribution(final double mean)
   {
      this(mean, new Random());
   }

   /**
    * Constructs a <code>PoissonDistribution</code> instance, using the provided <code>Random</code>
    * instance for random seed data
    * 
    * @param mean
    *           the mean value of this distribution
    * @throws IllegalArgumentException
    *            if mean is negative
    * @throws NullPointerException
    *            if random is null
    */
   public PoissonDistribution(final double mean, final Random random)
   {
      Validate.isTrue(mean >= 0.0, "mean must be >= 0.0 [%s]", mean);
      Validate.notNull(random, "random must not be null");
      this.mean = mean;
      this.random = random;
   }

   @Override
   public double getMean()
   {
      return this.mean;
   }

   /**
    * This implementation always returns <code>0.0</code>
    */
   @Override
   public double getSpread()
   {
      return 0.0;
   }

   @Override
   public double nextSample()
   {
      // note the 1 - random.nextDouble (instead of random.nextDouble) is to prevent the case where
      // 0 is chosen and an infinite sleep occurs.
      return this.mean * (-Math.log(1 - this.random.nextDouble()));
   }
}
