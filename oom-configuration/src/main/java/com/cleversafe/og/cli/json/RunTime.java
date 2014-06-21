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
// Date: Mar 24, 2014
// ---------------------

package com.cleversafe.og.cli.json;

import java.util.concurrent.TimeUnit;

public class RunTime
{
   long duration;
   TimeUnit unit;

   public RunTime()
   {
      this.unit = TimeUnit.SECONDS;
   }

   /**
    * @return the duration
    */
   public long getDuration()
   {
      return this.duration;
   }

   /**
    * @return the unit
    */
   public TimeUnit getUnit()
   {
      return this.unit;
   }
}
