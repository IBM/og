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

public class DeleteObjectNameSupplier implements Function<Map<String, String>, String> {
  private final ObjectManager objectManager;

  public DeleteObjectNameSupplier(final ObjectManager objectManager) {
    this.objectManager = checkNotNull(objectManager);
  }

  @Override
  public String apply(Map<String, String> context) {
    ObjectMetadata objectMetadata = this.objectManager.getNameForDelete();
    context.put(Headers.X_OG_OBJECT_NAME, objectMetadata.getName());

    return objectMetadata.getName();
  }

  @Override
  public String toString() {
    return "DeleteObjectNameSupplier []";
  }
}
