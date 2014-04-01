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
// Date: Feb 7, 2014
// ---------------------

package com.cleversafe.oom.scheduling;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.TimeUnit;

import com.cleversafe.oom.distribution.Distribution;
import com.cleversafe.oom.operation.Response;

public class RequestRateScheduler implements Scheduler
{
   private final Distribution count;
   private final TimeUnit unit;
   private long lastCalledTimestamp;

   public RequestRateScheduler(final Distribution count, final TimeUnit unit)
   {
      this.count = checkNotNull(count, "count must not be null");
      this.unit = checkNotNull(unit, "unit must not be null");
   }

   @Override
   public void waitForNext()
   {
      long timestamp = System.nanoTime();
      long sleepRemaining = nextSleepDuration() - adjustment(timestamp);
      while (sleepRemaining > 0)
      {
         try
         {
            TimeUnit.NANOSECONDS.sleep(sleepRemaining);
         }
         catch (final InterruptedException e)
         {
            this.lastCalledTimestamp = System.nanoTime();
            return;
         }
         final long endTimestamp = System.nanoTime();
         final long sleptTime = endTimestamp - timestamp;
         timestamp = endTimestamp;
         sleepRemaining -= sleptTime;
      }
      this.lastCalledTimestamp = timestamp;
   }

   private final long nextSleepDuration()
   {
      return (long) (this.unit.toNanos(1) / this.count.nextSample());
   }

   private final long adjustment(final long timestamp)
   {
      if (this.lastCalledTimestamp > 0)
         return timestamp - this.lastCalledTimestamp;
      return 0;
   }

   @Override
   public void complete(final Response response)
   {
      // do nothing, request rate is independent of operation completion
   }
}
