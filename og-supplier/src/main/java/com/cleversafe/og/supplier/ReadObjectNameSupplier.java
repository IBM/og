/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.supplier;

import static com.google.common.base.Preconditions.checkNotNull;

import com.cleversafe.og.object.ObjectManager;
import com.google.common.base.Supplier;

public class ReadObjectNameSupplier implements Supplier<String> {
  private final ObjectManager objectManager;

  public ReadObjectNameSupplier(final ObjectManager objectManager) {
    this.objectManager = checkNotNull(objectManager);
  }

  @Override
  public String get() {
    return this.objectManager.acquireNameForRead().getName();
  }

  @Override
  public String toString() {
    return "ReadObjectNameSupplier []";
  }
}
