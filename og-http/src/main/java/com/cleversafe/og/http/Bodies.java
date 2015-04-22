/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.http;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.DataType;

/**
 * A utility class for creating body instances
 * 
 * @since 1.0
 */
public class Bodies {
  private static final Body NONE_BODY = Bodies.create(DataType.NONE, 0);

  private Bodies() {}

  /**
   * Creates a body instance representing no body
   * 
   * @return an body instance
   */
  public static Body none() {
    return NONE_BODY;
  }

  /**
   * Creates a body instance representing a body with random data
   * 
   * @param size the size of the body
   * @return a random body instance
   * @throws IllegalArgumentException if size is negative
   */
  public static Body random(final long size) {
    return create(DataType.RANDOM, size);
  }

  /**
   * Creates a body instance representing a body with zeroes for data
   * 
   * @param size the size of the body
   * @return a zero based body instance
   * @throws IllegalArgumentException if size is negative
   */
  public static Body zeroes(final long size) {
    return create(DataType.ZEROES, size);
  }

  private static Body create(final DataType data, final long size) {
    checkNotNull(data);
    checkArgument(size >= 0, "size must be >= 0 [%s]", size);

    return new Body() {
      @Override
      public DataType getDataType() {
        return data;
      }

      @Override
      public long getSize() {
        return size;
      }

      @Override
      public String toString() {
        return String.format("Body [data=%s, size=%s]", data, size);
      }
    };
  }
}
