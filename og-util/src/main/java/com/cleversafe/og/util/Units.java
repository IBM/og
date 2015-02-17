/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;

/**
 * A utility class for working with units
 */
public class Units {
  private static final Map<String, TimeUnit> TIME_UNITS;
  private static final Map<String, SizeUnit> SIZE_UNITS;

  static {
    TIME_UNITS =
        ImmutableMap.<String, TimeUnit>builder().put("NS", TimeUnit.NANOSECONDS)
            .put("NANO", TimeUnit.NANOSECONDS).put("NANOS", TimeUnit.NANOSECONDS)
            .put("NANOSEC", TimeUnit.NANOSECONDS).put("NANOSECS", TimeUnit.NANOSECONDS)
            .put("NANOSECOND", TimeUnit.NANOSECONDS).put("NANOSECONDS", TimeUnit.NANOSECONDS)

            .put("MICRO", TimeUnit.MICROSECONDS).put("MICROS", TimeUnit.MICROSECONDS)
            .put("MICROSEC", TimeUnit.MICROSECONDS).put("MICROSECS", TimeUnit.MICROSECONDS)
            .put("MICROSECOND", TimeUnit.MICROSECONDS).put("MICROSECONDS", TimeUnit.MICROSECONDS)

            .put("MS", TimeUnit.MILLISECONDS).put("MILLI", TimeUnit.MILLISECONDS)
            .put("MILLIS", TimeUnit.MILLISECONDS).put("MILLISEC", TimeUnit.MILLISECONDS)
            .put("MILLISECS", TimeUnit.MILLISECONDS).put("MILLISECOND", TimeUnit.MILLISECONDS)
            .put("MILLISECONDS", TimeUnit.MILLISECONDS)

            .put("S", TimeUnit.SECONDS).put("SEC", TimeUnit.SECONDS).put("SECS", TimeUnit.SECONDS)
            .put("SECOND", TimeUnit.SECONDS).put("SECONDS", TimeUnit.SECONDS)

            .put("M", TimeUnit.MINUTES).put("MIN", TimeUnit.MINUTES).put("MINS", TimeUnit.MINUTES)
            .put("MINUTE", TimeUnit.MINUTES).put("MINUTES", TimeUnit.MINUTES)

            .put("H", TimeUnit.HOURS).put("HR", TimeUnit.HOURS).put("HRS", TimeUnit.HOURS)
            .put("HOUR", TimeUnit.HOURS).put("HOURS", TimeUnit.HOURS)

            .put("D", TimeUnit.DAYS).put("DAY", TimeUnit.DAYS).put("DAYS", TimeUnit.DAYS).build();

    SIZE_UNITS =
        ImmutableMap.<String, SizeUnit>builder().put("B", SizeUnit.BYTES)
            .put("BYTE", SizeUnit.BYTES).put("BYTES", SizeUnit.BYTES)

            .put("KB", SizeUnit.KILOBYTES).put("KILOBYTE", SizeUnit.KILOBYTES)
            .put("KILOBYTES", SizeUnit.KILOBYTES)

            .put("KIB", SizeUnit.KIBIBYTES).put("KIBIBYTE", SizeUnit.KIBIBYTES)
            .put("KIBIBYTES", SizeUnit.KIBIBYTES)

            .put("MB", SizeUnit.MEGABYTES).put("MEGABYTE", SizeUnit.MEGABYTES)
            .put("MEGABYTES", SizeUnit.MEGABYTES)

            .put("MIB", SizeUnit.MEBIBYTES).put("MEBIBYTE", SizeUnit.MEBIBYTES)
            .put("MEBIBYTES", SizeUnit.MEBIBYTES)

            .put("GB", SizeUnit.GIGABYTES).put("GIGABYTE", SizeUnit.GIGABYTES)
            .put("GIGABYTES", SizeUnit.GIGABYTES)

            .put("GIB", SizeUnit.GIBIBYTES).put("GIBIBYTE", SizeUnit.GIBIBYTES)
            .put("GIBIBYTES", SizeUnit.GIBIBYTES)

            .put("TB", SizeUnit.TERABYTES).put("TERABYTE", SizeUnit.TERABYTES)
            .put("TERABYTES", SizeUnit.TERABYTES)

            .put("TIB", SizeUnit.TEBIBYTES).put("TEBIBYTE", SizeUnit.TEBIBYTES)
            .put("TEBIBYTES", SizeUnit.TEBIBYTES)

            .put("PB", SizeUnit.PETABYTES).put("PETABYTE", SizeUnit.PETABYTES)
            .put("PETABYTES", SizeUnit.PETABYTES)

            .put("PIB", SizeUnit.PEBIBYTES).put("PEBIBYTE", SizeUnit.PEBIBYTES)
            .put("PEBIBYTES", SizeUnit.PEBIBYTES).build();
  }

  private Units() {}

  /**
   * Returns the associated TimeUnit instance for the provided string
   * 
   * @param time a time unit string
   * @return the associated TimeUnit instance for the provided string
   * @throws IllegalArgumentException if no TimeUnit can be determined from the provided string
   */
  public static TimeUnit time(final String time) {
    checkNotNull(time);
    final TimeUnit unit = TIME_UNITS.get(time.toUpperCase());
    checkArgument(unit != null, "Could not parse time [%s]", time);
    return unit;
  }

  /**
   * Returns the associated SizeUnit instance for the provided string
   * 
   * @param size a size unit string
   * @return the associated SizeUnit instance for the provided string
   * @throws IllegalArgumentException if no SizeUnit can be determined from the provided string
   */
  public static SizeUnit size(final String size) {
    checkNotNull(size);
    final SizeUnit unit = SIZE_UNITS.get(size.toUpperCase());
    checkArgument(unit != null, "Could not parse size [%s]", size);
    return unit;
  }
}
