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

package com.cleversafe.og.util.distribution;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Random;

/**
 * A <code>Distribution</code> implementation that returns values conforming to a poisson
 * distribution.
 */
public class PoissonDistribution implements Distribution
{
   private final double average;
   private final Random random;

   /**
    * Constructs a <code>PoissonDistribution</code> instance
    * 
    * @param average
    *           the average value of this distribution
    * @throws IllegalArgumentException
    *            if average is negative
    */
   public PoissonDistribution(final double average)
   {
      this(average, new Random());
   }

   /**
    * Constructs a <code>PoissonDistribution</code> instance, using the provided <code>Random</code>
    * instance for random seed data
    * 
    * @param average
    *           the average value of this distribution
    * @throws IllegalArgumentException
    *            if average is negative
    * @throws NullPointerException
    *            if random is null
    */
   public PoissonDistribution(final double average, final Random random)
   {
      checkArgument(average >= 0.0, "average must be >= 0.0 [%s]", average);
      checkNotNull(random);
      this.average = average;
      this.random = random;
   }

   @Override
   public double getAverage()
   {
      return this.average;
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
      return this.average * (-Math.log(1 - this.random.nextDouble()));
   }
}
