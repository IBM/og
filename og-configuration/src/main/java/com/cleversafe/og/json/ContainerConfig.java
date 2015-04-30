/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json;

import java.util.List;

public class ContainerConfig {
  public static final int NONE = -1;
  public String prefix;
  public SelectionType selection;
  public int minSuffix;
  public int maxSuffix;
  public List<Double> weights;

  public ContainerConfig(final String container) {
    this();
    this.prefix = container;
  }

  public ContainerConfig() {
    this.prefix = null;
    this.selection = SelectionType.RANDOM;
    this.minSuffix = NONE;
    this.maxSuffix = NONE;
    this.weights = null;
  }
}
