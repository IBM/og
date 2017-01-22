/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.http;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.ibm.og.api.Body;
import com.ibm.og.api.DataType;

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

  /**
   * Creates a body instance representing a body with custom data
   *
   * @param size the size of the body
   * @return a custom defined body instance
   * @throws IllegalArgumentException if size is negative
   */
  public static Body custom(final long size, String content) {
    return create(DataType.CUSTOM, size, content);
  }

  private static Body create(final DataType data, final long size) {
    checkNotNull(data);
    checkArgument(size >= 0, "size must be >= 0 [%s]", size);

    return new BodyImpl(System.nanoTime(), size, data, null);
  }

  private static Body create(final DataType data, final long size, String content) {
    checkNotNull(data);
    checkArgument(size >= 0, "size must be >= 0 [%s]", size);

    return new BodyImpl(System.nanoTime(), size, data, content);
  }

  private static class BodyImpl implements Body {

    private final long seed;
    private final long size;
    private final DataType dataType;
    private final String content;

    public BodyImpl(final long seed, final long size, final DataType dataType, String content) {
      // Force the seed to zero for non random data so that it won't affect hashCode() and equals()
      this.seed = dataType.equals(DataType.RANDOM) ? seed : 0;
      this.size = size;
      this.dataType = dataType;
      this.content = content;
    }

    @Override
    public DataType getDataType() {
      return this.dataType;
    }

    @Override
    public long getRandomSeed() {
      return this.seed;
    }

    @Override
    public long getSize() {
      return this.size;
    }

    @Override
    public String getContent() { return this.content; }

    @Override
    public String toString() {
      return "BodyImpl [seed=" + this.seed + ", size=" + this.size + ", dataType=" + this.dataType
          + "]";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((this.dataType == null) ? 0 : this.dataType.hashCode());
      result = prime * result + (int) (this.seed ^ (this.seed >>> 32));
      result = prime * result + (int) (this.size ^ (this.size >>> 32));
      return result;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final BodyImpl other = (BodyImpl) obj;
      if (this.dataType != other.dataType) {
        return false;
      }
      if (this.seed != other.seed) {
        return false;
      }
      if (this.size != other.size) {
        return false;
      }
      return true;
    }

  }
}
