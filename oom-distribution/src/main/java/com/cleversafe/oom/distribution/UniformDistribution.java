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

package com.cleversafe.oom.distribution;

import java.util.Random;

import org.apache.commons.lang3.Validate;

/**
 * A <code>Distribution</code> implementation that returns values conforming to a uniform
 * distribution.
 */
public class UniformDistribution implements Distribution
{
   private final double mean;
   private final double spread;
   private final Random random;

   /**
    * Constructs a <code>UniformDistribution</code> instance
    * 
    * @param mean
    *           the mean value of this distribution
    * @param spread
    *           the spread of this distribution. Spread is defined as distance between min and max
    *           values.
    * @throws IllegalArgumentException
    *            if mean is negative
    * @throws IllegalArgumentException
    *            if spread is negative
    */
   public UniformDistribution(final double mean, final double spread)
   {
      this(mean, spread, new Random());
   }

   /**
    * Constructs a <code>UniformDistribution</code> instance, using the provided <code>Random</code>
    * instance for random seed data
    * 
    * @param mean
    *           the mean value of this distribution
    * @param spread
    *           the spread of this distribution
    * @throws IllegalArgumentException
    *            if mean is negative
    * @throws IllegalArgumentException
    *            if spread is negative
    * @throws NullPointerException
    *            if random is null
    */
   public UniformDistribution(final double mean, final double spread, final Random random)
   {
      Validate.isTrue(mean >= 0.0, "mean must be >= 0.0 [%s]", mean);
      Validate.isTrue(spread >= 0.0, "spread must be >= 0.0 [%s]", spread);
      Validate.notNull(random, "random must not be null");
      this.mean = mean;
      this.spread = spread;
      this.random = random;
   }

   @Override
   public double getMean()
   {
      return this.mean;
   }

   @Override
   public double getSpread()
   {
      return this.spread;
   }

   @Override
   public double nextSample()
   {
      double result;
      final double halfWidth = this.spread / 2;
      do
      {
         result = (this.mean - halfWidth) + (this.spread * this.random.nextDouble());
      } while (result < 0);
      return result;
   }
}
