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
// Date: Oct 23, 2013
// ---------------------

package com.cleversafe.og.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * An implementation that randomly chooses an element amongst a collection of items.
 * 
 * @param <T>
 *           the type of elements for this collection to store
 */
public class WeightedRandomChoice<T>
{
   private final List<Choice<T>> choices;
   private double totalWeight;
   private final Random random;

   /**
    * An implementation for wrapping a <coded>WeightedRandomChoice</code> choice and its weight.
    * 
    * @param <S>
    *           the type of this choice
    */
   public class Choice<S>
   {
      private final S value;
      private final double weight;

      private Choice(final S choice, final double weight)
      {
         this.value = choice;
         this.weight = weight;
      }

      /**
       * @return the value of this choice
       */
      public S getValue()
      {
         return this.value;
      }

      /**
       * @return the weight of this choice
       */
      public double getWeight()
      {
         return this.weight;
      }
   }

   /**
    * Constructs a <code>WeightedRandomChoice</code> instance with a default <code>Random</code>
    * instance for random seed data
    */
   public WeightedRandomChoice()
   {
      this(new Random());
   }

   /**
    * Constructs a <code>WeightedRandomChoice</code> instance with the provided <code>Random</code>
    * instance
    * 
    * @param random
    *           the instance to use for random seed data
    * @throws NullPointerException
    *            if random is null
    */
   public WeightedRandomChoice(final Random random)
   {
      checkNotNull(random, "random must not be null");
      this.choices = new ArrayList<Choice<T>>();
      this.totalWeight = 0.0;
      this.random = random;
   }

   /**
    * Adds a choice to the collection of elements to randomly choose from. A default weight of 1.0
    * is applied to this choice
    * 
    * @param choice
    *           the choice to add
    * @throws NullPointerException
    *            if choice is null
    */
   public void addChoice(final T choice)
   {
      addChoice(choice, 1.0);
   }

   /**
    * Adds a choice to the collection of elements to randomly choose from
    * 
    * @param choice
    *           the choice to add
    * @param weight
    *           the weight to give to this choice when selecting the next random choice
    * @throws NullPointerException
    *            if choice is null
    * @throws IllegalArgumentException
    *            if weight is negative
    */
   public void addChoice(final T choice, final double weight)
   {
      checkNotNull(choice, "choice must not be null");
      checkArgument(weight > 0.0, "weight must be > 0.0 [%s]", weight);
      this.choices.add(new Choice<T>(choice, weight));
      this.totalWeight += weight;
   }

   /**
    * Randomly selects the next choice to use from the collection, based on the weight of each
    * choice
    * 
    * @return the next randomly selected choice
    * @throws IllegalStateException
    *            if <code>addChoice</code> has not been called at least once prior to calling this
    *            method
    */
   public T nextChoice()
   {
      if (this.choices.size() < 1)
      {
         throw new IllegalStateException("at least one choice must be added");
      }

      final double rnd = this.random.nextDouble() * this.totalWeight;
      double previousWeights = 0.0;

      for (final Choice<T> choice : this.choices)
      {
         if (rnd < previousWeights + choice.weight)
         {
            return choice.value;
         }
         previousWeights += choice.weight;
      }
      return this.choices.get(this.choices.size() - 1).value;
   }

   /**
    * @return an iterator of all previously added choices
    */
   public Iterator<Choice<T>> choiceIterator()
   {
      return this.choices.iterator();
   }
}
