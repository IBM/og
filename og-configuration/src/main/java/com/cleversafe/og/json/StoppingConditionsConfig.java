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

package com.cleversafe.og.json;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoppingConditionsConfig
{
   private static final Logger _logger = LoggerFactory.getLogger(StoppingConditionsConfig.class);
   private final long operations;
   private final double runtime;
   private final TimeUnit runtimeUnit;
   private final Map<Integer, Integer> statusCodes;
   private final long aborts;

   public StoppingConditionsConfig()
   {
      this.operations = 0;
      this.runtime = 0.0;
      this.runtimeUnit = TimeUnit.SECONDS;
      this.statusCodes = new HashMap<Integer, Integer>();
      this.aborts = 0;
   }

   /**
    * @return the operations
    */
   public long getOperations()
   {
      return this.operations;
   }

   /**
    * @return the runtime
    */
   public double getRuntime()
   {
      return this.runtime;
   }

   /**
    * @return the runtime_unit
    */
   public TimeUnit getRuntimeUnit()
   {
      return this.runtimeUnit;
   }

   /**
    * @return the statusCodes
    */
   public Map<Integer, Integer> getStatusCodes()
   {
      return this.statusCodes;
   }

   /**
    * @return the aborts
    */
   public long getAborts()
   {
      return this.aborts;
   }
}
