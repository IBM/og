/* Copyright (c) IBM Corporation 2018. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.object;

import com.ibm.og.api.Operation;
import com.ibm.og.api.Request;
import com.ibm.og.api.Response;
import com.ibm.og.util.Context;

import java.util.Set;

/**
 * A {@code ObjectNameConsumer} implementation which consumes object names for writing LegalHold to an object operation
 *
 * @since 1.0
 */
public class ExtendRetentionObjectNameConsumer extends AbstractObjectNameConsumer {
  /**
   * Constructs an instance
   *
   * @param objectManager the object manager for this instance to work with
   * @param statusCodes the status codes this instance should work with
   * @throws IllegalArgumentException if any status code in status codes is invalid
   */
  public ExtendRetentionObjectNameConsumer(final ObjectManager objectManager,
                                           final Set<Integer> statusCodes) {
    super(objectManager, Operation.EXTEND_RETENTION, statusCodes);
  }

  @Override
  protected void updateObjectManager(final ObjectMetadata objectName) {

    this.objectManager.updateObject(objectName);
  }

  protected int getObjectRetention(final Request request, final Response response) {
    final String sRetention = request.getContext().get(Context.X_OG_OBJECT_RETENTION);
    final String sRExtension = request.getContext().get(Context.X_OG_OBJECT_RETENTION_EXT);
    if (response.getStatusCode() == 200) {
      return Integer.parseInt(sRetention) + Integer.parseInt(sRExtension);
    } else {
      return Integer.parseInt(sRetention);
    }
  }

  @Override
  public String toString() {
    return "ExtendRetentionObjectNameConsumer []";
  }
}
