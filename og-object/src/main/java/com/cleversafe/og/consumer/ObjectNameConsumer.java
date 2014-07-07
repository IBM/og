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

package com.cleversafe.og.consumer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

import com.cleversafe.og.http.util.HttpUtil;
import com.cleversafe.og.object.LegacyObjectName;
import com.cleversafe.og.object.ObjectName;
import com.cleversafe.og.object.manager.ObjectManager;
import com.cleversafe.og.object.manager.ObjectManagerException;
import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.util.Operation;
import com.cleversafe.og.util.consumer.Consumer;
import com.cleversafe.og.util.producer.ProducerException;
import com.google.common.collect.Iterables;
import com.google.common.io.BaseEncoding;

public abstract class ObjectNameConsumer implements Consumer<Response>
{
   protected final ObjectManager objectManager;
   private final Map<String, Request> pendingRequests;
   private final Operation operation;
   private final List<Integer> statusCodes;

   public ObjectNameConsumer(
         final ObjectManager objectManager,
         final Map<String, Request> pendingRequests,
         final Operation operation,
         final List<Integer> statusCodes)
   {
      this.objectManager = checkNotNull(objectManager);
      this.pendingRequests = checkNotNull(pendingRequests);
      this.operation = checkNotNull(operation);
      checkNotNull(statusCodes);
      checkArgument(!statusCodes.isEmpty(), "statusCodes must not be empty");
      for (final int statusCode : statusCodes)
      {
         checkArgument(HttpUtil.VALID_STATUS_CODES.contains(statusCode),
               "all statusCodes in list must be valid status codes [%s]", statusCode);
      }
      this.statusCodes = statusCodes;
   }

   @Override
   public void consume(final Response response)
   {
      checkNotNull(response);
      final String requestId = response.getMetadata(Metadata.REQUEST_ID);
      final Request request = this.pendingRequests.get(requestId);
      if (request == null)
         throw new ProducerException(String.format(
               "No matching request found for response with request id [%s]", requestId));

      // if this consumer is not relevant for the current response, ignore
      if (this.operation != HttpUtil.toOperation(request.getMethod()))
         return;

      // if the status code of this response does not match what can be consumed, ignore
      if (!Iterables.contains(this.statusCodes, response.getStatusCode()))
         return;

      final String objectString = getObjectString(request, response);
      if (objectString == null)
         throw new ProducerException("Unable to determine object");

      final ObjectName objectName =
            LegacyObjectName.forBytes(BaseEncoding.base16().lowerCase().decode(objectString));
      updateManager(objectName);
   }

   protected String getObjectString(final Request request, final Response response)
   {
      String objectString = request.getMetadata(Metadata.OBJECT_NAME);
      // SOH writes
      if (objectString == null)
         objectString = response.getMetadata(Metadata.OBJECT_NAME);

      return objectString;
   }

   private void updateManager(final ObjectName objectName)
   {
      try
      {
         updateObjectManager(objectName);
      }
      catch (final ObjectManagerException e)
      {
         throw new ProducerException(e);
      }
   }

   protected abstract void updateObjectManager(ObjectName objectName);
}
