/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json;

import java.util.concurrent.TimeUnit;

public class ConcurrencyConfig {
  public ConcurrencyType type;
  public double count;
  public TimeUnit unit;
  public double rampup;
  public TimeUnit rampupUnit;

  public ConcurrencyConfig(final double count) {
    this();
    this.count = count;
  }

  public ConcurrencyConfig() {
    this.type = ConcurrencyType.THREADS;
    this.count = 1.0;
    this.unit = TimeUnit.SECONDS;
    this.rampup = 0.0;
    this.rampupUnit = TimeUnit.SECONDS;
  }
}
