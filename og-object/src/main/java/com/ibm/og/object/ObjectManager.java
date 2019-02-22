/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;


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
   * Selects an existing object name under management. Callers must call {@code getComplete } when
   * finished with the object returned by this method. Object will only be returned it is not already
   * obtained by calling this method or get()
   *
   * @return an available object name for reading
   */
  ObjectMetadata getOnce();

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
   * Removes an existing object name from management temporarily.
   * When the response for operation is received, object can either
   * updated back for management or deleted
   *
   * @return an object currently under management
   */
  ObjectMetadata removeForUpdate();
  /**
   * Removes the specified object from management
   *
   * @return void
   */
  ObjectMetadata removeObject(ObjectMetadata objectMetadata);

  /**
   * Adds the updated object into management
   *
   * @return void
   */
  void updateObject(ObjectMetadata objectMetadata);

  /**
   * Removes the updated (deleted) object from management
   *
   * @return void
   */
  public void removeUpdatedObject(final ObjectMetadata id);

  /**
   * Get the object from the currently updating cache
   *
   * @return ObjectMetadata
   */
  public ObjectMetadata getObjectFromUpdatingCache(final String id);

  /**
   * Remove the object from the currently updating cache by name
   *
   * @return void
   */
  public void removeUpdatedObjectByName(final String name);

  /**
   * Returns the count of objects currently being updated or deleted
   *
   * @return int
   */
  public int getCurrentlyUpdatingCount();

  /**
   * Shuts down this object manager
   */
  void shutdown();
}
