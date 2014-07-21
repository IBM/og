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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class LoadTest implements Callable<Boolean>
{
   private static final Logger _logger = LoggerFactory.getLogger(LoadTest.class);
   private final OperationManager operationManager;
   private final Client client;
   private final Scheduler scheduler;
   private final EventBus eventBus;
   private final ExecutorService executorService;
   private final AtomicBoolean running;
   private final AtomicBoolean success;

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
      final ThreadFactory fac = new ThreadFactoryBuilder().setNameFormat("test-%d").build();
      this.executorService = Executors.newCachedThreadPool(fac);
      this.running = new AtomicBoolean(true);
      this.success = new AtomicBoolean(true);
   }

   @Override
   public Boolean call()
   {
      try
      {
         while (this.running.get())
         {
            final Request nextRequest = this.operationManager.next();
            final ListenableFuture<Response> future = this.client.execute(nextRequest);
            future.addListener(getListener(nextRequest, future), this.executorService);
            this.scheduler.waitForNext();
         }
      }
      catch (final OperationManagerException e)
      {
         this.success.set(false);
         this.running.set(false);
         _logger.error("Exception while producing request", e);
         return this.success.get();
      }
      finally
      {
         final boolean shutdownSuccess =
               MoreExecutors.shutdownAndAwaitTermination(this.executorService, 5, TimeUnit.SECONDS);

         if (!shutdownSuccess)
            _logger.error("Error while shutting down executor service");
      }
      return this.success.get();
   }

   public void stopTest()
   {
      this.running.set(false);
   }

   private Runnable getListener(final Request request, final ListenableFuture<Response> future)
   {
      return new RequestCallback(request, future);
   }

   private class RequestCallback implements Runnable
   {
      private final Request request;
      private final ListenableFuture<Response> future;

      public RequestCallback(final Request request, final ListenableFuture<Response> future)
      {
         this.request = request;
         this.future = future;
      }

      @Override
      public void run()
      {
         try
         {
            final Response response = this.future.get();
            final Pair<Request, Response> operation =
                  new Pair<Request, Response>(this.request, response);
            LoadTest.this.eventBus.post(operation);
         }
         catch (final Exception e)
         {
            LoadTest.this.success.set(false);
            LoadTest.this.running.set(false);
            _logger.error("Exception while consuming response", e);
         }
      }
   }
}
