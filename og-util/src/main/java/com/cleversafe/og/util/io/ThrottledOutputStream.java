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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.google.common.util.concurrent.RateLimiter;

public class ThrottledOutputStream extends FilterOutputStream
{
   private final RateLimiter rateLimiter;

   public ThrottledOutputStream(final OutputStream out, final long bytesPerSecond)
   {
      super(checkNotNull(out));
      checkArgument(bytesPerSecond > 0, "bytesPerSecond must be > 0 [%s]", bytesPerSecond);
      this.rateLimiter = RateLimiter.create(bytesPerSecond);
   }

   @Override
   public void write(final int b) throws IOException
   {
      super.write(b);
      this.rateLimiter.acquire();
   }

   @Override
   public void write(final byte[] b) throws IOException
   {
      this.write(b, 0, b.length);
   }

   @Override
   public void write(final byte[] b, final int off, final int len) throws IOException
   {
      // out.write rather than super.write, FilterOutputStream.write calls write(int b) in loop
      this.out.write(b, off, len);
      this.rateLimiter.acquire(len);
   }
}
