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
// Date: Aug 11, 2014
// ---------------------

package com.cleversafe.og.util.distribution;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Random;

public abstract class AbstractDistribution implements Distribution {
  protected final double average;
  protected final double spread;
  protected final Random random;

  protected AbstractDistribution(final double average, final double spread, final Random random) {
    checkArgument(average >= 0.0, "average must be >= 0.0 [%s]", average);
    checkArgument(spread >= 0.0, "spread must be >= 0.0 [%s]", spread);
    this.average = average;
    this.spread = spread;
    this.random = checkNotNull(random);
  }

  @Override
  public double getAverage() {
    return this.average;
  }

  @Override
  public double getSpread() {
    return this.spread;
  }

  @Override
  public abstract double nextSample();
}
