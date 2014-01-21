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
// Date: Jan 20, 2014
// ---------------------

package com.cleversafe.oom.statistic;

import org.apache.commons.lang3.Validate;

import com.cleversafe.oom.operation.OperationType;

public class StatisticsImpl implements Statistics
{
   private final Counters ctrs;
   // timers are used for tracking ACTIVE_DURATION
   private final Timers timers;
   private final long globalTimestamp;
   private long globalIntervalTimestamp;
   private final long initialObjectCount;
   private final long averageObjectSize;

   /**
    * Constructs a <code>StatisticsImpl</code> instance with default <code>Counter</code> and
    * <code>Timers</code> instances.
    * 
    * @param initialObjectCount
    *           the initial count of objects on the vault
    * @param averageObjectSize
    *           the average object size, in bytes
    * @throws IllegalArgumentException
    *            if initialObjectCount is negative
    * @throws IllegalArgumentException
    *            if averageObjectSize is negative
    */
   public StatisticsImpl(final long initialObjectCount, final long averageObjectSize)
   {
      Validate.isTrue(initialObjectCount >= 0, "initialObjectCount must be >= 0 [%s]",
            initialObjectCount);
      Validate.isTrue(averageObjectSize >= 0, "averageObjectSize must be >= 0 [%s]",
            averageObjectSize);

      this.ctrs = new Counters();
      this.timers = new Timers();
      final long timestamp = System.nanoTime();
      this.globalTimestamp = timestamp;
      this.globalIntervalTimestamp = timestamp;
      this.initialObjectCount = initialObjectCount;
      this.averageObjectSize = averageObjectSize;
   }

   // Copy constructor.
   private StatisticsImpl(final StatisticsImpl orig)
   {
      this.ctrs = new Counters(orig.ctrs);
      this.timers = new Timers(orig.timers);
      this.globalTimestamp = orig.globalTimestamp;
      this.globalIntervalTimestamp = orig.globalIntervalTimestamp;
      this.initialObjectCount = orig.initialObjectCount;
      this.averageObjectSize = orig.averageObjectSize;
   }

   @Override
   public long beginOperation(final OperationType operationType)
   {
      validateOperationType(operationType);
      modifyAll(operationType, Counter.COUNT, 1);
      final long op = modifyBoth(operationType, Counter.ACTIVE_COUNT, 1);
      final long all = modifyBoth(OperationType.ALL, Counter.ACTIVE_COUNT, 1);
      modifyAllActiveMinMaxCounter(operationType);
      final long beginTimestamp = System.nanoTime();

      // start timer for this operation if it is the first active
      if (op == 1)
      {
         this.timers.start(operationType, false, beginTimestamp);
         this.timers.start(operationType, true, beginTimestamp);

         // start all timer if this operation is the first active overall
         if (all == 1)
         {
            this.timers.start(OperationType.ALL, false, beginTimestamp);
            this.timers.start(OperationType.ALL, true, beginTimestamp);
         }
      }
      return beginTimestamp;
   }

   @Override
   public void ttfb(final OperationType operationType, final long ttfb)
   {
      validateOperationType(operationType);
      Validate.isTrue(ttfb >= 0, "ttfb must be >= 0 [%s]", ttfb);
      Validate.validState(getCounter(operationType, Counter.ACTIVE_COUNT, false) > 0,
            "no operations of this type are active [%s]", operationType);
      modifyAll(operationType, Counter.TTFB, ttfb);
   }

   @Override
   public void bytes(final OperationType operationType, final long bytes)
   {
      validateOperationType(operationType);
      Validate.isTrue(bytes >= 0, "bytes must be >= 0 [%s]", bytes);
      Validate.validState(getCounter(operationType, Counter.ACTIVE_COUNT, false) > 0,
            "no operations of this type are active [%s]", operationType);
      modifyAll(operationType, Counter.BYTES, bytes);
   }

   @Override
   public long completeOperation(final OperationType operationType, final long beginTimestamp)
   {
      return endOperation(operationType, Counter.COMPLETE_COUNT, beginTimestamp);
   }

   @Override
   public long failOperation(final OperationType operationType, final long beginTimestamp)
   {
      return endOperation(operationType, Counter.FAILURE_COUNT, beginTimestamp);
   }

   @Override
   public long abortOperation(final OperationType operationType, final long beginTimestamp)
   {
      return endOperation(operationType, Counter.ABORT_COUNT, beginTimestamp);
   }

   // Called by complete/fail/abortOperation
   private long endOperation(
         final OperationType o,
         final Counter endCounter,
         final long beginTimestamp)
   {
      final long endTimestamp = System.nanoTime();
      validateOperationType(o);
      Validate.isTrue(beginTimestamp >= 0, "beginTimestamp must be >= 0 [%s]", beginTimestamp);
      Validate.isTrue(beginTimestamp <= endTimestamp,
            "beginTimestamp must be <= endTimestamp");
      Validate.validState(getCounter(o, Counter.ACTIVE_COUNT, false) > 0,
            "no operations of this type are active [%s]", o);

      final long duration = endTimestamp - beginTimestamp;
      modifyAll(o, endCounter, 1);
      modifyAll(o, Counter.DURATION, duration);
      final long op = modifyBoth(o, Counter.ACTIVE_COUNT, -1);
      final long all = modifyBoth(OperationType.ALL, Counter.ACTIVE_COUNT, -1);
      modifyAllActiveMinMaxCounter(o);

      // stop timer and update ACTIVE_DURATION for this operation if it is the last active
      if (op == 0)
      {
         final long opDuration = this.timers.elapsed(o, false, endTimestamp);
         final long opIDuration = this.timers.elapsed(o, true, endTimestamp);
         modify(o, Counter.ACTIVE_DURATION, false, opDuration);
         modify(o, Counter.ACTIVE_DURATION, true, opIDuration);

         // stop all timer and update ACTIVE_DURATION if this operation is the last active overall
         if (all == 0)
         {
            final long allDuration = this.timers.elapsed(OperationType.ALL, false, endTimestamp);
            final long allIDuration = this.timers.elapsed(OperationType.ALL, true, endTimestamp);
            modify(OperationType.ALL, Counter.ACTIVE_DURATION, false, allDuration);
            modify(OperationType.ALL, Counter.ACTIVE_DURATION, true, allIDuration);
         }
      }
      return endTimestamp;
   }

   // Convenience method for validating OperationType
   private void validateOperationType(final OperationType o)
   {
      Validate.notNull(o, "operationType must not be null");
      Validate.isTrue(o != OperationType.ALL, "operationType must not be ALL");
   }

   private long modify(final OperationType o, final Counter c, final boolean i, final long amt)
   {
      return this.ctrs.modify(o, c, i, amt);
   }

   private long modifyBoth(final OperationType o, final Counter c, final long amt)
   {
      modify(o, c, false, amt);
      return modify(o, c, true, amt);
   }

   private void modifyAll(final OperationType o, final Counter c, final long amt)
   {
      modifyBoth(o, c, amt);
      modifyBoth(OperationType.ALL, c, amt);
   }

   private void modifyActiveMinMaxCounter(final OperationType operationType, final boolean interval)
   {
      final long active = this.ctrs.get(operationType, Counter.ACTIVE_COUNT, interval);
      final long activeMin = this.ctrs.get(operationType, Counter.ACTIVE_COUNT_MIN, interval);
      final long activeMax = this.ctrs.get(operationType, Counter.ACTIVE_COUNT_MAX, interval);

      if (active < activeMin)
         this.ctrs.set(operationType, Counter.ACTIVE_COUNT_MIN, interval, active);
      if (active > activeMax)
         this.ctrs.set(operationType, Counter.ACTIVE_COUNT_MAX, interval, active);
   }

   private void modifyAllActiveMinMaxCounter(final OperationType o)
   {
      modifyActiveMinMaxCounter(o, false);
      modifyActiveMinMaxCounter(o, true);
      modifyActiveMinMaxCounter(OperationType.ALL, false);
      modifyActiveMinMaxCounter(OperationType.ALL, true);
   }

   @Override
   public long getCounter(
         final OperationType operationType,
         final Counter counter,
         final boolean interval)
   {
      Validate.notNull(operationType, "operationType must not be null");
      Validate.notNull(counter, "counter must not be null");
      return this.ctrs.get(operationType, counter, interval);
   }

   private long getTimestamp(final boolean interval)
   {
      if (interval)
         return this.globalIntervalTimestamp;
      return this.globalTimestamp;
   }

   @Override
   public double getStat(final OperationType operationType, final Stat stat, final boolean interval)
   {
      Validate.notNull(operationType, "operationType must not be null");
      Validate.notNull(stat, "stat must not be null");
      final long timestamp = getTimestamp(interval);

      switch (stat)
      {
         case THROUGHPUT :
            return avg(operationType, Counter.BYTES, Counter.DURATION, interval);
         case ACTIVE_THROUGHPUT :
            return avg(operationType, Counter.BYTES, Counter.ACTIVE_DURATION, interval);
         case ELAPSED_THROUGHPUT :
            return avg(operationType, Counter.BYTES, System.nanoTime() - timestamp, interval);
         case DURATION_AVG :
            return avg(operationType, Counter.DURATION, Counter.COUNT, interval);
         case BYTES_AVG :
            return avg(operationType, Counter.BYTES, Counter.COUNT, interval);
         case TTFB_AVG :
            return avg(operationType, Counter.TTFB, Counter.COUNT, interval);
         case RATE :
            return avg(operationType, Counter.COUNT, System.nanoTime() - timestamp, interval);
      }
      return 0.0;
   }

   private double avg(final OperationType o, final Counter num, final Counter denom, final boolean i)
   {
      return avg(getCounter(o, num, i), getCounter(o, denom, i));
   }

   private double avg(final OperationType o, final Counter num, final long denom, final boolean i)
   {
      return avg(getCounter(o, num, i), denom);
   }

   private double avg(final long numerator, final long denominator)
   {
      if (denominator > 0)
         return (double) numerator / denominator;
      return 0.0;
   }

   @Override
   public long getDuration(final boolean interval)
   {
      return System.nanoTime() - getTimestamp(interval);
   }

   @Override
   public long getVaultFill()
   {
      final long bytesWritten = getCounter(OperationType.WRITE, Counter.BYTES, false);
      final long objectsDeleted = getCounter(OperationType.DELETE, Counter.COMPLETE_COUNT, false);

      return (this.initialObjectCount * this.averageObjectSize) + bytesWritten
            - (objectsDeleted * this.averageObjectSize);
   }

   @Override
   public Statistics snapshot()
   {
      final StatisticsImpl snapshot = new StatisticsImpl(this);
      this.ctrs.clear(true);

      for (final OperationType o : OperationType.values())
      {
         // must persist active counters across snapshots
         final long activeCount = snapshot.getCounter(o, Counter.ACTIVE_COUNT, true);
         this.ctrs.set(o, Counter.ACTIVE_COUNT, true, activeCount);
         this.ctrs.set(o, Counter.ACTIVE_COUNT_MIN, true, activeCount);
         this.ctrs.set(o, Counter.ACTIVE_COUNT_MAX, true, activeCount);
      }
      final long snapshotTimestamp = System.nanoTime();
      this.timers.reset(true, snapshotTimestamp);
      this.globalIntervalTimestamp = snapshotTimestamp;
      return snapshot;
   }

}
