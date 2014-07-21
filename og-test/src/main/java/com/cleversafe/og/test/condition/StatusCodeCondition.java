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

import com.cleversafe.og.http.util.HttpUtil;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.test.LoadTest;
import com.cleversafe.og.util.Operation;
import com.cleversafe.og.util.Pair;
import com.google.common.eventbus.Subscribe;

public class StatusCodeCondition implements TestCondition
{
   private static final Logger _logger = LoggerFactory.getLogger(StatusCodeCondition.class);
   private final Operation operation;
   private final int statusCode;
   private final long thresholdValue;
   private final LoadTest test;
   private final Statistics stats;

   public StatusCodeCondition(
         final Operation operation,
         final int statusCode,
         final long thresholdValue,
         final LoadTest test,
         final Statistics stats)
   {
      this.operation = checkNotNull(operation);
      checkArgument(HttpUtil.VALID_STATUS_CODES.contains(statusCode),
            "statusCode must be a valid status code [%s]", statusCode);
      this.statusCode = statusCode;
      checkArgument(thresholdValue > 0, "thresholdValue must be > 0 [%s]", thresholdValue);
      this.thresholdValue = thresholdValue;
      this.test = checkNotNull(test);
      this.stats = checkNotNull(stats);
   }

   @Subscribe
   public void update(final Pair<Request, Response> operation)
   {
      if (isTriggered())
         this.test.stopTest();
   }

   @Override
   public boolean isTriggered()
   {
      final long currentValue = this.stats.getStatusCode(this.operation, this.statusCode);
      if (currentValue >= this.thresholdValue)
         return true;
      return false;
   }
}
