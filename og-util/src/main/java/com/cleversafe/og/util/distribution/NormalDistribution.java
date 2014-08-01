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
 * A distribution implementation that returns values conforming to a normal distribution.
 * 
 * @since 1.0
 */
public class NormalDistribution implements Distribution
{
   private final double average;
   private final double spread;
   private final Random random;

   /**
    * Constructs a normal distribution instance
    * 
    * @param average
    *           the average value of this distribution
    * @param spread
    *           the spread of this distribution. Spread is defined as the first standard deviation.
    * @throws IllegalArgumentException
    *            if average is negative
    * @throws IllegalArgumentException
    *            if spread is negative
    */
   public NormalDistribution(final double average, final double spread)
   {
      this(average, spread, new Random());
   }

   /**
    * Constructs a normal distribution instance, using the provided random instance for random seed
    * data
    * 
    * @param average
    *           the average value of this distribution
    * @param spread
    *           the spread of this distribution. Spread is defined as the first standard deviation.
    * @throws IllegalArgumentException
    *            if average is negative
    * @throws IllegalArgumentException
    *            if spread is negative
    */
   public NormalDistribution(final double average, final double spread, final Random random)
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

   /**
    * {@inheritDoc}
    * 
    * This implementation will always return a non-negative sample.
    */
   @Override
   public double nextSample()
   {
      double result;
      do
      {
         result = this.average + (this.spread * this.random.nextGaussian());
      } while (result < 0);
      return result;
   }

   @Override
   public String toString()
   {
      return "NormalDistribution [average=" + this.average + ", spread=" + this.spread + "]";
   }
}
