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
// Date: Feb 6, 2013
// ---------------------

package com.cleversafe.oom.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An <code>InputStream</code> implementation that tracks time to first byte.
 */
public class MonitoringInputStream extends FilterInputStream
{
   private boolean firstRead;
   private long ttfb;

   /**
    * Constructs a <code>MonitoringInputStream</code> instance using the provided stream
    * 
    * @param in
    *           the stream to wrap
    */
   public MonitoringInputStream(final InputStream in)
   {
      super(in);
      this.firstRead = true;
   }

   @Override
   public int read() throws IOException
   {
      if (this.firstRead)
      {
         final int size = firstRead();
         this.firstRead = false;
         return size;
      }
      return super.read();
   }

   @Override
   public int read(final byte[] b) throws IOException
   {
      return read(b, 0, b.length);
   }

   @Override
   public int read(final byte[] b, final int off, final int len) throws IOException
   {
      if (this.firstRead)
      {
         final int size = firstRead(b, off, len);
         this.firstRead = false;
         return size;
      }
      return super.read(b, off, len);

   }

   private int firstRead() throws IOException
   {
      final long beginTTFB = System.nanoTime();
      final int size = super.read();
      this.ttfb = System.nanoTime() - beginTTFB;
      return size;
   }

   private int firstRead(final byte[] b, final int off, final int len) throws IOException
   {
      final long beginTTFB = System.nanoTime();
      final int size = super.read(b, off, len);
      this.ttfb = System.nanoTime() - beginTTFB;
      return size;
   }

   /**
    * @return ttfb, in nanoseconds
    */
   public long getTTFB()
   {
      return this.ttfb;
   }
}
