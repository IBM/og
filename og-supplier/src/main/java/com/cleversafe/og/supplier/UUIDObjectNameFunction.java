/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.supplier;

import java.util.Map;
import java.util.UUID;

import com.cleversafe.og.util.Context;
import com.google.common.base.Function;

/**
 * A function which generates uuid-like object names for write, in a format similar to SOH object
 * names
 * 
 * @since 1.0
 */
public class UUIDObjectNameFunction implements Function<Map<String, String>, String> {

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
    final String objectName = UUID.randomUUID().toString().replace("-", "") + "0000";
    context.put(Context.X_OG_OBJECT_NAME, objectName);

    return objectName;
  }

  @Override
  public String toString() {
    return "UUIDObjectNameFunction []";
  }
}
