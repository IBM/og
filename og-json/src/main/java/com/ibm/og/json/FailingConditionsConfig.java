/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FailingConditionsConfig {
  public long operations;
  public double runtime;
  public TimeUnit runtimeUnit;
  public long concurrentRequests;
  public Map<Integer, Integer> statusCodes;

  public FailingConditionsConfig() {
    this.operations = 0;
    this.runtime = 0.0;
    this.runtimeUnit = TimeUnit.SECONDS;
    this.concurrentRequests = 2000;
    this.statusCodes = Maps.newHashMap();
  }
}
