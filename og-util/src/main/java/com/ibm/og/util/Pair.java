/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.util;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A key-value pair
 * 
 * @param <K> the type of key
 * @param <V> the type of value
 * @since 1.0
 */
public class Pair<K, V> {
  private final K key;
  private final V value;

  private Pair(final K key, final V value) {
    this.key = checkNotNull(key);
    this.value = checkNotNull(value);
  }

  /**
   * Constructs a pair instance
   * 
   * @param key the key for this pair
   * @param value the value for this pair
   * @throws NullPointerException of key or value are null
   */
  public static <K, V> Pair<K, V> of(final K key, final V value) {
    return new Pair<K, V>(key, value);
  }

  /**
   * Gets the key for this pair
   * 
   * @return this key
   */
  public K getKey() {
    return this.key;
  }

  /**
   * Gets the value for this pair
   * 
   * @return this value
   */
  public V getValue() {
    return this.value;
  }

  @Override
  public String toString() {
    return String.format("Pair [key=%s, value=%s]", this.key, this.value);
  }
}
