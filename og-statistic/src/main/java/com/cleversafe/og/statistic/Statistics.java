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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.http.util.HttpUtil;
import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.util.Operation;
import com.cleversafe.og.util.Pair;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AtomicLongMap;

public class Statistics
{
   private static final Logger _logger = LoggerFactory.getLogger(Statistics.class);
   private final Map<Operation, AtomicLongMap<Counter>> counters;
   private final Map<Operation, AtomicLongMap<Integer>> scCounters;

   public Statistics()
   {
      this.counters = new HashMap<Operation, AtomicLongMap<Counter>>();
      this.scCounters = new HashMap<Operation, AtomicLongMap<Integer>>();
      for (final Operation operation : Operation.values())
      {
         this.counters.put(operation, AtomicLongMap.<Counter> create());
         this.scCounters.put(operation, AtomicLongMap.<Integer> create());
      }
   }

   @Subscribe
   public void update(final Pair<Request, Response> result)
   {
      checkNotNull(result);
      final Request request = result.getKey();
      final Response response = result.getValue();

      final Operation operation = HttpUtil.toOperation(request.getMethod());
      updateCounter(operation, Counter.OPERATIONS, 1);
      updateCounter(Operation.ALL, Counter.OPERATIONS, 1);
      final long bytes = getBytes(operation, request, response);
      updateCounter(operation, Counter.BYTES, bytes);
      updateCounter(Operation.ALL, Counter.BYTES, bytes);
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

   private long getBytes(final Operation operation, final Request request, final Response response)
   {
      if (Operation.WRITE == operation)
         return request.getEntity().getSize();
      else if (Operation.READ == operation)
         return response.getEntity().getSize();
      return 0;
   }

   private void updateCounter(final Operation operation, final Counter counter, final long value)
   {
      this.counters.get(operation).addAndGet(counter, value);
   }

   private void updateStatusCode(final Operation operation, final int statusCode)
   {
      this.scCounters.get(operation).incrementAndGet(statusCode);
   }

   public long get(final Operation operation, final Counter counter)
   {
      checkNotNull(operation);
      checkNotNull(counter);
      return this.counters.get(operation).get(counter);
   }

   public long getStatusCode(final Operation operation, final int statusCode)
   {
      checkNotNull(operation);
      checkArgument(HttpUtil.VALID_STATUS_CODES.contains(statusCode),
            "statusCode must be a valid status code [%s]", statusCode);

      return this.scCounters.get(operation).get(statusCode);
   }

   public Iterator<Entry<Integer, Long>> statusCodeIterator(final Operation operation)
   {
      checkNotNull(operation);
      return new TreeMap<Integer, Long>(this.scCounters.get(operation).asMap()).entrySet().iterator();
   }
}
