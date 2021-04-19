/* Copyright (c) IBM Corporation 2020. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;

import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkArgument;

public class ObjectFileHeader {

  // 1 byte for object name, 1 byte for
  public static final int OBJECT_FILE_HEADER_BYTES = 1;
  public static final int OBJECT_NAME_HEADER_BYTES = 1;
  public static final int OBJECT_VERSION_HEADER_BYTES = 1;
  public static final int OBJECT_SIZE_HEADER_BYTES = 1;
  public static final int OBJECT_SUFFIX_HEADER_BYTES = 1;
  public static final int OBJECT_LEGAL_HOLDS_HEADER_BYTES = 1;
  public static final int OBJECT_RETENTION_HEADER_BYTES = 1;

  public static final int HEADER_LENGTH = OBJECT_FILE_HEADER_BYTES + OBJECT_NAME_HEADER_BYTES + OBJECT_VERSION_HEADER_BYTES + OBJECT_SIZE_HEADER_BYTES +
          OBJECT_SUFFIX_HEADER_BYTES + OBJECT_LEGAL_HOLDS_HEADER_BYTES + OBJECT_RETENTION_HEADER_BYTES;

  private final ByteBuffer header;

  ObjectFileHeader(byte[] header) {
    this.header = ByteBuffer.wrap(header);
  }

  public int getObjectFileHeaderLen() {
    return header.get(0);
  }


  public int getObjectNameLen() {
    return header.get(OBJECT_FILE_HEADER_BYTES);
  }

  public int getObjectVersionLen() {
    return header.get(OBJECT_FILE_HEADER_BYTES + OBJECT_NAME_HEADER_BYTES);
  }

  public int getObjectSizeLen() {
    return header.get(OBJECT_FILE_HEADER_BYTES + OBJECT_NAME_HEADER_BYTES + OBJECT_SIZE_HEADER_BYTES);
  }

  public int getObjectSuffixLen() {
    return header.get(OBJECT_FILE_HEADER_BYTES + OBJECT_NAME_HEADER_BYTES + OBJECT_SIZE_HEADER_BYTES
              + OBJECT_SUFFIX_HEADER_BYTES);
  }

  public int getObjectLegalHoldsLen() {
    return header.get(OBJECT_FILE_HEADER_BYTES + OBJECT_NAME_HEADER_BYTES + OBJECT_SIZE_HEADER_BYTES
            + OBJECT_SUFFIX_HEADER_BYTES + OBJECT_LEGAL_HOLDS_HEADER_BYTES);
  }

  public int getObjectRetentionLen() {
    return header.get(OBJECT_FILE_HEADER_BYTES + OBJECT_NAME_HEADER_BYTES + OBJECT_SIZE_HEADER_BYTES
            + OBJECT_SUFFIX_HEADER_BYTES + OBJECT_LEGAL_HOLDS_HEADER_BYTES + OBJECT_RETENTION_HEADER_BYTES);
  }


  public static ObjectFileHeader fromMetadata(int objectNameLen, int objectVersionLen, int objectSizeLen,
                                                     int objectSuffixLen, int objectLegalHoldsLen,
                                                     int objectRetentionLen) {
    ByteBuffer b = ByteBuffer.allocate(ObjectFileHeader.HEADER_LENGTH);
    b.put((byte)ObjectFileHeader.HEADER_LENGTH);
    b.put((byte)objectNameLen);
    b.put((byte)objectVersionLen);
    b.put((byte)objectSizeLen);
    b.put((byte)objectSuffixLen);
    b.put((byte)objectLegalHoldsLen);
    b.put((byte)objectRetentionLen);
    return new ObjectFileHeader(b.array());
  }

  public static ObjectFileHeader fromCharString(String line) {
      ByteBuffer b = ByteBuffer.allocate(line.getBytes().length);
      String[] components = line.split(",");
      checkArgument(components.length == 7, "Invalid object file header - %s", line);
      byte headerLength = Byte.parseByte(components[0]);
      byte objectNameLength = Byte.parseByte(components[1]);
      byte objectVersionLength = Byte.parseByte(components[2]);
      byte objectSizeLength = Byte.parseByte(components[3]);
      byte objectSuffixLength = Byte.parseByte(components[4]);
      byte objectLegalHoldsLength = Byte.parseByte(components[5]);
      byte objectRetentionLength = Byte.parseByte(components[6]);
      ObjectFileHeader header = fromMetadata(objectNameLength, objectVersionLength,
              objectSizeLength, objectSuffixLength, objectLegalHoldsLength, objectRetentionLength);
      return header;
  }

  public byte[] getBytes() {
    return this.header.array();
  }

}
