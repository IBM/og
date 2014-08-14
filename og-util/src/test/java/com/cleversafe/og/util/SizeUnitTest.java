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

package com.cleversafe.og.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collection;

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
   public void bytes()
   {
      assertThat(SizeUnit.BYTES.toBytes(this.size), is(this.size));
   }

   @Test
   public void kilobytes()
   {
      assertThat(SizeUnit.KILOBYTES.toBytes(this.size), is(si(this.size, 1)));
   }

   @Test
   public void kibibytes()
   {
      assertThat(SizeUnit.KIBIBYTES.toBytes(this.size), is(iec(this.size, 1)));
   }

   @Test
   public void megabytes()
   {
      assertThat(SizeUnit.MEGABYTES.toBytes(this.size), is(si(this.size, 2)));
   }

   @Test
   public void mebibytes()
   {
      assertThat(SizeUnit.MEBIBYTES.toBytes(this.size), is(iec(this.size, 2)));
   }

   @Test
   public void gigabytes()
   {
      assertThat(SizeUnit.GIGABYTES.toBytes(this.size), is(si(this.size, 3)));
   }

   @Test
   public void gibibytes()
   {
      assertThat(SizeUnit.GIBIBYTES.toBytes(this.size), is(iec(this.size, 3)));
   }

   @Test
   public void terabytes()
   {
      assertThat(SizeUnit.TERABYTES.toBytes(this.size), is(si(this.size, 4)));
   }

   @Test
   public void tebibytes()
   {
      assertThat(SizeUnit.TEBIBYTES.toBytes(this.size), is(iec(this.size, 4)));
   }

   @Test
   public void petabytes()
   {
      assertThat(SizeUnit.PETABYTES.toBytes(this.size), is(si(this.size, 5)));
   }

   @Test
   public void pebibytes()
   {
      assertThat(SizeUnit.PEBIBYTES.toBytes(this.size), is(iec(this.size, 5)));
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
