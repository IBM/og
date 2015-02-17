/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.util.distribution;

import java.util.Random;

/**
 * A distribution implementation that returns values conforming to a poisson distribution.
 * 
 * @since 1.0
 */
public class PoissonDistribution extends AbstractDistribution {
  /**
   * Constructs a poission distribution instance, using the provided random instance for random seed
   * data
   * 
   * @param average the average value of this distribution
   * @throws IllegalArgumentException if average is negative
   */
  public PoissonDistribution(final double average, final Random random) {
    super(average, 0.0, random);
  }

  /**
   * This implementation always returns {@code 0.0}
   */
  @Override
  public double getSpread() {
    return super.getSpread();
  }

  @Override
  public double nextSample() {
    // note the 1 - random.nextDouble (instead of random.nextDouble) is to prevent the case where
    // 0 is chosen and an infinite sleep occurs.
    return this.average * (-Math.log(1 - this.random.nextDouble()));
  }

  @Override
  public String toString() {
    return "PoissonDistribution [average=" + this.average + "]";
  }
}
