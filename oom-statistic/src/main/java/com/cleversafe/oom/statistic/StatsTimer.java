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
// Date: Nov 05, 2013
// ---------------------

package com.cleversafe.oom.statistic;

import org.apache.commons.lang3.Validate;

/**
 * A simple timer implementation. A <code>StatsTimer</code> instance can be started and stopped
 * multiple times.
 */
public class StatsTimer
{
   private long timestamp;

   /**
    * Constructs a <code>StatsTimer</code> instance without an initial start value.
    */
   public StatsTimer()
   {}

   /**
    * Constructs a <code>StatsTimer</code> instance with an initial start value that matches the
    * specified timer.
    * 
    * @param timer
    *           the existing timer to construct this instance from
    * @throws NullPointerException
    *            if timer is null
    */
   public StatsTimer(final StatsTimer timer)
   {
      Validate.notNull(timer, "timer must not be null");
      this.timestamp = timer.timestamp;
   }

   /**
    * Starts this timer's internal clock
    * 
    * @param timestamp
    *           the timestamp to start this timer at
    * @return the timestamp used to start this timer
    * @throws IllegalArgumentException
    *            if timestamp is negative
    */
   public long startTimer(final long timestamp)
   {
      Validate.isTrue(timestamp >= 0, "timestamp must be >= 0 [%s]", timestamp);
      this.timestamp = timestamp;
      return timestamp;
   }

   /**
    * Stops this timer's internal clock
    * 
    * @param timestamp
    *           the timestamp to stop this timer at
    * @return the elapsed duration between the start and stop of this timer
    * @throws IllegalArgumentException
    *            if timestamp is less than the timestamp used to start this timer
    */
   public long stopTimer(final long timestamp)
   {
      Validate.isTrue(timestamp >= this.timestamp,
            "timestamp must be >= startTimer timestamp");
      return timestamp - this.timestamp;
   }
}
