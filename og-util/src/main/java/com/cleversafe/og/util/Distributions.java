/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.math3.distribution.ConstantRealDistribution;
import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;


public class Distributions {
  private Distributions() {}

  public static Distribution uniform(double average, final double spread) {
    checkArgument(average >= 0.0, "average must be >= 0.0 [%s]", average);
    checkArgument(spread >= 0.0, "spread must be >= 0.0 [%s]", spread);

    if (spread == 0.0)
      return constant(average);

    double lower = average - spread;
    double upper = average + spread;
    checkArgument(lower >= 0.0, "average - spread must be >= 0.0 [%s]", lower);
    String s = String.format("UniformDistribution [average=%s, spread=%s]", average, spread);
    return new RealDistributionAdapter(new UniformRealDistribution(lower, upper), s);
  }

  public static Distribution normal(final double average, final double spread) {
    checkArgument(average >= 0.0, "average must be >= 0.0 [%s]", average);
    checkArgument(spread >= 0.0, "spread must be >= 0.0 [%s]", spread);

    if (spread == 0.0)
      return constant(average);

    final double min = average - (3 * spread);
    checkArgument(min >= 0.0, "three standard deviations must be >= 0.0 [%s]", min);
    String s = String.format("NormalDistribution [average=%s, spread=%s]", average, spread);
    return new RealDistributionAdapter(new NormalDistribution(average, spread), s);
  }

  public static Distribution lognormal(final double average, final double spread) {
    // FIXME configure lognormal distribution range correctly
    checkArgument(average >= 0.0, "average must be >= 0.0 [%s]", average);
    checkArgument(spread >= 0.0, "spread must be >= 0.0 [%s]", spread);

    if (spread == 0.0)
      return constant(average);

    final double min = average - (3 * spread);
    checkArgument(min >= 0.0, "three standard deviations must be >= 0.0 [%s]", min);
    String s = String.format("LogNormalDistribution [average=%s, spread=%s]", average, spread);
    return new RealDistributionAdapter(new LogNormalDistribution(average, spread), s);
  }

  public static Distribution poisson(final double average) {
    checkArgument(average >= 0.0, "average must be >= 0.0 [%s]", average);
    String s = String.format("PoissonDistribution [average=%s]", average);
    return new IntegerDistributionAdapter(new PoissonDistribution(average), s);
  }

  private static Distribution constant(final double average) {
    checkArgument(average >= 0.0, "average must be >= 0.0 [%s]", average);
    String s = String.format("ConstantDistribution [average=%s]", average);
    return new RealDistributionAdapter(new ConstantRealDistribution(average), s);
  }

  private static class RealDistributionAdapter implements Distribution {
    private final RealDistribution d;
    private final String toString;

    public RealDistributionAdapter(RealDistribution d, String toString) {
      this.d = checkNotNull(d);
      this.toString = checkNotNull(toString);
    }

    @Override
    public double getAverage() {
      return this.d.getNumericalMean();
    }

    @Override
    public double nextSample() {
      return this.d.sample();
    }

    @Override
    public String toString() {
      return this.toString;
    }
  }

  private static class IntegerDistributionAdapter implements Distribution {
    private final IntegerDistribution d;
    private final String toString;

    public IntegerDistributionAdapter(IntegerDistribution d, String toString) {
      this.d = checkNotNull(d);
      this.toString = checkNotNull(toString);
    }

    @Override
    public double getAverage() {
      return this.d.getNumericalMean();
    }

    @Override
    public double nextSample() {
      return this.d.sample();
    }

    @Override
    public String toString() {
      return this.toString;
    }
  }
}
