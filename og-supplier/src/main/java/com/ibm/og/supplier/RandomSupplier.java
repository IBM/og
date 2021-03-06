/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Random;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;

/**
 * A supplier which chooses a random value to supply
 * 
 * @param <T> the type of values to supply
 * @since 1.0
 */
public class RandomSupplier<T> implements Supplier<T> {
  private final List<Choice<T>> choices;
  private final Random random;

  private RandomSupplier(final Builder<T> builder) {
    this.choices = checkNotNull(builder.choices);
    checkArgument(!this.choices.isEmpty(), "choices must not be empty");
    this.random = checkNotNull(builder.random);
  }

  private static class Choice<S> {
    private final S value;
    private final Supplier<? extends Number> weight;
    private double currentWeight;

    private Choice(final S choice, final Supplier<? extends Number> weight) {
      this.value = choice;
      this.weight = weight;
      this.currentWeight = 0.0;
    }
  }

  @Override
  public T get() {
    final double totalWeight = getCurrentWeights();
    final double rnd = this.random.nextDouble() * totalWeight;
    double previousWeights = 0.0;

    for (final Choice<T> choice : this.choices) {
      if (rnd < previousWeights + choice.currentWeight) {
        return choice.value;
      }
      previousWeights += choice.currentWeight;
    }
    throw new IllegalStateException("Incorrect weight calculation");
  }

  private double getCurrentWeights() {
    double currentTotalWeight = 0.0;
    for (final Choice<T> choice : this.choices) {
      choice.currentWeight = choice.weight.get().doubleValue();
      currentTotalWeight += choice.currentWeight;
    }
    return currentTotalWeight;
  }

  /**
   * A builder of random choice supplier instances
   * 
   * @param <T> the type of values to add to this builder
   */
  public static class Builder<T> {
    private final List<Choice<T>> choices;
    private Random random;

    /**
     * Constructs a new builder
     */
    public Builder() {
      this.choices = Lists.newArrayList();
      this.random = new Random();
    }

    /**
     * Adds a choice with a default weight of {@code 1.0}
     * 
     * @param choice the choice to add
     * @return this builder
     */
    public Builder<T> withChoice(final T choice) {
      return withChoice(choice, 1.0);
    }

    /**
     * Adds a choice with the provided weight
     * 
     * @param choice the choice to add
     * @param weight the weight to give the choice
     * @return this builder
     * @throws IllegalArgumentException if weight is negative or zero
     */
    public Builder<T> withChoice(final T choice, final double weight) {
      checkArgument(weight > 0.0, "weight must be > 0.0 [%s]", weight);
      return withChoice(choice, Suppliers.of(weight));
    }

    /**
     * Adds a choice with the provided weight, which may be dynamic
     * 
     * @param choice the choice to add
     * @param weight the weight to give the choice, which may be dynamic
     * @return this builder
     */
    public Builder<T> withChoice(final T choice, final Supplier<? extends Number> weight) {
      checkNotNull(choice);
      checkNotNull(weight);
      this.choices.add(new Choice<T>(choice, weight));
      return this;
    }

    /**
     * Configures this builder to use a provided random instance
     * 
     * @param random the random instance to use for value selection
     * @return this builder
     */
    public Builder<T> withRandom(final Random random) {
      this.random = random;
      return this;
    }

    /**
     * Creates a random choice supplier instance
     * 
     * @return a new random choice supplier instance
     * @throws IllegalArgumentException if no choices were added prior to calling this method
     */
    public RandomSupplier<T> build() {
      return new RandomSupplier<T>(this);
    }
  }

  @Override
  public String toString() {
    final StringBuilder s = new StringBuilder("RandomSupplier [");
    for (final Choice<T> choice : this.choices) {
      s.append(String.format("%nchoice=%s, weight=%s,", choice.value, choice.weight));
    }
    return s.append(String.format("%n]")).toString();
  }
}
