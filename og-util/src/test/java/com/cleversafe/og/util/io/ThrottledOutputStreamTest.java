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
// Date: Jul 15, 2014
// ---------------------

package com.cleversafe.og.util.io;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("resource")
public class ThrottledOutputStreamTest
{
   private OutputStream out;

   @Before
   public void before()
   {
      this.out = mock(OutputStream.class);
   }

   @Test(expected = NullPointerException.class)
   public void testNullOutputStream()
   {
      new ThrottledOutputStream(null, 10);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeBytesPerSecond()
   {
      new ThrottledOutputStream(this.out, -1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testZeroBytesPerSecond()
   {
      new ThrottledOutputStream(this.out, 0);
   }

   @Test
   public void testPositiveBytesPerSecond()
   {
      new ThrottledOutputStream(this.out, 1);
   }

   @Test
   public void testWriteByte() throws IOException
   {
      final OutputStream throttled = new ThrottledOutputStream(this.out, 1000);
      final long timestampStart = System.nanoTime();
      for (int i = 0; i < 100; i++)
      {
         throttled.write(1);
      }
      final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart);
      final long delta = Math.abs(duration - 100);
      // within 10% of expected duration (100 milliseconds)
      Assert.assertTrue(delta < 10);
   }

   @Test
   public void testWriteByteArray() throws IOException
   {
      final OutputStream throttled = new ThrottledOutputStream(this.out, 1000);
      final byte[] buf = new byte[100];
      final long timestampStart = System.nanoTime();
      throttled.write(buf);
      throttled.write(buf);
      final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart);
      final long delta = Math.abs(duration - 100);
      // within 10% of expected duration (100 milliseconds)
      Assert.assertTrue(delta < 10);
   }
}
