/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.test.condition;

/**
 * A condition which, when triggered, indicates the OG load test should be stopped
 * 
 * @since 1.0
 */
public interface TestCondition {
  /**
   * Returns whether this condition is triggered or not
   * 
   * @return the state of this condition
   */
  boolean isTriggered();
}
