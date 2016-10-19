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
package com.cleversafe.og.s3.v4;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.AmazonClientException;

class DecodedStreamBuffer {
  private static final Log log = LogFactory.getLog(DecodedStreamBuffer.class);

  private final byte[] bufferArray;
  private final int maxBufferSize;
  private int byteBuffered;
  private int pos = -1;
  private boolean bufferSizeOverflow;

  public DecodedStreamBuffer(final int maxBufferSize) {
    this.bufferArray = new byte[maxBufferSize];
    this.maxBufferSize = maxBufferSize;
  }

  public void buffer(final byte read) {
    this.pos = -1;
    if (this.byteBuffered >= this.maxBufferSize) {
      if (log.isDebugEnabled()) {
        log.debug("Buffer size " + this.maxBufferSize + " has been exceeded and the input stream "
            + "will not be repeatable. Freeing buffer memory");
      }
      this.bufferSizeOverflow = true;
    } else {
      this.bufferArray[this.byteBuffered++] = read;
    }
  }

  public void buffer(final byte[] src, final int srcPos, final int length) {
    this.pos = -1;
    if (this.byteBuffered + length > this.maxBufferSize) {
      if (log.isDebugEnabled()) {
        log.debug("Buffer size " + this.maxBufferSize + " has been exceeded and the input stream "
            + "will not be repeatable. Freeing buffer memory");
      }
      this.bufferSizeOverflow = true;
    } else {
      System.arraycopy(src, srcPos, this.bufferArray, this.byteBuffered, length);
      this.byteBuffered += length;
    }
  }

  public boolean hasNext() {
    return (this.pos != -1) && (this.pos < this.byteBuffered);
  }

  public byte next() {
    return this.bufferArray[this.pos++];
  }

  public void startReadBuffer() {
    if (this.bufferSizeOverflow) {
      throw new AmazonClientException("The input stream is not repeatable since the buffer size "
          + this.maxBufferSize + " has been exceeded.");
    }
    this.pos = 0;
  }
}
