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
// Date: Feb 10, 2014
// ---------------------

package com.cleversafe.og.scheduling;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.concurrent.Semaphore;

import com.cleversafe.og.operation.Response;

public class ConcurrentRequestScheduler implements Scheduler
{
   private final Semaphore sem;

   public ConcurrentRequestScheduler(final int concurrentRequests)
   {
      checkArgument(concurrentRequests > 0, "concurrentRequests must be > 0");
      this.sem = new Semaphore(concurrentRequests - 1);
   }

   @Override
   public void waitForNext()
   {
      try
      {
         this.sem.acquire();
      }
      catch (final InterruptedException e)
      {
         return;
      }
   }

   @Override
   public void complete(final Response response)
   {
      this.sem.release();
   }
}
