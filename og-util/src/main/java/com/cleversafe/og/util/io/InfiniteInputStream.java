//
// Cleversafe open-source code header - Version 1.2 - February 15, 2008
//
// Cleversafe Dispersed Storage(TM) is software for secure, private and
// reliable storage of the world's data using information dispersal.
//
// Copyright (C) 2005-2008 Cleversafe, Inc.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
// USA.
//
// Contact Information: Cleversafe, 224 North Desplaines Street, Suite 500
// Chicago IL 60661
// email licensing@cleversafe.org
//
// END-OF-HEADER
// -----------------------
// @author: mmotwani
//
// Date: Dec 19, 2008
// ---------------------

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
  private int mark;

  /**
   * Constructs an infinite input stream, using the provided byte array as its source of data.
   * <p>
   * Note: this class does not perform a defensive copy of the provided byte array, so callers must
   * take care not to modify the byte array after construction of this input stream.
   * 
   * @param buf the byte array to use as a data source for this input stream
   * @throws IllegalArgumentException if buf length is zero
   */
  public InfiniteInputStream(final byte[] buf) {
    this.buf = checkNotNull(buf);
    checkArgument(buf.length > 0, "buf length must be > 0 [%s]", buf.length);
    this.cursor = 0;
    this.mark = 0;
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
    if (off < 0 || len < 0 || len > b.length - off)
      throw new IndexOutOfBoundsException();
    else if (len == 0)
      return 0;

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
    this.mark = this.cursor;
  }

  @Override
  public void reset() {
    this.cursor = this.mark;
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
