/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.util.io;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import com.ibm.og.api.Body;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

/**
 * A utility class for creating input and output streams
 * 
 * @since 1.0
 */
public class Streams {
  public static final int REPEAT_LENGTH = 1024;
  private static final byte[] ZERO_BUF = new byte[REPEAT_LENGTH];
  private static final InputStream NONE_INPUTSTREAM = new InputStream() {
    @Override
    public int read() {
      return -1;
    }

    @Override
    public boolean markSupported() {
      return true;
    }

    @Override
    public void reset() {}
  };

  private Streams() {}

  /**
   * Creates an input stream from the provided body description. The size of this stream and its
   * data are determined by the provided body's size and type, respectively.
   * 
   * @param body the description of an body
   * @return an input stream instance
   */
  public static InputStream create(final Body body) {
    checkNotNull(body);
    switch (body.getDataType()) {
      case NONE:
        return NONE_INPUTSTREAM;
      case ZEROES:
        return create(ZERO_BUF, body.getSize());
      case CUSTOM:
      case FILE:
        return create(body.getData(), body.getSize());
      default:
        return create(createRandomBuffer(body.getRandomSeed()), body.getSize());
    }
  }

  private static InputStream create(final byte[] buf, final long size) {
    return ByteStreams.limit(new InfiniteInputStream(buf), size);
  }

  private static byte[] createRandomBuffer(final long seed) {
    final byte[] buf = new byte[REPEAT_LENGTH];
    new Random(seed).nextBytes(buf);
    return buf;
  }

  /**
   * Creates an input stream which is throttled with a maximum throughput
   * 
   * @param in the backing input stream to throttle
   * @param bytesPerSecond the maximum throughput this input stream is allowed to read at
   * @return an instance of an input stream, throttled with a maximum rate of {@code bytesPerSecond}
   * @throws IllegalArgumentException if bytesPerSecond is negative or zero
   */
  public static InputStream throttle(final InputStream in, final long bytesPerSecond) {
    return new ThrottledInputStream(in, bytesPerSecond);
  }

  /**
   * Creates an output stream which is throttled with a maximum throughput
   * 
   * @param out the backing output stream to throttle
   * @param bytesPerSecond the maximum throughput this input stream is allowed to write at
   * @return an instance of an output stream, throttled with a maximum rate of
   *         {@code bytesPerSecond}
   * @throws IllegalArgumentException if bytesPerSecond is negative or zero
   */
  public static OutputStream throttle(final OutputStream out, final long bytesPerSecond) {
    return new ThrottledOutputStream(out, bytesPerSecond);
  }
}
