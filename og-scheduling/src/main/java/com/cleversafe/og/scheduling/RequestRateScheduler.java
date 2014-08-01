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

package com.cleversafe.og.scheduling;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.util.distribution.Distribution;

/**
 * A scheduler which permits calls at a configured rate
 * 
 * @since 1.0
 */
public class RequestRateScheduler implements Scheduler
{
   private static final Logger _logger = LoggerFactory.getLogger(RequestRateScheduler.class);
   private final Distribution count;
   private final TimeUnit unit;
   private long lastCalledTimestamp;

   /**
    * Construcst an instace using the provided rate {@code count / unit }
    * 
    * @param count
    *           the numerator of the rate to configure
    * @param unit
    *           the denominator of the rate to configure
    */
   public RequestRateScheduler(final Distribution count, final TimeUnit unit)
   {
      this.count = checkNotNull(count);
      this.unit = checkNotNull(unit);
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
            _logger.info("Interrupted while waiting to schedule next request", e);
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
}
