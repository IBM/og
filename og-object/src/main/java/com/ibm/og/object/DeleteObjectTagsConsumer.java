/* Copyright (c) IBM Corporation 2020. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;

import com.ibm.og.api.Operation;

import java.util.Set;

/**
 * A {@code DeleteObjectTagsConsumer} implementation which consumes handles response for writing object tags
 *
 * @since 1.11.0
 */
public class DeleteObjectTagsConsumer extends AbstractObjectNameConsumer {
  /**
   * Constructs an instance
   *
   * @param objectManager the object manager for this instance to work with
   * @param statusCodes the status codes this instance should work with
   * @throws IllegalArgumentException if any status code in status codes is invalid
   */
  public DeleteObjectTagsConsumer(final ObjectManager objectManager,
                                  final Set<Integer> statusCodes) {
    super(objectManager, Operation.DELETE_TAGS, statusCodes);
  }

  @Override
  protected void updateObjectManager(final ObjectMetadata objectName) {
    //add the object back to the object store without considering the returned status code
    this.objectManager.updateObject(objectName);
  }

  @Override
  public String toString() {
    return "DeleteObjectTagsConsumer []";
  }
}
