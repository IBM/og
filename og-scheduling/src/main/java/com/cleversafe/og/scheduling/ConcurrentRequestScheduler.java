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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.util.Pair;
import com.google.common.eventbus.Subscribe;

public class ConcurrentRequestScheduler implements Scheduler
{
   private static final Logger _logger = LoggerFactory.getLogger(ConcurrentRequestScheduler.class);
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
         _logger.info("Interrupted while waiting to schedule next request", e);
         return;
      }
   }

   @Subscribe
   public void complete(final Pair<Request, Response> operation)
   {
      this.sem.release();
   }
}
