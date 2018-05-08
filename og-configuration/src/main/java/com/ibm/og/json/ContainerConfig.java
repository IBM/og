/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json;

import java.util.List;

public class ContainerConfig {
  public static final int NONE = -1;
  public String prefix;
  public SelectionType selection;
  public int minSuffix;
  public int maxSuffix;
  public List<Double> weights;
  public int objectRestorePeriod;

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
    this.objectRestorePeriod = -1;
  }
}
