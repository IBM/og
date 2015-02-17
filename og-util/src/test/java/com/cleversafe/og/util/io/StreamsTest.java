/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.util.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Data;

public class StreamsTest {
  private Body body;

  @Before
  public void before() {
    this.body = mock(Body.class);
  }

  @Test(expected = NullPointerException.class)
  public void nullBody() {
    Streams.create(null);
  }

  @Test
  public void createNone() throws IOException {
    when(this.body.getData()).thenReturn(Data.NONE);
    when(this.body.getSize()).thenReturn(0L);
    assertThat(Streams.create(this.body).read(), is(-1));
  }

  @Test
  public void createRandom() throws IOException {
    when(this.body.getData()).thenReturn(Data.RANDOM);
    when(this.body.getSize()).thenReturn(1024L);
    final InputStream in = Streams.create(this.body);
    final byte[] buf = new byte[1024];
    boolean nonZero = false;

    assertThat(in.read(buf), is(1024));
    for (int i = 0; i < buf.length; i++) {
      if (buf[i] != 0)
        nonZero = true;
    }
    assertThat(nonZero, is(true));
  }

  @Test
  public void createZeroes() throws IOException {
    when(this.body.getData()).thenReturn(Data.ZEROES);
    when(this.body.getSize()).thenReturn(1024L);
    final InputStream in = Streams.create(this.body);
    final byte[] buf = new byte[1024];

    assertThat(in.read(buf), is(1024));
    for (int i = 0; i < buf.length; i++) {
      assertThat((int) buf[i], is(0));
    }
  }

  @Test
  public void throttleInputStream() {
    Streams.throttle(mock(InputStream.class), 1);
  }

  @Test
  public void throttleOutputStream() {
    Streams.throttle(mock(OutputStream.class), 1);
  }
}
