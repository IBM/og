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
// Date: Jun 23, 2014
// ---------------------

package com.cleversafe.og.producer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A producer which produces values in a cycle. Example:
 * <p>
 * 
 * <pre>
 * {@code List<Integer> values = new ArrayList<Integer>();
 * values.add(1);
 * values.add(2);
 * values.add(3);
 * Producer<Integer> cycle = new CycleProducer<Integer>(values);}
 * </pre>
 * <p>
 * Subsequent calls to produce will return {@code (1, 2, 3, 1, 2, 3, 1)} and so on.
 * 
 * @param <T>
 *           the type of values to produce
 * @since 1.0
 */
public class CycleProducer<T> implements Producer<T>
{
   private static final Logger _logger = LoggerFactory.getLogger(CycleProducer.class);
   private final List<T> values;
   private final AtomicLong counter;

   /**
    * Constructs a producer using the provided list of values
    * 
    * @param values
    *           the values to produce
    * @throws IllegalArgumentException
    *            if values is empty
    * @throws NullPointerException
    *            if values contains any null elements
    */
   public CycleProducer(final List<T> values)
   {
      checkNotNull(values);
      checkArgument(!values.isEmpty(), "values must not be empty");
      // defensive copy
      this.values = new ArrayList<T>();
      for (final T value : values)
      {
         this.values.add(checkNotNull(value, "values must not contain any null elements"));
      }
      this.counter = new AtomicLong(0);
   }

   @Override
   public T produce()
   {
      final int idx = (int) (this.counter.getAndIncrement() % this.values.size());
      return this.values.get(idx);
   }

   @Override
   public String toString()
   {
      final StringBuilder s = new StringBuilder("CycleProducer [");
      for (final T value : this.values)
      {
         s.append(String.format(Locale.US, "%n%s", value));
      }
      return s.append(String.format("%n]")).toString();
   }
}
