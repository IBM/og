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

package com.cleversafe.oom.object;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.Validate;

/**
 * An <code>ObjectName</code> implementation that represents an object name of a fixed length of 18
 * bytes, as is expected by the legacy bin file format.
 */
public class LegacyObjectName implements ObjectName
{
   public static final int ID_LENGTH = 18;
   private final ByteBuffer bytes;

   private LegacyObjectName(final byte[] objectName)
   {
      this.bytes = ByteBuffer.allocate(ID_LENGTH);
      this.bytes.put(objectName);
   }

   private LegacyObjectName(final UUID objectName)
   {
      this.bytes = ByteBuffer.allocate(ID_LENGTH);
      setUUID(objectName);
   }

   /**
    * Configures an <code>LegacyObjectName</code> instance, using the provided bytes as the name
    * 
    * @param objectName
    *           the object name, in bytes
    * @return a <code>LegacyObjectName</code> instance
    * @throws NullPointerException
    *            if objectName is null
    * @throws IllegalArgumentException
    *            if the length of objectName is not 18
    */
   public static LegacyObjectName forBytes(final byte[] objectName)
   {
      LegacyObjectName.validateBytes(objectName);
      return new LegacyObjectName(objectName);
   }

   /**
    * Configures an <code>ObjectName</code> instance, using the provided UUID as the name. The most
    * significant and least significant bits of the UUID will represent the first 16 bytes of this
    * instance, and the remaining 2 bytes will be zero padded
    * 
    * @param objectName
    *           the object name, as a UUID
    * @return a <code>LegacyObjectName</code> instance
    * @throws NullPointerException
    *            if objectName is null
    */
   public static LegacyObjectName forUUID(final UUID objectName)
   {
      Validate.notNull(objectName, "objectName must not be null");
      return new LegacyObjectName(objectName);
   }

   /**
    * {@inheritDoc}
    * 
    * @throws IllegalArgumentException
    *            if the length of objectName is not 18
    */
   @Override
   public void setName(final byte[] objectName)
   {
      LegacyObjectName.validateBytes(objectName);
      this.bytes.clear();
      this.bytes.put(objectName);
   }

   public void setName(final UUID objectName)
   {
      Validate.notNull(objectName, "objectName must not be null");
      this.bytes.clear();
      setUUID(objectName);
   }

   @Override
   public byte[] toBytes()
   {
      return this.bytes.array();
   }

   @Override
   public String toString()
   {
      return new String(Hex.encodeHex(toBytes()));
   }

   private static void validateBytes(final byte[] objectName)
   {
      Validate.notNull(objectName, "objectName must not be null");
      Validate.isTrue(objectName.length == ID_LENGTH, "objectName length must be = 0 [%s]",
            objectName.length);
   }

   private void setUUID(final UUID objectName)
   {
      this.bytes.putLong(objectName.getMostSignificantBits());
      this.bytes.putLong(objectName.getLeastSignificantBits());
      this.bytes.putShort((short) 0);
   }
}