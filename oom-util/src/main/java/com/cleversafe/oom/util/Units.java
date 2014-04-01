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
// Date: Mar 31, 2014
// ---------------------

package com.cleversafe.oom.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Units
{
   private static final Map<String, TimeUnit> TIME_UNITS;

   static
   {
      TIME_UNITS = new HashMap<String, TimeUnit>();
      TIME_UNITS.put("NS", TimeUnit.NANOSECONDS);
      TIME_UNITS.put("NANO", TimeUnit.NANOSECONDS);
      TIME_UNITS.put("NANOS", TimeUnit.NANOSECONDS);
      TIME_UNITS.put("NANOSEC", TimeUnit.NANOSECONDS);
      TIME_UNITS.put("NANOSECS", TimeUnit.NANOSECONDS);
      TIME_UNITS.put("NANOSECOND", TimeUnit.NANOSECONDS);
      TIME_UNITS.put("NANOSECONDS", TimeUnit.NANOSECONDS);

      TIME_UNITS.put("MICRO", TimeUnit.MICROSECONDS);
      TIME_UNITS.put("MICROS", TimeUnit.MICROSECONDS);
      TIME_UNITS.put("MICROSEC", TimeUnit.MICROSECONDS);
      TIME_UNITS.put("MICROSECS", TimeUnit.MICROSECONDS);
      TIME_UNITS.put("MICROSECOND", TimeUnit.MICROSECONDS);
      TIME_UNITS.put("MICROSECONDS", TimeUnit.MICROSECONDS);

      TIME_UNITS.put("MS", TimeUnit.MILLISECONDS);
      TIME_UNITS.put("MILLI", TimeUnit.MILLISECONDS);
      TIME_UNITS.put("MILLIS", TimeUnit.MILLISECONDS);
      TIME_UNITS.put("MILLISEC", TimeUnit.MILLISECONDS);
      TIME_UNITS.put("MILLISECS", TimeUnit.MILLISECONDS);
      TIME_UNITS.put("MILLISECOND", TimeUnit.MILLISECONDS);
      TIME_UNITS.put("MILLISECONDS", TimeUnit.MILLISECONDS);

      TIME_UNITS.put("S", TimeUnit.SECONDS);
      TIME_UNITS.put("SEC", TimeUnit.SECONDS);
      TIME_UNITS.put("SECS", TimeUnit.SECONDS);
      TIME_UNITS.put("SECOND", TimeUnit.SECONDS);
      TIME_UNITS.put("SECONDS", TimeUnit.SECONDS);

      TIME_UNITS.put("M", TimeUnit.MINUTES);
      TIME_UNITS.put("MIN", TimeUnit.MINUTES);
      TIME_UNITS.put("MINS", TimeUnit.MINUTES);
      TIME_UNITS.put("MINUTE", TimeUnit.MINUTES);
      TIME_UNITS.put("MINUTES", TimeUnit.MINUTES);

      TIME_UNITS.put("H", TimeUnit.HOURS);
      TIME_UNITS.put("HR", TimeUnit.HOURS);
      TIME_UNITS.put("HRS", TimeUnit.HOURS);
      TIME_UNITS.put("HOUR", TimeUnit.HOURS);
      TIME_UNITS.put("HOURS", TimeUnit.HOURS);

      TIME_UNITS.put("D", TimeUnit.DAYS);
      TIME_UNITS.put("DAY", TimeUnit.DAYS);
      TIME_UNITS.put("DAYS", TimeUnit.DAYS);
   }

   private Units()
   {}

   public static TimeUnit time(final String time)
   {
      checkNotNull(time, "time must not be null");
      final TimeUnit unit = TIME_UNITS.get(time.toUpperCase());
      checkArgument(unit != null, "Could not parse time [%s]", time);
      return unit;
   }
}
