/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.supplier;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

import com.google.common.io.BaseEncoding;
import com.ibm.og.object.ObjectManager;
import com.ibm.og.object.ObjectMetadata;
import com.ibm.og.util.Context;
import com.google.common.base.Function;

/**
 * A function which generates object names for read from a provided {@code ObjectManager}
 *
 * @since 1.0
 */
public class MetadataObjectNameFunction implements Function<Map<String, String>, String> {
  private final ObjectManager objectManager;
  private static final BaseEncoding ENCODING = BaseEncoding.base16().lowerCase();

  /**
   * Creates an instance
   *
   * @param objectManager the object manager to draw object names from
   * @throws NullPointerException if objectManager is null
   */
  public MetadataObjectNameFunction(final ObjectManager objectManager) {
    this.objectManager = checkNotNull(objectManager);
  }

  /**
   * Creates and returns an object name. Additionally, inserts the following entries into the
   * context:
   * <ul>
   * <li>Headers.X_OG_OBJECT_NAME
   * <li>Headers.X_OG_OBJECT_SIZE</li>
   * <li>Headers.X_OG_CONTAINER_SUFFIX</li>
   * </ul>
   *
   * @param context a request creation context for storing metadata to be used by other functions
   */
  @Override
  public String apply(final Map<String, String> context) {
    final ObjectMetadata objectMetadata = this.objectManager.get();
    String objectVersion = objectMetadata.getVersion();
    if (objectVersion != null) {
      byte[] buffer = new byte[16];
      ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
      byteBuffer.put(ENCODING.decode(objectVersion), 0, 16);
      UUID uuid = new UUID(byteBuffer.getLong(0), byteBuffer.getLong(8));
      if (uuid.getMostSignificantBits() != 0 && uuid.getLeastSignificantBits() != 0) {
        context.put(Context.X_OG_OBJECT_VERSION, uuid.toString());
      }
    }
    context.put(Context.X_OG_OBJECT_NAME, objectMetadata.getName());
    context.put(Context.X_OG_OBJECT_SIZE, String.valueOf(objectMetadata.getSize()));
    context.put(Context.X_OG_CONTAINER_SUFFIX, String.valueOf(objectMetadata.getContainerSuffix()));

    return objectMetadata.getName();
  }

  @Override
  public String toString() {
    return "MetadataObjectNameFunction []";
  }
}
