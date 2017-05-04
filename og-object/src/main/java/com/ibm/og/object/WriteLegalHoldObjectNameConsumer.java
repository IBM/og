/* Copyright (c) IBM Corporation 2017. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;

import com.ibm.og.api.Operation;

import java.util.Set;

/**
 * A {@code ObjectNameConsumer} implementation which consumes object names for writing LegalHold to an object operation
 *
 * @since 1.0
 */
public class WriteLegalHoldObjectNameConsumer extends AbstractObjectNameConsumer {
  /**
   * Constructs an instance
   *
   * @param objectManager the object manager for this instance to work with
   * @param statusCodes the status codes this instance should work with
   * @throws IllegalArgumentException if any status code in status codes is invalid
   */
  public WriteLegalHoldObjectNameConsumer(final ObjectManager objectManager,
                                          final Set<Integer> statusCodes) {
    super(objectManager, Operation.WRITE_LEGAL_HOLD, statusCodes);
  }

  @Override
  protected void updateObjectManager(final ObjectMetadata objectName) {
//    this.objectManager.getComplete(objectName);
//    this.objectManager.removeObject(objectName);
    this.objectManager.updateObject(objectName);
  }

  @Override
  public String toString() {
    return "WriteObjectNameConsumer []";
  }
}
