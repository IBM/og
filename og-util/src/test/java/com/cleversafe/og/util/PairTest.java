/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

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
