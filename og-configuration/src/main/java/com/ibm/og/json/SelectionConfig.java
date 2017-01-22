/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json;

import java.util.List;

import com.google.common.collect.Lists;

public class SelectionConfig<T> {
  public SelectionType selection;
  public List<ChoiceConfig<T>> choices;

  public SelectionConfig() {
    this.selection = SelectionType.RANDOM;
    this.choices = Lists.newArrayList();
  }
}
