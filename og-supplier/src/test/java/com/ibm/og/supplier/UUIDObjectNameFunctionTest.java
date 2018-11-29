/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Map;

import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

public class UUIDObjectNameFunctionTest {
  @Test
  public void uuidObjectNameSupplier() {
    final Function<Map<String, String>, String> s = new UUIDObjectNameFunction(false);
    final Map<String, String> context = Maps.newHashMap();
    assertThat(s.apply(context), is(not(s.apply(context))));
  }
}
