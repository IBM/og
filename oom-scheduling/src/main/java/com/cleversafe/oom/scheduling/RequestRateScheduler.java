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
import com.cleversafe.oom.operation.Operation;

public class RequestRateScheduler<T extends Operation> implements Scheduler<T>
{
   private final Distribution sleepDuration;
   private final TimeUnit unit;

   public RequestRateScheduler(final Distribution sleepDuration, final TimeUnit unit)
   {
      this.sleepDuration = checkNotNull(sleepDuration, "sleepDuration must not be null");
      this.unit = checkNotNull(unit, "unit must not be null");
   }

   @Override
   public void waitForNext()
   {
      final long nextSleepDuration = (long) this.sleepDuration.nextSample();
      long sleepRemaining = nextSleepDuration;
      while (sleepRemaining > 0)
      {
         final long beginSleepTime = System.nanoTime();
         try
         {
            this.unit.sleep(sleepRemaining);
         }
         catch (final InterruptedException e)
         {
         }
         final long sleptTime = System.nanoTime() - beginSleepTime;
         sleepRemaining -= TimeUnit.NANOSECONDS.convert(sleptTime, this.unit);
      }
   }

   @Override
   public void complete(final T operation)
   {
      // do nothing, request rate is independent of operation completion
   }
}
