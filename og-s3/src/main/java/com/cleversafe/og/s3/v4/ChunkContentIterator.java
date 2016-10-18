/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */
package com.cleversafe.og.s3.v4;

class ChunkContentIterator {

  private final byte[] signedChunk;
  private int pos;

  public ChunkContentIterator(final byte[] signedChunk) {
    this.signedChunk = signedChunk;
  }

  public boolean hasNext() {
    return this.pos < this.signedChunk.length;
  }

  public int read(final byte[] output, final int offset, final int length) {
    if (length == 0) {
      return 0;
    }
    if (!hasNext()) {
      return -1;
    }
    final int remaingBytesNum = this.signedChunk.length - this.pos;
    final int bytesToRead = Math.min(remaingBytesNum, length);
    System.arraycopy(this.signedChunk, this.pos, output, offset, bytesToRead);
    this.pos += bytesToRead;
    return bytesToRead;
  }
}
