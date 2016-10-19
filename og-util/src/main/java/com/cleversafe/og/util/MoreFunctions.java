/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

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

    @Override
    public String toString() {
      return String.format("SupplierFunction [%s]", this.supplier);
    }
  }

  /**
   * Creates a function which returns its value by looking up a key in an input map
   * 
   * @param key the lookup key
   * @return the value stored for the key in the input map
   * @throws NullPointerException if value is null
   */
  public static Function<Map<String, String>, String> keyLookup(final String key) {
    return new Function<Map<String, String>, String>() {
      @Override
      public String apply(final Map<String, String> input) {
        return checkNotNull(input.get(key));
      }

      @Override
      public String toString() {
        return String.format("KeyLookupFunction [%s]", key);
      }
    };
  }
}
