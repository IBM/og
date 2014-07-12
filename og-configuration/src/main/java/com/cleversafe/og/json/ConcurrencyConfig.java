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

package com.cleversafe.og.json;

import java.util.concurrent.TimeUnit;

import com.cleversafe.og.json.enums.ConcurrencyType;

public class ConcurrencyConfig
{
   private final ConcurrencyType type;
   private final double count;
   private final TimeUnit unit;
   private final double rampup;
   private final TimeUnit rampupUnit;

   public ConcurrencyConfig()
   {
      this.type = ConcurrencyType.THREADS;
      this.count = 1.0;
      this.unit = TimeUnit.SECONDS;
      this.rampup = 0.0;
      this.rampupUnit = TimeUnit.SECONDS;
   }

   /**
    * @return the type
    */
   public ConcurrencyType getType()
   {
      return this.type;
   }

   /**
    * @return the count
    */
   public double getCount()
   {
      return this.count;
   }

   /**
    * @return the unit
    */
   public TimeUnit getUnit()
   {
      return this.unit;
   }

   /**
    * @return the rampup
    */
   public double getRampup()
   {
      return this.rampup;
   }

   /**
    * @return the rampupUnit
    */
   public TimeUnit getRampupUnit()
   {
      return this.rampupUnit;
   }

}
