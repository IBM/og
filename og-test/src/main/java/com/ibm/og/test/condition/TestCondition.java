/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.test.condition;

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
