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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Random;

import org.junit.Test;

public class PoissonDistributionTest {
  @Test(expected = IllegalArgumentException.class)
  public void negativeAverage() {
    new PoissonDistribution(-1.0, new Random());
  }

  @Test
  public void zeroAverage() {
    new PoissonDistribution(0.0, new Random());
  }

  @Test(expected = NullPointerException.class)
  public void nullRandom() {
    new PoissonDistribution(10.0, null);
  }

  @Test
  public void sample() {
    final PoissonDistribution d = new PoissonDistribution(10.0, new Random());
    assertThat(d.getAverage(), is(10.0));
    assertThat(d.getSpread(), is(0.0));
    d.nextSample();
  }
}
