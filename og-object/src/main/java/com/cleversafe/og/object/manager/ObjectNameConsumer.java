//
// Copyright (C) 2005-2011 Cleversafe, Inc. All rights reserved.
//
// Contact Information:
// Cleversafe, Inc.
// 222 South Riverside Plaza
// Suite 1700
// Chicago, IL 60606, USA
//
// licensing@cleversafe.com
//
// END-OF-HEADER
//
// -----------------------
// @author: rveitch
//
// Date: Mar 29, 2014
// ---------------------

package com.cleversafe.og.object.manager;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

import com.cleversafe.og.api.Consumer;
import com.cleversafe.og.http.util.MethodUtil;
import com.cleversafe.og.http.util.UriUtil;
import com.cleversafe.og.object.LegacyObjectName;
import com.cleversafe.og.object.ObjectName;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.util.OperationType;
import com.google.common.collect.Iterables;
import com.google.common.io.BaseEncoding;

public class ObjectNameConsumer implements Consumer<Response>
{
   private final ObjectManager objectManager;
   private final Map<Long, Request> pendingRequests;
   private final OperationType operation;
   private final List<Integer> statusCodes;

   public ObjectNameConsumer(
         final ObjectManager objectManager,
         final Map<Long, Request> pendingRequests,
         final OperationType operation,
         final List<Integer> statusCodes)
   {
      this.objectManager = checkNotNull(objectManager, "objectManager must not be null");
      this.pendingRequests = checkNotNull(pendingRequests, "pendingRequests must not be null");
      this.operation = checkNotNull(operation, "operation must not be null");
      checkNotNull(statusCodes, "statusCodes must not be null");
      checkArgument(statusCodes.size() > 0, "statusCodes size must be > 0");
      for (final int statusCode : statusCodes)
      {
         // TODO use guava range
         checkArgument(statusCode >= 100 && statusCode <= 599,
               "all statusCodes in list must be in range [100, 599] [%s]", statusCode);
      }
      this.statusCodes = statusCodes;
   }

   @Override
   public void consume(final Response response)
   {
      checkNotNull(response, "response must not be null");
      final Request request = this.pendingRequests.get(response.getRequestId());

      // if this consumer is not relevant for the current response, ignore
      if (this.operation != MethodUtil.toOperationType(request.getMethod()))
         return;

      // if the status code of this response does not match what can be consumed, ignore
      if (!Iterables.contains(this.statusCodes, response.getStatusCode()))
         return;

      // TODO check for null?
      final String s = getObjectString(request, response);
      final ObjectName objectName =
            LegacyObjectName.forBytes(BaseEncoding.base16().lowerCase().decode(s));
      updateObjectManager(objectName);
   }

   protected String getObjectString(final Request request, final Response response)
   {
      return UriUtil.getObjectName(request.getURI());
   }

   private void updateObjectManager(final ObjectName objectName)
   {
      if (OperationType.WRITE == this.operation)
         this.objectManager.writeNameComplete(objectName);
      else if (OperationType.READ == this.operation)
         this.objectManager.releaseNameFromRead(objectName);
   }
}
