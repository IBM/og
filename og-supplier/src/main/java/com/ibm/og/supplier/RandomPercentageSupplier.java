/* Copyright (c) IBM Corporation 2017. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.supplier;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class RandomPercentageSupplier<T> implements Supplier<T> {
    private final List<RandomPercentageSupplier.Choice<T>> choices;
    private final Random random;

    private static class Choice<S> {
        private final S value;
        private final Supplier<? extends Number> percentage;
        private double currentPercentage;

        private Choice(final S choice, final Supplier<? extends Number> percentage) {
            this.value = choice;
            this.percentage = percentage;
            this.currentPercentage = 0.0;
        }
    }
    private RandomPercentageSupplier(final RandomPercentageSupplier.Builder<T> builder) {
        this.choices = checkNotNull(builder.choices);
        checkArgument(!this.choices.isEmpty(), "choices must not be empty");
        this.random = checkNotNull(builder.random);
    }

    @Override
    public T get() {
        final double totalPercent = getCurrentPercents();
        final double rnd = this.random.nextDouble() * totalPercent;
        double previousPercents = 0.0;

        RandomPercentageSupplier.Choice<T> unUsedChoice = this.choices.get(this.choices.size()-1);
        for (final RandomPercentageSupplier.Choice<T> choice : this.choices) {
            if (rnd < previousPercents + choice.currentPercentage) {
                if (choice.value != null) {
                    return choice.value;
                } else {
                    return null;
                }
            }
            previousPercents += choice.currentPercentage;
        }
        throw new IllegalStateException("Incorrect percentage calculation");
    }

    private double getCurrentPercents() {
        double currentTotalPercent = 0.0;
        for (final RandomPercentageSupplier.Choice<T> choice : this.choices) {
            choice.currentPercentage = choice.percentage.get().doubleValue();
            currentTotalPercent += choice.currentPercentage;
        }
        return currentTotalPercent;
    }

    /**
     * A builder of random choice supplier instances
     *
     * @param <T> the type of values to add to this builder
     */
    public static class Builder<T> {
        private final List<RandomPercentageSupplier.Choice<T>> choices;
        private double unusedPercentage = 100.00;
        private Random random;
        private double totalPercentage = 0.00;

        /**
         * Constructs a new builder
         */
        public Builder() {
            this.choices = Lists.newArrayList();
            this.random = new Random();
        }

        /**
         * Adds a choice with a default percentage of {@code 1.0}
         *
         * @param choice the choice to add
         * @return this builder
         */
        public RandomPercentageSupplier.Builder<T> withChoice(final T choice) {

            return withChoice(choice, 0.00);
        }

        /**
         * Adds a choice with the provided percentage
         *
         * @param choice the choice to add
         * @param percentage the percentage to give the choice
         * @return this builder
         * @throws IllegalArgumentException if percentage is negative or zero
         */
        public RandomPercentageSupplier.Builder<T> withChoice(final T choice, final double percentage) {
            checkArgument(percentage >= 0.0 && percentage <= 100.00, "percentage[%s] must be between 0.00 and 100.00 inclusive ", percentage);
             return withChoice(choice, Suppliers.of(percentage));
        }

        /**
         * Adds a choice with the provided percentage, which may be dynamic
         *
         * @param choice the choice to add
         * @param percentage the percentage to give the choice, which may be dynamic
         * @return this builder
         */
        public RandomPercentageSupplier.Builder<T> withChoice(final T choice, final Supplier<? extends Number> percentage) {
            checkNotNull(choice);
            checkNotNull(percentage);
            this.unusedPercentage -= percentage.get().doubleValue();;
            this.totalPercentage += percentage.get().doubleValue();
            checkArgument(this.totalPercentage <= 100.00,
                    "Total percentages[%s] must be less than or equal to 100.00", totalPercentage);
            if(this.choices.size() > 0) {
                this.choices.remove(this.choices.size()-1);
            }
            this.choices.add(new RandomPercentageSupplier.Choice<T>(choice, percentage));
            this.choices.add(new RandomPercentageSupplier.Choice<T>(null, Suppliers.of(this.unusedPercentage)));
            return this;
        }

        /**
         * Configures this builder to use a provided random instance
         *
         * @param random the random instance to use for value selection
         * @return this builder
         */
        public RandomPercentageSupplier.Builder<T> withRandom(final Random random) {
            this.random = random;
            return this;
        }

        /**
         * Creates a random choice supplier instance
         *
         * @return a new random choice supplier instance
         * @throws IllegalArgumentException if no choices were added prior to calling this method
         */
        public RandomPercentageSupplier<T> build() {
            return new RandomPercentageSupplier<T>(this);
        }
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder("RandomPercentageSupplier [");
        for (final RandomPercentageSupplier.Choice<T> choice : this.choices) {
            s.append(String.format("%nchoice=%s, percentage=%s,", choice.value, choice.percentage));
        }
        return s.append(String.format("%n]")).toString();
    }

}
