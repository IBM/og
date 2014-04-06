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

import com.cleversafe.oom.api.Producer;
import com.cleversafe.oom.object.ObjectName;
import com.cleversafe.oom.operation.RequestContext;

public class ObjectNameProducer implements Producer<String>
{
   private final ObjectManager objectManager;

   public ObjectNameProducer(final ObjectManager objectManager)
   {
      this.objectManager = checkNotNull(objectManager, "objectManager must not be null");
   }

   @Override
   public String produce(final RequestContext context)
   {
      switch (context.getMethod())
      {
         case GET :
            final ObjectName o = this.objectManager.acquireNameForRead();
            return o.toString();
         case DELETE :
            return this.objectManager.getNameForDelete().toString();
         default :
            throw new RuntimeException(String.format("http method unsupported [%s]",
                  context.getMethod()));
      }
   }
}
