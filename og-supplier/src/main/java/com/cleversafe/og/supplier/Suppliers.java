/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.List;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

/**
 * A utility class for creating supplier instances
 * 
 * @since 1.0
 */
public class Suppliers {
  private Suppliers() {}

  /**
   * Creates a supplier that always returns the same value
   * 
   * @param value the value to supply
   * @return a supply which always returns the same value
   * @throws NullPointerException if value is null
   */
  public static <T> Supplier<T> of(final T value) {
    checkNotNull(value);
    return new Supplier<T>() {
      @Override
      public T get() {
        return value;
      }

      @Override
      public String toString() {
        return value.toString();
      }
    };
  }

  /**
   * Creates a supplier that returns values in a cycle
   * 
   * @param values the values to supply
   * @return a supplier which supplies values in a cycle
   * @throws NullPointerException if values is null or contains null elements
   */
  public static <T> Supplier<T> cycle(final List<T> values) {
    final List<T> copy = ImmutableList.copyOf(values);
    checkArgument(!copy.isEmpty(), "values must not be empty");
    final Iterator<T> it = Iterators.cycle(copy);
    return new Supplier<T>() {
      @Override
      public T get() {
        return it.next();
      }

      @Override
      public String toString() {
        return String.format("cycle %s", copy);
      }
    };
  }

  /**
   * Creates a random supplier builder
   * 
   * @return a random supplier builder
   */
  public static <T> RandomSupplier.Builder<T> random() {
    return new RandomSupplier.Builder<T>();
  }
}
