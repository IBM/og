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
// Date: Nov 5, 2013
// ---------------------

package com.cleversafe.oom.statistic;

import org.apache.commons.lang3.Validate;

import com.cleversafe.oom.operation.OperationType;

/**
 * A counters implementation for storing simple tool measurements.
 */
public class Counters
{
   private final int intervalStride;
   private final int operationTypeStride;
   private final long[] ctrs;

   /**
    * Constructs a <code>Counters</code> instance with default counter values of <code>0</code>.
    */
   public Counters()
   {
      final int oLength = OperationType.values().length;
      final int cLength = Counter.values().length;
      this.intervalStride = oLength * cLength;
      this.operationTypeStride = cLength;
      // allocate an array for all counters; OperationType * Counter * interval (true/false)
      this.ctrs = new long[this.intervalStride * 2];
   }

   /**
    * Copy constructor.
    * 
    * @param counters
    *           the original <code>Counters</code> instance
    * @throws NullPointerException
    *            if counters is null
    */
   public Counters(final Counters counters)
   {
      Validate.notNull(counters, "counters must not be null");
      this.intervalStride = counters.intervalStride;
      this.operationTypeStride = counters.operationTypeStride;
      this.ctrs = new long[counters.ctrs.length];
      System.arraycopy(counters.ctrs, 0, this.ctrs, 0, counters.ctrs.length);
   }

   /**
    * Gets a counter.
    * 
    * @param operationType
    *           the operation type of the counter
    * @param counter
    *           the counter type to get the value of
    * @param interval
    *           if true, retrieve the counter value for the current interval, otherwise retrieve the
    *           overall counter value
    * @return the value of the given counter
    * @throws NullPointerException
    *            if operationType is null
    * @throws NullPointerException
    *            if counter is null
    */
   public long get(final OperationType operationType, final Counter counter, final boolean interval)
   {
      Validate.notNull(operationType, "operationType must not be null");
      Validate.notNull(counter, "counter must not be null");
      return this.ctrs[idx(operationType, counter, interval)];
   }

   /**
    * Sets a counter.
    * 
    * @param operationType
    *           the operation type of the counter
    * @param counter
    *           the counter type to get the value of
    * @param interval
    *           if true, retrieve the counter value for the current interval, otherwise retrieve the
    *           overall counter value
    * @param value
    *           the new value of the counter
    * @throws NullPointerException
    *            if operationType is null
    * @throws NullPointerException
    *            if counter is null
    * @throws IllegalArgumentException
    *            if value is negative
    */
   public void set(
         final OperationType operationType,
         final Counter counter,
         final boolean interval,
         final long value)
   {
      Validate.notNull(operationType, "operationType must not be null");
      Validate.notNull(counter, "counter must not be null");
      Validate.isTrue(value >= 0, "value must be >= 0 [%s]", value);
      this.ctrs[idx(operationType, counter, interval)] = value;
   }

   /**
    * Modifies a counter.
    * 
    * @param counter
    *           the counter to modify
    * @param amount
    *           the amount to modify the counter by
    * @return the updated value of the counter
    * @throws NullPointerException
    *            if counter is null
    * @throws IllegalStateException
    *            if modifying the counter would result in a negative value
    */
   public long modify(
         final OperationType operationType,
         final Counter counter,
         final boolean interval,
         final long amount)
   {
      Validate.notNull(operationType, "operationType must not be null");
      Validate.notNull(counter, "counter must not be null");
      Validate.validState(get(operationType, counter, interval) + amount >= 0,
            "Modifying counter by amount would make it negative [%s]", amount);

      final long newValue = get(operationType, counter, interval) + amount;
      set(operationType, counter, interval, newValue);
      return newValue;
   }

   /**
    * Clears a range of counters to their original value of <code>0</code>.
    * 
    * @param interval
    *           if true, clears all interval counters, otherwise clears all overall counters
    */
   public void clear(final boolean interval)
   {
      final int begin = (interval ? this.intervalStride : 0);
      final int end = begin + this.intervalStride;
      for (int i = begin; i < end; i++)
      {
         this.ctrs[i] = 0;
      }
   }

   private int idx(final OperationType o, final Counter c, final boolean i)
   {
      // algorithm for indexing into the array; the first half of the array contains overall
      // counters and the second half interval counters. Each half is further subdivided by
      // operation type, and finally by counter type
      return (i ? this.intervalStride : 0) + (o.ordinal() * this.operationTypeStride) + c.ordinal();
   }
}
