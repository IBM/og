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
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Data;

@SuppressWarnings("resource")
public class ThrottledInputStreamTest
{
   private InputStream in;
   private Body body;

   @Before
   public void before()
   {
      this.in = mock(InputStream.class);
      this.body = mock(Body.class);
      when(this.body.getData()).thenReturn(Data.ZEROES);
      when(this.body.getSize()).thenReturn(10000L);
   }

   @Test(expected = NullPointerException.class)
   public void testNullInputStream()
   {
      new ThrottledInputStream(null, 10);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeBytesPerSecond()
   {
      new ThrottledInputStream(this.in, -1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testZeroBytesPerSecond()
   {
      new ThrottledInputStream(this.in, 0);
   }

   @Test
   public void testPositiveBytesPerSecond()
   {
      new ThrottledInputStream(this.in, 1);
   }

   @Test
   public void testReadByte() throws IOException
   {
      final InputStream in = Streams.create(this.body);
      final InputStream throttled = new ThrottledInputStream(in, 1000);
      final long timestampStart = System.nanoTime();
      for (int i = 0; i < 100; i++)
      {
         throttled.read();
      }
      final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart);
      final long delta = Math.abs(duration - 100);
      // within 10% of expected duration (100 milliseconds)
      Assert.assertTrue(delta < 10);
   }

   @Test
   public void testReadByteArray() throws IOException
   {
      final InputStream in = Streams.create(this.body);
      final InputStream throttled = new ThrottledInputStream(in, 1000);
      final byte[] buf = new byte[100];
      final long timestampStart = System.nanoTime();
      throttled.read(buf);
      throttled.read(buf);
      final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestampStart);
      final long delta = Math.abs(duration - 100);
      // within 10% of expected duration (100 milliseconds)
      Assert.assertTrue(delta < 10);
   }
}
