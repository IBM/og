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
// Date: Mar 24, 2014
// ---------------------

package com.cleversafe.oom.soh;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

import com.cleversafe.oom.api.Consumer;
import com.cleversafe.oom.api.OperationManager;
import com.cleversafe.oom.api.OperationManagerException;
import com.cleversafe.oom.api.Producer;
import com.cleversafe.oom.http.HttpRequestContext;
import com.cleversafe.oom.operation.OperationType;
import com.cleversafe.oom.operation.OperationTypeMix;
import com.cleversafe.oom.operation.Request;
import com.cleversafe.oom.operation.RequestContext;
import com.cleversafe.oom.operation.Response;

public class SOHOperationManager implements OperationManager
{
   private final OperationTypeMix operationTypeMix;
   private final Map<OperationType, Producer<Request>> producers;
   private final List<Consumer<Response>> consumers;

   public SOHOperationManager(
         final OperationTypeMix mix,
         final Map<OperationType, Producer<Request>> producers,
         final List<Consumer<Response>> consumers)
   {
      this.operationTypeMix = checkNotNull(mix, "operationTypeMix must not be null");
      this.producers = checkNotNull(producers, "producers must not be null");
      this.consumers = checkNotNull(consumers, "consumers must not be null");
   }

   @Override
   public Request next() throws OperationManagerException
   {
      final RequestContext context = new HttpRequestContext();
      // TODO create stats object and use it to pass an appropriate fill to getNextOperationType
      final OperationType operationType = this.operationTypeMix.getNextOperationType(1);
      final Producer<Request> producer = this.producers.get(operationType);
      try
      {
         return producer.produce(context);
      }
      catch (final Exception e)
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
   }
}
