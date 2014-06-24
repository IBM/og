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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.http.util.MethodUtil;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.util.OperationType;
import com.google.common.eventbus.EventBus;

public class Statistics
{
   private static Logger _logger = LoggerFactory.getLogger(Statistics.class);
   private final Map<OperationType, Map<Counter, AtomicLong>> counters;
   // use concurrent hashmap at status code level, as additional status code keys may be mapped
   // concurrently during the lifetime of the test
   private final Map<OperationType, ConcurrentNavigableMap<Integer, AtomicLong>> scCounters;
   private final EventBus eventBus;
   private AtomicLong unmappedSC;

   public Statistics(final EventBus eventBus)
   {
      this.counters = new HashMap<OperationType, Map<Counter, AtomicLong>>();
      this.scCounters = new HashMap<OperationType, ConcurrentNavigableMap<Integer, AtomicLong>>();
      for (final OperationType operation : OperationType.values())
      {
         this.counters.put(operation, new HashMap<Counter, AtomicLong>());

         this.scCounters.put(operation, new ConcurrentSkipListMap<Integer, AtomicLong>());
         for (final Counter counter : Counter.values())
         {
            this.counters.get(operation).put(counter, new AtomicLong());
         }
      }
      this.eventBus = checkNotNull(eventBus);
      this.unmappedSC = new AtomicLong(1);
   }

   public void update(final Request request, final Response response)
   {
      final OperationType operation = MethodUtil.toOperationType(request.getMethod());
      updateCounter(operation, Counter.OPERATIONS, 1);
      updateCounter(OperationType.ALL, Counter.OPERATIONS, 1);
      if (response.getMetaDataEntry("exception") != null)
      {
         updateCounter(operation, Counter.ABORTS, 1);
         updateCounter(OperationType.ALL, Counter.ABORTS, 1);
      }
      else
      {
         updateStatusCode(operation, response.getStatusCode());
         updateStatusCode(OperationType.ALL, response.getStatusCode());
      }
      this.eventBus.post(this);
   }

   private void updateCounter(final OperationType operation, final Counter counter, final long value)
   {
      this.counters.get(operation).get(counter).addAndGet(value);
   }

   private void updateStatusCode(final OperationType operation, final int statusCode)
   {
      final AtomicLong existingSC =
            this.scCounters.get(operation).putIfAbsent(statusCode, this.unmappedSC);

      if (existingSC != null)
         existingSC.incrementAndGet();
      else
         this.unmappedSC = new AtomicLong(1);
   }

   public long get(final OperationType operation, final Counter counter)
   {
      checkNotNull(operation);
      checkNotNull(counter);
      return this.counters.get(operation).get(counter).get();
   }

   public long getStatusCode(final OperationType operation, final int statusCode)
   {
      final AtomicLong sc = this.scCounters.get(operation).get(statusCode);
      if (sc != null)
         return sc.get();
      return 0;
   }

   // TODO clean this up, would be nice not to expose AtomicLong and other internal details
   public Iterator<Entry<Integer, AtomicLong>> statusCodeIterator(final OperationType operation)
   {
      checkNotNull(operation);
      return this.scCounters.get(operation).entrySet().iterator();
   }
}
