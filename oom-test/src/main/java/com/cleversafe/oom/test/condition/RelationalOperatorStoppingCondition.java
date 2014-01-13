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

/**
 * A <code>StoppingCondition</code> abstract class for relational comparisons
 */
public abstract class RelationalOperatorStoppingCondition implements StoppingCondition
{
   protected final RelationalOperator operator;
   protected final long rightOperand;

   /**
    * Constructs an instance which triggers on comparison to a <code>RelationalOperator</code>
    * comparison.
    * 
    * @param operator
    *           the relational operator to use for comparison
    * @param rightOperand
    *           the operand to place on the right hand side of the comparison
    * @throws NullPointerException
    *            if operator is null
    */
   public RelationalOperatorStoppingCondition(
         final RelationalOperator operator,
         final long rightOperand)
   {
      Validate.notNull(operator, "operator must not be null");
      this.operator = operator;
      this.rightOperand = rightOperand;
   }

   protected abstract long leftOperand();

   @Override
   public boolean triggered()
   {
      final long leftOperand = leftOperand();

      switch (this.operator)
      {
         case NE :
            return leftOperand != this.rightOperand;
         case GT :
            return leftOperand > this.rightOperand;
         case LT :
            return leftOperand < this.rightOperand;
         case GE :
            return leftOperand >= this.rightOperand;
         case LE :
            return leftOperand <= this.rightOperand;
         default :
            return leftOperand == this.rightOperand;
      }
   }
}
