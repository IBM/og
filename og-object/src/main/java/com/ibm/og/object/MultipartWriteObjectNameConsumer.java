/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;

import com.ibm.og.api.Operation;

import java.util.Set;

/**
 * A {@code ObjectNameConsumer} implementation which consumes object names for multipart write operations
 * 
 * @since 1.0
 */
public class MultipartWriteObjectNameConsumer extends AbstractObjectNameConsumer {
  /**
   * Constructs an instance
   *
   * @param objectManager the object manager for this instance to work with
   * @param statusCodes the status codes this instance should work with
   * @throws IllegalArgumentException if any status code in status codes is invalid
   */
  public MultipartWriteObjectNameConsumer(final ObjectManager objectManager,
      final Set<Integer> statusCodes) {
    super(objectManager, Operation.MULTIPART_WRITE_COMPLETE, statusCodes);
  }

  @Override
  protected void updateObjectManager(final ObjectMetadata objectName) {
    this.objectManager.add(objectName);
  }

  @Override
  public String toString() {
    return "MultipartWriteObjectNameConsumer []";
  }
}
