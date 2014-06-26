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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.client.Client;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.operation.manager.OperationManager;
import com.cleversafe.og.operation.manager.OperationManagerException;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.test.condition.TestCondition;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class LoadTest
{
   private static Logger _logger = LoggerFactory.getLogger(LoadTest.class);
   private final OperationManager operationManager;
   private final Client client;
   private final Statistics stats;
   private final List<TestCondition> testConditions;
   private final Map<Long, Request> pendingRequests;
   private final ExecutorService executorService;
   AtomicBoolean running;

   public LoadTest(
         final OperationManager operationManager,
         final Client client,
         final Statistics stats,
         final List<TestCondition> testConditions,
         final Map<Long, Request> pendingRequests)
   {
      this.operationManager = checkNotNull(operationManager);
      this.client = checkNotNull(client);
      this.stats = checkNotNull(stats);
      this.testConditions = checkNotNull(testConditions);
      this.pendingRequests = checkNotNull(pendingRequests);
      final ThreadFactory fac = new ThreadFactoryBuilder().setNameFormat("test-%d").build();
      this.executorService = Executors.newCachedThreadPool(fac);
      this.running = new AtomicBoolean(true);
   }

   public boolean runTest()
   {
      try
      {
         while (isRunning())
         {
            final Request nextRequest = this.operationManager.next();
            this.pendingRequests.put(nextRequest.getId(), nextRequest);
            if (this.running.get())
            {
               final ListenableFuture<Response> future = this.client.execute(nextRequest);
               future.addListener(getListener(future), this.executorService);
            }
         }
      }
      catch (final OperationManagerException e)
      {
         _logger.error("", e);
         return false;
      }
      finally
      {
         final ListenableFuture<Boolean> complete = this.client.shutdown(true);
         try
         {
            complete.get(5, TimeUnit.SECONDS);
         }
         catch (final Exception e)
         {
            _logger.error("Exception while waiting for client shutdown completion", e);
         }
         final boolean success =
               MoreExecutors.shutdownAndAwaitTermination(this.executorService, 5, TimeUnit.SECONDS);

         if (!success)
            _logger.error("Error while shutting down executor service");
      }
      return true;
   }

   public boolean isRunning()
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
            final Request request = LoadTest.this.pendingRequests.get(response.getRequestId());
            LoadTest.this.stats.update(request, response);
            LoadTest.this.pendingRequests.remove(response.getRequestId());
         }
         catch (final Exception e)
         {
            // TODO fix this
            _logger.error("", e);
         }
      }
   }
}
