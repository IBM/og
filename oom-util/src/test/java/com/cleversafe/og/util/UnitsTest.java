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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.cleversafe.og.util.SizeUnit;
import com.cleversafe.og.util.Units;

public class UnitsTest
{
   @Test(expected = NullPointerException.class)
   public void testNullTime()
   {
      Units.time(null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testEmptyTimeString()
   {
      Units.time("");
   }

   @Test
   public void testCaseInsensitiveTime()
   {
      final TimeUnit t1 = Units.time("seconds");
      final TimeUnit t2 = Units.time("sECONds");
      Assert.assertEquals(t1, t2);
   }

   @Test
   public void testNanos()
   {
      final List<String> input = new ArrayList<String>();
      input.add("ns");
      input.add("nano");
      input.add("nanos");
      input.add("nanosec");
      input.add("nanosecs");
      input.add("nanosecond");
      input.add("nanoseconds");
      testTimeUnit(input, TimeUnit.NANOSECONDS);
   }

   @Test
   public void testMicros()
   {
      final List<String> input = new ArrayList<String>();
      input.add("micro");
      input.add("micros");
      input.add("microsec");
      input.add("microsecs");
      input.add("microsecond");
      input.add("microseconds");
      testTimeUnit(input, TimeUnit.MICROSECONDS);
   }

   @Test
   public void testMillis()
   {
      final List<String> input = new ArrayList<String>();
      input.add("ms");
      input.add("milli");
      input.add("millis");
      input.add("millisec");
      input.add("millisecs");
      input.add("millisecond");
      input.add("milliseconds");
      testTimeUnit(input, TimeUnit.MILLISECONDS);
   }

   @Test
   public void testSecs()
   {
      final List<String> input = new ArrayList<String>();
      input.add("s");
      input.add("sec");
      input.add("secs");
      input.add("second");
      input.add("seconds");
      testTimeUnit(input, TimeUnit.SECONDS);
   }

   @Test
   public void testMins()
   {
      final List<String> input = new ArrayList<String>();
      input.add("m");
      input.add("min");
      input.add("mins");
      input.add("minute");
      input.add("minutes");
      testTimeUnit(input, TimeUnit.MINUTES);
   }

   @Test
   public void testHrs()
   {
      final List<String> input = new ArrayList<String>();
      input.add("h");
      input.add("hr");
      input.add("hrs");
      input.add("hour");
      input.add("hours");
      testTimeUnit(input, TimeUnit.HOURS);
   }

   @Test
   public void testDays()
   {
      final List<String> input = new ArrayList<String>();
      input.add("d");
      input.add("day");
      input.add("days");
      testTimeUnit(input, TimeUnit.DAYS);
   }

   private void testTimeUnit(final List<String> input, final TimeUnit unit)
   {
      for (final String i : input)
      {
         Assert.assertEquals(unit, Units.time(i));
      }
   }

   @Test
   public void testBadTimeInput()
   {
      final List<String> input = new ArrayList<String>();
      input.add("foo");
      input.add("_");
      input.add("nano_seconds");

      for (final String i : input)
      {
         try
         {
            Units.time(i);
            // should never get here
            Assert.assertTrue(false);
         }
         catch (final IllegalArgumentException e)
         {
         }
      }
   }

   @Test(expected = NullPointerException.class)
   public void testNullSize()
   {
      Units.size(null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testEmptySizeString()
   {
      Units.size("");
   }

   @Test
   public void testCaseInsensitiveSize()
   {
      final SizeUnit s1 = Units.size("bytes");
      final SizeUnit s2 = Units.size("bYTEs");
      Assert.assertEquals(s1, s2);
   }

   @Test
   public void testBytes()
   {
      final List<String> input = new ArrayList<String>();
      input.add("b");
      input.add("byte");
      input.add("bytes");
      testSizeUnit(input, SizeUnit.BYTES);
   }

   @Test
   public void testKilobytes()
   {
      final List<String> input = new ArrayList<String>();
      input.add("kb");
      input.add("kilobyte");
      input.add("kilobytes");
      testSizeUnit(input, SizeUnit.KILOBYTES);
   }

   @Test
   public void testKibibytes()
   {
      final List<String> input = new ArrayList<String>();
      input.add("kib");
      input.add("kibibyte");
      input.add("kibibytes");
      testSizeUnit(input, SizeUnit.KIBIBYTES);
   }

   @Test
   public void testMegabytes()
   {
      final List<String> input = new ArrayList<String>();
      input.add("mb");
      input.add("megabyte");
      input.add("megabytes");
      testSizeUnit(input, SizeUnit.MEGABYTES);
   }

   @Test
   public void testMebibytes()
   {
      final List<String> input = new ArrayList<String>();
      input.add("mib");
      input.add("mebibyte");
      input.add("mebibytes");
      testSizeUnit(input, SizeUnit.MEBIBYTES);
   }

   @Test
   public void testGigabytes()
   {
      final List<String> input = new ArrayList<String>();
      input.add("gb");
      input.add("gigabyte");
      input.add("gigabytes");
      testSizeUnit(input, SizeUnit.GIGABYTES);
   }

   @Test
   public void testGibibytes()
   {
      final List<String> input = new ArrayList<String>();
      input.add("gib");
      input.add("gibibyte");
      input.add("gibibytes");
      testSizeUnit(input, SizeUnit.GIBIBYTES);
   }

   @Test
   public void testTerabytes()
   {
      final List<String> input = new ArrayList<String>();
      input.add("tb");
      input.add("terabyte");
      input.add("terabytes");
      testSizeUnit(input, SizeUnit.TERABYTES);
   }

   @Test
   public void testTebibytes()
   {
      final List<String> input = new ArrayList<String>();
      input.add("tib");
      input.add("tebibyte");
      input.add("tebibytes");
      testSizeUnit(input, SizeUnit.TEBIBYTES);
   }

   @Test
   public void testPetabytes()
   {
      final List<String> input = new ArrayList<String>();
      input.add("pb");
      input.add("petabyte");
      input.add("petabytes");
      testSizeUnit(input, SizeUnit.PETABYTES);
   }

   @Test
   public void testPebibytes()
   {
      final List<String> input = new ArrayList<String>();
      input.add("pib");
      input.add("pebibyte");
      input.add("pebibytes");
      testSizeUnit(input, SizeUnit.PEBIBYTES);
   }

   private void testSizeUnit(final List<String> input, final SizeUnit unit)
   {
      for (final String i : input)
      {
         Assert.assertEquals(unit, Units.size(i));
      }
   }

   @Test
   public void testBadSizeInput()
   {
      final List<String> input = new ArrayList<String>();
      input.add("cleverbyte");
      input.add("_");
      input.add("mega_byte");

      for (final String i : input)
      {
         try
         {
            Units.size(i);
            // should never get here
            Assert.assertTrue(false);
         }
         catch (final IllegalArgumentException e)
         {
         }
      }
   }
}
