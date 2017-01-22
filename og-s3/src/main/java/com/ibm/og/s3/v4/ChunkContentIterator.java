/*
 * Copyright 2014-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except
 * in compliance with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.ibm.og.s3.v4;

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
