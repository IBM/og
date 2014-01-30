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
// Date: Nov 20, 2013
// ---------------------

package com.cleversafe.oom.test.condition;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.TimeUnit;

import com.cleversafe.oom.statistic.Statistics;

/**
 * A <code>StoppingCondition</code> implementation that triggers when the configured duration has
 * been met or exceeded.
 */
public class DurationStoppingCondition implements StoppingCondition
{
   private final Statistics stats;
   private final long duration;
   private final TimeUnit unit;

   /**
    * Constructs an instance which triggers after the configured duration has been met or exceeded.
    * 
    * @param stats
    *           the stats instance to use for checking duration
    * @param duration
    *           the duration this instance should use for checking
    * @param unit
    *           the units of the specified duration
    * @throws NullPointerException
    *            if stats is null
    * @throws IllegalArgumentException
    *            if duration is negative
    * @throws NullPointerException
    *            if unit is null
    */
   public DurationStoppingCondition(final Statistics stats, final long duration, final TimeUnit unit)
   {
      checkNotNull(stats, "stats must not be null");
      checkArgument(duration >= 0, "duration must be >= 0 [%s]", duration);
      checkNotNull(unit, "unit must not be null");
      this.stats = stats;
      this.duration = duration;
      this.unit = unit;
   }

   @Override
   public boolean triggered()
   {
      return this.unit.convert(this.stats.getDuration(false), TimeUnit.NANOSECONDS) >= this.duration;
   }

   @Override
   public String toString()
   {
      // return legacy string for backwards compatibility
      return "run-time exceeded";
   }
}
