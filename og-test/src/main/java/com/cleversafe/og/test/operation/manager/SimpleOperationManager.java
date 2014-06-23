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
// Date: Apr 7, 2014
// ---------------------

package com.cleversafe.og.test.operation.manager;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

import com.cleversafe.og.api.Consumer;
import com.cleversafe.og.api.OperationManager;
import com.cleversafe.og.api.OperationManagerException;
import com.cleversafe.og.api.Producer;
import com.cleversafe.og.api.ProducerException;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.statistic.Statistics;

public class SimpleOperationManager implements OperationManager
{
   private final Producer<Producer<Request>> requestMix;
   private final List<Consumer<Response>> consumers;
   private final Scheduler scheduler;
   private final Map<Long, Request> pendingRequests;
   private final Statistics stats;

   public SimpleOperationManager(
         final Producer<Producer<Request>> requestMix,
         final List<Consumer<Response>> consumers,
         final Scheduler scheduler,
         final Map<Long, Request> pendingRequests,
         final Statistics stats)
   {
      this.requestMix = checkNotNull(requestMix, "requestMix must not be null");
      this.consumers = checkNotNull(consumers, "consumers must not be null");
      this.scheduler = checkNotNull(scheduler, "scheduler must not be null");
      this.pendingRequests = checkNotNull(pendingRequests, "pendingRequests must not be null");
      this.stats = checkNotNull(stats, "stats must not be null");
   }

   @Override
   public Request next() throws OperationManagerException
   {
      this.scheduler.waitForNext();

      final Producer<Request> producer = this.requestMix.produce();
      try
      {
         final Request request = producer.produce();
         this.pendingRequests.put(request.getId(), request);
         return request;
      }
      catch (final ProducerException e)
      {
         throw new OperationManagerException(e);
      }
   }

   @Override
   public void complete(final Response response)
   {
      for (final Consumer<Response> consumer : this.consumers)
      {
         consumer.consume(response);
      }
      this.scheduler.complete(response);
      final Request request = this.pendingRequests.remove(response.getRequestId());
      this.stats.update(request, response);
   }
}
