/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.util.io;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.common.util.concurrent.RateLimiter;

/**
 * An input stream with a configurable maximum throughput
 * 
 * @since 1.0
 */
public class ThrottledInputStream extends FilterInputStream {
  private final RateLimiter rateLimiter;

  /**
   * Constructs an input stream with a maximum throughput
   * 
   * @param in the backing input stream to read from
   * @param bytesPerSecond the maximum rate at which this input stream can read
   * @throws IllegalArgumentException if bytesPerSecond is negative or zero
   */
  public ThrottledInputStream(final InputStream in, final long bytesPerSecond) {
    super(checkNotNull(in));
    checkArgument(bytesPerSecond > 0, "bytesPerSecond must be > 0 [%s]", bytesPerSecond);
    this.rateLimiter = RateLimiter.create(bytesPerSecond);
  }

  @Override
  public int read() throws IOException {
    final int b = super.read();
    if (b > -1) {
      throttle(1);
    }

    return b;
  }

  @Override
  public int read(final byte[] b) throws IOException {
    return this.read(b, 0, b.length);
  }

  @Override
  public int read(final byte[] b, final int off, final int len) throws IOException {
    final int bytesRead = super.read(b, off, len);
    if (bytesRead > 0) {
      throttle(bytesRead);
    }

    return bytesRead;
  }

  private void throttle(final int bytes) {
    if (bytes == 1) {
      this.rateLimiter.acquire();
    } else if (bytes > 1) {
      // acquire blocks based on previously acquired permits. If multiple bytes read, call
      // acquire twice so throttling occurs even if read is only called once (small files)
      this.rateLimiter.acquire(bytes - 1);
      this.rateLimiter.acquire();
    }
  }

  @Override
  public String toString() {
    return String.format("ThrottledInputStream [in=%s, bytesPerSecond=%s]", this.in,
        this.rateLimiter.getRate());
  }
}
