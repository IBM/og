/* Copyright (c) IBM Corporation 2019. All Rights Reserved.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MultiDeleteConsumer extends AbstractObjectNameConsumer {

  private static final Logger _logger = LoggerFactory.getLogger(MultiDeleteConsumer.class);


  public MultiDeleteConsumer(final ObjectManager objectManager, final Set<Integer> statusCodes) {
    super(objectManager, Operation.MULTI_DELETE, statusCodes);
  }

  @Override
  protected void updateObjectManager(final ObjectMetadata objectName) {
    this.objectManager.removeUpdatedObject(objectName);
    _logger.trace("consume object objectName");
  }

  @Override
  protected void updateObjectManager(final Request request, final Response response) {
    Map<String, String> requestContext = request.getContext();
    Map<String, String> responseContext = response.getContext();
    Set<String> failedSet = new HashSet<String>();

    if (response.getStatusCode() == 200) {
      // use the object list in the request and reconcile with failed objects
      int failedCount = 0;
      if (responseContext.get(Context.X_OG_MULTI_DELETE_FAILED_OBJECTS_COUNT) != null) {
        failedCount = Integer.parseInt(responseContext.get(Context.X_OG_MULTI_DELETE_FAILED_OBJECTS_COUNT));
      }
      if (failedCount > 0) {
        for (int i = 0; i < failedCount; i++) {
          String k = String.format("failed-object-%d", i);
          String v = responseContext.get(k);
          failedSet.add(v);
        }
      }
      // go through the object names in requestContext and take action
      int requestCount = Integer.parseInt(requestContext.get(Context.X_OG_MULTI_DELETE_REQUEST_OBJECTS_COUNT));
      if (requestCount > 0) {
        // successful deletes. remove them from updating cache
        for (int i = 0; i < requestCount; i++) {
          String k = String.format("multidelete-object-%d", i);
          String v = requestContext.get(k);
          ObjectMetadata id = this.objectManager.getObjectFromUpdatingCache(v);
          if (failedSet.contains(v)) {
            //remove from updating cache and back to object management
            this.objectManager.updateObject(id);
          } else {
            // remove deleted object
            updateObjectManager(id);
          }
        }

      }
    } else {
      // add all objects in the updating cache back to object management
      // go through the object names in requestContext and take action
      int count = Integer.parseInt(requestContext.get(Context.X_OG_MULTI_DELETE_REQUEST_OBJECTS_COUNT));
      if (count > 0) {
        // request failed.  Add all objects in the request back to object manager
        for (int i = 0; i < count; i++) {
          String k = String.format("multidelete-object-%d", i);
          String v = requestContext.get(k);
          ObjectMetadata id = this.objectManager.getObjectFromUpdatingCache(v);
          //remove from updating cache and back to object management
          this.objectManager.updateObject(id);
        }
      }
    }
  }


  @Override
  public String toString() {
    return "MultiDeleteObjectNameConsumer []";
  }

}

