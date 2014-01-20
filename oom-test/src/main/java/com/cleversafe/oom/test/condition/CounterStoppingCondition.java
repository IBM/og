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

import org.apache.commons.lang3.Validate;

import com.cleversafe.oom.operation.OperationType;
import com.cleversafe.oom.statistic.Counter;
import com.cleversafe.oom.statistic.Statistics;

/**
 * A <code>StoppingCondition</code> implementation that checks counter values.
 */
public class CounterStoppingCondition extends RelationalOperatorStoppingCondition
{
   private final Statistics stats;
   private final OperationType type;
   private final Counter counter;
   private final boolean interval;
   private final String toString;

   /**
    * Constructs an instance which triggers on comparison to a <code>Stats</code> counter.
    * 
    * @param type
    *           the operation type of the counter to check
    * @param counter
    *           the counter to check
    * @param interval
    *           if true, check the interval counter, else check the overall counter
    * @param operator
    *           the relational operator to use for comparison
    * @param rightOperand
    *           the operand to place on the right hand side of the comparison
    * @throws NullPointerException
    *            if type is null
    * @throws NullPointerException
    *            if counter is null
    * @throws NullPointerException
    *            if operator is null
    */
   public CounterStoppingCondition(
         final Statistics stats,
         final OperationType type,
         final Counter counter,
         final boolean interval,
         final RelationalOperator operator,
         final long rightOperand)
   {
      this(stats, type, counter, interval, operator, rightOperand, null);
   }

   /**
    * Constructs an instance which triggers on comparison to a <code>Stats</code> counter.
    * 
    * @param type
    *           the operation type of the counter to check
    * @param counter
    *           the counter to check
    * @param interval
    *           if true, check the interval counter, else check the overall counter
    * @param operator
    *           the relational operator to use for comparison
    * @param rightOperand
    *           the operand to place on the right hand side of the comparison
    * @param toString
    *           string representation of this instance, optional
    * @throws NullPointerException
    *            if type is null
    * @throws NullPointerException
    *            if counter is null
    * @throws NullPointerException
    *            if operator is null
    */
   public CounterStoppingCondition(
         final Statistics stats,
         final OperationType type,
         final Counter counter,
         final boolean interval,
         final RelationalOperator operator,
         final long rightOperand,
         final String toString)
   {
      super(operator, rightOperand);
      Validate.notNull(stats, "stats must not be null");
      Validate.notNull(type, "type must not be null");
      Validate.notNull(counter, "counter must not be null");
      this.stats = stats;
      this.type = type;
      this.counter = counter;
      this.interval = interval;
      this.toString = toString;
   }

   @Override
   protected long leftOperand()
   {
      return this.stats.getCounter(this.type, this.counter, this.interval);
   }

   @Override
   public String toString()
   {
      // return legacy string for backwards compatibility
      if (this.toString != null)
         return this.toString;

      return String.format("OperationType.%s, Counter.%s %s %d", this.type, this.counter,
            this.operator, this.rightOperand);
   }
}
