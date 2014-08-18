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

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.api.Client;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.HttpResponse;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.util.Pair;
import com.google.common.base.Supplier;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;

public class LoadTest implements Callable<Boolean>
{
   private static final Logger _logger = LoggerFactory.getLogger(LoadTest.class);
   private final Supplier<Request> requestSupplier;
   private final Client client;
   private final Scheduler scheduler;
   private final EventBus eventBus;
   private volatile boolean running;
   private volatile boolean success;
   private final Map<String, ListenableFuture<Response>> activeOperations;
   private final CountDownLatch completed;

   @Inject
   public LoadTest(
         final Supplier<Request> requestSupplier,
         final Client client,
         final Scheduler scheduler,
         final EventBus eventBus,
         final LoadTestSubscriberExceptionHandler handler)
   {
      this.requestSupplier = checkNotNull(requestSupplier);
      this.client = checkNotNull(client);
      this.scheduler = checkNotNull(scheduler);
      this.eventBus = checkNotNull(eventBus);
      checkNotNull(handler).setLoadTest(this);
      this.running = true;
      this.success = true;
      this.activeOperations = new ConcurrentHashMap<String, ListenableFuture<Response>>();
      this.completed = new CountDownLatch(1);
   }

   @Override
   public Boolean call()
   {
      try
      {
         while (this.running)
         {
            final Request request = this.requestSupplier.get();
            final ListenableFuture<Response> future = this.client.execute(request);
            // TODO better key than request_id? make Request hashable?
            this.activeOperations.put(request.headers().get(Headers.X_OG_REQUEST_ID), future);
            addCallback(request, future);
            this.scheduler.waitForNext();
         }
      }
      catch (final Exception e)
      {
         this.success = false;
         this.running = false;
         _logger.error("Exception while producing request", e);
      }

      if (!this.activeOperations.isEmpty())
         Uninterruptibles.awaitUninterruptibly(this.completed);
      return this.success;
   }

   public void stopTest()
   {
      this.running = false;
      for (final Entry<String, ListenableFuture<Response>> operation : this.activeOperations.entrySet())
      {
         operation.getValue().cancel(true);
      }
   }

   public void abortTest()
   {
      this.success = false;
      stopTest();
   }

   private void addCallback(final Request request, final ListenableFuture<Response> future)
   {
      Futures.addCallback(future, new FutureCallback<Response>()
      {
         @Override
         public void onSuccess(final Response result)
         {
            removeActiveOperation();
            postOperation(result);

         }

         @Override
         public void onFailure(final Throwable t)
         {
            removeActiveOperation();
            _logger.error("Exception while processing operation", t);
            LoadTest.this.running = false;
            final HttpResponse response = new HttpResponse.Builder().withStatusCode(599).build();
            postOperation(response);
         }

         private void removeActiveOperation()
         {
            LoadTest.this.activeOperations.remove(request.headers().get(Headers.X_OG_REQUEST_ID));
            if (!LoadTest.this.running && LoadTest.this.activeOperations.isEmpty())
               LoadTest.this.completed.countDown();
         }

         private void postOperation(final Response response)
         {
            LoadTest.this.eventBus.post(Pair.of(request, response));
         }
      });
   }

   @Override
   public String toString()
   {
      return String.format("LoadTest [%nrequestSupplier=%s,%n%nscheduler=%s,%n%nclient=%s%n]",
            this.requestSupplier,
            this.scheduler,
            this.client);
   }
}
