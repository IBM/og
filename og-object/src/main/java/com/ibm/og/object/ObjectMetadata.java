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
   *  Gets the number of legal holds
   *  @return the no. of legal holds
   */
  int getNumberOfLegalHolds();

  /**
   *  Gets the retention period
   *  @return the retention period
   */
  int getRetention();

  /**
   * Gets the object version
   *
   * @return the version of the object
   */
  String getVersion();

  /**
   * returns whether the object has a version or not
   *
   * @return boolean that indicates whether the object has version or not
   */
  boolean hasVersion();

  /**
   * Convert this instance's internal representation into bytes
   * 
   * @return this object name as bytes
   */
  byte[] toBytes(boolean withVersionId);
}
