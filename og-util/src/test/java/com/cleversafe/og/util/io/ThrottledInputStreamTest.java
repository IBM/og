/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.util.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.DataType;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@SuppressWarnings("resource")
@RunWith(DataProviderRunner.class)
@Ignore
public class ThrottledInputStreamTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private Body body;

  @Before
  public void before() {
    this.body = mock(Body.class);
    when(this.body.getDataType()).thenReturn(DataType.ZEROES);
    when(this.body.getSize()).thenReturn(10000L);
  }

  @DataProvider
  public static Object[][] provideInvalidThrottleInputStream() {
    final InputStream in = mock(InputStream.class);
    return new Object[][] {{null, 1, NullPointerException.class},
        {in, -1, IllegalArgumentException.class}, {in, 0, IllegalArgumentException.class}};
  }

  @Test
  @UseDataProvider("provideInvalidThrottleInputStream")
  public void invalidInputStream(final InputStream in, final long bytesPerSecond,
      final Class<Exception> expectedException) {
    this.thrown.expect(expectedException);
    Streams.throttle(in, bytesPerSecond);
  }

  @Test
  public void positiveBytesPerSecond() {
    new ThrottledInputStream(mock(InputStream.class), 1);
  }

  @Test
  public void readOneByteAtATime() throws IOException {
    final InputStream in = new ThrottledInputStream(Streams.create(this.body), 1000);
    final long timestampStart = System.nanoTime();
    for (int i = 0; i < 100; i++) {
      in.read();
    }
    final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart);
    final long delta = Math.abs(duration - 100);
    // within 15% of expected duration (100 milliseconds)
    assertThat(delta, lessThan(15L));
  }

  @Test
  public void read() throws IOException {
    final InputStream in = new ThrottledInputStream(Streams.create(this.body), 1000);
    final long timestampStart = System.nanoTime();
    in.read(new byte[100]);
    final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart);
    final long delta = Math.abs(duration - 100);
    // within 15% of expected duration (100 milliseconds)
    assertThat(delta, lessThan(15L));
  }
}
