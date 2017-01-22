/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;

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
   * Gets the container suffix of this object
   * 
   * @return the container suffix of this object
   */
  int getContainerSuffix();

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
