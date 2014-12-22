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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.junit.Test;

public class LegacyObjectNameTest {
  @Test(expected = NullPointerException.class)
  public void nullObjectNameBytes() {
    LegacyObjectName.forBytes(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void objectNameBytesLessThan18() {
    LegacyObjectName.forBytes(new byte[LegacyObjectName.ID_LENGTH - 1]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void objectNameBytesGreaterThan18() {
    LegacyObjectName.forBytes(new byte[LegacyObjectName.ID_LENGTH + 1]);
  }

  @Test(expected = NullPointerException.class)
  public void nullObjectNameUUID() {
    LegacyObjectName.forUUID(null);
  }

  @Test
  public void legacyObjectNameBytes() {
    final UUID uuid = UUID.randomUUID();
    final LegacyObjectName objectName = LegacyObjectName.forBytes(bytes(uuid));
    assertThat(objectName.toString(), is(string(uuid)));
  }

  @Test
  public void legacyObjectNameUUID() {
    final UUID uuid = UUID.randomUUID();
    final LegacyObjectName objectName = LegacyObjectName.forUUID(uuid);
    assertThat(objectName.toString(), is(string(uuid)));
  }

  @Test
  public void compareEqualsNull() {
    assertThat(LegacyObjectName.forUUID(UUID.randomUUID()).equals(null), is(false));
  }

  @Test
  public void compareEqualsNonMatchingType() {
    final LegacyObjectName objectName = LegacyObjectName.forUUID(UUID.randomUUID());
    assertThat(objectName.equals("NOT_AN_OBJECT_NAME"), is(false));
  }

  private byte[] bytes(final UUID uuid) {
    return ByteBuffer.allocate(18).putLong(uuid.getMostSignificantBits())
        .putLong(uuid.getLeastSignificantBits()).putShort((short) 0).array();
  }

  private String string(final UUID uuid) {
    return uuid.toString().replace("-", "") + "0000";
  }
}
