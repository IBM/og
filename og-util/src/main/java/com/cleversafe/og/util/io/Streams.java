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

/**
 * A utility class for creating input and output streams
 * 
 * @since 1.0
 */
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

   /**
    * Creates an input stream from the provided entity description. The size of this stream and its
    * data are determined by the provided entity's size and type, respectively.
    * 
    * @param entity
    *           the description of an entity
    * @return an input stream instance
    */
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

   /**
    * Creates an input stream which is throttled with a maximum throughput
    * 
    * @param in
    *           the backing input stream to throttle
    * @param bytesPerSecond
    *           the maximum throughput this input stream is allowed to read at
    * @return an instance of an input stream, throttled with a maximum rate of
    *         {@code bytesPerSecond}
    * @throws IllegalArgumentException
    *            if bytesPerSecond is negative or zero
    */
   public static InputStream throttle(final InputStream in, final long bytesPerSecond)
   {
      return new ThrottledInputStream(in, bytesPerSecond);
   }

   /**
    * Creates an output stream which is throttled with a maximum throughput
    * 
    * @param out
    *           the backing output stream to throttle
    * @param bytesPerSecond
    *           the maximum throughput this input stream is allowed to write at
    * @return an instance of an output stream, throttled with a maximum rate of
    *         {@code bytesPerSecond}
    * @throws IllegalArgumentException
    *            if bytesPerSecond is negative or zero
    */
   public static OutputStream throttle(final OutputStream out, final long bytesPerSecond)
   {
      return new ThrottledOutputStream(out, bytesPerSecond);
   }
}
