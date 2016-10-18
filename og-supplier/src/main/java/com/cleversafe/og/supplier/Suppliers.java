/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

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
   * A supplier which chooses a long value in a cycle
   * 
   * @since 1.0
   */
  public static Supplier<Long> cycle(final long minValue, final long maxValue) {
    checkArgument(minValue >= 0, "minValue must be >= 0 [%s]", minValue);
    checkArgument(minValue <= maxValue, "minValue must be <= maxValue, [%s, %s]", minValue,
        maxValue);

    return new Supplier<Long>() {
      // FIXME simplify this implementation
      long currentValue = minValue - 1;

      @Override
      public Long get() {
        this.currentValue = (this.currentValue + 1) % (maxValue + 1);
        if (this.currentValue == 0) {
          this.currentValue = minValue;
        }

        return this.currentValue;
      }

      @Override
      public String toString() {
        return String.format("cycle [minValue=%s, maxValue=%s]", minValue, maxValue);
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

  /**
   * A supplier which chooses a random long to supply
   * 
   * @since 1.0
   */
  public static Supplier<Long> random(final long minValue, final long maxValue) {
    checkArgument(minValue >= 0, "minValue must be >= 0 [%s]", minValue);
    checkArgument(minValue <= maxValue, "minValue must be <= maxValue, [%s, %s]", minValue,
        maxValue);
    final Random random = new Random();
    return new Supplier<Long>() {

      @Override
      public Long get() {
        return minValue + Math.round(random.nextDouble() * (maxValue - minValue));
      }

      @Override
      public String toString() {
        return String.format("random [minValue=%s, maxValue=%s]", minValue, maxValue);
      }
    };
  }
}
