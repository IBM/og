/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json;

import java.util.List;

import com.google.common.collect.Lists;

public class SelectionConfig<T> {
  public SelectionType type;
  public List<ChoiceConfig<T>> choices;

  public SelectionConfig() {
    this.type = SelectionType.RANDOM;
    this.choices = Lists.newArrayList();
  }

  public SelectionConfig(T choice) {
    this();
    this.choices.add(new ChoiceConfig<T>(choice));
  }
}
