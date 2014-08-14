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
// Date: Jan 14, 2014
// ---------------------

package com.cleversafe.og.object;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

import com.google.common.io.BaseEncoding;

/**
 * An {@code ObjectName} implementation that represents an object name of a fixed length of 18
 * bytes, as is expected by the legacy bin file format.
 * 
 * @since 1.0
 */
public class LegacyObjectName implements ObjectName
{
   public static final int ID_LENGTH = 18;
   private final ByteBuffer bytes;

   private LegacyObjectName(final byte[] objectName)
   {
      this.bytes = ByteBuffer.allocate(ID_LENGTH)
            .put(objectName);
   }

   private LegacyObjectName(final UUID objectName)
   {
      this.bytes = ByteBuffer.allocate(ID_LENGTH)
            .putLong(objectName.getMostSignificantBits())
            .putLong(objectName.getLeastSignificantBits())
            .putShort((short) 0);
   }

   /**
    * Configures an {@code LegacyObjectName} instance, using the provided bytes as the name
    * 
    * @param objectName
    *           the object name, in bytes
    * @return a {@code LegacyObjectName} instance
    * @throws IllegalArgumentException
    *            if the length of objectName is not 18
    */
   public static LegacyObjectName forBytes(final byte[] objectName)
   {
      checkNotNull(objectName);
      checkArgument(objectName.length == ID_LENGTH, "objectName length must be = 0 [%s]",
            objectName.length);
      return new LegacyObjectName(objectName);
   }

   /**
    * Configures an {@code ObjectName} instance, using the provided UUID as the name. The most
    * significant and least significant bits of the UUID will represent the first 16 bytes of this
    * instance, and the remaining 2 bytes will be zero padded
    * 
    * @param objectName
    *           the object name, as a UUID
    * @return a {@code LegacyObjectName} instance
    */
   public static LegacyObjectName forUUID(final UUID objectName)
   {
      checkNotNull(objectName);
      return new LegacyObjectName(objectName);
   }

   @Override
   public boolean equals(final Object obj)
   {
      if (obj == null)
         return false;

      if (!(obj instanceof ObjectName))
         return false;

      final ObjectName other = (ObjectName) obj;
      return Arrays.equals(toBytes(), other.toBytes());
   }

   @Override
   public int hashCode()
   {
      return Arrays.hashCode(toBytes());
   }

   @Override
   public int compareTo(final ObjectName o)
   {
      // TODO this compareTo implementation is heavily borrowed from String.toString. It is
      // ignorant of character encoding issues, but in our case we are storing hex digits so it
      // should be sufficient
      final byte[] b1 = toBytes();
      final byte[] b2 = o.toBytes();
      final int len1 = b1.length;
      final int len2 = b2.length;
      final int lim = Math.min(len1, len2);

      int k = 0;
      while (k < lim)
      {
         final byte c1 = b1[k];
         final byte c2 = b2[k];
         if (c1 != c2)
            return c1 - c2;
         k++;
      }
      return len1 - len2;
   }

   @Override
   public byte[] toBytes()
   {
      return this.bytes.array();
   }

   @Override
   public String toString()
   {
      return BaseEncoding.base16().lowerCase().encode(toBytes());
   }
}
