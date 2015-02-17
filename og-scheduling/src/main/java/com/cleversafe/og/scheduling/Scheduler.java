/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.scheduling;

/**
 * A scheduler allows for a number of calls to {@link #waitForNext} at a configured rate
 * 
 * @since 1.0
 */
public interface Scheduler {
  /**
   * Blocks until permitted to continue
   */
  void waitForNext();
}
