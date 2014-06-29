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
// Date: Jun 22, 2014
// ---------------------

package com.cleversafe.og.test.condition;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.util.Operation;

public class StatusCodeCondition implements TestCondition
{
   private static final Logger _logger = LoggerFactory.getLogger(StatusCodeCondition.class);
   private final Operation operation;
   private final int statusCode;
   private final long thresholdValue;

   public StatusCodeCondition(
         final Operation operation,
         final int statusCode,
         final long thresholdValue)
   {
      this.operation = checkNotNull(operation);
      // TODO use guava range
      checkArgument(statusCode >= 100 && statusCode <= 599,
            "statusCode must be in range [100, 599] [%s]", statusCode);
      this.statusCode = statusCode;
      checkArgument(thresholdValue > 0, "thresholdValue must be > 0 [%s]", thresholdValue);
      this.thresholdValue = thresholdValue;
   }

   @Override
   public boolean isTriggered(final Statistics stats)
   {
      final long currentValue = stats.getStatusCode(this.operation, this.statusCode);
      if (currentValue >= this.thresholdValue)
         return true;
      return false;
   }
}
