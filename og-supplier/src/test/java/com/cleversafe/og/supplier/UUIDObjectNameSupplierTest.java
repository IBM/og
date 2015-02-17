/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.Test;

import com.google.common.base.Supplier;

public class UUIDObjectNameSupplierTest {
  @Test
  public void uuidObjectNameSupplier() {
    final Supplier<String> s = new UUIDObjectNameSupplier();
    assertThat(s.get(), is(not(s.get())));
  }
}
