/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.test;

import org.junit.Test;

import com.cleversafe.og.test.condition.LoadTestResult;

public class LoadTestResultTest {

  @Test(expected = IllegalArgumentException.class)
  public void negativeTimestampStart() {
    new LoadTestResult(-1, 1, true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void timestampStartGreaterThanTimestampFinish() {
    new LoadTestResult(100, 50, true);
  }

  @Test
  public void timestampStartEqualTimestampFinish() {
    new LoadTestResult(100, 100, true);
  }

  @Test
  public void timestampStartLessThanTimestampFinish() {
    new LoadTestResult(100, 101, true);
  }
}


