/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.supplier;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.ibm.og.object.ObjectManager;
import com.ibm.og.object.ObjectMetadata;
import com.ibm.og.util.Context;
import com.google.common.base.Function;

/**
 * A function which generates object names for read from a provided {@code ObjectManager}
 * 
 * @since 1.0
 */
public class ReadObjectNameFunction implements Function<Map<String, String>, String> {
  private final ObjectManager objectManager;

  /**
   * Creates an instance
   * 
   * @param objectManager the object manager to draw object names from
   * @throws NullPointerException if objectManager is null
   */
  public ReadObjectNameFunction(final ObjectManager objectManager) {
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
    final ObjectMetadata objectMetadata = this.objectManager.get();
    context.put(Context.X_OG_OBJECT_NAME, objectMetadata.getName());
    context.put(Context.X_OG_OBJECT_SIZE, String.valueOf(objectMetadata.getSize()));
    context.put(Context.X_OG_CONTAINER_SUFFIX, String.valueOf(objectMetadata.getContainerSuffix()));
    context.put(Context.X_OG_LEGAL_HOLD_SUFFIX, String.valueOf(objectMetadata.getNumberOfLegalHolds()));
    //context.put(Context.X_OG_LEGAL_HOLD_PREFIX, "LegalHold");
    context.put(Context.X_OG_OBJECT_RETENTION, String.valueOf(objectMetadata.getRetention()));

    return objectMetadata.getName();
  }

  @Override
  public String toString() {
    return "ReadObjectNameFunction []";
  }
}
