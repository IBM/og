/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json;

import com.cleversafe.og.util.SizeUnit;

public class FilesizeConfig {
  DistributionType distribution;
  double average;
  SizeUnit averageUnit;
  double spread;
  SizeUnit spreadUnit;
  double weight;

  public FilesizeConfig(final double average) {
    this();
    this.average = average;
  }

  public FilesizeConfig() {
    this.distribution = DistributionType.UNIFORM;
    this.average = 5.0;
    this.averageUnit = SizeUnit.MEBIBYTES;
    this.spread = 0.0;
    this.spreadUnit = SizeUnit.MEBIBYTES;
    this.weight = 1.0;
  }

  public DistributionType getDistribution() {
    return this.distribution;
  }

  public double getAverage() {
    return this.average;
  }

  public SizeUnit getAverageUnit() {
    return this.averageUnit;
  }

  public double getSpread() {
    return this.spread;
  }

  public SizeUnit getSpreadUnit() {
    return this.spreadUnit;
  }

  public double getWeight() {
    return this.weight;
  }
}
