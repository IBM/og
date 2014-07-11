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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.client.Client;
import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.operation.manager.OperationManager;
import com.cleversafe.og.operation.manager.OperationManagerException;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.test.condition.TestCondition;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.Uninterruptibles;

public class LoadTest
{
   private static final Logger _logger = LoggerFactory.getLogger(LoadTest.class);
   private final OperationManager operationManager;
   private final Client client;
   private final Scheduler scheduler;
   private final Statistics stats;
   private final List<TestCondition> testConditions;
   private final Map<String, Request> pendingRequests;
   private final ExecutorService executorService;
   private final Thread testThread;
   private final AtomicBoolean running;
   private final AtomicBoolean success;

   public LoadTest(
         final OperationManager operationManager,
         final Client client,
         final Scheduler scheduler,
         final Statistics stats,
         final List<TestCondition> testConditions,
         final Map<String, Request> pendingRequests)
   {
      this.operationManager = checkNotNull(operationManager);
      this.client = checkNotNull(client);
      this.scheduler = checkNotNull(scheduler);
      this.stats = checkNotNull(stats);
      this.testConditions = checkNotNull(testConditions);
      this.pendingRequests = checkNotNull(pendingRequests);
      final ThreadFactory fac = new ThreadFactoryBuilder().setNameFormat("test-%d").build();
      this.executorService = Executors.newCachedThreadPool(fac);
      this.testThread = Thread.currentThread();
      this.running = new AtomicBoolean(true);
      this.success = new AtomicBoolean(true);
   }

   public boolean runTest()
   {
      try
      {
         while (isRunning())
         {
            final Request nextRequest = this.operationManager.next();
            this.pendingRequests.put(nextRequest.getMetadata(Metadata.REQUEST_ID), nextRequest);
            final ListenableFuture<Response> future = this.client.execute(nextRequest);
            future.addListener(getListener(future), this.executorService);
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
         final ListenableFuture<Boolean> complete = this.client.shutdown(true);
         try
         {
            Uninterruptibles.getUninterruptibly(complete);
         }
         catch (final ExecutionException e)
         {
            this.success.set(false);
            _logger.error("Exception while waiting for client shutdown completion", e);
         }
         final boolean shutdownSuccess =
               MoreExecutors.shutdownAndAwaitTermination(this.executorService, 5, TimeUnit.SECONDS);

         if (!shutdownSuccess)
            _logger.error("Error while shutting down executor service");
      }
      return this.success.get();
   }

   private boolean isRunning()
   {
      if (!this.running.get())
         return false;

      for (final TestCondition condition : this.testConditions)
      {
         if (condition.isTriggered(this.stats))
            return false;
      }

      return true;
   }

   public void stopTest()
   {
      this.running.set(false);
      this.testThread.interrupt();
      Uninterruptibles.joinUninterruptibly(this.testThread);
   }

   private Runnable getListener(final ListenableFuture<Response> future)
   {
      return new RequestCallback(future);
   }

   private class RequestCallback implements Runnable
   {
      private final ListenableFuture<Response> future;

      public RequestCallback(final ListenableFuture<Response> future)
      {
         this.future = future;
      }

      @Override
      public void run()
      {
         try
         {
            final Response response = this.future.get();

            LoadTest.this.operationManager.complete(response);
            LoadTest.this.scheduler.complete(response);

            final String requestId = response.getMetadata(Metadata.REQUEST_ID);
            final Request request = LoadTest.this.pendingRequests.get(requestId);
            LoadTest.this.stats.update(request, response);
            LoadTest.this.pendingRequests.remove(requestId);
         }
         catch (final Exception e)
         {
            LoadTest.this.success.set(false);
            LoadTest.this.running.set(false);
            _logger.error("Exception while consuming response", e);
            LoadTest.this.testThread.interrupt();
         }
      }
   }
}
