/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
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
