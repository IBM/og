/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.util.io;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;

/**
 * An input stream that is backed by a byte array.
 * <P>
 * This class is useful for modeling an arbitrarily long source stream of bytes.
 * 
 * @since 1.0
 */
public class InfiniteInputStream extends InputStream {
  private final byte[] buf;
  private int cursor;
  private int markLocation;

  /**
   * Constructs an infinite input stream, using the provided byte array as its source of data.
   * <p>
   * Note: this class does not perform a defensive copy of the provided byte array, so callers must
   * take care not to modify the byte array after construction of this input stream.
   * 
   * @param buf the byte array to use as a data source for this input stream
   * @throws NullPointerException if buf is null
   * @throws IllegalArgumentException if buf length is zero
   */
  public InfiniteInputStream(final byte[] buf) {
    this.buf = checkNotNull(buf);
    checkArgument(buf.length > 0, "buf length must be > 0 [%s]", buf.length);
    this.cursor = 0;
    this.markLocation = 0;
  }

  @Override
  public int read() {
    return this.buf[updateCursor(1)];
  }

  @Override
  public int read(final byte[] b) {
    return this.read(b, 0, b.length);
  }

  @Override
  public int read(final byte[] b, final int off, final int len) {
    checkNotNull(b);
    if (off < 0 || len < 0 || len > b.length - off) {
      throw new IndexOutOfBoundsException();
    } else if (len == 0) {
      return 0;
    }

    int copied = 0;
    while (copied < len) {
      final int toCopy = Math.min(this.buf.length - this.cursor, len - copied);
      System.arraycopy(this.buf, this.cursor, b, off + copied, toCopy);
      updateCursor(toCopy);
      copied += toCopy;
    }

    return len;
  }

  @Override
  public int available() {
    return Integer.MAX_VALUE;
  }

  @Override
  public void mark(final int readlimit) {
    this.markLocation = this.cursor;
  }

  @Override
  public void reset() {
    this.cursor = this.markLocation;
  }

  @Override
  public boolean markSupported() {
    return true;
  }

  private int updateCursor(final int amount) {
    final int oldCursor = this.cursor;
    this.cursor = (this.cursor + amount) % this.buf.length;
    return oldCursor;
  }

  @Override
  public String toString() {
    return "InfiniteInputStream []";
  }
}
