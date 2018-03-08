/* Copyright (c) IBM Corporation 2017. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;

import com.ibm.og.api.Operation;
import com.ibm.og.api.Request;
import com.ibm.og.api.Response;
import com.ibm.og.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * A {@code ObjectNameConsumer} implementation which consumes object names for read operations
 *
 * @since 1.0
 */
public class DeleteObjectConsumer extends AbstractObjectNameConsumer {
  private static final Logger _logger = LoggerFactory.getLogger(DeleteObjectConsumer.class);
  /**
   * Constructs an instance
   *
   * @param objectManager the object manager for this instance to work with
   * @param statusCodes the status codes this instance should work with
   * @throws IllegalArgumentException if any status code in status codes is invalid
   */
  public DeleteObjectConsumer(final ObjectManager objectManager, final Set<Integer> statusCodes) {
    super(objectManager, Operation.DELETE, statusCodes);
  }

  @Override
  protected void updateObjectManager(final ObjectMetadata objectName) {
    this.objectManager.removeUpdatedObject(objectName);
    _logger.trace("consume object objectName");
  }

  @Override
  protected void updateObjectManager(final Request request, final Response response) {
    // remove the object permanently unless the status code is 451
    ObjectMetadata object = getObjectName(request, response);
    updateObjectManager(object);
    // response code 599 is returned when apache client is shutdown. The object might have been
    // possibly deleted. So do not add object to the object store
    if (response.getStatusCode() != 204 && response.getStatusCode() != 599) {
      _logger.trace("adding object {}", object);
      this.objectManager.add(object);
    }
  }


  @Override
  public String toString() {
    return "DeleteObjectNameConsumer []";
  }
}
