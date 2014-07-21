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
// Date: Feb 6, 2014
// ---------------------

package com.cleversafe.og.test;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.client.Client;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.operation.manager.OperationManager;
import com.cleversafe.og.operation.manager.OperationManagerException;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.util.Pair;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class LoadTest implements Callable<Boolean>
{
   private static final Logger _logger = LoggerFactory.getLogger(LoadTest.class);
   private final OperationManager operationManager;
   private final Client client;
   private final Scheduler scheduler;
   private final EventBus eventBus;
   private volatile boolean running;
   private volatile boolean success;

   @Inject
   public LoadTest(
         final OperationManager operationManager,
         final Client client,
         final Scheduler scheduler,
         final EventBus eventBus)
   {
      this.operationManager = checkNotNull(operationManager);
      this.client = checkNotNull(client);
      this.scheduler = checkNotNull(scheduler);
      this.eventBus = checkNotNull(eventBus);
      this.running = true;
      this.success = true;
   }

   @Override
   public Boolean call()
   {
      try
      {
         while (this.running)
         {
            final Request request = this.operationManager.next();
            final ListenableFuture<Response> future = this.client.execute(request);
            addCallback(request, future);
            this.scheduler.waitForNext();
         }
      }
      catch (final OperationManagerException e)
      {
         this.success = false;
         this.running = false;
         _logger.error("Exception while producing request", e);
         return this.success;
      }

      return this.success;
   }

   public void stopTest()
   {
      this.running = false;
   }

   private void addCallback(final Request request, final ListenableFuture<Response> future)
   {
      Futures.addCallback(future, new FutureCallback<Response>()
      {
         @Override
         public void onSuccess(final Response result)
         {
            final Pair<Request, Response> operation = new Pair<Request, Response>(request, result);
            LoadTest.this.eventBus.post(operation);
         }

         @Override
         public void onFailure(final Throwable t)
         {
            _logger.error("Exception while processing operation", t);
            LoadTest.this.success = false;
            LoadTest.this.running = false;
         }
      });
   }
}
