/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;

import java.util.Set;

import com.ibm.og.api.Operation;

/**
 * A {@code ObjectNameConsumer} implementation which consumes object names for write operations
 * 
 * @since 1.0
 */
public class WriteObjectNameConsumer extends AbstractObjectNameConsumer {
  /**
   * Constructs an instance
   * 
   * @param objectManager the object manager for this instance to work with
   * @param statusCodes the status codes this instance should work with
   * @throws IllegalArgumentException if any status code in status codes is invalid
   */
  public WriteObjectNameConsumer(final ObjectManager objectManager,
      final Set<Integer> statusCodes) {
    super(objectManager, Operation.WRITE, statusCodes);
  }

  @Override
  protected void updateObjectManager(final ObjectMetadata objectName) {
    this.objectManager.add(objectName);
  }

  @Override
  public String toString() {
    return "WriteObjectNameConsumer []";
  }
}
