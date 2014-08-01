//
// Cleversafe open-source code header - Version 1.2 - February 15, 2008
//
// Cleversafe Dispersed Storage(TM) is software for secure, private and
// reliable storage of the world's data using information dispersal.
//
// Copyright (C) 2005-2008 Cleversafe, Inc.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
// USA.
//
// Contact Information: Cleversafe, 224 North Desplaines Street, Suite 500
// Chicago IL 60661
// email licensing@cleversafe.org
//
// END-OF-HEADER
// -----------------------
// @author: mmotwani
//
// Date: Dec 19, 2008
// ---------------------

package com.cleversafe.og.util.io;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An input stream that is backed by a byte array.
 * <P>
 * This class is useful for modeling an arbitrarily long source stream of bytes.
 * 
 * @since 1.0
 */
public class FixedBufferInputStream extends SizedInputStream
{
   private final byte[] buf;
   private final long size;
   private long available;

   /**
    * Constructs an input stream of the provided size, using the provided byte array as its source
    * of data.
    * <p>
    * Note: this class does not perform a defensive copy of the provided byte array, so callers must
    * take care not to modify the byte array after construction of this input stream.
    * 
    * @param buf
    *           the byte array to use as a data source for this input stream
    * @param size
    *           the size of this input stream
    * @throws IllegalArgumentException
    *            if buf length is zero
    * @throws IllegalArgumentException
    *            if size is negative
    */
   public FixedBufferInputStream(final byte[] buf, final long size)
   {
      this.buf = checkNotNull(buf);
      checkArgument(buf.length > 0, "buf length must be > 0 [%s]", buf.length);
      checkArgument(size >= 0, "size must be >= 0, [%s]", size);

      this.size = size;
      this.available = size;
   }

   @Override
   public int read()
   {
      if (this.available == 0)
      {
         return -1;
      }

      final int idx = (int) ((this.size - this.available) % this.buf.length);
      this.available--;
      return this.buf[idx];
   }

   @Override
   public int read(final byte[] b)
   {
      return this.read(b, 0, b.length);
   }

   @Override
   // TODO: implement internal state that tracks our cursor or offset position
   public int read(final byte[] b, final int off, final int len)
   {
      checkNotNull(b);
      if (off < 0 || len < 0 || len > b.length - off)
      {
         throw new IndexOutOfBoundsException();
      }
      else if (len == 0)
      {
         return 0;
      }
      else if (this.available == 0)
      {
         return -1;
      }

      int offset = off;
      for (int i = 0; i < len / this.buf.length
            && this.available >= this.buf.length; i++)
      {
         System.arraycopy(this.buf, 0, b, offset, this.buf.length);
         offset += this.buf.length;
         this.available -= this.buf.length;
      }

      final int min = (int) Math.min(this.available, len - (offset - off));
      if (min > 0)
      {
         System.arraycopy(this.buf, 0, b, offset, min);
         offset += min;
         this.available -= min;
      }

      return offset - off;
   }

   @Override
   public void reset()
   {
      this.available = this.size;
   }

   @Override
   public int available()
   {
      return (int) Math.min(this.available, Integer.MAX_VALUE);
   }

   @Override
   public long getSize()
   {
      return this.size;
   }
}
