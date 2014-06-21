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
// Date: Apr 6, 2014
// ---------------------

package com.cleversafe.og.object.manager;

import java.util.UUID;

import com.cleversafe.og.api.Producer;

public class UUIDObjectNameProducer implements Producer<String>
{
   @Override
   public String produce()
   {
      return UUID.randomUUID().toString().replace("-", "") + "0000";
   }
}
