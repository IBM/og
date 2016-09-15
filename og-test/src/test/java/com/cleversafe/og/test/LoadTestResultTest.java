/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.test;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import com.cleversafe.og.test.condition.LoadTestResult;

public class LoadTestResultTest {

  @Test(expected = IllegalArgumentException.class)
  public void negativeTimestampStart() {
    new LoadTestResult(-1, 1, true, ImmutableList.of("Illegal start time stamp"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void timestampStartGreaterThanTimestampFinish() {
    new LoadTestResult(100, 50, true, ImmutableList.of("Illegal start time stamp"));
  }

  @Test
  public void timestampStartEqualTimestampFinish() {
    new LoadTestResult(100, 100, true, ImmutableList.of("Start timestamp is equal to End timestamp"));
  }

  @Test
  public void timestampStartLessThanTimestampFinish() {
    new LoadTestResult(100, 101, true, ImmutableList.of("Test Success"));
  }
}


