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
// Date: Jul 13, 2014
// ---------------------

package com.cleversafe.og.util.io;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.util.Bodies;

public class StreamsTest
{
   private InputStream in;
   private OutputStream out;

   @Before
   public void before()
   {
      this.in = mock(InputStream.class);
      this.out = mock(OutputStream.class);
   }

   @Test(expected = NullPointerException.class)
   public void testNullBody()
   {
      Streams.create(null);
   }

   @Test
   public void testNoneCreate() throws IOException
   {
      final Body e = Bodies.none();
      final SizedInputStream i = Streams.create(e);
      Assert.assertEquals(-1, i.read());
      Assert.assertEquals(0, i.getSize());
   }

   @Test
   public void testRandomCreateInputStream() throws IOException
   {
      final Body e = Bodies.random(1024);
      final SizedInputStream stream = Streams.create(e);
      Assert.assertEquals(1024, stream.getSize());
      final byte[] buf = new byte[1024];
      Assert.assertEquals(1024, stream.read(buf));
      boolean nonZero = false;
      for (int i = 0; i < buf.length; i++)
      {
         if (buf[i] != 0)
            nonZero = true;
      }
      Assert.assertTrue(nonZero);
   }

   @Test
   public void testZeroesCreateInputStream() throws IOException
   {
      final Body e = Bodies.zeroes(1024);
      final SizedInputStream stream = Streams.create(e);
      Assert.assertEquals(1024, stream.getSize());
      final byte[] buf = new byte[1024];
      Assert.assertEquals(1024, stream.read(buf));
      for (int i = 0; i < buf.length; i++)
      {
         Assert.assertEquals(0, buf[i]);
      }
   }

   @Test(expected = NullPointerException.class)
   public void testThrottleInputStreamNullStream()
   {
      Streams.throttle((InputStream) null, 10);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testThrottleInputStreamNegativeBytesPerSecond()
   {
      Streams.throttle(this.in, -1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testThrottleInputStreamZeroBytesPerSecond()
   {
      Streams.throttle(this.in, 0);
   }

   @Test
   public void testThrottleInputStreamPositiveBytesPerSecond()
   {
      Streams.throttle(this.in, 1);
   }

   @Test(expected = NullPointerException.class)
   public void testThrottleOutputStreamNullStream()
   {
      Streams.throttle((OutputStream) null, 10);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testThrottleOutputStreamNegativeBytesPerSecond()
   {
      Streams.throttle(this.out, -1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testThrottleOutputStreamZeroBytesPerSecond()
   {
      Streams.throttle(this.out, 0);
   }

   @Test
   public void testThrottleOutputStreamPositiveBytesPerSecond()
   {
      Streams.throttle(this.out, 1);
   }
}
