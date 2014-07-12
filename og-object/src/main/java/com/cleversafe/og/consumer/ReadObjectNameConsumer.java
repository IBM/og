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
// Date: Jun 29, 2014
// ---------------------

package com.cleversafe.og.consumer;

import java.util.List;

import com.cleversafe.og.object.ObjectName;
import com.cleversafe.og.object.manager.ObjectManager;
import com.cleversafe.og.util.Operation;

public class ReadObjectNameConsumer extends ObjectNameConsumer
{
   public ReadObjectNameConsumer(final ObjectManager objectManager, final List<Integer> statusCodes)
   {
      super(objectManager, Operation.READ, statusCodes);
   }

   @Override
   protected void updateObjectManager(final ObjectName objectName)
   {
      this.objectManager.releaseNameFromRead(objectName);
   }
}
