/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.object;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.junit.Test;

public class LegacyObjectMetadataTest {
  @Test(expected = NullPointerException.class)
  public void nullFromBytes() {
    LegacyObjectMetadata.fromBytes(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void bytesLessThanObjectLength() {
    LegacyObjectMetadata.fromBytes(new byte[LegacyObjectMetadata.OBJECT_SIZE - 1]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void bytesGreaterThanObjectLength() {
    LegacyObjectMetadata.fromBytes(new byte[LegacyObjectMetadata.OBJECT_SIZE + 1]);
  }

  @Test(expected = NullPointerException.class)
  public void fromMetadataNullObjectName() {
    LegacyObjectMetadata.fromMetadata(null, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromMetadataShortObjectName() {
    LegacyObjectMetadata.fromMetadata("", 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromMetadataLongObjectName() {
    final String objectName = UUID.randomUUID().toString().replace("-", "") + "12345";
    LegacyObjectMetadata.fromMetadata(objectName, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromMetadataNegativeObjectSize() {
    LegacyObjectMetadata.fromMetadata(objectString(UUID.randomUUID()), -1);
  }

  @Test
  public void legacyObjectMetadataFromBytes() {
    final UUID objectName = UUID.randomUUID();
    final String objectString = objectString(objectName);
    final long objectSize = Long.MAX_VALUE;

    final LegacyObjectMetadata objectMetadata =
        LegacyObjectMetadata.fromBytes(bytes(objectName, objectSize));

    final String canonical =
        String.format("%s,%s", objectMetadata.getName(), objectMetadata.getSize());
    assertThat(canonical, is(canonicalize(objectString, objectSize)));
  }

  @Test
  public void legacyObjectMetadataFromMetadata() {
    final UUID objectName = UUID.randomUUID();
    final String objectString = objectString(objectName);
    final long objectSize = 0;

    final LegacyObjectMetadata objectMetadata =
        LegacyObjectMetadata.fromMetadata(objectString, objectSize);

    final String canonical =
        String.format("%s,%s", objectMetadata.getName(), objectMetadata.getSize());
    assertThat(canonical, is(canonicalize(objectString, objectSize)));
  }

  @Test
  public void compareEqualsNull() {
    final String objectString = objectString(UUID.randomUUID());
    final long objectSize = 0;

    assertThat(LegacyObjectMetadata.fromMetadata(objectString, objectSize).equals(null), is(false));
  }

  @Test
  public void compareEqualsNonMatchingType() {
    final String objectString = objectString(UUID.randomUUID());
    final long objectSize = 0;

    final LegacyObjectMetadata objectName =
        LegacyObjectMetadata.fromMetadata(objectString, objectSize);

    assertThat(objectName.equals("NOT_AN_OBJECT_NAME"), is(false));
  }

  private byte[] bytes(final UUID objectName, final long objectSize) {
    return ByteBuffer.allocate(LegacyObjectMetadata.OBJECT_SIZE)
        .putLong(objectName.getMostSignificantBits()).putLong(objectName.getLeastSignificantBits())
        .putShort((short) 0).putLong(objectSize).array();
  }

  private String objectString(final UUID objectName) {
    return objectName.toString().replace("-", "") + "0000";
  }

  private String canonicalize(final String objectName, final long objectSize) {
    return String.format("%s,%s", objectName, objectSize);
  }
}
