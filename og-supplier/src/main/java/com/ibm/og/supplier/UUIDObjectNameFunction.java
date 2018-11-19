/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.supplier;

import java.util.Map;
import java.util.UUID;

import com.ibm.og.util.Context;
import com.google.common.base.Function;

/**
 * A function which generates uuid-like object names for write, in a format similar to SOH object
 * names
 * 
 * @since 1.0
 */
public class UUIDObjectNameFunction implements Function<Map<String, String>, String> {

  private boolean octalNamingMode = false;

  public UUIDObjectNameFunction(boolean octalNamingMode) {
    this.octalNamingMode = octalNamingMode;
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
    if (!this.octalNamingMode) {
      final String objectName = UUID.randomUUID().toString().replace("-", "") + "0000";
      context.put(Context.X_OG_OBJECT_NAME, objectName);

      return objectName;

    } else {
      UUID uuid = UUID.randomUUID();
      long msl = uuid.getMostSignificantBits();
      long lsl = uuid.getLeastSignificantBits();
      msl = msl & 0x7777777777777777L;
      lsl = lsl & 0x7777777777777777L;
      UUID uuid2 = new UUID(lsl, msl);
      final String objectName = uuid2.toString().replace("-", "") + "0000";
      context.put(Context.X_OG_OBJECT_NAME, objectName);

      return objectName;
    }
  }

  @Override
  public String toString() {
    return "UUIDObjectNameFunction []";
  }
}
