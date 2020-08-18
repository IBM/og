/* Copyright (c) IBM Corporation 2020. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.supplier;

import com.google.common.base.Function;
import com.ibm.og.object.ObjectManager;
import com.ibm.og.object.ObjectMetadata;
import com.ibm.og.util.Context;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A function which generates object names for deletion from a provided {@code ObjectManager}
 * 
 * @since 1.0
 */
public class DeleteTagsObjectNameFunction implements Function<Map<String, String>, String> {
  private final ObjectManager objectManager;

  /**
   * Creates an instance
   *
   * @param objectManager the object manager to draw object names from
   * @throws NullPointerException if objectManager is null
   */
  public DeleteTagsObjectNameFunction(final ObjectManager objectManager) {
    this.objectManager = checkNotNull(objectManager);
  }

  /**
   * Creates and returns an object name. Additionally, inserts the following entries into the
   * context:
   * <ul>
   * <li>Headers.X_OG_OBJECT_NAME
   * <li>Headers.X_OG_OBJECT_SIZE</li>
   * <li>Headers.X_OG_CONTAINER_SUFFIX</li>
   * </ul>
   * 
   * @param context a request creation context for storing metadata to be used by other functions
   */
  @Override
  public String apply(final Map<String, String> context) {
    final ObjectMetadata objectMetadata = this.objectManager.removeForUpdate();
    context.put(Context.X_OG_OBJECT_NAME, objectMetadata.getName());
    context.put(Context.X_OG_OBJECT_SIZE, String.valueOf(objectMetadata.getSize()));
    context.put(Context.X_OG_CONTAINER_SUFFIX, String.valueOf(objectMetadata.getContainerSuffix()));

    return objectMetadata.getName();
  }

  @Override
  public String toString() {
    return "DeleteObjectNameFunction []";
  }
}
