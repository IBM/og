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

/**
 * An implementation for generating a sequence of <code>OperationType</code> values.
 */
public class OperationTypeMix
{
   private final long readPercentage;
   private final long writePercentage;
   private final long deletePercentage;
   private final long floor;
   private final long midpoint;
   private final long ceiling;
   private boolean onlyWrite;
   private boolean onlyDelete;
   private final WeightedRandomChoice<OperationType> operationMix;

   /**
    * Constructs an <code>OperationTypeMix</code> instance for generating <code>OperationType</code>
    * values according to the configured IO mix
    * 
    * @param readPercentage
    * @param writePercentage
    * @param deletePercentage
    * @param floor
    *           the lowest vault fill for deletes to take place, in bytes
    * @param ceiling
    *           the highest vault fill for writes to take places, in bytes
    * @throws IllegalArgumentException
    *            if readPercentage is negative
    * @throws IllegalArgumentException
    *            if writePercentage is negative
    * @throws IllegalArgumentException
    *            if deletePercentage is negative
    * @throws IllegalArgumentException
    *            if floor is negative
    * @throws IllegalArgumentException
    *            if ceiling is less than floor
    */
   public OperationTypeMix(
         final long readPercentage,
         final long writePercentage,
         final long deletePercentage,
         final long floor,
         final long ceiling)
   {
      this(readPercentage, writePercentage, deletePercentage, floor, ceiling, new Random());
   }

   /**
    * Constructs an <code>OperationTypeMix</code> instance for generating <code>OperationType</code>
    * values according to the configured IO mix and using the provided <code>Random</code> instance
    * for random seed data
    * 
    * @param readPercentage
    * @param writePercentage
    * @param deletePercentage
    * @param floor
    *           the lowest vault fill for deletes to take place, in bytes
    * @param ceiling
    *           the highest vault fill for writes to take places, in bytes
    * @throws IllegalArgumentException
    *            if readPercentage is negative
    * @throws IllegalArgumentException
    *            if writePercentage is negative
    * @throws IllegalArgumentException
    *            if deletePercentage is negative
    * @throws IllegalArgumentException
    *            if floor is negative
    * @throws IllegalArgumentException
    *            if ceiling is less than floor
    * @throws NullPointerException
    *            if random is null
    */
   public OperationTypeMix(
         final long readPercentage,
         final long writePercentage,
         final long deletePercentage,
         final long floor,
         final long ceiling,
         final Random random)
   {
      checkArgument(0 <= readPercentage && readPercentage <= 100,
            "readPercentage must be in range [0, 100] [%s]", readPercentage);
      checkArgument(0 <= writePercentage && writePercentage <= 100,
            "writePercentage must be in range [0, 100] [%s]", writePercentage);
      checkArgument(0 <= deletePercentage && deletePercentage <= 100,
            "deletePercentage must be in range [0, 100] [%s]", deletePercentage);
      final long sumPercentage = readPercentage + writePercentage + deletePercentage;
      checkArgument(sumPercentage == 100, "Sum of percentages must be 100 [%s]", sumPercentage);
      checkArgument(floor >= 0, "floor must be >= 0 [%s]", floor);
      checkArgument(ceiling >= floor, "ceiling must be >= floor [%s]", ceiling);
      checkNotNull(random, "random must not be null");

      this.readPercentage = readPercentage;
      this.writePercentage = writePercentage;
      this.deletePercentage = deletePercentage;
      this.operationMix = new WeightedRandomChoice<OperationType>(random);
      if (readPercentage > 0)
         this.operationMix.addChoice(OperationType.READ, readPercentage);
      if (writePercentage > 0)
         this.operationMix.addChoice(OperationType.WRITE, writePercentage);
      if (deletePercentage > 0)
         this.operationMix.addChoice(OperationType.DELETE, deletePercentage);
      this.floor = floor;
      this.midpoint = (floor + ceiling) / 2;
      this.ceiling = ceiling;
      this.onlyWrite = false;
      this.onlyDelete = false;
   }

   /**
    * @return the configured read percentage
    */
   public long getReadPercentage()
   {
      return this.readPercentage;
   }

   /**
    * @return the configured write percentage
    */
   public long getWritePercentage()
   {
      return this.writePercentage;
   }

   /**
    * @return the configured delete percentage
    */
   public long getDeletePercentage()
   {
      return this.deletePercentage;
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
