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

import java.util.Map;

import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

public class UUIDObjectNameSupplierTest {
  @Test
  public void uuidObjectNameSupplier() {
    final Function<Map<String, String>, String> s = new UUIDObjectNameSupplier();
    final Map<String, String> context = Maps.newHashMap();
    assertThat(s.apply(context), is(not(s.apply(context))));
  }
}
