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

package com.cleversafe.og.object;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.cleversafe.og.api.Metadata;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.api.Response;
import com.cleversafe.og.http.HttpUtil;
import com.cleversafe.og.util.Operation;
import com.cleversafe.og.util.Pair;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.BaseEncoding;

/**
 * A consumer of object names
 * 
 * @since 1.0
 */
public abstract class AbstractObjectNameConsumer
{
   protected final ObjectManager objectManager;
   private final Operation operation;
   private final List<Integer> statusCodes;

   /**
    * Constructs an instance
    * 
    * @param objectManager
    *           the object manager for this instance to work with
    * @param operation
    *           the operation type this instance should work with
    * @param statusCodes
    *           the status codes this instance should work with
    * @throws IllegalArgumentException
    *            if any status code in status codes is invalid
    */
   public AbstractObjectNameConsumer(
         final ObjectManager objectManager,
         final Operation operation,
         final List<Integer> statusCodes)
   {
      this.objectManager = checkNotNull(objectManager);
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

   /**
    * Consumes operations and processes object names
    * 
    * @param operation
    *           the operation to process
    */
   @Subscribe
   public void consume(final Pair<Request, Response> operation)
   {
      checkNotNull(operation);
      final Request request = operation.getKey();
      final Response response = operation.getValue();

      // if this consumer is not relevant for the current response, ignore
      if (this.operation != HttpUtil.toOperation(request.getMethod()))
         return;

      // if the status code of this response does not match what can be consumed, ignore
      if (!Iterables.contains(this.statusCodes, response.getStatusCode()))
         return;

      final String objectString = getObjectString(request, response);
      if (objectString == null)
         throw new IllegalStateException("Unable to determine object");

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
      updateObjectManager(objectName);
   }

   protected abstract void updateObjectManager(ObjectName objectName);
}
