//
// Copyright (C) 2005-2011 Cleversafe, Inc. All rights reserved.
//
// Contact Information:
// Cleversafe, Inc.
// 222 South Riverside Plaza
// Suite 1700
// Chicago, IL 60606, USA
//
// licensing@cleversafe.com
//
// END-OF-HEADER
//
// -----------------------
// @author: rveitch
//
// Date: Apr 6, 2014
// ---------------------

package com.cleversafe.og.supplier;

import java.util.UUID;

import com.google.common.base.Supplier;

public class UUIDObjectNameSupplier implements Supplier<String> {
  @Override
  public String get() {
    return UUID.randomUUID().toString().replace("-", "") + "0000";
  }

  @Override
  public String toString() {
    return "UUIDObjectNameSupplier []";
  }
}
