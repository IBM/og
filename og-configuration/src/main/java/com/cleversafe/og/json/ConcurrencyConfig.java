/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.json;

import java.util.concurrent.TimeUnit;

public class ConcurrencyConfig {
  public ConcurrencyType type;
  public Double count;
  public TimeUnit unit;
  public double rampup;
  public TimeUnit rampupUnit;

  public ConcurrencyConfig() {
    this.type = null;
    this.count = null;
    this.unit = TimeUnit.SECONDS;
    this.rampup = 0.0;
    this.rampupUnit = TimeUnit.SECONDS;
  }
}
