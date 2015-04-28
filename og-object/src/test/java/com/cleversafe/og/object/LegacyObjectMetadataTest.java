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
    LegacyObjectMetadata.fromMetadata(null, 0, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromMetadataShortObjectName() {
    LegacyObjectMetadata.fromMetadata("", 0, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromMetadataLongObjectName() {
    String objectName = UUID.randomUUID().toString().replace("-", "") + "12345";
    LegacyObjectMetadata.fromMetadata(objectName, 0, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromMetadataNegativeObjectSize() {
    LegacyObjectMetadata.fromMetadata(objectString(UUID.randomUUID()), -1, 0);
  }

  @Test
  public void legacyObjectMetadataFromBytes() {
    final UUID objectName = UUID.randomUUID();
    String objectString = objectString(objectName);
    long objectSize = Long.MAX_VALUE;
    int containerSuffix = Integer.MAX_VALUE;

    final LegacyObjectMetadata objectMetadata =
        LegacyObjectMetadata.fromBytes(bytes(objectName, objectSize, containerSuffix));

    String canonical = String.format("%s,%s", objectMetadata.getName(), objectMetadata.getSize());
    assertThat(canonical, is(canonicalize(objectString, objectSize, containerSuffix)));
  }

  @Test
  public void legacyObjectMetadataFromMetadata() {
    final UUID objectName = UUID.randomUUID();
    String objectString = objectString(objectName);
    long objectSize = 0;
    int containerSuffix = 0;

    final LegacyObjectMetadata objectMetadata =
        LegacyObjectMetadata.fromMetadata(objectString, objectSize, containerSuffix);

    String canonical =
        String.format("%s,%s,%s", objectMetadata.getName(), objectMetadata.getSize(),
            objectMetadata.getContainerSuffix());
    assertThat(canonical, is(canonicalize(objectString, objectSize, containerSuffix)));
  }

  @Test
  public void compareEqualsNull() {
    String objectString = objectString(UUID.randomUUID());
    long objectSize = 0;
    int containerSuffix = 0;

    assertThat(
        LegacyObjectMetadata.fromMetadata(objectString, objectSize, containerSuffix).equals(null),
        is(false));
  }

  @Test
  public void compareEqualsNonMatchingType() {
    String objectString = objectString(UUID.randomUUID());
    long objectSize = 0;
    int containerSuffix = 0;

    final LegacyObjectMetadata objectName =
        LegacyObjectMetadata.fromMetadata(objectString, objectSize, containerSuffix);

    assertThat(objectName.equals("NOT_AN_OBJECT_NAME"), is(false));
  }

  private byte[] bytes(final UUID objectName, long objectSize, int containerSuffix) {
    return ByteBuffer.allocate(LegacyObjectMetadata.OBJECT_SIZE)
        .putLong(objectName.getMostSignificantBits()).putLong(objectName.getLeastSignificantBits())
        .putShort((short) 0).putLong(objectSize).putInt(containerSuffix).array();
  }

  private String objectString(UUID objectName) {
    return objectName.toString().replace("-", "") + "0000";
  }

  private String canonicalize(final String objectName, long objectSize, int containerSuffix) {
    return String.format("%s,%s,%s", objectName, objectSize, containerSuffix);
  }
}
