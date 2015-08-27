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
  public DistributionType distribution;
  public double average;
  public SizeUnit averageUnit;
  public double spread;
  public SizeUnit spreadUnit;

  public FilesizeConfig(final double average) {
    this();
    this.average = average;
  }

  public FilesizeConfig() {
    this.distribution = DistributionType.UNIFORM;
    this.average = 5242880.0;
    this.averageUnit = SizeUnit.BYTES;
    this.spread = 0.0;
    this.spreadUnit = SizeUnit.BYTES;
  }
}
