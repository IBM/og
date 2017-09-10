/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.google.common.io.BaseEncoding;

/**
 * A defacto {@code ObjectMetadata} implementation.
 * 
 * @since 1.0
 */
public class LegacyObjectMetadata implements ObjectMetadata {
  public static final int OBJECT_NAME_SIZE = 18;
  public static final int OBJECT_SIZE_SIZE = 8;
  public static final int OBJECT_SUFFIX_SIZE = 4;
  public static final int OBJECT_LEGAL_HOLDS_SIZE = 1;
  public static final int OBJECT_RETENTION_SIZE = 4;
  public static final int OBJECT_SIZE = OBJECT_NAME_SIZE + OBJECT_SIZE_SIZE + OBJECT_SUFFIX_SIZE +
          OBJECT_LEGAL_HOLDS_SIZE + OBJECT_RETENTION_SIZE;
  private static final BaseEncoding ENCODING = BaseEncoding.base16().lowerCase();
  protected final ByteBuffer objectBuffer;
  public static final byte MAJOR_VERSION = (byte)2;
  public static final byte MINOR_VERSION = (byte)0;


  protected LegacyObjectMetadata(final ByteBuffer objectBuffer) {
    this.objectBuffer = objectBuffer;
  }

  /**
   * Configures an instance using the provided metadata packed as bytes
   * 
   * @param objectBytes the object metadata as bytes
   * @return a {@code LegacyObjectMetadata} instance
   * @throws IllegalArgumentException if the length of objectBytes is invalid
   */
  public static LegacyObjectMetadata fromBytes(final byte[] objectBytes) {
    checkNotNull(objectBytes);
    checkArgument(objectBytes.length == OBJECT_SIZE,
        String.format("objectName length must be == %s", OBJECT_SIZE) + " [%s]",
        objectBytes.length);

    return new LegacyObjectMetadata(ByteBuffer.allocate(OBJECT_SIZE).put(objectBytes));
  }

  /**
   * Configures an instance using the provided metadata,
   * 
   * @param objectName the object name; must be base16 encoded / uuid friendly
   * @param objectSize the size of the object
   * @param containerSuffix object's container suffix
   * @return a {@code LegacyObjectMetadata} instance
   * @throws IllegalArgumentException if objectSize is negative
   */
  public static LegacyObjectMetadata fromMetadata(final String objectName, final long objectSize,
      final int containerSuffix, final byte numLegalHolds, final int retentionPeriod) {
    checkNotNull(objectName);
    // HACK; assume 1 char == 2 bytes for object name string length checking
    final int stringLength = 2 * OBJECT_NAME_SIZE;
    checkArgument(objectName.length() == stringLength,
        String.format("objectName length must be == %s", stringLength) + " [%s]",
        objectName.length());
    checkArgument(objectSize >= 0, "objectSize must be >= 0 [%s]", objectSize);
    checkArgument(containerSuffix >= -1, "containerSuffix must be >= -1 [%s]", containerSuffix);
    checkArgument(numLegalHolds >= -1, "numLegalHolds must be >= -1 [%s]", numLegalHolds);
    checkArgument(retentionPeriod >= -1, "retentionPeriod must be >= -1 [%s]", retentionPeriod);

    final ByteBuffer objectBuffer = ByteBuffer.allocate(OBJECT_SIZE);
    objectBuffer.put(ENCODING.decode(objectName), 0, OBJECT_NAME_SIZE);
    objectBuffer.putLong(objectSize);
    objectBuffer.putInt(containerSuffix);
    objectBuffer.put(numLegalHolds);
    objectBuffer.putInt(retentionPeriod);
    return new LegacyObjectMetadata(objectBuffer);
  }

  @Override
  public String getName() {
    return ENCODING.encode(this.objectBuffer.array(), 0, OBJECT_NAME_SIZE);
  }

  @Override
  public long getSize() {
    return this.objectBuffer.getLong(OBJECT_NAME_SIZE);
  }

  @Override
  public int getContainerSuffix() {
    return this.objectBuffer.getInt(OBJECT_NAME_SIZE + OBJECT_SIZE_SIZE);
  }

  @Override
  public int getNumberOfLegalHolds() {
    return this.objectBuffer.get(OBJECT_NAME_SIZE + OBJECT_SIZE_SIZE + OBJECT_SUFFIX_SIZE);
  }

  @Override
  public int getRetention() {
    int retention = this.objectBuffer.getInt(OBJECT_NAME_SIZE + OBJECT_SIZE_SIZE + OBJECT_SUFFIX_SIZE + OBJECT_LEGAL_HOLDS_SIZE);
    return retention;
  }

  @Override
  public boolean equals(final Object obj) {
    //todo: fix this. do we need to take into account object legal holds and retention for equality
    if (obj == null) {
      return false;
    }

    if (!(obj instanceof ObjectMetadata)) {
      return false;
    }

    final ObjectMetadata other = (ObjectMetadata) obj;
    byte[] a1 = Arrays.copyOf(toBytes(), OBJECT_NAME_SIZE + OBJECT_SIZE_SIZE + OBJECT_SUFFIX_SIZE);
    byte[] a2 = Arrays.copyOf(other.toBytes(), OBJECT_NAME_SIZE + OBJECT_SIZE_SIZE + OBJECT_SUFFIX_SIZE);
    return  Arrays.equals(a1, a2);
  }

  @Override
  public int hashCode() {
    byte[] b = Arrays.copyOf(toBytes(), OBJECT_NAME_SIZE + OBJECT_SIZE_SIZE + OBJECT_SUFFIX_SIZE);
    int hashcode = Arrays.hashCode(b);
    return hashcode;
  }

  @Override
  public int compareTo(final ObjectMetadata o) {
    // TODO this compareTo implementation is heavily borrowed from String.toString. It is
    // ignorant of character encoding issues, but in our case we are storing hex digits so it
    // should be sufficient
    final byte[] b1 = toBytes();
    final byte[] b2 = o.toBytes();
    final int len1 = b1.length;
    final int len2 = b2.length;
    final int lim = Math.min(len1, len2);

    int k = 0;
    while (k < lim) {
      final byte c1 = b1[k];
      final byte c2 = b2[k];
      if (c1 != c2) {
        return c1 - c2;
      }
      k++;
    }
    return len1 - len2;
  }

  @Override
  public byte[] toBytes() {
    return this.objectBuffer.array();
    //return Arrays.copyOf(this.objectBuffer.array(), OBJECT_NAME_SIZE + OBJECT_SIZE_SIZE + OBJECT_SUFFIX_SIZE);
  }

  @Override
  public String toString() {
    return String.format("LegacyObjectMetadata [name=%s, size=%s legalholds=%s retention=%s]", getName(), getSize(),
            getNumberOfLegalHolds(), getRetention());
  }
}
