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

import org.junit.Assert;
import org.junit.Test;

public class LegacyObjectNameTest
{
   @Test(expected = NullPointerException.class)
   public void testNullObjectNameBytes()
   {
      LegacyObjectName.forBytes(null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testObjectNameBytesLessThan18()
   {
      LegacyObjectName.forBytes(new byte[LegacyObjectName.ID_LENGTH - 1]);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testObjectNameBytesGreaterThan18()
   {
      LegacyObjectName.forBytes(new byte[LegacyObjectName.ID_LENGTH + 1]);
   }

   @Test(expected = NullPointerException.class)
   public void testNullObjectNameUUID()
   {
      LegacyObjectName.forUUID(null);
   }

   @Test
   public void testLegacyObjectNameBytes()
   {
      final UUID uuid = UUID.randomUUID();
      final byte[] bid = createBytes(uuid);
      final String sid = uuid.toString().replace("-", "") + "0000";
      final LegacyObjectName objectName = LegacyObjectName.forBytes(bid);
      Assert.assertEquals(sid, objectName.toString());
      assertBytesEqual(uuid, objectName);
   }

   @Test
   public void testLegacyObjectNameUUID()
   {
      final UUID uuid = UUID.randomUUID();
      final String sid = uuid.toString().replace("-", "") + "0000";
      final LegacyObjectName objectName = LegacyObjectName.forUUID(uuid);
      Assert.assertEquals(sid, objectName.toString());
      assertBytesEqual(uuid, objectName);
   }

   @Test(expected = NullPointerException.class)
   public void testSetNullBytes()
   {
      final LegacyObjectName objectName = LegacyObjectName.forUUID(UUID.randomUUID());
      objectName.setName((byte[]) null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testSetBytesLessThan18()
   {
      final LegacyObjectName objectName = LegacyObjectName.forUUID(UUID.randomUUID());
      objectName.setName(new byte[LegacyObjectName.ID_LENGTH - 1]);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testSetBytesGreaterThan18()
   {
      final LegacyObjectName objectName = LegacyObjectName.forUUID(UUID.randomUUID());
      objectName.setName(new byte[LegacyObjectName.ID_LENGTH + 1]);
   }

   @Test(expected = NullPointerException.class)
   public void testSetNullUUID()
   {
      final LegacyObjectName objectName = LegacyObjectName.forUUID(UUID.randomUUID());
      objectName.setName((UUID) null);
   }

   @Test
   public void testSetObjectNameBytes()
   {
      final LegacyObjectName objectName = LegacyObjectName.forUUID(UUID.randomUUID());
      final UUID uuid = UUID.randomUUID();
      final byte[] bid = createBytes(uuid);
      final String sid = uuid.toString().replace("-", "") + "0000";
      objectName.setName(bid);
      Assert.assertEquals(sid, objectName.toString());
      assertBytesEqual(uuid, objectName);
   }

   @Test
   public void testSetObjectNameUUID()
   {
      final LegacyObjectName objectName = LegacyObjectName.forUUID(UUID.randomUUID());
      final UUID uuid = UUID.randomUUID();
      final String sid = uuid.toString().replace("-", "") + "0000";
      objectName.setName(uuid);
      Assert.assertEquals(sid, objectName.toString());
      assertBytesEqual(uuid, objectName);
   }

   private void assertBytesEqual(final UUID uuid, final LegacyObjectName objectName)
   {

      final byte[] expectedBytes = createBytes(uuid);
      final byte[] actualBytes = objectName.toBytes();
      Assert.assertEquals(expectedBytes.length, actualBytes.length);
      for (int i = 0; i < expectedBytes.length; i++)
      {
         Assert.assertEquals(expectedBytes[0], actualBytes[0]);
      }
   }

   private byte[] createBytes(final UUID uuid)
   {
      final ByteBuffer buf = ByteBuffer.allocate(18);
      buf.putLong(uuid.getMostSignificantBits());
      buf.putLong(uuid.getLeastSignificantBits());
      buf.putShort((short) 0);
      return buf.array();
   }
}
