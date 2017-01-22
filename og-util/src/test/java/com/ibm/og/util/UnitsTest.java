/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class UnitsTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @DataProvider
  public static Object[][] provideInvalidTime() {
    return new Object[][] {{null, NullPointerException.class}, {"", IllegalArgumentException.class},
        {"nano_seconds", IllegalArgumentException.class}};
  }

  @Test
  @UseDataProvider("provideInvalidTime")
  public void invalidTime(final String invalidTime, final Class<Exception> expectedException) {
    this.thrown.expect(expectedException);
    Units.time(invalidTime);
  }

  @DataProvider
  public static Object[][] provideTime() {
    return new Object[][] {
        {ImmutableList.of("ns", "nano", "nanos", "nanosec", "nanosecs", "nanosecond",
            "nanoseconds"), TimeUnit.NANOSECONDS},
        {ImmutableList.of("micro", "micros", "microsec", "microsecs", "microsecond",
            "microseconds"), TimeUnit.MICROSECONDS},
        {ImmutableList.of("ms", "milli", "millis", "millisec", "millisecs", "millisecond",
            "milliseconds"), TimeUnit.MILLISECONDS},
        {ImmutableList.of("s", "sec", "secs", "second", "seconds"), TimeUnit.SECONDS},
        {ImmutableList.of("m", "min", "mins", "minute", "minutes"), TimeUnit.MINUTES},
        {ImmutableList.of("h", "hr", "hrs", "hour", "hours"), TimeUnit.HOURS},
        {ImmutableList.of("d", "day", "days"), TimeUnit.DAYS}};
  }

  @Test
  @UseDataProvider("provideTime")
  public void time(final List<String> units, final TimeUnit unit) {
    for (final String u : units) {
      assertThat(Units.time(u), is(unit));
    }
  }

  @Test
  public void caseInsensitiveTime() {
    assertThat(Units.time("seconds"), is(Units.time("sECONds")));
  }

  @DataProvider
  public static Object[][] provideInvalidSize() {
    return new Object[][] {{null, NullPointerException.class}, {"", IllegalArgumentException.class},
        {"mega_byte", IllegalArgumentException.class}};
  }

  @Test
  @UseDataProvider("provideInvalidSize")
  public void invalidSize(final String invalidSize, final Class<Exception> expectedException) {
    this.thrown.expect(expectedException);
    Units.size(invalidSize);
  }

  @DataProvider
  public static Object[][] provideSize() {
    return new Object[][] {{ImmutableList.of("b", "byte", "bytes"), SizeUnit.BYTES},
        {ImmutableList.of("kb", "kilobyte", "kilobytes"), SizeUnit.KILOBYTES},
        {ImmutableList.of("kib", "kibibyte", "kibibytes"), SizeUnit.KIBIBYTES},
        {ImmutableList.of("mb", "megabyte", "megabytes"), SizeUnit.MEGABYTES},
        {ImmutableList.of("mib", "mebibyte", "mebibytes"), SizeUnit.MEBIBYTES},
        {ImmutableList.of("gb", "gigabyte", "gigabytes"), SizeUnit.GIGABYTES},
        {ImmutableList.of("gib", "gibibyte", "gibibytes"), SizeUnit.GIBIBYTES},
        {ImmutableList.of("tb", "terabyte", "terabytes"), SizeUnit.TERABYTES},
        {ImmutableList.of("tib", "tebibyte", "tebibytes"), SizeUnit.TEBIBYTES},
        {ImmutableList.of("pb", "petabyte", "petabytes"), SizeUnit.PETABYTES},
        {ImmutableList.of("pib", "pebibyte", "pebibytes"), SizeUnit.PEBIBYTES},};
  }

  @Test
  @UseDataProvider("provideSize")
  public void size(final List<String> units, final SizeUnit unit) {
    for (final String u : units) {
      assertThat(Units.size(u), is(unit));
    }
  }

  @Test
  public void caseInsensitiveSize() {
    assertThat(Units.size("bytes"), is(Units.size("bYTEs")));
  }
}
