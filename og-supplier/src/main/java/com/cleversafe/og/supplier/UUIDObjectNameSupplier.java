/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.supplier;

import java.util.Map;
import java.util.UUID;

import com.cleversafe.og.http.Headers;
import com.google.common.base.Function;

/**
 * An object name supplier which generates uuid-like object names for write, in a format similar to
 * SOH object names
 * 
 * @since 1.0
 */
public class UUIDObjectNameSupplier implements Function<Map<String, String>, String> {

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
  public String apply(Map<String, String> context) {
    String objectName = UUID.randomUUID().toString().replace("-", "") + "0000";
    context.put(Headers.X_OG_OBJECT_NAME, objectName);

    return objectName;
  }

  @Override
  public String toString() {
    return "UUIDObjectNameSupplier []";
  }
}
