/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.supplier;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Supplier;

/**
 * A supplier which remembers the last value it supplied
 * 
 * @param <T> the type of value to supply
 * @since 1.0
 */
public class CachingSupplier<T> implements Supplier<T> {
  private final Supplier<T> supplier;
  private T cachedValue;

  /**
   * Constructs a supplier using the provided base supplier
   * 
   * @param supplier the base supplier to cache values for
   */
  public CachingSupplier(final Supplier<T> supplier) {
    this.supplier = checkNotNull(supplier);
  }

  @Override
  public T get() {
    this.cachedValue = this.supplier.get();
    return this.cachedValue;
  }

  /**
   * Returns the most recently supplied value
   * 
   * @return the most recently supplied value, or null if {@code get} has never been called
   */
  public T getCachedValue() {
    return this.cachedValue;
  }

  @Override
  public String toString() {
    return String.format("CachingSupplier [%s]", this.supplier);
  }
}
