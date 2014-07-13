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

import javax.inject.Inject;

import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.operation.manager.OperationManager;
import com.cleversafe.og.operation.manager.OperationManagerException;
import com.cleversafe.og.util.Pair;
import com.cleversafe.og.util.consumer.Consumer;
import com.cleversafe.og.util.producer.Producer;
import com.cleversafe.og.util.producer.ProducerException;

public class SimpleOperationManager implements OperationManager
{
   private final Producer<Producer<Request>> requestMix;
   private final List<Consumer<Pair<Request, Response>>> consumers;

   @Inject
   public SimpleOperationManager(
         final Producer<Producer<Request>> requestMix,
         final List<Consumer<Pair<Request, Response>>> consumers)
   {
      this.requestMix = checkNotNull(requestMix);
      this.consumers = checkNotNull(consumers);
   }

   @Override
   public Request next() throws OperationManagerException
   {
      try
      {
         final Producer<Request> producer = this.requestMix.produce();
         return producer.produce();
      }
      catch (final ProducerException e)
      {
         throw new OperationManagerException(e);
      }
   }

   @Override
   public void complete(final Request request, final Response response)
   {
      final Pair<Request, Response> operation = new Pair<Request, Response>(request, response);
      for (final Consumer<Pair<Request, Response>> consumer : this.consumers)
      {
         consumer.consume(operation);
      }
   }
}
