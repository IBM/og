/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;

public class StoppingConditionsConfig {
  public long operations;
  public double runtime;
  public TimeUnit runtimeUnit;
  public long concurrentRequests;
  public Map<Integer, Integer> statusCodes;

  public StoppingConditionsConfig() {
    this.operations = 0;
    this.runtime = 0.0;
    this.runtimeUnit = TimeUnit.SECONDS;
    this.concurrentRequests = 2000;
    this.statusCodes = Maps.newHashMap();
  }
}
