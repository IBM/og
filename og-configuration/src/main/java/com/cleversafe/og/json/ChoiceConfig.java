/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json;

public class ChoiceConfig<T> {
  public T choice;
  public double weight;

  public ChoiceConfig() {
    this.choice = null;
    this.weight = 1.0;
  }

  public ChoiceConfig(final T choice) {
    this();
    this.choice = choice;
  }
}
