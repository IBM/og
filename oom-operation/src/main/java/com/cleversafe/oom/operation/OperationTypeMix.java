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
// @author: abaptist
//
// Date: Nov 6, 2012
// ---------------------

package com.cleversafe.oom.operation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Random;

import com.cleversafe.oom.util.WeightedRandomChoice;
import com.google.common.math.DoubleMath;

/**
 * An implementation for generating a sequence of <code>OperationType</code> values.
 */
public class OperationTypeMix
{
   private final double write;
   private final double read;
   private final double delete;
   private final long floor;
   private final long midpoint;
   private final long ceiling;
   private boolean onlyWrite;
   private boolean onlyDelete;
   private final static double err = Math.pow(0.1, 6);
   private final WeightedRandomChoice<OperationType> operationMix;

   /**
    * Constructs an <code>OperationTypeMix</code> instance for generating <code>OperationType</code>
    * values according to the configured IO mix
    * 
    * @param write
    *           the percentage of write operations
    * @param read
    *           the percentage of read operations
    * @param delete
    *           the percentage of delete operations
    * @param floor
    *           the lowest vault fill for deletes to take place, in bytes
    * @param ceiling
    *           the highest vault fill for writes to take places, in bytes
    * @throws IllegalArgumentException
    *            if write is not in range [0.0, 100.0]
    * @throws IllegalArgumentException
    *            if read is not in range [0.0, 100.0]
    * @throws IllegalArgumentException
    *            if delete is not in range [0.0, 100.0]
    * @throws IllegalArgumentException
    *            if floor is negative
    * @throws IllegalArgumentException
    *            if ceiling is less than floor
    */
   public OperationTypeMix(
         final double write,
         final double read,
         final double delete,
         final long floor,
         final long ceiling)
   {
      this(write, read, delete, floor, ceiling, new Random());
   }

   /**
    * Constructs an <code>OperationTypeMix</code> instance for generating <code>OperationType</code>
    * values according to the configured IO mix and using the provided <code>Random</code> instance
    * for random seed data
    * 
    * @param write
    * @param read
    * @param delete
    * @param floor
    *           the lowest vault fill for deletes to take place, in bytes
    * @param ceiling
    *           the highest vault fill for writes to take places, in bytes
    * @throws IllegalArgumentException
    *            if write is negative
    * @throws IllegalArgumentException
    *            if read is negative
    * @throws IllegalArgumentException
    *            if delete is negative
    * @throws IllegalArgumentException
    *            if floor is negative
    * @throws IllegalArgumentException
    *            if ceiling is less than floor
    * @throws NullPointerException
    *            if random is null
    */
   public OperationTypeMix(
         final double write,
         final double read,
         final double delete,
         final long floor,
         final long ceiling,
         final Random random)
   {
      checkArgument(inRange(write), "write must be in range [0.0, 100.0] [%s]", write);
      checkArgument(inRange(read), "read must be in range [0.0, 100.0] [%s]", read);
      checkArgument(inRange(delete), "delete must be in range [0.0, 100.0] [%s]", delete);

      final double sum = read + write + delete;
      checkArgument(DoubleMath.fuzzyEquals(sum, 100.0, err),
            "Sum of percentages must be 100.0 [%s]", sum);

      checkArgument(floor >= 0, "floor must be >= 0 [%s]", floor);
      checkArgument(ceiling >= floor, "ceiling must be >= floor [%s]", ceiling);
      checkNotNull(random, "random must not be null");

      this.write = write;
      this.read = read;
      this.delete = delete;
      this.operationMix = new WeightedRandomChoice<OperationType>(random);
      if (write > 0)
         this.operationMix.addChoice(OperationType.WRITE, write);
      if (read > 0)
         this.operationMix.addChoice(OperationType.READ, read);
      if (delete > 0)
         this.operationMix.addChoice(OperationType.DELETE, delete);
      this.floor = floor;
      this.midpoint = (floor + ceiling) / 2;
      this.ceiling = ceiling;
      this.onlyWrite = false;
      this.onlyDelete = false;
   }

   private boolean inRange(final double v)
   {
      return DoubleMath.fuzzyCompare(v, 0.0, err) >= 0
            && DoubleMath.fuzzyCompare(v, 100.0, err) <= 0;
   }

   /**
    * @return the configured write percentage
    */
   public double getWrite()
   {
      return this.write;
   }

   /**
    * @return the configured read percentage
    */
   public double getRead()
   {
      return this.read;
   }

   /**
    * @return the configured delete percentage
    */
   public double getDelete()
   {
      return this.delete;
   }

   /**
    * Calculates the next <code>OperationType</code> to use. If the current vault fill is within
    * floor and ceiling, the configured IO mix is used. Otherwise, writes or deletes will be used to
    * bring the vault fill to half way between floor and ceiling
    * 
    * @param vaultFill
    *           the current vault fill, in bytes
    * @return the next <code>OperationType</code> to use
    */
   public OperationType getNextOperationType(final long vaultFill)
   {
      checkArgument(vaultFill >= 0, "vaultFill must be >= 0 [%s]", vaultFill);

      if (vaultFill < this.floor || (this.onlyWrite && vaultFill < this.midpoint))
      {
         this.onlyWrite = true;
         this.onlyDelete = false;
         return OperationType.WRITE;
      }
      else if (vaultFill > this.ceiling || (this.onlyDelete && vaultFill > this.midpoint))
      {
         this.onlyDelete = true;
         this.onlyWrite = false;
         return OperationType.DELETE;
      }
      else
      {
         this.onlyWrite = false;
         this.onlyDelete = false;
         return this.operationMix.nextChoice();
      }
   }
}
