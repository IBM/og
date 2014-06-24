//
// Copyright (C) 2005-2011 Cleversafe, Inc. All rights reserved.
//
// Contact Information:
// Cleversafe, Inc.
// 222 South Riverside Plaza
// Suite 1700
// Chicago, IL 60606, USA
//
// licensing@cleversafe.com
//
// END-OF-HEADER
//
// -----------------------
// @author: rveitch
//
// Date: Jun 24, 2014
// ---------------------

package com.cleversafe.og.util.producer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.api.Producer;

public class RandomChoiceProducer<T> implements Producer<T>
{
   private static Logger _logger = LoggerFactory.getLogger(RandomChoiceProducer.class);
   private final List<Choice<T>> choices;
   private final Random random;

   private RandomChoiceProducer(final List<Choice<T>> choices, final Random random)
   {
      this.choices = checkNotNull(choices);
      checkArgument(choices.size() > 0, "choices size must be > 0");
      this.random = checkNotNull(random);
   }

   private static class Choice<S>
   {
      public final S value;
      public final Producer<Double> weight;
      public double currentWeight;

      private Choice(final S choice, final Producer<Double> weight)
      {
         this.value = choice;
         this.weight = weight;
         this.currentWeight = 0.0;
      }
   }

   @Override
   public T produce()
   {
      final double totalWeight = getCurrentWeights();
      final double rnd = this.random.nextDouble() * totalWeight;
      double previousWeights = 0.0;

      for (final Choice<T> choice : this.choices)
      {
         if (rnd < previousWeights + choice.currentWeight)
         {
            return choice.value;
         }
         previousWeights += choice.currentWeight;
      }
      return this.choices.get(this.choices.size() - 1).value;
   }

   private double getCurrentWeights()
   {
      double currentTotalWeight = 0.0;
      for (final Choice<T> choice : this.choices)
      {
         choice.currentWeight = choice.weight.produce();
         currentTotalWeight += choice.currentWeight;
      }
      return currentTotalWeight;
   }

   public static <T> Builder<T> custom(final Class<T> cls)
   {
      return new Builder<T>();
   }

   public static class Builder<T>
   {
      private final List<Choice<T>> choices;
      private Random random;

      private Builder()
      {
         this.choices = new ArrayList<Choice<T>>();
         this.random = new Random();
      }

      public Builder<T> withChoice(final T choice)
      {
         return withChoice(choice, 1.0);
      }

      public Builder<T> withChoice(final T choice, final double weight)
      {
         checkArgument(weight > 0.0, "weight must be > 0.0 [%s]", weight);
         return withChoice(choice, Producers.of(weight));
      }

      public Builder<T> withChoice(final T choice, final Producer<Double> weight)
      {
         checkNotNull(choice);
         checkNotNull(weight);
         this.choices.add(new Choice<T>(choice, weight));
         return this;
      }

      public Builder<T> withRandom(final Random random)
      {
         this.random = random;
         return this;
      }

      public RandomChoiceProducer<T> build()
      {
         return new RandomChoiceProducer<T>(this.choices, this.random);
      }
   }
}
