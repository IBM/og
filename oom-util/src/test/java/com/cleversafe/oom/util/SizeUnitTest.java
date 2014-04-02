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
// Date: Apr 1, 2014
// ---------------------

package com.cleversafe.oom.util;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

// TODO test long overflow conditions
@RunWith(value = Parameterized.class)
public class SizeUnitTest
{
   private final long size;

   public SizeUnitTest(final long size)
   {
      this.size = size;
   }

   @Parameters
   public static Collection<Object[]> data()
   {
      return Arrays.asList(new Object[][]{{-1L}, {0L}, {1L}, {1000L}});
   }

   @Test
   public void testBytes()
   {
      Assert.assertEquals(this.size, SizeUnit.BYTES.toBytes(this.size));
   }

   @Test
   public void testKilobytes()
   {
      Assert.assertEquals(si(this.size, 1), SizeUnit.KILOBYTES.toBytes(this.size));
   }

   @Test
   public void testKibibytes()
   {
      Assert.assertEquals(iec(this.size, 1), SizeUnit.KIBIBYTES.toBytes(this.size));
   }

   @Test
   public void testMegabytes()
   {
      Assert.assertEquals(si(this.size, 2), SizeUnit.MEGABYTES.toBytes(this.size));
   }

   @Test
   public void testMebibytes()
   {
      Assert.assertEquals(iec(this.size, 2), SizeUnit.MEBIBYTES.toBytes(this.size));
   }

   @Test
   public void testGigabytes()
   {
      Assert.assertEquals(si(this.size, 3), SizeUnit.GIGABYTES.toBytes(this.size));
   }

   @Test
   public void testGibibytes()
   {
      Assert.assertEquals(iec(this.size, 3), SizeUnit.GIBIBYTES.toBytes(this.size));
   }

   @Test
   public void testTerabytes()
   {
      Assert.assertEquals(si(this.size, 4), SizeUnit.TERABYTES.toBytes(this.size));
   }

   @Test
   public void testTebibytes()
   {
      Assert.assertEquals(iec(this.size, 4), SizeUnit.TEBIBYTES.toBytes(this.size));
   }

   @Test
   public void testPetabytes()
   {
      Assert.assertEquals(si(this.size, 5), SizeUnit.PETABYTES.toBytes(this.size));
   }

   @Test
   public void testPebibytes()
   {
      Assert.assertEquals(iec(this.size, 5), SizeUnit.PEBIBYTES.toBytes(this.size));
   }

   private static long si(final long size, final int exp)
   {
      return size * ((long) Math.pow(1000, exp));
   }

   private static long iec(final long size, final int exp)
   {
      return size * ((long) Math.pow(1024, exp));
   }
}
