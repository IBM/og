/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.object;


/**
 * A collection of objects and their corresponding metadata
 * 
 * @since 1.0
 */
public interface ObjectManager {
  /**
   * Adds an object under management
   * 
   * @param objectMetadata the associated metadata for this object
   */
  void add(ObjectMetadata objectMetadata);

  /**
   * Selects an existing object name under management. Callers must call {@code getComplete } when
   * finished with the object returned by this method
   * 
   * @return an available object name for reading
   */
  ObjectMetadata get();

  /**
   * Informs this object manager that the caller is done reading this object
   * 
   * @param objectMetadata the object that the caller has finished using
   */
  void getComplete(ObjectMetadata objectMetadata);

  /**
   * Removes an existing object name from management
   * 
   * @return an object currently under management
   */
  ObjectMetadata remove();

  /**
   * Shuts down this object manager
   */
  void shutdown();
}
