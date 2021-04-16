/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.junit.Test;

public class LegacyObjectMetadataTest {
  @Test(expected = NullPointerException.class)
  public void nullFromBytes() {
    LegacyObjectMetadata.fromBytes(null, false);
  }

  @Test(expected = IllegalArgumentException.class)
  public void bytesLessThanObjectLength() {
    LegacyObjectMetadata.fromBytes(new byte[LegacyObjectMetadata.OBJECT_SIZE - 1], true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void bytesGreaterThanObjectLength() {
    LegacyObjectMetadata.fromBytes(new byte[LegacyObjectMetadata.OBJECT_SIZE + 1], false);
  }

  @Test(expected = NullPointerException.class)
  public void fromMetadataNullObjectName() {
    LegacyObjectMetadata.fromMetadata(null, 0, 0, (byte)-1, -1, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromMetadataShortObjectName() {
    LegacyObjectMetadata.fromMetadata("", 0,
            0, (byte)-1, -1, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromMetadataInvalidContainerSuffix() {
    LegacyObjectMetadata.fromMetadata(objectString(UUID.randomUUID()), 1, -2, (byte)-1,
            -1, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromMetadataLongObjectName() {
    final String objectName = UUID.randomUUID().toString().replace("-", "") + "12345";
    LegacyObjectMetadata.fromMetadata(objectName, 0, 0, (byte)-1, -1, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromMetadataNegativeObjectSize() {
    LegacyObjectMetadata.fromMetadata(objectString(UUID.randomUUID()), -1, 0, (byte)-1,
            -1, null);
  }

  @Test
  public void legacyObjectMetadataFromBytes() {
    final UUID objectName = UUID.randomUUID();
    final String objectString = objectString(objectName);
    final long objectSize = Long.MAX_VALUE;
    final int containerSuffix = Integer.MAX_VALUE;

    final LegacyObjectMetadata objectMetadata =
        LegacyObjectMetadata.fromBytes(bytes(objectName, objectSize, containerSuffix), false);

    final String canonical = String.format("%s,%s,%s", objectMetadata.getName(),
        objectMetadata.getSize(), objectMetadata.getContainerSuffix());
    assertThat(canonical, is(canonicalize(objectString, objectSize, containerSuffix)));
  }

  @Test
  public void legacyObjectMetadataFromMetadata() {
    final UUID objectName = UUID.randomUUID();
    final String objectString = objectString(objectName);
    final long objectSize = 0;
    final int containerSuffix = 0;

    final LegacyObjectMetadata objectMetadata =
        LegacyObjectMetadata.fromMetadata(objectString, objectSize, containerSuffix, (byte)-1, -1, null);

    final String canonical = String.format("%s,%s,%s", objectMetadata.getName(),
        objectMetadata.getSize(), objectMetadata.getContainerSuffix());
    assertThat(canonical, is(canonicalize(objectString, objectSize, containerSuffix)));
  }

  @Test
  public void legacyObjectMetadataWithObjectVersionFromMetadata() {
    final UUID objectName = UUID.randomUUID();
    final String objectString = objectString(objectName);
    final UUID objectVersion = UUID.randomUUID();
    final String objectVersionString = objectVersionString(objectVersion);
    final long objectSize = 0;
    final int containerSuffix = 0;

    final LegacyObjectMetadata objectMetadata =
            LegacyObjectMetadata.fromMetadata(objectString, objectSize, containerSuffix, (byte)-1, -1,
                    objectVersionString);

    final String canonical = String.format("%s,%s,%s,%s", objectMetadata.getName(), objectMetadata.getVersion(),
            objectMetadata.getSize(), objectMetadata.getContainerSuffix());
    assertThat(canonical, is(canonicalize2(objectString, objectSize, containerSuffix, objectVersionString)));
  }


  @Test
  public void compareEqualsNull() {
    final String objectString = objectString(UUID.randomUUID());
    final long objectSize = 0;
    final int containerSuffix = 0;

    assertThat(
        LegacyObjectMetadata.fromMetadata(objectString, objectSize, containerSuffix, (byte)-1, -1, null).equals(null),
        is(false));
  }

  @Test
  public void compareEqualsNonMatchingType() {
    final String objectString = objectString(UUID.randomUUID());
    final long objectSize = 0;
    final int containerSuffix = 0;

    final LegacyObjectMetadata objectName =
        LegacyObjectMetadata.fromMetadata(objectString, objectSize, containerSuffix, (byte)-1, -1, null);

    assertThat(objectName.equals("NOT_AN_OBJECT_NAME"), is(false));
  }

  private byte[] bytes(final UUID objectName, final long objectSize, final int containerSuffix) {
    return ByteBuffer.allocate(LegacyObjectMetadata.OBJECT_SIZE)
        .putLong(objectName.getMostSignificantBits()).putLong(objectName.getLeastSignificantBits())
        .putShort((short) 0).putLong(objectSize).putInt(containerSuffix).array();
  }

  private String objectString(final UUID objectName) {
    return objectName.toString().replace("-", "") + "0000";
  }

  private String objectVersionString(final UUID objectVersion) {
    return objectVersion.toString().replace("-", "");
  }

  private String canonicalize(final String objectName, final long objectSize,
                                 final int containerSuffix) {
    return String.format("%s,%s,%s", objectName, objectSize, containerSuffix);
  }

  private String canonicalize2(final String objectName, final long objectSize,
                              final int containerSuffix, final String objectVersion) {
    return String.format("%s,%s,%s,%s", objectName, objectVersion, objectSize, containerSuffix);
  }
}
