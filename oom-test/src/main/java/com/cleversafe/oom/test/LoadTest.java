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

import com.cleversafe.oom.client.Client;
import com.cleversafe.oom.operation.Operation;
import com.cleversafe.oom.operation.manager.OperationManager;
import com.cleversafe.oom.scheduling.Scheduler;
import com.cleversafe.oom.test.condition.StoppingCondition;
import com.google.common.util.concurrent.ListenableFuture;

public class LoadTest<T extends Operation>
{
   private final OperationManager<T> operationManager;
   private final Scheduler<T> scheduler;
   private final Client<T> client;
   private final List<StoppingCondition> stoppingConditions;
   private final boolean shouldSchedule;
   private final ExecutorService executorService;

   public LoadTest(
         final OperationManager<T> operationManager,
         final Scheduler<T> scheduler,
         final Client<T> client,
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

   public void runTest()
   {
      while (shouldSchedule())
      {
         final T nextOperation = this.operationManager.next();
         final ListenableFuture<T> future = this.client.execute(nextOperation);
         future.addListener(getListener(nextOperation), this.executorService);

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

   private Runnable getListener(final T operation)
   {
      return new OperationCallback(operation);
   }

   private class OperationCallback implements Runnable
   {
      private final T operation;

      public OperationCallback(final T operation)
      {
         this.operation = operation;
      }

      @Override
      public void run()
      {
         LoadTest.this.operationManager.complete(this.operation);
         LoadTest.this.scheduler.complete(this.operation);
      }
   }
}
