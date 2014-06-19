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

package com.cleversafe.oom.object.manager;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.cleversafe.oom.api.Consumer;
import com.cleversafe.oom.object.LegacyObjectName;
import com.cleversafe.oom.object.ObjectName;
import com.cleversafe.oom.operation.Request;
import com.cleversafe.oom.operation.Response;
import com.google.common.base.Splitter;
import com.google.common.io.BaseEncoding;

public class ObjectNameConsumer implements Consumer<Response>
{
   private final ObjectManager objectManager;
   private final Map<Long, Request> pendingRequests;
   private static final Splitter uriSplitter = Splitter.on("/").omitEmptyStrings();

   public ObjectNameConsumer(
         final ObjectManager objectManager,
         final Map<Long, Request> pendingRequests)
   {
      this.objectManager = checkNotNull(objectManager, "objectManager must not be null");
      this.pendingRequests = checkNotNull(pendingRequests, "pendingRequests must not be null");
   }

   @Override
   public void consume(final Response response)
   {
      // must be non-null
      final Request request = this.pendingRequests.get(response.getRequestId());
      // TODO metadata constants?
      final String responseObjectName = response.getMetaDataEntry("object_name");
      // TODO move processing for SOH write object name response somewhere else
      if (responseObjectName != null)
      {
         // SOH writes
         // TODO fix ObjectManager interface to take strings?
         this.objectManager.writeNameComplete(objectNameFromString(responseObjectName));
      }
      else
      {
         final ObjectName objectName = objectNameFromURI(request.getURI());
         if (objectName != null)
         {
            switch (request.getMethod())
            {
               case GET :
                  this.objectManager.releaseNameFromRead(objectName);
                  break;
               // TODO need to account for response codes
               case PUT :
                  this.objectManager.writeNameComplete(objectName);
                  break;
               default :
                  throw new RuntimeException(String.format("http method unsupported [%s]",
                        request.getMethod()));
            }
         }
      }
   }

   private static ObjectName objectNameFromURI(final URI uri)
   {
      final List<String> parts = uriSplitter.splitToList(uri.getPath());
      // TODO this will break for soh writes rooted at /soh, need a better approach
      // for consumption in general
      if (parts.size() >= 2)
         return objectNameFromString(parts.get(parts.size() - 1));
      return null;
   }

   private static ObjectName objectNameFromString(final String objectName)
   {
      return LegacyObjectName.forBytes(BaseEncoding.base16().lowerCase().decode(objectName));
   }
}
