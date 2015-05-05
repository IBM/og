/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.supplier;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.cleversafe.og.http.Headers;
import com.cleversafe.og.object.ObjectManager;
import com.cleversafe.og.object.ObjectMetadata;
import com.google.common.base.Function;

/**
 * An object name supplier which generates object names for deletion from a provided
 * {@code ObjectManager}
 * 
 * @since 1.0
 */
public class DeleteObjectNameSupplier implements Function<Map<String, String>, String> {
  private final ObjectManager objectManager;

  /**
   * Creates an instance
   * 
   * @param objectManager the object manager to draw object names from
   * @throws NullPointerException if objectManager is null
   */
  public DeleteObjectNameSupplier(final ObjectManager objectManager) {
    this.objectManager = checkNotNull(objectManager);
  }

  /**
   * Creates and returns an object name. Additionally, inserts the following entries into the
   * context:
   * <ul>
   * <li>Headers.X_OG_OBJECT_NAME
   * </ul>
   * 
   * @param context a request creation context for storing metadata to be used by other functions
   */
  @Override
  public String apply(final Map<String, String> context) {
    final ObjectMetadata objectMetadata = this.objectManager.remove();
    context.put(Headers.X_OG_OBJECT_NAME, objectMetadata.getName());
    context.put(Headers.X_OG_OBJECT_SIZE, String.valueOf(objectMetadata.getSize()));
    context.put(Headers.X_OG_CONTAINER_SUFFIX, String.valueOf(objectMetadata.getContainerSuffix()));

    return objectMetadata.getName();
  }

  @Override
  public String toString() {
    return "DeleteObjectNameSupplier []";
  }
}
