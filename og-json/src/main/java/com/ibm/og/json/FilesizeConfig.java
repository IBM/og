/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json;

import com.ibm.og.util.SizeUnit;
import com.ibm.og.util.json.type.DistributionType;

public class FilesizeConfig {
  public DistributionType distribution;
  public Double average;
  public SizeUnit averageUnit;
  public double spread;
  public SizeUnit spreadUnit;

  public FilesizeConfig(final double average) {
    this();
    this.average = average;
  }

  public FilesizeConfig() {
    this.distribution = DistributionType.UNIFORM;
    this.average = null;
    this.averageUnit = SizeUnit.BYTES;
    this.spread = 0.0;
    this.spreadUnit = SizeUnit.BYTES;
  }
}
