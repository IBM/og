/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.test;

import com.google.common.collect.ImmutableList;
import com.ibm.og.test.condition.LoadTestResult;
import org.junit.Test;

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


