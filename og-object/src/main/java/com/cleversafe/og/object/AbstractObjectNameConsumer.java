/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.object;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.HttpUtil;
import com.cleversafe.og.util.Operation;
import com.cleversafe.og.util.Pair;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.Subscribe;

/**
 * A consumer of object names
 * 
 * @since 1.0
 */
public abstract class AbstractObjectNameConsumer {
  protected final ObjectManager objectManager;
  private final Operation operation;
  private final Set<Integer> statusCodes;

  /**
   * Constructs an instance
   * 
   * @param objectManager the object manager for this instance to work with
   * @param operation the operation type this instance should work with
   * @param statusCodes the status codes this instance should work with
   * @throws IllegalArgumentException if any status code in status codes is invalid
   */
  public AbstractObjectNameConsumer(final ObjectManager objectManager, final Operation operation,
      final Set<Integer> statusCodes) {
    this.objectManager = checkNotNull(objectManager);
    this.operation = checkNotNull(operation);
    this.statusCodes = ImmutableSet.copyOf(statusCodes);
    checkArgument(!this.statusCodes.isEmpty(), "statusCodes must not be empty");
    for (final int statusCode : this.statusCodes) {
      checkArgument(HttpUtil.VALID_STATUS_CODES.contains(statusCode),
          "all statusCodes in list must be valid status codes [%s]", statusCode);
    }
  }

  /**
   * Consumes operations and processes object names
   * 
   * @param operation the operation to process
   */
  @Subscribe
  public void consume(final Pair<Request, Response> operation) {
    checkNotNull(operation);
    final Request request = operation.getKey();
    final Response response = operation.getValue();

    // if this consumer is not relevant for the current response, ignore
    if (this.operation != HttpUtil.toOperation(request.getMethod())) {
      return;
    }

    // if the status code of this response does not match what can be consumed, ignore
    if (!this.statusCodes.contains(response.getStatusCode())) {
      return;
    }

    final String objectString = getObjectString(request, response);
    if (objectString == null) {
      throw new IllegalStateException("Unable to determine object");
    }

    final ObjectMetadata objectName = getObjectName(request, response);
    updateObjectManager(objectName);
  }

  private ObjectMetadata getObjectName(final Request request, final Response response) {
    final String objectString = getObjectString(request, response);
    final long objectSize = getObjectSize(request);
    final int containerSuffix = getContainerSuffix(request);
    return LegacyObjectMetadata.fromMetadata(objectString, objectSize, containerSuffix);
  }

  protected String getObjectString(final Request request, final Response response) {
    String objectString = request.headers().get(Headers.X_OG_OBJECT_NAME);
    // SOH writes
    if (objectString == null) {
      objectString = response.headers().get(Headers.X_OG_OBJECT_NAME);
    }

    return objectString;
  }

  private long getObjectSize(final Request request) {
    if (this.operation == Operation.WRITE) {
      return request.getBody().getSize();
    }
    return Long.parseLong(request.headers().get(Headers.X_OG_OBJECT_SIZE));
  }

  protected int getContainerSuffix(final Request request) {
    final String containerSuffix = request.headers().get(Headers.X_OG_CONTAINER_SUFFIX);
    if (containerSuffix == null) {
      return -1;
    } else {
      return Integer.parseInt(containerSuffix);
    }
  }

  protected abstract void updateObjectManager(ObjectMetadata objectName);
}
