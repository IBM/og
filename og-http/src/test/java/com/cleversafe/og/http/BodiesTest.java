/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.DataType;

public class BodiesTest {
  @Test
  public void none() {
    final Body body = Bodies.none();
    assertThat(body.getDataType(), is(DataType.NONE));
    assertThat(body.getSize(), is(0L));
  }

  @Test(expected = IllegalArgumentException.class)
  public void randomNegativeSize() {
    Bodies.random(-1);
  }

  @Test
  public void random() {
    final Body body = Bodies.random(1);
    assertThat(body.getDataType(), is(DataType.RANDOM));
    assertThat(body.getSize(), is(1L));
  }

  @Test(expected = IllegalArgumentException.class)
  public void zeroesNegativeSize() {
    Bodies.zeroes(-1);
  }

  @Test
  public void zeroes() {
    final Body body = Bodies.zeroes(1);
    assertThat(body.getDataType(), is(DataType.ZEROES));
    assertThat(body.getSize(), is(1L));
  }
}
