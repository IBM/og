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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.http.util.HttpUtil;
import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.util.Operation;

public class Statistics
{
   private static final Logger _logger = LoggerFactory.getLogger(Statistics.class);
   private final Map<Operation, Map<Counter, AtomicLong>> counters;
   // use concurrent hashmap at status code level, as additional status code keys may be mapped
   // concurrently during the lifetime of the test
   private final Map<Operation, ConcurrentNavigableMap<Integer, AtomicLong>> scCounters;
   private AtomicLong unmappedSC;

   public Statistics()
   {
      this.counters = new HashMap<Operation, Map<Counter, AtomicLong>>();
      this.scCounters = new HashMap<Operation, ConcurrentNavigableMap<Integer, AtomicLong>>();
      for (final Operation operation : Operation.values())
      {
         this.counters.put(operation, new HashMap<Counter, AtomicLong>());

         this.scCounters.put(operation, new ConcurrentSkipListMap<Integer, AtomicLong>());
         for (final Counter counter : Counter.values())
         {
            this.counters.get(operation).put(counter, new AtomicLong());
         }
      }
      this.unmappedSC = new AtomicLong(1);
   }

   public void update(final Request request, final Response response)
   {
      checkNotNull(request);
      checkNotNull(response);

      final Operation operation = HttpUtil.toOperation(request.getMethod());
      updateCounter(operation, Counter.OPERATIONS, 1);
      updateCounter(Operation.ALL, Counter.OPERATIONS, 1);
      if (response.getMetadata(Metadata.ABORTED) != null)
      {
         updateCounter(operation, Counter.ABORTS, 1);
         updateCounter(Operation.ALL, Counter.ABORTS, 1);
      }
      else
      {
         updateStatusCode(operation, response.getStatusCode());
         updateStatusCode(Operation.ALL, response.getStatusCode());
      }
   }

   private void updateCounter(final Operation operation, final Counter counter, final long value)
   {
      this.counters.get(operation).get(counter).addAndGet(value);
   }

   private void updateStatusCode(final Operation operation, final int statusCode)
   {
      final AtomicLong existingSC =
            this.scCounters.get(operation).putIfAbsent(statusCode, this.unmappedSC);

      if (existingSC != null)
         existingSC.incrementAndGet();
      else
         this.unmappedSC = new AtomicLong(1);
   }

   public long get(final Operation operation, final Counter counter)
   {
      checkNotNull(operation);
      checkNotNull(counter);
      return this.counters.get(operation).get(counter).get();
   }

   public long getStatusCode(final Operation operation, final int statusCode)
   {
      checkNotNull(operation);
      checkArgument(HttpUtil.VALID_STATUS_CODES.contains(statusCode),
            "statusCode must be a valid status code [%s]", statusCode);

      final AtomicLong sc = this.scCounters.get(operation).get(statusCode);
      if (sc != null)
         return sc.get();
      return 0;
   }

   public Iterator<Entry<Integer, Long>> statusCodeIterator(final Operation operation)
   {
      checkNotNull(operation);
      final Iterator<Entry<Integer, AtomicLong>> it =
            this.scCounters.get(operation).entrySet().iterator();

      return new Iterator<Entry<Integer, Long>>()
      {

         @Override
         public boolean hasNext()
         {
            return it.hasNext();
         }

         @Override
         public Entry<Integer, Long> next()
         {
            final Entry<Integer, AtomicLong> e = it.next();
            return new AbstractMap.SimpleImmutableEntry<Integer, Long>(e.getKey(),
                  e.getValue().get());
         }

         @Override
         public void remove()
         {}
      };
   }
}
