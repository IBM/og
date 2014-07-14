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
// Date: Jun 28, 2014
// ---------------------

package com.cleversafe.og.util.io;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("resource")
public class FixedBufferInputStreamTest
{
   private long size;
   private byte[] buf;
   private FixedBufferInputStream stream;

   @Before
   public void before()
   {
      this.size = 1024;
      this.buf = new byte[]{1, 2, 3, 4, 5};
      this.stream = new FixedBufferInputStream(this.buf, this.size);
   }

   @Test(expected = NullPointerException.class)
   public void testNullBuf()
   {
      new FixedBufferInputStream(null, 1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testZeroLengthBuf()
   {
      new FixedBufferInputStream(new byte[0], 1);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeSize()
   {
      new FixedBufferInputStream(new byte[1], -1);
   }

   @Test
   public void testZeroSize()
   {
      new FixedBufferInputStream(new byte[1], 0);
   }

   @Test
   public void testPositiveSize()
   {
      new FixedBufferInputStream(new byte[1], 1);
   }

   @Test
   public void testGetSize()
   {
      Assert.assertEquals(this.size, this.stream.getSize());
   }

   @Test
   public void testReadOneByteAtATime()
   {
      int counter = 0;
      int val;
      while ((val = this.stream.read()) > -1)
      {
         Assert.assertEquals(this.buf[counter % this.buf.length], val);
         Assert.assertEquals(this.size - counter - 1, this.stream.available());
         counter++;
      }
   }

   @Test(expected = NullPointerException.class)
   public void testReadNullBuf()
   {
      new FixedBufferInputStream(new byte[1], 1).read(null);
   }

   @Test
   public void testReadZeroBuf()
   {
      final int i = this.stream.read(new byte[0]);
      Assert.assertEquals(0, i);
   }

   @Test
   public void testSmallBuf()
   {
      final byte[] small = new byte[4];
      this.stream.read(small);
      for (int j = 0; j < small.length; j++)
      {
         Assert.assertEquals(this.buf[j], small[j]);
      }
   }

   @Test
   public void testLargeBuf()
   {
      final byte[] large = new byte[6];
      this.stream.read(large);
      for (int j = 0; j < large.length; j++)
      {
         Assert.assertEquals(this.buf[j % this.buf.length], large[j]);
      }
   }

   @Test(expected = NullPointerException.class)
   public void testIdxReadNullBuf()
   {
      this.stream.read(null, 0, 1);
   }

   @Test
   public void testIdxReadZeroBuf()
   {
      final int i = this.stream.read(new byte[0], 0, 0);
      Assert.assertEquals(0, i);
   }

   @Test(expected = IndexOutOfBoundsException.class)
   public void testIdxReadNegativeOffset()
   {
      this.stream.read(new byte[1], -1, 1);
   }

   @Test(expected = IndexOutOfBoundsException.class)
   public void testIdxReadLargeOffset()
   {
      final byte[] buf = new byte[1];
      this.stream.read(buf, buf.length, 1);
   }

   @Test(expected = IndexOutOfBoundsException.class)
   public void testIdxReadNegativeLength()
   {
      this.stream.read(new byte[0], 0, -1);
   }

   @Test
   public void testIdxReadZeroLength()
   {
      final int i = this.stream.read(new byte[1], 0, 0);
      Assert.assertEquals(0, i);
   }

   @Test
   public void testIdxReadPositiveLength()
   {
      final int i = this.stream.read(new byte[1], 0, 1);
      Assert.assertEquals(1, i);
   }

   @Test
   public void testExhaustedRead()
   {
      while (this.stream.available() > 0)
      {
         this.stream.read();
      }
      final int i = this.stream.read(new byte[1]);
      Assert.assertEquals(-1, i);
   }

   @Test
   public void testReset()
   {
      final int size = 10;
      final FixedBufferInputStream i = new FixedBufferInputStream(new byte[1], size);
      Assert.assertEquals(size, i.available());
      i.read();
      Assert.assertEquals(size - 1, i.available());
      i.reset();
      Assert.assertEquals(size, i.available());
   }
}
