/* Copyright (c) IBM Corporation 2017. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;

import com.ibm.og.api.Operation;
import com.ibm.og.api.Request;
import com.ibm.og.api.Response;
import com.ibm.og.util.Context;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@code ObjectNameConsumer} implementation which consumes object names for write copy operations
 *
 * @since 1.3.0
 */
public class WriteCopyObjectNameConsumer extends AbstractObjectNameConsumer {
  /**
   * Constructs an instance
   *
   * @param objectManager the object manager for this instance to work with
   * @param statusCodes the status codes this instance should work with
   * @throws IllegalArgumentException if any status code in status codes is invalid
   */
  public WriteCopyObjectNameConsumer(final ObjectManager objectManager, final Set<Integer> statusCodes) {
    super(objectManager, Operation.WRITE_COPY, statusCodes);
  }

  protected String getSourceObjectString(final Request request, final Response response) {
    return request.getContext().get(Context.X_OG_SSE_SOURCE_OBJECT_NAME);
  }

  protected long getSourceObjectSize(final Request request) {

    return Long.parseLong(request.getContext().get(Context.X_OG_OBJECT_SIZE));
  }

  protected int getSourceObjectContainerSuffix(final Request request) {
    final String containerSuffix = request.getContext().get(Context.X_OG_SSE_SOURCE_OBJECT_CONTAINER_SUFFIX);
    if (containerSuffix == null) {
      return -1;
    } else {
      return Integer.parseInt(containerSuffix);
    }
  }

  @Override
  protected void updateObjectManager(final ObjectMetadata objectName) {
    this.objectManager.add(objectName);
  }

  @Override
  protected void updateObjectManager(final Request request, final Response response) {
    // handle the primary object for the operation
    final ObjectMetadata objectName = getObjectName(request, response);
    updateObjectManager(objectName);
    //handle source object for the write copy
    final ObjectMetadata sourceObjectName = getSSESourceObjectName(request, response);
    updateSourceObjectinObjectManager(sourceObjectName);
  }

  private ObjectMetadata getSSESourceObjectName(final Request request, final Response response) {
    final String objectString = getSourceObjectString(request, response);
    final long objectSize = getSourceObjectSize(request);
    final int containerSuffix = getSourceObjectContainerSuffix(request);
    final byte numLegalHolds = getNumberOfLegalHolds(request, response);
    final long retention = getObjectionRetention(request);
    if (objectString == null) {
      return null;
    } else {
      return LegacyObjectMetadata.fromMetadata(objectString, objectSize, containerSuffix, numLegalHolds, retention);
    }
  }

  protected void updateSourceObjectinObjectManager(ObjectMetadata objectName) {
    this.objectManager.getComplete(objectName);
  }

  @Override
  public String toString() {
    return "WriteCopyObjectNameConsumer []";
  }
}
