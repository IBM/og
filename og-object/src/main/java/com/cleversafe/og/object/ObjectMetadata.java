/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.object;

/**
 * Metadata that describes a previously written object.
 * 
 * @since 1.0
 */
public interface ObjectMetadata extends Comparable<ObjectMetadata> {

  /**
   * Gets the name of this object
   * 
   * @return the name of this object
   */
  String getName();

  /**
   * Gets the size of this object
   * 
   * @return the size of this object
   */
  long getSize();

  /**
   * Convert this instance's internal representation into bytes
   * 
   * @return this object name as bytes
   */
  byte[] toBytes();
}
