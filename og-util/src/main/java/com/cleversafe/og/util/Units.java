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

package com.cleversafe.og.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;

/**
 * A utility class for working with units
 */
public class Units
{
   private static final Map<String, TimeUnit> TIME_UNITS;
   private static final Map<String, SizeUnit> SIZE_UNITS;

   static
   {
      TIME_UNITS = Maps.newHashMap();
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

      SIZE_UNITS = Maps.newHashMap();
      SIZE_UNITS.put("B", SizeUnit.BYTES);
      SIZE_UNITS.put("BYTE", SizeUnit.BYTES);
      SIZE_UNITS.put("BYTES", SizeUnit.BYTES);

      SIZE_UNITS.put("KB", SizeUnit.KILOBYTES);
      SIZE_UNITS.put("KILOBYTE", SizeUnit.KILOBYTES);
      SIZE_UNITS.put("KILOBYTES", SizeUnit.KILOBYTES);

      SIZE_UNITS.put("KIB", SizeUnit.KIBIBYTES);
      SIZE_UNITS.put("KIBIBYTE", SizeUnit.KIBIBYTES);
      SIZE_UNITS.put("KIBIBYTES", SizeUnit.KIBIBYTES);

      SIZE_UNITS.put("MB", SizeUnit.MEGABYTES);
      SIZE_UNITS.put("MEGABYTE", SizeUnit.MEGABYTES);
      SIZE_UNITS.put("MEGABYTES", SizeUnit.MEGABYTES);

      SIZE_UNITS.put("MIB", SizeUnit.MEBIBYTES);
      SIZE_UNITS.put("MEBIBYTE", SizeUnit.MEBIBYTES);
      SIZE_UNITS.put("MEBIBYTES", SizeUnit.MEBIBYTES);

      SIZE_UNITS.put("GB", SizeUnit.GIGABYTES);
      SIZE_UNITS.put("GIGABYTE", SizeUnit.GIGABYTES);
      SIZE_UNITS.put("GIGABYTES", SizeUnit.GIGABYTES);

      SIZE_UNITS.put("GIB", SizeUnit.GIBIBYTES);
      SIZE_UNITS.put("GIBIBYTE", SizeUnit.GIBIBYTES);
      SIZE_UNITS.put("GIBIBYTES", SizeUnit.GIBIBYTES);

      SIZE_UNITS.put("TB", SizeUnit.TERABYTES);
      SIZE_UNITS.put("TERABYTE", SizeUnit.TERABYTES);
      SIZE_UNITS.put("TERABYTES", SizeUnit.TERABYTES);

      SIZE_UNITS.put("TIB", SizeUnit.TEBIBYTES);
      SIZE_UNITS.put("TEBIBYTE", SizeUnit.TEBIBYTES);
      SIZE_UNITS.put("TEBIBYTES", SizeUnit.TEBIBYTES);

      SIZE_UNITS.put("PB", SizeUnit.PETABYTES);
      SIZE_UNITS.put("PETABYTE", SizeUnit.PETABYTES);
      SIZE_UNITS.put("PETABYTES", SizeUnit.PETABYTES);

      SIZE_UNITS.put("PIB", SizeUnit.PEBIBYTES);
      SIZE_UNITS.put("PEBIBYTE", SizeUnit.PEBIBYTES);
      SIZE_UNITS.put("PEBIBYTES", SizeUnit.PEBIBYTES);
   }

   private Units()
   {}

   /**
    * Returns the associated TimeUnit instance for the provided string
    * 
    * @param time
    *           a time unit string
    * @return the associated TimeUnit instance for the provided string
    * @throws IllegalArgumentException
    *            if no TimeUnit can be determined from the provided string
    */
   public static TimeUnit time(final String time)
   {
      checkNotNull(time);
      final TimeUnit unit = TIME_UNITS.get(time.toUpperCase());
      checkArgument(unit != null, "Could not parse time [%s]", time);
      return unit;
   }

   /**
    * Returns the associated SizeUnit instance for the provided string
    * 
    * @param size
    *           a size unit string
    * @return the associated SizeUnit instance for the provided string
    * @throws IllegalArgumentException
    *            if no SizeUnit can be determined from the provided string
    */
   public static SizeUnit size(final String size)
   {
      checkNotNull(size);
      final SizeUnit unit = SIZE_UNITS.get(size.toUpperCase());
      checkArgument(unit != null, "Could not parse size [%s]", size);
      return unit;
   }
}
