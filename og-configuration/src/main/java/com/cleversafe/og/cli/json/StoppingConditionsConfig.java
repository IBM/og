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
// Date: Jun 22, 2014
// ---------------------

package com.cleversafe.og.cli.json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoppingConditionsConfig
{
   private static Logger _logger = LoggerFactory.getLogger(StoppingConditionsConfig.class);
   private long operations;

   /**
    * @return the operations
    */
   public long getOperations()
   {
      return this.operations;
   }
}
