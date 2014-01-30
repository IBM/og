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
// Date: Nov 05, 2013
// ---------------------

package com.cleversafe.oom.statistic;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.cleversafe.oom.operation.OperationType;

/**
 * A simple timer implementation. A <code>Timers</code> instance can be started and stopped multiple
 * times.
 */
public class Timers
{
   private final int intervalStride;
   private final long[] timers;

   /**
    * Constructs a <code>Timers</code> instance with default timer values of <code>0</code>.
    */
   public Timers()
   {
      // allocate an array for all timers; OperationType * interval (true/false)
      this.intervalStride = OperationType.values().length;
      this.timers = new long[this.intervalStride * 2];
   }

   /**
    * Copy constructor.
    * 
    * @param timers
    *           the original <code>Timers</code> instance
    * @throws NullPointerException
    *            if timers is null
    */
   public Timers(final Timers timers)
   {
      checkNotNull(timers, "timers must not be null");
      this.intervalStride = timers.intervalStride;
      this.timers = new long[timers.timers.length];
      System.arraycopy(timers.timers, 0, this.timers, 0, timers.timers.length);
   }

   /**
    * Starts (or restarts) a timer's internal clock.
    * 
    * @param operationType
    *           the operation type of the timer
    * @param interval
    *           if true, starts the timer for the current interval, otherwise starts the overall
    *           timer
    * @param timestamp
    *           the timestamp to set the timer's clock to
    * @return the timestamp that was used for this call
    * @throws NullPointerException
    *            if operationType is null
    * @throws IllegalArgumentException
    *            if timestamp is negative
    */
   public long start(final OperationType operationType, final boolean interval, final long timestamp)
   {
      checkNotNull(operationType, "operationType must not be null");
      checkArgument(timestamp >= 0, "timestamp must be >= 0 [%s]", timestamp);
      this.timers[idx(operationType, interval)] = timestamp;
      return timestamp;
   }

   /**
    * Gets elapsed time since this timer was started.
    * 
    * @param operationType
    *           the operation type of the timer
    * @param interval
    *           if true, gets elapsed time for the current interval, otherwise the overall elapsed
    *           time
    * @param timestamp
    *           the timestamp to use when calculating elapsed time
    * @return elapsed time, in nanoseconds
    * @throws NullPointerException
    *            if operationType is null
    * @throws IllegalArgumentException
    *            if timestamp is less than the timestamp used to start this timer
    */
   public long elapsed(
         final OperationType operationType,
         final boolean interval,
         final long timestamp)
   {
      checkNotNull(operationType, "operationType must not be null");
      final long startTimestamp = this.timers[idx(operationType, interval)];
      checkArgument(timestamp >= startTimestamp, "timestamp must be >= start timestamp");
      return timestamp - startTimestamp;
   }

   /**
    * Resets a range of timers to a specified value.
    * 
    * @param interval
    *           if true, resets all interval timers, otherwise resets all overall timers
    * @param timestamp
    *           the timestamp to reset the timer clocks to
    * @throws IllegalArgumentException
    *            if timestamp is negative
    */
   public void reset(final boolean interval, final long timestamp)
   {
      checkArgument(timestamp >= 0, "timestamp must be >= 0 [%s]", timestamp);
      final int begin = (interval ? this.intervalStride : 0);
      final int end = begin + this.intervalStride;
      for (int i = begin; i < end; i++)
      {
         this.timers[i] = timestamp;
      }
   }

   private int idx(final OperationType o, final boolean i)
   {
      // algorithm for indexing into the array; the first half of the array contains overall
      // timers and the second half interval timers.
      return (i ? this.intervalStride : 0) + o.ordinal();
   }
}
