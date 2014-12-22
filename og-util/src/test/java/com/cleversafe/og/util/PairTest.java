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
// Date: Apr 2, 2014
// ---------------------

package com.cleversafe.og.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

public class PairTest {
  @Test(expected = NullPointerException.class)
  public void nullKey() {
    Pair.of(null, "value");
  }

  @Test(expected = NullPointerException.class)
  public void nullValue() {
    Pair.of("key", null);
  }

  @Test
  public void pair() {
    final Pair<String, String> p = Pair.of("key", "value");
    assertThat(p.getKey(), is("key"));
    assertThat(p.getValue(), is("value"));
  }
}
