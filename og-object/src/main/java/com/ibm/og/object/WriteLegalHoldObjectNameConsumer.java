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
    this.objectManager.updateObject(objectName);
  }

  @Override
  protected byte getNumberOfLegalHolds(final Request request, final Response response) {
    final String nHolds = request.getContext().get(Context.X_OG_NUM_LEGAL_HOLDS);

    if (nHolds == null) {
      return 0;
    } else {
      if (response.getStatusCode() != 200) {
        return Byte.valueOf(nHolds);
      }
      else {
        if (Byte.valueOf(nHolds) >= 0) {
          byte reducedHolds = Byte.valueOf(nHolds);
          reducedHolds += (byte) 1;
          return reducedHolds;
        } else {
          return Byte.valueOf(nHolds);
        }
      }
    }
  }

  @Override
  public String toString() {
    return "WriteLegalHoldObjectNameConsumer []";
  }
}
