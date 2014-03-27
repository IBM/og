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

import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.oom.api.OperationManager;
import com.cleversafe.oom.api.OperationManagerException;
import com.cleversafe.oom.client.Client;
import com.cleversafe.oom.operation.Request;
import com.cleversafe.oom.operation.Response;
import com.google.common.util.concurrent.ListenableFuture;

public class LoadTest
{
   private static Logger _logger = LoggerFactory.getLogger(LoadTest.class);
   private final OperationManager operationManager;
   private final Client client;
   private final ExecutorService executorService;

   public LoadTest(
         final OperationManager operationManager,
         final Client client,
         final ExecutorService executorService)
   {
      this.operationManager = checkNotNull(operationManager, "operationManager must not be null");
      this.client = checkNotNull(client, "client must not be null");
      this.executorService = checkNotNull(executorService, "executorService must not be null");
   }

   public void runTest()
   {
      try
      {
         while (true)
         {
            final Request nextRequest = this.operationManager.next();
            final ListenableFuture<Response> future = this.client.execute(nextRequest);
            future.addListener(getListener(future), this.executorService);
         }
      }
      catch (final OperationManagerException e)
      {
         _logger.error("", e);
      }
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
         }
         catch (final Exception e)
         {
            // TODO fix this
            _logger.error("", e);
         }
      }
   }
}
