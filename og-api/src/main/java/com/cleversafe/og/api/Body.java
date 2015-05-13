/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.api;

/**
 * A description of an http request or response body. Implementations should override hashCode() and
 * equals() to support proper caching for aws v4 auth hashes.
 * 
 * 
 * @since 1.0
 */
public interface Body {
  /**
   * Gets the data type of this body
   * 
   * @return the type of data for this body
   * @see DataType
   */
  DataType getDataType();

  /**
   * @return the seed that will be used to generate the random data for this body. Note that we use
   *         Infinite Streams which only generate Streams.REPEAT_LENGTH random bytes and then repeat
   *         those over and over until we have {@link #getSize()}.
   */
  long getRandomSeed();

  /**
   * Gets the size of this body
   * 
   * @return the size of this body
   */
  long getSize();
}
