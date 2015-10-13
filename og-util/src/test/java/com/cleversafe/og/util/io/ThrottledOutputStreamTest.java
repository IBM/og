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

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

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
public class ThrottledOutputStreamTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private OutputStream out;

  @Before
  public void before() {
    this.out = mock(OutputStream.class);
  }

  @DataProvider
  public static Object[][] provideInvalidThrottleOutputStream() {
    final OutputStream out = mock(OutputStream.class);
    return new Object[][] {{null, 1, NullPointerException.class},
        {out, -1, IllegalArgumentException.class}, {out, 0, IllegalArgumentException.class}};
  }

  @Test
  @UseDataProvider("provideInvalidThrottleOutputStream")
  public void invalidOutputStream(final OutputStream out, final long bytesPerSecond,
      final Class<Exception> expectedException) {
    this.thrown.expect(expectedException);
    Streams.throttle(out, bytesPerSecond);
  }

  @Test
  public void positiveBytesPerSecond() {
    new ThrottledOutputStream(this.out, 1);
  }

  @Test
  public void writeOneByteAtATime() throws IOException {
    final OutputStream throttled = new ThrottledOutputStream(this.out, 1000);
    final long timestampStart = System.nanoTime();
    for (int i = 0; i < 100; i++) {
      throttled.write(1);
    }
    final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart);
    final long delta = Math.abs(duration - 100);
    // within 10% of expected duration (100 milliseconds)
    assertThat(delta, lessThan(10L));
  }

  @Test
  public void write() throws IOException {
    final OutputStream throttled = new ThrottledOutputStream(this.out, 1000);
    final byte[] buf = new byte[100];
    final long timestampStart = System.nanoTime();
    throttled.write(buf);
    final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart);
    final long delta = Math.abs(duration - 100);
    // within 10% of expected duration (100 milliseconds)
    assertThat(delta, lessThan(10L));
  }
}
