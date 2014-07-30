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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Random;

/**
 * A distribution implementation that returns values conforming to a uniform distribution.
 * 
 * @since 1.0
 */
public class UniformDistribution implements Distribution
{
   private final double average;
   private final double spread;
   private final Random random;

   /**
    * Constructs a uniform distribution instance
    * 
    * @param average
    *           the average value of this distribution
    * @param spread
    *           the spread of this distribution. Spread is defined as distance between min and max
    *           values.
    * @throws IllegalArgumentException
    *            if average is negative
    * @throws IllegalArgumentException
    *            if spread is negative
    */
   public UniformDistribution(final double average, final double spread)
   {
      this(average, spread, new Random());
   }

   /**
    * Constructs a uniform distribution instance, using the provided random instance for random seed
    * data
    * 
    * @param average
    *           the average value of this distribution
    * @param spread
    *           the spread of this distribution
    * @throws IllegalArgumentException
    *            if average is negative
    * @throws IllegalArgumentException
    *            if spread is negative
    */
   public UniformDistribution(final double average, final double spread, final Random random)
   {
      checkArgument(average >= 0.0, "average must be >= 0.0 [%s]", average);
      checkArgument(spread >= 0.0, "spread must be >= 0.0 [%s]", spread);
      checkNotNull(random);
      this.average = average;
      this.spread = spread;
      this.random = random;
   }

   @Override
   public double getAverage()
   {
      return this.average;
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
         result = (this.average - halfWidth) + (this.spread * this.random.nextDouble());
      } while (result < 0);
      return result;
   }
}
