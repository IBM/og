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
// Date: Nov 18, 2013
// ---------------------

package com.cleversafe.oom.test.condition;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.cleversafe.oom.operation.OperationType;
import com.cleversafe.oom.statistic.Counter;
import com.cleversafe.oom.statistic.Statistics;

/**
 * A <code>StoppingCondition</code> implementation that checks vault fill. Note: this implementation
 * does not query the vault in use for fill level. Instead, a combination of initial
 * <code>ObjectManager</code> object count, size distribution, and write/delete operation counts are
 * used to provide a loose estimate.
 */
public class VaultFillStoppingCondition extends RelationalOperatorStoppingCondition
{
   private final Statistics stats;
   private final long initialObjectCount;
   private final long averageObjectSize;
   private final String toString;

   /**
    * Constructs an instance which triggers on vault fill.
    * 
    * @param stats
    *           the stats instance to get object write and delete counts from
    * @param initialObjectCount
    *           initial object count as provided by an object manager instance
    * @param averageObjectSize
    *           average object size as determined by size distribution
    * @param operator
    *           the relational operator to use for comparison
    * @param vaultFill
    *           the desired vaultFill to compare to
    * @throws NullPointerException
    *            if stats is null
    * @throws IllegalArgumentException
    *            if initialObjectCount is negative
    * @throws IllegalArgumentException
    *            if averageObjectSize is negative
    * @throws NullPointerException
    *            if operator is null
    * @throws IllegalArgumentException
    *            if vaultFill is negative
    */
   public VaultFillStoppingCondition(
         final Statistics stats,
         final long initialObjectCount,
         final long averageObjectSize,
         final RelationalOperator operator,
         final long vaultFill)
   {
      this(stats, initialObjectCount, averageObjectSize, operator, vaultFill, null);
   }

   /**
    * Constructs an instance which triggers on vault fill.
    * 
    * @param stats
    *           the stats instance to get object write and delete counts from
    * @param initialObjectCount
    *           initial object count as provided by an object manager instance
    * @param averageObjectSize
    *           average object size as determined by size distribution
    * @param operator
    *           the relational operator to use for comparison
    * @param vaultFill
    *           the desired vaultFill to compare to
    * @param toString
    *           string representation of this instance, optional
    * @throws NullPointerException
    *            if stats is null
    * @throws IllegalArgumentException
    *            if initialObjectCount is negative
    * @throws IllegalArgumentException
    *            if averageObjectSize is negative
    * @throws NullPointerException
    *            if operator is null
    * @throws IllegalArgumentException
    *            if vaultFill is negative
    */
   public VaultFillStoppingCondition(
         final Statistics stats,
         final long initialObjectCount,
         final long averageObjectSize,
         final RelationalOperator operator,
         final long vaultFill,
         final String toString)
   {
      super(operator, vaultFill);
      checkNotNull(stats, "stats must not be null");
      checkArgument(initialObjectCount >= 0, "initialObjectCount must be >= 0 [%s]",
            initialObjectCount);
      checkArgument(averageObjectSize >= 0, "averageObjectSize must be >= 0 [%s]",
            averageObjectSize);
      checkArgument(vaultFill >= 0, "vaultFill must be >= 0 [%s]", vaultFill);
      this.stats = stats;
      this.initialObjectCount = initialObjectCount;
      this.averageObjectSize = averageObjectSize;
      this.toString = toString;
   }

   @Override
   protected long leftOperand()
   {
      long objectCount = this.initialObjectCount;

      // bit of a synchronization hack; have to get a consistent view of how many write and delete
      // operations have occurred at this moment in time
      synchronized (this.stats)
      {
         objectCount += this.stats.getCounter(OperationType.WRITE, Counter.COUNT, false);
         objectCount -= this.stats.getCounter(OperationType.DELETE, Counter.COUNT, false);
      }
      return this.averageObjectSize * objectCount;
   }

   @Override
   public String toString()
   {
      // return legacy string for backwards compatibility
      if (this.toString != null)
         return this.toString;

      return String.format("VaultFill %s %d", this.operator, this.rightOperand);
   }
}
