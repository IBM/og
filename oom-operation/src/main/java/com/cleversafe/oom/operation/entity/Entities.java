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
// Date: Feb 12, 2014
// ---------------------

package com.cleversafe.oom.operation.entity;

import java.io.File;
import java.io.InputStream;
import java.util.Random;

import com.cleversafe.oom.util.FixedBufferInputStream;

public class Entities
{
   private static final int BUF_SIZE = 1024;
   private static final Entity EMPTY = new Entity()
   {
      @Override
      public InputStream asInputStream()
      {
         return null;
      }

      @Override
      public boolean isFile()
      {
         return false;
      }

      @Override
      public File getFile()
      {
         return null;
      }

      @Override
      public long getSize()
      {
         return 0;
      }

   };

   private Entities()
   {}

   public static Entity random(final long size)
   {
      final byte[] buf = new byte[Entities.BUF_SIZE];
      final Random random = new Random(System.currentTimeMillis());
      random.nextBytes(buf);
      return new FixedBufferInputStreamEntity(new FixedBufferInputStream(buf, size));
   }

   public static Entity empty()
   {
      return Entities.EMPTY;
   }
}
