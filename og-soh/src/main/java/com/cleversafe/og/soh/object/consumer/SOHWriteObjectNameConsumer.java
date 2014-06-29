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
// Date: Jun 23, 2014
// ---------------------

package com.cleversafe.og.soh.object.consumer;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.consumer.ObjectNameConsumer;
import com.cleversafe.og.object.manager.ObjectManager;
import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.util.Operation;

public class SOHWriteObjectNameConsumer extends ObjectNameConsumer
{
   private static final Logger _logger = LoggerFactory.getLogger(SOHWriteObjectNameConsumer.class);

   public SOHWriteObjectNameConsumer(
         final ObjectManager objectManager,
         final Map<Long, Request> pendingRequests,
         final Operation operation,
         final List<Integer> statusCodes)
   {
      super(objectManager, pendingRequests, operation, statusCodes);
   }

   @Override
   protected String getObjectString(final Request request, final Response response)
   {
      return response.getMetadata(Metadata.OBJECT_NAME);
   }
}
