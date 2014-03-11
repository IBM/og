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

package com.cleversafe.oom.test;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.cleversafe.oom.api.OperationManager;
import com.cleversafe.oom.api.OperationManagerException;
import com.cleversafe.oom.client.Client;
import com.cleversafe.oom.operation.Request;
import com.cleversafe.oom.operation.Response;
import com.cleversafe.oom.scheduling.Scheduler;
import com.cleversafe.oom.test.condition.StoppingCondition;
import com.google.common.util.concurrent.ListenableFuture;

public class LoadTest
{
   private final OperationManager operationManager;
   private final Scheduler scheduler;
   private final Client client;
   private final List<StoppingCondition> stoppingConditions;
   private final boolean shouldSchedule;
   private final ExecutorService executorService;

   public LoadTest(
         final OperationManager operationManager,
         final Scheduler scheduler,
         final Client client,
         final List<StoppingCondition> stoppingConditions)
   {
      this.operationManager = checkNotNull(operationManager, "operationManager must not be null");
      this.scheduler = checkNotNull(scheduler, "scheduler must not be null");
      this.client = checkNotNull(client, "client must not be null");
      this.stoppingConditions =
            checkNotNull(stoppingConditions, "stoppingConditions must not be null");
      this.shouldSchedule = true;
      this.executorService = Executors.newCachedThreadPool();
   }

   // TODO runTest should not throw an exception
   public void runTest() throws OperationManagerException
   {
      while (shouldSchedule())
      {
         final Request nextRequest = this.operationManager.next();
         final ListenableFuture<Response> future = this.client.execute(nextRequest);
         future.addListener(getListener(future), this.executorService);

         this.scheduler.waitForNext();
      }
      // clean up test
      this.executorService.shutdownNow();
   }

   private boolean shouldSchedule()
   {
      // once triggered, should always return false
      if (!this.shouldSchedule)
         return false;

      for (final StoppingCondition condition : this.stoppingConditions)
      {
         if (condition.triggered())
            return false;
      }
      return true;
   }

   public void stopTest()
   {
      this.stoppingConditions.add(new StoppingCondition()
      {
         @Override
         public boolean triggered()
         {
            return true;
         }
      });
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
            LoadTest.this.operationManager.complete(this.future.get());
            LoadTest.this.scheduler.complete(this.future.get());
         }
         catch (final Exception e)
         {
            // TODO fix this
         }
      }
   }
}
