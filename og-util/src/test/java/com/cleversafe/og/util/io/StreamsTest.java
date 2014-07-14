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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.cleversafe.og.operation.Entity;
import com.cleversafe.og.util.Entities;

public class StreamsTest
{
   @Test(expected = NullPointerException.class)
   public void testNullEntity()
   {
      Streams.create(null);
   }

   @Test
   public void testNoneCreate() throws IOException
   {
      final Entity e = Entities.none();
      final SizedInputStream i = Streams.create(e);
      Assert.assertEquals(-1, i.read());
      Assert.assertEquals(0, i.getSize());
   }

   @Test
   public void testRandomCreateInputStream() throws IOException
   {
      final Entity e = Entities.random(1024);
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
      final Entity e = Entities.zeroes(1024);
      final SizedInputStream stream = Streams.create(e);
      Assert.assertEquals(1024, stream.getSize());
      final byte[] buf = new byte[1024];
      Assert.assertEquals(1024, stream.read(buf));
      for (int i = 0; i < buf.length; i++)
      {
         Assert.assertEquals(0, buf[i]);
      }
   }
}
