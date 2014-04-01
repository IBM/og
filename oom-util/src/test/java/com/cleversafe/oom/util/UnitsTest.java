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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class UnitsTest
{
   @Test(expected = NullPointerException.class)
   public void testNullTime()
   {
      Units.time(null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testEmptyString()
   {
      Units.time("");
   }

   @Test
   public void testCaseInsensitive()
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
      testUnit(input, TimeUnit.NANOSECONDS);
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
      testUnit(input, TimeUnit.MICROSECONDS);
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
      testUnit(input, TimeUnit.MILLISECONDS);
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
      testUnit(input, TimeUnit.SECONDS);
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
      testUnit(input, TimeUnit.MINUTES);
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
      testUnit(input, TimeUnit.HOURS);
   }

   @Test
   public void testDays()
   {
      final List<String> input = new ArrayList<String>();
      input.add("d");
      input.add("day");
      input.add("days");
      testUnit(input, TimeUnit.DAYS);
   }

   private void testUnit(final List<String> input, final TimeUnit unit)
   {
      for (final String i : input)
      {
         Assert.assertEquals(unit, Units.time(i));
      }
   }

   @Test
   public void testBadInput()
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
}
