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
// Date: Jun 21, 2014
// ---------------------

package com.cleversafe.og.statistic;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.operation.Method;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.util.OperationType;
import com.google.common.eventbus.EventBus;

public class Statistics
{
   private static Logger _logger = LoggerFactory.getLogger(Statistics.class);
   Map<OperationType, Map<Counter, AtomicLong>> counters;
   EventBus eventBus;

   public Statistics(final EventBus eventBus)
   {
      this.counters = new HashMap<OperationType, Map<Counter, AtomicLong>>();
      for (final OperationType operation : OperationType.values())
      {
         this.counters.put(operation, new HashMap<Counter, AtomicLong>());
         for (final Counter counter : Counter.values())
         {
            this.counters.get(operation).put(counter, new AtomicLong());
         }
      }
      this.eventBus = checkNotNull(eventBus, "eventBus must not be null");
   }

   public void update(final Request request, final Response response)
   {
      final OperationType operation = fromMethod(request.getMethod());
      this.counters.get(operation).get(Counter.OPERATIONS).addAndGet(1);
      this.counters.get(OperationType.ALL).get(Counter.OPERATIONS).addAndGet(1);
      this.eventBus.post(this);
   }

   public long get(final OperationType operation, final Counter counter)
   {
      checkNotNull(operation, "operation must not be null");
      checkNotNull(counter, "counter must not be null");
      return this.counters.get(operation).get(counter).get();
   }

   private OperationType fromMethod(final Method method)
   {
      switch (method)
      {
         case GET :
         case HEAD :
            return OperationType.READ;
         case POST :
         case PUT :
            return OperationType.WRITE;
         case DELETE :
            return OperationType.DELETE;
         default :
            throw new RuntimeException(String.format("Unrecognized method [%s]", method));
      }
   }
}
