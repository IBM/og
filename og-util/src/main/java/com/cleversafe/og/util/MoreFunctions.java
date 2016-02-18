/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.util;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Supplier;

/**
 * Utility class for creating and manipulating {@code Function} instances
 * 
 * @since 1.0
 */
public class MoreFunctions {
  private MoreFunctions() {}

  /**
   * Creates a function which wraps an underlying supplier
   * 
   * @param supplier supplier to wrap
   * @return a function instance which delegates to an underlying supplier
   */
  public static <O, T> Function<O, T> forSupplier(final Supplier<T> supplier) {
    return new SupplierFunction<O, T>(supplier);
  }

  private static class SupplierFunction<O, T> implements Function<O, T> {

    private final Supplier<T> supplier;

    private SupplierFunction(final Supplier<T> supplier) {
      this.supplier = checkNotNull(supplier);
    }

    @Override
    public T apply(@Nullable final O input) {
      return this.supplier.get();
    }
  }
}
