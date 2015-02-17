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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@SuppressWarnings("resource")
@RunWith(DataProviderRunner.class)
public class InfiniteInputStreamTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private static final int BUF_LENGTH = 5;
  private byte[] buf;
  private InfiniteInputStream in;

  @Before
  public void before() {
    this.buf = new byte[BUF_LENGTH];
    for (int i = 0; i < this.buf.length; i++) {
      this.buf[i] = (byte) i;
    }
    this.in = new InfiniteInputStream(this.buf);
  }

  @Test(expected = NullPointerException.class)
  public void nullBuffer() {
    new InfiniteInputStream(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyBuffer() {
    new InfiniteInputStream(new byte[0]);
  }

  @Test
  public void readOneByteAtATime() {
    for (int i = 0; i < 2 * this.buf.length; i++) {
      assertThat(this.in.read(), is((int) this.buf[i % this.buf.length]));
      assertThat(this.in.available(), is(Integer.MAX_VALUE));
    }
  }

  @Test(expected = NullPointerException.class)
  public void readNullBuffer() {
    this.in.read(null);
  }

  @DataProvider
  public static Object[][] provideInvalidRead() {
    final byte[] buf = new byte[1];
    return new Object[][] { {null, 0, 1, NullPointerException.class},
        {buf, -1, 1, IndexOutOfBoundsException.class},
        {buf, buf.length, 1, IndexOutOfBoundsException.class},
        {buf, 0, -1, IndexOutOfBoundsException.class},
        {buf, 0, buf.length + 1, IndexOutOfBoundsException.class},
        {buf, 1, buf.length, IndexOutOfBoundsException.class}};
  }

  @Test
  @UseDataProvider("provideInvalidRead")
  public void invalidRead(final byte[] buf, final int off, final int len,
      final Class<Exception> expectedException) {
    this.thrown.expect(expectedException);
    this.in.read(buf, off, len);
  }

  @DataProvider
  public static Object[][] provideRead() {
    return new Object[][] { {new byte[0]}, {new byte[BUF_LENGTH - 1]}, {new byte[BUF_LENGTH]},
        {new byte[BUF_LENGTH + 1]}};
  }

  @Test
  @UseDataProvider("provideRead")
  public void read(final byte[] readBuffer) {
    assertThat(this.in.read(readBuffer), is(readBuffer.length));

    for (int i = 0; i < readBuffer.length; i++) {
      assertThat(readBuffer[i], is(this.buf[i % this.buf.length]));
      assertThat(this.in.available(), is(Integer.MAX_VALUE));
    }
  }

  @Test
  public void readZeroLength() {
    assertThat(this.in.read(new byte[1], 0, 0), is(0));
  }

  @Test
  public void readPositiveLength() {
    assertThat(this.in.read(new byte[1], 0, 1), is(1));
  }

  @Test
  public void reset() {
    this.in.read();
    this.in.mark(Integer.MAX_VALUE);
    final int r1 = this.in.read();
    this.in.reset();
    assertThat(this.in.read(), is(r1));
  }
}
