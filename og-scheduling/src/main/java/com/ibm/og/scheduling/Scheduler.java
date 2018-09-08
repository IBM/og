/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.scheduling;

/**
 * A scheduler allows for a number of calls to {@link #schedule} at a configured rate
 * 
 * @since 1.0
 */
public interface Scheduler {
  /**
   * Blocks until permitted to continue
   */
  void schedule();
}
