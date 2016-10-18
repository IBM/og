/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.util;

/**
 * An object that emits values based on a configured statistical distribution.
 * 
 * @since 1.0
 */
public interface Distribution {
  /**
   * 
   * @return the configured average
   */
  double getAverage();

  /**
   * 
   * @return the next value as determined by the configured distribution
   */
  double nextSample();
}
