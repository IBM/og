/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json;

import java.util.Map;

import com.google.common.collect.Maps;

public class OperationConfig {
  public double weight;
  public SelectionConfig<String> host;
  public ObjectConfig object;
  public Map<String, SelectionConfig<String>> headers;

  public OperationConfig(final double weight) {
    this();
    this.weight = weight;
  }

  public OperationConfig() {
    this.weight = 0.0;
    this.host = null;
    this.object = new ObjectConfig();
    this.headers = Maps.newLinkedHashMap();
  }
}
