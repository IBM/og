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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import com.cleversafe.og.operation.Entity;

public class Streams
{
   // TODO performance of single random instance used by multiple threads? Need to quantify
   private static final Random RANDOM = new Random();
   private static final int BUF_SIZE = 1024;
   private static final byte[] ZERO_BUF = new byte[BUF_SIZE];
   private static final SizedInputStream NONE_INPUTSTREAM = new SizedInputStream()
   {
      @Override
      public int read()
      {
         return -1;
      }

      @Override
      public long getSize()
      {
         return 0;
      }
   };

   private Streams()
   {}

   public static SizedInputStream create(final Entity entity)
   {
      checkNotNull(entity);
      switch (entity.getType())
      {
         case NONE :
            return NONE_INPUTSTREAM;
         case ZEROES :
            return new FixedBufferInputStream(ZERO_BUF, entity.getSize());
         default :
            return new FixedBufferInputStream(createRandomBuffer(), entity.getSize());
      }
   }

   private static byte[] createRandomBuffer()
   {
      final byte[] buf = new byte[BUF_SIZE];
      RANDOM.nextBytes(buf);
      return buf;
   }

   public static InputStream throttle(final InputStream in, final long bytesPerSecond)
   {
      return new ThrottledInputStream(in, bytesPerSecond);
   }

   public static OutputStream throttle(final OutputStream out, final long bytesPerSecond)
   {
      return new ThrottledOutputStream(out, bytesPerSecond);
   }
}
