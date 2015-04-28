/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.util;

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
