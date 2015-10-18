/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.test.condition;

import static com.google.common.base.Preconditions.checkArgument;

public class LoadTestResult {
  public final long timestampStart;
  public final long timestampFinish;
  public final boolean success;

  public LoadTestResult(final long timestampStart, final long timestampFinish,
      final boolean success) {
    checkArgument(timestampStart >= 0, "timestampStart must be >= 0 [%s]", timestampStart);
    checkArgument(timestampStart <= timestampFinish,
        "timestampStart must be <= timestampFinish [%s, %s]", timestampStart, timestampFinish);
    this.timestampStart = timestampStart;
    this.timestampFinish = timestampFinish;
    this.success = success;
  }
}
