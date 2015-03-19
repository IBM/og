/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.util.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An <code>InputStream</code> implementation that tracks time to first byte.
 */
public class MonitoringInputStream extends FilterInputStream {
  private long firstRead;

  /**
   * Constructs a <code>MonitoringInputStream</code> instance using the provided stream
   * 
   * @param in the stream to wrap
   */
  public MonitoringInputStream(final InputStream in) {
    super(in);
  }

  @Override
  public int read() throws IOException {
    int val = super.read();
    if (this.firstRead == 0) {
      this.firstRead = System.nanoTime();
    }
    return val;
  }

  @Override
  public int read(final byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(final byte[] b, final int off, final int len) throws IOException {
    int size = super.read(b, off, len);
    if (this.firstRead == 0) {
      this.firstRead = System.nanoTime();
    }
    return size;
  }

  /**
   * @return first read timestamp, in nanoseconds
   */
  public long getFirstRead() {
    return this.firstRead;
  }

  @Override
  public String toString() {
    return String.format("MonitoringInputStream [in=%s]", this.in);
  }
}
