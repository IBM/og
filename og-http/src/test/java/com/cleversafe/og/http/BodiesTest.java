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
// Date: Jun 28, 2014
// ---------------------

package com.cleversafe.og.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Data;

public class BodiesTest {
  @Test
  public void none() {
    final Body body = Bodies.none();
    assertThat(body.getData(), is(Data.NONE));
    assertThat(body.getSize(), is(0L));
  }

  @Test(expected = IllegalArgumentException.class)
  public void randomNegativeSize() {
    Bodies.random(-1);
  }

  @Test
  public void random() {
    final Body body = Bodies.random(1);
    assertThat(body.getData(), is(Data.RANDOM));
    assertThat(body.getSize(), is(1L));
  }

  @Test(expected = IllegalArgumentException.class)
  public void zeroesNegativeSize() {
    Bodies.zeroes(-1);
  }

  @Test
  public void zeroes() {
    final Body body = Bodies.zeroes(1);
    assertThat(body.getData(), is(Data.ZEROES));
    assertThat(body.getSize(), is(1L));
  }
}
